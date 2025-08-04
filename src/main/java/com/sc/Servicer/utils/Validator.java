package com.sc.Servicer.utils;

import com.sc.common.Constants;

public class Validator {
    public boolean isValidRole(String role){
        return Constants.ADMIN.equals(role) ||
                Constants.STUDENT.equals(role) ||
                Constants.TEACHER.equals(role);
    }
}
