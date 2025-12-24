# 🥗 MatchMeal (MM) Backend

> **건강한 식단 관리와 챌린지를 통한 습관 형성, AI 영양 코치의 맞춤형 조언까지.**  
> MatchMeal 서비스의 핵심 비즈니스 로직과 데이터 처리를 담당하는 Backend REST API 서버입니다.

## 📖 프로젝트 개요

**MatchMeal Backend**는 **Java 17**과 **Spring Boot 3.4**를 기반으로 구축된 고성능 API 서버입니다.  
안정적인 **RESTful API**를 제공하며, 대용량 트래픽 처리를 고려한 아키텍처와 **Spring Batch**를 활용한 데이터 처리 시스템을 갖추고 있습니다.  
Spring Security와 JWT를 이용한 안전한 인증/인가 시스템을 구현하였으며, Redis 캐싱을 통해 성능을 최적화했습니다.

- **개발 기간**: 2025.11 ~ 2025.12
- **서비스 형태**: REST API Server

---

## ✨ 주요 기능

### 1. 🔐 보안 및 인증 (Security & Auth)
- **JWT 인증**: Access/Refresh Token 기반의 무상태(Stateless) 인증 시스템을 구현했습니다.
- **OAuth2 소셜 로그인**: Kakao, Naver, Google 등 소셜 계정을 통한 간편 로그인 및 자동 회원가입을 지원합니다.
- **Spring Security**: 필터 체인을 활용한 정교한 권한 제어 및 보안 설정을 적용했습니다.

### 2. 📝 데이터 관리 및 비즈니스 로직
- **식단 관리**: 음식 데이터베이스 CRUD 및 영양소 계산 로직을 처리합니다.
- **챌린지 시스템**: 챌린지 생성, 참여, 달성률 계산 및 보상 지급 로직을 수행합니다.
- **커뮤니티**: 게시글/댓글 작성, 좋아요, 조회수 등 커뮤니티 기능을 뒷받침합니다.
- **마이페이지**: 사용자 신체 정보 관리 및 통계 데이터를 제공합니다.

### 3. 🤖 AI 및 외부 서비스 연동
- **AI 영양 코치**: AI 서버와 통신하여 사용자 맞춤형 식단 추천 및 상담 결과를 저장/조회합니다.
- **AWS S3**: 식단 사진, 프로필 이미지 등 미디어 파일의 업로드/다운로드를 처리합니다.
- **포인트 결제**: KakaoPay API와 연동하여 포인트 충전 및 결제 승인/취소/환불 로직을 처리합니다.

### 4. ⚡ 성능 최적화 및 실시간 통신
- **Redis 캐싱**: 자주 조회되는 음식 데이터 등을 캐싱하여 DB 부하를 줄이고 응답 속도를 개선했습니다.
- **WebSocket**: 실시간 알림(댓글, 초대 등) 및 채팅 기능을 위한 웹소켓 서버를 구축했습니다.
- **대용량 처리**: Spring Batch를 활용하여 공공 데이터 동기화 및 매일 자정 랭킹 집계 작업을 자동화했습니다.

---

## 🛠 기술 스택 (Tech Stack)

### Backend Core
![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)

### Database & Caching
![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![MyBatis](https://img.shields.io/badge/MyBatis-000000?style=for-the-badge&logo=mybatis&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)

### Security & Docs
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=spring-security&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-black?style=for-the-badge&logo=JSON%20web%20tokens)
![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)

### Utilities & Infra
- **Spring Batch**: 대용량 데이터 배치 처리
- **Spring Cloud AWS**: AWS S3 파일 스토리지 연동
- **Lombok**: 보일러플레이트 코드 감소
- **WebSocket (Stomp)**: 실시간 양방향 통신

---

## 📂 프로젝트 구조 (Project Structure)

```bash
MM-BE/
├── src/
│   ├── main/
│   │   ├── java/com/pagoda/matchmeal/
│   │   │   ├── common/      # 전역 공통 설정 (GlobalConfig, Exception, Util)
│   │   │   ├── controller/  # API 진입점 (Controller)
│   │   │   ├── service/     # 비즈니스 로직 (Service)
│   │   │   ├── mapper/      # DB 접근 계층 (MyBatis Mapper Interface)
│   │   │   ├── model/       # DTO, VO, Entity 객체
│   │   │   ├── scheduler/   # 스케줄러 및 배치 작업
│   │   │   └── MatchmealApplication.java
│   │   └── resources/
│   │       ├── mapper/      # MyBatis XML Mapper 파일
│   │       ├── application.yml
│   │       └── ...
└── README.md
```

---

## 🚀 시작하기 (Getting Started)

### 1. 요구사항 (Prerequisites)
- **Java JDK 17** 이상
- **MySQL 8.x**
- **Redis**

### 2. 설치 및 실행 (Installation & Run)

```bash
# 프로젝트 클론
git clone [repository-url]

# 프로젝트 폴더 이동
cd MM-BE

# Database 생성 & 설정
# MySQL에서 'matchmeal' 데이터베이스를 생성하고 
# src/main/resources/application.yml에 DB 및 Redis 설정을 확인하세요.

# 빌드 및 실행 (Windows)
./gradlew bootRun

# 빌드 및 실행 (Mac/Linux)
./gradlew bootRun
```

### 3. API 문서 확인 (Swagger)
서버 실행 후 다음 URL에서 API 명세를 확인할 수 있습니다.
- `http://localhost:8080/swagger-ui/index.html`

---

## 🏗 시스템 아키텍처

```mermaid
graph TD
    Client[Client (Vue.js)] -->|REST API| Controller
    Client -->|WebSocket| WebSocketHandler
    
    subgraph "Backend Server"
        Controller --> Service
        WebSocketHandler --> Service
        Service -->|MyBatis| Mapper
        Service -->|Cache| Redis
    end
    
    Mapper -->|SQL| MySQL[(MySQL DB)]
    Redis --> RedisDB[(Redis)]
    Service -.->|API| AI_Server[AI Server]
    Service -.->|API| KakaoPay[KakaoPay]
    Service -.->|SDK| S3[AWS S3]
```

---

## 🤝 기여하기 (Contributing)
1. Fork Project
2. Create Feature Branch (`git checkout -b feature/NewFeature`)
3. Commit Changes (`git commit -m 'Add NewFeature'`)
4. Push to Branch (`git push origin feature/NewFeature`)
5. Open Pull Request

---

## 📝 라이센스
This project is licensed under the MIT License.
