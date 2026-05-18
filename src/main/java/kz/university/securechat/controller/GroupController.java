package kz.university.securechat.controller;

import kz.university.securechat.entity.ChatGroup;
import kz.university.securechat.entity.GroupMember;
import kz.university.securechat.entity.User;
import kz.university.securechat.repository.ChatGroupRepository;
import kz.university.securechat.repository.GroupMemberRepository;
import kz.university.securechat.repository.MessageRepository;
import kz.university.securechat.repository.UserRepository;
import kz.university.securechat.service.MessageProducer;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final ChatGroupRepository chatGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final MessageProducer messageProducer;
    private final MessageRepository messageRepository;

    public GroupController(ChatGroupRepository chatGroupRepository, GroupMemberRepository groupMemberRepository,
                           UserRepository userRepository, MessageProducer messageProducer,
                           MessageRepository messageRepository) {
        this.chatGroupRepository = chatGroupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
        this.messageProducer = messageProducer;
        this.messageRepository = messageRepository;
    }

    public static class CreateGroupRequest {
        public String name;
        public List<MemberKey> members;
    }

    public static class MemberKey {
        public String uuid;
        public String encryptedKey;
    }

    @PostMapping
    public ResponseEntity<?> createGroup(@RequestBody CreateGroupRequest req, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        User creatorUser = userRepository.findByUsername(principal.getName()).orElse(null);
        if (creatorUser == null) return ResponseEntity.status(401).build();

        ChatGroup group = chatGroupRepository.save(new ChatGroup(req.name, creatorUser.getUuid()));

        for (MemberKey mk : req.members) {
            Optional<User> u = userRepository.findByUuid(mk.uuid);
            u.ifPresent(user -> {
                groupMemberRepository.save(new GroupMember(group, user, mk.encryptedKey, creatorUser.getUuid()));
                if (!user.getUuid().equals(creatorUser.getUuid())) {
                    messageProducer.sendMessage("system-notifications", user.getUsername() + "|new-group");
                }
            });
        }
        return ResponseEntity.ok(Map.of("id", group.getId(), "name", group.getName()));
    }

    @GetMapping
    public ResponseEntity<?> getMyGroups(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        User me = userRepository.findByUsername(principal.getName()).orElse(null);
        if (me == null) return ResponseEntity.badRequest().build();

        List<GroupMember> memberships = groupMemberRepository.findByUser(me);
        List<Map<String, Object>> res = new ArrayList<>();
        for (GroupMember gm : memberships) {
            res.add(Map.of(
                    "id", gm.getGroup().getId(),
                    "name", gm.getGroup().getName(),
                    "encryptedKey", gm.getEncryptedKey(),
                    "addedBy", gm.getAddedBy(),
                    "creatorUuid", gm.getGroup().getCreatorUuid()
            ));
        }
        return ResponseEntity.ok(res);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteGroup(@PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        User me = userRepository.findByUsername(principal.getName()).orElse(null);
        if (me == null) return ResponseEntity.status(401).build();

        ChatGroup group = chatGroupRepository.findById(id).orElse(null);
        if (group == null) return ResponseEntity.notFound().build();
        if (!group.getCreatorUuid().equals(me.getUuid())) return ResponseEntity.status(403).build();

        List<GroupMember> members = groupMemberRepository.findByGroup(group);
        for (GroupMember gm : members) {
            if (!gm.getUser().getUuid().equals(me.getUuid())) {
                messageProducer.sendMessage("system-notifications",
                        gm.getUser().getUsername() + "|group-deleted|" + id);
            }
        }

        messageRepository.deleteByGroupId(id);
        groupMemberRepository.deleteAll(members);
        chatGroupRepository.delete(group);

        return ResponseEntity.ok(Map.of("message", "Group deleted"));
    }

    @DeleteMapping("/{id}/leave")
    @Transactional
    public ResponseEntity<?> leaveGroup(@PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        User me = userRepository.findByUsername(principal.getName()).orElse(null);
        if (me == null) return ResponseEntity.status(401).build();

        ChatGroup group = chatGroupRepository.findById(id).orElse(null);
        if (group == null) return ResponseEntity.notFound().build();

        if (group.getCreatorUuid().equals(me.getUuid())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Creator cannot leave — delete the group instead"));
        }

        GroupMember membership = groupMemberRepository.findByGroupAndUser(group, me).orElse(null);
        if (membership == null) return ResponseEntity.status(403).build();

        groupMemberRepository.delete(membership);
        return ResponseEntity.ok(Map.of("message", "Left group"));
    }
}