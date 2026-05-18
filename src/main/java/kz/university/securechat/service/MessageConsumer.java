package kz.university.securechat.service;

import kz.university.securechat.controller.ChatMessage;
import kz.university.securechat.controller.GroupMessage;
import kz.university.securechat.entity.User;
import kz.university.securechat.repository.UserRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class MessageConsumer {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    public MessageConsumer(SimpMessagingTemplate messagingTemplate, UserRepository userRepository) {
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
    }

    @KafkaListener(topics = "secret-chat", groupId = "${random.uuid}")
    public void listen(String payload) {
        try {
            String[] parts = payload.split("\\|", 3);
            if (parts.length < 3) return;

            String senderUuid = parts[0];
            String recipientUuid = parts[1];
            String content = parts[2];

            User recipient = userRepository.findByUuid(recipientUuid).orElse(null);
            if (recipient != null) {
                ChatMessage msg = new ChatMessage(senderUuid, recipientUuid, content);
                messagingTemplate.convertAndSendToUser(recipient.getUsername(), "/queue/messages", msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @KafkaListener(topics = "group-chat", groupId = "${random.uuid}")
    public void listenGroup(String payload) {
        try {
            String[] parts = payload.split("\\|", 3);
            if (parts.length < 3) return;

            String senderUuid = parts[0];
            Long groupId = Long.parseLong(parts[1]);
            String content = parts[2];

            GroupMessage msg = new GroupMessage(senderUuid, groupId, content);
            messagingTemplate.convertAndSend("/topic/group/" + groupId, (Object) msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @KafkaListener(topics = "system-notifications", groupId = "#{T(java.util.UUID).randomUUID().toString()}")
    public void listenSystem(String payload) {
        try {
            String[] parts = payload.split("\\|");
            if (parts.length == 2 && "new-group".equals(parts[1])) {
                messagingTemplate.convertAndSendToUser(parts[0], "/queue/new-group", "new");
            } else if (parts.length == 3 && "user-deleted".equals(parts[1])) {
                messagingTemplate.convertAndSendToUser(parts[0], "/queue/user-deleted", parts[2]);
            } else if (parts.length == 3 && "group-deleted".equals(parts[1])) {
                messagingTemplate.convertAndSendToUser(parts[0], "/queue/group-deleted", parts[2]);
            } else if (parts.length == 3 && "chat-deleted".equals(parts[1])) {
                messagingTemplate.convertAndSendToUser(parts[0], "/queue/chat-deleted", parts[2]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}