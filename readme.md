## CareerLink Monorepo Setup

CareerLink은 QR 기반 학생-상담사 상담 연결 웹앱입니다. 이 저장소는 학생 접수, 학생 현황 폴링, 상담사 처리, 관리자 유형 관리 기능이 포함된 Vue 3 SPA와 Spring Boot 백엔드를 함께 관리합니다.

### 구조

- `frontend`: Vue 3 + Vite + TypeScript SPA
- `backend`: Spring Boot + Spring Web + Spring Data JPA + H2
- `docker-compose.yml`: backend / frontend 통합 실행 구성

### 주요 화면

- `/receive`: 학생 입장 정보 입력 후 홈 진입
- `/home`: 학생 상담 유형 선택 및 다건 상담 신청
- `/status`: 학생 상담 목록 조회
- `/status/:id`: 학생 상담 상태 조회, 접수 단계 취소, 10초 폴링
- `/counselor`: 상담사 입장, 접수 목록 조회, 수락/완료 처리
- `/admin`: 상담 유형 조회, 추가, 수정, 삭제

### 로컬 개발 실행

#### Backend

```bash
./backend/gradlew -p backend bootRun
```

- 기본 주소: `http://localhost:8080`
- 헬스 체크: `http://localhost:8080/api/health`
- 상담 유형 조회: `http://localhost:8080/api/types`
- H2 콘솔: `http://localhost:8080/h2-console`

#### Frontend

```bash
npm --prefix frontend install
npm --prefix frontend run dev
```

- 기본 주소: `http://localhost:5173`
- `/api` 요청은 기본적으로 `http://localhost:8080`으로 프록시됩니다.

### 테스트

#### Backend 테스트 / 패키징

```bash
./backend/gradlew -p backend clean test bootJar
```

- 학생/상담사/관리자 API 통합 테스트
- 상태 전이 서비스 테스트
- 개인정보 마스킹 통합 테스트
- 전체 API 플로우 통합 테스트 (`FullConsultationFlowIntegrationTest`)
- PRD v3 시나리오 통합 테스트 (`PrdV3ScenarioIntegrationTest`)
- Testcontainers 기반 PostgreSQL 통합 테스트 (`PostgresIntegrationTest`, Docker 가능 환경에서 자동 실행)

#### Frontend 빌드

```bash
npm --prefix frontend run build
```

#### Frontend E2E (Playwright)

Playwright는 프론트엔드 dev server를 자동으로 띄우며, 백엔드는 별도로 `http://localhost:8080`에서 실행 중이어야 합니다.

```bash
npm --prefix frontend run test:e2e
```

브라우저가 설치되어 있지 않다면 먼저 아래를 실행합니다.

```bash
npx --prefix frontend playwright install chromium
```

포함된 E2E 스모크 테스트:

- `/receive` → `/home` 2단계 진입 후 상담 신청
- 동일 학생의 다건 신청 후 `/status` 목록 확인
- `/status/:id` 상세 화면에서 `RECEIVED` 상태 취소 확인

### Docker Compose 실행

```bash
docker compose config
docker compose up --build -d
docker compose ps
curl http://localhost:8080/api/health
curl http://localhost:8080/api/types
curl http://localhost:5174
docker compose down
```

- frontend: `http://localhost:5174`
- backend: `http://localhost:8080`
- postgres: Docker Compose 내부 `postgres:5432`
- backend는 `SPRING_PROFILES_ACTIVE=docker`로 실행됩니다.
- docker 프로파일은 PostgreSQL 컨테이너를 사용하며, 기본 계정은 `careerlink / careerlink`, DB 이름은 `careerlink`입니다. 필요 시 `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `SPRING_DATASOURCE_*` 환경변수로 오버라이드할 수 있습니다.
- 범용 운영 프로파일은 `application-prod.yml`이며, docker 프로파일은 동일한 PostgreSQL/HikariCP 설정을 유지하되 Compose 전용 호스트명(`postgres`)을 사용하도록 별도로 둡니다.

### 데이터 / 프로파일

- local 프로파일: `backend/data` 경로의 파일 기반 H2 사용
- test 프로파일: H2 in-memory 사용
- prod 프로파일: PostgreSQL + HikariCP 커넥션 풀 사용
- docker 프로파일: PostgreSQL 사용 (Compose 전용 호스트명 반영)
- 기본 상담 유형 5종은 애플리케이션 시작 시 자동 시드됩니다.

### 운영 DB / 스키마 전략

- `application-prod.yml`에는 PostgreSQL 연결 정보와 HikariCP 기본값이 정의되어 있습니다.
- HikariCP 기본값:
  - `maximum-pool-size=10`
  - `minimum-idle=5`
  - `connection-timeout=30000`
  - `idle-timeout=600000`
  - `max-lifetime=1800000`
- 현재 단계에서는 `spring.jpa.hibernate.ddl-auto=update`를 유지합니다.
  - 이유: 데이터 모델이 아직 자주 바뀌는 초기 개발 단계이며, 지금 Flyway/Liquibase를 도입하면 마이그레이션 스크립트 관리 비용이 기능 개발 속도를 크게 떨어뜨릴 수 있습니다.
  - 대신 운영 리스크를 인지하고, 스키마가 안정화되는 시점에 Flyway로 전환하여 `ddl-auto=validate` 체계로 옮기는 것을 권장합니다.

### PostgreSQL 검증

- docker compose 환경에서 PostgreSQL 기반 backend/frontend/postgres 기동을 실기동 검증했습니다.
- PostgreSQL named volume의 데이터 영속성은 상담 접수 후 backend/postgres 재시작 뒤 동일 `studentPhone` 조회로 재확인했습니다.
- `PostgresIntegrationTest`는 Testcontainers로 PostgreSQL 컨테이너를 띄워 학생 접수/취소, 상담사 수락/완료, 관리자 유형 생성/삭제 제약을 자동 검증합니다.

### 참고

- 학생 상태 화면은 `COMPLETED` 전까지 10초 주기로 자동 갱신됩니다.
- 학생은 `/receive`에서 입장 정보를 저장한 뒤 `/home`에서 여러 상담을 연속 신청할 수 있습니다.
- 학생 상담 목록과 상세 화면에는 `CANCELLED` 상태가 유지되며, `RECEIVED` 단계에서만 취소할 수 있습니다.
- 상담사 화면은 담당 상담 유형 기준으로 `RECEIVED`, `IN_PROGRESS` 목록을 조회합니다.
- 관리자 화면에서 추가/수정한 상담 유형은 학생/상담사 API 조회에도 즉시 반영되며, 참조 중인 유형은 삭제할 수 없습니다.
