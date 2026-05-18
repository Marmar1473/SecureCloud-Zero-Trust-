package kz.university.securechat.repository;

import kz.university.securechat.entity.ChatGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatGroupRepository extends JpaRepository<ChatGroup, Long> {
}