package com.classwise.api.repository;

import com.classwise.api.entity.ClassEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClassRepository extends JpaRepository<ClassEntity, Integer> {

    Optional<ClassEntity> findByInviteCode(String inviteCode);
}

