package com.gvn.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceResponse {
    private Integer id;
    private String code;
    private String name;
    private String short_description;
    private String icon;
    private Integer category_id;
    
    public static ServiceResponse fromService(com.gvn.entity.Service service) {
        if (service == null) {
            return null;
        }
        return ServiceResponse.builder()
                .id(service.getId())
                .code(service.getCode())
                .name(service.getName())
                .short_description(service.getShortDescription())
                .icon(service.getIcon())
                .category_id(service.getCategory() != null ? service.getCategory().getId() : null)
                .build();
    }
}
