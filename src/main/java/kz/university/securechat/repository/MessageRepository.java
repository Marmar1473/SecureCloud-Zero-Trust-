package kz.university.securechat.repository;

import kz.university.securechat.entity.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface MessageRepository extends JpaRepository<MessageEntity, Long> {

    @Query("SELECT m FROM MessageEntity m WHERE (m.senderUuid = :u1 AND m.recipientUuid = :u2) OR (m.senderUuid = :u2 AND m.recipientUuid = :u1) ORDER BY m.timestamp ASC")
    List<MessageEntity> findP2PMessages(@Param("u1") String u1, @Param("u2") String u2);

    List<MessageEntity> findByGroupIdOrderByTimestampAsc(Long groupId);

    @Query("SELECT DISTINCT m.senderUuid FROM MessageEntity m WHERE m.recipientUuid = :userUuid " +
            "UNION " +
            "SELECT DISTINCT m.recipientUuid FROM MessageEntity m WHERE m.senderUuid = :userUuid AND m.recipientUuid IS NOT NULL")
    List<String> findActiveContacts(@Param("userUuid") String userUuid);

    @Query("SELECT DISTINCT m.senderUuid FROM MessageEntity m WHERE m.recipientUuid = :userUuid AND m.read = false")
    List<String> findUnreadSenderUuids(@Param("userUuid") String userUuid);

    @Modifying
    @Transactional
    @Query("UPDATE MessageEntity m SET m.read = true WHERE m.senderUuid = :senderUuid AND m.recipientUuid = :recipientUuid")
    void markAsRead(@Param("senderUuid") String senderUuid, @Param("recipientUuid") String recipientUuid);

    @Query("SELECT DISTINCT m.groupId FROM MessageEntity m WHERE m.groupId IS NOT NULL AND m.read = false AND m.senderUuid != :userUuid AND EXISTS (SELECT gm FROM GroupMember gm WHERE gm.group.id = m.groupId AND gm.user.uuid = :userUuid)")
    List<Long> findUnreadGroupIds(@Param("userUuid") String userUuid);

    @Modifying
    @Transactional
    @Query("UPDATE MessageEntity m SET m.read = true WHERE m.groupId = :groupId")
    void markGroupAsRead(@Param("groupId") Long groupId);

    void deleteByRecipientUuidOrSenderUuid(String recipientUuid, String senderUuid);

    @Modifying
    @Transactional
    @Query("DELETE FROM MessageEntity m WHERE (m.senderUuid = :u1 AND m.recipientUuid = :u2) OR (m.senderUuid = :u2 AND m.recipientUuid = :u1)")
    void deleteP2PMessages(@Param("u1") String u1, @Param("u2") String u2);

    @Modifying
    @Transactional
    void deleteByGroupId(Long groupId);
}