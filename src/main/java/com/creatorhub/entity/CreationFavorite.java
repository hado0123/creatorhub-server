package com.creatorhub.entity;

import com.creatorhub.entity.base.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "creation_favorite",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_creation_favorite_member_creation",
                columnNames = {"member_id", "creation_id"}
        ),
        indexes = {
                // 내 관심작품 목록 조회 (최신순)
                @Index(
                        name = "idx_creation_favorite_member_created_at",
                        columnList = "member_id, created_at"
                ),
                // 작품별 관심자 조회 (알림 대상 추출)
                @Index(
                        name = "idx_creation_favorite_creation",
                        columnList = "creation_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreationFavorite extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creation_id", nullable = false)
    private Creation creation;

    @Builder(access = AccessLevel.PRIVATE)
    private CreationFavorite(Member member, Creation creation) {
        this.member = member;
        this.creation = creation;
    }

    public static CreationFavorite create(Member member, Creation creation) {
        return CreationFavorite.builder()
                .member(member)
                .creation(creation)
                .build();
    }
}
