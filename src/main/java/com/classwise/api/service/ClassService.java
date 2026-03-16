package com.classwise.api.service;

import com.classwise.api.entity.ClassEntity;
import com.classwise.api.entity.Subject;
import com.classwise.api.entity.User;
import com.classwise.api.repository.ClassRepository;
import com.classwise.api.repository.SubjectRepository;
import com.classwise.api.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ClassService {

    private final ClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final EntityManager em;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ClassService(ClassRepository classRepository, SubjectRepository subjectRepository,
            UserRepository userRepository, EntityManager em) {
        this.classRepository = classRepository;
        this.subjectRepository = subjectRepository;
        this.userRepository = userRepository;
        this.em = em;
    }

    public Map<String, Object> listClasses(String search, String subject, String teacher, int page, int limit) {
        int currentPage = Math.max(1, page);
        int limitPerPage = Math.max(1, limit);
        int offset = (currentPage - 1) * limitPerPage;

        StringBuilder whereSql = new StringBuilder(" WHERE 1=1 ");
        if (search != null && !search.trim().isEmpty()) {
            whereSql.append(" AND (LOWER(c.name) LIKE LOWER(:search) OR LOWER(c.invite_code) LIKE LOWER(:search)) ");
        }
        if (subject != null && !subject.trim().isEmpty()) {
            whereSql.append(" AND s.name = :subject ");
        }
        if (teacher != null && !teacher.trim().isEmpty()) {
            whereSql.append(" AND u.name = :teacher ");
        }

        String countSql = "SELECT COUNT(*) FROM classes c " +
                "JOIN subjects s ON c.subject_id = s.id " +
                "JOIN \"user\" u ON c.teacher_id = u.id " + whereSql;
        Query countQuery = em.createNativeQuery(countSql);
        setSearchParams(countQuery, search, subject, teacher);
        long totalItems = ((Number) countQuery.getSingleResult()).longValue();

        String dataSql = "SELECT c.id, c.name, c.invite_code, c.capacity, c.status, " +
                "c.banner_url, c.banner_cld_pub_id, c.description, c.schedules, " +
                "c.created_at, c.updated_at, " +
                "s.id as subject_id, s.name as subject_name, s.code as subject_code, " +
                "d.id as dept_id, d.name as dept_name, " +
                "u.id as teacher_id, u.name as teacher_name, u.image as teacher_image, " +
                "(SELECT COUNT(*) FROM enrollments e WHERE e.class_id = c.id) as student_count " +
                "FROM classes c " +
                "JOIN subjects s ON c.subject_id = s.id " +
                "JOIN departments d ON s.department_id = d.id " +
                "JOIN \"user\" u ON c.teacher_id = u.id " +
                whereSql +
                " ORDER BY c.created_at DESC LIMIT :limit OFFSET :offset";
        Query dataQuery = em.createNativeQuery(dataSql);
        setSearchParams(dataQuery, search, subject, teacher);
        dataQuery.setParameter("limit", limitPerPage);
        dataQuery.setParameter("offset", offset);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = dataQuery.getResultList();
        List<Map<String, Object>> items = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row[0]);
            item.put("name", row[1]);
            item.put("inviteCode", row[2]);
            item.put("capacity", row[3]);
            item.put("status", row[4] != null ? row[4].toString() : null);
            item.put("bannerUrl", row[5]);
            item.put("bannerCldPubId", row[6]);
            item.put("description", row[7]);
            item.put("schedules", row[8]);
            item.put("createdAt", row[9]);
            item.put("updatedAt", row[10]);
            item.put("subject", Map.of(
                    "id", row[11],
                    "name", row[12],
                    "code", row[13]));
            item.put("department", Map.of(
                    "id", row[14],
                    "name", row[15]));
            Map<String, Object> teacherMap = new LinkedHashMap<>();
            teacherMap.put("id", row[16]);
            teacherMap.put("name", row[17]);
            teacherMap.put("image", row[18]);
            item.put("teacher", teacherMap);
            item.put("studentCount", ((Number) row[19]).longValue());
            items.add(item);
        }

        int totalPages = (int) Math.ceil((double) totalItems / limitPerPage);

        Map<String, Object> pagination = new LinkedHashMap<>();
        pagination.put("page", currentPage);
        pagination.put("limit", limitPerPage);
        pagination.put("total", totalItems);
        pagination.put("totalPages", totalPages);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("data", items);
        response.put("pagination", pagination);
        return response;
    }

    public Map<String, Object> getClassDetails(Integer id) {
        String sql = "SELECT c.id, c.name, c.invite_code, c.capacity, c.status, " +
                "c.banner_url, c.banner_cld_pub_id, c.description, c.schedules, " +
                "c.created_at, c.updated_at, " +
                "s.id as subject_id, s.name as subject_name, s.code as subject_code, " +
                "d.id as dept_id, d.name as dept_name, d.code as dept_code, " +
                "u.id as teacher_id, u.name as teacher_name, u.email as teacher_email, u.image as teacher_image, " +
                "(SELECT COUNT(*) FROM enrollments e WHERE e.class_id = c.id) as student_count " +
                "FROM classes c " +
                "JOIN subjects s ON c.subject_id = s.id " +
                "JOIN departments d ON s.department_id = d.id " +
                "JOIN \"user\" u ON c.teacher_id = u.id " +
                "WHERE c.id = :id";

        @SuppressWarnings("unchecked")
        List<Object[]> results = em.createNativeQuery(sql).setParameter("id", id).getResultList();
        if (results.isEmpty())
            return null;

        Object[] row = results.get(0);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", row[0]);
        result.put("name", row[1]);
        result.put("inviteCode", row[2]);
        result.put("capacity", row[3]);
        result.put("status", row[4] != null ? row[4].toString() : null);
        result.put("bannerUrl", row[5]);
        result.put("bannerCldPubId", row[6]);
        result.put("description", row[7]);
        result.put("schedules", row[8]);
        result.put("createdAt", row[9]);
        result.put("updatedAt", row[10]);
        result.put("subject", Map.of("id", row[11], "name", row[12], "code", row[13]));
        result.put("department", Map.of("id", row[14], "name", row[15], "code", row[16]));
        Map<String, Object> teacherMap = new LinkedHashMap<>();
        teacherMap.put("id", row[17]);
        teacherMap.put("name", row[18]);
        teacherMap.put("email", row[19]);
        teacherMap.put("image", row[20]);
        result.put("teacher", teacherMap);
        result.put("studentCount", ((Number) row[21]).longValue());
        return result;
    }

    public Map<String, Object> getClassUsers(Integer classId, String role, int page, int limit) {
        int currentPage = Math.max(1, page);
        int limitPerPage = Math.max(1, limit);
        int offset = (currentPage - 1) * limitPerPage;

        // Teachers for a class = the teacher assigned to the class
        // Students for a class = enrolled students
        if ("teacher".equals(role)) {
            String sql = "SELECT u.id, u.name, u.email, u.image, u.role, u.created_at " +
                    "FROM \"user\" u JOIN classes c ON c.teacher_id = u.id WHERE c.id = :classId";
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery(sql)
                    .setParameter("classId", classId).getResultList();

            List<Map<String, Object>> items = new ArrayList<>();
            for (Object[] row : rows) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", row[0]);
                item.put("name", row[1]);
                item.put("email", row[2]);
                item.put("image", row[3]);
                item.put("role", row[4] != null ? row[4].toString() : null);
                item.put("createdAt", row[5]);
                items.add(item);
            }
            Map<String, Object> pagination = new LinkedHashMap<>();
            pagination.put("page", 1);
            pagination.put("limit", limitPerPage);
            pagination.put("total", (long) items.size());
            pagination.put("totalPages", 1);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("data", items);
            response.put("pagination", pagination);
            return response;
        }

        // Students
        String countSql = "SELECT COUNT(DISTINCT u.id) FROM \"user\" u " +
                "JOIN enrollments e ON e.student_id = u.id WHERE e.class_id = :classId";
        long totalItems = ((Number) em.createNativeQuery(countSql)
                .setParameter("classId", classId).getSingleResult()).longValue();

        String dataSql = "SELECT DISTINCT u.id, u.name, u.email, u.image, u.role, u.created_at " +
                "FROM \"user\" u JOIN enrollments e ON e.student_id = u.id " +
                "WHERE e.class_id = :classId ORDER BY u.created_at DESC LIMIT :limit OFFSET :offset";
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(dataSql)
                .setParameter("classId", classId)
                .setParameter("limit", limitPerPage)
                .setParameter("offset", offset)
                .getResultList();

        List<Map<String, Object>> items = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row[0]);
            item.put("name", row[1]);
            item.put("email", row[2]);
            item.put("image", row[3]);
            item.put("role", row[4] != null ? row[4].toString() : null);
            item.put("createdAt", row[5]);
            items.add(item);
        }

        int totalPages = (int) Math.ceil((double) totalItems / limitPerPage);

        Map<String, Object> pagination = new LinkedHashMap<>();
        pagination.put("page", currentPage);
        pagination.put("limit", limitPerPage);
        pagination.put("total", totalItems);
        pagination.put("totalPages", totalPages);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("data", items);
        response.put("pagination", pagination);
        return response;
    }

    @Transactional
    public Map<String, Object> createClass(String name, String teacherId, Integer subjectId,
            Integer capacity, String description, String status,
            String bannerUrl, String bannerCldPubId, Object schedules) {
        Optional<Subject> subjectOpt = subjectRepository.findById(subjectId);
        Optional<User> teacherOpt = userRepository.findById(teacherId);
        if (subjectOpt.isEmpty() || teacherOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid subject or teacher");
        }

        ClassEntity entity = new ClassEntity();
        entity.setSubject(subjectOpt.get());
        entity.setTeacher(teacherOpt.get());
        entity.setInviteCode(UUID.randomUUID().toString().substring(0, 8));
        entity.setName(name);
        entity.setBannerCldPubId(bannerCldPubId);
        entity.setBannerUrl(bannerUrl);
        entity.setCapacity(capacity != null ? capacity : 50);
        entity.setDescription(description);
        entity.setStatus(entity.getStatus());
        Instant now = Instant.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        if (schedules != null) {
            try {
                entity.setSchedules(objectMapper.writeValueAsString(schedules));
            } catch (JsonProcessingException e) {
                entity.setSchedules("[]");
            }
        } else {
            entity.setSchedules("[]");
        }

        ClassEntity saved = classRepository.save(entity);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", saved.getId());
        result.put("name", saved.getName());
        result.put("inviteCode", saved.getInviteCode());
        return result;
    }

    private void setSearchParams(Query query, String search, String subject, String teacher) {
        if (search != null && !search.trim().isEmpty()) {
            query.setParameter("search", "%" + search + "%");
        }
        if (subject != null && !subject.trim().isEmpty()) {
            query.setParameter("subject", subject);
        }
        if (teacher != null && !teacher.trim().isEmpty()) {
            query.setParameter("teacher", teacher);
        }
    }
}
