package com.thinkai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {
    private Long id;
    private Long userId;
    private List<CartItemDto> items;
    private int totalItems;
    private BigDecimal totalAmount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemDto {
        private Long id;
        private Long courseId;
        private String courseTitle;
        private String thumbnailUrl;
        private String instructorName;
        private BigDecimal price;
        private LocalDateTime addedAt;
    }
}