package com.taskmanager.task.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MaxCodePointsValidator implements ConstraintValidator<MaxCodePoints, String> {

    private int max;

    @Override
    public void initialize(MaxCodePoints annotation) {
        max = annotation.value();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || value.codePointCount(0, value.length()) <= max;
    }
}
