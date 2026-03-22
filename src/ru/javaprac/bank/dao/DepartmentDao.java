package ru.javaprac.bank.dao;

import ru.javaprac.bank.entity.Department;

import java.util.List;
import java.util.Optional;

public class DepartmentDao {

    public List<Department> findAll() {
        return DaoHelper.inTransaction(session ->
            session.createNativeQuery("select * from departments order by name", Department.class).list()
        );
    }

    public Optional<Department> findById(Long id) {
        return DaoHelper.inTransaction(session -> {
            Department d = session.get(Department.class, id);
            return Optional.ofNullable(d);
        });
    }

    public List<Department> searchByNameOrAddress(String query) {
        if (query == null || query.isBlank()) {
            return findAll();
        }
        String pattern = "%" + query.trim().toLowerCase() + "%";
        return DaoHelper.inTransaction(session ->
            session.createNativeQuery(
                "select * from departments " +
                    "where lower(name) like :q or lower(coalesce(address,'')) like :q " +
                    "order by name",
                Department.class)
                .setParameter("q", pattern)
                .list()
        );
    }

    public long countClients(Long departmentId) {
        Number n = DaoHelper.inTransaction(session ->
            (Number) session.createNativeQuery(
                "select count(*) from clients where department_id = :depId")
                .setParameter("depId", departmentId)
                .uniqueResult()
        );
        return n == null ? 0 : n.longValue();
    }

    public Double sumBalanceByDepartment(Long departmentId) {
        Number n = DaoHelper.inTransaction(session ->
            (Number) session.createNativeQuery(
                "select coalesce(sum(a.points), 0) " +
                    "from accounts a join clients c on c.id = a.client_id " +
                    "where c.department_id = :depId")
                .setParameter("depId", departmentId)
                .uniqueResult()
        );
        return n == null ? 0d : n.doubleValue();
    }

    public void save(Department department) {
        DaoHelper.runInTransaction(s -> s.persist(department));
    }

    public Department update(Department department) {
        return DaoHelper.inTransaction(session -> {
            session.merge(department);
            return department;
        });
    }

    public boolean deleteIfNoClients(Long departmentId) {
        return DaoHelper.inTransaction(session -> {
            Number cnt = (Number) session.createNativeQuery(
                "select count(*) from clients where department_id = :depId")
                .setParameter("depId", departmentId)
                .uniqueResult();
            if (cnt != null && cnt.longValue() > 0) {
                return false;
            }
            Department d = session.get(Department.class, departmentId);
            if (d != null) {
                session.remove(d);
                return true;
            }
            return false;
        });
    }
}
