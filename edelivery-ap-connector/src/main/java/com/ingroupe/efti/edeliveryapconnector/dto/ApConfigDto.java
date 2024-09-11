package com.ingroupe.efti.edeliveryapconnector.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApConfigDto {
    final String url;
    final String username;
    final String password;
}

