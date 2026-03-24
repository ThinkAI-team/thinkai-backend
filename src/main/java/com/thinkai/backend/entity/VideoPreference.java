package com.thinkai.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "video_preferences")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "playback_speed", nullable = false)
    private Double playbackSpeed = 1.0;

    @Column(name = "auto_play", nullable = false)
    private Boolean autoPlay = true;

    @Column(name = "quality", length = 10)
    private String quality = "auto";
}
