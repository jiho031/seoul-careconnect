package com.seoulcareconnect.dto.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailCheckRequest {

    private String email;
    private String verifyCode;
}