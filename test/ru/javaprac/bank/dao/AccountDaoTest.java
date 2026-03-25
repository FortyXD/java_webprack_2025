package ru.javaprac.bank.dao;

import org.testng.annotations.Test;
import ru.javaprac.bank.entity.*;
import ru.javaprac.bank.entity.Account;

import ru.javaprac.bank.util.HibernateUtil;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.testng.Assert.*;

public class AccountDaoTest extends TestBase {

    private final AccountDao dao = new AccountDao();
    private final ClientDao clientDao = new ClientDao();
    private final CurrencyDao currencyDao = new CurrencyDao();

    @Test
    public void findAll_returnsNonEmptyList() {
        List<Account> list = dao.findAll();
        assertNotNull(list);
        assertFalse(list.isEmpty());
    }

    @Test
    public void findById_whenExists_returnsAccount() {
        List<Account> all = dao.findAll();
        assertFalse(all.isEmpty());
        Optional<Account> opt = dao.findById(all.get(0).getId());
        assertTrue(opt.isPresent());
        assertEquals(opt.get().getSpecialNumber(), all.get(0).getSpecialNumber());
    }

    @Test
    public void findById_whenNotExists_returnsEmpty() {
        assertFalse(dao.findById(999999L).isPresent());
    }

    @Test
    public void findBySpecialNumber_whenExists_returnsAccount() {
        List<Account> all = dao.findAll();
        assertFalse(all.isEmpty());
        String num = all.get(0).getSpecialNumber();
        Optional<Account> opt = dao.findBySpecialNumber(num);
        assertTrue(opt.isPresent());
        assertEquals(opt.get().getSpecialNumber(), num);
    }

    @Test
    public void findBySpecialNumber_whenNotExists_returnsEmpty() {
        assertFalse(dao.findBySpecialNumber("NON-EXISTENT").isPresent());
    }

    @Test
    public void findFiltered_byClient_returnsMatching() {
        List<Client> clients = clientDao.findAll();
        assertFalse(clients.isEmpty());
        List<Account> filtered = dao.findFiltered(clients.get(0).getId(), null, null, null, null);
        assertNotNull(filtered);
        filtered.forEach(a -> assertEquals(a.getClient().getId(), clients.get(0).getId()));
    }

    @Test
    public void findFiltered_byStatus_returnsMatching() {
        List<Account> active = dao.findFiltered(null, null, AccountStatus.active, null, null);
        assertNotNull(active);
        active.forEach(a -> assertEquals(a.getStatus(), AccountStatus.active));
    }

    @Test
    public void findFiltered_allNullFilters_returnsSameAsFindAll() {
        List<Account> all = dao.findAll();
        List<Account> filtered = dao.findFiltered(null, null, null, null, null);
        assertEquals(filtered.size(), all.size());
    }

    @Test
    public void loadAccountsByIds_skipsMissingAccountRows() throws Exception {
        Method m = AccountDao.class.getDeclaredMethod(
            "loadAccountsByIds", org.hibernate.Session.class, List.class);
        m.setAccessible(true);
        try (var session = HibernateUtil.getSessionFactory().openSession()) {
            @SuppressWarnings("unchecked")
            List<Account> list = (List<Account>) m.invoke(null, session, List.of(999_999_999L));
            assertNotNull(list);
            assertTrue(list.isEmpty());
        }
    }

    @Test
    public void credit_whenAccountExists_returnsTrueAndUpdatesBalance() {
        Optional<Account> acc = dao.findBySpecialNumber("ACC-0003");
        assertTrue(acc.isPresent());
        double before = acc.get().getPoints();
        boolean ok = dao.credit(acc.get().getId(), BigDecimal.valueOf(10), "Test credit");
        assertTrue(ok);
        Optional<Account> after = dao.findById(acc.get().getId());
        assertTrue(after.isPresent());
        assertEquals(after.get().getPoints(), before + 10, 0.001);
        dao.debit(after.get().getId(), BigDecimal.valueOf(10), "Revert test");
    }

    @Test
    public void credit_whenAccountNotExists_returnsFalse() {
        assertFalse(dao.credit(999999L, BigDecimal.ONE, "Test"));
    }

    @Test
    public void debit_whenSufficientFunds_returnsTrue() {
        Optional<Account> acc = dao.findBySpecialNumber("ACC-0003");
        assertTrue(acc.isPresent());
        double before = acc.get().getPoints();
        if (before < 1) {
            dao.credit(acc.get().getId(), BigDecimal.valueOf(10), "Setup");
            before = 10;
        }
        boolean ok = dao.debit(acc.get().getId(), BigDecimal.ONE, "Test debit");
        assertTrue(ok);
        Optional<Account> after = dao.findById(acc.get().getId());
        assertTrue(after.isPresent());
        assertEquals(after.get().getPoints(), before - 1, 0.001);
        dao.credit(after.get().getId(), BigDecimal.ONE, "Revert");
    }

    @Test
    public void debit_whenInsufficientFunds_returnsFalse() {
        Optional<Account> acc = dao.findBySpecialNumber("ACC-0004");
        assertTrue(acc.isPresent());
        double before = acc.get().getPoints();
        boolean ok = dao.debit(acc.get().getId(), BigDecimal.valueOf(999999), "Impossible");
        assertFalse(ok);
        Optional<Account> after = dao.findById(acc.get().getId());
        assertTrue(after.isPresent());
        assertEquals(after.get().getPoints(), before, 0.001);
    }

    @Test
    public void closeAccount_whenBalanceZero_returnsTrue() {
        Client client = clientDao.findAll().get(0);
        Currency curr = currencyDao.findByName("RUB").orElseThrow();
        long id = dao.createSavingAccount(client, curr, "ACC-CLOSE-" + System.currentTimeMillis(),
            BigDecimal.valueOf(5), BigDecimal.valueOf(1000));
        Optional<Account> a = dao.findById(id);
        assertTrue(a.isPresent());
        assertEquals(a.get().getPoints(), 0, 0.001);
        boolean closed = dao.closeAccount(id);
        assertTrue(closed);
        Optional<Account> after = dao.findById(id);
        assertTrue(after.isPresent());
        assertEquals(after.get().getStatus(), AccountStatus.closed);
    }

    @Test
    public void closeAccount_whenBalanceNonZero_returnsFalse() {
        Optional<Account> acc = dao.findBySpecialNumber("ACC-0003");
        assertTrue(acc.isPresent());
        if (acc.get().getPoints() == 0) {
            dao.credit(acc.get().getId(), BigDecimal.ONE, "Setup");
        }
        boolean closed = dao.closeAccount(acc.get().getId());
        assertFalse(closed);
    }

    @Test
    public void findOperationsByAccount_returnsOperations() {
        Optional<Account> acc = dao.findBySpecialNumber("ACC-0001");
        assertTrue(acc.isPresent());
        List<AccountOperation> ops = dao.findOperationsByAccount(acc.get().getId());
        assertNotNull(ops);
        assertFalse(ops.isEmpty(), "ACC-0001 should have operations from seed");
        ops.forEach(op -> {
            assertNotNull(op.getId());
            assertNotNull(op.getKind());
            assertNotNull(op.getAmount());
            assertEquals(op.getAccount().getId(), acc.get().getId());
        });
    }

    @Test
    public void createCreditAccount_persistsAccount() {
        Client client = clientDao.findAll().get(0);
        Currency curr = currencyDao.findByName("RUB").orElseThrow();
        long id = dao.createCreditAccount(client, curr, "ACC-NEW-CR-" + System.currentTimeMillis(),
            BigDecimal.valueOf(100000), BigDecimal.valueOf(12), PaymentMethod.manual);
        assertTrue(id > 0);
        Optional<Account> a = dao.findById(id);
        assertTrue(a.isPresent());
        assertTrue(a.get() instanceof CreditDetails);
        assertEquals(((CreditDetails) a.get()).getMaxCredit().doubleValue(), 100000, 0.01);
    }

    @Test
    public void createDepositAccount_persistsAccount() {
        Client client = clientDao.findAll().get(0);
        Currency curr = currencyDao.findByName("RUB").orElseThrow();
        long id = dao.createDepositAccount(client, curr, "ACC-NEW-DP-" + System.currentTimeMillis(),
            BigDecimal.valueOf(5000), LocalDate.now().plusDays(90), false, PaymentMethod.auto);
        assertTrue(id > 0);
        Optional<Account> a = dao.findById(id);
        assertTrue(a.isPresent());
        assertTrue(a.get() instanceof DepositDetails);
        assertEquals(((DepositDetails) a.get()).getInitialAmount().doubleValue(), 5000, 0.01);
    }

    @Test
    public void createSavingAccount_persistsAccount() {
        Client client = clientDao.findAll().get(0);
        Currency curr = currencyDao.findByName("RUB").orElseThrow();
        long id = dao.createSavingAccount(client, curr, "ACC-NEW-SV-" + System.currentTimeMillis(),
            BigDecimal.valueOf(4.5), BigDecimal.valueOf(30000));
        assertTrue(id > 0);
        Optional<Account> a = dao.findById(id);
        assertTrue(a.isPresent());
        assertTrue(a.get() instanceof SavingDetails);
        assertEquals(((SavingDetails) a.get()).getInterestRate().doubleValue(), 4.5, 0.01);
    }

    @Test
    public void credit_whenAccountClosed_returnsFalse() {
        Client client = clientDao.findAll().get(0);
        Currency curr = currencyDao.findByName("RUB").orElseThrow();
        long id = dao.createSavingAccount(client, curr, "ACC-CR-CLOSED-" + System.currentTimeMillis(),
            BigDecimal.valueOf(4), BigDecimal.valueOf(1000));
        assertTrue(dao.closeAccount(id), "Setup failed: account should be closable");
        assertFalse(dao.credit(id, BigDecimal.ONE, "Must fail for closed account"));
    }

    @Test
    public void debit_whenAccountNotExists_returnsFalse() {
        assertFalse(dao.debit(999999L, BigDecimal.ONE, "No account"));
    }

    @Test
    public void debit_whenAccountClosed_returnsFalse() {
        Client client = clientDao.findAll().get(0);
        Currency curr = currencyDao.findByName("RUB").orElseThrow();
        long id = dao.createSavingAccount(client, curr, "ACC-DB-CLOSED-" + System.currentTimeMillis(),
            BigDecimal.valueOf(3), BigDecimal.valueOf(500));
        assertTrue(dao.closeAccount(id), "Setup failed: account should be closable");
        assertFalse(dao.debit(id, BigDecimal.ONE, "Must fail for closed account"));
    }

    @Test
    public void closeAccount_whenNotExists_returnsFalse() {
        assertFalse(dao.closeAccount(999999L));
    }

    @Test
    public void closeAccount_whenAlreadyClosed_returnsFalse() {
        Client client = clientDao.findAll().get(0);
        Currency curr = currencyDao.findByName("RUB").orElseThrow();
        long id = dao.createSavingAccount(client, curr, "ACC-ALREADY-CLOSED-" + System.currentTimeMillis(),
            BigDecimal.valueOf(5), BigDecimal.valueOf(5000));
        assertTrue(dao.closeAccount(id), "First close should succeed");
        assertFalse(dao.closeAccount(id), "Second close must fail");
    }

    @Test
    public void findOperationsByAccount_whenNoOperations_returnsEmptyList() {
        Client client = clientDao.findAll().get(0);
        Currency curr = currencyDao.findByName("RUB").orElseThrow();
        long id = dao.createSavingAccount(client, curr, "ACC-NO-OPS-" + System.currentTimeMillis(),
            BigDecimal.valueOf(2), BigDecimal.valueOf(10000));
        List<AccountOperation> ops = dao.findOperationsByAccount(id);
        assertNotNull(ops);
        assertTrue(ops.isEmpty(), "Fresh account should have no operations");
    }

    @Test
    public void findFiltered_withAllFilters_returnsCreatedAccount() {
        Client client = clientDao.findAll().get(0);
        Currency curr = currencyDao.findByName("RUB").orElseThrow();
        long id = dao.createSavingAccount(client, curr, "ACC-FILTER-ALL-" + System.currentTimeMillis(),
            BigDecimal.valueOf(6), BigDecimal.valueOf(15000));
        Instant now = Instant.now();
        List<Account> filtered = dao.findFiltered(
            client.getId(),
            AccountType.saving,
            AccountStatus.active,
            now.minusSeconds(60),
            now.plusSeconds(60)
        );
        assertTrue(filtered.stream().anyMatch(a -> a.getId().equals(id)));
    }
}
