package ru.javaprac.bank.dao;

import org.testng.annotations.Test;
import ru.javaprac.bank.entity.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.testng.Assert.*;

public class EntityCoverageTest {

    @Test
    public void department_roundTripsAllFields() {
        Department department = new Department();
        List<Client> clients = List.of(new Client());

        department.setId(10L);
        department.setName("Central");
        department.setAddress("Main st.");
        department.setPhoneNumber("+7-999");
        department.setEmail("central@example.com");
        department.setLongitude(new BigDecimal("37.617300"));
        department.setLatitude(new BigDecimal("55.755800"));
        department.setClients(clients);

        assertEquals(department.getId(), Long.valueOf(10));
        assertEquals(department.getName(), "Central");
        assertEquals(department.getAddress(), "Main st.");
        assertEquals(department.getPhoneNumber(), "+7-999");
        assertEquals(department.getEmail(), "central@example.com");
        assertEquals(department.getLongitude(), new BigDecimal("37.617300"));
        assertEquals(department.getLatitude(), new BigDecimal("55.755800"));
        assertSame(department.getClients(), clients);
    }

    @Test
    public void client_roundTripsAllFields() {
        Department department = new Department();
        List<Account> accounts = List.of(new SavingDetails());
        Client client = new Client();

        client.setId(11L);
        client.setDepartment(department);
        client.setType(ClientType.LEGAL_ENTITY);
        client.setSurname("Ivanov");
        client.setSecondName("Ivan");
        client.setPassport("1234 567890");
        client.setSnils("123-456-789 00");
        client.setPhone("+7-111");
        client.setAddress("Lenina 1");
        client.setLinkToPhoto("https://example.com/photo.jpg");
        client.setAccounts(accounts);

        assertEquals(client.getId(), Long.valueOf(11));
        assertSame(client.getDepartment(), department);
        assertEquals(client.getType(), ClientType.LEGAL_ENTITY);
        assertEquals(client.getSurname(), "Ivanov");
        assertEquals(client.getSecondName(), "Ivan");
        assertEquals(client.getPassport(), "1234 567890");
        assertEquals(client.getSnils(), "123-456-789 00");
        assertEquals(client.getPhone(), "+7-111");
        assertEquals(client.getAddress(), "Lenina 1");
        assertEquals(client.getLinkToPhoto(), "https://example.com/photo.jpg");
        assertSame(client.getAccounts(), accounts);
    }

    @Test
    public void currency_roundTripsAllFields() {
        Currency currency = new Currency();
        List<Account> accounts = List.of(new SavingDetails());

        currency.setId(12L);
        currency.setName("EUR");
        currency.setAccounts(accounts);

        assertEquals(currency.getId(), Long.valueOf(12));
        assertEquals(currency.getName(), "EUR");
        assertSame(currency.getAccounts(), accounts);
    }

    @Test
    public void savingAccount_roundTripsInheritedAndOwnFields() {
        Department department = new Department();
        Client client = new Client();
        Currency currency = new Currency();
        AccountOperation operation = new AccountOperation();
        List<AccountOperation> operations = List.of(operation);
        Instant createdAt = Instant.parse("2026-03-26T10:15:30Z");
        Instant updatedAt = Instant.parse("2026-03-26T11:15:30Z");
        Instant closedAt = Instant.parse("2026-03-26T12:15:30Z");

        client.setDepartment(department);

        SavingDetails account = new SavingDetails();
        account.setId(13L);
        account.setClient(client);
        account.setSpecialNumber("ACC-TEST-001");
        account.setPoints(123.45);
        account.setCurrency(currency);
        account.setStatus(AccountStatus.suspended);
        account.setCreatedAt(createdAt);
        account.setUpdatedAt(updatedAt);
        account.setClosedAt(closedAt);
        account.setOperations(operations);
        account.setInterestRate(new BigDecimal("4.50"));
        account.setMaxLimit(new BigDecimal("15000.00"));

        assertEquals(account.getId(), Long.valueOf(13));
        assertSame(account.getClient(), client);
        assertEquals(account.getSpecialNumber(), "ACC-TEST-001");
        assertEquals(account.getPoints(), 123.45, 0.0001);
        assertSame(account.getCurrency(), currency);
        assertEquals(account.getStatus(), AccountStatus.suspended);
        assertEquals(account.getCreatedAt(), createdAt);
        assertEquals(account.getUpdatedAt(), updatedAt);
        assertEquals(account.getClosedAt(), closedAt);
        assertSame(account.getOperations(), operations);
        assertEquals(account.getInterestRate(), new BigDecimal("4.50"));
        assertEquals(account.getMaxLimit(), new BigDecimal("15000.00"));
    }

    @Test
    public void creditDetails_roundTripsOwnFields() {
        CreditDetails credit = new CreditDetails();

        credit.setMaxCredit(new BigDecimal("200000.00"));
        credit.setCurrentDebt(new BigDecimal("1500.25"));
        credit.setInterestRate(new BigDecimal("12.75"));
        credit.setPaymentMethod(PaymentMethod.manual);

        assertEquals(credit.getMaxCredit(), new BigDecimal("200000.00"));
        assertEquals(credit.getCurrentDebt(), new BigDecimal("1500.25"));
        assertEquals(credit.getInterestRate(), new BigDecimal("12.75"));
        assertEquals(credit.getPaymentMethod(), PaymentMethod.manual);
    }

    @Test
    public void depositDetails_roundTripsOwnFields() {
        DepositDetails deposit = new DepositDetails();
        LocalDate endDate = LocalDate.of(2026, 12, 31);

        deposit.setInitialAmount(new BigDecimal("5000.00"));
        deposit.setEndDate(endDate);
        deposit.setAutomaticRenewal(true);
        deposit.setPaymentMethod(PaymentMethod.auto);

        assertEquals(deposit.getInitialAmount(), new BigDecimal("5000.00"));
        assertEquals(deposit.getEndDate(), endDate);
        assertTrue(deposit.isAutomaticRenewal());
        assertEquals(deposit.getPaymentMethod(), PaymentMethod.auto);
    }

    @Test
    public void accountOperation_roundTripsAllFields() {
        AccountOperation operation = new AccountOperation();
        Account account = new SavingDetails();
        Instant createdAt = Instant.parse("2026-03-26T13:15:30Z");

        operation.setId(14L);
        operation.setAccount(account);
        operation.setKind(OperationKind.credit);
        operation.setAmount(new BigDecimal("99.99"));
        operation.setDescription("Manual top-up");
        operation.setCreatedAt(createdAt);

        assertEquals(operation.getId(), Long.valueOf(14));
        assertSame(operation.getAccount(), account);
        assertEquals(operation.getKind(), OperationKind.credit);
        assertEquals(operation.getAmount(), new BigDecimal("99.99"));
        assertEquals(operation.getDescription(), "Manual top-up");
        assertEquals(operation.getCreatedAt(), createdAt);
    }

    @Test
    public void clientType_and_converter_cover_valid_null_and_invalid_values() {
        ClientTypeConverter converter = new ClientTypeConverter();

        assertEquals(ClientType.LEGAL_ENTITY.getDbValue(), "legal entity");
        assertEquals(ClientType.NATURAL_PERSON.getDbValue(), "natural person");
        assertEquals(ClientType.fromDb("legal entity"), ClientType.LEGAL_ENTITY);
        assertEquals(ClientType.fromDb("natural person"), ClientType.NATURAL_PERSON);
        assertNull(converter.convertToDatabaseColumn(null));
        assertEquals(converter.convertToDatabaseColumn(ClientType.NATURAL_PERSON), "natural person");
        assertNull(converter.convertToEntityAttribute(null));
        assertEquals(converter.convertToEntityAttribute("legal entity"), ClientType.LEGAL_ENTITY);

        IllegalArgumentException ex = expectThrows(
            IllegalArgumentException.class,
            () -> ClientType.fromDb("unsupported")
        );
        assertTrue(ex.getMessage().contains("Unknown client_type"));
    }

    @Test
    public void enums_exposeExpectedConstants() {
        assertEquals(AccountStatus.valueOf("active"), AccountStatus.active);
        assertEquals(AccountStatus.valueOf("closed"), AccountStatus.closed);
        assertEquals(AccountStatus.valueOf("suspended"), AccountStatus.suspended);

        assertEquals(AccountType.valueOf("credit"), AccountType.credit);
        assertEquals(AccountType.valueOf("deposit"), AccountType.deposit);
        assertEquals(AccountType.valueOf("saving"), AccountType.saving);

        assertEquals(OperationKind.valueOf("credit"), OperationKind.credit);
        assertEquals(OperationKind.valueOf("debit"), OperationKind.debit);

        assertEquals(PaymentMethod.valueOf("auto"), PaymentMethod.auto);
        assertEquals(PaymentMethod.valueOf("manual"), PaymentMethod.manual);
    }
}
