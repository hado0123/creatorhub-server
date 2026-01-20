package com.creatorhub.entity;

import com.creatorhub.constant.EpisodeThumbnailType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(
        name = "episode_thumbnail",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_episode_thumbnail_episode_type",
                columnNames = {"episode_id", "type"}
        ),
        indexes = {
                @Index(name = "idx_episode_thumbnail_episode_id", columnList = "episode_id"),
                @Index(name = "idx_episode_thumbnail_file_object_id", columnList = "file_object_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE episode_thumbnail SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class EpisodeThumbnail extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_object_id", nullable = false)
    private FileObject fileObject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "episode_id", nullable = false)
    private Episode episode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    private EpisodeThumbnailType type;

    @Builder(access = AccessLevel.PRIVATE)
    private EpisodeThumbnail(FileObject fileObject,
                             Episode episode,
                             EpisodeThumbnailType type) {
        this.fileObject = fileObject;
        this.episode = episode;
        this.type = type;
    }

    public static EpisodeThumbnail create(FileObject fileObject,
                                          Episode episode,
                                          EpisodeThumbnailType type) {
        return EpisodeThumbnail.builder()
                .fileObject(fileObject)
                .episode(episode)
                .type(type)
                .build();
    }

    public void changeEpisode(Episode episode) {
        this.episode = episode;
    }

    public void changeFileObject(FileObject fileObject) {
        this.fileObject = fileObject;
    }

    public void changeType(EpisodeThumbnailType type) {
        this.type = type;
    }
}
