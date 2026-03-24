# Creatorhub
웹툰을 창작하는 작가와 독자 모두를 위한 **웹툰 플랫폼의 백엔드 서버**로 작품 업로드, 작품 뷰, 결제 시스템 등 핵심 기능을 제공합니다.
<br/>
<br/>

<img src="./docs/images/cutfast_convert_recording.gif" width="750" alt="demo" />

## 주요기능
#### 👨‍🎨 작가 기능
- 작품 등록 / 회차 등록

#### 📚 독자 기능
- Cursor 방식의 요일별 작품 조회(인기순·조회수·별점순)
- 작품 및 회차 상세 조회
- 관심 작품 등록 / 회차별 평점·좋아요 등록

#### 💳 결제 시스템(진행중)
- Toss Payments 결제 및 코인 충전

<br/>

## 🛠️ 기술 스택

### Backend
- Spring Boot 3.5.7, MySQL 8.0, Redis 7.2-alpine, Gradle 8.14.3
- Spring Data JPA, Flyway (DB Migration)

### Infra
- AWS (S3, Lambda, SQS, SNS, CloudFront)
- Docker Compose (배포·실행)
- Prometheus + Grafana(모니터링), k6(부하테스트)

### Frontend
- 샘플 프론트엔드는 React, TypeScript 기반으로 구현
- Claude AI를 활용해 프로토타이핑 및 반복 작업을 자동화

<br/>

## ☁️ 아키텍쳐 다이어그램
<img src="./docs/images/total_architecture.png" alt="아키텍쳐" />

<br/>

## 🗄️ ERD
[💡ERD 상세 보러가기](https://www.erdcloud.com/d/acRK6DAyfKdTHhQCe)
<img src="./docs/images/creatorhub_erd.png" alt="erd" />

<br/>

## 📦 프로젝트 구조

### 1) Application

```
src/
└─ main/
   ├─ java/
   └─ resources/
```
- 도메인 중심 패키지 구조
- application-{env}.yml 기반 환경 분리


### 2) Infra
```
infra/
└─ lambda-image-resize/
```
- Lambda 기반 이미지 리사이징 분리
- 비동기 처리 파이프라인 구성


### 3) Performance & Monitoring
```
k6/
```
- k6 부하 테스트 + Prometheus/Grafana 모니터링


### 4) Deployment
```
docker-compose*.yml
.env
```
- Docker Compose 기반 개발/운영 환경 분리

<br/>

## ⭐ ISSUE 및 해결방법

### 1. 썸네일·원고 이미지 처리
[💡이미지 처리 문제 해결 과정 상세보기](docs/architecture/creation-image-upload-resize.md)

- **문제:** 이미지 업로드/리사이징을 서버에서 직접 처리시 부하 증가
- **해결:** S3 Presigned URL + Lambda 기반 비동기 처리로 서버 부하 감소
<br/>

- **문제:** Lambda 리사이징 실패시 재처리
- **해결:** 실패된 Lambda 처리는 3회 재시도 후 실패시 메시지큐(SQS)로 이동 및 SNS로 slack 알람
<br/>

- **문제:** 업로드 후 첫 이미지 노출시 지연 문제
- **해결:** CDN(CloudFront)을 사용해 지연 최소화
<br/>

- **문제:** 비동기 이미지 처리 완료 여부를 클라이언트가 확인하기 어려움 → Polling과 SSE 중 선택 필요
- **해결:** 'Lambda → Backend Callback → SSE 알림' 구조 적용을 통해 불필요한 트래픽을 줄이고 클라이언트에서 코드 구현을 단순화


### 2. JWT + Redis 기반 인증
[💡인증시 문제 해결 과정 상세보기](docs/architecture/jwt_redis.md)

- **문제:** 세션방식과 JWT 방식중 인증방식 선택 필요
- **해결:** 차후 서버의 수평 확장시(Scale-out) 비용·지연 부담이 커져 JWT 방식 선택
<br/>

- **문제:** Refresh Token 저장소로 RDB 또는 Redis 중 선택 필요
- **해결:** TTL 기반 자동 만료로 토큰 정리 작업을 최소화하기 위해 Redis 선택


### 3. 요일별 웹툰 조회시 페이징 처리
- **문제:** Offset 기반 페이징은 데이터 증가 시 성능 저하 발생
- **해결:** 'Cursor 기반 페이징' 적용

### 4. 데이터 무결성 처리
- **문제:** 관심작품, 좋아요 등 사용자의 사용 실수로 동시 요청시 중복데이터 발생 가능
- **해결:** 'DB Unique 제약조건 + 원자적 업데이트 쿼리' 사용을 통해 데이터 무결성 유지

### 5. DB 스키마 관리
- **문제:** DB 스키마 변경 이력 관리 필요
- **해결:** 버전 기반 스키마 관리 및 환경 간 DB 불일치 방지를 위해 Flyway 사용

<br/>

## ⭐ 성능 테스트 및 개선
- Prometheus, Grafana를 통한 모니터링 환경 구현 + k6 부하 테스트
- Creations(작품): 7,000건, Episodes(회차): 518,146건

### 1. 요일별 웹툰 조회 API

#### 개선 결과
| Metric       | Before   | After                     |
|--------------| -------- |---------------------------|
| Throughput(TPS) | 93 req/s | **1,094 req/s (11.7배 ↑)** |
| P95 Latency  | 3.13 s   | **297ms (90% ↓)**         |

#### 핵심 개선 포인트
- Episode 테이블 518만 행의 'JOIN + GROUP BY' 집계 제거 
- Creation 테이블 비정규화 (사전 집계 컬럼 생성)
- JPQL → Native Query 변경 및 JOIN 구조 개선
- 쿼리 3회 호출 → 1회 조회로 통합
- HikariCP 커넥션 풀, Mysql DB 메모리 튜닝

[💡자세한 성능 분석 과정보기](docs/performance/creation-list-load-improvements.md)

### 2. 특정 회차 웹툰 조회 API

#### 개선 결과
| Metric       | Before    | After                     |
|--------------| --------- |---------------------------|
| Throughput(TPS) | 151 req/s | **2,748.5 req/s (18배 ↑)** |
| P95 Latency  | 1.97 s    | **131.98ms (93% ↓)**      |

#### 핵심 개선 포인트
- Caffeine 캐시를 적용해 반복 조회 성능 개선 및 캐시 스탬피드 완화
- 조회수 증가 시 Redis에 집계 후 10초 주기 배치로 DB에 반영하여 업데이트 Lock 경쟁 제거
- 조회수 업데이트 로직은 비동기 처리로 분리
- SUM(view_count) 집계 쿼리를 단순 증가(+1) 방식으로 변경

[💡자세한 성능 분석 과정보기](docs/performance/episode-read-load-improvements.md)

<br/>

## ⚙️ 설계 규칙
### 1. DB / Entity 설계 정책
[💡RDB 설계 원칙과 제약조건 상세보기](docs/db/schema-policy.md) <br/>
- 도메인 식별자 및 사용자 액션(좋아요, 관심작품, 평점 등)은 DB `UNIQUE` 제약으로 중복을 차단하여 데이터 무결성과 멱등성 보장
- Enum 값은 DB에 VARCHAR로 저장하여 코드 Enum 변경이 DB 마이그레이션으로 확장되는 것 방지
- 썸네일 및 원고 이미지는 display_order로 관리하여 순서 충돌 및 중복 데이터 방지

[💡Index 정책 상세보기](docs/db/index-design.md) <br/>
- 인덱스는 컬럼이 아닌 실제 조회 패턴(WHERE + ORDER BY)을 기준으로 설계
- 정렬과 페이징 성능을 위해 복합 인덱스와 UNIQUE 제약을 활용하여 정렬 비용 최소화

[💡Entity 정책 상세보기](docs/db/entity-relationship-policy.md) <br/>
- JPA 연관관계는 **단방향 우선 전략**을 적용하고, 생명주기 관리가 필요한 경우에만 양방향 + cascade/orphanRemoval 사용

### 2. Spring Security 기반 인증 구조
- JWT 인증 + Role 기반 접근 제어 → API 권한 관리 일관성 확보
### 3. Lambda Callback HMAC 검증
- 이미지 리사이징 완료시 백엔드 콜백 요청에 대해 HMAC 검증 적용 → 외부 요청 위변조 방지

<br/>

## 🐳 Docker 기반 실행/배포 가이드

- MySQL DB, Redis, Spring Boot 앱(creatorhub-server), prometheus, grafana를 Docker Compose를 통해 실행할 수 있습니다. 
- 모든 민감한 설정 값은 실행 시 환경변수(.env)로 주입합니다.

💡[실행 가이드 보기](getting-started.md)
<br/>
💡[배포 가이드 보기](deployment-guide.md)

<br/>
