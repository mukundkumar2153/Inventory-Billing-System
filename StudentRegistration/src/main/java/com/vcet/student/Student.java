package com.vcet.student;

public class Student {
    private int id;
    private String name;
    private String registerNumber;
    private String email;
    private String department;
    private String mobile;

    public Student() {}

    public Student(int id, String name, String registerNumber, String email, String department, String mobile) {
        this.id = id;
        this.name = name;
        this.registerNumber = registerNumber;
        this.email = email;
        this.department = department;
        this.mobile = mobile;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRegisterNumber() { return registerNumber; }
    public void setRegisterNumber(String registerNumber) { this.registerNumber = registerNumber; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
}
