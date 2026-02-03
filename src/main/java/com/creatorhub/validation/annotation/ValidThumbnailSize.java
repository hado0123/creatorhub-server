package com.creatorhub.validation.annotation;

import com.creatorhub.validation.validator.ThumbnailSizeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ThumbnailSizeValidator.class)
public @interface ValidThumbnailSize {
    String message() default "썸네일 파일 사이즈가 허용 범위를 초과했습니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
