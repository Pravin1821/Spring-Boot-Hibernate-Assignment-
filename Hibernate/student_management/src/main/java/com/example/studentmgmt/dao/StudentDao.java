package com.example.studentmgmt.dao;

import com.example.studentmgmt.exception.StudentNotFoundException;
import com.example.studentmgmt.model.Student;
import com.example.studentmgmt.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.List;

public class StudentDao {

    private final SessionFactory sessionFactory;

    public StudentDao() {
        this.sessionFactory = HibernateUtil.getSessionFactory();
    }

    public StudentDao(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void save(Student student) {
        if (student == null) {
            throw new IllegalArgumentException("Student cannot be null");
        }
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = null;
            try {
                transaction = session.beginTransaction();
                session.persist(student);
                transaction.commit();
            } catch (Exception e) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                throw e;
            }
        }
    }

    public void update(Student student) {
        if (student == null || student.getId() == null) {
            throw new IllegalArgumentException("Student and Student ID cannot be null for updates");
        }
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = null;
            try {
                transaction = session.beginTransaction();

                Student existing = session.get(Student.class, student.getId());
                if (existing == null) {
                    throw new StudentNotFoundException("Student not found with ID: " + student.getId());
                }

                existing.setName(student.getName());
                existing.setEmail(student.getEmail());
                existing.setCourse(student.getCourse());
                existing.setAge(student.getAge());

                session.merge(existing);
                transaction.commit();
            } catch (Exception e) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                throw e;
            }
        }
    }

    public void delete(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Student ID cannot be null for deletion");
        }
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = null;
            try {
                transaction = session.beginTransaction();

                Student student = session.get(Student.class, id);
                if (student == null) {
                    throw new StudentNotFoundException("Student not found with ID: " + id);
                }

                session.remove(student);
                transaction.commit();
            } catch (Exception e) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                throw e;
            }
        }
    }


    public Student getById(Long id) {
        if (id == null) {
            return null;
        }
        try (Session session = sessionFactory.openSession()) {
            return session.get(Student.class, id);
        }
    }

    public List<Student> getAll() {
        try (Session session = sessionFactory.openSession()) {
            Query<Student> query = session.createQuery("from Student", Student.class);
            return query.list();
        }
    }
}
