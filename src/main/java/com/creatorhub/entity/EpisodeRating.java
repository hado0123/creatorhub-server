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
        name = "episode_rating",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_episode_rating_member_episode",
                        columnNames = {"member_id", "episode_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE episode_rating SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class EpisodeRating extends BaseSoftDeleteTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "episode_id", nullable = false)
    private Episode episode;

    @Column(nullable = false)
    private Integer score;

    @Builder(access = AccessLevel.PRIVATE)
    private EpisodeRating(Member member, Episode episode, Integer score) {
        this.member = member;
        this.episode = episode;
        this.score = score;
    }

    public static EpisodeRating create(Member member, Episode episode, int score) {
        return EpisodeRating.builder()
                .member(member)
                .episode(episode)
                .score(score)
                .build();
    }
}