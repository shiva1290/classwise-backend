package com.classwise.api.repository;

import com.classwise.api.entity.ClassEntity;
import com.classwise.api.entity.Enrollment;
import com.classwise.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Integer> {

    Optional<Enrollment> findByClassEntityAndStudent(ClassEntity classEntity, User student);

    Optional<Enrollment> findByClassEntityIdAndStudentId(Integer classEntityId, String studentId);
}
