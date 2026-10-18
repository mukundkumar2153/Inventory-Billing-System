package com.vcet.student;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** GET: show a pre-filled edit form. POST: validate and update the record. */
@WebServlet("/update")
public class UpdateServlet extends HttpServlet {

    private final StudentDAO dao = new StudentDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int id = parseId(req.getParameter("id"));
        Student s = null;
        String error = null;
        try {
            s = id > 0 ? dao.getById(id) : null;
            if (s == null) error = "Student record not found.";
        } catch (SQLException e) {
            error = "Database error: " + e.getMessage();
        }
        render(req, resp, s, error);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");

        Student s = new Student(
                parseId(req.getParameter("id")),
                trim(req.getParameter("studentName")),
                trim(req.getParameter("registerNumber")),
                trim(req.getParameter("email")),
                trim(req.getParameter("department")),
                trim(req.getParameter("mobile")));

        String error = Validator.validate(s);
        if (error == null) {
            try {
                if (dao.update(s)) {
                    redirect(req, resp, true, "Student record updated successfully.");
                    return;
                }
                error = "No record was updated (it may have been deleted).";
            } catch (SQLIntegrityConstraintViolationException e) {
                error = "Register number " + s.getRegisterNumber() + " is already used by another student.";
            } catch (SQLException e) {
                error = "Database error: " + e.getMessage();
            }
        }
        render(req, resp, s, error);
    }

    private void render(HttpServletRequest req, HttpServletResponse resp, Student s, String error) throws IOException {
        resp.setContentType("text/html;charset=UTF-8");
        String ctx = req.getContextPath();
        try (PrintWriter out = resp.getWriter()) {
            out.print(Html.head("Edit Student", ctx));
            out.print("<section class=\"card\"><h1>Edit Student</h1>");
            if (error != null) out.print(Html.message(false, error));

            if (s != null) {
                out.print("<form id=\"studentForm\" method=\"post\" action=\"" + ctx + "/update\" novalidate "
                        + "onsubmit=\"return validateForm();\">"
                        + "<input type=\"hidden\" name=\"id\" value=\"" + s.getId() + "\">"
                        + field("studentName", "Student Name", "text", s.getName(), "e.g. Mukund Kumar")
                        + field("registerNumber", "Register Number", "text", s.getRegisterNumber(), "e.g. 24AIDS101")
                        + field("email", "Email Address", "email", s.getEmail(), "you@example.com")
                        + "<div class=\"field\"><label for=\"department\">Department</label>"
                        + "<select id=\"department\" name=\"department\">" + Html.departmentOptions(s.getDepartment()) + "</select>"
                        + "<span class=\"error\" id=\"departmentError\"></span></div>"
                        + field("mobile", "Mobile Number", "tel", s.getMobile(), "10-digit mobile number")
                        + "<div class=\"actions\"><button type=\"submit\" class=\"btn\">Update</button> "
                        + "<a class=\"btn btn-secondary\" href=\"" + ctx + "/students\">Cancel</a></div>"
                        + "</form>"
                        + "<script src=\"" + ctx + "/js/validate.js\"></script>");
            } else {
                out.print("<div class=\"actions\"><a class=\"btn\" href=\"" + ctx + "/students\">Back to list</a></div>");
            }
            out.print("</section>");
            out.print(Html.foot());
        }
    }

    private static String field(String id, String label, String type, String value, String placeholder) {
        return "<div class=\"field\"><label for=\"" + id + "\">" + label + "</label>"
                + "<input type=\"" + type + "\" id=\"" + id + "\" name=\"" + id + "\" value=\"" + Html.esc(value)
                + "\" placeholder=\"" + placeholder + "\">"
                + "<span class=\"error\" id=\"" + id + "Error\"></span></div>";
    }

    static void redirect(HttpServletRequest req, HttpServletResponse resp, boolean ok, String msg) throws IOException {
        resp.sendRedirect(req.getContextPath() + "/students?type=" + (ok ? "success" : "error")
                + "&msg=" + URLEncoder.encode(msg, StandardCharsets.UTF_8));
    }

    private static int parseId(String v) {
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String trim(String v) {
        return v == null ? "" : v.trim();
    }
}
