# TeamSky Learning Platform - 설계 문서

## 원본 요구사항

본 과제는 학습 플랫폼의 핵심 기능인 '단원별 문제 풀이' 및 '풀이 이력 조회' API를 구축하는 것입니다. 단순 기능을 넘어 객체지향적 설계와 데이터 처리 성능을 고려한 구현 능력을 평가합니다.

---

## 기술 스택

| 항목 | 버전 | 비고 |
|------|------|------|
| Java | 21 | 명시됨 |
| Spring Boot | 3.4.13 | LTS (2025.12 기준) |
| Spring Data JPA | 3.4.x | Spring Boot 연동 |
| MySQL | 8.4 | LTS |
| Gradle | 8.x | Build Tool |
| JUnit 5 | 5.10.x | 테스트 프레임워크 |

---

## 유즈케이스 정의

### 1. 단원별 문제 풀이

#### UC-1.1 랜덤 문제 조회
**기본 유즈케이스**
- 사용자가 단원을 선택하면 해당 단원의 풀지 않은 문제 중 1개를 랜덤 조회
- 직전에 건너뛴 문제는 제외
- 30명 이상 풀이 시 정답률 제공 (소수점 첫째자리 반올림)
- 모든 문제를 다 푼 경우 적절한 응답 반환

**확장 유즈케이스**
- **UC-1.1.E1** 난이도별 문제 필터링: 사용자가 원하는 난이도(상/중/하)의 문제만 조회
- **UC-1.1.E2** 오답 문제 재풀이: 이전에 틀린 문제만 랜덤 조회하는 모드

#### UC-1.2 문제 제출
**기본 유즈케이스**
- 객관식: 선택지 번호 제출 (복수 정답 가능)
- 주관식: 텍스트 답안 제출
- 정답/부분정답/오답 판정
- 제출 즉시 해설 반환

**확장 유즈케이스**
- **UC-1.2.E1** 제출 시간 기록: 문제 조회 시점부터 제출까지 소요 시간 저장
- **UC-1.2.E2** 힌트 사용 기능: 힌트 사용 여부 기록 (힌트 사용 시 정답률 계산 제외)

#### UC-1.3 문제 넘기기
**기본 유즈케이스**
- 현재 문제를 풀지 않고 새로운 문제를 랜덤 제공
- 직전 건너뛴 문제는 다음 추출 대상에서 제외

**확장 유즈케이스**
- **UC-1.3.E1** 넘기기 횟수 제한: 단원당 최대 넘기기 횟수 설정 가능
- **UC-1.3.E2** 북마크 기능: 넘긴 문제를 나중에 풀기 위해 북마크 저장

---

### 2. 풀이 이력 조회

#### UC-2.1 풀었던 문제 상세 조회
**기본 유즈케이스**
- 특정 문제에 대한 자신의 풀이 이력 상세 조회
- 정답 여부, 해설, 정답, 내 답안, 정답률 반환

**확장 유즈케이스**
- **UC-2.1.E1** 풀이 이력 목록 조회: 단원별 풀었던 문제 목록을 페이징하여 조회
- **UC-2.1.E2** 오답 노트: 틀린 문제만 필터링하여 조회
- **UC-2.1.E3** 풀이 통계: 단원별 정답률, 총 풀이 수, 평균 소요 시간 통계

---

### 3. 히든 유즈케이스 (도메인 식별을 위한 암묵적 요구사항)

#### UC-H.1 사용자 관리
- 사용자 생성/조회 (테스트 및 데이터 연동용)

#### UC-H.2 단원(챕터) 관리
- 단원 생성/조회

#### UC-H.3 문제 관리
- 문제 및 선택지 생성/조회
- 정답 관리

---

## 도메인 모델

### 식별된 도메인

```
┌─────────────────────────────────────────────────────────────────┐
│                        Bounded Contexts                          │
├─────────────┬─────────────┬─────────────┬─────────────┬─────────┤
│    user     │   chapter   │   problem   │ submission  │  stats  │
│  (사용자)   │   (단원)    │   (문제)    │   (제출)    │ (통계)  │
└─────────────┴─────────────┴─────────────┴─────────────┴─────────┘
```

### 도메인별 엔티티

#### user 도메인
- `User`: 사용자 정보

#### chapter 도메인
- `Chapter`: 단원 정보

#### problem 도메인
- `Problem`: 문제 (content, type, difficulty, explanation)
- `Choice`: 객관식 선택지
- `Answer`: 정답 (객관식/주관식 공통)

#### submission 도메인
- `Submission`: 문제 제출 내역
- `SkippedProblem`: 건너뛴 문제 기록
- `Bookmark`: 북마크한 문제 (확장)

#### stats 도메인
- `ProblemStats`: 문제별 통계 (캐싱용)

---

## ERD

```
┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│    users     │       │   chapters   │       │   problems   │
├──────────────┤       ├──────────────┤       ├──────────────┤
│ id (PK)      │       │ id (PK)      │◄──────│ id (PK)      │
│ name         │       │ name         │       │ chapter_id   │
│ created_at   │       │ description  │       │ content      │
│ updated_at   │       │ created_at   │       │ problem_type │
└──────────────┘       └──────────────┘       │ difficulty   │
       │                                       │ explanation  │
       │                                       │ created_at   │
       │                                       └──────────────┘
       │                                              │
       │         ┌────────────────────────────────────┼──────────────┐
       │         │                                    │              │
       │         ▼                                    ▼              ▼
       │  ┌──────────────┐                    ┌──────────────┐┌──────────────┐
       │  │   choices    │                    │   answers    ││problem_stats │
       │  ├──────────────┤                    ├──────────────┤├──────────────┤
       │  │ id (PK)      │                    │ id (PK)      ││ id (PK)      │
       │  │ problem_id   │                    │ problem_id   ││ problem_id   │
       │  │ choice_number│                    │ answer_value ││ total_count  │
       │  │ content      │                    │ answer_type  ││ correct_count│
       │  └──────────────┘                    └──────────────┘└──────────────┘
       │
       ▼
┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│ submissions  │       │skipped_probs │       │  bookmarks   │
├──────────────┤       ├──────────────┤       ├──────────────┤
│ id (PK)      │       │ id (PK)      │       │ id (PK)      │
│ user_id      │       │ user_id      │       │ user_id      │
│ problem_id   │       │ problem_id   │       │ problem_id   │
│ answer_status│       │ chapter_id   │       │ created_at   │
│ user_answer  │       │ skipped_at   │       └──────────────┘
│ time_spent   │       └──────────────┘
│ hint_used    │
│ created_at   │
└──────────────┘
```

---

## API 설계 (RESTful)

### Problem 도메인

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/problems/random` | 랜덤 문제 조회 |
| GET | `/api/v1/problems/random/wrong` | 오답 문제 재풀이 (UC-1.1.E2) |
| GET | `/api/v1/problems/{problemId}` | 문제 상세 조회 |

### Submission 도메인

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/submissions` | 문제 제출 |
| POST | `/api/v1/submissions/skip` | 문제 넘기기 |
| GET | `/api/v1/submissions/history` | 풀이 이력 목록 (UC-2.1.E1) |
| GET | `/api/v1/submissions/{submissionId}` | 풀이 상세 조회 |
| GET | `/api/v1/submissions/wrong` | 오답 노트 (UC-2.1.E2) |

### Stats 도메인

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/stats/chapters/{chapterId}` | 단원별 통계 (UC-2.1.E3) |
| GET | `/api/v1/stats/users/{userId}` | 사용자 전체 통계 |

### Bookmark 도메인 (확장)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/bookmarks` | 북마크 추가 (UC-1.3.E2) |
| GET | `/api/v1/bookmarks` | 북마크 목록 |
| DELETE | `/api/v1/bookmarks/{bookmarkId}` | 북마크 삭제 |

---

## 패키지 구조

```
src/main/java/com/teamsky/learning
├── LearningApplication.java
├── common/
│   ├── config/
│   ├── exception/
│   └── response/
├── user/
│   ├── UserController.java
│   ├── UserService.java
│   ├── UserRepository.java
│   ├── entity/
│   ├── request/
│   └── response/
├── chapter/
│   ├── ChapterController.java
│   ├── ChapterService.java
│   ├── ChapterRepository.java
│   ├── entity/
│   ├── request/
│   └── response/
├── problem/
│   ├── ProblemController.java
│   ├── ProblemService.java
│   ├── ProblemRepository.java
│   ├── ChoiceRepository.java
│   ├── AnswerRepository.java
│   ├── entity/
│   ├── request/
│   └── response/
├── submission/
│   ├── SubmissionController.java
│   ├── SubmissionService.java
│   ├── SubmissionRepository.java
│   ├── SkippedProblemRepository.java
│   ├── entity/
│   ├── request/
│   └── response/
├── stats/
│   ├── StatsController.java
│   ├── StatsService.java
│   ├── ProblemStatsRepository.java
│   ├── entity/
│   └── response/
└── bookmark/
    ├── BookmarkController.java
    ├── BookmarkService.java
    ├── BookmarkRepository.java
    ├── entity/
    ├── request/
    └── response/
```

---

## 테스트 전략

### 단위 테스트 (Unit Test)
- Service 레이어 비즈니스 로직 테스트
- 정답/부분정답/오답 판정 로직
- 정답률 계산 로직

### 통합 테스트 (Integration Test)
- Controller 레이어 API 테스트
- Repository 레이어 쿼리 테스트

### 테스트 케이스 매핑

| 유즈케이스 | 테스트 클래스 |
|-----------|--------------|
| UC-1.1 | `ProblemServiceTest`, `ProblemControllerTest` |
| UC-1.1.E1 | `ProblemServiceTest#filterByDifficulty` |
| UC-1.1.E2 | `ProblemServiceTest#getWrongProblems` |
| UC-1.2 | `SubmissionServiceTest`, `SubmissionControllerTest` |
| UC-1.2.E1 | `SubmissionServiceTest#recordTimeSpent` |
| UC-1.2.E2 | `SubmissionServiceTest#useHint` |
| UC-1.3 | `SubmissionServiceTest#skipProblem` |
| UC-1.3.E2 | `BookmarkServiceTest` |
| UC-2.1 | `SubmissionServiceTest#getDetail` |
| UC-2.1.E1 | `SubmissionServiceTest#getHistoryList` |
| UC-2.1.E2 | `SubmissionServiceTest#getWrongNotes` |
| UC-2.1.E3 | `StatsServiceTest` |

---

## 성능 고려사항

### 정답률 계산 최적화
- `problem_stats` 테이블에 집계 데이터 캐싱
- 제출 시 비동기로 통계 업데이트
- 30명 미만 체크를 위한 `total_count` 컬럼 활용

### 쿼리 최적화
- 랜덤 문제 조회 시 제외 대상 서브쿼리 최적화
- 복합 인덱스 설계: `(chapter_id, user_id)`, `(problem_id, user_id)`

---

## 구현 우선순위

1. **Phase 1**: 기본 엔티티 및 Repository 구현
2. **Phase 2**: 기본 유즈케이스 구현 (UC-1.1, UC-1.2, UC-1.3, UC-2.1)
3. **Phase 3**: 확장 유즈케이스 구현
4. **Phase 4**: 테스트 코드 작성 및 리팩토링
5. **Phase 5**: Docker Compose 및 문서화
