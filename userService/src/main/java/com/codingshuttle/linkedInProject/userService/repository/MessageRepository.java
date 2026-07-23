package com.codingshuttle.linkedInProject.userService.repository;

import com.codingshuttle.linkedInProject.userService.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    /** The other party in every thread I am part of. Recency handled in Java. */
    @Query("select distinct case when m.senderId = :userId then m.recipientId else m.senderId end "
            + "from Message m where m.senderId = :userId or m.recipientId = :userId")
    List<Long> findPartnerIds(@Param("userId") Long userId);

    @Query("select m from Message m where "
            + "(m.senderId = :a and m.recipientId = :b) or (m.senderId = :b and m.recipientId = :a) "
            + "order by m.createdAt desc")
    Page<Message> findThread(@Param("a") Long a, @Param("b") Long b, Pageable pageable);

    @Query("select m from Message m where "
            + "(m.senderId = :a and m.recipientId = :b) or (m.senderId = :b and m.recipientId = :a) "
            + "order by m.createdAt desc limit 1")
    Optional<Message> findLatestBetween(@Param("a") Long a, @Param("b") Long b);

    long countByRecipientIdAndReadFalse(Long recipientId);

    long countByRecipientIdAndSenderIdAndReadFalse(Long recipientId, Long senderId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Message m set m.read = true where m.recipientId = :userId and m.senderId = :partnerId and m.read = false")
    int markThreadRead(@Param("userId") Long userId, @Param("partnerId") Long partnerId);
}
