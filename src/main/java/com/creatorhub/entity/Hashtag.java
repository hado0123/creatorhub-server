package com.creatorhub.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "hashtag",
        uniqueConstraints = @UniqueConstraint(name = "uk_hashtag_title", columnNames = "title")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Hashtag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String title;

    @Builder(access = AccessLevel.PRIVATE)
    private Hashtag(String title) {
        this.title = title;
    }

    public static Hashtag create(String title) {
        return Hashtag.builder()
                .title(title)
                .build();
    }
}

