package com.gvn.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OptionResponse {
    private String id;
    private String name;
    private String value;
    private Integer display_order;
    
    public static OptionResponse fromServiceOption(com.gvn.entity.ServiceOption option) {
        if (option == null) {
            return null;
        }
        return OptionResponse.builder()
                .id(option.getId())
                .name(option.getName())
                .value(option.getValue())
                .display_order(option.getDisplayOrder())
                .build();
    }
}
