package ru.javaprac.bank.dao;

import ru.javaprac.bank.entity.*;
import ru.javaprac.bank.entity.Account;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AccountDao {

    public List<Account> findAll() {
        return DaoHelper.inTransaction(session -> {
            List<Number> ids = session
                .createNativeQuery("select id from accounts order by created_at desc")
                .list();
            return loadAccountsByIds(session, ids);
        });
    }

    public Optional<Account> findById(Long id) {
        return DaoHelper.inTransaction(session -> {
            Account a = session.get(Account.class, id);
            return Optional.ofNullable(a);
        });
    }

    public Optional<Account> findBySpecialNumber(String specialNumber) {
        return DaoHelper.inTransaction(session -> {
            Number id = (Number) session
                .createNativeQuery("select id from accounts where special_number = :sn")
                .setParameter("sn", specialNumber)
                .uniqueResult();
            return id == null ? Optional.empty() : Optional.ofNullable(session.get(Account.class, id.longValue()));
        });
    }

    public List<Account> findFiltered(Long clientId, AccountType accountType, AccountStatus status,
                                     Instant createdFrom, Instant createdTo) {
        return DaoHelper.inTransaction(session -> {
            var sql = new StringBuilder("select id from accounts where 1=1");
            if (clientId != null) sql.append(" and client_id = :clientId");
            if (accountType != null) sql.append(" and account_type = :accountType");
            if (status != null) sql.append(" and status = :status");
            if (createdFrom != null) sql.append(" and created_at >= :createdFrom");
            if (createdTo != null) sql.append(" and created_at <= :createdTo");
            sql.append(" order by created_at desc");
            var q = session.createNativeQuery(sql.toString());
            if (clientId != null) q.setParameter("clientId", clientId);
            if (accountType != null) q.setParameter("accountType", accountType.name());
            if (status != null) q.setParameter("status", status.name());
            if (createdFrom != null) q.setParameter("createdFrom", createdFrom);
            if (createdTo != null) q.setParameter("createdTo", createdTo);
            return loadAccountsByIds(session, q.list());
        });
    }

    private static List<Account> loadAccountsByIds(org.hibernate.Session session, List<?> rawIds) {
        List<Account> result = new ArrayList<>(rawIds.size());
        for (Object rawId : rawIds) {
            Long id = ((Number) rawId).longValue();
            Account account = session.get(Account.class, id);
            if (account != null) {
                result.add(account);
            }
        }
        return result;
    }

    public long createCreditAccount(Client client, Currency currency, String specialNumber,
                                   BigDecimal maxCredit, BigDecimal interestRate, PaymentMethod paymentMethod) {
        return DaoHelper.inTransaction(session -> {
            var acc = new CreditDetails();
            acc.setClient(client);
            acc.setCurrency(currency);
            acc.setSpecialNumber(specialNumber);
            acc.setPoints(0);
            acc.setStatus(AccountStatus.active);
            acc.setCreatedAt(Instant.now());
            acc.setUpdatedAt(Instant.now());
            acc.setMaxCredit(maxCredit);
            acc.setCurrentDebt(BigDecimal.ZERO);
            acc.setInterestRate(interestRate);
            acc.setPaymentMethod(paymentMethod);
            session.persist(acc);
            return acc.getId();
        });
    }

    public long createDepositAccount(Client client, Currency currency, String specialNumber,
                                    BigDecimal initialAmount, LocalDate endDate,
                                    boolean automaticRenewal, PaymentMethod paymentMethod) {
        return DaoHelper.inTransaction(session -> {
            var acc = new DepositDetails();
            acc.setClient(client);
            acc.setCurrency(currency);
            acc.setSpecialNumber(specialNumber);
            acc.setPoints(initialAmount.doubleValue());
            acc.setStatus(AccountStatus.active);
            acc.setCreatedAt(Instant.now());
            acc.setUpdatedAt(Instant.now());
            acc.setInitialAmount(initialAmount);
            acc.setEndDate(endDate);
            acc.setAutomaticRenewal(automaticRenewal);
            acc.setPaymentMethod(paymentMethod);
            session.persist(acc);
            return acc.getId();
        });
    }

    public long createSavingAccount(Client client, Currency currency, String specialNumber,
                                   BigDecimal interestRate, BigDecimal maxLimit) {
        return DaoHelper.inTransaction(session -> {
            var acc = new SavingDetails();
            acc.setClient(client);
            acc.setCurrency(currency);
            acc.setSpecialNumber(specialNumber);
            acc.setPoints(0);
            acc.setStatus(AccountStatus.active);
            acc.setCreatedAt(Instant.now());
            acc.setUpdatedAt(Instant.now());
            acc.setInterestRate(interestRate);
            acc.setMaxLimit(maxLimit);
            session.persist(acc);
            return acc.getId();
        });
    }

    public boolean credit(Long accountId, BigDecimal amount, String description) {
        return DaoHelper.inTransaction(session -> {
            Account a = session.get(Account.class, accountId);
            if (a == null || a.getStatus() != AccountStatus.active) return false;
            double newPoints = a.getPoints() + amount.doubleValue();
            a.setPoints(newPoints);
            a.setUpdatedAt(Instant.now());
            var op = new AccountOperation();
            op.setAccount(a);
            op.setKind(OperationKind.credit);
            op.setAmount(amount);
            op.setDescription(description);
            op.setCreatedAt(Instant.now());
            session.persist(op);
            return true;
        });
    }

    public boolean debit(Long accountId, BigDecimal amount, String description) {
        return DaoHelper.inTransaction(session -> {
            Account a = session.get(Account.class, accountId);
            if (a == null || a.getStatus() != AccountStatus.active) return false;
            if (a.getPoints() < amount.doubleValue()) return false;
            double newPoints = a.getPoints() - amount.doubleValue();
            a.setPoints(newPoints);
            a.setUpdatedAt(Instant.now());
            var op = new AccountOperation();
            op.setAccount(a);
            op.setKind(OperationKind.debit);
            op.setAmount(amount);
            op.setDescription(description);
            op.setCreatedAt(Instant.now());
            session.persist(op);
            return true;
        });
    }

    public boolean closeAccount(Long accountId) {
        return DaoHelper.inTransaction(session -> {
            Account a = session.get(Account.class, accountId);
            if (a == null || a.getStatus() == AccountStatus.closed) return false;
            if (a.getPoints() != 0) return false;
            a.setStatus(AccountStatus.closed);
            a.setClosedAt(Instant.now());
            a.setUpdatedAt(Instant.now());
            return true;
        });
    }

    public List<AccountOperation> findOperationsByAccount(Long accountId) {
        return DaoHelper.inTransaction(session ->
            session.createNativeQuery(
                "select * from account_operations where account_id = :accId order by created_at desc",
                AccountOperation.class)
                .setParameter("accId", accountId)
                .list()
        );
    }
}
