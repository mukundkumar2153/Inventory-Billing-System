package com.vcet.student;

import java.util.regex.Pattern;

/** Server-side validation (mirrors the JavaScript rules on the client). */
public class Validator {

    private static final Pattern NAME   = Pattern.compile("^[A-Za-z][A-Za-z .]{2,49}$");
    private static final Pattern REGNO  = Pattern.compile("^[A-Za-z0-9]{5,20}$");
    private static final Pattern EMAIL  = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern MOBILE = Pattern.compile("^[6-9][0-9]{9}$");

    /** Returns an error message, or null if everything is valid. */
    public static String validate(Student s) {
        if (s.getName() == null || !NAME.matcher(s.getName()).matches())
            return "Invalid name. Use 3-50 letters (spaces and dots allowed).";
        if (s.getRegisterNumber() == null || !REGNO.matcher(s.getRegisterNumber()).matches())
            return "Invalid register number. Use 5-20 letters/digits.";
        if (s.getEmail() == null || !EMAIL.matcher(s.getEmail()).matches())
            return "Invalid email address.";
        if (s.getDepartment() == null || s.getDepartment().isEmpty() || !Html.isValidDepartment(s.getDepartment()))
            return "Please select a valid department.";
        if (s.getMobile() == null || !MOBILE.matcher(s.getMobile()).matches())
            return "Invalid mobile number. Enter 10 digits starting with 6-9.";
        return null;
    }
}
