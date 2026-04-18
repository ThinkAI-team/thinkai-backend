package com.thinkai.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cart_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "course_title")
    private String courseTitle;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "instructor_name")
    private String instructorName;

    @Column(name = "price_at_add", precision = 10, scale = 2)
    private BigDecimal priceAtAdd;

    @Column(name = "added_at")
    private LocalDateTime addedAt;

    @PrePersist
    protected void onCreate() {
        addedAt = LocalDateTime.now();
    }
}