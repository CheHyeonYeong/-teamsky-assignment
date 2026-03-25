# CLAUDE.md - Claude 동작 가이드

> 이 문서는 Claude가 이 프로젝트에서 작업할 때 따라야 할 단계별 가이드입니다.

> Gradle은 Java 21로 실행해야 합니다. PowerShell에서는 `.\scripts\gradlew-java21.ps1`를 기본 명령으로 사용합니다.

---

## Step 1: 프로젝트 컨텍스트 파악

### 1.1 필수 파일 읽기
```
1. PLAN.md 읽기 → 유즈케이스, ERD, API 설계 파악
2. CLAUDE.md 읽기 → 코딩 규칙, 동작 방식 파악
3. build.gradle 읽기 → 의존성 및 버전 확인
```

### 1.2 프로젝트 구조 확인
```
src/main/java/com/teamsky/learning/
├── common/          → 공통 모듈 (config, exception, response)
├── user/            → 사용자 도메인
├── chapter/         → 단원 도메인
├── problem/         → 문제 도메인 (핵심)
├── submission/      → 제출 도메인 (핵심)
├── stats/           → 통계 도메인
└── bookmark/        → 북마크 도메인 (확장)
```

---

## Step 2: 작업 요청 분석

### 2.1 요청 유형 판별
```
[새 기능 추가]     → Step 3으로
[버그 수정]        → Step 4로
[리팩토링]         → Step 5로
[테스트 추가]      → Step 6으로
[문서화]           → Step 7로
```

### 2.2 영향 범위 파악
- 어떤 도메인에 영향을 주는가?
- 기존 코드 변경이 필요한가?
- 새 파일 생성이 필요한가?

---

## Step 3: 새 기능 추가 워크플로우

### 3.1 유즈케이스 확인
```
PLAN.md에서 해당 유즈케이스 찾기
→ 없으면 사용자에게 요구사항 명확화 요청
```

### 3.2 TDD 방식 구현
```
① 테스트 코드 먼저 작성
   - src/test/java/com/teamsky/learning/{domain}/{Domain}ServiceTest.java
   - @DisplayName으로 한글 설명 추가
   - given-when-then 패턴 사용

② 실패하는 테스트 확인
   .\scripts\gradlew-java21.ps1 test --tests "{테스트클래스명}"

③ 프로덕션 코드 구현
   - Entity → Repository → Service → Controller 순서
   - Record 클래스로 Request/Response 작성

④ 테스트 통과 확인
   .\scripts\gradlew-java21.ps1 test

⑤ 리팩토링 (필요시)
```

### 3.3 패키지 구조 준수
```java
// 새 도메인 추가 시 이 구조 따르기
/{domain}
├── {Domain}Controller.java    // @RestController, @RequestMapping
├── {Domain}Service.java       // @Service, @Transactional
├── {Domain}Repository.java    // extends JpaRepository
├── entity/
│   └── {Entity}.java          // @Entity, @Table
├── request/
│   └── {Action}Request.java   // record, @NotNull 등 validation
└── response/
    └── {Action}Response.java  // record, static of() 메서드
```

### 3.4 코딩 스타일
```java
// ✅ 좋은 예 - 함수형, Record 사용
public record ProblemResponse(Long problemId, String content) {
    public static ProblemResponse of(Problem problem) {
        return new ProblemResponse(problem.getId(), problem.getContent());
    }
}

// ✅ 좋은 예 - Stream API
List<Long> ids = problems.stream()
    .map(Problem::getId)
    .toList();

// ✅ 좋은 예 - Optional 체이닝
return repository.findById(id)
    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

// ❌ 나쁜 예 - 명령형 스타일
List<Long> ids = new ArrayList<>();
for (Problem p : problems) {
    ids.add(p.getId());
}
```

---

## Step 4: 버그 수정 워크플로우

### 4.1 버그 재현
```
① 관련 테스트 코드 확인
② 버그 재현하는 테스트 케이스 작성
③ 테스트 실패 확인
```

### 4.2 원인 분석
```
① 에러 로그 분석
② 관련 Service/Repository 코드 확인
③ 데이터 흐름 추적
```

### 4.3 수정 및 검증
```
① 코드 수정
② 테스트 통과 확인
③ 기존 테스트 영향 없는지 확인 (.\scripts\gradlew-java21.ps1 test)
```

---

## Step 5: 리팩토링 워크플로우

### 5.1 리팩토링 전 확인
```
① 기존 테스트 모두 통과하는지 확인
② 리팩토링 범위 명확히 정의
③ 영향받는 코드 파악
```

### 5.2 안전한 리팩토링
```
① 작은 단위로 변경
② 변경마다 테스트 실행
③ 기능 변경 없이 구조만 개선
```

### 5.3 리팩토링 패턴
```java
// 메서드 추출
private AnswerStatus judgeMultipleChoice(...) { }
private AnswerStatus judgeSubjective(...) { }

// 조건문 → 다형성
sealed interface AnswerJudger permits MultipleChoiceJudger, SubjectiveJudger {}
```

---

## Step 6: 테스트 추가 워크플로우

### 6.1 테스트 구조
```java
@ExtendWith(MockitoExtension.class)
@DisplayName("{도메인} 서비스 테스트")
class {Domain}ServiceTest {

    @InjectMocks
    private {Domain}Service service;

    @Mock
    private {Domain}Repository repository;

    @Nested
    @DisplayName("{기능명}")
    class {FeatureName} {

        @Test
        @DisplayName("{시나리오 설명}")
        void should{Result}When{Condition}() {
            // given

            // when

            // then
        }
    }
}
```

### 6.2 테스트 커버리지 우선순위
```
1. 핵심 비즈니스 로직 (정답 판정, 정답률 계산)
2. 예외 상황 처리 (문제 없음, 사용자 없음)
3. 경계값 테스트 (30명 기준 정답률)
4. API 통합 테스트
```

### 6.3 Mock 사용 규칙
```java
// given 절에서 Mock 설정
given(repository.findById(1L)).willReturn(Optional.of(entity));
doNothing().when(service).validateExists(1L);

// then 절에서 검증
verify(repository).save(any(Entity.class));
assertThat(result.status()).isEqualTo(AnswerStatus.CORRECT);
```

---

## Step 7: 문서화 워크플로우

### 7.1 코드 문서화
```java
// Swagger 어노테이션 사용
@Operation(summary = "랜덤 문제 조회", description = "풀지 않은 문제 중 1개 랜덤 조회")
@GetMapping("/random")
public ResponseEntity<ProblemResponse> getRandomProblem(...) { }
```

### 7.2 README.md 작성 시
```markdown
## 실행 방법
## API 명세
## 설계 의도
## 기술적 결정 사항
```

---

## Step 8: 커밋 & 빌드

### 8.1 커밋 전 체크리스트
```
□ 테스트 모두 통과 (.\scripts\gradlew-java21.ps1 test)
□ 빌드 성공 (.\scripts\gradlew-java21.ps1 build)
□ 코드 스타일 일관성
□ 불필요한 import 제거
□ 주석/로그 정리
```

### 8.2 커밋 메시지 형식
```
feat: 랜덤 문제 조회 API 구현

- 사용자가 풀지 않은 문제 중 랜덤 1개 조회
- 직전 건너뛴 문제 제외 로직 추가
- 정답률 계산 (30명 이상일 때만 반환)
```

### 8.3 커밋 타입
| Type | 설명 |
|------|------|
| feat | 새로운 기능 |
| fix | 버그 수정 |
| refactor | 리팩토링 |
| test | 테스트 추가/수정 |
| docs | 문서 수정 |
| chore | 빌드, 설정 변경 |

**⚠️ 주의: 커밋 메시지에 AI 명시 금지**

---

## Step 9: 빌드 명령어

### 9.1 기본 명령어
```bash
# 빌드 (테스트 포함)
.\scripts\gradlew-java21.ps1 build

# 테스트만 실행
.\scripts\gradlew-java21.ps1 test

# 특정 테스트만 실행
.\scripts\gradlew-java21.ps1 test --tests "SubmissionServiceTest"

# 애플리케이션 실행
.\scripts\gradlew-java21.ps1 bootRun

# 클린 빌드
.\scripts\gradlew-java21.ps1 clean build
```

### 9.2 Docker
```bash
# MySQL + App 실행
docker-compose up -d

# 로그 확인
docker-compose logs -f app
```

---

## 기술 스택 (필수 준수)

| 항목 | 버전 |
|------|------|
| Java | 21 |
| Spring Boot | 3.4.x |
| MySQL | 8.4 |
| Gradle | 8.x |
| JUnit 5 | 5.10.x |

---

## 핵심 비즈니스 로직 참고

### 정답 판정 로직
```
객관식:
- 모든 정답 선택 → CORRECT
- 정답 1개 이상 포함 → PARTIAL
- 정답 0개 → WRONG

주관식:
- 정답과 일치 (대소문자 무시) → CORRECT
- 불일치 → WRONG
```

### 정답률 계산
```
- 30명 이상 풀이 → 정답률 반환 (소수점 첫째자리 반올림)
- 30명 미만 → null 반환
- 부분 정답 → 오답으로 간주
```

### 문제 넘기기
```
- 직전 1회 건너뛴 문제만 제외
- 새 문제 랜덤 선택
```

---

## 유즈케이스 참조

자세한 내용은 `PLAN.md` 참조

| 코드 | 유즈케이스 |
|------|-----------|
| UC-1.1 | 랜덤 문제 조회 |
| UC-1.2 | 문제 제출 |
| UC-1.3 | 문제 넘기기 |
| UC-2.1 | 풀이 상세 조회 |
| UC-1.1.E1 | 난이도별 필터링 |
| UC-1.1.E2 | 오답 재풀이 |
| UC-2.1.E1 | 풀이 이력 목록 |
| UC-2.1.E2 | 오답 노트 |
| UC-2.1.E3 | 풀이 통계 |
