package com.vcet.student;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Displays all registered students in a table with Edit / Delete actions. */
@WebServlet("/students")
public class ListServlet extends HttpServlet {

    private final StudentDAO dao = new StudentDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html;charset=UTF-8");
        String ctx = req.getContextPath();

        String msg = req.getParameter("msg");
        boolean ok = "success".equals(req.getParameter("type"));

        try (PrintWriter out = resp.getWriter()) {
            out.print(Html.head("All Students", ctx));
            out.print("<section class=\"card\"><h1>Registered Students</h1>");
            if (msg != null && !msg.isEmpty()) {
                out.print(Html.message(ok, msg));
            }

            try {
                List<Student> list = dao.getAll();
                if (list.isEmpty()) {
                    out.print("<p class=\"empty\">No students registered yet.</p>");
                } else {
                    out.print("<p class=\"count\">Total records: <strong>" + list.size() + "</strong></p>");
                    out.print("<div class=\"table-wrap\"><table class=\"data\">"
                            + "<thead><tr><th>#</th><th>Name</th><th>Register No.</th><th>Email</th>"
                            + "<th>Department</th><th>Mobile</th><th>Actions</th></tr></thead><tbody>");
                    int n = 1;
                    for (Student s : list) {
                        out.print("<tr>"
                                + "<td>" + (n++) + "</td>"
                                + "<td>" + Html.esc(s.getName()) + "</td>"
                                + "<td>" + Html.esc(s.getRegisterNumber()) + "</td>"
                                + "<td>" + Html.esc(s.getEmail()) + "</td>"
                                + "<td>" + Html.esc(s.getDepartment()) + "</td>"
                                + "<td>" + Html.esc(s.getMobile()) + "</td>"
                                + "<td class=\"row-actions\">"
                                + "<a class=\"btn btn-small\" href=\"" + ctx + "/update?id=" + s.getId() + "\">Edit</a>"
                                + "<form method=\"post\" action=\"" + ctx + "/delete\" "
                                + "onsubmit=\"return confirm('Delete this student record?');\">"
                                + "<input type=\"hidden\" name=\"id\" value=\"" + s.getId() + "\">"
                                + "<button type=\"submit\" class=\"btn btn-small btn-danger\">Delete</button>"
                                + "</form></td></tr>");
                    }
                    out.print("</tbody></table></div>");
                }
            } catch (SQLException e) {
                out.print(Html.message(false, "Could not load records: " + e.getMessage()));
            }

            out.print("<div class=\"actions\"><a class=\"btn\" href=\"" + ctx + "/index.html\">+ New Registration</a></div>");
            out.print("</section>");
            out.print(Html.foot());
        }
    }
}
