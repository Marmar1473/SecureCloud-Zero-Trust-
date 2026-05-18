package kz.university.securechat.controller;

public class GroupMessage {
    private String sender;
    private Long groupId;
    private String content;

    public GroupMessage() {}

    public GroupMessage(String sender, Long groupId, String content) {
        this.sender = sender;
        this.groupId = groupId;
        this.content = content;
    }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}