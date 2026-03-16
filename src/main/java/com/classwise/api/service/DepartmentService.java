package com.classwise.api.service;

import com.classwise.api.entity.Department;
import com.classwise.api.repository.DepartmentRepository;
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
public class DepartmentService {

        private final DepartmentRepository departmentRepository;
        private final EntityManager em;

        public DepartmentService(DepartmentRepository departmentRepository, EntityManager em) {
                this.departmentRepository = departmentRepository;
                this.em = em;
        }

        public Map<String, Object> listDepartments(String search, int page, int limit) {
                int currentPage = Math.max(1, page);
                int limitPerPage = Math.max(1, limit);
                int offset = (currentPage - 1) * limitPerPage;

                StringBuilder whereSql = new StringBuilder(" WHERE 1=1 ");
                if (search != null && !search.trim().isEmpty()) {
                        whereSql.append(" AND (LOWER(d.name) LIKE LOWER(:search) OR LOWER(d.code) LIKE LOWER(:search)) ");
                }

                String countSql = "SELECT COUNT(*) FROM departments d " + whereSql;
                Query countQuery = em.createNativeQuery(countSql);
                if (search != null && !search.trim().isEmpty()) {
                        countQuery.setParameter("search", "%" + search + "%");
                }
                long totalItems = ((Number) countQuery.getSingleResult()).longValue();

                String dataSql = "SELECT d.id, d.code, d.name, d.description, d.created_at, d.updated_at, " +
                                "(SELECT COUNT(*) FROM subjects s WHERE s.department_id = d.id) as total_subjects " +
                                "FROM departments d " + whereSql +
                                " ORDER BY d.created_at DESC LIMIT :limit OFFSET :offset";
                Query dataQuery = em.createNativeQuery(dataSql);
                if (search != null && !search.trim().isEmpty()) {
                        dataQuery.setParameter("search", "%" + search + "%");
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
                        item.put("totalSubjects", ((Number) row[6]).longValue());
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
        public Map<String, Object> createDepartment(String code, String name, String description) {
                Department dept = new Department();
                dept.setCode(code);
                dept.setName(name);
                dept.setDescription(description);
                Instant now = Instant.now();
                dept.setCreatedAt(now);
                dept.setUpdatedAt(now);
                departmentRepository.save(dept);

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("id", dept.getId());
                result.put("code", dept.getCode());
                result.put("name", dept.getName());
                result.put("description", dept.getDescription());
                result.put("createdAt", dept.getCreatedAt());
                result.put("updatedAt", dept.getUpdatedAt());
                return result;
        }

        public Map<String, Object> getDepartmentDetails(Integer id) {
                Optional<Department> deptOpt = departmentRepository.findById(id);
                if (deptOpt.isEmpty())
                        return null;

                Department dept = deptOpt.get();

                String sql = "SELECT " +
                                "(SELECT COUNT(*) FROM subjects WHERE department_id = :id) as total_subjects, " +
                                "(SELECT COUNT(*) FROM classes c JOIN subjects s ON c.subject_id = s.id WHERE s.department_id = :id) as total_classes, "
                                +
                                "(SELECT COUNT(DISTINCT e.student_id) FROM enrollments e JOIN classes c ON e.class_id = c.id JOIN subjects s ON c.subject_id = s.id WHERE s.department_id = :id) as enrolled_students";
                Query query = em.createNativeQuery(sql);
                query.setParameter("id", id);
                Object[] totals = (Object[]) query.getSingleResult();

                Map<String, Object> department = new LinkedHashMap<>();
                department.put("id", dept.getId());
                department.put("code", dept.getCode());
                department.put("name", dept.getName());
                department.put("description", dept.getDescription());
                department.put("createdAt", dept.getCreatedAt());
                department.put("updatedAt", dept.getUpdatedAt());

                Map<String, Object> totalsMap = new LinkedHashMap<>();
                totalsMap.put("subjects", ((Number) totals[0]).longValue());
                totalsMap.put("classes", ((Number) totals[1]).longValue());
                totalsMap.put("enrolledStudents", ((Number) totals[2]).longValue());

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("department", department);
                result.put("totals", totalsMap);
                return result;
        }

        public Map<String, Object> getDepartmentSubjects(Integer departmentId, int page, int limit) {
                int currentPage = Math.max(1, page);
                int limitPerPage = Math.max(1, limit);
                int offset = (currentPage - 1) * limitPerPage;

                String countSql = "SELECT COUNT(*) FROM subjects WHERE department_id = :deptId";
                long totalItems = ((Number) em.createNativeQuery(countSql)
                                .setParameter("deptId", departmentId).getSingleResult()).longValue();

                String dataSql = "SELECT s.id, s.code, s.name, s.description, s.created_at, s.updated_at, " +
                                "(SELECT COUNT(*) FROM classes c WHERE c.subject_id = s.id) as total_classes " +
                                "FROM subjects s WHERE s.department_id = :deptId ORDER BY s.created_at DESC LIMIT :limit OFFSET :offset";
                @SuppressWarnings("unchecked")
                List<Object[]> rows = em.createNativeQuery(dataSql)
                                .setParameter("deptId", departmentId)
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
                        item.put("totalClasses", ((Number) row[6]).longValue());
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

        public Map<String, Object> getDepartmentClasses(Integer departmentId, int page, int limit) {
                int currentPage = Math.max(1, page);
                int limitPerPage = Math.max(1, limit);
                int offset = (currentPage - 1) * limitPerPage;

                String countSql = "SELECT COUNT(*) FROM classes c JOIN subjects s ON c.subject_id = s.id WHERE s.department_id = :deptId";
                long totalItems = ((Number) em.createNativeQuery(countSql)
                                .setParameter("deptId", departmentId).getSingleResult()).longValue();

                String dataSql = "SELECT c.id, c.name, c.invite_code, c.capacity, c.status, c.banner_url, " +
                                "c.banner_cld_pub_id, c.description, c.schedules, c.created_at, c.updated_at, " +
                                "s.id as subject_id, s.name as subject_name, s.code as subject_code, " +
                                "u.id as teacher_id, u.name as teacher_name, u.image as teacher_image, " +
                                "(SELECT COUNT(*) FROM enrollments e WHERE e.class_id = c.id) as student_count " +
                                "FROM classes c " +
                                "JOIN subjects s ON c.subject_id = s.id " +
                                "JOIN \"user\" u ON c.teacher_id = u.id " +
                                "WHERE s.department_id = :deptId " +
                                "ORDER BY c.created_at DESC LIMIT :limit OFFSET :offset";
                @SuppressWarnings("unchecked")
                List<Object[]> rows = em.createNativeQuery(dataSql)
                                .setParameter("deptId", departmentId)
                                .setParameter("limit", limitPerPage)
                                .setParameter("offset", offset)
                                .getResultList();

                List<Map<String, Object>> items = new ArrayList<>();
                for (Object[] row : rows) {
                        items.add(buildClassItem(row));
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

        public Map<String, Object> getDepartmentUsers(Integer departmentId, String role, int page, int limit) {
                int currentPage = Math.max(1, page);
                int limitPerPage = Math.max(1, limit);
                int offset = (currentPage - 1) * limitPerPage;

                String baseJoin;
                if ("teacher".equals(role)) {
                        baseJoin = "FROM \"user\" u " +
                                        "JOIN classes c ON c.teacher_id = u.id " +
                                        "JOIN subjects s ON c.subject_id = s.id " +
                                        "WHERE s.department_id = :deptId AND u.role = 'teacher'";
                } else {
                        baseJoin = "FROM \"user\" u " +
                                        "JOIN enrollments e ON e.student_id = u.id " +
                                        "JOIN classes c ON e.class_id = c.id " +
                                        "JOIN subjects s ON c.subject_id = s.id " +
                                        "WHERE s.department_id = :deptId AND u.role = 'student'";
                }

                String countSql = "SELECT COUNT(DISTINCT u.id) " + baseJoin;
                long totalItems = ((Number) em.createNativeQuery(countSql)
                                .setParameter("deptId", departmentId).getSingleResult()).longValue();

                String dataSql = "SELECT DISTINCT u.id, u.name, u.email, u.image, u.role, u.created_at " +
                                baseJoin + " ORDER BY u.created_at DESC LIMIT :limit OFFSET :offset";
                @SuppressWarnings("unchecked")
                List<Object[]> rows = em.createNativeQuery(dataSql)
                                .setParameter("deptId", departmentId)
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

        private Map<String, Object> buildClassItem(Object[] row) {
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
                Map<String, Object> teacher = new LinkedHashMap<>();
                teacher.put("id", row[14]);
                teacher.put("name", row[15]);
                teacher.put("image", row[16]);
                item.put("teacher", teacher);
                item.put("studentCount", ((Number) row[17]).longValue());
                return item;
        }
}
