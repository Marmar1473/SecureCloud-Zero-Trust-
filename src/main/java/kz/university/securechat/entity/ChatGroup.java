package kz.university.securechat.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "chat_groups")
public class ChatGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String creatorUuid;

    public ChatGroup() {}

    public ChatGroup(String name, String creatorUuid) {
        this.name = name;
        this.creatorUuid = creatorUuid;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCreatorUuid() { return creatorUuid; }
    public void setCreatorUuid(String creatorUuid) { this.creatorUuid = creatorUuid; }
}