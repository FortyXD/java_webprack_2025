package ru.javaprac.bank.dao;

import org.testng.annotations.Test;
import ru.javaprac.bank.entity.Account;
import ru.javaprac.bank.entity.AccountStatus;
import ru.javaprac.bank.entity.Client;
import ru.javaprac.bank.entity.ClientType;
import ru.javaprac.bank.entity.Currency;
import ru.javaprac.bank.entity.Department;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.testng.Assert.*;

public class ClientDaoTest extends TestBase {

    private final ClientDao dao = new ClientDao();
    private final DepartmentDao deptDao = new DepartmentDao();
    private final AccountDao accountDao = new AccountDao();
    private final CurrencyDao currencyDao = new CurrencyDao();

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
    public void update_modifiesClient() {
        Department dep = deptDao.findAll().get(0);
        Client c = new Client();
        c.setDepartment(dep);
        c.setType(ClientType.NATURAL_PERSON);
        c.setSurname("UpdClient" + System.currentTimeMillis());
        c.setPassport("UPD-" + System.currentTimeMillis());
        c.setPhone("+7-000-001");
        dao.save(c);

        c.setPhone("+7-000-999");
        dao.update(c);

        Optional<Client> found = dao.findById(c.getId());
        assertTrue(found.isPresent());
        assertEquals(found.get().getPhone(), "+7-000-999");
        dao.deleteIfNoOpenAccountsOrZeroBalance(c.getId());
    }

    @Test
    public void findFiltered_bySearch_returnsMatchingClient() {
        Department dep = deptDao.findAll().get(0);
        String marker = "Searchable" + System.currentTimeMillis();
        Client c = new Client();
        c.setDepartment(dep);
        c.setType(ClientType.NATURAL_PERSON);
        c.setSurname(marker);
        c.setPassport("SRC-" + System.currentTimeMillis());
        dao.save(c);

        List<Client> filtered = dao.findFiltered(null, null, marker.substring(0, 8));
        assertTrue(filtered.stream().anyMatch(x -> x.getId().equals(c.getId())));

        dao.deleteIfNoOpenAccountsOrZeroBalance(c.getId());
    }

    @Test
    public void findFiltered_withAllFilters_returnsCreatedClient() {
        Department dep = deptDao.findAll().get(0);
        String marker = "Combo" + System.currentTimeMillis();
        Client c = new Client();
        c.setDepartment(dep);
        c.setType(ClientType.NATURAL_PERSON);
        c.setSurname(marker);
        c.setPassport("CMB-" + System.currentTimeMillis());
        dao.save(c);

        List<Client> filtered = dao.findFiltered(dep.getId(), ClientType.NATURAL_PERSON, marker);
        assertTrue(filtered.stream().anyMatch(x -> x.getId().equals(c.getId())));

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

    @Test
    public void deleteIfNoOpenAccountsOrZeroBalance_whenNoOpenButNonZeroBalance_returnsFalse() {
        Department dep = deptDao.findAll().get(0);
        Currency curr = currencyDao.findByName("RUB").orElseThrow();

        Client c = new Client();
        c.setDepartment(dep);
        c.setType(ClientType.NATURAL_PERSON);
        c.setSurname("SuspClient" + System.currentTimeMillis());
        c.setPassport("SUS-" + System.currentTimeMillis());
        dao.save(c);

        long accId = accountDao.createSavingAccount(
            c, curr, "ACC-SUSP-" + System.currentTimeMillis(),
            BigDecimal.valueOf(3), BigDecimal.valueOf(1000)
        );
        assertTrue(accountDao.credit(accId, BigDecimal.valueOf(100), "Setup non-zero balance"));

        DaoHelper.runInTransaction(session -> {
            Account a = session.get(Account.class, accId);
            a.setStatus(AccountStatus.suspended);
            a.setUpdatedAt(java.time.Instant.now());
        });

        boolean deleted = dao.deleteIfNoOpenAccountsOrZeroBalance(c.getId());
        assertFalse(deleted, "Non-zero balance must block deletion even without active accounts");
    }

    @Test
    public void deleteIfNoOpenAccountsOrZeroBalance_whenClientNotFound_returnsFalse() {
        assertFalse(dao.deleteIfNoOpenAccountsOrZeroBalance(999999L));
    }
}
