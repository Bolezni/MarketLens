package com.bolezni.mvp_test.authorization.store;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, String> {
    Optional<UserEntity> findByEmail(String email);

    @Modifying
    @Query("UPDATE UserEntity u SET u.tokenVersion = u.tokenVersion + 1 WHERE u.id = :userId")
    void incrementTokenVersion(@Param("userId") String userId);

    @Query("SELECT u.tokenVersion FROM UserEntity u WHERE u.id = :userId")
    int getTokenVersion(@Param("userId") String userId);
}
