# Entity Relationship Policy

# 1. 설계 원칙

## 1.1 단방향 우선 전략
- 모든 연관관계는 기본적으로 **단방향**
- 조회 편의 때문에 무조건 양방향을 사용하지 않음
- 영속성 관리 복잡도 최소화
- 대량 컬렉션의 무분별한 로딩 방지


## 1.2 양방향 사용 기준

양방향은 아래 조건을 만족할 때만 사용한다.
- 부모가 자식의 **생명주기를 통제**해야 하는 경우
- `cascade = ALL` + `orphanRemoval = true` 가 필요한 경우
- 집합 단위 추가/삭제/교체가 필요한 경우


## 1.3 Aggregate Root 중심 설계

- `Creation` = 작품 도메인의 Aggregate Root
- `Episode` = 회차 도메인의 Aggregate Root
- 하위 엔티티는 루트 엔티티에서만 생성/삭제
> **예외**: `ManuscriptImage`, `EpisodeThumbnail`은 전처리 로직의 복잡도로 인해 전용 서비스(`ManuscriptImageService`, `EpisodeThumbnailService`)에서 직접 저장한다.


## 1.4 LAZY 기본 전략

- 모든 연관관계는 기본은 `LAZY`
- 인가 체크는 엔티티 체이닝이 아닌 **전용 JPQL 조회 쿼리 사용**
- 연쇄 접근에 의한 불필요한 쿼리 발생 방지

---

# 2. 관계 구조 및 설계 의도


## 2.1 Member 도메인

### Creator → Member (단방향, 1:1, FK: Creator)

- Creator가 Member를 참조
- 모든 회원이 작가는 아님
- Creator는 Member의 확장 개념

**설계 의도**
- 선택적 확장 구조
- FK는 확장 개념이 보유
- Member는 Creator를 몰라도 됨


## 2.2 Creator 도메인

### Creator → FileObject (단방향)

- 프로필 이미지 참조
- 역방향 탐색 불필요


## 2.3 Creation 도메인 (작품)

### Creation → Creator (단방향)

- 작품은 하나의 작가에 속함

**양방향을 사용하지 않은 이유**
- Creator → Creation 컬렉션은 대량 데이터 가능성
- 작품 목록 조회는 전용 쿼리로 처리
- 메모리 로딩 방지


### Creation ←→ CreationThumbnail (양방향, cascade/orphan)

- 작품이 썸네일 집합을 관리
- 썸네일은 작품에 완전 종속

**양방향을 사용한 이유**
- 집합 단위 관리 필요
- orphan 자동 제거 필요
- Aggregate 내부 구성요소

### Creation ←→ CreationHashtag (양방향, cascade/orphan)

- 작품이 해시태그 연결 엔티티를 관리
- 연결 엔티티는 작품의 구성요소 역할

**양방향을 사용한 이유**
- 수정 시 해시태그 집합 교체 가능
- orphanRemoval 필요
- 부모 중심 집합 제어

### CreationHashtag → Hashtag (단방향)

- 해시태그 마스터 참조

**양방향을 사용하지 않은 이유**
- Hashtag → Creation 대량 데이터 위험
- 조회는 전용 쿼리로 해결

### CreationThumbnail → FileObject (단방향)

- 썸네일 파일 참조
- FileObject는 공용 리소스

### CreationThumbnail → CreationThumbnail (self-reference)

- 원본 → 리사이즈 구조
- 파생 썸네일 관리 목적

### CreationFavorite → Member (단방향)
### CreationFavorite → Creation (단방향)

- 관심(즐겨찾기) 연결 엔티티

**양방향을 사용하지 않은 이유**
- Member가 모든 관심작품을 들고 있을 필요 없음
- Creation이 Favorite 컬렉션을 들고 있을 필요 없음
- 카운트는 집계 쿼리 기반 관리

## 2.4 Episode 도메인 (회차)

### Episode → Creation (단방향)

- 회차는 하나의 작품에 속함

**양방향을 사용하지 않은 이유**
- 작품 → 회차 컬렉션은 대량 가능성
- 페이지네이션 조회 사용
- 메모리 부담 방지

### Episode ←→ ManuscriptImage (양방향, cascade/orphan)

- 회차가 원고 이미지 집합 관리

**양방향을 사용한 이유**
- 수정 시 집합 교체 가능
- orphanRemoval 필요
- 완전 종속 관계


### Episode ←→ EpisodeThumbnail (양방향, cascade/orphan)

- 회차 썸네일 집합 관리

**양방향을 사용한 이유**
- orphan 자동 제거 필요
- 집합 단위 제어 필요

### EpisodeLike → Member (단방향)
### EpisodeLike → Episode (단방향)

- 좋아요 연결 엔티티
- 역방향 탐색 불필요


### EpisodeRating → Member (단방향)
### EpisodeRating → Episode (단방향)

- 평점 연결 엔티티
- 집계 기반 조회

---

# 3. 전체 설계 요약

## 전체 관계
```
Creator  →  Member (단방향, 1:1, FK: Creator)
Creator  →  FileObject (단방향)
Creation →  Creator (단방향)
Creation ←→ CreationThumbnail (양방향, cascade/orphan)
Creation ←→ CreationHashtag (양방향, cascade/orphan)
CreationHashtag → Hashtag (단방향)
CreationThumbnail → FileObject (단방향)
CreationThumbnail → CreationThumbnail (단방향 self-ref)
CreationFavorite → Member (단방향)
CreationFavorite → Creation (단방향)
Episode  →  Creation (단방향)
Episode ←→ ManuscriptImage (양방향, cascade/orphan)
Episode ←→ EpisodeThumbnail (양방향, cascade/orphan)
ManuscriptImage → FileObject (단방향)
EpisodeThumbnail → FileObject (단방향)
EpisodeLike → Member (단방향)
EpisodeLike → Episode (단방향)
EpisodeRating → Member (단방향)
EpisodeRating → Episode (단방향)
```

## 양방향 사용 기준 정리

| 조건 | 사용 여부 |
|------|------------|
| 부모가 자식 생명주기 통제 | 사용 |
| orphanRemoval 필요 | 사용 |
| 집합 단위 추가/삭제/교체 필요 | 사용 |
| 단순 참조 | 사용하지 않음 |
| 연결 테이블 역할 | 단방향 |
| 대량 데이터 가능성 | 단방향 |