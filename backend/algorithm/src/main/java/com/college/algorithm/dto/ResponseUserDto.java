package com.college.algorithm.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ResponseUserDto {
    private final Long userId;
    private final String nickname;
    private final String email;
    private final String profile;
    private final Long tried;
    private final Long solved;
}
