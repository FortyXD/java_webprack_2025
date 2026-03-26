package ru.javaprac.bank.dao;

import org.testng.annotations.Test;
import ru.javaprac.bank.entity.Department;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.testng.Assert.*;

public class DepartmentDaoTest extends TestBase {

    private final DepartmentDao dao = new DepartmentDao();

    @Test
    public void findAll_returnsNonEmptyList() {
        List<Department> list = dao.findAll();
        assertNotNull(list);
        assertFalse(list.isEmpty(), "Expected at least one department from seed data");
        for (Department d : list) {
            assertNotNull(d.getId());
            assertNotNull(d.getName());
        }
    }

    @Test
    public void findById_whenExists_returnsDepartment() {
        List<Department> all = dao.findAll();
        assertFalse(all.isEmpty());
        Long id = all.get(0).getId();
        Optional<Department> opt = dao.findById(id);
        assertTrue(opt.isPresent());
        Department d = opt.get();
        assertEquals(d.getId(), id);
        assertEquals(d.getName(), all.get(0).getName());
        assertEquals(d.getAddress(), all.get(0).getAddress());
    }

    @Test
    public void findById_whenNotExists_returnsEmpty() {
        Optional<Department> opt = dao.findById(999999L);
        assertFalse(opt.isPresent());
    }

    @Test
    public void searchByNameOrAddress_findsMatching() {
        List<Department> list = dao.searchByNameOrAddress("Central");
        assertNotNull(list);
        assertFalse(list.isEmpty());
        assertTrue(list.stream().anyMatch(d ->
            d.getName() != null && d.getName().toLowerCase().contains("central")));
    }

    @Test
    public void searchByNameOrAddress_emptyQuery_returnsAll() {
        List<Department> all = dao.findAll();
        List<Department> search = dao.searchByNameOrAddress("");
        assertEquals(search.size(), all.size());
    }

    @Test
    public void searchByNameOrAddress_nullQuery_returnsAll() {
        List<Department> all = dao.findAll();
        List<Department> search = dao.searchByNameOrAddress(null);
        assertEquals(search.size(), all.size());
    }

    @Test
    public void searchByNameOrAddress_whenNoMatches_returnsEmptyList() {
        List<Department> search = dao.searchByNameOrAddress("NoSuchDepartment-" + System.nanoTime());
        assertNotNull(search);
        assertTrue(search.isEmpty());
    }

    @Test
    public void searchByNameOrAddress_trimmedQuery_matchesAddressCaseInsensitively() {
        Department d = new Department();
        d.setName("Address Search " + System.currentTimeMillis());
        d.setAddress("Marker Avenue " + System.currentTimeMillis());
        dao.save(d);

        List<Department> search = dao.searchByNameOrAddress("  marker avenue  ");
        assertTrue(search.stream().anyMatch(dep -> dep.getId().equals(d.getId())));

        dao.deleteIfNoClients(d.getId());
    }

    @Test
    public void countClients_returnsCorrectCount() {
        List<Department> all = dao.findAll();
        assertFalse(all.isEmpty());
        long cnt = dao.countClients(all.get(0).getId());
        assertTrue(cnt >= 0);
    }

    @Test
    public void countClients_whenNoClients_returnsZero() {
        Department d = new Department();
        d.setName("Empty Dept " + System.currentTimeMillis());
        d.setAddress("Nowhere");
        dao.save(d);
        assertEquals(dao.countClients(d.getId()), 0L);
        dao.deleteIfNoClients(d.getId());
    }

    @Test
    public void sumBalanceByDepartment_returnsNumber() {
        List<Department> all = dao.findAll();
        assertFalse(all.isEmpty());
        Double sum = dao.sumBalanceByDepartment(all.get(0).getId());
        assertNotNull(sum);
        assertTrue(sum >= 0);
    }

    @Test
    public void sumBalanceByDepartment_whenNoAccounts_returnsZero() {
        Department d = new Department();
        d.setName("No Accounts Dept " + System.currentTimeMillis());
        d.setAddress("Void");
        dao.save(d);
        assertEquals(dao.sumBalanceByDepartment(d.getId()), 0.0);
        dao.deleteIfNoClients(d.getId());
    }

    @Test
    public void save_persistsNewDepartment() {
        Department d = new Department();
        d.setName("Test Branch " + System.currentTimeMillis());
        d.setAddress("Test st. 1");
        d.setPhoneNumber("+1-111-111");
        dao.save(d);
        assertNotNull(d.getId());
        Optional<Department> found = dao.findById(d.getId());
        assertTrue(found.isPresent());
        assertEquals(found.get().getName(), d.getName());
        dao.deleteIfNoClients(d.getId());
    }

    @Test
    public void update_modifiesDepartment() {
        Department d = new Department();
        d.setName("Update Test " + System.currentTimeMillis());
        d.setAddress("Original");
        dao.save(d);
        d.setAddress("Updated Address");
        dao.update(d);
        Optional<Department> found = dao.findById(d.getId());
        assertTrue(found.isPresent());
        assertEquals(found.get().getAddress(), "Updated Address");
        dao.deleteIfNoClients(d.getId());
    }

    @Test
    public void deleteIfNoClients_whenNoClients_returnsTrue() {
        Department d = new Department();
        d.setName("Delete Test " + System.currentTimeMillis());
        dao.save(d);
        boolean deleted = dao.deleteIfNoClients(d.getId());
        assertTrue(deleted);
        assertFalse(dao.findById(d.getId()).isPresent());
    }

    @Test
    public void deleteIfNoClients_whenHasClients_returnsFalse() {
        List<Department> all = dao.findAll();
        for (Department d : all) {
            if (dao.countClients(d.getId()) > 0) {
                boolean deleted = dao.deleteIfNoClients(d.getId());
                assertFalse(deleted);
                break;
            }
        }
    }

    @Test
    public void deleteIfNoClients_whenDepartmentNotFound_returnsFalse() {
        assertFalse(dao.deleteIfNoClients(999999L));
    }

    @Test
    public void countClients_whenQueryReturnsNull_returnsZero() throws Exception {
        StubSessionPlan plan = new StubSessionPlan().addUniqueResult(null);

        long count = withTemporarySessionFactory(
            stubSessionFactory(plan),
            () -> dao.countClients(1L)
        );

        assertEquals(count, 0L);
    }

    @Test
    public void sumBalanceByDepartment_whenQueryReturnsNull_returnsZero() throws Exception {
        StubSessionPlan plan = new StubSessionPlan().addUniqueResult(null);

        double sum = withTemporarySessionFactory(
            stubSessionFactory(plan),
            () -> dao.sumBalanceByDepartment(1L)
        );

        assertEquals(sum, 0.0);
    }

    @Test
    public void deleteIfNoClients_whenCountQueryReturnsNull_stillDeletesExistingDepartment() throws Exception {
        Department d = new Department();
        d.setId(321L);
        StubSessionPlan plan = new StubSessionPlan()
            .addUniqueResult(null)
            .withEntity(Department.class, d);

        boolean deleted = withTemporarySessionFactory(
            stubSessionFactory(plan),
            () -> dao.deleteIfNoClients(d.getId())
        );

        assertTrue(deleted);
        assertSame(plan.removedEntity(), d);
    }
}
