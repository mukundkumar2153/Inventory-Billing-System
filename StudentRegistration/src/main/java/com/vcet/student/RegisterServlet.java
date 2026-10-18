package com.vcet.student;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Receives the registration form, validates it, and inserts it via JDBC. */
@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private final StudentDAO dao = new StudentDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.sendRedirect(req.getContextPath() + "/index.html");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("text/html;charset=UTF-8");

        Student s = new Student(0,
                trim(req.getParameter("studentName")),
                trim(req.getParameter("registerNumber")),
                trim(req.getParameter("email")),
                trim(req.getParameter("department")),
                trim(req.getParameter("mobile")));

        boolean success = false;
        String msg;

        String error = Validator.validate(s);
        if (error != null) {
            msg = error;
        } else {
            try {
                success = dao.insert(s);
                msg = success
                        ? "Registration successful! Welcome, " + s.getName() + "."
                        : "Registration failed: no record was saved. Please try again.";
            } catch (SQLIntegrityConstraintViolationException e) {
                msg = "Register number " + s.getRegisterNumber() + " is already registered.";
            } catch (SQLException e) {
                msg = "Database error: could not save details. (" + e.getMessage() + ")";
            }
        }

        String ctx = req.getContextPath();
        try (PrintWriter out = resp.getWriter()) {
            out.print(Html.head("Registration Result", ctx));
            out.print("<section class=\"card\"><h1>Registration Result</h1>");
            out.print(Html.message(success, msg));
            if (success) {
                out.print("<table class=\"summary\">"
                        + "<tr><th>Name</th><td>" + Html.esc(s.getName()) + "</td></tr>"
                        + "<tr><th>Register No.</th><td>" + Html.esc(s.getRegisterNumber()) + "</td></tr>"
                        + "<tr><th>Email</th><td>" + Html.esc(s.getEmail()) + "</td></tr>"
                        + "<tr><th>Department</th><td>" + Html.esc(s.getDepartment()) + "</td></tr>"
                        + "<tr><th>Mobile</th><td>" + Html.esc(s.getMobile()) + "</td></tr>"
                        + "</table>");
            }
            out.print("<div class=\"actions\">"
                    + "<a class=\"btn\" href=\"" + ctx + "/index.html\">" + (success ? "Register Another" : "Go Back") + "</a> "
                    + "<a class=\"btn btn-secondary\" href=\"" + ctx + "/students\">View All Students</a>"
                    + "</div></section>");
            out.print(Html.foot());
        }
    }

    private static String trim(String v) {
        return v == null ? "" : v.trim();
    }
}
