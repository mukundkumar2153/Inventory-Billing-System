-- Student Registration System: database setup
CREATE DATABASE IF NOT EXISTS student_db;
USE student_db;

CREATE TABLE IF NOT EXISTS students (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    student_name    VARCHAR(50)  NOT NULL,
    register_number VARCHAR(20)  NOT NULL UNIQUE,
    email           VARCHAR(100) NOT NULL,
    department      VARCHAR(100) NOT NULL,
    mobile          VARCHAR(10)  NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
