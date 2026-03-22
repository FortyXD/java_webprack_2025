package ru.javaprac.bank.util;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public final class HibernateUtil {

    private static volatile SessionFactory sessionFactory;

    private HibernateUtil() {}

    public static SessionFactory getSessionFactory() {
        SessionFactory sf = sessionFactory;
        if (sf == null) {
            synchronized (HibernateUtil.class) {
                sf = sessionFactory;
                if (sf == null) {
                    Configuration cfg = new Configuration().configure();
                    String url = System.getProperty("hibernate.connection.url");
                    if (url != null) {
                        cfg.setProperty("hibernate.connection.url", url);
                    }
                    String user = System.getProperty("hibernate.connection.username");
                    if (user != null) {
                        cfg.setProperty("hibernate.connection.username", user);
                    }
                    String pass = System.getProperty("hibernate.connection.password");
                    if (pass != null) {
                        cfg.setProperty("hibernate.connection.password", pass);
                    }
                    sessionFactory = sf = cfg.buildSessionFactory();
                }
            }
        }
        return sf;
    }

    public static void shutdown() {
        if (sessionFactory != null) {
            sessionFactory.close();
            sessionFactory = null;
        }
    }
}
