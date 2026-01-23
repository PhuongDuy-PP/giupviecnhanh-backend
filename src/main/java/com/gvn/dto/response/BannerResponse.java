package com.gvn.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BannerResponse {
    private UUID id;
    private String title;
    private String image_url;
    private String link_url;
    private Integer display_order;
    
    public static BannerResponse fromBanner(com.gvn.entity.Banner banner) {
        if (banner == null) {
            return null;
        }
        return BannerResponse.builder()
                .id(banner.getId())
                .title(banner.getTitle())
                .image_url(banner.getImageUrl())
                .link_url(banner.getLinkUrl())
                .display_order(banner.getDisplayOrder())
                .build();
    }
}
