package ru.javaprac.bank.dao;

import org.testng.annotations.Test;
import ru.javaprac.bank.dao.StatisticsDao.DepartmentStats;

import java.util.List;

import static org.testng.Assert.*;

public class StatisticsDaoTest extends TestBase {

    private final StatisticsDao dao = new StatisticsDao();

    @Test
    public void getDepartmentStats_returnsNonEmptyList() {
        List<DepartmentStats> stats = dao.getDepartmentStats();
        assertNotNull(stats);
        assertFalse(stats.isEmpty());
        for (DepartmentStats s : stats) {
            assertTrue(s.departmentId() > 0);
            assertNotNull(s.departmentName());
            assertTrue(s.clientCount() >= 0);
            assertTrue(s.totalBalance() >= 0);
        }
    }

    @Test
    public void getDepartmentStats_hasCorrectStructure() {
        List<DepartmentStats> stats = dao.getDepartmentStats();
        assertFalse(stats.isEmpty());
        DepartmentStats first = stats.get(0);
        assertNotNull(first.departmentName());
    }

    @Test
    public void getDepartmentStats_whenQueryReturnsEmptyList_returnsEmptyList() throws Exception {
        StubSessionPlan plan = new StubSessionPlan().addListResult(List.of());

        List<DepartmentStats> stats = withTemporarySessionFactory(
            stubSessionFactory(plan),
            () -> dao.getDepartmentStats()
        );

        assertNotNull(stats);
        assertTrue(stats.isEmpty());
    }

    @Test
    public void getTotalCreditDebt_returnsNonNegative() {
        double debt = dao.getTotalCreditDebt();
        assertTrue(debt >= 0);
    }

    @Test
    public void getTotalDepositAmount_returnsNonNegative() {
        double amount = dao.getTotalDepositAmount();
        assertTrue(amount >= 0);
    }

    @Test
    public void getTotalCreditDebt_whenQueryReturnsNull_returnsZero() throws Exception {
        StubSessionPlan plan = new StubSessionPlan().addUniqueResult(null);

        double debt = withTemporarySessionFactory(
            stubSessionFactory(plan),
            () -> dao.getTotalCreditDebt()
        );

        assertEquals(debt, 0.0);
    }

    @Test
    public void getTotalDepositAmount_whenQueryReturnsNull_returnsZero() throws Exception {
        StubSessionPlan plan = new StubSessionPlan().addUniqueResult(null);

        double amount = withTemporarySessionFactory(
            stubSessionFactory(plan),
            () -> dao.getTotalDepositAmount()
        );

        assertEquals(amount, 0.0);
    }
}
