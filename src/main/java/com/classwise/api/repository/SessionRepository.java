package com.classwise.api.repository;

import com.classwise.api.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SessionRepository extends JpaRepository<Session, String> {

    Optional<Session> findByToken(String token);

    @Query("SELECT s FROM Session s JOIN FETCH s.user WHERE s.token = :token")
    Optional<Session> findByTokenWithUser(@Param("token") String token);

    void deleteByToken(String token);

    void deleteByUserId(String userId);
}
