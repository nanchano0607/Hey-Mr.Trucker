package com.example.capshop.dto.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FindUserIdRequest {
    private String name;
    private String phone;
}
