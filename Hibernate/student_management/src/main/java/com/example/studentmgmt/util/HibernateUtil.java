package com.example.studentmgmt.util;

import com.example.studentmgmt.model.Student;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {

    private static SessionFactory sessionFactory;
    private static SessionFactory testSessionFactory;

    public static synchronized SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            try {
                Configuration configuration = new Configuration();
                configuration.configure("hibernate.cfg.xml");
                sessionFactory = configuration.buildSessionFactory();
            } catch (Exception e) {
                System.err.println("Initial SessionFactory creation failed: " + e.getMessage());
                throw new ExceptionInInitializerError(e);
            }
        }
        return sessionFactory;
    }

    public static synchronized SessionFactory getTestSessionFactory() {
        if (testSessionFactory == null) {
            try {
                Configuration configuration = new Configuration();
                // Programmatic configuration for H2 database
                configuration.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
                configuration.setProperty("hibernate.connection.url", "jdbc:h2:mem:student_test;DB_CLOSE_DELAY=-1");
                configuration.setProperty("hibernate.connection.username", "sa");
                configuration.setProperty("hibernate.connection.password", "");
                configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
                configuration.setProperty("hibernate.hbm2ddl.auto", "create-drop");
                configuration.setProperty("hibernate.show_sql", "true");
                configuration.setProperty("hibernate.format_sql", "true");

                // Mapped entities
                configuration.addAnnotatedClass(Student.class);

                testSessionFactory = configuration.buildSessionFactory();
            } catch (Exception e) {
                System.err.println("Test SessionFactory creation failed: " + e.getMessage());
                throw new ExceptionInInitializerError(e);
            }
        }
        return testSessionFactory;
    }

    public static void shutdown() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
        }
        if (testSessionFactory != null && !testSessionFactory.isClosed()) {
            testSessionFactory.close();
        }
    }
}
