/* Client-side validation for the student form (registration + edit). */

var rules = {
    studentName: function (v) {
        return /^[A-Za-z][A-Za-z .]{2,49}$/.test(v) ? "" : "Enter a valid name (3-50 letters, spaces and dots allowed).";
    },
    registerNumber: function (v) {
        return /^[A-Za-z0-9]{5,20}$/.test(v) ? "" : "Register number must be 5-20 letters/digits, no spaces.";
    },
    email: function (v) {
        return /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(v) ? "" : "Enter a valid email address.";
    },
    department: function (v) {
        return v !== "" ? "" : "Please select your department.";
    },
    mobile: function (v) {
        return /^[6-9][0-9]{9}$/.test(v) ? "" : "Enter a 10-digit mobile number starting with 6-9.";
    }
};

function setError(id, msg) {
    var input = document.getElementById(id);
    var span = document.getElementById(id + "Error");
    if (span) span.textContent = msg;
    if (input) input.classList.toggle("invalid", msg !== "");
    return msg === "";
}

function validateField(id) {
    var el = document.getElementById(id);
    if (!el) return true;
    return setError(id, rules[id](el.value.trim()));
}

function validateForm() {
    var ok = true;
    var firstBad = null;
    Object.keys(rules).forEach(function (id) {
        if (!validateField(id)) {
            ok = false;
            if (!firstBad) firstBad = document.getElementById(id);
        }
    });
    if (firstBad) firstBad.focus();
    return ok;
}

function clearErrors() {
    Object.keys(rules).forEach(function (id) { setError(id, ""); });
}

document.addEventListener("DOMContentLoaded", function () {
    Object.keys(rules).forEach(function (id) {
        var el = document.getElementById(id);
        if (!el) return;
        el.addEventListener("blur", function () { validateField(id); });
        el.addEventListener("input", function () {
            if (el.classList.contains("invalid")) validateField(id);
        });
    });
    // Mobile: allow digits only
    var mobile = document.getElementById("mobile");
    if (mobile) {
        mobile.addEventListener("input", function () {
            mobile.value = mobile.value.replace(/[^0-9]/g, "");
        });
    }
});
