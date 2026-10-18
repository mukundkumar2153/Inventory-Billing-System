# SmartStore – POS & Inventory Billing System

SmartStore is a simple web app for a small shop. It helps the shop owner to manage products, check stock, make bills for customers, and see daily sales.

It is built with **Java (Servlet)**, an embedded **Tomcat** server, and **MySQL**.

---

## Features

### 1. Dashboard
- Today's revenue (including GST)
- Total number of bills
- Total number of active products
- Low stock count
- Bar chart of sales for the last 7 days
- Low stock warnings (stock 5 or less, and "Out of Stock" items)
- Last 5 bills with a "View" button

### 2. POS Billing
- Search products by name, barcode or category
- Add products to the cart with quantity
- Increase, decrease or remove items in the cart
- Clear the full cart
- Stock check, so you cannot add more than what is available
- Enter customer name (required) and phone (optional)
- Payment modes: **CASH, CARD, UPI**
- GST is calculated automatically for each item
- Generate the bill and open the invoice page for printing

### 3. Products & Stock
- Add a new product
- Edit an existing product
- Delete a product (soft delete – the product is hidden, not removed from the database)
- Search products by name, category or barcode
- Product details: name, category, barcode/SKU, price, GST %, stock quantity, unit (Pcs, Kg, Ltr, Box, etc.)

### 4. Safe billing
- Billing uses a database transaction. If anything fails, everything is rolled back.
- Stock is reduced only after the bill is saved successfully.

---

## Tech Stack

| Part | Technology |
|------|------------|
| Language | Java |
| Web layer | Jakarta Servlet 6 |
| Server | Embedded Apache Tomcat 10.1 |
| Database | MySQL |
| Database driver | MySQL Connector/J 8.3.0 |
| Frontend | HTML + CSS (pages are made by the servlet) |

---

## Project Files

| File | What it does |
|------|--------------|
| `App.java` | Starts the embedded Tomcat server on port 8080 and opens the browser |
| `Main.java` | Main servlet. Has all the pages and logic (dashboard, billing, products, invoice) |
| `index.html` | Template for the dashboard page |
| `style.css` | Styles for the whole app |
| `run.bat` / `start.bat` | Windows scripts to compile and run the app |
| `Smart-Store Product Management System.pdf` | Project document |
| `SmartStore_POS.pptx` | Project presentation |
| `SmartStore POS and Inventory Architecture.png` | Architecture diagram |
| `JAVA_Assignment-3.docx` | Assignment document |
| `StudentRegistration/` | Separate folder, not part of this app |

---

## Requirements

- Java JDK 17 or higher
- MySQL Server
- These JAR files (Maven can download them):
  - `tomcat-embed-core` 10.1.19
  - `jakarta.annotation-api` 2.1.1
  - `mysql-connector-j` 8.3.0

---

## Database Setup

1. Open MySQL and create the database:

```sql
CREATE DATABASE stock_billing;
USE stock_billing;
```

2. Create the tables. The columns below are taken from what the code uses. Change the types if you need to.

```sql
CREATE TABLE products (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  category VARCHAR(50) DEFAULT 'General',
  barcode VARCHAR(50),
  price DECIMAL(10,2) NOT NULL,
  gst DECIMAL(5,2) DEFAULT 5.00,
  quantity INT DEFAULT 0,
  unit VARCHAR(20) DEFAULT 'Pcs',
  active TINYINT DEFAULT 1
);

CREATE TABLE bills (
  id INT AUTO_INCREMENT PRIMARY KEY,
  customer VARCHAR(100) NOT NULL,
  phone VARCHAR(20),
  payment_mode VARCHAR(10) NOT NULL,
  bill_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE bill_items (
  id INT AUTO_INCREMENT PRIMARY KEY,
  bill_id INT NOT NULL,
  product_id INT NOT NULL,
  quantity INT NOT NULL,
  price DECIMAL(10,2) NOT NULL,
  gst_percent DECIMAL(5,2) NOT NULL,
  total DECIMAL(10,2) NOT NULL,
  gst_amount DECIMAL(10,2) NOT NULL,
  FOREIGN KEY (bill_id) REFERENCES bills(id),
  FOREIGN KEY (product_id) REFERENCES products(id)
);
```

---

## Configuration

The app reads the database settings from environment variables. If they are not set, it uses default values written in `Main.java`.

| Variable | Meaning | Default |
|----------|---------|---------|
| `DB_URL` | JDBC URL | `jdbc:mysql://localhost:3306/stock_billing?...` |
| `DB_USER` | MySQL username | `root` |
| `DB_PASSWORD` | MySQL password | set in `Main.java` |
| `PORT` | Server port | `8080` |

Please set your own `DB_PASSWORD` using an environment variable. Do not keep a real password inside the code.

---

## How to Run

### On Windows (easy way)

1. Start MySQL and create the database and tables (see above).
2. Open `run.bat` and check that the JAR paths in the `CP` line match your computer. Right now they point to a local `.m2` folder, so change them to your own path.
3. Double-click `run.bat` (or `start.bat`).
4. The script will compile the code, start the server and open your browser.

### Manual way

```bash
# compile
javac -cp ".;path/to/tomcat-embed-core-10.1.19.jar;path/to/jakarta.annotation-api-2.1.1.jar;path/to/mysql-connector-j-8.3.0.jar" Main.java App.java

# run
java -cp ".;path/to/tomcat-embed-core-10.1.19.jar;path/to/jakarta.annotation-api-2.1.1.jar;path/to/mysql-connector-j-8.3.0.jar" App
```

On Linux or Mac, use `:` instead of `;` in the classpath.

Then open: **http://localhost:8080/Main**

---

## App Pages

| Page | URL |
|------|-----|
| Dashboard | `/Main` |
| POS Billing | `/Main?action=billing` |
| Products & Stock | `/Main?action=products` |
| Add Product | `/Main?action=add` |
| Edit Product | `/Main?action=edit&id=1` |
| Invoice | `/Main?action=invoice&id=1` |

---

## Notes

- There is no login system yet. The app shows one user: "Store Owner (Admin)".
- Low stock limit is 5 items. You can change `LOW_STOCK` in `Main.java`.
- Deleted products are only marked inactive, so old bills stay safe.

---

## Future Ideas

- Login for admin and staff
- Sales reports (daily, monthly) with export
- Barcode scanner support
- Purchase and supplier management

---

## Author

**Mukund Kumar**
GitHub: [mukundkumar2153](https://github.com/mukundkumar2153)
