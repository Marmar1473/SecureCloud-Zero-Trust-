package kz.university.securechat.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
public class MessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String senderUuid;

    @Column
    private String recipientUuid;

    @Column
    private Long groupId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    @Column(nullable = false)
    private boolean read = false;

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public MessageEntity() {}

    public MessageEntity(String senderUuid, String recipientUuid, Long groupId, String content) {
        this.senderUuid = senderUuid;
        this.recipientUuid = recipientUuid;
        this.groupId = groupId;
        this.content = content;
    }

    public Long getId() { return id; }
    public String getSenderUuid() { return senderUuid; }
    public String getRecipientUuid() { return recipientUuid; }
    public Long getGroupId() { return groupId; }
    public String getContent() { return content; }
    public LocalDateTime getTimestamp() { return timestamp; }
}