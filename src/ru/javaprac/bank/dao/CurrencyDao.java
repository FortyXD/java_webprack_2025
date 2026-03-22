package ru.javaprac.bank.dao;

import ru.javaprac.bank.entity.Currency;

import java.util.List;
import java.util.Optional;

public class CurrencyDao {

    public List<Currency> findAll() {
        return DaoHelper.inTransaction(session ->
            session.createNativeQuery("select * from currencys order by name", Currency.class).list()
        );
    }

    public Optional<Currency> findById(Long id) {
        return DaoHelper.inTransaction(session -> {
            Currency c = session.get(Currency.class, id);
            return Optional.ofNullable(c);
        });
    }

    public Optional<Currency> findByName(String name) {
        return DaoHelper.inTransaction(session -> session
            .createNativeQuery("select * from currencys where name = :name", Currency.class)
            .setParameter("name", name)
            .list()
            .stream()
            .findFirst());
    }
}
