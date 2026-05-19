#!/bin/bash
set -e

# Автоматически определяем IP node1
NODE_IP=$(hostname -I | awk '{print $1}')
echo ">>> Используем IP: $NODE_IP"

# ── 0а. Расширение LVM на текущей ноде (мастер) ──────────────────────────────
echo "0а. Расширение LVM до полного размера диска..."
VG_NAME=$(sudo vgdisplay --short 2>/dev/null | awk '{print $1}' | head -1)
LV_PATH=$(sudo lvdisplay 2>/dev/null | grep "LV Path" | awk '{print $3}' | head -1)

if [ -n "$LV_PATH" ]; then
  sudo lvextend -l +100%FREE "$LV_PATH" 2>/dev/null && \
    sudo resize2fs "$LV_PATH" 2>/dev/null && \
    echo "LVM расширен: $(df -h / | tail -1 | awk '{print $2}') всего, $(df -h / | tail -1 | awk '{print $4}') свободно" \
    || echo "LVM уже максимального размера или расширение не требуется"
else
  echo "LVM не найден, пропускаем"
fi

# Подставляем IP в app.yaml (|| true чтобы не падать при повторном запуске)
sed -i "s|MINIO_PUBLIC_URL_PLACEHOLDER|http://$NODE_IP:30000|g" app.yaml || true

echo "0б. Установка CloudNativePG..."
sudo k3s kubectl apply --server-side -f https://raw.githubusercontent.com/cloudnative-pg/cloudnative-pg/release-1.22/releases/cnpg-1.22.1.yaml

echo "Ждем готовности оператора..."
sudo k3s kubectl wait --for=condition=Available deployment/cnpg-controller-manager -n cnpg-system --timeout=300s

echo "1. Применение секретов и запуск инфраструктуры..."
sudo k3s kubectl apply -f secrets.yaml
sudo k3s kubectl apply -f db-cluster.yaml
sudo k3s kubectl apply -f minio.yaml
sudo k3s kubectl apply -f redpanda.yaml

echo "2. Ожидание базы данных..."
while ! sudo k3s kubectl get pod securechat-db-1 > /dev/null 2>&1; do
  sleep 3
  echo "Ждем под БД..."
done
sudo k3s kubectl wait --for=condition=Ready pod/securechat-db-1 --timeout=400s

echo "3. Ожидание Redpanda..."
sudo k3s kubectl rollout status statefulset/redpanda --timeout=300s

echo "4. Ожидание и настройка MinIO..."
sudo k3s kubectl rollout status statefulset/minio --timeout=300s

MINIO_USER=$(sudo k3s kubectl get secret securechat-secrets -o jsonpath="{.data.MINIO_ROOT_USER}" | base64 --decode)
MINIO_PASS=$(sudo k3s kubectl get secret securechat-secrets -o jsonpath="{.data.MINIO_ROOT_PASSWORD}" | base64 --decode)

sudo k3s kubectl exec -it minio-0 -- mc alias set myminio http://localhost:9000 "$MINIO_USER" "$MINIO_PASS"
sudo k3s kubectl exec -it minio-0 -- mc mb myminio/securechat-files 2>/dev/null || echo "Бакет уже существует"

echo "5. Создание топиков Redpanda..."
echo "Ждем готовности Kafka API..."
until sudo k3s kubectl exec -it redpanda-0 -- rpk topic list > /dev/null 2>&1; do
  sleep 3
  echo "Redpanda еще не готова..."
done

sudo k3s kubectl exec -it redpanda-0 -- rpk topic create secret-chat --partitions 3 --replicas 3 2>/dev/null || echo "Топик secret-chat уже существует"
sudo k3s kubectl exec -it redpanda-0 -- rpk topic create group-chat --partitions 3 --replicas 3 2>/dev/null || echo "Топик group-chat уже существует"
sudo k3s kubectl exec -it redpanda-0 -- rpk topic create system-notifications --partitions 3 --replicas 3 2>/dev/null || echo "Топик system-notifications уже существует"

echo "5б. Настройка retention для топиков Redpanda..."
sudo k3s kubectl exec -it redpanda-0 -- rpk topic alter-config secret-chat \
  --set retention.ms=3600000 --set retention.bytes=52428800
sudo k3s kubectl exec -it redpanda-0 -- rpk topic alter-config group-chat \
  --set retention.ms=3600000 --set retention.bytes=52428800
sudo k3s kubectl exec -it redpanda-0 -- rpk topic alter-config system-notifications \
  --set retention.ms=3600000 --set retention.bytes=10485760

echo "6. Запуск приложения..."
sudo k3s kubectl apply -f app.yaml
sudo k3s kubectl rollout status deployment/securechat-app --timeout=300s

echo "========================================"
echo "СИСТЕМА УСПЕШНО РАЗВЕРНУТА!"
echo "Приложение:    http://$NODE_IP:30080"
echo "MinIO консоль: http://$NODE_IP:30001"
echo "========================================"
