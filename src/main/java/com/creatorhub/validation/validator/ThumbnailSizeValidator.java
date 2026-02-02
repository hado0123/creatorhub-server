package com.creatorhub.validation.validator;

import com.creatorhub.validation.annotation.ValidThumbnailSize;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

@Component
public class ThumbnailSizeValidator implements ConstraintValidator<ValidThumbnailSize, Long> {

    @Value("${file.max.thumbnail-size}")
    private DataSize maxSize;

    @Override
    public boolean isValid(Long value, ConstraintValidatorContext context) {
        if (value == null) return true; // @NotNull이 따로 처리
        return value <= maxSize.toBytes();
    }
}
