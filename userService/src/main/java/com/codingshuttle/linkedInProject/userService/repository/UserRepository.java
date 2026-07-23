package com.codingshuttle.linkedInProject.userService.repository;

import com.codingshuttle.linkedInProject.userService.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    List<User> findByNameContainingIgnoreCase(String name);

    /**
     * Matches name, headline or company. Company was the obvious gap - a
     * name-only search cannot answer "who works at Acme". The exclude set holds
     * the caller plus anyone in a block relationship with them, so blocked
     * users vanish from search in both directions. It is never empty (it always
     * contains the caller), which keeps the NOT IN clause valid.
     */
    @Query("select u from User u where u.id not in :excludeIds and ("
            + "lower(u.name) like lower(concat('%', :query, '%')) or "
            + "lower(u.headline) like lower(concat('%', :query, '%')) or "
            + "lower(u.currentCompany) like lower(concat('%', :query, '%')))")
    Page<User> search(@Param("query") String query,
                      @Param("excludeIds") java.util.Collection<Long> excludeIds,
                      Pageable pageable);
}
