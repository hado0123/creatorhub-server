package com.creatorhub.entity;

import com.creatorhub.entity.base.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "episode_like",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_episode_like_member_episode",
                columnNames = {"member_id", "episode_id"}
        ),
        indexes = {
                @Index(name = "idx_episode_like_episode", columnList = "episode_id"),
                @Index(name = "idx_episode_like_member_created_at", columnList = "member_id, created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EpisodeLike extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "episode_id", nullable = false)
    private Episode episode;

    @Builder(access = AccessLevel.PRIVATE)
    private EpisodeLike(Member member, Episode episode) {
        this.member = member;
        this.episode = episode;
    }

    public static EpisodeLike like(Member member, Episode episode) {
        return EpisodeLike.builder()
                .member(member)
                .episode(episode)
                .build();
    }
}
