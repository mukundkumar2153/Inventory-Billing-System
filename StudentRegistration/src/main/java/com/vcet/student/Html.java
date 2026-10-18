package com.vcet.student;

/** Small helpers for generating HTML from servlets. */
public class Html {

    public static final String[] DEPARTMENTS = {
        "Computer Engineering (CO)",
        "Computer Science and Engineering - Data Science (CSE-DS)",
        "Information Technology (IT)",
        "Artificial Intelligence and Data Science (AI DS)",
        "Mechanical Engineering (ME)",
        "Electronics and Telecommunication Engineering (ETE)",
        "Civil Engineering (CE)"
    };

    public static boolean isValidDepartment(String d) {
        for (String x : DEPARTMENTS) {
            if (x.equals(d)) return true;
        }
        return false;
    }

    /** Escape text to prevent XSS. */
    public static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    public static String head(String title, String ctx) {
        return "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n"
                + "<meta charset=\"UTF-8\">\n"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n"
                + "<title>" + esc(title) + "</title>\n"
                + "<link rel=\"stylesheet\" href=\"" + ctx + "/css/style.css\">\n"
                + "</head>\n<body>\n"
                + "<header class=\"topbar\"><div class=\"topbar-inner\">"
                + "<span class=\"brand\">VCET &middot; Student Portal</span>"
                + "<nav><a href=\"" + ctx + "/index.html\">Register</a>"
                + "<a href=\"" + ctx + "/students\">All Students</a></nav>"
                + "</div></header>\n"
                + "<main class=\"container\">\n";
    }

    public static String foot() {
        return "</main>\n</body>\n</html>";
    }

    public static String message(boolean success, String text) {
        return "<div class=\"alert " + (success ? "alert-success" : "alert-error") + "\">"
                + (success ? "&#10004; " : "&#10006; ") + esc(text) + "</div>\n";
    }

    /** Department <option> list with the given value pre-selected. */
    public static String departmentOptions(String selected) {
        StringBuilder sb = new StringBuilder();
        sb.append("<option value=\"\">-- Select Department --</option>\n");
        for (String d : DEPARTMENTS) {
            sb.append("<option value=\"").append(esc(d)).append("\"");
            if (d.equals(selected)) sb.append(" selected");
            sb.append(">").append(esc(d)).append("</option>\n");
        }
        return sb.toString();
    }
}
