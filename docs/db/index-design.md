# Index Design

이 문서는 CreatorHub의 데이터베이스 인덱스 설계를 정리한 문서입니다.  


## 인덱스 설계 원칙

1. **조회 패턴 우선**: 컬럼이 아닌 실제 쿼리 패턴(WHERE + ORDER BY)을 기준으로 설계
2. **복합 인덱스 활용**: 단일 컬럼보다 조회 조건과 정렬을 함께 처리할 수 있는 복합 인덱스 우선
3. **UNIQUE 제약 활용**: 비즈니스 규칙상 유일성이 필요한 경우 UNIQUE 제약이 인덱스 역할을 겸하도록 설계
4. **정렬 비용 제거**: ORDER BY가 있는 조회는 정렬 컬럼이 인덱스 후행 컬럼으로 포함되도록 설계
5. **페이징 최적화**: LIMIT 페이징은 WHERE + ORDER BY를 함께 커버하는 인덱스로 필요한 만큼만 읽고 종료하도록 설계

---

## 1. 회원 (member)

### 인덱스
```sql
UNIQUE INDEX uk_member_email (email)
```

### 조회 패턴
```sql
WHERE email = ?
```

### 목적
- 로그인 시 이메일 기반 회원 조회
- 이메일 중복 확인

---

## 2. 관심작품 목록 (creation_favorite)

### 인덱스
```sql
UNIQUE INDEX uk_creation_favorite_member_creation (member_id, creation_id)
INDEX idx_creation_favorite_member_created_at (member_id, created_at)
```

### 조회 패턴
```sql
-- 회원의 관심작품 최신순 조회
WHERE member_id = ?
ORDER BY created_at DESC
LIMIT ?
```

### 목적
- `uk_creation_favorite_member_creation`: 중복 관심 등록 방지
- `idx_creation_favorite_member_created_at`: 회원 기준 관심작품 조회 시 정렬 비용 제거 및 페이징 성능 확보


---

## 3. 연재 요일 (creation_publish_day)

### 인덱스
```sql
UNIQUE INDEX uk_creation_publish_day (creation_id, publish_day)
```

### 조회 패턴
```sql
WHERE creation_id = ?
```

### 목적
- 작품별 연재 요일 조회
- 중복 연재 요일 등록 방지
---

## 4. 작품 썸네일 (creation_thumbnail)

### 인덱스
```sql
UNIQUE INDEX uk_creation_thumbnail_creation_type_order (creation_id, type, display_order)
```

### 조회 패턴
```sql
WHERE creation_id = ?
ORDER BY display_order ASC
```

### 목적
- 썸네일 순서 조회 시 추가 정렬 방지
- 중복된 썸네일 등록 방지
---

## 5. 작품-해시태그 연결 (creation_hashtag)

### 인덱스
```sql
UNIQUE INDEX uk_creation_hashtag_creation_id_hashtag_id (creation_id, hashtag_id)
INDEX idx_creation_hashtag_hashtag_creation(hashtag_id, creation_id)
```

### 조회 패턴
```sql
-- 작품별 해시태그 조회
WHERE creation_id = ?

-- 해시태그별 작품 조회
WHERE hashtag_id = ?
```

### 목적
- `uk_creation_hashtag_creation_id_hashtag_id`: 중복 방지(무결성)
- `idx_creation_hashtag_hashtag_creation`: 해시태그 기반 작품 조회 최적화

---

## 6. 해시태그 (hashtag)

### 인덱스
```sql
UNIQUE INDEX uk_hashtag_title (title)
```

### 조회 패턴
```sql
WHERE title = ?
```

### 목적
- 해시태그 제목 기반 조회
- 중복 해시태그명 생성 방지
---

## 7. 에피소드 (episode)

### 인덱스
```sql
INDEX idx_episode_creation_episode_num (creation_id, episode_num)
```

### 조회 패턴
```sql
-- 작품의 에피소드 목록 조회
WHERE creation_id = ?
ORDER BY episode_num DESC
LIMIT ?

-- 특정 에피소드 조회
WHERE creation_id = ? AND episode_num = ?
```

### 목적
- 작품별 에피소드 목록 조회 시 정렬 비용 제거 및 LIMIT 페이징 최적화
- 작품 내 특정 회차(creation_id + episode_num) 단건 조회 최적화

---

## 8. 에피소드 썸네일 (episode_thumbnail)

### 인덱스
```sql
UNIQUE INDEX uk_episode_thumbnail_episode_type (episode_id, type)
INDEX idx_episode_thumbnail_file_object_id (file_object_id)
```

### 조회 패턴
```sql
-- 에피소드별 썸네일 조회
WHERE episode_id = ?

-- 파일 객체 기준 썸네일 조회
WHERE file_object_id = ?
```

### 목적
- `uk_episode_thumbnail_episode_type`: 에피소드당 타입별 썸네일 유일성 보장 (UNIQUE 제약이 인덱스 겸함)
- `idx_episode_thumbnail_file_object_id`: 파일 객체 기준 역참조 조회 최적화

---

## 9. 원고 이미지 (manuscript_image)

### 인덱스
```sql
UNIQUE INDEX uk_manuscript_image_episode_display_order (episode_id, display_order)
INDEX idx_manuscript_image_file_object_id (file_object_id)
```

### 조회 패턴
```sql
-- 에피소드 원고 이미지 순서대로 조회
WHERE episode_id = ?
ORDER BY display_order ASC

-- 파일 객체 기준 원고 이미지 조회
WHERE file_object_id = ?
```

### 목적
- `uk_manuscript_image_episode_display_order`: 에피소드 내 이미지 순서 유일성 보장 및 정렬 비용 제거
- `idx_manuscript_image_file_object_id`: 파일 객체 기준 역참조 조회 최적화

---

## 10. 에피소드 좋아요 (episode_like)

### 인덱스
```sql
UNIQUE INDEX uk_episode_like_member_episode (member_id, episode_id)
INDEX idx_episode_like_member_created_at (member_id, created_at)
```

### 조회 패턴
```sql
-- 회원의 좋아요 목록 최신순 조회
WHERE member_id = ?
ORDER BY created_at DESC
LIMIT ?
```

### 목적
- `uk_episode_like_member_episode`: 중복 좋아요 방지
- `idx_episode_like_member_created_at`: 회원 기준 좋아요 목록 조회 시 정렬 비용 제거 및 페이징 성능 확보



---

## 11. 에피소드 평점 (episode_rating)

### 인덱스
```sql
UNIQUE INDEX uk_episode_rating_member_episode (member_id, episode_id)
```

### 조회 패턴
```sql
-- 회원이 특정 에피소드에 남긴 평점 조회
WHERE member_id = ? AND episode_id = ?
```

### 목적
- `uk_episode_rating_member_episode`: 회원당 에피소드별 평점 중복 방지

---

## 12. 파일 객체 (file_object)

### 인덱스
```sql
UNIQUE INDEX uk_file_object_storage_key (storage_key)
```

### 조회 패턴
```sql
WHERE storage_key = ?
```

### 목적
- 스토리지 키 기반 파일 조회
- 중복 파일 등록 방지



