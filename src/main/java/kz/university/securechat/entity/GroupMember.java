package kz.university.securechat.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "group_members")
public class GroupMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private ChatGroup group;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String encryptedKey;

    @Column(nullable = false)
    private String addedBy;

    public GroupMember() {}

    public GroupMember(ChatGroup group, User user, String encryptedKey, String addedBy) {
        this.group = group;
        this.user = user;
        this.encryptedKey = encryptedKey;
        this.addedBy = addedBy;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ChatGroup getGroup() { return group; }
    public void setGroup(ChatGroup group) { this.group = group; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getEncryptedKey() { return encryptedKey; }
    public void setEncryptedKey(String encryptedKey) { this.encryptedKey = encryptedKey; }
    public String getAddedBy() { return addedBy; }
    public void setAddedBy(String addedBy) { this.addedBy = addedBy; }
}