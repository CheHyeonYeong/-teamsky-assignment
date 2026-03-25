# TeamSky Learning

## 1. 프로젝트 개요

학습 플랫폼의 핵심 기능인 단원별 문제 풀이, 문제 제출, 풀이 상세 조회를 중심으로 구현한 API 서버입니다.

과제 요구사항 충족에 집중하되, 아래 항목도 함께 고려했습니다.

- 객체지향적 구조
- 멀티 모듈 아키텍처
- DDD 스타일 패키지 분리
- MySQL 기준 쿼리 성능
- 테스트 가능성
- Swagger 및 실행 문서화

## 2. 기술 스택

- Java 21
- Spring Boot 3.4.1
- Spring Web
- Spring Data JPA
- MySQL 8
- Gradle
- springdoc-openapi

## 3. 모듈 및 패키지 구조

프로젝트는 멀티 모듈로 분리했습니다.

- `app`
  - Spring Boot 진입점
  - 런타임 설정 파일
  - 실제 실행 가능한 애플리케이션 모듈
- `core`
  - 도메인 로직
  - 컨트롤러
  - 서비스
  - JPA 리포지토리
  - 엔티티
  - 테스트

`core` 내부는 도메인 경계 기준으로 나눴습니다.

- `problem`
- `submission`
- `stats`
- `bookmark`
- `chapter`
- `user`
- `shared`

각 컨텍스트는 아래 레이어로 분리했습니다.

- `domain`
  - 엔티티와 도메인 상태
- `application`
  - 유스케이스를 조합하는 서비스
- `infrastructure`
  - JPA 리포지토리와 영속성 구현
- `presentation`
  - 컨트롤러, request/response DTO

완전한 포트-어댑터 구조까지 밀어붙이기보다는, 과제 규모에서 설명 가능하고 테스트 가능한 수준의 DDD 스타일 구조를 선택했습니다.

## 4. 설계 의도

### 4.1 상태 테이블 도입

초기 구조처럼 `submissions` 만으로 모든 상태를 계산하면, 랜덤 문제 조회, 오답 재출제, 통계 계산이 모두 비싸집니다.

그래서 아래 요약 상태 테이블을 도입했습니다.

- `user_problem_state`
  - 사용자별 문제 최신 상태 저장
  - 미풀이 / 현재 오답 문제 조회 최적화
- `user_chapter_state`
  - 사용자별 단원 마지막 skip 문제 저장
  - 직전 건너뛴 문제 1건 제외
- `user_submission_stats`
  - 사용자 전체 제출 집계
- `user_chapter_submission_stats`
  - 사용자-단원별 제출 집계
- `problem_stats`
  - 문제별 정답률 집계

이 구조로 `ORDER BY RAND()` 와 대규모 `submissions` 스캔을 줄이고, 읽기 성능을 안정화했습니다.

### 4.2 안정적인 API 계약

Spring `Page` 객체를 그대로 응답하지 않고 `PageResponse<T>` 로 감쌌습니다.  
프레임워크 내부 JSON 모양에 API 계약이 종속되지 않도록 하기 위한 선택입니다.

### 4.3 운영/개발 환경 분리

- 기본 프로필은 `ddl-auto=validate`
- 로컬 프로필은 `ddl-auto=update`

운영 기본값은 보수적으로 유지하고, 로컬 개발 편의성은 별도 프로필로 분리했습니다.

## 5. 요구사항 대응

### 5.1 단원별 랜덤 문제 조회

다음 요구사항을 반영했습니다.

- 사용자가 선택한 단원에서 문제를 1개씩 반환
- 객관식 선택지는 5개 기준으로 응답
- 단일 정답 / 복수 정답 문제 모두 지원
- 사용자가 아직 풀지 않은 문제만 랜덤 조회
- 문제 넘기기 시 같은 단원에서 새 문제를 다시 조회
- 특정 사용자, 특정 단원 기준으로 직전에 skip 한 문제 1건은 다음 랜덤 대상에서 제외
- 더 이상 제공할 문제가 없으면 `NO_MORE_PROBLEMS` 비즈니스 예외를 통해 `404 NOT_FOUND` 응답

정답률 계산 규칙도 과제 기준에 맞췄습니다.

- 동일 문제를 푼 사용자가 30명 이상일 때만 노출
- `PARTIAL` 은 정답률 계산에서 오답으로 간주
- 소수점 첫째 자리에서 반올림
- 30명 미만이면 `null`

정답률은 제출 횟수가 아니라 사용자 수 기준이 되도록, 동일 사용자의 첫 제출만 `problem_stats` 에 반영합니다.

### 5.2 문제 제출

다음 제출 방식을 지원합니다.

- 객관식: 선택지 번호 목록 제출
- 주관식: 문자열 제출

채점 결과는 아래 3가지입니다.

- `CORRECT`
- `PARTIAL`
- `WRONG`

객관식 부분 정답은 과제 문구에 맞춰 구현했습니다.

- 정답을 하나라도 포함하면 `PARTIAL`
- 예: 정답 `[1,2]`, 제출 `[1,3]` -> `PARTIAL`

제출 직후 바로 아래 정보를 반환합니다.

- 문제 ID
- 채점 결과
- 해설
- 정답 목록

### 5.3 풀었던 문제 상세 조회

사용자가 특정 문제에 대해 가장 최근에 제출한 풀이 결과를 상세 조회할 수 있습니다.

응답에는 아래 정보가 포함됩니다.

- 문제 ID
- 채점 결과
- 해설
- 정답 목록
- 사용자 답안
- 정답률

과제 필수 요구는 상세 조회이지만, 추가로 단원별 풀이 이력 조회 API도 함께 제공합니다.

## 6. 추가로 고려한 사항

### 6.1 성능

- `ORDER BY RAND()` 제거
- 상태 테이블 기반 조회로 랜덤 문제 / 오답 문제 조회 최적화
- 제출 이력 / 오답 이력 / 통계 조회는 DTO projection 및 집계 테이블 사용
- MySQL 기준 인덱스 추가

최근 부하 테스트 기준 결과 예시는 아래와 같습니다.

- `mixed / 10000 req / 250 concurrency / timeout 10s` -> 성공률 `100%`
- `mixed / 10000 req / 500 concurrency / timeout 10s` -> 성공률 `100%`
- `mixed / 5000 req / 800 concurrency / timeout 10s` -> 성공률 `100%`

### 6.2 테스트

- 서비스 단위 테스트
- MySQL 기반 리포지토리 테스트
- 전체 테스트는 Java 21 기준으로 실행
- H2 대신 MySQL 중심으로 구성

### 6.3 문서화

- Swagger UI 제공
- 실행 방법, 테스트 방법, 마이그레이션 방법, 부하 테스트 방법 정리

## 7. 실행 방법

### 7.1 로컬 실행

기본 프로필은 스키마를 자동 수정하지 않습니다.  
로컬 개발 시에는 `local` 프로필을 사용합니다.

```powershell
$env:SPRING_PROFILES_ACTIVE="local"
.\scripts\gradlew-java21.ps1 :app:bootRun
```

로컬 프로필은 아래 환경변수를 우선 사용합니다.

- `LOCAL_DB_URL`
- `LOCAL_DB_USERNAME`
- `LOCAL_DB_PASSWORD`

기본 로컬 DB 값은 아래와 같습니다.

```text
jdbc:mysql://localhost:3306/learning?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true
```

### 7.2 전체 테스트

```powershell
.\scripts\gradlew-java21.ps1 test
```

테스트 프로필은 아래 환경변수를 사용할 수 있습니다.

- `TEST_DB_URL`
- `TEST_DB_USERNAME`
- `TEST_DB_PASSWORD`

## 8. Docker Compose

MySQL과 애플리케이션을 한 번에 띄울 수 있도록 구성했습니다.

```powershell
docker compose up -d --build
```

기본 엔드포인트:

- 앱: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI: `http://localhost:8080/api-docs`

## 9. 기존 DB 마이그레이션

기존 스키마를 사용 중인 경우에는 아래 SQL을 먼저 적용하면 됩니다.

```powershell
mysql --host localhost --port 3306 --user root --password=root learning < scripts/migrations/20260325_backfill_state_and_stats.sql
```

이 마이그레이션은 아래를 처리합니다.

- 상태 테이블 생성
- 집계 테이블 생성
- 기존 `submissions` 기반 backfill
- 레거시 `skipped_problems` 가 있으면 마지막 skip 상태 이관

## 10. Swagger

Swagger 문서는 아래 경로로 확인할 수 있습니다.

- `/swagger-ui.html`
- `/api-docs`

## 11. 부하 테스트

앱 실행 후 아래 스크립트로 부하를 걸 수 있습니다.

```powershell
python scripts/load_test.py --scenario mixed --base-url http://localhost:8080 --requests 10000 --concurrency 250 --timeout 10
```

지원 시나리오:

- `mixed`
- `read-problem`
- `read-history`
- `read-wrong`
- `read-user-stats`
- `write-subjective`

## 12. 과제 기준 최종 정리

필수 요구사항은 충족하도록 구현했습니다.

- 단원별 랜덤 문제 조회
- 직전 skip 문제 제외
- 정답률 30명 기준 처리
- 객관식/주관식 제출
- 과제 기준 부분 정답 처리
- 제출 즉시 채점 결과 반환
- 풀이 상세 조회
- request validation

우대사항도 대부분 반영했습니다.

- 멀티 모듈 아키텍처
- DDD 스타일 패키지 구조
- 단위 테스트 및 통합 테스트
- Swagger 문서화
- README 기반 설계 설명
- MySQL 중심 성능 개선
- Docker Compose 환경 구성
