package com.creatorhub.entity;

import com.creatorhub.entity.base.BaseSoftDeleteTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(
        name = "manuscript_image",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_manuscript_image_episode_display_order",
                columnNames = {"episode_id", "display_order"}
        ),
        indexes = {
                @Index(name = "idx_manuscript_image_episode_id", columnList = "episode_id"),
                @Index(name = "idx_manuscript_image_file_object_id", columnList = "file_object_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE manuscript_image SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class ManuscriptImage extends BaseSoftDeleteTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_object_id", nullable = false)
    private FileObject fileObject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "episode_id", nullable = false)
    private Episode episode;

    @Column(nullable = false)
    private Integer displayOrder;

    @Builder(access = AccessLevel.PRIVATE)
    private ManuscriptImage(FileObject fileObject,
                            Episode episode,
                            Integer displayOrder) {
        this.fileObject = fileObject;
        this.episode = episode;
        this.displayOrder = displayOrder;
    }

    public static ManuscriptImage create(FileObject fileObject,
                                         Episode episode,
                                         Integer displayOrder) {
        return ManuscriptImage.builder()
                .fileObject(fileObject)
                .episode(episode)
                .displayOrder(displayOrder)
                .build();
    }

    public void changeEpisode(Episode episode) {
        this.episode = episode;
    }

    public void changeFileObject(FileObject fileObject) {
        this.fileObject = fileObject;
    }

    public void changeDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
