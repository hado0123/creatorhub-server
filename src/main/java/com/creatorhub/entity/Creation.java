package com.creatorhub.entity;

import com.creatorhub.constant.CreationFormat;
import com.creatorhub.constant.CreationGenre;
import com.creatorhub.constant.PublishDay;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "creation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE creation SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Creation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private Creator creator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    private CreationFormat format;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    private CreationGenre genre;

    @Column(nullable = false, length = 30)
    private String title;

    @Column(nullable = false, length = 400)
    private String plot;

    @Column(nullable = false)
    private boolean isPublic;

    // 연재요일은 자주 바뀌지 않으므로 별도의 엔티티가 아닌 @ElementCollection사용
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "creation_publish_day",
            joinColumns = @JoinColumn(name = "creation_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "publish_day", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    private Set<PublishDay> publishDays = new HashSet<>();

    @OneToMany(mappedBy = "creation", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<CreationThumbnail> creationThumbnails = new ArrayList<>();

    @OneToMany(mappedBy = "creation", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<CreationHashtag> creationHashtags = new ArrayList<>();

    @Builder(access = AccessLevel.PRIVATE)
    private Creation(Creator creator,
                     CreationFormat format,
                     CreationGenre genre,
                     String title,
                     String plot,
                     boolean isPublic) {
        this.creator = creator;
        this.format = format;
        this.genre = genre;
        this.title = title;
        this.plot = plot;
        this.isPublic = isPublic;
    }

    public static Creation create(Creator creator,
                                  CreationFormat format,
                                  CreationGenre genre,
                                  String title,
                                  String plot,
                                  Boolean isPublic) {
        return Creation.builder()
                .creator(creator)
                .format(format)
                .genre(genre)
                .title(title)
                .plot(plot)
                .isPublic(isPublic)
                .build();
    }

    public void publish() {
        this.isPublic = true;
    }
    public void unpublish() { this.isPublic = false; }

    public void addThumbnail(CreationThumbnail thumbnail) {
        this.creationThumbnails.add(thumbnail);
        thumbnail.changeCreation(this);
    }

    public void setPublishDays(Set<PublishDay> days) {
        this.publishDays.clear();
        if (days != null) this.publishDays.addAll(days);
    }

    public void addHashtag(Hashtag hashtag) {
        this.creationHashtags.add(CreationHashtag.link(this, hashtag));
    }
}