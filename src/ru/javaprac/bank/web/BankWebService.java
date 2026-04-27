package ru.javaprac.bank.web;

import org.hibernate.exception.ConstraintViolationException;
import ru.javaprac.bank.dao.AccountDao;
import ru.javaprac.bank.dao.ClientDao;
import ru.javaprac.bank.dao.DaoHelper;
import ru.javaprac.bank.dao.DepartmentDao;
import ru.javaprac.bank.entity.Account;
import ru.javaprac.bank.entity.AccountOperation;
import ru.javaprac.bank.entity.AccountStatus;
import ru.javaprac.bank.entity.AccountType;
import ru.javaprac.bank.entity.Client;
import ru.javaprac.bank.entity.ClientType;
import ru.javaprac.bank.entity.CreditDetails;
import ru.javaprac.bank.entity.Currency;
import ru.javaprac.bank.entity.Department;
import ru.javaprac.bank.entity.DepositDetails;
import ru.javaprac.bank.entity.OperationKind;
import ru.javaprac.bank.entity.PaymentMethod;
import ru.javaprac.bank.entity.SavingDetails;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BankWebService {

    private final DepartmentDao departmentDao = new DepartmentDao();
    private final ClientDao clientDao = new ClientDao();
    private final AccountDao accountDao = new AccountDao();

    public Map<String, Object> dashboard() {
        return DaoHelper.inTransaction(session -> {
            Map<String, Object> result = ok("Сводка загружена");
            result.put("totals", mapOf(
                    "departmentCount", scalarLong(session.createNativeQuery("select count(*) from departments").uniqueResult()),
                    "clientCount", scalarLong(session.createNativeQuery("select count(*) from clients").uniqueResult()),
                    "accountCount", scalarLong(session.createNativeQuery("select count(*) from accounts").uniqueResult()),
                    "totalBalance", scalarDouble(session.createNativeQuery("select coalesce(sum(points), 0) from accounts").uniqueResult()),
                    "activeAccounts", scalarLong(session.createNativeQuery("select count(*) from accounts where status = 'active'").uniqueResult()),
                    "creditDebt", scalarDouble(session.createNativeQuery("select coalesce(sum(current_dept), 0) from credit").uniqueResult()),
                    "depositAmount", scalarDouble(session.createNativeQuery("select coalesce(sum(initial_amount), 0) from deposit").uniqueResult())
            ));
            result.put("departments", departmentRows(null));
            result.put("accountTypes", accountTypeSummary());
            return result;
        });
    }

    public Map<String, Object> referenceData() {
        return DaoHelper.inTransaction(session -> {
            Map<String, Object> result = ok("Справочники загружены");
            result.put("departments", departmentRows(null));
            result.put("clients", clientRows(null, null, null));
            result.put("currencies", session
                    .createNativeQuery("select id, name from currencys order by name")
                    .list()
                    .stream()
                    .map(row -> {
                        Object[] r = (Object[]) row;
                        return mapOf("id", scalarLong(r[0]), "name", r[1]);
                    })
                    .toList());
            result.put("clientTypes", List.of(
                    mapOf("value", ClientType.NATURAL_PERSON.name(), "label", "Физическое лицо"),
                    mapOf("value", ClientType.LEGAL_ENTITY.name(), "label", "Юридическое лицо")
            ));
            result.put("accountTypes", List.of(
                    mapOf("value", AccountType.saving.name(), "label", "Накопительный"),
                    mapOf("value", AccountType.deposit.name(), "label", "Вклад"),
                    mapOf("value", AccountType.credit.name(), "label", "Кредит")
            ));
            result.put("accountStatuses", List.of(
                    mapOf("value", AccountStatus.active.name(), "label", "Открыт"),
                    mapOf("value", AccountStatus.closed.name(), "label", "Закрыт"),
                    mapOf("value", AccountStatus.suspended.name(), "label", "Приостановлен")
            ));
            result.put("paymentMethods", List.of(
                    mapOf("value", PaymentMethod.manual.name(), "label", "Ручной"),
                    mapOf("value", PaymentMethod.auto.name(), "label", "Авто")
            ));
            return result;
        });
    }

    public Map<String, Object> departments(String search) {
        Map<String, Object> result = ok("Отделения загружены");
        result.put("departments", departmentRows(search));
        return result;
    }

    public Map<String, Object> department(Long id) {
        return DaoHelper.inTransaction(session -> {
            Object[] row = singleRow(session.createNativeQuery(
                    "select d.id, d.name, d.address, d.phone_number, d.email, d.longitude, d.latitude, " +
                            "count(distinct c.id), coalesce(sum(a.points), 0) " +
                            "from departments d " +
                            "left join clients c on c.department_id = d.id " +
                            "left join accounts a on a.client_id = c.id " +
                            "where d.id = :id " +
                            "group by d.id, d.name, d.address, d.phone_number, d.email, d.longitude, d.latitude")
                    .setParameter("id", id)
                    .uniqueResult());
            if (row == null) {
                return fail("Отделение не найдено");
            }
            Map<String, Object> result = ok("Отделение загружено");
            result.put("department", mapOf(
                    "id", scalarLong(row[0]),
                    "name", row[1],
                    "address", row[2],
                    "phoneNumber", row[3],
                    "email", row[4],
                    "longitude", row[5],
                    "latitude", row[6],
                    "clientCount", scalarLong(row[7]),
                    "totalBalance", scalarDouble(row[8])
            ));
            result.put("clients", clientRows(id, null, null));
            result.put("accounts", accountsForDepartment(id));
            return result;
        });
    }

    public Map<String, Object> saveDepartment(Map<String, String> params) {
        String name = clean(params.get("name"));
        if (name == null) {
            return fail("Название отделения обязательно.");
        }

        try {
            return DaoHelper.inTransaction(session -> {
                Long id = parseLong(params.get("id"));
                Department department = id == null ? new Department() : session.get(Department.class, id);
                if (department == null) {
                    return fail("Отделение не найдено.");
                }

                department.setName(name);
                department.setAddress(clean(params.get("address")));
                department.setPhoneNumber(clean(params.get("phoneNumber")));
                department.setEmail(clean(params.get("email")));
                department.setLongitude(parseBigDecimal(params.get("longitude"), true, "Долгота"));
                department.setLatitude(parseBigDecimal(params.get("latitude"), true, "Широта"));

                if (id == null) {
                    session.persist(department);
                }
                Map<String, Object> result = ok(id == null ? "Отделение добавлено." : "Отделение обновлено.");
                result.put("id", department.getId());
                return result;
            });
        } catch (IllegalArgumentException e) {
            return fail(e.getMessage());
        }
    }

    public Map<String, Object> deleteDepartment(Long id) {
        boolean deleted = departmentDao.deleteIfNoClients(id);
        return deleted
                ? ok("Отделение удалено.")
                : fail("Нельзя удалить отделение: к нему прикреплены клиенты.");
    }

    public Map<String, Object> clients(Long departmentId, String typeValue, String search) {
        Map<String, Object> result = ok("Клиенты загружены");
        result.put("clients", clientRows(departmentId, parseClientTypeDb(typeValue), search));
        result.put("departments", departmentRows(null));
        return result;
    }

    public Map<String, Object> client(Long id) {
        return DaoHelper.inTransaction(session -> {
            Object[] row = singleRow(session.createNativeQuery(
                    "select c.id, c.department_id, d.name, c.type, c.surname, c.second_name, c.passport, " +
                            "c.snils, c.phone, c.address, c.link_to_photo, count(a.id), coalesce(sum(a.points), 0) " +
                            "from clients c " +
                            "join departments d on d.id = c.department_id " +
                            "left join accounts a on a.client_id = c.id " +
                            "where c.id = :id " +
                            "group by c.id, c.department_id, d.name, c.type, c.surname, c.second_name, " +
                            "c.passport, c.snils, c.phone, c.address, c.link_to_photo")
                    .setParameter("id", id)
                    .uniqueResult());
            if (row == null) {
                return fail("Клиент не найден.");
            }
            Map<String, Object> result = ok("Клиент загружен");
            result.put("client", mapClientDetail(row));
            result.put("accounts", accountRows(id, null, null, null, null));
            result.put("operations", operationsForClient(id));
            return result;
        });
    }

    public Map<String, Object> saveClient(Map<String, String> params) {
        String name = clean(params.get("surname"));
        Long departmentId = parseLong(params.get("departmentId"));
        ClientType type = parseClientType(params.get("type"));
        if (departmentId == null) {
            return fail("Выберите отделение клиента.");
        }
        if (type == null) {
            return fail("Выберите тип клиента.");
        }
        if (name == null) {
            return fail(type == ClientType.LEGAL_ENTITY ? "Название организации обязательно." : "Фамилия клиента обязательна.");
        }

        try {
            return DaoHelper.inTransaction(session -> {
                Department department = session.get(Department.class, departmentId);
                if (department == null) {
                    return fail("Выбранное отделение не найдено.");
                }

                Long id = parseLong(params.get("id"));
                Client client = id == null ? new Client() : session.get(Client.class, id);
                if (client == null) {
                    return fail("Клиент не найден.");
                }

                client.setDepartment(department);
                client.setType(type);
                client.setSurname(name);
                client.setSecondName(clean(params.get("secondName")));
                client.setPassport(clean(params.get("passport")));
                client.setSnils(clean(params.get("snils")));
                client.setPhone(clean(params.get("phone")));
                client.setAddress(clean(params.get("address")));
                client.setLinkToPhoto(clean(params.get("linkToPhoto")));

                if (id == null) {
                    session.persist(client);
                }
                Map<String, Object> result = ok(id == null ? "Клиент добавлен." : "Клиент обновлен.");
                result.put("id", client.getId());
                return result;
            });
        } catch (ConstraintViolationException e) {
            return fail("Паспорт или СНИЛС уже используются другим клиентом.");
        } catch (RuntimeException e) {
            if (isConstraintViolation(e)) {
                return fail("Паспорт или СНИЛС уже используются другим клиентом.");
            }
            throw e;
        }
    }

    public Map<String, Object> deleteClient(Long id) {
        boolean deleted = clientDao.deleteIfNoOpenAccountsOrZeroBalance(id);
        return deleted
                ? ok("Клиент удален.")
                : fail("Нельзя удалить клиента: у него есть открытые счета или ненулевой баланс.");
    }

    public Map<String, Object> accounts(Long clientId, String accountType, String status,
                                        String createdFrom, String createdTo) {
        Map<String, Object> result = ok("Счета загружены");
        result.put("accounts", accountRows(clientId, parseAccountType(accountType), parseAccountStatus(status),
                parseDateStart(createdFrom), parseDateEnd(createdTo)));
        result.put("clients", clientRows(null, null, null));
        return result;
    }

    public Map<String, Object> account(Long id) {
        return DaoHelper.inTransaction(session -> {
            Object[] row = accountBaseRow(id);
            if (row == null) {
                return fail("Счет не найден.");
            }
            Map<String, Object> account = mapAccount(row);
            account.put("details", accountSpecificDetails(id, String.valueOf(account.get("accountType"))));
            Map<String, Object> result = ok("Счет загружен");
            result.put("account", account);
            result.put("operations", operationsForAccount(id));
            return result;
        });
    }

    public Map<String, Object> saveAccount(Map<String, String> params) {
        Long clientId = parseLong(params.get("clientId"));
        Long currencyId = parseLong(params.get("currencyId"));
        AccountType type = parseAccountType(params.get("accountType"));
        String specialNumber = clean(params.get("specialNumber"));

        if (clientId == null) {
            return fail("Выберите клиента.");
        }
        if (currencyId == null) {
            return fail("Выберите валюту.");
        }
        if (type == null) {
            return fail("Выберите тип счета.");
        }
        if (specialNumber == null) {
            return fail("Номер счета обязателен.");
        }

        try {
            return DaoHelper.inTransaction(session -> {
                Client client = session.get(Client.class, clientId);
                Currency currency = session.get(Currency.class, currencyId);
                if (client == null) {
                    return fail("Клиент не найден.");
                }
                if (currency == null) {
                    return fail("Валюта не найдена.");
                }

                Account account;
                switch (type) {
                    case credit -> {
                        CreditDetails credit = new CreditDetails();
                        credit.setMaxCredit(parseRequiredMoney(params.get("maxCredit"), "Кредитный лимит"));
                        credit.setCurrentDebt(parseMoneyOrZero(params.get("currentDebt")));
                        credit.setInterestRate(parseRequiredMoney(params.get("interestRate"), "Процентная ставка"));
                        credit.setPaymentMethod(parsePaymentMethod(params.get("paymentMethod")));
                        account = credit;
                    }
                    case deposit -> {
                        DepositDetails deposit = new DepositDetails();
                        BigDecimal initialAmount = parseMoneyOrZero(params.get("initialAmount"));
                        LocalDate endDate = parseLocalDate(params.get("endDate"));
                        if (endDate == null) {
                            throw new IllegalArgumentException("Укажите дату окончания вклада.");
                        }
                        deposit.setInitialAmount(initialAmount);
                        deposit.setEndDate(endDate);
                        deposit.setAutomaticRenewal("true".equalsIgnoreCase(params.get("automaticRenewal")));
                        deposit.setPaymentMethod(parsePaymentMethod(params.get("paymentMethod")));
                        deposit.setPoints(initialAmount.doubleValue());
                        account = deposit;
                    }
                    case saving -> {
                        SavingDetails saving = new SavingDetails();
                        saving.setInterestRate(parseRequiredMoney(params.get("interestRate"), "Процентная ставка"));
                        saving.setMaxLimit(parseRequiredMoney(params.get("maxLimit"), "Максимальный лимит"));
                        account = saving;
                    }
                    default -> throw new IllegalArgumentException("Неподдерживаемый тип счета.");
                }

                Instant now = Instant.now();
                account.setClient(client);
                account.setCurrency(currency);
                account.setSpecialNumber(specialNumber);
                if (!(account instanceof DepositDetails)) {
                    account.setPoints(0);
                }
                account.setStatus(AccountStatus.active);
                account.setCreatedAt(now);
                account.setUpdatedAt(now);
                session.persist(account);

                Map<String, Object> result = ok("Счет открыт.");
                result.put("id", account.getId());
                return result;
            });
        } catch (IllegalArgumentException e) {
            return fail(e.getMessage());
        } catch (RuntimeException e) {
            if (isConstraintViolation(e)) {
                return fail("Счет с таким номером уже существует.");
            }
            throw e;
        }
    }

    public Map<String, Object> performOperation(Long accountId, Map<String, String> params) {
        OperationKind kind = parseOperationKind(params.get("kind"));
        BigDecimal amount;
        try {
            amount = parseRequiredMoney(params.get("amount"), "Сумма операции");
        } catch (IllegalArgumentException e) {
            return fail(e.getMessage());
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return fail("Сумма операции должна быть больше нуля.");
        }

        return DaoHelper.inTransaction(session -> {
            Account account = session.get(Account.class, accountId);
            if (account == null) {
                return fail("Счет не найден.");
            }
            if (account.getStatus() != AccountStatus.active) {
                return fail("Операции доступны только для открытого счета.");
            }
            if (kind == OperationKind.debit && account.getPoints() < amount.doubleValue()) {
                return fail("Недостаточно средств для списания.");
            }

            double delta = kind == OperationKind.credit ? amount.doubleValue() : -amount.doubleValue();
            account.setPoints(account.getPoints() + delta);
            account.setUpdatedAt(Instant.now());

            AccountOperation operation = new AccountOperation();
            operation.setAccount(account);
            operation.setKind(kind);
            operation.setAmount(amount);
            operation.setDescription(clean(params.get("description")));
            operation.setCreatedAt(Instant.now());
            session.persist(operation);

            Map<String, Object> result = ok(kind == OperationKind.credit ? "Начисление выполнено." : "Списание выполнено.");
            result.put("id", operation.getId());
            result.put("balance", account.getPoints());
            return result;
        });
    }

    public Map<String, Object> closeAccount(Long accountId) {
        return DaoHelper.inTransaction(session -> {
            Account account = session.get(Account.class, accountId);
            if (account == null) {
                return fail("Счет не найден.");
            }
            if (account.getStatus() == AccountStatus.closed) {
                return fail("Счет уже закрыт.");
            }
            if (account.getPoints() != 0) {
                return fail("Нельзя закрыть счет с ненулевым балансом.");
            }
            account.setStatus(AccountStatus.closed);
            account.setClosedAt(Instant.now());
            account.setUpdatedAt(Instant.now());
            return ok("Счет закрыт.");
        });
    }

    public List<Map<String, Object>> departmentRows(String search) {
        return DaoHelper.inTransaction(session -> {
            StringBuilder sql = new StringBuilder(
                    "select d.id, d.name, d.address, d.phone_number, d.email, " +
                            "count(distinct c.id), coalesce(sum(a.points), 0) " +
                            "from departments d " +
                            "left join clients c on c.department_id = d.id " +
                            "left join accounts a on a.client_id = c.id ");
            if (clean(search) != null) {
                sql.append("where lower(d.name) like :q or lower(coalesce(d.address, '')) like :q ");
            }
            sql.append("group by d.id, d.name, d.address, d.phone_number, d.email order by d.name");
            var query = session.createNativeQuery(sql.toString());
            if (clean(search) != null) {
                query.setParameter("q", "%" + search.trim().toLowerCase(Locale.ROOT) + "%");
            }
            List<Object[]> rows = query.list();
            return rows.stream().map(row -> mapOf(
                    "id", scalarLong(row[0]),
                    "name", row[1],
                    "address", row[2],
                    "phoneNumber", row[3],
                    "email", row[4],
                    "clientCount", scalarLong(row[5]),
                    "totalBalance", scalarDouble(row[6])
            )).toList();
        });
    }

    private List<Map<String, Object>> clientRows(Long departmentId, String typeDbValue, String search) {
        StringBuilder sql = new StringBuilder(
                "select c.id, c.department_id, d.name, c.type, c.surname, c.second_name, c.phone, " +
                        "count(a.id), coalesce(sum(a.points), 0) " +
                        "from clients c " +
                        "join departments d on d.id = c.department_id " +
                        "left join accounts a on a.client_id = c.id " +
                        "where 1=1 ");
        if (departmentId != null) {
            sql.append("and c.department_id = :departmentId ");
        }
        if (typeDbValue != null) {
            sql.append("and c.type = :type ");
        }
        if (clean(search) != null) {
            sql.append("and (lower(coalesce(c.surname, '')) like :search " +
                    "or lower(coalesce(c.second_name, '')) like :search " +
                    "or lower(coalesce(c.phone, '')) like :search) ");
        }
        sql.append("group by c.id, c.department_id, d.name, c.type, c.surname, c.second_name, c.phone ");
        sql.append("order by c.surname, c.second_name");

        return DaoHelper.inTransaction(session -> {
            var query = session.createNativeQuery(sql.toString());
            if (departmentId != null) {
                query.setParameter("departmentId", departmentId);
            }
            if (typeDbValue != null) {
                query.setParameter("type", typeDbValue);
            }
            if (clean(search) != null) {
                query.setParameter("search", "%" + search.trim().toLowerCase(Locale.ROOT) + "%");
            }
            List<Object[]> rows = query.list();
            return rows.stream().map(row -> mapOf(
                    "id", scalarLong(row[0]),
                    "departmentId", scalarLong(row[1]),
                    "departmentName", row[2],
                    "type", row[3],
                    "typeLabel", clientTypeLabel(String.valueOf(row[3])),
                    "name", clientDisplayName(row[4], row[5]),
                    "surname", row[4],
                    "secondName", row[5],
                    "phone", row[6],
                    "accountCount", scalarLong(row[7]),
                    "totalBalance", scalarDouble(row[8])
            )).toList();
        });
    }

    private List<Map<String, Object>> accountRows(Long clientId, AccountType accountType, AccountStatus status,
                                                  Instant createdFrom, Instant createdTo) {
        StringBuilder sql = new StringBuilder(
                "select a.id, a.special_number, a.points, a.status, a.account_type, a.created_at, a.closed_at, " +
                        "c.id, c.surname, c.second_name, d.name, cur.name " +
                        "from accounts a " +
                        "join clients c on c.id = a.client_id " +
                        "join departments d on d.id = c.department_id " +
                        "join currencys cur on cur.id = a.currency_id " +
                        "where 1=1 ");
        if (clientId != null) {
            sql.append("and c.id = :clientId ");
        }
        if (accountType != null) {
            sql.append("and a.account_type = :accountType ");
        }
        if (status != null) {
            sql.append("and a.status = :status ");
        }
        if (createdFrom != null) {
            sql.append("and a.created_at >= :createdFrom ");
        }
        if (createdTo != null) {
            sql.append("and a.created_at <= :createdTo ");
        }
        sql.append("order by a.created_at desc");

        return DaoHelper.inTransaction(session -> {
            var query = session.createNativeQuery(sql.toString());
            if (clientId != null) {
                query.setParameter("clientId", clientId);
            }
            if (accountType != null) {
                query.setParameter("accountType", accountType.name());
            }
            if (status != null) {
                query.setParameter("status", status.name());
            }
            if (createdFrom != null) {
                query.setParameter("createdFrom", createdFrom);
            }
            if (createdTo != null) {
                query.setParameter("createdTo", createdTo);
            }
            List<Object[]> rows = query.list();
            return rows.stream().map(this::mapAccount).toList();
        });
    }

    private List<Map<String, Object>> accountsForDepartment(Long departmentId) {
        return DaoHelper.inTransaction(session -> {
            List<Object[]> rows = session.createNativeQuery(
                    "select a.id, a.special_number, a.points, a.status, a.account_type, a.created_at, a.closed_at, " +
                            "c.id, c.surname, c.second_name, d.name, cur.name " +
                            "from accounts a " +
                            "join clients c on c.id = a.client_id " +
                            "join departments d on d.id = c.department_id " +
                            "join currencys cur on cur.id = a.currency_id " +
                            "where d.id = :departmentId " +
                            "order by a.created_at desc")
                    .setParameter("departmentId", departmentId)
                    .list();
            return rows.stream().map(this::mapAccount).toList();
        });
    }

    private List<Map<String, Object>> operationsForClient(Long clientId) {
        return DaoHelper.inTransaction(session -> {
            List<Object[]> rows = session.createNativeQuery(
                    "select op.id, op.kind, op.amount, op.description, op.created_at, a.id, a.special_number " +
                            "from account_operations op " +
                            "join accounts a on a.id = op.account_id " +
                            "where a.client_id = :clientId " +
                            "order by op.created_at desc")
                    .setParameter("clientId", clientId)
                    .list();
            return rows.stream().map(this::mapOperation).toList();
        });
    }

    private List<Map<String, Object>> operationsForAccount(Long accountId) {
        return DaoHelper.inTransaction(session -> {
            List<Object[]> rows = session.createNativeQuery(
                    "select op.id, op.kind, op.amount, op.description, op.created_at, a.id, a.special_number " +
                            "from account_operations op " +
                            "join accounts a on a.id = op.account_id " +
                            "where a.id = :accountId " +
                            "order by op.created_at desc")
                    .setParameter("accountId", accountId)
                    .list();
            return rows.stream().map(this::mapOperation).toList();
        });
    }

    private List<Map<String, Object>> accountTypeSummary() {
        return DaoHelper.inTransaction(session -> {
            List<Object[]> rows = session.createNativeQuery(
                    "select account_type, count(*), coalesce(sum(points), 0) " +
                            "from accounts group by account_type order by account_type")
                    .list();
            return rows.stream().map(row -> mapOf(
                    "accountType", row[0],
                    "accountTypeLabel", accountTypeLabel(String.valueOf(row[0])),
                    "count", scalarLong(row[1]),
                    "totalBalance", scalarDouble(row[2])
            )).toList();
        });
    }

    private Object[] accountBaseRow(Long id) {
        return singleRow(DaoHelper.inTransaction(session -> session.createNativeQuery(
                "select a.id, a.special_number, a.points, a.status, a.account_type, a.created_at, a.closed_at, " +
                        "c.id, c.surname, c.second_name, d.name, cur.name " +
                        "from accounts a " +
                        "join clients c on c.id = a.client_id " +
                        "join departments d on d.id = c.department_id " +
                        "join currencys cur on cur.id = a.currency_id " +
                        "where a.id = :id")
                .setParameter("id", id)
                .uniqueResult()));
    }

    private Map<String, Object> accountSpecificDetails(Long accountId, String accountType) {
        return DaoHelper.inTransaction(session -> {
            Object row = switch (accountType) {
                case "credit" -> session.createNativeQuery(
                                "select max_credit, current_dept, interest_rate, payment_method from credit where id = :id")
                        .setParameter("id", accountId)
                        .uniqueResult();
                case "deposit" -> session.createNativeQuery(
                                "select initial_amount, end_date, automatic_renewal, payment_method from deposit where id = :id")
                        .setParameter("id", accountId)
                        .uniqueResult();
                case "saving" -> session.createNativeQuery(
                                "select interest_rate, max_limit from saving_account where id = :id")
                        .setParameter("id", accountId)
                        .uniqueResult();
                default -> null;
            };
            Object[] detail = singleRow(row);
            if (detail == null) {
                return Map.of();
            }
            return switch (accountType) {
                case "credit" -> mapOf(
                        "maxCredit", detail[0],
                        "currentDebt", detail[1],
                        "interestRate", detail[2],
                        "paymentMethod", detail[3],
                        "paymentMethodLabel", paymentMethodLabel(String.valueOf(detail[3])));
                case "deposit" -> mapOf(
                        "initialAmount", detail[0],
                        "endDate", detail[1],
                        "automaticRenewal", detail[2],
                        "paymentMethod", detail[3],
                        "paymentMethodLabel", paymentMethodLabel(String.valueOf(detail[3])));
                case "saving" -> mapOf(
                        "interestRate", detail[0],
                        "maxLimit", detail[1]);
                default -> Map.of();
            };
        });
    }

    private Map<String, Object> mapClientDetail(Object[] row) {
        return mapOf(
                "id", scalarLong(row[0]),
                "departmentId", scalarLong(row[1]),
                "departmentName", row[2],
                "type", row[3],
                "typeLabel", clientTypeLabel(String.valueOf(row[3])),
                "name", clientDisplayName(row[4], row[5]),
                "surname", row[4],
                "secondName", row[5],
                "passport", row[6],
                "snils", row[7],
                "phone", row[8],
                "address", row[9],
                "linkToPhoto", row[10],
                "accountCount", scalarLong(row[11]),
                "totalBalance", scalarDouble(row[12])
        );
    }

    private Map<String, Object> mapAccount(Object[] row) {
        return mapOf(
                "id", scalarLong(row[0]),
                "specialNumber", row[1],
                "points", scalarDouble(row[2]),
                "status", row[3],
                "statusLabel", accountStatusLabel(String.valueOf(row[3])),
                "accountType", row[4],
                "accountTypeLabel", accountTypeLabel(String.valueOf(row[4])),
                "createdAt", row[5],
                "closedAt", row[6],
                "clientId", scalarLong(row[7]),
                "clientName", clientDisplayName(row[8], row[9]),
                "departmentName", row[10],
                "currency", row[11]
        );
    }

    private Map<String, Object> mapOperation(Object[] row) {
        return mapOf(
                "id", scalarLong(row[0]),
                "kind", row[1],
                "kindLabel", "credit".equals(String.valueOf(row[1])) ? "Начисление" : "Списание",
                "amount", row[2],
                "description", row[3],
                "createdAt", row[4],
                "accountId", scalarLong(row[5]),
                "specialNumber", row[6]
        );
    }

    private static Object[] singleRow(Object row) {
        if (row == null) {
            return null;
        }
        if (row instanceof Object[] values) {
            return values;
        }
        return new Object[]{row};
    }

    private static String clientDisplayName(Object surname, Object secondName) {
        String first = surname == null ? "" : String.valueOf(surname);
        String second = secondName == null ? "" : String.valueOf(secondName);
        return (first + " " + second).trim();
    }

    private static String clientTypeLabel(String dbValue) {
        return "legal entity".equals(dbValue) ? "Юридическое лицо" : "Физическое лицо";
    }

    private static String accountTypeLabel(String value) {
        return switch (value) {
            case "credit" -> "Кредит";
            case "deposit" -> "Вклад";
            case "saving" -> "Накопительный";
            default -> value;
        };
    }

    private static String accountStatusLabel(String value) {
        return switch (value) {
            case "active" -> "Открыт";
            case "closed" -> "Закрыт";
            case "suspended" -> "Приостановлен";
            default -> value;
        };
    }

    private static String paymentMethodLabel(String value) {
        return "auto".equals(value) ? "Авто" : "Ручной";
    }

    private static Map<String, Object> ok(String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", message);
        return result;
    }

    private static Map<String, Object> fail(String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("message", message);
        return result;
    }

    private static Map<String, Object> mapOf(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static Long parseLong(String value) {
        String cleaned = clean(value);
        if (cleaned == null) {
            return null;
        }
        return Long.parseLong(cleaned);
    }

    private static BigDecimal parseBigDecimal(String value, boolean optional, String label) {
        String cleaned = clean(value);
        if (cleaned == null) {
            if (optional) {
                return null;
            }
            throw new IllegalArgumentException(label + " обязательна.");
        }
        try {
            return new BigDecimal(cleaned.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " должна быть числом.");
        }
    }

    private static BigDecimal parseRequiredMoney(String value, String label) {
        BigDecimal amount = parseBigDecimal(value, false, label);
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(label + " не может быть отрицательной.");
        }
        return amount;
    }

    private static BigDecimal parseMoneyOrZero(String value) {
        BigDecimal amount = parseBigDecimal(value, true, "Сумма");
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Сумма не может быть отрицательной.");
        }
        return amount;
    }

    private static ClientType parseClientType(String value) {
        String cleaned = clean(value);
        if (cleaned == null) {
            return null;
        }
        return ClientType.valueOf(cleaned);
    }

    private static String parseClientTypeDb(String value) {
        ClientType type = parseClientType(value);
        return type == null ? null : type.getDbValue();
    }

    private static AccountType parseAccountType(String value) {
        String cleaned = clean(value);
        return cleaned == null ? null : AccountType.valueOf(cleaned);
    }

    private static AccountStatus parseAccountStatus(String value) {
        String cleaned = clean(value);
        return cleaned == null ? null : AccountStatus.valueOf(cleaned);
    }

    private static PaymentMethod parsePaymentMethod(String value) {
        String cleaned = clean(value);
        return cleaned == null ? PaymentMethod.manual : PaymentMethod.valueOf(cleaned);
    }

    private static OperationKind parseOperationKind(String value) {
        String cleaned = clean(value);
        if ("debit".equals(cleaned)) {
            return OperationKind.debit;
        }
        return OperationKind.credit;
    }

    private static LocalDate parseLocalDate(String value) {
        String cleaned = clean(value);
        return cleaned == null ? null : LocalDate.parse(cleaned);
    }

    private static Instant parseDateStart(String value) {
        LocalDate date = parseLocalDate(value);
        return date == null ? null : date.atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
    }

    private static Instant parseDateEnd(String value) {
        LocalDate date = parseLocalDate(value);
        return date == null ? null : date.plusDays(1).atStartOfDay().minusNanos(1).toInstant(java.time.ZoneOffset.UTC);
    }

    private static long scalarLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    private static double scalarDouble(Object value) {
        return value == null ? 0d : ((Number) value).doubleValue();
    }

    private static boolean isConstraintViolation(Throwable e) {
        Throwable current = e;
        while (current != null) {
            if (current instanceof ConstraintViolationException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
