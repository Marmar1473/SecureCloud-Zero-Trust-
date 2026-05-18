package kz.university.securechat.controller;

import kz.university.securechat.entity.MessageEntity;
import kz.university.securechat.entity.User;
import kz.university.securechat.repository.GroupMemberRepository;
import kz.university.securechat.repository.MessageRepository;
import kz.university.securechat.repository.UserRepository;
import kz.university.securechat.service.MessageProducer;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class ChatController {

    private final MessageProducer messageProducer;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final GroupMemberRepository groupMemberRepository;

    public ChatController(MessageProducer messageProducer, UserRepository userRepository,
                          MessageRepository messageRepository, GroupMemberRepository groupMemberRepository) {
        this.messageProducer = messageProducer;
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.groupMemberRepository = groupMemberRepository;
    }

    @GetMapping("/api/user")
    public ResponseEntity<?> getCurrentUser(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        if (user == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(Map.of("username", user.getUsername(), "uuid", user.getUuid()));
    }

    @PostMapping("/send")
    public ResponseEntity<Void> sendMessage(@RequestBody ChatMessage msg, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        User sender = userRepository.findByUsername(principal.getName()).orElse(null);
        if (sender == null) return ResponseEntity.status(401).build();

        msg.setSender(sender.getUuid());

        MessageEntity entity = new MessageEntity(msg.getSender(), msg.getRecipient(), null, msg.getContent());
        messageRepository.save(entity);

        messageProducer.sendMessage("secret-chat", msg.getSender() + "|" + msg.getRecipient() + "|" + msg.getContent());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/send/pfs")
    public ResponseEntity<Void> sendPfsMessage(@RequestBody ChatMessage msg, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        User sender = userRepository.findByUsername(principal.getName()).orElse(null);
        if (sender == null) return ResponseEntity.status(401).build();

        msg.setSender(sender.getUuid());

        messageProducer.sendMessage("secret-chat", msg.getSender() + "|" + msg.getRecipient() + "|" + msg.getContent());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/send/group")
    public ResponseEntity<Void> sendGroupMessage(@RequestBody GroupMessage msg, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        User sender = userRepository.findByUsername(principal.getName()).orElse(null);
        if (sender == null) return ResponseEntity.status(401).build();

        msg.setSender(sender.getUuid());

        MessageEntity entity = new MessageEntity(msg.getSender(), null, msg.getGroupId(), msg.getContent());
        messageRepository.save(entity);

        messageProducer.sendMessage("group-chat", msg.getSender() + "|" + msg.getGroupId() + "|" + msg.getContent());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/messages/p2p/{uuid}")
    public ResponseEntity<?> getP2PHistory(@PathVariable String uuid, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        User me = userRepository.findByUsername(principal.getName()).orElse(null);
        if (me == null) return ResponseEntity.status(401).build();

        List<MessageEntity> history = messageRepository.findP2PMessages(me.getUuid(), uuid);
        List<ChatMessage> dtos = history.stream()
                .map(m -> new ChatMessage(m.getSenderUuid(), m.getRecipientUuid(), m.getContent()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/api/messages/group/{id}")
    public ResponseEntity<?> getGroupHistory(@PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        User me = userRepository.findByUsername(principal.getName()).orElse(null);
        if (me == null) return ResponseEntity.status(401).build();

        boolean isMember = groupMemberRepository.findByUser(me).stream().anyMatch(gm -> gm.getGroup().getId().equals(id));
        if (!isMember) return ResponseEntity.status(403).build();

        List<MessageEntity> history = messageRepository.findByGroupIdOrderByTimestampAsc(id);
        List<GroupMessage> dtos = history.stream()
                .map(m -> new GroupMessage(m.getSenderUuid(), m.getGroupId(), m.getContent()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/api/contacts")
    public ResponseEntity<?> getActiveContacts(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        User me = userRepository.findByUsername(principal.getName()).orElse(null);
        if (me == null) return ResponseEntity.status(401).build();

        List<String> contactUuids = messageRepository.findActiveContacts(me.getUuid());

        List<Map<String, String>> contacts = contactUuids.stream()
                .map(uuid -> userRepository.findByUuid(uuid))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .map(user -> Map.of("username", user.getUsername(), "uuid", user.getUuid(), "publicKey", user.getPublicKey()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(contacts);
    }

    @GetMapping("/api/messages/unread")
    public ResponseEntity<?> getUnreadSenders(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        User me = userRepository.findByUsername(principal.getName()).orElse(null);
        if (me == null) return ResponseEntity.status(401).build();
        List<String> senderUuids = messageRepository.findUnreadSenderUuids(me.getUuid());
        return ResponseEntity.ok(senderUuids);
    }

    @PostMapping("/api/messages/read/{senderUuid}")
    public ResponseEntity<?> markAsRead(@PathVariable String senderUuid, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        User me = userRepository.findByUsername(principal.getName()).orElse(null);
        if (me == null) return ResponseEntity.status(401).build();
        messageRepository.markAsRead(senderUuid, me.getUuid());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/messages/unread-groups")
    public ResponseEntity<?> getUnreadGroups(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        User me = userRepository.findByUsername(principal.getName()).orElse(null);
        if (me == null) return ResponseEntity.status(401).build();
        List<Long> groupIds = messageRepository.findUnreadGroupIds(me.getUuid());
        return ResponseEntity.ok(groupIds);
    }

    @PostMapping("/api/messages/read-group/{groupId}")
    public ResponseEntity<?> markGroupAsRead(@PathVariable Long groupId, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        messageRepository.markGroupAsRead(groupId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/api/messages/p2p/{uuid}")
    @Transactional
    public ResponseEntity<?> deleteP2PChat(@PathVariable String uuid, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        User me = userRepository.findByUsername(principal.getName()).orElse(null);
        if (me == null) return ResponseEntity.status(401).build();
        messageRepository.deleteP2PMessages(me.getUuid(), uuid);
        userRepository.findByUuid(uuid).ifPresent(contact -> {
            messageProducer.sendMessage("system-notifications",
                    contact.getUsername() + "|chat-deleted|" + me.getUuid());
        });
        return ResponseEntity.ok(Map.of("message", "Chat deleted"));
    }
}