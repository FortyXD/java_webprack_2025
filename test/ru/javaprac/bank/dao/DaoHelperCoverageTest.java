package ru.javaprac.bank.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.testng.annotations.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.*;

public class DaoHelperCoverageTest extends TestBase {

    @Test
    public void runInTransaction_whenActionSucceeds_commitsAndClosesSession() throws Exception {
        TxTracker tracker = new TxTracker();
        SessionFactory factory = trackingSessionFactory(tracker);

        withTemporarySessionFactory(factory, () -> {
            AtomicInteger calls = new AtomicInteger();
            DaoHelper.runInTransaction(session -> {
                assertNotNull(session);
                calls.incrementAndGet();
            });

            assertEquals(calls.get(), 1);
            assertEquals(tracker.commits.get(), 1);
            assertEquals(tracker.rollbacks.get(), 0);
            assertEquals(tracker.closes.get(), 1);
            return null;
        });
    }

    @Test
    public void runInTransaction_whenActionFails_rollsBackAndRethrows() throws Exception {
        TxTracker tracker = new TxTracker();
        SessionFactory factory = trackingSessionFactory(tracker);

        withTemporarySessionFactory(factory, () -> {
            IllegalStateException ex = expectThrows(
                IllegalStateException.class,
                () -> DaoHelper.runInTransaction(session -> {
                    throw new IllegalStateException("boom");
                })
            );

            assertEquals(ex.getMessage(), "boom");
            assertEquals(tracker.commits.get(), 0);
            assertEquals(tracker.rollbacks.get(), 1);
            assertEquals(tracker.closes.get(), 1);
            return null;
        });
    }

    @Test
    public void inTransaction_whenActionSucceeds_returnsValueAndCommits() throws Exception {
        TxTracker tracker = new TxTracker();
        SessionFactory factory = trackingSessionFactory(tracker);

        withTemporarySessionFactory(factory, () -> {
            String value = DaoHelper.inTransaction(session -> {
                assertNotNull(session);
                return "done";
            });

            assertEquals(value, "done");
            assertEquals(tracker.commits.get(), 1);
            assertEquals(tracker.rollbacks.get(), 0);
            assertEquals(tracker.closes.get(), 1);
            return null;
        });
    }

    @Test
    public void inTransaction_whenActionFails_rollsBackAndRethrows() throws Exception {
        TxTracker tracker = new TxTracker();
        SessionFactory factory = trackingSessionFactory(tracker);

        withTemporarySessionFactory(factory, () -> {
            RuntimeException ex = expectThrows(
                RuntimeException.class,
                () -> DaoHelper.inTransaction(session -> {
                    throw new RuntimeException("fail");
                })
            );

            assertEquals(ex.getMessage(), "fail");
            assertEquals(tracker.commits.get(), 0);
            assertEquals(tracker.rollbacks.get(), 1);
            assertEquals(tracker.closes.get(), 1);
            return null;
        });
    }

    private SessionFactory trackingSessionFactory(TxTracker tracker) {
        Transaction tx = (Transaction) Proxy.newProxyInstance(
            Transaction.class.getClassLoader(),
            new Class<?>[]{Transaction.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "commit" -> {
                    tracker.commits.incrementAndGet();
                    yield null;
                }
                case "rollback" -> {
                    tracker.rollbacks.incrementAndGet();
                    yield null;
                }
                case "toString" -> "TrackingTransaction";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> defaultValue(method.getReturnType());
            }
        );

        Session session = (Session) Proxy.newProxyInstance(
            Session.class.getClassLoader(),
            new Class<?>[]{Session.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "beginTransaction" -> tx;
                case "close" -> {
                    tracker.closes.incrementAndGet();
                    yield null;
                }
                case "toString" -> "TrackingSession";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> defaultValue(method.getReturnType());
            }
        );

        return (SessionFactory) Proxy.newProxyInstance(
            SessionFactory.class.getClassLoader(),
            new Class<?>[]{SessionFactory.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "openSession" -> session;
                case "close" -> null;
                case "toString" -> "TrackingSessionFactory";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> defaultValue(method.getReturnType());
            }
        );
    }

    private Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0f;
        }
        if (returnType == double.class) {
            return 0d;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return null;
    }

    private static final class TxTracker {
        private final AtomicInteger commits = new AtomicInteger();
        private final AtomicInteger rollbacks = new AtomicInteger();
        private final AtomicInteger closes = new AtomicInteger();
    }
}
