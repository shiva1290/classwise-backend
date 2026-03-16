package com.classwise.api.service;

import com.classwise.api.entity.ClassEntity;
import com.classwise.api.entity.Enrollment;
import com.classwise.api.entity.User;
import com.classwise.api.repository.ClassRepository;
import com.classwise.api.repository.EnrollmentRepository;
import com.classwise.api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final ClassRepository classRepository;
    private final UserRepository userRepository;

    public EnrollmentService(EnrollmentRepository enrollmentRepository,
            ClassRepository classRepository,
            UserRepository userRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.classRepository = classRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Map<String, Object> createEnrollment(Integer classId, String studentId) {
        Optional<ClassEntity> classOpt = classRepository.findById(classId);
        if (classOpt.isEmpty()) {
            throw new IllegalArgumentException("Class not found");
        }

        Optional<User> userOpt = userRepository.findById(studentId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Student not found");
        }

        Optional<Enrollment> existingEnrollment = enrollmentRepository.findByClassEntityIdAndStudentId(classId,
                studentId);
        if (existingEnrollment.isPresent()) {
            throw new IllegalStateException("Student is already enrolled in this class");
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setClassEntity(classOpt.get());
        enrollment.setStudent(userOpt.get());
        Instant now = Instant.now();
        enrollment.setCreatedAt(now);
        enrollment.setUpdatedAt(now);
        enrollmentRepository.save(enrollment);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", enrollment.getId());
        result.put("classId", classId);
        result.put("studentId", studentId);
        result.put("createdAt", enrollment.getCreatedAt());
        return result;
    }

    @Transactional
    public Map<String, Object> joinClass(String inviteCode, String studentId) {
        Optional<ClassEntity> classOpt = classRepository.findByInviteCode(inviteCode);
        if (classOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid invite code");
        }

        return createEnrollment(classOpt.get().getId(), studentId);
    }
}
