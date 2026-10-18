# Student Registration System

**Subject:** 2013611 - Full Stack Java Programming (SE / Sem III)
**Tech:** HTML, CSS, JavaScript, Java Servlet, JDBC, MySQL, Apache Tomcat 9

Student registration web app with client-side validation (JavaScript),
server-side validation (Servlet), and MySQL storage via JDBC `PreparedStatement`.
Includes full CRUD: Insert, Display, Update, Delete.

## Project structure

```
StudentRegistration/
├── database/schema.sql                  <- run this in MySQL first
├── pom.xml                              <- Maven build (optional)
└── src/main/
    ├── java/com/vcet/student/
    │   ├── DBConnection.java            <- JDBC connection (edit user/password here)
    │   ├── Student.java                 <- model
    │   ├── StudentDAO.java              <- CRUD with PreparedStatement
    │   ├── Validator.java               <- server-side validation
    │   ├── Html.java                    <- HTML helpers (escape, layout)
    │   ├── RegisterServlet.java         <- /register  (Insert)
    │   ├── ListServlet.java             <- /students  (Display)
    │   ├── UpdateServlet.java           <- /update    (Update)
    │   └── DeleteServlet.java           <- /delete    (Delete)
    └── webapp/
        ├── index.html                   <- registration form
        ├── css/style.css
        ├── js/validate.js               <- client-side validation
        └── WEB-INF/web.xml
```

## Setup (step by step)

### 1. Database
Open MySQL Workbench / command line and run:
```
source database/schema.sql;
```
Or copy-paste the contents of `database/schema.sql`.

### 2. DB username / password
Open `src/main/java/com/vcet/student/DBConnection.java` and set
`DB_USER` and `DB_PASSWORD` to your MySQL credentials (default: root / root).

### 3A. Run with Maven (easiest)
```
mvn clean package
```
Copy `target/StudentRegistration.war` into Tomcat's `webapps/` folder, start Tomcat.

### 3B. Run with Eclipse
1. File > New > Dynamic Web Project > name `StudentRegistration`, target runtime Tomcat 9.
2. Copy `src/main/java/*` into the project's `src/main/java`.
3. Copy everything inside `src/main/webapp/*` into `src/main/webapp`.
4. Download **mysql-connector-j-8.x.jar** and put it in `WEBAPP/WEB-INF/lib`.
5. Right-click project > Run As > Run on Server.

### 4. Open in browser
```
http://localhost:8080/StudentRegistration/
```

## Important
- **Tomcat 9** uses `javax.servlet`. If you use **Tomcat 10+**, replace every
  `javax.servlet` with `jakarta.servlet` in the servlet files (and the pom dependency).
- MySQL service must be running before you submit the form.

## URLs
| URL | Purpose |
|---|---|
| `/index.html` | Registration form |
| `/register` | Servlet that saves the student |
| `/students` | List all students (Edit / Delete buttons) |
| `/update?id=N` | Edit a student |
| `/delete` | Delete a student (POST) |

## Marking-scheme coverage
| Indicator | Where |
|---|---|
| Client-side design & validation | `index.html`, `css/style.css`, `js/validate.js` |
| Servlet implementation | `RegisterServlet`, `ListServlet`, `UpdateServlet`, `DeleteServlet` |
| JDBC & connectivity | `DBConnection`, `StudentDAO` (PreparedStatement) |
| CRUD operations | Insert, display, update, delete all working |
| Error handling & output | Success/error alerts, duplicate register-number check, records table |
