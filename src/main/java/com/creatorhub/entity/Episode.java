package com.creatorhub.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "episode",
        indexes = {
                @Index(name = "idx_episode_creation_id", columnList = "creation_id"),
                @Index(name = "idx_episode_creation_episode_num", columnList = "creation_id, episode_num")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE episode SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Episode extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creation_id", nullable = false)
    private Creation creation;

    @Column(nullable = false)
    private Integer episodeNum;

    @Column(length = 35, nullable = false)
    private String title;

    @Column(length = 100, nullable = false)
    private String creatorNote;

    @Column(nullable = false)
    private boolean isCommentEnabled;

    @Column(nullable = false)
    private boolean isPublic;

    @Column
    private Integer likeCount;

    @Column
    private Integer favoriteCount;

    @Column(precision = 3, scale = 2)
    private BigDecimal ratingAverage;

    @OneToMany(mappedBy = "episode", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<ManuscriptImage> manuscriptImages = new ArrayList<>();

    @OneToMany(mappedBy = "episode", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<EpisodeThumbnail> episodeThumbnails = new ArrayList<>();

    @Builder(access = AccessLevel.PRIVATE)
    private Episode(Creation creation,
                    Integer episodeNum,
                    String title,
                    String creatorNote,
                    boolean isCommentEnabled,
                    boolean isPublic) {
        this.creation = creation;
        this.episodeNum = episodeNum;
        this.title = title;
        this.creatorNote = creatorNote;
        this.isCommentEnabled = isCommentEnabled;
        this.isPublic = isPublic;
        this.likeCount = 0;
        this.favoriteCount = 0;
        this.ratingAverage = new BigDecimal(0);
    }

    public static Episode create(Creation creation,
                                 Integer episodeNum,
                                 String title,
                                 String creatorNote,
                                 Boolean isCommentEnabled,
                                 Boolean isPublic) {
        return Episode.builder()
                .creation(creation)
                .episodeNum(episodeNum)
                .title(title)
                .creatorNote(creatorNote)
                .isCommentEnabled(isCommentEnabled != null && isCommentEnabled)
                .isPublic(isPublic != null && isPublic)
                .build();
    }

    public void publish() { this.isPublic = true; }
    public void unpublish() { this.isPublic = false; }

    public void enableComments() { this.isCommentEnabled = true; }
    public void disableComments() { this.isCommentEnabled = false; }

    public void changeTitle(String title) { this.title = title; }
    public void changeCreatorNote(String note) { this.creatorNote = note; }

    public void addManuscriptImage(ManuscriptImage image) {
        this.manuscriptImages.add(image);
        image.changeEpisode(this);
    }

    public void addThumbnail(EpisodeThumbnail thumbnail) {
        this.episodeThumbnails.add(thumbnail);
        thumbnail.changeEpisode(this);
    }
}
