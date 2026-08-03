package com.example.employeemap;

import com.example.employeemap.dao.EmployeeDao;
import com.example.employeemap.model.Employee;
import com.example.employeemap.model.Passport;
import com.example.employeemap.util.HibernateUtil;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EmployeePassportTest {

    private static final SessionFactory sessionFactory = HibernateUtil.getTestSessionFactory();
    private EmployeeDao employeeDao;

    @BeforeEach
    public void setUp() {
        employeeDao = new EmployeeDao(sessionFactory);
        // Clear database before each test. Clear child table references first.
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            session.createMutationQuery("delete from Employee").executeUpdate();
            session.createMutationQuery("delete from Passport").executeUpdate();
            tx.commit();
        }
    }

    @AfterAll
    public static void tearDown() {
        HibernateUtil.shutdown();
    }

    @Test
    public void testEmployeeWithoutPassport() {
        Employee employee = new Employee("John Doe", "john@example.com");
        employeeDao.save(employee);

        assertNotNull(employee.getId());
        assertNull(employee.getPassport());

        Employee retrieved = employeeDao.getById(employee.getId());
        assertNotNull(retrieved);
        assertEquals("John Doe", retrieved.getName());
        assertNull(retrieved.getPassport());
    }

    @Test
    public void testCreateEmployeeWithPassport() {
        Passport passport = new Passport("P123456", LocalDate.now().plusYears(10));
        Employee employee = new Employee("Alice", "alice@example.com", passport);

        employeeDao.save(employee);

        assertNotNull(employee.getId());
        assertNotNull(passport.getId());

        Employee retrieved = employeeDao.getByIdEager(employee.getId());
        assertNotNull(retrieved);
        assertNotNull(retrieved.getPassport());
        assertEquals("P123456", retrieved.getPassport().getPassportNumber());
    }

    @Test
    public void testDuplicatePassportNumberThrowsException() {
        Passport passport1 = new Passport("DUP_PASS", LocalDate.now().plusYears(5));
        Employee employee1 = new Employee("User One", "user1@example.com", passport1);
        employeeDao.save(employee1);

        Passport passport2 = new Passport("DUP_PASS", LocalDate.now().plusYears(5));
        Employee employee2 = new Employee("User Two", "user2@example.com", passport2);

        // Attempting to save employee2 with same passport number should fail due to unique constraint on passport_number
        assertThrows(Exception.class, () -> employeeDao.save(employee2));

        // Verify only 1 employee was saved
        List<Employee> list = employeeDao.getAll();
        assertEquals(1, list.size());
    }

    @Test
    public void testCascadeDelete() {
        Passport passport = new Passport("CASC_DEL", LocalDate.now().plusYears(5));
        Employee employee = new Employee("Bob", "bob@example.com", passport);
        employeeDao.save(employee);

        Long employeeId = employee.getId();
        Long passportId = passport.getId();

        // Verify they are saved
        assertNotNull(employeeDao.getById(employeeId));
        try (Session session = sessionFactory.openSession()) {
            assertNotNull(session.get(Passport.class, passportId));
        }

        // Delete employee
        employeeDao.delete(employeeId);

        // Verify employee is deleted
        assertNull(employeeDao.getById(employeeId));

        // Verify passport is ALSO deleted (Cascade delete check)
        try (Session session = sessionFactory.openSession()) {
            assertNull(session.get(Passport.class, passportId));
        }
    }

    @Test
    public void testLazyEagerLoadingVerification() {
        Passport passport = new Passport("LAZY_EAGER", LocalDate.now().plusYears(5));
        Employee employee = new Employee("Charlie", "charlie@example.com", passport);
        employeeDao.save(employee);

        Long id = employee.getId();

        // 1. Lazy loading verification using getById()
        Employee lazyEmployee = employeeDao.getById(id);
        assertNotNull(lazyEmployee);
        // Standard lazy loading verification
        assertFalse(Hibernate.isInitialized(lazyEmployee.getPassport()),
                "Passport should NOT be initialized initially (LAZY fetch)");

        // Accessing properties of a lazy-loaded proxy outside active session throws LazyInitializationException
        assertThrows(org.hibernate.LazyInitializationException.class, () -> {
            lazyEmployee.getPassport().getPassportNumber();
        });

        // 2. Eager loading verification using HQL JOIN FETCH query
        Employee eagerEmployee = employeeDao.getByIdEager(id);
        assertNotNull(eagerEmployee);
        assertTrue(Hibernate.isInitialized(eagerEmployee.getPassport()),
                "Passport should be pre-initialized (EAGER join fetch)");
        // Since it is eager loaded, accessing properties succeeds even outside session
        assertEquals("LAZY_EAGER", eagerEmployee.getPassport().getPassportNumber());
    }
}
