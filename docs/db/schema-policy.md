# DB Schema Policy

이 문서는 CreatorHub의 RDB 설계 원칙과 제약조건 정책을 설명한다.

---

## 1. enum(상수) 타입의 데이터는 DB에서 VARCHAR 타입으로 사용한다.

### 문제
Enum을 DB 컬럼에 직접 매핑하면, Enum 상수의 변경이 곧 DB 데이터 변경 작업으로 이어진다.
<br/>
이는 코드 변경이 데이터 마이그레이션 이슈로 확장되어 운영 부담과 데이터 정합성 문제를 유발한다.

### 정책
DB는 Enum을 알지 못하도록 설계하고, Enum 값은 단순 문자열(VARCHAR)로 저장한다.

### 적용 예시
  - file_object: status 컬럼
    ```
    public enum FileObjectStatus {
        INIT,
        READY,
        FAILED
    }
    ```
    ```
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    private FileObjectStatus status;
    ```

### 결과
코드의 Enum 변경이 DB 데이터 변경으로 확장되지 않도록 분리되었다.

---

## 2. Unique 제약을 통한 중복 제한

### 문제
도메인에서 '하나만 존재해야 하는 값'을 코드 레벨에서만 검증할 경우 동시성 상황에서 중복 데이터가 생성될 수 있다.
<br/>
이러한 값들은 시스템 내에서 보조 식별자 역할을 하기 때문에 중복이 발생하면 데이터 무결성이 깨진다.

### 정책
도메인 식별자 성격을 가지는 컬럼은 DB 레벨의 UNIQUE 제약으로 중복을 원천 차단한다.

### 적용
- member: UNIQUE (email) => 회원의 email은 중복될 수 없다.
- hashtag: UNIQUE (title) => 해시태그명은 중복될 수 없다.

### 결과
- 코드의 중복 검증 로직에 의존하지 않고 DB가 도메인 식별자의 유일성을 직접 보장한다.
- UNIQUE 제약으로 인해 생성된 인덱스를 통해 식별자 기준 조회 시 효율적인 탐색이 가능하다.

---

## 3. Unique 제약을 통한 관계 무결성 보장

### 문제
조인 테이블 및 보조 식별자 성격의 데이터는 코드 레벨에서만 중복을 검증할 경우 동시성 상황에서 중복 데이터가 생성될 수 있다.
<br/>
이러한 중복은 도메인 규칙을 깨뜨리고 데이터 무결성을 훼손한다.

### 정책
도메인 구조상 중복되면 안 되는 관계는 DB 레벨의 UNIQUE 제약으로 중복을 원천 차단한다.

### 적용
- creation_hashtag: UNIQUE (creation_id, hashtag_id) => 한 작품에 중복된 해시태그를 등록할 수 없다.
- creation_publish_day: UNIQUE (creation_id, publish_day) => 한 작품에 중복된 요일을 등록할 수 없다.

### 결과
코드의 중복 검증 로직에 의존하지 않고 DB가 관계의 무결성을 직접 보장한다.

---

## 4. Unique 제약을 통한 멱등성 보장(사용자 액션)

### 문제
관심작품, 좋아요, 평점 기능에서 중복 요청 및 동시성 이슈가 발생할 수 있다.

### 정책
DB 레벨에서 유니크 제약으로 중복을 차단한다.

### 적용
- creation_favorite: UNIQUE (member_id, creation_id) => 한 작품은 한번만 관심 작품으로 등록할 수 있다.
- episode_like: UNIQUE (member_id, episode_id) => 한 회차에 한번만 좋아요를 할 수 있다.
- episode_rating: UNIQUE (member_id, episode_id) => 한 회차에 한번만 별점을 등록할 수 있다.

### 결과
코드 레벨의 중복 체크 없이, DB가 멱등성을 보장한다.

---

## 5. Thumbnail Slot 정책

### 문제
썸네일을 단순 다건 저장 구조로 두면 발생하는 문제점들
- 작품당 대표 썸네일이 여러 개 생길 수 있다.
- 파생 이미지 순서가 충돌할 수 있다.
- 동시성 상황에서 중복 데이터가 발생할 수 있다.

### 정책
- 썸네일을 '여러 장의 이미지'가 아닌, '정해진 슬롯 구조'로 관리한다.
- DB가 썸네일의 '슬롯 구조'를 직접 강제하도록 아래 유니크 제약을 둔다.

### 적용
- creation_thumbnail: UNIQUE (creation_id, type, display_order)
- episode_thumbnail: UNIQUE (episode_id, type)
- manuscript_image: UNIQUE (episode_id, display_order)

### 결과
- 공통: 썸네일 구조 규칙을 코드가 아닌 DB가 보장한다.
- 상세
  - creation_thumbnail
    - type 컬럼 값: 'POSTER', 'HORIZONTAL', 'DERIVED'
    - 'POSTER', 'HORIZONTAL'은 display_order=0 고정 → 작품 하나 당 포스터형, 가로형의 대표 썸네일이 1개씩 보장된다.
    - 'DERIVED' 이미지의 display_order는 1..N 슬롯으로 순서 및 중복을 방지한다.
  - episode_thumbnail
    - type 컬럼 값: 'EPISODE', 'SNS'
    - 회차 하나 당 에피소드형, SNS형의 대표 썸네일이 1개씩 보장된다.
  - manuscript_image
    - 원고 이미지의 display_order는 1..N 슬롯으로 순서 및 중복을 방지한다.

---

