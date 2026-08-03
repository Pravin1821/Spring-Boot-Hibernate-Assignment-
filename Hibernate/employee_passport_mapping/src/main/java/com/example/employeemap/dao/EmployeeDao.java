package com.example.employeemap.dao;

import com.example.employeemap.model.Employee;
import com.example.employeemap.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.List;

public class EmployeeDao {

    private final SessionFactory sessionFactory;

    public EmployeeDao() {
        this.sessionFactory = HibernateUtil.getSessionFactory();
    }

    public EmployeeDao(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void save(Employee employee) {
        if (employee == null) {
            throw new IllegalArgumentException("Employee cannot be null");
        }
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = null;
            try {
                transaction = session.beginTransaction();
                session.persist(employee);
                transaction.commit();
            } catch (Exception e) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                throw e;
            }
        }
    }

    public void update(Employee employee) {
        if (employee == null || employee.getId() == null) {
            throw new IllegalArgumentException("Employee and Employee ID cannot be null for updates");
        }
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = null;
            try {
                transaction = session.beginTransaction();
                session.merge(employee);
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
            throw new IllegalArgumentException("Employee ID cannot be null for deletion");
        }
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = null;
            try {
                transaction = session.beginTransaction();
                Employee employee = session.get(Employee.class, id);
                if (employee != null) {
                    session.remove(employee);
                }
                transaction.commit();
            } catch (Exception e) {
                if (transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                throw e;
            }
        }
    }

    public Employee getById(Long id) {
        if (id == null) {
            return null;
        }
        try (Session session = sessionFactory.openSession()) {
            return session.get(Employee.class, id);
        }
    }

    public Employee getByIdEager(Long id) {
        if (id == null) {
            return null;
        }
        try (Session session = sessionFactory.openSession()) {
            Query<Employee> query = session.createQuery(
                    "from Employee e left join fetch e.passport where e.id = :id",
                    Employee.class
            );
            query.setParameter("id", id);
            return query.uniqueResult();
        }
    }

    public List<Employee> getAll() {
        try (Session session = sessionFactory.openSession()) {
            Query<Employee> query = session.createQuery("from Employee", Employee.class);
            return query.list();
        }
    }
}
