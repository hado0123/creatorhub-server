# 🐳 Docker 기반 배포 가이드

## 📦 Docker Image Build & Push 전략

### 이미지 네이밍 규칙

- Docker Hub Repository: `docker계정id/creatorhub-app`
- 태그 형식: `creatorhub-app:{version}`

예시:
- `docker계정id/creatorhub-app:1.0`
- `docker계정id/creatorhub-app:1.1`
- `docker계정id/creatorhub-app:2.0`

### 버전 관리 정책 (Semantic-like 전략)

| 변경 유형 | 예시 | 버전 증가 |
|------------|--------|------------|
| 버그 수정 | 로직 수정, 설정 수정 | 1.0 → 1.1 |
| 간단한 기능 추가 | API 추가, 내부 기능 개선 | 1.1 → 1.2 |
| 주요 기능 추가 / 구조 변경 | 인증 구조 변경, DB 스키마 변경 | 1.x → 2.0 |

- **소수점 변경 (x.y)** → 하위 호환 유지
- **정수 변경 (x.0)** → 주요 변경 사항 발생

---

### 1. Docker 이미지 빌드

```bash
docker build -t docker계정id/creatorhub-app:1.0 .
```

- 현재 디렉토리의 Dockerfile을 기준으로 이미지 생성
- 태그: `1.0`


### 2. Docker Hub에 Push

```bash
docker push docker계정id/creatorhub-app:1.0
```

- 해당 버전 이미지를 Docker Hub에 업로드
- EC2 또는 다른 서버에서 pull 받아 배포 가능


## 🚀 EC2(Ubuntu) Docker Compose 배포 가이드
[docker 설치 방법 참고](https://docs.docker.com/engine/install/ubuntu/)

### 1️. 사전 준비

#### Docker & Docker Compose 설치 확인

```bash
docker --version
docker compose version
```

정상적으로 버전이 출력되어야 한다.


### 2. Docker Hub 로그인

```bash
docker login -u docker계정id
```

- 비밀번호 입력
- 성공 시 `Login Succeeded` 출력

### 3. 배포 파일 준비

EC2 서버에 아래 파일을 준비한다.

- `docker-compose.yml`
- `docker-compose.prod.yml`
- `docker-compose.monitoring.yml(필요시)`
- `.env`

방법:
- 해당 업로드
- 또는 `vi`로 직접 작성


### 4. docker-compose.prod.yml 수정

👉 `image`가 있어야 자동 pull이 동작한다(반드시 추가) <br/>

### 예시

```yaml
services:
  app:
    image: docker계정id/creatorhub-app:1.0 ✔️추가(백엔드)
    container_name: creatorhub-app
    ...
```



### 🔎 자동 Pull 동작 원리

`docker compose up` 실행 시:

- 해당 태그 이미지가 로컬에 없으면 자동 pull
- 로컬에 이미 존재하면 pull 하지 않음



### ⚠️ 수동 Pull이 필요한 경우

**같은 태그인데 이미지가 변경된 경우**

예: `1.0` 태그를 다시 빌드해서 push한 경우 => 이때는 반드시 수동 pull 실행

```bash
docker pull docker계정id/creatorhub-app:1.0
```


### 5. 인프라 컨테이너 먼저 실행(혹은 재실행)

MySQL, Redis는 기동 시간이 필요하므로 먼저 실행한다.

```bash
sudo docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d mysql redis
```

### 6. 애플리케이션 실행

```bash
sudo docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d app frontend
```

참고) 모니터링 올리기(필요시)
```bash
sudo docker compose -f docker-compose.monitoring.yml up -d
```

### 7. 정상 실행 확인

#### 컨테이너 목록 확인

```bash
sudo docker ps
```

#### 로그 확인

```bash
sudo docker logs -f creatorhub-mysql
sudo docker logs -f creatorhub-redis
sudo docker logs -f creatorhub-app
sudo docker logs -f creatorhub-frontend
```
