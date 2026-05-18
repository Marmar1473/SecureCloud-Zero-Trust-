package kz.university.securechat.repository;

import kz.university.securechat.entity.ChatGroup;
import kz.university.securechat.entity.GroupMember;
import kz.university.securechat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    List<GroupMember> findByUser(User user);
    List<GroupMember> findByGroup(ChatGroup group);
    Optional<GroupMember> findByGroupAndUser(ChatGroup group, User user);
}