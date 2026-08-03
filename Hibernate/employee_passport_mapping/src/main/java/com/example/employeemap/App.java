package com.example.employeemap;

import com.example.employeemap.dao.EmployeeDao;
import com.example.employeemap.model.Employee;
import com.example.employeemap.model.Passport;
import com.example.employeemap.util.HibernateUtil;

import java.time.LocalDate;
import java.util.List;

public class App {
    public static void main(String[] args) {
        System.out.println("Starting Hibernate Employee and Passport mapping seeding application...");

        EmployeeDao employeeDao = new EmployeeDao();

        // 1. Create passport and employee
        Passport passport = new Passport("PASS-9988-XY", LocalDate.now().plusYears(10));
        Employee employee = new Employee("James Bond", "james.bond@mi6.gov.uk", passport);

        System.out.println("Saving employee: " + employee.getName() + " with passport: " + passport.getPassportNumber());
        employeeDao.save(employee);

        // 2. Create another employee without passport
        Employee employeeNoPass = new Employee("John Smith", "john.smith@gmail.com");
        System.out.println("Saving employee without passport: " + employeeNoPass.getName());
        employeeDao.save(employeeNoPass);

        // 3. Retrieve all employees and print
        System.out.println("Retrieving all employees from MySQL database...");
        List<Employee> list = employeeDao.getAll();
        for (Employee emp : list) {
            System.out.println("ID: " + emp.getId() + ", Name: " + emp.getName() + ", Email: " + emp.getEmail());
            Employee eagerEmp = employeeDao.getByIdEager(emp.getId());
            if (eagerEmp.getPassport() != null) {
                System.out.println("  -> Passport Number: " + eagerEmp.getPassport().getPassportNumber());
            } else {
                System.out.println("  -> No Passport Assigned");
            }
        }

        // Shutdown Hibernate
        HibernateUtil.shutdown();
        System.out.println("Database seeding completed successfully!");
    }
}
