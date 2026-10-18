import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

@WebServlet(urlPatterns = {"/Main", "/main"})
public class Main extends HttpServlet {

    private static final String URL = env("DB_URL",
            "jdbc:mysql://localhost:3306/stock_billing?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8");
    private static final String USER = env("DB_USER", "root");
    private static final String PASSWORD = env("DB_PASSWORD", "viva123");

    private static final int LOW_STOCK = 5;
    private static final List<String> PAYMENTS = List.of("CASH", "CARD", "UPI");

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ignored) {
        }
    }

    private static String env(String key, String def) {
        String v = System.getenv(key);
        return v == null ? def : v;
    }

    private static class Line {
        int id, stock, qty;
        String name, unit;
        BigDecimal price, gstPct;

        BigDecimal subtotal() {
            return price.multiply(BigDecimal.valueOf(qty));
        }

        BigDecimal gst() {
            return subtotal().multiply(gstPct)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    private String template() throws IOException {
        InputStream in = null;
        try {
            if (getServletContext() != null) {
                in = getServletContext().getResourceAsStream("/index.html");
                if (in == null) {
                    in = getServletContext().getResourceAsStream("index.html");
                }
            }
        } catch (Exception ignored) {
        }

        if (in == null) {
            in = Main.class.getResourceAsStream("/index.html");
        }
        if (in == null) {
            in = Main.class.getClassLoader().getResourceAsStream("index.html");
        }
        if (in == null) {
            File f = new File("index.html");
            if (f.exists() && f.isFile()) {
                in = new FileInputStream(f);
            }
        }

        if (in != null) {
            try (InputStream input = in) {
                return new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        // Built-in fallback template
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1'>"
                + "<title>SmartStore POS &amp; Inventory</title>"
                + "<link rel='stylesheet' href='style.css'></head><body>"
                + "<header class='navbar'>"
                + "<div class='logo'><div class='logo-box'>▣</div><div><h2>SmartStore</h2><small>POS &amp; Inventory</small></div></div>"
                + "<nav class='nav'>"
                + "<a href='Main' class='active'>▥ Dashboard</a>"
                + "<a href='Main?action=billing'>🛒 POS Billing</a>"
                + "<a href='Main?action=products'>▦ Products &amp; Stock</a>"
                + "</nav>"
                + "<div class='admin'>Store Owner (Admin)<br><b>OWNER (ADMIN)</b></div>"
                + "</header>"
                + "<div class='container'>"
                + "<div class='heading'><h1>📈 Business Analytics Dashboard</h1><p>Real-time overview of sales, transactions, and stock health</p></div>"
                + "<div class='cards'>"
                + "<div class='card stat'><div class='icon'>₹</div><div><small>TODAY'S REVENUE</small><h2>₹{{REVENUE}}</h2></div></div>"
                + "<div class='card stat'><div class='icon'>🛒</div><div><small>TOTAL BILLS</small><h2>{{BILLS}}</h2></div></div>"
                + "<div class='card stat'><div class='icon'>▦</div><div><small>TOTAL PRODUCTS</small><h2>{{PRODUCTS}}</h2></div></div>"
                + "<div class='card stat'><div class='icon'>⚠</div><div><small>LOW STOCK</small><h2>{{LOW_STOCK}}</h2></div></div>"
                + "</div>"
                + "<div class='grid'>"
                + "<div class='card'><div class='section-title'><h2>Daily Sales Revenue (Last 7 Days)</h2></div>{{CHART}}</div>"
                + "<div class='card'><div class='section-title'><h2>⚠ Low Stock Warnings</h2></div>{{LOW_STOCK_LIST}}</div>"
                + "</div>"
                + "<div class='card table-card'><div class='section-title'><h2>Recent Billed Transactions</h2><a href='Main?action=billing' class='btn'>+ New Bill</a></div>"
                + "<table><tr><th>Invoice</th><th>Customer</th><th>Payment</th><th>Total</th><th>Action</th></tr>{{RECENT_BILLS}}</table>"
                + "</div></div></body></html>";
    }

    private static String esc(String s) {
        if (s == null)
            return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String money(BigDecimal v) {
        if (v == null)
            return "0.00";
        return v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static int parseInt(HttpServletRequest req, String name) {
        String val = trim(req.getParameter(name));
        return val.isEmpty() ? 0 : Integer.parseInt(val);
    }

    private String navLink(String href, String label, boolean on) {
        return "<a href='" + href + "'" + (on ? " class='active'" : "") + ">" + label + "</a>";
    }

    private String page(String active, String title, String body) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1'>"
                + "<title>" + esc(title) + " - SmartStore</title>"
                + "<link rel='stylesheet' href='style.css'></head><body>"
                + "<header class='navbar'>"
                + "<div class='logo'><div class='logo-box'>▣</div>"
                + "<div><h2>SmartStore</h2><small>POS &amp; Inventory</small></div></div>"
                + "<nav class='nav'>"
                + navLink("Main", "▥ Dashboard", "dashboard".equals(active))
                + navLink("Main?action=billing", "🛒 POS Billing", "billing".equals(active))
                + navLink("Main?action=products", "▦ Products &amp; Stock", "products".equals(active))
                + "</nav>"
                + "<div class='admin'>Store Owner (Admin)<br><b>OWNER (ADMIN)</b></div>"
                + "</header>"
                + "<div class='container'>" + body + "</div>"
                + "</body></html>";
    }

    private void error(HttpServletResponse resp, Exception e) throws IOException {
        String msg;
        if (e instanceof IllegalArgumentException) {
            msg = e instanceof NumberFormatException
                    ? "Invalid input. Please check the numerical values entered."
                    : e.getMessage();
            resp.setStatus(400);
        } else {
            log("Request failed", e);
            msg = "An error occurred: " + e.getMessage();
            resp.setStatus(500);
        }
        resp.getWriter().println(page("", "Error",
                "<div class='card empty'><h2>" + esc(msg) + "</h2>"
                        + "<p style='margin-top:15px;'><a class='btn' href='Main'>Back to Dashboard</a> "
                        + "<a class='btn light' href='Main?action=billing'>Back to Billing</a></p></div>"));
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, Integer> cart(HttpServletRequest req) {
        HttpSession s = req.getSession();
        Map<Integer, Integer> c = (Map<Integer, Integer>) s.getAttribute("cart");
        if (c == null) {
            c = new TreeMap<>();
            s.setAttribute("cart", c);
        }
        return c;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String uri = req.getRequestURI();
        if (uri != null && uri.endsWith("style.css")) {
            resp.setContentType("text/css;charset=UTF-8");
            File f = new File("style.css");
            if (f.exists() && f.isFile()) {
                try (FileInputStream fis = new FileInputStream(f)) {
                    fis.transferTo(resp.getOutputStream());
                    return;
                }
            } else if (getServletContext() != null) {
                try (InputStream in = getServletContext().getResourceAsStream("/style.css")) {
                    if (in != null) {
                        in.transferTo(resp.getOutputStream());
                        return;
                    }
                }
            }
        }

        req.setCharacterEncoding("UTF-8");
        resp.setContentType("text/html;charset=UTF-8");

        String action = trim(req.getParameter("action"));

        try {
            switch (action) {
                case "billing":
                    billingPage(req, resp);
                    break;
                case "products":
                    productsPage(req, resp);
                    break;
                case "add":
                    productFormPage(resp, null);
                    break;
                case "edit":
                    editProductPage(req, resp);
                    break;
                case "invoice":
                    invoicePage(req, resp);
                    break;
                default:
                    dashboard(resp);
            }
        } catch (Exception e) {
            error(resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        resp.setContentType("text/html;charset=UTF-8");

        String action = trim(req.getParameter("action"));

        try {
            if (action.startsWith("cart")) {
                cartAction(req, action);
                resp.sendRedirect("Main?action=billing");
                return;
            }

            switch (action) {
                case "bill":
                    createBill(req, resp);
                    break;
                case "save":
                    saveProduct(req, resp);
                    break;
                case "delete":
                    deleteProduct(req, resp);
                    break;
                default:
                    resp.sendRedirect("Main");
            }
        } catch (Exception e) {
            error(resp, e);
        }
    }

    private void cartAction(HttpServletRequest req, String action) throws Exception {
        switch (action) {
            case "cartAdd": {
                int q = parseInt(req, "quantity");
                if (q < 1)
                    q = 1;
                changeCart(req, parseInt(req, "productId"), q);
                break;
            }
            case "cartInc":
                changeCart(req, parseInt(req, "productId"), 1);
                break;
            case "cartDec":
                changeCart(req, parseInt(req, "productId"), -1);
                break;
            case "cartRemove":
                cart(req).remove(parseInt(req, "productId"));
                break;
            case "cartClear":
                cart(req).clear();
                break;
            default:
                break;
        }
    }

    private void changeCart(HttpServletRequest req, int pid, int delta) throws Exception {
        Map<Integer, Integer> cart = cart(req);
        int qty = cart.getOrDefault(pid, 0) + delta;

        if (qty <= 0) {
            cart.remove(pid);
            return;
        }

        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT name,quantity FROM products WHERE id=? AND active=1")) {

            ps.setInt(1, pid);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next())
                    throw new IllegalArgumentException("Product not found or inactive.");
                int available = rs.getInt("quantity");
                if (qty > available)
                    throw new IllegalArgumentException("Only " + available
                            + " items in stock for \"" + rs.getString("name") + "\"");
            }
        }

        cart.put(pid, qty);
    }

    private List<Line> cartLines(Connection con, Map<Integer, Integer> cart, boolean lock)
            throws SQLException {

        List<Line> lines = new ArrayList<>();
        if (cart.isEmpty())
            return lines;

        String sql = "SELECT name,price,gst,quantity,unit FROM products WHERE id=? AND active=1"
                + (lock ? " FOR UPDATE" : "");

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            Iterator<Map.Entry<Integer, Integer>> it = cart.entrySet().iterator();

            while (it.hasNext()) {
                Map.Entry<Integer, Integer> e = it.next();
                ps.setInt(1, e.getKey());

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        it.remove();
                        continue;
                    }

                    Line l = new Line();
                    l.id = e.getKey();
                    l.qty = e.getValue();
                    l.name = rs.getString("name");
                    l.unit = rs.getString("unit");
                    l.price = rs.getBigDecimal("price");
                    l.gstPct = rs.getBigDecimal("gst");
                    l.stock = rs.getInt("quantity");
                    lines.add(l);
                }
            }
        }

        return lines;
    }

    private int scalarInt(Connection con, String sql) throws SQLException {
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private BigDecimal scalarMoney(Connection con, String sql) throws SQLException {
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() && rs.getBigDecimal(1) != null ? rs.getBigDecimal(1) : BigDecimal.ZERO;
        }
    }

    private void dashboard(HttpServletResponse resp) throws Exception {

        BigDecimal revenue;
        int bills, products, lowCount = 0;
        LocalDate today;
        StringBuilder lowList = new StringBuilder();
        StringBuilder recent = new StringBuilder();
        Map<LocalDate, BigDecimal> daily = new LinkedHashMap<>();

        try (Connection con = connect()) {

            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery("SELECT CURDATE()")) {
                if (rs.next() && rs.getDate(1) != null) {
                    today = rs.getDate(1).toLocalDate();
                } else {
                    today = LocalDate.now();
                }
            }

            // Total revenue including GST for today
            revenue = scalarMoney(con,
                    "SELECT COALESCE(SUM(bi.total + bi.gst_amount), 0) FROM bill_items bi "
                            + "JOIN bills b ON bi.bill_id=b.id "
                            + "WHERE DATE(b.bill_date)=CURDATE()");

            bills = scalarInt(con, "SELECT COUNT(*) FROM bills");
            products = scalarInt(con, "SELECT COUNT(*) FROM products WHERE active=1");

            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT name,quantity,unit FROM products "
                            + "WHERE active=1 AND quantity<=? ORDER BY quantity ASC")) {

                ps.setInt(1, LOW_STOCK);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        lowCount++;
                        int q = rs.getInt("quantity");
                        String pillClass = q == 0 ? "pill danger" : "pill low";
                        String statusLabel = q == 0 ? "Out of Stock" : q + " " + esc(rs.getString("unit")) + " left";
                        lowList.append("<div class='warning'><b>")
                                .append(esc(rs.getString("name")))
                                .append("</b><span class='").append(pillClass).append("'>")
                                .append(statusLabel)
                                .append("</span></div>");
                    }
                }
            }

            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT b.id,b.customer,b.payment_mode,b.bill_date,"
                                 + "COALESCE(SUM(bi.total+bi.gst_amount),0) total "
                                 + "FROM bills b LEFT JOIN bill_items bi ON b.id=bi.bill_id "
                                 + "GROUP BY b.id,b.customer,b.payment_mode,b.bill_date "
                                 + "ORDER BY b.id DESC LIMIT 5")) {

                while (rs.next()) {
                    recent.append("<tr><td>#").append(rs.getInt("id"))
                            .append("</td><td>").append(esc(rs.getString("customer")))
                            .append("</td><td><span class='badge'>").append(esc(rs.getString("payment_mode")))
                            .append("</span></td><td>₹").append(money(rs.getBigDecimal("total")))
                            .append("</td><td><a class='btn light' href='Main?action=invoice&id=")
                            .append(rs.getInt("id")).append("'>View</a></td></tr>");
                }
            }

            for (int i = 6; i >= 0; i--)
                daily.put(today.minusDays(i), BigDecimal.ZERO);

            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT DATE(b.bill_date) d, COALESCE(SUM(bi.total+bi.gst_amount),0) v "
                                 + "FROM bills b JOIN bill_items bi ON bi.bill_id=b.id "
                                 + "WHERE DATE(b.bill_date) >= CURDATE() - INTERVAL 6 DAY "
                                 + "GROUP BY DATE(b.bill_date)")) {

                while (rs.next()) {
                    java.sql.Date d = rs.getDate("d");
                    if (d != null) {
                        LocalDate ld = d.toLocalDate();
                        if (daily.containsKey(ld))
                            daily.put(ld, rs.getBigDecimal("v"));
                    }
                }
            }
        }

        if (lowList.length() == 0)
            lowList.append("<div class='empty'>✓ All products are well stocked</div>");

        if (recent.length() == 0)
            recent.append("<tr><td colspan='5' class='empty'>No bills generated yet</td></tr>");

        BigDecimal max = daily.values().stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        StringBuilder chart = new StringBuilder("<div class='chart'>");

        for (Map.Entry<LocalDate, BigDecimal> e : daily.entrySet()) {
            int pct = max.signum() == 0 ? 0
                    : e.getValue().multiply(BigDecimal.valueOf(100))
                    .divide(max, 0, RoundingMode.HALF_UP).intValue();

            chart.append("<div class='bar-col'><small>₹")
                    .append(e.getValue().setScale(0, RoundingMode.HALF_UP).toPlainString())
                    .append("</small><div class='bar-area'><div class='bar' style='height:")
                    .append(pct).append("%'></div></div><small>")
                    .append(e.getKey().getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                    .append("</small></div>");
        }
        chart.append("</div>");

        String html = template()
                .replace("{{REVENUE}}", money(revenue))
                .replace("{{BILLS}}", String.valueOf(bills))
                .replace("{{PRODUCTS}}", String.valueOf(products))
                .replace("{{LOW_STOCK}}", String.valueOf(lowCount))
                .replace("{{CHART}}", chart.toString())
                .replace("{{LOW_STOCK_LIST}}", lowList.toString())
                .replace("{{RECENT_BILLS}}", recent.toString());

        resp.getWriter().println(html);
    }

    private void productsPage(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        String search = trim(req.getParameter("search"));
        StringBuilder rows = new StringBuilder();

        String sql = "SELECT id,name,category,barcode,price,gst,quantity,unit "
                + "FROM products WHERE active=1 AND (name LIKE ? OR barcode LIKE ? OR category LIKE ?) "
                + "ORDER BY id DESC";

        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            String term = "%" + search + "%";
            ps.setString(1, term);
            ps.setString(2, term);
            ps.setString(3, term);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int qty = rs.getInt("quantity");
                    String pillClass = qty == 0 ? "pill danger" : (qty <= LOW_STOCK ? "pill low" : "pill");
                    String barcode = trim(rs.getString("barcode"));

                    rows.append("<tr><td>📦</td><td><b>").append(esc(rs.getString("name")))
                            .append("</b></td><td>").append(esc(rs.getString("category")))
                            .append("</td><td><code>").append(barcode.isEmpty() ? "-" : esc(barcode))
                            .append("</code></td><td>₹").append(money(rs.getBigDecimal("price")))
                            .append("</td><td>").append(money(rs.getBigDecimal("gst")))
                            .append("%</td><td><span class='").append(pillClass).append("'>")
                            .append(qty).append(' ').append(esc(rs.getString("unit")))
                            .append("</span></td><td style='white-space:nowrap'>")
                            .append("<a class='btn light' style='padding:6px 12px;margin-right:6px;' href='Main?action=edit&id=")
                            .append(rs.getInt("id")).append("'>Edit</a>")
                            .append("<form method='post' action='Main' style='display:inline;' ")
                            .append("onsubmit='return confirm(\"Are you sure you want to delete this product?\")'>")
                            .append("<input type='hidden' name='action' value='delete'>")
                            .append("<input type='hidden' name='id' value='").append(rs.getInt("id")).append("'>")
                            .append("<button class='btn red' style='padding:6px 12px;'>Delete</button></form></td></tr>");
                }
            }
        }

        if (rows.length() == 0)
            rows.append("<tr><td colspan='8' class='empty'>No products found</td></tr>");

        String body = "<div class='heading'><h1>▦ Product &amp; Stock Management</h1>"
                + "<p>Manage store inventory, prices, and stock levels</p></div>"
                + "<div style='display:flex;gap:10px;justify-content:space-between;align-items:center;flex-wrap:wrap;'>"
                + "<a class='btn' href='Main?action=add'>+ Add New Product</a>"
                + "<form class='search' method='get' action='Main' style='margin:0;max-width:400px;flex:1;'>"
                + "<input type='hidden' name='action' value='products'>"
                + "<input name='search' placeholder='Search by name, category, or barcode...' value='" + esc(search) + "'>"
                + "<button class='btn'>Search</button>"
                + (search.isEmpty() ? "" : "<a class='btn light' href='Main?action=products'>Reset</a>")
                + "</form></div>"
                + "<div class='card table-card' style='margin-top:16px'><table>"
                + "<tr><th>Icon</th><th>Product</th><th>Category</th><th>Barcode</th>"
                + "<th>Price</th><th>GST</th><th>Stock</th><th>Action</th></tr>"
                + rows
                + "</table></div>";

        resp.getWriter().println(page("products", "Products", body));
    }

    private static class ProductData {
        int id;
        String name = "";
        String category = "General";
        String barcode = "";
        BigDecimal price = BigDecimal.ZERO;
        BigDecimal gst = new BigDecimal("5.00");
        int quantity = 0;
        String unit = "Pcs";
    }

    private void editProductPage(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        int id = parseInt(req, "id");
        ProductData p = new ProductData();

        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT id,name,category,barcode,price,gst,quantity,unit FROM products WHERE id=? AND active=1")) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Product not found with ID " + id);
                }
                p.id = rs.getInt("id");
                p.name = rs.getString("name");
                p.category = rs.getString("category");
                p.barcode = rs.getString("barcode");
                p.price = rs.getBigDecimal("price");
                p.gst = rs.getBigDecimal("gst");
                p.quantity = rs.getInt("quantity");
                p.unit = rs.getString("unit");
            }
        }

        productFormPage(resp, p);
    }

    private void productFormPage(HttpServletResponse resp, ProductData p) throws IOException {
        boolean isEdit = (p != null && p.id > 0);
        String title = isEdit ? "Edit Product (#" + p.id + ")" : "Add New Product";
        String subtitle = isEdit ? "Update product and stock information" : "Enter product details to add to inventory";

        String name = isEdit ? esc(p.name) : "";
        String category = isEdit ? esc(p.category) : "General";
        String barcode = isEdit ? esc(p.barcode) : "";
        String price = isEdit ? p.price.toPlainString() : "";
        String gst = isEdit ? p.gst.toPlainString() : "5.00";
        String quantity = isEdit ? String.valueOf(p.quantity) : "10";
        String unit = isEdit ? p.unit : "Pcs";

        String[] units = {"Pcs", "Pouch", "Strip", "Box", "Kg", "Ltr", "Packet", "Bottle", "Meter"};
        StringBuilder unitOptions = new StringBuilder();
        for (String u : units) {
            unitOptions.append("<option value='").append(u).append("'")
                    .append(u.equalsIgnoreCase(unit) ? " selected" : "")
                    .append(">").append(u).append("</option>");
        }

        String body = "<div class='heading'><h1>" + title + "</h1><p>" + subtitle + "</p></div>"
                + "<div class='card' style='max-width:640px'>"
                + "<form method='post' action='Main' class='form-grid'>"
                + "<input type='hidden' name='action' value='save'>"
                + (isEdit ? "<input type='hidden' name='id' value='" + p.id + "'>" : "")
                + "<div class='full'><label><b>Product Name *</b></label><br>"
                + "<input class='full' name='name' placeholder='e.g., Wireless Mouse' value='" + name + "' required maxlength='100'></div>"
                + "<div><label><b>Category *</b></label><br>"
                + "<input name='category' placeholder='e.g., Electronics' value='" + category + "' required maxlength='50'></div>"
                + "<div><label><b>Barcode / SKU</b></label><br>"
                + "<input name='barcode' placeholder='e.g., 1004' value='" + barcode + "' maxlength='50'></div>"
                + "<div><label><b>Price (₹) *</b></label><br>"
                + "<input name='price' type='number' step='0.01' min='0' placeholder='0.00' value='" + price + "' required></div>"
                + "<div><label><b>GST (%) *</b></label><br>"
                + "<input name='gst' type='number' step='0.01' min='0' max='100' value='" + gst + "' placeholder='5.00' required></div>"
                + "<div><label><b>Stock Quantity *</b></label><br>"
                + "<input name='quantity' type='number' min='0' placeholder='0' value='" + quantity + "' required></div>"
                + "<div><label><b>Unit *</b></label><br>"
                + "<select name='unit'>" + unitOptions + "</select></div>"
                + "<div class='full' style='margin-top:10px;'>"
                + "<button class='btn' type='submit'>" + (isEdit ? "Update Product" : "Save Product") + "</button> "
                + "<a class='btn light' href='Main?action=products'>Cancel</a></div>"
                + "</form></div>";

        resp.getWriter().println(page("products", title, body));
    }

    private void saveProduct(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        int id = parseInt(req, "id");
        String name = trim(req.getParameter("name"));
        if (name.isEmpty())
            throw new IllegalArgumentException("Product name is required");

        String category = trim(req.getParameter("category"));
        if (category.isEmpty())
            category = "General";

        String barcode = trim(req.getParameter("barcode"));
        if (barcode.isEmpty())
            barcode = null;

        String priceStr = trim(req.getParameter("price"));
        if (priceStr.isEmpty())
            throw new IllegalArgumentException("Product price is required");
        BigDecimal price = new BigDecimal(priceStr);
        if (price.signum() < 0)
            throw new IllegalArgumentException("Price cannot be negative");

        String g = trim(req.getParameter("gst"));
        BigDecimal gst = g.isEmpty() ? new BigDecimal("5.00") : new BigDecimal(g);
        if (gst.signum() < 0 || gst.compareTo(BigDecimal.valueOf(100)) > 0)
            throw new IllegalArgumentException("GST must be between 0% and 100%");

        int quantity = parseInt(req, "quantity");
        if (quantity < 0)
            throw new IllegalArgumentException("Quantity cannot be negative");

        String unit = trim(req.getParameter("unit"));
        if (unit.isEmpty())
            unit = "Pcs";

        try (Connection con = connect()) {
            if (id > 0) {
                // Update existing product
                String sql = "UPDATE products SET name=?, category=?, barcode=?, price=?, gst=?, quantity=?, unit=? WHERE id=? AND active=1";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, name);
                    ps.setString(2, category);
                    ps.setString(3, barcode);
                    ps.setBigDecimal(4, price);
                    ps.setBigDecimal(5, gst);
                    ps.setInt(6, quantity);
                    ps.setString(7, unit);
                    ps.setInt(8, id);
                    ps.executeUpdate();
                }
            } else {
                // Insert new product
                String sql = "INSERT INTO products (name,category,barcode,price,gst,quantity,unit,active) VALUES(?,?,?,?,?,?,?,1)";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, name);
                    ps.setString(2, category);
                    ps.setString(3, barcode);
                    ps.setBigDecimal(4, price);
                    ps.setBigDecimal(5, gst);
                    ps.setInt(6, quantity);
                    ps.setString(7, unit);
                    ps.executeUpdate();
                }
            }
        }

        resp.sendRedirect("Main?action=products");
    }

    private void deleteProduct(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        int id = parseInt(req, "id");
        if (id <= 0)
            throw new IllegalArgumentException("Invalid product ID");

        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement("UPDATE products SET active=0 WHERE id=?")) {

            ps.setInt(1, id);
            ps.executeUpdate();
        }

        cart(req).remove(id);
        resp.sendRedirect("Main?action=products");
    }

    private void billingPage(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        String search = trim(req.getParameter("search"));
        StringBuilder cards = new StringBuilder();
        List<Line> lines;

        try (Connection con = connect()) {

            String sql = "SELECT id,name,category,price,quantity,unit FROM products WHERE active=1 ";
            if (!search.isEmpty()) {
                sql += "AND (name LIKE ? OR barcode LIKE ? OR category LIKE ?) ";
            }
            sql += "ORDER BY name ASC";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                if (!search.isEmpty()) {
                    String term = "%" + search + "%";
                    ps.setString(1, term);
                    ps.setString(2, term);
                    ps.setString(3, term);
                }

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int stock = rs.getInt("quantity");

                        cards.append("<div class='product'><div class='product-image'>📦</div>")
                                .append("<div class='product-info'><h3>").append(esc(rs.getString("name")))
                                .append("</h3><span class='category-tag'>").append(esc(rs.getString("category")))
                                .append("</span><br><span style='font-size:12px;color:#7b8492;'>").append(stock).append(' ')
                                .append(esc(rs.getString("unit"))).append(" in stock</span>")
                                .append("<div class='price'>₹").append(money(rs.getBigDecimal("price")))
                                .append("</div></div>");

                        if (stock > 0) {
                            cards.append("<form method='post' action='Main'>")
                                    .append("<input type='hidden' name='action' value='cartAdd'>")
                                    .append("<input type='hidden' name='productId' value='")
                                    .append(rs.getInt("id")).append("'>")
                                    .append("<input type='number' name='quantity' value='1' min='1' max='")
                                    .append(stock).append("' style='width:60px;margin-right:4px;'> ")
                                    .append("<button class='btn' style='padding:8px 14px;'>Add</button></form>");
                        } else {
                            cards.append("<span class='pill danger'>Out of stock</span>");
                        }

                        cards.append("</div>");
                    }
                }
            }

            if (cards.length() == 0) {
                cards.append("<div class='empty' style='grid-column:1/-1;'>No products match your search.</div>");
            }

            lines = cartLines(con, cart(req), false);
        }

        BigDecimal sub = BigDecimal.ZERO;
        BigDecimal gst = BigDecimal.ZERO;
        StringBuilder cartHtml = new StringBuilder();

        for (Line l : lines) {
            sub = sub.add(l.subtotal());
            gst = gst.add(l.gst());

            cartHtml.append("<div class='cart-item'><div class='cart-name'><b>")
                    .append(esc(l.name)).append("</b><br><small>₹").append(money(l.price))
                    .append(" × ").append(l.qty).append(" ").append(esc(l.unit))
                    .append(" (GST ").append(money(l.gstPct)).append("%)</small></div>")
                    .append("<form method='post' action='Main' class='qty'>")
                    .append("<input type='hidden' name='productId' value='").append(l.id).append("'>")
                    .append("<button type='submit' name='action' value='cartDec' title='Decrease'>−</button>")
                    .append("<span style='font-weight:bold;min-width:20px;text-align:center;'>").append(l.qty).append("</span>")
                    .append("<button type='submit' name='action' value='cartInc' title='Increase'>+</button>")
                    .append("<button type='submit' name='action' value='cartRemove' class='remove-btn' title='Remove'>✕</button></form>")
                    .append("<b style='min-width:75px;text-align:right;'>₹").append(money(l.subtotal())).append("</b></div>");
        }

        boolean empty = lines.isEmpty();

        if (empty)
            cartHtml.append("<div class='empty'>Cart is empty. Add products from the list.</div>");

        String totals = "<div class='total'>"
                + "<div class='line'><span>Subtotal</span><span>₹" + money(sub) + "</span></div>"
                + "<div class='line'><span>GST Amount</span><span>₹" + money(gst) + "</span></div>"
                + "<div class='line grand'><span>Grand Total</span><span>₹" + money(sub.add(gst)) + "</span></div>"
                + "</div>";

        StringBuilder pay = new StringBuilder();
        for (String p : PAYMENTS)
            pay.append("<option value='").append(p).append("'>").append(p).append("</option>");

        String checkout = "<form method='post' action='Main' style='margin-top:18px'>"
                + "<input name='customer' placeholder='Customer Name *' required maxlength='100'>"
                + "<input name='phone' placeholder='Customer Phone (optional)' maxlength='20'>"
                + "<label style='font-size:12px;font-weight:bold;color:#687384;'>Payment Mode</label>"
                + "<select name='payment'>" + pay + "</select>"
                + "<button class='btn green' name='action' value='bill'" + (empty ? " disabled" : "")
                + " style='width:100%;margin-top:8px;'>✓ Generate &amp; Print Bill</button> "
                + "<button class='btn red' name='action' value='cartClear' formnovalidate"
                + (empty ? " disabled" : "") + " style='width:100%;margin-top:8px;'>Clear Cart</button></form>";

        String body = "<div class='heading'><h1>🛒 POS Billing System</h1>"
                + "<p>Search products, manage cart, and complete customer checkout</p></div>"
                + "<form class='search' method='get' action='Main' style='margin-bottom:18px'>"
                + "<input type='hidden' name='action' value='billing'>"
                + "<input name='search' placeholder='Search products by name, barcode, or category...' value='" + esc(search) + "'>"
                + "<button class='btn'>Filter</button>"
                + (search.isEmpty() ? "" : "<a class='btn light' href='Main?action=billing'>Clear</a>")
                + "</form>"
                + "<div class='pos'><div><div class='products'>" + cards + "</div></div>"
                + "<div class='card cart'><h2>🛒 Current Cart</h2>"
                + cartHtml + totals + checkout + "</div></div>";

        resp.getWriter().println(page("billing", "POS Billing", body));
    }

    private void createBill(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        String customer = trim(req.getParameter("customer"));
        if (customer.isEmpty())
            throw new IllegalArgumentException("Customer name is required");

        String phone = trim(req.getParameter("phone"));
        String payment = trim(req.getParameter("payment"));
        if (!PAYMENTS.contains(payment))
            throw new IllegalArgumentException("Invalid payment mode: " + payment);

        Map<Integer, Integer> cart = cart(req);
        if (cart.isEmpty())
            throw new IllegalArgumentException("Cart is empty");

        int billId = 0;
        Connection con = null;

        try {
            con = connect();
            con.setAutoCommit(false);

            List<Line> lines = cartLines(con, cart, true);

            if (lines.isEmpty())
                throw new IllegalArgumentException("Cart is empty or selected products are no longer available");

            for (Line l : lines) {
                if (l.qty > l.stock)
                    throw new IllegalArgumentException("Insufficient stock for \""
                            + l.name + "\" (Available: " + l.stock + ", In Cart: " + l.qty + ")");
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO bills (customer,phone,payment_mode) VALUES(?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {

                ps.setString(1, customer);
                ps.setString(2, phone.isEmpty() ? null : phone);
                ps.setString(3, payment);
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        billId = rs.getInt(1);
                    } else {
                        throw new SQLException("Failed to generate bill ID.");
                    }
                }
            }

            try (PreparedStatement item = con.prepareStatement(
                    "INSERT INTO bill_items "
                            + "(bill_id,product_id,quantity,price,gst_percent,total,gst_amount) "
                            + "VALUES(?,?,?,?,?,?,?)");
                 PreparedStatement stock = con.prepareStatement(
                         "UPDATE products SET quantity=quantity-? WHERE id=? AND quantity>=?")) {

                for (Line l : lines) {
                    item.setInt(1, billId);
                    item.setInt(2, l.id);
                    item.setInt(3, l.qty);
                    item.setBigDecimal(4, l.price);
                    item.setBigDecimal(5, l.gstPct);
                    item.setBigDecimal(6, l.subtotal());
                    item.setBigDecimal(7, l.gst());
                    item.executeUpdate();

                    stock.setInt(1, l.qty);
                    stock.setInt(2, l.id);
                    stock.setInt(3, l.qty);

                    if (stock.executeUpdate() != 1)
                        throw new IllegalArgumentException("Insufficient stock during checkout for " + l.name);
                }
            }

            con.commit();

        } catch (Exception e) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException ignored) {
                }
            }
            throw e;
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException ignored) {
                }
            }
        }

        cart.clear();
        resp.sendRedirect("Main?action=invoice&id=" + billId);
    }

    private void invoicePage(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        int id = parseInt(req, "id");
        if (id <= 0)
            throw new IllegalArgumentException("Invalid invoice ID");

        String customer = "", phone = "", payment = "", date = "";
        StringBuilder rows = new StringBuilder();
        BigDecimal sub = BigDecimal.ZERO;
        BigDecimal gst = BigDecimal.ZERO;

        try (Connection con = connect()) {

            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT customer,phone,payment_mode,bill_date FROM bills WHERE id=?")) {

                ps.setInt(1, id);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next())
                        throw new IllegalArgumentException("Invoice #" + id + " not found.");

                    customer = rs.getString("customer");
                    phone = rs.getString("phone");
                    payment = rs.getString("payment_mode");
                    Timestamp ts = rs.getTimestamp("bill_date");
                    date = ts != null
                            ? ts.toLocalDateTime().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH))
                            : "N/A";
                }
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT COALESCE(p.name, 'Item #' || bi.product_id) AS item_name, "
                            + "bi.quantity, bi.price, bi.gst_percent, bi.total, bi.gst_amount "
                            + "FROM bill_items bi LEFT JOIN products p ON p.id=bi.product_id "
                            + "WHERE bi.bill_id=? ORDER BY bi.id ASC")) {

                ps.setInt(1, id);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        BigDecimal itemTotal = rs.getBigDecimal("total");
                        BigDecimal itemGst = rs.getBigDecimal("gst_amount");
                        sub = sub.add(itemTotal);
                        gst = gst.add(itemGst);

                        rows.append("<tr><td>").append(esc(rs.getString("item_name")))
                                .append("</td><td>").append(rs.getInt("quantity"))
                                .append("</td><td>₹").append(money(rs.getBigDecimal("price")))
                                .append("</td><td>").append(money(rs.getBigDecimal("gst_percent")))
                                .append("% (₹").append(money(itemGst)).append(")")
                                .append("</td><td>₹").append(money(itemTotal.add(itemGst)))
                                .append("</td></tr>");
                    }
                }
            }
        }

        String body = "<div class='invoice'>"
                + "<div class='invoice-head'><div><h2>SmartStore</h2><small>POS &amp; Inventory Management</small></div>"
                + "<div style='text-align:right'><h2>Invoice #" + id + "</h2><small>" + esc(date) + "</small></div></div>"
                + "<div class='invoice-meta'>"
                + "<p><b>Customer Name:</b> " + esc(customer) + "<br>"
                + "<b>Contact Phone:</b> " + (phone == null || phone.isEmpty() ? "N/A" : esc(phone)) + "<br>"
                + "<b>Payment Mode:</b> <span class='badge'>" + esc(payment) + "</span></p>"
                + "</div>"
                + "<table><thead><tr><th>Item Description</th><th>Qty</th><th>Unit Price</th><th>GST</th><th>Net Amount</th></tr></thead>"
                + "<tbody>" + rows + "</tbody></table>"
                + "<div class='total'>"
                + "<div class='line'><span>Subtotal (Before Tax)</span><span>₹" + money(sub) + "</span></div>"
                + "<div class='line'><span>Total GST Tax</span><span>₹" + money(gst) + "</span></div>"
                + "<div class='line grand'><span>Grand Total (Paid)</span><span>₹" + money(sub.add(gst)) + "</span></div>"
                + "</div>"
                + "<div class='no-print' style='margin-top:25px;display:flex;gap:10px;justify-content:flex-end;'>"
                + "<button class='btn' onclick='window.print()'>🖨 Print Invoice</button> "
                + "<a class='btn light' href='Main?action=billing'>+ New Bill</a> "
                + "<a class='btn light' href='Main'>Dashboard</a></div>"
                + "</div>";

        resp.getWriter().println(page("", "Invoice #" + id, body));
    }
}