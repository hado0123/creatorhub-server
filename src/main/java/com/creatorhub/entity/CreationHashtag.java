package com.creatorhub.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "creation_hashtag",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_creation_hashtag_creation_id_hashtag_id",
                        columnNames = {"creation_id", "hashtag_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreationHashtag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creation_id", nullable = false)
    private Creation creation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hashtag_id", nullable = false)
    private Hashtag hashtag;

    @Builder(access = AccessLevel.PRIVATE)
    private CreationHashtag(Creation creation, Hashtag hashtag) {
        this.creation = creation;
        this.hashtag = hashtag;
    }

    public static CreationHashtag link(Creation creation, Hashtag hashtag) {
        return CreationHashtag.builder()
                .creation(creation)
                .hashtag(hashtag)
                .build();
    }
}