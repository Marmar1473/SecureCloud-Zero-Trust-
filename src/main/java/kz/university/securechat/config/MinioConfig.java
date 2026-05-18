package kz.university.securechat.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Value("${minio.url}")
    private String url;

    @Value("${minio.access.key:minioadmin}")
    private String accessKey;

    @Value("${minio.secret.key:minioadmin}")
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        String resolvedAccessKey = System.getenv("MINIO_ACCESS_KEY") != null ?
                System.getenv("MINIO_ACCESS_KEY") : accessKey;
        String resolvedSecretKey = System.getenv("MINIO_SECRET_KEY") != null ?
                System.getenv("MINIO_SECRET_KEY") : secretKey;

        String publicUrl = System.getenv("MINIO_PUBLIC_URL");
        String endpoint = (publicUrl != null && !publicUrl.isBlank()) ? publicUrl : url;

        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(resolvedAccessKey, resolvedSecretKey)
                .build();
    }
}