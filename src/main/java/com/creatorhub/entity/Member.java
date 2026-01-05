package com.creatorhub.entity;

import com.creatorhub.constant.Gender;
import com.creatorhub.constant.Role;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;

@Entity
@Table(
        name = "member",
        uniqueConstraints = @UniqueConstraint(name = "uk_member_email", columnNames = "email")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE member SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 60)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private LocalDate birthday;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @OneToOne(mappedBy = "member", fetch = FetchType.LAZY)
    private Creator creator;

    @Builder(access = AccessLevel.PRIVATE)
    private Member(String email,
                   String password,
                   String name,
                   LocalDate birthday,
                   Gender gender,
                   Role role) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.birthday = birthday;
        this.gender = gender;
        this.role = role;
    }

    public static Member createMember(String email,
                                      String encodedPassword,
                                      String name,
                                      LocalDate birthday,
                                      Gender gender) {

        return Member.builder()
                .email(email)
                .password(encodedPassword)
                .name(name)
                .birthday(birthday)
                .gender(gender)
                .role(Role.MEMBER)
                .build();
    }

    public void changeRole(Role role) {
        this.role = role;
    }
}
