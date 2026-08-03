package com.example.studentmgmt;

import com.example.studentmgmt.dao.StudentDao;
import com.example.studentmgmt.model.Student;
import com.example.studentmgmt.util.HibernateUtil;

import java.util.List;

public class App {
    public static void main(String[] args) {
        System.out.println("Starting Hibernate Student Management seeding application...");

        StudentDao studentDao = new StudentDao();

        // 1. Save a test student
        Student student = new Student("Jane Doe", "jane.doe@example.com", "Computer Science", 21);
        System.out.println("Saving student: " + student.getName() + " (" + student.getEmail() + ")");
        studentDao.save(student);

        // 2. Retrieve and verify
        System.out.println("Retrieving all students from MySQL database...");
        List<Student> students = studentDao.getAll();
        for (Student s : students) {
            System.out.println("ID: " + s.getId() + ", Name: " + s.getName() + ", Email: " + s.getEmail() + ", Course: " + s.getCourse());
        }

        // Shutdown Hibernate
        HibernateUtil.shutdown();
        System.out.println("Database seeding completed successfully!");
    }
}
