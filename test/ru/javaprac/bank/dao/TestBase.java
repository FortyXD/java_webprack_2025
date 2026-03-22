package ru.javaprac.bank.dao;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import ru.javaprac.bank.util.HibernateUtil;

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
}
