package com.example.studentmgmt;

import com.example.studentmgmt.dao.StudentDao;
import com.example.studentmgmt.exception.StudentNotFoundException;
import com.example.studentmgmt.model.Student;
import com.example.studentmgmt.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class StudentDaoTest {

    private static final SessionFactory sessionFactory = HibernateUtil.getTestSessionFactory();
    private StudentDao studentDao;

    @BeforeEach
    public void setUp() {
        studentDao = new StudentDao(sessionFactory);
        // Clear database table before each test
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            session.createMutationQuery("delete from Student").executeUpdate();
            tx.commit();
        }
    }

    @AfterAll
    public static void tearDown() {
        HibernateUtil.shutdown();
    }

    @Test
    public void testInsertAndRetrieveStudent() {
        Student student = new Student("Alice", "alice@example.com", "Computer Science", 20);
        studentDao.save(student);

        assertNotNull(student.getId());

        Student retrieved = studentDao.getById(student.getId());
        assertNotNull(retrieved);
        assertEquals("Alice", retrieved.getName());
        assertEquals("alice@example.com", retrieved.getEmail());
        assertEquals("Computer Science", retrieved.getCourse());
        assertEquals(20, retrieved.getAge());
    }

    @Test
    public void testUpdateDetails() {
        Student student = new Student("Bob", "bob@example.com", "Physics", 21);
        studentDao.save(student);

        student.setName("Robert");
        student.setCourse("Theoretical Physics");
        studentDao.update(student);

        Student updated = studentDao.getById(student.getId());
        assertEquals("Robert", updated.getName());
        assertEquals("Theoretical Physics", updated.getCourse());
    }

    @Test
    public void testDeleteStudent() {
        Student student = new Student("Charlie", "charlie@example.com", "Chemistry", 22);
        studentDao.save(student);

        Long id = student.getId();
        studentDao.delete(id);

        assertNull(studentDao.getById(id));
    }

    @Test
    public void testDuplicateEmailThrowsExceptionAndRollback() {
        Student student1 = new Student("Alice", "duplicate@example.com", "Math", 19);
        studentDao.save(student1);

        Student student2 = new Student("Bob", "duplicate@example.com", "Biology", 20);

        // Attempting to save student2 should fail due to unique constraint on email
        assertThrows(Exception.class, () -> studentDao.save(student2));

        // Verify Bob was not saved andAlice remains
        List<Student> students = studentDao.getAll();
        assertEquals(1, students.size());
        assertEquals("Alice", students.getFirst().getName());
    }

    @Test
    public void testInvalidIdHandling() {
        // Retrieve invalid ID returns null
        assertNull(studentDao.getById(9999L));

        // Update invalid ID throws StudentNotFoundException
        Student nonExistent = new Student("Ghost", "ghost@example.com", "Occult", 99);
        nonExistent.setId(9999L);
        assertThrows(StudentNotFoundException.class, () -> studentDao.update(nonExistent));

        // Delete invalid ID throws StudentNotFoundException
        assertThrows(StudentNotFoundException.class, () -> studentDao.delete(9999L));
    }

    @Test
    public void testEmptyDatabaseReturnsEmptyList() {
        List<Student> list = studentDao.getAll();
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testNullValuesConstraintViolationAndRollback() {
        // Name is null (not allowed since nullable = false)
        Student studentWithNullName = new Student(null, "nullname@example.com", "Drama", 21);
        assertThrows(Exception.class, () -> studentDao.save(studentWithNullName));

        // Email is null (not allowed since nullable = false)
        Student studentWithNullEmail = new Student("NoEmail", null, "Drama", 21);
        assertThrows(Exception.class, () -> studentDao.save(studentWithNullEmail));

        // Verify no students were inserted
        List<Student> list = studentDao.getAll();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testTransactionRollbackOnMidTransactionError() {
        Student valid = new Student("Valid", "valid@example.com", "Engineering", 20);
        studentDao.save(valid);

        Student invalid = new Student(null, "invalid_rollback@example.com", "Engineering", 20);
        assertThrows(Exception.class, () -> studentDao.save(invalid));

        // Database should only have the 1 valid student
        List<Student> students = studentDao.getAll();
        assertEquals(1, students.size());
        assertEquals("valid@example.com", students.getFirst().getEmail());
    }
}
