package com.dbms.common;

import org.springframework.stereotype.Component;

@Component
public class Validation {

    public boolean isValidInput(String input) {
        try {
            if (input.isEmpty() && input.trim().isEmpty()) {
                return false;
            } else {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isAlphaNumeric(String input) {
        try {
            if (input.isEmpty() && input.trim().isEmpty()) {
                return false;
            } else if(input.matches("[A-Za-z0-9]+")){
                return true;
            } else{
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

}
