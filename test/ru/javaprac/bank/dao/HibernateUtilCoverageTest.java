package ru.javaprac.bank.dao;

import org.hibernate.SessionFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import ru.javaprac.bank.util.HibernateUtil;

import java.lang.reflect.Field;

import static org.testng.Assert.*;

public class HibernateUtilCoverageTest extends TestBase {

    private String originalUrl;
    private String originalUser;
    private String originalPass;

    @AfterMethod(alwaysRun = true)
    public void restoreHibernateState() throws Exception {
        HibernateUtil.shutdown();
        restoreProperty("hibernate.connection.url", originalUrl);
        restoreProperty("hibernate.connection.username", originalUser);
        restoreProperty("hibernate.connection.password", originalPass);
    }

    @Test
    public void getSessionFactory_whenPropertiesAreMissing_usesConfigDefaultsAndCachesInstance() {
        rememberProperties();
        System.clearProperty("hibernate.connection.url");
        System.clearProperty("hibernate.connection.username");
        System.clearProperty("hibernate.connection.password");
        HibernateUtil.shutdown();

        SessionFactory first = HibernateUtil.getSessionFactory();
        SessionFactory second = HibernateUtil.getSessionFactory();

        assertNotNull(first);
        assertSame(second, first);
    }

    @Test
    public void getSessionFactory_whenInitializedBetweenChecks_returnsExistingFactory() throws Exception {
        rememberProperties();
        HibernateUtil.shutdown();
        writeSessionFactory(null);

        SessionFactory injected = stubSessionFactory(new StubSessionPlan());
        SessionFactory[] fromThread = new SessionFactory[1];
        Throwable[] failure = new Throwable[1];

        synchronized (HibernateUtil.class) {
            Thread t = new Thread(() -> {
                try {
                    fromThread[0] = HibernateUtil.getSessionFactory();
                } catch (Throwable t1) {
                    failure[0] = t1;
                }
            }, "hibernate-util-race");
            t.start();

            long deadline = System.currentTimeMillis() + 2_000;
            while (t.getState() != Thread.State.BLOCKED && System.currentTimeMillis() < deadline) {
                Thread.yield();
            }
            assertEquals(t.getState(), Thread.State.BLOCKED, "Thread must wait on HibernateUtil monitor");

            writeSessionFactory(injected);
        }

        Thread.sleep(10);
        assertNull(failure[0]);
        assertSame(fromThread[0], injected);
    }

    @Test
    public void shutdown_whenFactoryExists_closesItAndClearsStaticField() throws Exception {
        TrackingSessionFactory tracker = new TrackingSessionFactory();

        withTemporarySessionFactory(tracker.factory, () -> {
            HibernateUtil.shutdown();
            assertEquals(tracker.closeCalls, 1);
            assertNull(readSessionFactory());
            return null;
        });
    }

    @Test
    public void shutdown_whenFactoryAbsent_doesNothing() throws Exception {
        rememberProperties();
        HibernateUtil.shutdown();
        writeSessionFactory(null);

        HibernateUtil.shutdown();

        assertNull(readSessionFactory());
    }

    private void rememberProperties() {
        originalUrl = System.getProperty("hibernate.connection.url");
        originalUser = System.getProperty("hibernate.connection.username");
        originalPass = System.getProperty("hibernate.connection.password");
    }

    private void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    private SessionFactory readSessionFactory() throws Exception {
        return (SessionFactory) sessionFactoryField().get(null);
    }

    private void writeSessionFactory(SessionFactory value) throws Exception {
        sessionFactoryField().set(null, value);
    }

    private Field sessionFactoryField() throws Exception {
        Field field = HibernateUtil.class.getDeclaredField("sessionFactory");
        field.setAccessible(true);
        return field;
    }

    private static final class TrackingSessionFactory {
        private int closeCalls;
        private final SessionFactory factory;

        private TrackingSessionFactory() {
            factory = (SessionFactory) java.lang.reflect.Proxy.newProxyInstance(
                SessionFactory.class.getClassLoader(),
                new Class<?>[]{SessionFactory.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "close" -> {
                        closeCalls++;
                        yield null;
                    }
                    case "toString" -> "CloseTrackingSessionFactory";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> null;
                }
            );
        }
    }
}
