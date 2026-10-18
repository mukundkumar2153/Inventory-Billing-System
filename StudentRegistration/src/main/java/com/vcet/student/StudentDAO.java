package com.vcet.student;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access object: all CRUD operations use PreparedStatement
 * (prevents SQL injection).
 */
public class StudentDAO {

    private static final String INSERT =
            "INSERT INTO students (student_name, register_number, email, department, mobile) VALUES (?, ?, ?, ?, ?)";
    private static final String SELECT_ALL =
            "SELECT id, student_name, register_number, email, department, mobile FROM students ORDER BY id DESC";
    private static final String SELECT_ONE =
            "SELECT id, student_name, register_number, email, department, mobile FROM students WHERE id = ?";
    private static final String UPDATE =
            "UPDATE students SET student_name = ?, register_number = ?, email = ?, department = ?, mobile = ? WHERE id = ?";
    private static final String DELETE =
            "DELETE FROM students WHERE id = ?";

    /** CREATE */
    public boolean insert(Student s) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(INSERT)) {
            ps.setString(1, s.getName());
            ps.setString(2, s.getRegisterNumber());
            ps.setString(3, s.getEmail());
            ps.setString(4, s.getDepartment());
            ps.setString(5, s.getMobile());
            return ps.executeUpdate() > 0;
        }
    }

    /** READ (all) */
    public List<Student> getAll() throws SQLException {
        List<Student> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    /** READ (one) */
    public Student getById(int id) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_ONE)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** UPDATE */
    public boolean update(Student s) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(UPDATE)) {
            ps.setString(1, s.getName());
            ps.setString(2, s.getRegisterNumber());
            ps.setString(3, s.getEmail());
            ps.setString(4, s.getDepartment());
            ps.setString(5, s.getMobile());
            ps.setInt(6, s.getId());
            return ps.executeUpdate() > 0;
        }
    }

    /** DELETE */
    public boolean delete(int id) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(DELETE)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Student map(ResultSet rs) throws SQLException {
        return new Student(
                rs.getInt("id"),
                rs.getString("student_name"),
                rs.getString("register_number"),
                rs.getString("email"),
                rs.getString("department"),
                rs.getString("mobile"));
    }
}
