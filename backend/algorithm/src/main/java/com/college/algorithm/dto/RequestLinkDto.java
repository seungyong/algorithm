package com.college.algorithm.dto;

import com.college.algorithm.type.LinkKind;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RequestLinkDto {
    @NotNull(message = "존재하지 않은 SNS 유형입니다.")
    private final LinkKind linkKind;
    private final String domain;
}
