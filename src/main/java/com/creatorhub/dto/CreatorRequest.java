package com.creatorhub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatorRequest(
        @NotBlank(message = "작가명은 필수입니다.")
        @Size(max = 150, message = "작가명은 150자 이하여야 합니다.")
        String creatorName,

        @Size(max = 300, message = "소개글은 300자 이하여야 합니다.")
        String introduction
) { }

