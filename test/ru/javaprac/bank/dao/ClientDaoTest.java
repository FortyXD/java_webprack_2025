package ru.javaprac.bank.dao;

import org.testng.annotations.Test;
import ru.javaprac.bank.entity.Client;
import ru.javaprac.bank.entity.ClientType;
import ru.javaprac.bank.entity.Department;

import java.util.List;
import java.util.Optional;

import static org.testng.Assert.*;

public class ClientDaoTest extends TestBase {

    private final ClientDao dao = new ClientDao();
    private final DepartmentDao deptDao = new DepartmentDao();

    @Test
    public void findAll_returnsNonEmptyList() {
        List<Client> list = dao.findAll();
        assertNotNull(list);
        assertFalse(list.isEmpty());
    }

    @Test
    public void findById_whenExists_returnsClient() {
        List<Client> all = dao.findAll();
        assertFalse(all.isEmpty());
        Long id = all.get(0).getId();
        Optional<Client> opt = dao.findById(id);
        assertTrue(opt.isPresent());
        Client c = opt.get();
        assertEquals(c.getId(), id);
        assertNotNull(c.getDepartment());
    }

    @Test
    public void findById_whenNotExists_returnsEmpty() {
        assertFalse(dao.findById(999999L).isPresent());
    }

    @Test
    public void findFiltered_byDepartment_returnsMatching() {
        List<Department> depts = deptDao.findAll();
        assertFalse(depts.isEmpty());
        List<Client> filtered = dao.findFiltered(depts.get(0).getId(), null, null);
        assertNotNull(filtered);
        filtered.forEach(c -> assertEquals(c.getDepartment().getId(), depts.get(0).getId()));
    }

    @Test
    public void findFiltered_byType_returnsMatching() {
        List<Client> natural = dao.findFiltered(null, ClientType.NATURAL_PERSON, null);
        assertNotNull(natural);
        natural.forEach(c -> assertEquals(c.getType(), ClientType.NATURAL_PERSON));
    }

    @Test
    public void save_persistsNewClient() {
        List<Department> depts = deptDao.findAll();
        assertFalse(depts.isEmpty());
        Client c = new Client();
        c.setDepartment(depts.get(0));
        c.setType(ClientType.NATURAL_PERSON);
        c.setSurname("TestSurname" + System.currentTimeMillis());
        c.setSecondName("TestName");
        c.setPassport("9999 999999");
        c.setPhone("+0-000-000");
        dao.save(c);
        assertNotNull(c.getId());
        Optional<Client> found = dao.findById(c.getId());
        assertTrue(found.isPresent());
        assertEquals(found.get().getSurname(), c.getSurname());
        dao.deleteIfNoOpenAccountsOrZeroBalance(c.getId());
    }

    @Test
    public void deleteIfNoOpenAccountsOrZeroBalance_whenNoAccounts_returnsTrue() {
        List<Department> depts = deptDao.findAll();
        Client c = new Client();
        c.setDepartment(depts.get(0));
        c.setType(ClientType.NATURAL_PERSON);
        c.setSurname("DelTest" + System.currentTimeMillis());
        c.setPassport("8888 888888");
        dao.save(c);
        boolean deleted = dao.deleteIfNoOpenAccountsOrZeroBalance(c.getId());
        assertTrue(deleted);
        assertFalse(dao.findById(c.getId()).isPresent());
    }

    @Test
    public void deleteIfNoOpenAccountsOrZeroBalance_whenHasAccounts_returnsFalse() {
        List<Client> all = dao.findAll();
        assertFalse(all.isEmpty());
        Client withAccounts = all.get(0);
        boolean deleted = dao.deleteIfNoOpenAccountsOrZeroBalance(withAccounts.getId());
        assertFalse(deleted, "Client with accounts must not be deletable");
    }
}
