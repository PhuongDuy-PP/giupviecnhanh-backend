package com.gvn.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailableServiceResponse {
    private Integer id;
    private String name;
    private List<ServiceResponse> services;
}
