# creatorhub-server
웹툰을 창작하는 작가와 작품을 즐겨보는 독자 모두를 위한 웹툰 플랫폼의 백엔드 서버로 작품 업로드, 작품 뷰, 정산 시스템 등 핵심 기능을 제공합니다.
<br/>
<br/>

---

## 🛠️ 1. 개발 환경

- JDK 21
- Spring Boot 3.5.7
- MySQL 8.0
- Redis 7.2-alpine
- Docker
- Gradle 8.14.3
- IntelliJ IDEA


---

## 📦 2. 프로젝트 구조

```shell
creatorhub-server/
├─ infra/
│  └─ lambda-image-resize/ 
│        ├─ Dockerfile # 람다 컨테이너 이미지 빌드용(ECR 업로드 대상)
│        └─ index.js # 리사이징 lambda함수
├─ mysql-data/  # 로컬 MySQL 볼륨 데이터(자동 생성, gitignore)
├─ mysql-init/  # MySQL 초기화 스크립트(개발/테스트용)
├─ src/
│  └─ main/
│     └─ resources/
│        ├─ application.yml # 공통 설정
│        ├─ application-local.yml # 개발용 설정
│        ├─ application-local-secret.yml # 개발용 비공개 설정(gitignore)
│        ├─ application-prod.yml # 배포용 설정
│        └─ application-test.yml  # 테스트 코드용 설정
├─ .env # docker 컨테이너 생성시 사용되는 비공개 설정
├─ docker-compose.override.yml # 개발용 docker-compose
├─ docker-compose.prod.yml # 배포용 docker-compose
├─ docker-compose.yml # 공통 docker-compose
└─ Dockerfile # 백엔드 배포용 컨테이너 이미지 빌드용
```
---

## 🐳 3. Docker 기반 실행/배포

- MySQL DB, Redis, Spring Boot 앱(creatorhub-server)을 Docker Compose를 통해 실행할 수 있습니다. 
- 모든 민감한 설정 값은 실행 시 환경변수(.env)로 주입합니다.

<br/>

### 🔹 Local Development (개발용 실행)
1. 개발시 mysql, redis 컨테이너만 사용합니다.

```bash
# docker-compose.override.yml 포함
docker compose up -d mysql redis
```

- (참고) docker-compose.override.yml은 내 로컬의 특정 포트로 들어온 요청을 컨테이너 안의 포트로 포워딩하기 위해 아래와 같이 구성되어 있습니다.
```bash
 services:
  mysql:
    ports: ["3306:3306"]
  redis:
    ports: ["6379:6379"]
```

<br/>

2. IAM에서 로컬 개발용 사용자를 생성한 뒤, 로컬에서 AWS 자격증명을 설정합니다.

- 로컬에서 presigned URL 발급 및 ECR push을 위해 사용합니다.(배포는 EC2 IAM Role를 사용)
  - `presigner.presignPutObject(...)` 호출 시, 백엔드가 S3 PutObject 요청에 서명한 presigned URL을 발급합니다.
  - ECR에는 Lambda 컨테이너 이미지(Docker 이미지) 를 push 합니다.
- 설정을 위해 AWS CLI를 설치한 후 aws configure 로 자격증명을 등록합니다.[(AWS CLI 설치: 링크)](https://docs.aws.amazon.com/ko_kr/cli/latest/userguide/getting-started-install.html)

```bash
aws configure
```

```bash
AWS Access Key ID:     IAM User에서 발급받은 Access Key
AWS Secret Access Key: IAM User에서 발급받은 Secret Key
Default region name:   ap-northeast-2
Default output format: json
```

<br/>

3. 개발 환경에서는 application-local.yml을 사용하므로 환경변수를 local로 지정해 실행합니다.

- gradlew로 실행시 아래와 같이 실행합니다.
```bash
./gradlew bootRun --args="--spring.profiles.active=local" # 리눅스
.\gradlew.bat bootRun --args="--spring.profiles.active=local" # 윈도우
```

- IDE로 실행시 환경변수에 아래 값을 지정한 후 실행합니다.
```bash
SPRING_PROFILES_ACTIVE=local
```

<br/>

### 🔹 Production Deployment (배포/운영 실행)

1. 배포를 한다면 mysql과 redis 컨테이너를 생성하고 백그라운드로 실행합니다.
```bash
# docker-compose.prod.yml 포함
# mysql, redis를 먼저 기동하여 초기화 시간 확보
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d mysql redis
```

```bash
# docker-compose.prod.yml 포함
# spring app 이미지 빌드 후 실행
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build app
```