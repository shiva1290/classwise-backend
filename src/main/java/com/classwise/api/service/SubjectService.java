package com.classwise.api.service;

import com.classwise.api.entity.Department;
import com.classwise.api.entity.Subject;
import com.classwise.api.repository.DepartmentRepository;
import com.classwise.api.repository.SubjectRepository;
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

@Service
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final DepartmentRepository departmentRepository;
    private final EntityManager em;

    public SubjectService(SubjectRepository subjectRepository, DepartmentRepository departmentRepository,
            EntityManager em) {
        this.subjectRepository = subjectRepository;
        this.departmentRepository = departmentRepository;
        this.em = em;
    }

    public Map<String, Object> listSubjects(String search, String departmentFilter, int page, int limit) {
        int currentPage = Math.max(1, page);
        int limitPerPage = Math.max(1, limit);
        int offset = (currentPage - 1) * limitPerPage;

        StringBuilder whereSql = new StringBuilder(" WHERE 1=1 ");
        if (search != null && !search.trim().isEmpty()) {
            whereSql.append(" AND (LOWER(s.name) LIKE LOWER(:search) OR LOWER(s.code) LIKE LOWER(:search)) ");
        }
        if (departmentFilter != null && !departmentFilter.trim().isEmpty()) {
            whereSql.append(" AND d.name = :department ");
        }

        String countSql = "SELECT COUNT(*) FROM subjects s JOIN departments d ON s.department_id = d.id " + whereSql;
        Query countQuery = em.createNativeQuery(countSql);
        if (search != null && !search.trim().isEmpty()) {
            countQuery.setParameter("search", "%" + search + "%");
        }
        if (departmentFilter != null && !departmentFilter.trim().isEmpty()) {
            countQuery.setParameter("department", departmentFilter);
        }
        long totalItems = ((Number) countQuery.getSingleResult()).longValue();

        String dataSql = "SELECT s.id, s.code, s.name, s.description, s.created_at, s.updated_at, " +
                "d.id as dept_id, d.name as dept_name, d.code as dept_code, " +
                "(SELECT COUNT(*) FROM classes c WHERE c.subject_id = s.id) as total_classes " +
                "FROM subjects s JOIN departments d ON s.department_id = d.id " + whereSql +
                " ORDER BY s.created_at DESC LIMIT :limit OFFSET :offset";
        Query dataQuery = em.createNativeQuery(dataSql);
        if (search != null && !search.trim().isEmpty()) {
            dataQuery.setParameter("search", "%" + search + "%");
        }
        if (departmentFilter != null && !departmentFilter.trim().isEmpty()) {
            dataQuery.setParameter("department", departmentFilter);
        }
        dataQuery.setParameter("limit", limitPerPage);
        dataQuery.setParameter("offset", offset);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = dataQuery.getResultList();
        List<Map<String, Object>> items = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row[0]);
            item.put("code", row[1]);
            item.put("name", row[2]);
            item.put("description", row[3]);
            item.put("createdAt", row[4]);
            item.put("updatedAt", row[5]);
            item.put("department", Map.of(
                    "id", row[6],
                    "name", row[7],
                    "code", row[8]));
            item.put("totalClasses", ((Number) row[9]).longValue());
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
    public Map<String, Object> createSubject(Integer departmentId, String name, String code, String description) {
        Optional<Department> deptOpt = departmentRepository.findById(departmentId);
        if (deptOpt.isEmpty()) {
            throw new IllegalArgumentException("Department not found");
        }

        Subject subject = new Subject();
        subject.setDepartment(deptOpt.get());
        subject.setName(name);
        subject.setCode(code);
        subject.setDescription(description);
        Instant now = Instant.now();
        subject.setCreatedAt(now);
        subject.setUpdatedAt(now);
        subjectRepository.save(subject);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", subject.getId());
        result.put("code", subject.getCode());
        result.put("name", subject.getName());
        result.put("description", subject.getDescription());
        result.put("departmentId", departmentId);
        result.put("createdAt", subject.getCreatedAt());
        result.put("updatedAt", subject.getUpdatedAt());
        return result;
    }

    public Map<String, Object> getSubjectDetails(Integer id) {
        Optional<Subject> subjectOpt = subjectRepository.findById(id);
        if (subjectOpt.isEmpty())
            return null;

        String sql = "SELECT s.id, s.code, s.name, s.description, s.created_at, s.updated_at, " +
                "d.id as dept_id, d.name as dept_name, d.code as dept_code, " +
                "(SELECT COUNT(*) FROM classes c WHERE c.subject_id = s.id) as total_classes " +
                "FROM subjects s JOIN departments d ON s.department_id = d.id WHERE s.id = :id";
        Object[] row = (Object[]) em.createNativeQuery(sql).setParameter("id", id).getSingleResult();

        Map<String, Object> subject = new LinkedHashMap<>();
        subject.put("id", row[0]);
        subject.put("code", row[1]);
        subject.put("name", row[2]);
        subject.put("description", row[3]);
        subject.put("createdAt", row[4]);
        subject.put("updatedAt", row[5]);
        subject.put("department", Map.of("id", row[6], "name", row[7], "code", row[8]));

        Map<String, Object> totals = Map.of("classes", ((Number) row[9]).longValue());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("subject", subject);
        result.put("totals", totals);
        return result;
    }

    public Map<String, Object> getSubjectClasses(Integer subjectId, int page, int limit) {
        int currentPage = Math.max(1, page);
        int limitPerPage = Math.max(1, limit);
        int offset = (currentPage - 1) * limitPerPage;

        String countSql = "SELECT COUNT(*) FROM classes WHERE subject_id = :subId";
        long totalItems = ((Number) em.createNativeQuery(countSql)
                .setParameter("subId", subjectId).getSingleResult()).longValue();

        String dataSql = "SELECT c.id, c.name, c.invite_code, c.capacity, c.status, c.banner_url, " +
                "c.banner_cld_pub_id, c.description, c.schedules, c.created_at, c.updated_at, " +
                "u.id as teacher_id, u.name as teacher_name, u.image as teacher_image, " +
                "(SELECT COUNT(*) FROM enrollments e WHERE e.class_id = c.id) as student_count " +
                "FROM classes c JOIN \"user\" u ON c.teacher_id = u.id " +
                "WHERE c.subject_id = :subId ORDER BY c.created_at DESC LIMIT :limit OFFSET :offset";
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(dataSql)
                .setParameter("subId", subjectId)
                .setParameter("limit", limitPerPage)
                .setParameter("offset", offset)
                .getResultList();

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
            Map<String, Object> teacher = new LinkedHashMap<>();
            teacher.put("id", row[11]);
            teacher.put("name", row[12]);
            teacher.put("image", row[13]);
            item.put("teacher", teacher);
            item.put("studentCount", ((Number) row[14]).longValue());
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

    public Map<String, Object> getSubjectUsers(Integer subjectId, String role, int page, int limit) {
        int currentPage = Math.max(1, page);
        int limitPerPage = Math.max(1, limit);
        int offset = (currentPage - 1) * limitPerPage;

        String baseJoin;
        if ("teacher".equals(role)) {
            baseJoin = "FROM \"user\" u JOIN classes c ON c.teacher_id = u.id WHERE c.subject_id = :subId AND u.role = 'teacher'";
        } else {
            baseJoin = "FROM \"user\" u JOIN enrollments e ON e.student_id = u.id JOIN classes c ON e.class_id = c.id WHERE c.subject_id = :subId AND u.role = 'student'";
        }

        String countSql = "SELECT COUNT(DISTINCT u.id) " + baseJoin;
        long totalItems = ((Number) em.createNativeQuery(countSql)
                .setParameter("subId", subjectId).getSingleResult()).longValue();

        String dataSql = "SELECT DISTINCT u.id, u.name, u.email, u.image, u.role, u.created_at " +
                baseJoin + " ORDER BY u.created_at DESC LIMIT :limit OFFSET :offset";
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(dataSql)
                .setParameter("subId", subjectId)
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
}
