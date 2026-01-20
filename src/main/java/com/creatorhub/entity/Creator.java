package com.creatorhub.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "creator")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE creator SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Creator extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_object_id")
    private FileObject fileObject;

    @Column(nullable = false, length = 150)
    private String creatorName;

    @Column(length = 300)
    private String introduction;

    @Builder(access = AccessLevel.PRIVATE)
    private Creator(Member member,
                    FileObject fileObject,
                    String creatorName,
                    String introduction) {
        this.member = member;
        this.fileObject = fileObject;
        this.creatorName = creatorName;
        this.introduction = introduction;
    }

    public static Creator createCreator(Member member,
                                        String creatorName,
                                        String introduction) {
        return Creator.builder()
                .member(member)
                .creatorName((creatorName != null) ? creatorName : member.getName()) // 필명이 없을시 기존 회원가입 name 사용
                .introduction(introduction)
                .build();
    }
}

