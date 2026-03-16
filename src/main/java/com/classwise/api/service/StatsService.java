package com.classwise.api.service;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatsService {

    private final EntityManager em;

    public StatsService(EntityManager em) {
        this.em = em;
    }

    public Map<String, Object> getOverview() {
        String sql = "SELECT " +
                "(SELECT COUNT(*) FROM \"user\") as users_count, " +
                "(SELECT COUNT(*) FROM \"user\" WHERE role = 'teacher') as teachers_count, " +
                "(SELECT COUNT(*) FROM \"user\" WHERE role = 'admin') as admins_count, " +
                "(SELECT COUNT(*) FROM subjects) as subjects_count, " +
                "(SELECT COUNT(*) FROM departments) as departments_count, " +
                "(SELECT COUNT(*) FROM classes) as classes_count";
        Object[] row = (Object[]) em.createNativeQuery(sql).getSingleResult();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("usersCount", ((Number) row[0]).longValue());
        result.put("teachersCount", ((Number) row[1]).longValue());
        result.put("adminsCount", ((Number) row[2]).longValue());
        result.put("subjectsCount", ((Number) row[3]).longValue());
        result.put("departmentsCount", ((Number) row[4]).longValue());
        result.put("classesCount", ((Number) row[5]).longValue());
        return result;
    }

    public Map<String, Object> getLatest(int limit) {
        String classesSql = "SELECT c.id, c.name, c.invite_code, c.status, c.created_at, " +
                "s.id as subject_id, s.name as subject_name, s.code as subject_code, " +
                "u.id as teacher_id, u.name as teacher_name, u.image as teacher_image " +
                "FROM classes c JOIN subjects s ON c.subject_id = s.id JOIN \"user\" u ON c.teacher_id = u.id " +
                "ORDER BY c.created_at DESC LIMIT :limit";
        @SuppressWarnings("unchecked")
        List<Object[]> classRows = em.createNativeQuery(classesSql)
                .setParameter("limit", limit).getResultList();

        List<Map<String, Object>> latestClasses = new ArrayList<>();
        for (Object[] row : classRows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row[0]);
            item.put("name", row[1]);
            item.put("inviteCode", row[2]);
            item.put("status", row[3] != null ? row[3].toString() : null);
            item.put("createdAt", row[4]);
            item.put("subject", Map.of("id", row[5], "name", row[6], "code", row[7]));
            Map<String, Object> teacher = new LinkedHashMap<>();
            teacher.put("id", row[8]);
            teacher.put("name", row[9]);
            teacher.put("image", row[10]);
            item.put("teacher", teacher);
            latestClasses.add(item);
        }

        String teachersSql = "SELECT u.id, u.name, u.email, u.image, u.created_at " +
                "FROM \"user\" u WHERE u.role = 'teacher' ORDER BY u.created_at DESC LIMIT :limit";
        @SuppressWarnings("unchecked")
        List<Object[]> teacherRows = em.createNativeQuery(teachersSql)
                .setParameter("limit", limit).getResultList();

        List<Map<String, Object>> latestTeachers = new ArrayList<>();
        for (Object[] row : teacherRows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row[0]);
            item.put("name", row[1]);
            item.put("email", row[2]);
            item.put("image", row[3]);
            item.put("createdAt", row[4]);
            latestTeachers.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("latestClasses", latestClasses);
        result.put("latestTeachers", latestTeachers);
        return result;
    }

    public Map<String, Object> getCharts() {
        @SuppressWarnings("unchecked")
        List<Object[]> roleRows = em.createNativeQuery(
                "SELECT role, COUNT(*) FROM \"user\" GROUP BY role").getResultList();
        List<Map<String, Object>> usersByRole = new ArrayList<>();
        for (Object[] row : roleRows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("role", row[0] != null ? row[0].toString() : "unknown");
            item.put("count", ((Number) row[1]).longValue());
            usersByRole.add(item);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> deptRows = em.createNativeQuery(
                "SELECT d.name, COUNT(s.id) FROM departments d LEFT JOIN subjects s ON s.department_id = d.id GROUP BY d.id, d.name ORDER BY COUNT(s.id) DESC")
                .getResultList();
        List<Map<String, Object>> subjectsByDepartment = new ArrayList<>();
        for (Object[] row : deptRows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("department", row[0]);
            item.put("count", ((Number) row[1]).longValue());
            subjectsByDepartment.add(item);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> subRows = em.createNativeQuery(
                "SELECT s.name, COUNT(c.id) FROM subjects s LEFT JOIN classes c ON c.subject_id = s.id GROUP BY s.id, s.name ORDER BY COUNT(c.id) DESC")
                .getResultList();
        List<Map<String, Object>> classesBySubject = new ArrayList<>();
        for (Object[] row : subRows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("subject", row[0]);
            item.put("count", ((Number) row[1]).longValue());
            classesBySubject.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("usersByRole", usersByRole);
        result.put("subjectsByDepartment", subjectsByDepartment);
        result.put("classesBySubject", classesBySubject);
        return result;
    }
}
