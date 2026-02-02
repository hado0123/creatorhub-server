package com.creatorhub.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = com.creatorhub.validation.validator.ManuscriptSizeValidator.class)
@Documented
public @interface ValidManuscriptSize {
    String message() default "원고 파일 사이즈가 허용 범위를 초과했습니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
