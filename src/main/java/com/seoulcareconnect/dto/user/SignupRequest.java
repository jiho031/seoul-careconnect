package com.seoulcareconnect.dto.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {

    private String email;
    private String password;
    private String name;
    private String phone;
    private String ageGroup;
    private String district;
    private String birthYear;
}