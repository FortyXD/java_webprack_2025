package ru.javaprac.bank.dao;

import ru.javaprac.bank.entity.AccountStatus;
import ru.javaprac.bank.entity.Client;
import ru.javaprac.bank.entity.ClientType;

import java.util.List;
import java.util.Optional;

public class ClientDao {

    public List<Client> findAll() {
        return DaoHelper.inTransaction(session ->
            session.createNativeQuery(
                "select * from clients order by surname, second_name",
                Client.class
            ).list()
        );
    }

    public Optional<Client> findById(Long id) {
        return DaoHelper.inTransaction(session -> {
            Client c = session.get(Client.class, id);
            return Optional.ofNullable(c);
        });
    }

    public List<Client> findFiltered(Long departmentId, ClientType type, String search) {
        return DaoHelper.inTransaction(session -> {
            var sql = new StringBuilder("select * from clients where 1=1");
            if (departmentId != null) {
                sql.append(" and department_id = :depId");
            }
            if (type != null) {
                sql.append(" and type = :type");
            }
            if (search != null && !search.isBlank()) {
                sql.append(" and (lower(coalesce(surname,'')) like :search")
                    .append(" or lower(coalesce(second_name,'')) like :search")
                    .append(" or lower(coalesce(phone,'')) like :search)");
            }
            sql.append(" order by surname, second_name");
            var q = session.createNativeQuery(sql.toString(), Client.class);
            if (departmentId != null) q.setParameter("depId", departmentId);
            if (type != null) q.setParameter("type", type.getDbValue());
            if (search != null && !search.isBlank()) {
                q.setParameter("search", "%" + search.trim().toLowerCase() + "%");
            }
            return q.list();
        });
    }

    public void save(Client client) {
        DaoHelper.runInTransaction(session -> session.persist(client));
    }

    public Client update(Client client) {
        return DaoHelper.inTransaction(session -> {
            session.merge(client);
            return client;
        });
    }

    public boolean deleteIfNoOpenAccountsOrZeroBalance(Long clientId) {
        return DaoHelper.inTransaction(session -> {
            Number openCount = (Number) session.createNativeQuery(
                "select count(*) from accounts where client_id = :clientId and status = :active")
                .setParameter("clientId", clientId)
                .setParameter("active", AccountStatus.active.name())
                .uniqueResult();
            if (openCount != null && openCount.longValue() > 0) {
                return false;
            }
            Number totalBalance = (Number) session.createNativeQuery(
                "select coalesce(sum(points), 0) from accounts where client_id = :clientId")
                .setParameter("clientId", clientId)
                .uniqueResult();
            if (totalBalance != null && totalBalance.doubleValue() != 0) {
                return false;
            }
            Client c = session.get(Client.class, clientId);
            if (c != null) {
                session.remove(c);
                return true;
            }
            return false;
        });
    }
}
