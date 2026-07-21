package com.example.capshop.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FindUserIdRequest {
    private String name;
    private String phone;
}
