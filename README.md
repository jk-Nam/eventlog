# Event Log Pipeline

## 기능

### Step 1: 이벤트 생성기
- 4가지 이벤트 타입 지원 (PAGE_VIEW, PURCHASE, CLICK, ERROR)
- 랜덤 이벤트 자동 생성 (기본 100개)
- 각 이벤트별 상세 메타데이터 포함

### Step 2: 로그 저장
- PostgreSQL 데이터베이스에 필드별 구분 저장
- 이벤트 타입별 별도 테이블 구조
- JPA를 활용한 데이터 영속화

### Step 3: 데이터 집계 분석
1. **전환 퍼널 분석**: 페이지 조회 → 클릭 → 구매 전환율
2. **매출 분석**: 일별 매출, 평균 구매액, 구매 빈도
3. **에러 모니터링**: 에러 발생 빈도, 영향받는 사용자, 심각도

### Step 4: Docker 실행 환경
- Docker Compose로 전체 스택 실행
- 앱 + DB 통합 구성
- 자동 이벤트 생성 및 분석

### Step 5: 결과 시각화
- JFreeChart 라이브러리 활용
- 4가지 차트 이미지 자동 생성 (PNG)
  1. **이벤트 타입별 분포** (Pie Chart)
  2. **전환 퍼널 분석** (Bar Chart)
  3. **일별 매출** (Bar Chart)
  4. **에러 심각도 분포** (Pie Chart)
- `charts/` 디렉토리에 저장

## 설계 및 구현

### Step 1: 이벤트 설계

**4가지 이벤트 타입 선택 이유:**

웹 서비스에서 가장 자주 분석하는 4가지 핵심 이벤트를 선택했습니다. **PAGE_VIEW**(페이지 조회)는 트래픽 분석, **PURCHASE**(구매)는 매출 분석, **CLICK**(클릭)은 UX 개선, **ERROR**(에러)는 서비스 안정성 모니터링에 필수적입니다.

특히 Step 3의 데이터 집계 분석을 고려하여 설계했습니다:
- **전환 퍼널 분석**(PAGE_VIEW → CLICK → PURCHASE)을 위한 사용자 여정 추적
- **매출 분석**을 위한 구매 상세 정보 (상품, 가격, 수량, 결제 방법)
- **에러 모니터링**을 위한 에러 심각도 및 영향 범위 파악

각 이벤트는 실무에서 필요한 최소한의 필드로 구성하되, 추후 분석에 필요한 메타데이터를 충분히 포함하도록 설계했습니다.

### Step 2: PostgreSQL 선택 이유

**PostgreSQL을 선택한 이유:**

1. **점유율 상승**: DB-Engines 랭킹에서 PostgreSQL은 지속적으로 성장하며 현재 관계형 데이터베이스 중 2위를 차지하고 있습니다. 2024년 기준 전년 대비 가장 높은 성장률을 보이고 있습니다.

2. **기업 채택 증가**: Netflix, Instagram, Uber, Apple 등 글로벌 기업들이 PostgreSQL을 핵심 데이터베이스로 사용하고 있으며, 국내 스타트업과 중견 기업에서도 MySQL에서 PostgreSQL로 전환하는 추세입니다.

3. **기술적 우수성**: JSON 컬럼 지원으로 유연한 메타데이터 저장이 가능하고, 복잡한 집계 쿼리(Window Function, CTE 등)에 최적화되어 있어 이벤트 로그 분석에 적합합니다.

4. **실무 호환성**: 대부분의 ORM 프레임워크와 호환성이 뛰어나며, 엔터프라이즈급 기능(MVCC, 트랜잭션, 복제)을 오픈소스로 제공합니다.

### 스키마 설계 원칙

이벤트 타입별로 테이블을 분리한 이유는 각 타입마다 필요한 필드가 다르기 때문입니다. 공통 정보(id, user_id, timestamp 등)는 `events` 테이블에, 타입별 상세 정보는 별도 테이블(`page_views`, `purchases`, `clicks`, `errors`)에 저장하여 정규화를 유지하면서도 쿼리 성능을 최적화했습니다. 이 구조는 새로운 이벤트 타입 추가 시에도 기존 테이블에 영향을 주지 않아 확장성이 뛰어납니다.

### 구현 과정에서 고민한 점

#### 1. JPA 엔티티 관계 매핑 문제
초기에는 `EventEntity`와 타입별 엔티티 간 양방향 OneToOne 관계로 설계했으나, 순환 참조로 인한 `null identifier` 에러가 발생했습니다. 여러 시도 끝에 **단방향 관계**로 변경하고, 각 엔티티를 독립적으로 저장하는 방식으로 해결했습니다.

#### 2. Docker 컨테이너 시작 순서 제어
애플리케이션 컨테이너가 PostgreSQL보다 먼저 시작되어 데이터베이스 연결 실패가 발생했습니다. `depends_on`과 `healthcheck`를 조합하여 PostgreSQL이 완전히 준비된 후 애플리케이션이 시작되도록 구성했습니다.

#### 3. 차트 파일 접근성
JFreeChart로 생성한 차트 이미지가 컨테이너 내부에만 저장되어 호스트에서 확인할 수 없었습니다. Docker Compose의 **볼륨 마운트**(`./charts:/app/charts`)를 사용하여 호스트 디렉토리에 직접 저장하도록 수정했습니다.

## 기술 스택

- **언어**: Java 17
- **프레임워크**: Spring Boot 4.0.6
- **데이터베이스**: PostgreSQL 16
- **빌드 도구**: Gradle 9.4.1
- **컨테이너**: Docker, Docker Compose
- **ORM**: Spring Data JPA, Hibernate
- **차트**: JFreeChart 1.5.4
- **기타**: Lombok, Jackson

## 실행 방법

### Docker Compose 실행 (권장)

```bash
# 전체 스택 실행 (PostgreSQL + 애플리케이션)
docker-compose up --build

# 백그라운드 실행
docker-compose up --build -d

# 로그 확인
docker logs -f eventlog-app

# 생성된 차트 확인
ls -lh charts/

# 종료
docker-compose down

# 볼륨까지 삭제
docker-compose down -v
```

**실행 결과:**
- ✅ 100개 이벤트 자동 생성
- ✅ PostgreSQL에 저장
- ✅ 3가지 분석 실행 (전환 퍼널, 매출, 에러)
- ✅ 4개 차트 이미지 생성 (`charts/` 디렉토리)

### 로컬 실행

```bash
# PostgreSQL 먼저 시작
docker-compose up postgres -d

# 애플리케이션 실행
./gradlew bootRun

# 또는
./gradlew build
java -jar build/libs/eventlog-0.0.1-SNAPSHOT.jar
```

## 데이터베이스 확인

```bash
# PostgreSQL 접속
docker exec -it eventlog-postgres psql -U postgres -d eventlog

# 테이블 확인
\dt

# 이벤트 개수 확인
SELECT COUNT(*) FROM events;

# 이벤트 타입별 통계
SELECT event_type, COUNT(*) FROM events GROUP BY event_type;

# 매출 분석
SELECT SUM(total_amount) as total_revenue FROM purchases;
```

## 프로젝트 구조

```
eventlog/
├── src/
│   └── main/
│       ├── java/kr/java/eventlog/
│       │   ├── dto/              # 데이터 전송 객체
│       │   ├── entity/           # JPA 엔티티
│       │   ├── generator/        # 이벤트 생성기
│       │   ├── model/            # 도메인 모델
│       │   ├── repository/       # 데이터 저장소
│       │   ├── runner/           # 애플리케이션 러너
│       │   └── service/          # 비즈니스 로직
│       └── resources/
│           └── application.yaml  # 설정 파일
├── Dockerfile                    # 애플리케이션 이미지
├── docker-compose.yml           # Docker Compose 설정
└── build.gradle                 # Gradle 빌드 설정
```

## 데이터베이스 스키마

### events (공통 이벤트 정보)
- `id` (PK): 이벤트 고유 ID
- `event_type`: 이벤트 타입
- `user_id`: 사용자 ID
- `session_id`: 세션 ID
- `timestamp`: 발생 시간

### page_views (페이지 조회 상세)
- `event_id` (PK, FK)
- `url`: 페이지 URL
- `referrer`: 유입 경로
- `duration_seconds`: 체류 시간
- `user_agent`: 사용자 에이전트

### purchases (구매 상세)
- `event_id` (PK, FK)
- `product_id`: 상품 ID
- `product_name`: 상품명
- `price`: 가격
- `quantity`: 수량
- `total_amount`: 총액
- `payment_method`: 결제 방법

### clicks (클릭 상세)
- `event_id` (PK, FK)
- `element_id`: 요소 ID
- `element_type`: 요소 타입
- `page_url`: 페이지 URL
- `x`, `y`: 클릭 좌표

### errors (에러 상세)
- `event_id` (PK, FK)
- `error_code`: 에러 코드
- `error_message`: 에러 메시지
- `severity`: 심각도
- `page`: 발생 페이지

## 환경 변수

| 변수명 | 기본값 | 설명 |
|--------|--------|------|
| `SPRING_PROFILES_ACTIVE` | dev | 프로필 (dev/prod) |
| `DB_URL` | jdbc:postgresql://localhost:5432/eventlog | DB URL |
| `DB_USERNAME` | postgres | DB 사용자 |
| `DB_PASSWORD` | postgres | DB 비밀번호 |

## 포트

- **애플리케이션**: 8081
- **PostgreSQL**: 5432

## 분석 결과 예시

### 콘솔 출력
```
[1] 전환 퍼널 분석 (페이지 조회 → 클릭 → 구매)
총 사용자 수: 5
페이지 조회 사용자: 5 (100.0%)
클릭한 사용자: 5 (100.0%)
구매한 사용자: 5 (100.0%)
클릭률 (CTR): 100.0%
전환율 (CVR): 100.0%

[2] 일별 매출 분석
날짜           | 구매자수 | 구매건수 | 총매출      | 평균구매액  | 인당매출
2026-05-22     | 5        | 23       | 3,290,000원 | 143,043원   | 658,000원

[3] 에러 발생 현황
총 에러 유형: 7
총 에러 발생 횟수: 7
```

### 생성된 차트
실행 후 `charts/` 디렉토리에 다음 차트들이 생성됩니다:

1. **event_type_distribution.png** - 이벤트 타입별 분포 (파이 차트)
2. **conversion_funnel.png** - 전환 퍼널 (바 차트)
3. **daily_revenue.png** - 일별 매출 (바 차트)
4. **error_severity.png** - 에러 심각도 분포 (파이 차트)

## 개발

```bash
# 의존성 설치
./gradlew dependencies

# 컴파일
./gradlew compileJava

# 테스트
./gradlew test

# 빌드
./gradlew build
```
