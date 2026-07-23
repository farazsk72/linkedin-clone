package com.codingshuttle.linkedInProject.userService.repository;

import com.codingshuttle.linkedInProject.userService.entity.Block;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BlockRepository extends JpaRepository<Block, Long> {

    boolean existsByBlockerUserIdAndBlockedUserId(Long blockerUserId, Long blockedUserId);

    void deleteByBlockerUserIdAndBlockedUserId(Long blockerUserId, Long blockedUserId);

    List<Block> findByBlockerUserId(Long blockerUserId);

    /** True if either user has blocked the other - used to gate any interaction. */
    @Query("select count(b) > 0 from Block b where "
            + "(b.blockerUserId = :a and b.blockedUserId = :b) or "
            + "(b.blockerUserId = :b and b.blockedUserId = :a)")
    boolean blockExistsBetween(@Param("a") Long a, @Param("b") Long b);

    /** Every user id in a block relationship with me, in either direction. */
    @Query("select case when b.blockerUserId = :userId then b.blockedUserId else b.blockerUserId end "
            + "from Block b where b.blockerUserId = :userId or b.blockedUserId = :userId")
    List<Long> findAllRelatedUserIds(@Param("userId") Long userId);
}
