package ru.javaprac.bank.dao;

import org.hibernate.Session;
import org.hibernate.Transaction;
import ru.javaprac.bank.util.HibernateUtil;

import java.util.function.Consumer;
import java.util.function.Function;

public final class DaoHelper {

    private DaoHelper() {}

    public static void runInTransaction(Consumer<Session> action) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                action.accept(session);
                tx.commit();
            } catch (Exception e) {
                tx.rollback();
                throw e;
            }
        }
    }

    public static <T> T inTransaction(Function<Session, T> action) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                T result = action.apply(session);
                tx.commit();
                return result;
            } catch (Exception e) {
                tx.rollback();
                throw e;
            }
        }
    }
}
