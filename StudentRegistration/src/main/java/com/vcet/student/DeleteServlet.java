package com.vcet.student;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Deletes a student by id (POST only), then returns to the list. */
@WebServlet("/delete")
public class DeleteServlet extends HttpServlet {

    private final StudentDAO dao = new StudentDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.sendRedirect(req.getContextPath() + "/students");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            int id = Integer.parseInt(req.getParameter("id"));
            if (dao.delete(id)) {
                UpdateServlet.redirect(req, resp, true, "Student record deleted successfully.");
            } else {
                UpdateServlet.redirect(req, resp, false, "Record not found; nothing was deleted.");
            }
        } catch (NumberFormatException e) {
            UpdateServlet.redirect(req, resp, false, "Invalid student id.");
        } catch (SQLException e) {
            UpdateServlet.redirect(req, resp, false, "Database error: " + e.getMessage());
        }
    }
}
