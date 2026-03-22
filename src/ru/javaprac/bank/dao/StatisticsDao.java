package ru.javaprac.bank.dao;

import java.util.List;

public class StatisticsDao {

    public record DepartmentStats(long departmentId, String departmentName, long clientCount, double totalBalance) {}

    public List<DepartmentStats> getDepartmentStats() {
        return DaoHelper.inTransaction(session -> {
            List<Object[]> rows = session.createNativeQuery(
                "select d.id, d.name, count(distinct c.id), coalesce(sum(a.points), 0) " +
                    "from departments d " +
                    "left join clients c on c.department_id = d.id " +
                    "left join accounts a on a.client_id = c.id " +
                    "group by d.id, d.name " +
                    "order by d.name")
                .list();
            return rows.stream()
                .map(row -> new DepartmentStats(
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    ((Number) row[2]).longValue(),
                    ((Number) row[3]).doubleValue()))
                .toList();
        });
    }

    public double getTotalCreditDebt() {
        Number n = DaoHelper.inTransaction(session ->
            (Number) session.createNativeQuery(
                "select coalesce(sum(current_dept), 0) from credit")
                .uniqueResult()
        );
        return n == null ? 0 : n.doubleValue();
    }

    public double getTotalDepositAmount() {
        Number n = DaoHelper.inTransaction(session ->
            (Number) session.createNativeQuery(
                "select coalesce(sum(initial_amount), 0) from deposit")
                .uniqueResult()
        );
        return n == null ? 0 : n.doubleValue();
    }
}
