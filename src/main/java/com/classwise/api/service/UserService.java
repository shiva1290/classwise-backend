package com.classwise.api.service;

import com.classwise.api.entity.User;
import com.classwise.api.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final EntityManager em;

    public UserService(UserRepository userRepository, EntityManager em) {
        this.userRepository = userRepository;
        this.em = em;
    }

    public Map<String, Object> listUsers(String search, String role, int page, int limit) {
        int currentPage = Math.max(1, page);
        int limitPerPage = Math.max(1, limit);
        int offset = (currentPage - 1) * limitPerPage;

        StringBuilder whereSql = new StringBuilder(" WHERE 1=1 ");
        if (search != null && !search.trim().isEmpty()) {
            whereSql.append(" AND (LOWER(u.name) LIKE LOWER(:search) OR LOWER(u.email) LIKE LOWER(:search)) ");
        }
        if (role != null && !role.trim().isEmpty()) {
            whereSql.append(" AND u.role = CAST(:role AS role) ");
        }

        String countSql = "SELECT COUNT(*) FROM \"user\" u " + whereSql;
        Query countQuery = em.createNativeQuery(countSql);
        if (search != null && !search.trim().isEmpty()) {
            countQuery.setParameter("search", "%" + search + "%");
        }
        if (role != null && !role.trim().isEmpty()) {
            countQuery.setParameter("role", role);
        }
        long totalItems = ((Number) countQuery.getSingleResult()).longValue();

        String dataSql = "SELECT u.id, u.name, u.email, u.image, u.role, u.image_cld_pub_id, u.created_at, u.updated_at "
                +
                "FROM \"user\" u " + whereSql +
                " ORDER BY u.created_at DESC LIMIT :limit OFFSET :offset";
        Query dataQuery = em.createNativeQuery(dataSql);
        if (search != null && !search.trim().isEmpty()) {
            dataQuery.setParameter("search", "%" + search + "%");
        }
        if (role != null && !role.trim().isEmpty()) {
            dataQuery.setParameter("role", role);
        }
        dataQuery.setParameter("limit", limitPerPage);
        dataQuery.setParameter("offset", offset);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = dataQuery.getResultList();
        List<Map<String, Object>> items = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row[0]);
            item.put("name", row[1]);
            item.put("email", row[2]);
            item.put("image", row[3]);
            item.put("role", row[4] != null ? row[4].toString() : null);
            item.put("imageCldPubId", row[5]);
            item.put("createdAt", row[6]);
            item.put("updatedAt", row[7]);
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

    public Map<String, Object> getUser(String id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty())
            return null;

        User user = userOpt.get();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.getId());
        result.put("name", user.getName());
        result.put("email", user.getEmail());
        result.put("image", user.getImage());
        result.put("role", user.getRole().name());
        result.put("imageCldPubId", user.getImageCldPubId());
        result.put("createdAt", user.getCreatedAt());
        result.put("updatedAt", user.getUpdatedAt());
        return result;
    }

    public Map<String, Object> getUserDepartments(String userId, int page, int limit) {
        int currentPage = Math.max(1, page);
        int limitPerPage = Math.max(1, limit);
        int offset = (currentPage - 1) * limitPerPage;

        // Get departments via classes the user is either teaching or enrolled in
        String baseJoin = "FROM departments d WHERE d.id IN (" +
                "SELECT DISTINCT s.department_id FROM subjects s JOIN classes c ON c.subject_id = s.id " +
                "WHERE c.teacher_id = :userId " +
                "UNION " +
                "SELECT DISTINCT s.department_id FROM subjects s JOIN classes c ON c.subject_id = s.id " +
                "JOIN enrollments e ON e.class_id = c.id WHERE e.student_id = :userId" +
                ")";

        String countSql = "SELECT COUNT(*) " + baseJoin;
        long totalItems = ((Number) em.createNativeQuery(countSql)
                .setParameter("userId", userId).getSingleResult()).longValue();

        String dataSql = "SELECT d.id, d.code, d.name, d.description, d.created_at, d.updated_at " +
                baseJoin + " ORDER BY d.created_at DESC LIMIT :limit OFFSET :offset";
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(dataSql)
                .setParameter("userId", userId)
                .setParameter("limit", limitPerPage)
                .setParameter("offset", offset)
                .getResultList();

        List<Map<String, Object>> items = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row[0]);
            item.put("code", row[1]);
            item.put("name", row[2]);
            item.put("description", row[3]);
            item.put("createdAt", row[4]);
            item.put("updatedAt", row[5]);
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

    public Map<String, Object> getUserSubjects(String userId, int page, int limit) {
        int currentPage = Math.max(1, page);
        int limitPerPage = Math.max(1, limit);
        int offset = (currentPage - 1) * limitPerPage;

        String baseJoin = "FROM subjects s JOIN departments d ON s.department_id = d.id WHERE s.id IN (" +
                "SELECT DISTINCT c.subject_id FROM classes c WHERE c.teacher_id = :userId " +
                "UNION " +
                "SELECT DISTINCT c.subject_id FROM classes c JOIN enrollments e ON e.class_id = c.id WHERE e.student_id = :userId"
                +
                ")";

        String countSql = "SELECT COUNT(*) " + baseJoin;
        long totalItems = ((Number) em.createNativeQuery(countSql)
                .setParameter("userId", userId).getSingleResult()).longValue();

        String dataSql = "SELECT s.id, s.code, s.name, s.description, s.created_at, s.updated_at, " +
                "d.id as dept_id, d.name as dept_name, d.code as dept_code " +
                baseJoin + " ORDER BY s.created_at DESC LIMIT :limit OFFSET :offset";
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(dataSql)
                .setParameter("userId", userId)
                .setParameter("limit", limitPerPage)
                .setParameter("offset", offset)
                .getResultList();

        List<Map<String, Object>> items = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row[0]);
            item.put("code", row[1]);
            item.put("name", row[2]);
            item.put("description", row[3]);
            item.put("createdAt", row[4]);
            item.put("updatedAt", row[5]);
            item.put("department", Map.of("id", row[6], "name", row[7], "code", row[8]));
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
}
