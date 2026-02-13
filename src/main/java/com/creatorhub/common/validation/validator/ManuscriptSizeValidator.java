package com.creatorhub.common.validation.validator;

import com.creatorhub.common.validation.annotation.ValidManuscriptSize;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

@Component
@RequiredArgsConstructor
public class ManuscriptSizeValidator implements ConstraintValidator<ValidManuscriptSize, Long> {

    @Value("${file.max.manuscript-size}")
    private DataSize maxSize;

    @Override
    public boolean isValid(Long value, ConstraintValidatorContext context) {
        if (value == null) return true; // @NotNull이 처리
        return value <= maxSize.toBytes();
    }
}
