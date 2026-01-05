package com.creatorhub.entity;

import com.creatorhub.constant.CreationThumbnailType;
import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(
        name = "creation_thumbnail",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_creation_thumbnail_creation_type_order",
                columnNames = {"creation_id", "type", "display_order"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE creation_thumbnail SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class CreationThumbnail extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creation_id", nullable = false)
    private Creation creation;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_object_id", nullable = false)
    private FileObject fileObject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    private CreationThumbnailType type;

    @Column(name = "display_order", nullable = false)
    private short displayOrder;

    // self FK
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_image_id")
    private CreationThumbnail sourceImage;

    @Builder(access = AccessLevel.PRIVATE)
    private CreationThumbnail(Creation creation,
                              FileObject fileObject,
                              CreationThumbnailType type,
                              short displayOrder,
                              CreationThumbnail sourceImage) {
        this.creation = creation;
        this.fileObject = fileObject;
        this.type = type;
        this.displayOrder = displayOrder;
        this.sourceImage = sourceImage;
    }

    public static CreationThumbnail create(Creation creation,
                                           FileObject fileObject,
                                           CreationThumbnailType type,
                                           short displayOrder,
                                           CreationThumbnail sourceImage) {
        return CreationThumbnail.builder()
                .creation(creation)
                .fileObject(fileObject)
                .type(type)
                .displayOrder(displayOrder)
                .sourceImage(sourceImage)
                .build();
    }

    public void changeCreation(Creation creation) {
        this.creation = creation;
    }
}
