package ru.javaprac.bank.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import ru.javaprac.bank.util.HibernateUtil;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public abstract class TestBase {

    @BeforeSuite
    public void initSessionFactory() {
        try {
            HibernateUtil.getSessionFactory();
        } catch (Exception e) {
            System.err.println("Hibernate init failed. Is PostgreSQL running? Run: ant init");
            e.printStackTrace();
            throw e;
        }
    }

    @AfterSuite
    public void shutdownSessionFactory() {
        HibernateUtil.shutdown();
    }

    protected <T> T withTemporarySessionFactory(SessionFactory replacement, Callable<T> action) throws Exception {
        Field field = HibernateUtil.class.getDeclaredField("sessionFactory");
        field.setAccessible(true);
        SessionFactory original = (SessionFactory) field.get(null);
        field.set(null, replacement);
        try {
            return action.call();
        } finally {
            field.set(null, original);
        }
    }

    protected SessionFactory stubSessionFactory(StubSessionPlan plan) {
        Transaction tx = proxy(Transaction.class, (proxy, method, args) -> switch (method.getName()) {
            case "commit", "rollback" -> null;
            default -> defaultValue(method.getReturnType());
        });

        Session session = proxy(Session.class, (proxy, method, args) -> switch (method.getName()) {
            case "beginTransaction" -> tx;
            case "createNativeQuery" -> newQueryProxy(plan);
            case "get" -> plan.entity((Class<?>) args[0]);
            case "remove" -> {
                plan.setRemovedEntity(args[0]);
                yield null;
            }
            case "merge" -> args[0];
            case "persist", "close" -> null;
            default -> defaultValue(method.getReturnType());
        });

        return proxy(SessionFactory.class, (proxy, method, args) -> switch (method.getName()) {
            case "openSession" -> session;
            case "close" -> null;
            default -> defaultValue(method.getReturnType());
        });
    }

    private NativeQuery<?> newQueryProxy(StubSessionPlan plan) {
        final NativeQuery<?>[] query = new NativeQuery<?>[1];
        query[0] = proxy(NativeQuery.class, (proxy, method, args) -> switch (method.getName()) {
            case "setParameter" -> query[0];
            case "uniqueResult" -> plan.nextUniqueResult();
            case "list" -> plan.nextListResult();
            default -> defaultValue(method.getReturnType());
        });
        return query[0];
    }

    @SuppressWarnings("unchecked")
    private <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class<?>[]{type},
            (proxy, method, args) -> {
                String name = method.getName();
                if ("toString".equals(name)) {
                    return type.getSimpleName() + "Stub";
                }
                if ("hashCode".equals(name)) {
                    return System.identityHashCode(proxy);
                }
                if ("equals".equals(name)) {
                    return proxy == (args == null ? null : args[0]);
                }
                return handler.invoke(proxy, method, args);
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

    protected static final class StubSessionPlan {
        private final List<Object> uniqueResults = new ArrayList<>();
        private final List<List<?>> listResults = new ArrayList<>();
        private final Map<Class<?>, Object> entities = new HashMap<>();
        private int uniqueIndex;
        private int listIndex;
        private Object removedEntity;

        protected StubSessionPlan addUniqueResult(Object result) {
            uniqueResults.add(result);
            return this;
        }

        protected StubSessionPlan addListResult(List<?> result) {
            listResults.add(result);
            return this;
        }

        protected StubSessionPlan withEntity(Class<?> type, Object entity) {
            entities.put(type, entity);
            return this;
        }

        protected Object removedEntity() {
            return removedEntity;
        }

        private Object nextUniqueResult() {
            if (uniqueIndex >= uniqueResults.size()) {
                throw new IllegalStateException("No uniqueResult configured for stub query");
            }
            return uniqueResults.get(uniqueIndex++);
        }

        private List<?> nextListResult() {
            if (listIndex >= listResults.size()) {
                return List.of();
            }
            return listResults.get(listIndex++);
        }

        private Object entity(Class<?> type) {
            return entities.get(type);
        }

        private void setRemovedEntity(Object removedEntity) {
            this.removedEntity = removedEntity;
        }
    }
}
