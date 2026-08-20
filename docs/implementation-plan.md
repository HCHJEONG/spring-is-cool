# Implementation Plan

이 문서는 `implementation-plan-original.md`의 원칙을 바탕으로, 프로젝트의 큰 구현 흐름을 milestone 단위로 정리한 계획이다. 앞으로 구현 계획의 source of truth는 이 파일이다.

이 계획은 상세 ticket 목록이 아니다. 각 milestone은 하나의 제품적 질문을 검증할 만큼 충분히 큰 단위로 잡는다. 세부 작업 순서는 각 milestone에 들어갈 때 별도 문서로 쪼갤 수 있다.

가장 중요한 방향은 다음이다.

> Backend is the product.
> Text adventure is the interaction model.
> The cinematic renderer is the primary differentiator.
> Scene and world state must stay separate.
> AI may create intent, but deterministic renderer controls execution.
> External systems are adapters, not the core.

## 0. 전체 전략

이 프로젝트는 일반적인 Spring CRUD backend에서 출발하지 않는다. 첫 제품 표면은 SSH로 접속하는 terminal world다. 사용자는 GUI나 별도 client 없이 Spring Boot 서버에 접속하고, 서버가 직접 제어하는 cinematic Unicode/text scene과 prompt를 경험한다.

초기에는 의도적으로 작은 fake world를 사용한다. DB, OWL, ledger, MQTT, MCP, governance, full agent orchestration은 중요한 장기 후보지만, renderer와 terminal experience가 재미있다는 사실을 증명하기 전에는 중심으로 올리지 않는다.

전체 구현 순서는 다음 흐름을 따른다.

```text
Spring Boot SSH product surface
     ↓
Cinematic renderer and prompt interaction
     ↓
Prototype world state and command loop
     ↓
Scene model, layout, timing, style, validation
     ↓
World runtime, events, evidence, persistence
     ↓
AI Director and generated scenes
     ↓
Adapters: HTTP, CLI/TUI, MCP, external systems
     ↓
Semantic model, ledger, governance, physical/personal worlds
```

## 1. Milestone 1: SSH Cinematic Intro Prototype

### Goal

사용자가 SSH로 Spring Boot 서버에 접속하면, 서버가 직접 제어하는 cinematic Unicode/text intro scene이 재생되고, intro 종료 후 `> _` prompt가 표시된다.

이 milestone의 질문은 다음이다.

> Do users want to stay in this terminal world?

### Scope

- Kotlin + Gradle Kotlin DSL + Spring Boot baseline
- non-web Spring Boot application
- Apache MINA SSHD 기반 embedded SSH server
- demo user/password 기반 localhost SSH 접속
- hardcoded intro scene
- `CinematicTextRenderer`
- `TerminalOutput` 같은 ANSI/control sequence helper
- semantic style의 첫 형태
- line-level, character-level timing
- pause, typewriter effect, cursor hide/show, clear screen
- intro 이후 최소 prompt loop
- `quit`/`exit` 종료
- session disconnect/interruption cleanup
- Dockerfile
- runtime SSH configuration externalization
- clean clone 기반 local Docker image build
- image tar export
- `dev-demo` target-specific manual deployment script
- `aws-demo` target-specific manual deployment script
- remote docker load/run validation
- dev-demo direct SSH validation
- aws-demo bastion SSH validation
- `.fordeploy/README.md` manual deployment notes

### Key Components

- `EmbeddedSshServer`
  - Spring bean lifecycle 안에서 Apache MINA SSHD server를 시작하고 중지한다.
  - SSH bind address, port, credential, host key path는 runtime configuration으로 받는다.
- `WelcomeShellFactory`
  - 접속마다 shell command instance를 생성한다.
  - input/output/error stream과 exit callback을 session lifecycle에 연결한다.
- `IntroSceneProvider`
  - 첫 hardcoded intro scene을 제공한다.
  - 나중의 AI Director 또는 scene generation을 대체하는 fake component 역할을 한다.
- `CinematicTextRenderer`
  - scene model을 timed terminal output으로 변환한다.
  - timing, ANSI style, cursor, clear screen, typewriter reveal을 통제한다.
- `TerminalOutput`
  - raw ANSI/control sequence 생성을 한 곳에 모은다.
  - application logic에 terminal escape sequence가 흩어지지 않게 한다.
- prototype `SceneModel`
  - 아직 최종 Scene IR이 아니다.
  - text, style, reveal mode, delay before/after, character delay, clear behavior 같은 최소 의도를 담는다.

### Technical Baseline

```text
Language: Kotlin
Build system: Gradle
Gradle DSL: Kotlin DSL
JDK distribution: Eclipse Temurin
JDK version: 21
Java language version: 21
Packaging: executable Jar
Spring Boot config format: YAML
Initial application style: non-web Spring Boot process
SSH implementation: Apache MINA SSHD
```

첫 milestone에서는 Spring Web, Security, JPA, database, Actuator, Docker Compose Support를 추가하지 않는다. SSH intro 검증에는 HTTP API나 DB가 필요하지 않고, 이 단계에서 framework surface가 커지면 renderer와 terminal feel의 판단이 흐려진다.

ANSI escape sequence는 직접 생성하되 `TerminalOutput` 같은 작은 helper boundary를 둔다. Unicode width는 초기에는 단순하게 시작하지만, CJK/emoji 폭 계산을 위해 ICU4J 또는 Java wcwidth 계열 library 도입 가능성을 열어둔다.

### Suggested Work Items

1. Kotlin + Gradle Kotlin DSL 기반 Spring Boot skeleton을 만든다.
2. JDK baseline을 Eclipse Temurin JDK 21로 맞춘다.
3. non-web Spring Boot application으로 시작한다.
4. Apache MINA SSHD dependency를 추가한다.
5. `application.yaml`에 keep-alive와 `spring-is-cool.ssh` runtime 설정을 추가한다.
6. typed configuration properties를 만든다.
7. `EmbeddedSshServer`를 Spring bean lifecycle에 연결한다.
8. demo user/password 인증을 임시로 붙인다.
9. `WelcomeShellFactory`와 per-session shell command를 만든다.
10. char input, echo, backspace, enter, `quit`/`exit` 종료를 구현한다.
11. renderer package를 만들고 SSH adapter와 renderer core의 경계를 나눈다.
12. `TerminalOutput`에 write, style, clear screen, hide/show cursor helper를 둔다.
13. `IntroSceneProvider`가 hardcoded scene data를 반환하게 한다.
14. line-level delay와 character-level typewriter reveal을 구현한다.
15. punctuation delay를 넣어 문장 리듬을 시험한다.
16. `SceneStyle`과 `RevealMode`를 도입해 scene data가 연출 의도를 말하게 한다.
17. prompt를 단순 shell prompt가 아니라 세계가 계속 열린다는 느낌으로 다듬는다.
18. local `./gradlew test`, `./gradlew bootRun`, SSH 접속 테스트를 반복한다.
19. Dockerfile을 만들고 executable Jar 기반 image를 생성한다.
20. clean clone build, image tar export, target transfer, remote docker load/run scripts를 만든다.
21. `dev-demo`와 `aws-demo`에서 실제 SSH 접속으로 검증한다.

### Non-Goals

- database
- LLM provider
- ontology
- full command loop
- complete Scene IR
- production-grade SSH security
- CI/CD or registry-based deployment
- production-grade secret management

### Success Criteria

- 로컬 Spring Boot 앱이 실행된다.
- SSH 접속 직후 intro가 자동 재생된다.
- Unicode, spacing, color, timing이 함께 작동한다.
- intro는 30~60초 안에 끝난다.
- prompt가 세계가 계속될 수 있다는 느낌을 준다.
- renderer core와 SSH adapter가 분리되어 있다.
- Docker container에서도 같은 SSH intro experience가 작동한다.
- deployment scripts는 target-specific이고 clean clone build, tar transfer, remote docker load/run 흐름을 따른다.
- env/secrets와 SSH host key는 Docker image에 포함되지 않는다.
- `dev-demo`와 `aws-demo`에서 실제 SSH 접속으로 검증할 수 있다.

### Validation Commands

```bash
.fordeploy/deploy-dev-demo.sh
ssh demo@192.168.0.104 -p 2222
```

```bash
.fordeploy/deploy-aws-demo.sh
ssh -J aws-bastion demo@172.31.76.194 -p 2222
```

## 2. Milestone 2: First Interactive World Loop

### Goal

intro 이후 사용자가 최소한의 text-adventure command로 작은 hardcoded world와 상호작용하고, 그 결과가 cinematic response scene으로 렌더링된다.

이 milestone의 질문은 다음이다.

> 사용자가 머물기로 했을 때, 이 세계가 처음 몇 분 동안 살아 있는 것처럼 반응하는가?

### Scope

- `LOOK`, `ANSWER`, `HELP`, `QUIT` command
- `L`, `?`, `EXIT` 같은 작은 alias
- unknown command에 대한 세계 안의 반응
- SSH session 단위 in-memory world state
- 전화가 울리는지, 전화를 받았는지 같은 작은 상태
- command parser
- world command handler
- response scene provider
- intro scene과 command response scene의 renderer path 공유
- prompt loop와 command handling의 책임 분리
- parser/handler 단위 테스트

### Prototype Flow

```text
사용자 SSH 접속
     ↓
intro scene 재생
     ↓
prompt 표시
     ↓
사용자 command 입력
     ↓
command parser가 작은 intent로 변환
     ↓
fake world state 조회 또는 변경
     ↓
response scene 선택
     ↓
cinematic renderer가 timed response 출력
     ↓
prompt 재표시
```

초기 world는 첫 intro의 전화 장면에서 자연스럽게 이어진다.

```text
Office is dark.
Telephone is ringing.
Line is unanswered.
```

예시 response:

```text
> LOOK

The office waits in green phosphor silence.

                  ☎

The telephone is still ringing.

> _
```

```text
> ANSWER

You lift the receiver.

     CLICK.

A carrier tone breathes on the line.

UNKNOWN CALLER:
  "You are late."

> _
```

### Key Components

- `ShellSessionLoop`
  - SSH shell input/output loop를 담당한다.
  - echo, backspace, enter, prompt timing, session 종료를 처리한다.
  - command text를 application boundary로 넘기며 world state를 직접 변경하지 않는다.
- `CommandParser`
  - 빈 입력, 대소문자, alias, known/unknown command를 deterministic하게 처리한다.
  - 자연어와 LLM interpretation은 이 milestone에 넣지 않는다.
- `WorldSession`
  - SSH 접속 하나에 대응하는 임시 in-memory world state다.
  - persistence domain model 이전의 prototype state 역할을 한다.
- `WorldCommandHandler`
  - command intent를 받아 world state를 변경하고 response scene을 반환한다.
  - SSH adapter와 renderer 사이의 application boundary 역할을 한다.
- `SceneResponseProvider`
  - command 결과에 해당하는 hardcoded response scene을 제공한다.
  - 상태에 따른 작은 variation을 제공한다.

### Suggested Work Items

1. `IntroSceneLine`을 일반 `SceneLine`으로 확장하거나 이름을 정리한다.
2. `CinematicTextRenderer`가 intro 전용이 아닌 scene 또는 line sequence를 렌더링하게 한다.
3. prompt 출력 책임을 renderer와 shell loop 사이에서 정리한다.
4. SSH input loop를 별도 session loop로 분리한다.
5. `CommandParser`를 추가한다.
6. `WorldSession`을 추가한다.
7. `WorldCommandHandler`를 추가한다.
8. `LOOK`, `HELP`, `QUIT` response scene을 구현한다.
9. `ANSWER` command가 world state를 바꾸게 한다.
10. `ANSWER` 전후 `LOOK` response가 달라지게 한다.
11. unknown command response scene을 구현한다.
12. parser와 command handler 단위 테스트를 추가한다.
13. renderer가 command response scene을 처리하는 최소 테스트를 추가한다.
14. 로컬 SSH 접속으로 writing, silence, pacing을 조정한다.

### Non-Goals

- full adventure parser
- natural language LLM intent interpretation
- persistent world
- multi-user world
- save/load
- general quest engine

### Success Criteria

- intro 이후 `LOOK`, `ANSWER`, `HELP`, `QUIT`를 사용할 수 있다.
- `ANSWER` 이후 world state와 `LOOK` 응답이 달라진다.
- command response가 단순 echo가 아니라 timing/style/spacing을 가진 scene이다.
- SSH adapter가 world state를 직접 조작하지 않는다.
- database, LLM, HTTP, GUI 없이 동작한다.
- 로컬 SSH 접속으로 2~5분 정도의 수동 체감 테스트가 가능하다.

## 3. Milestone 3: Renderer Core Hardening

### Goal

intro와 command response에 흩어진 prototype renderer를, 앞으로 AI-generated scene과 여러 presentation adapter가 사용할 수 있는 작은 renderer core로 정리한다.

이 milestone의 질문은 다음이다.

> renderer가 demo scene 전용 코드가 아니라 반복해서 장면을 만들 수 있는 core가 되었는가?

### Scope

- `Scene`, `SceneLine`, `SceneBlock`, `SceneStyle`, `RevealMode` 같은 prototype scene model 정리
- intro-only 이름 제거
- renderer input validation의 첫 형태
- semantic style과 terminal theme 분리
- Green CRT, Monochrome 또는 Amber CRT theme 중 최소 2개 지원
- timing profile의 첫 형태
  - cinematic
  - fast
  - instant 또는 test
- prompt 렌더링과 scene 렌더링 책임 정리
- blocking renderer를 유지하되 cancellation boundary 명확화
- renderer unit test 추가
- terminal output snapshot 또는 event-sequence test 추가

### Non-Goals

- final Scene IR schema 확정
- full animation engine
- terminal emulator별 완전 호환
- browser/GUI renderer

### Success Criteria

- 같은 scene을 다른 theme/timing profile로 재생할 수 있다.
- 테스트에서는 instant timing으로 빠르게 검증할 수 있다.
- renderer가 invalid scene을 그대로 terminal bytes로 내보내지 않는다.
- raw ANSI 문자열은 terminal helper/theme boundary 밖으로 새지 않는다.

## 4. Milestone 4: Terminal Layout and Text Art System

### Goal

텍스트를 단순 문자열 출력이 아니라 terminal canvas 위의 visual medium으로 다룰 수 있게 한다.

이 milestone의 질문은 다음이다.

> Unicode, CJK, emoji, box drawing, whitespace를 사용해 안정적인 terminal composition을 만들 수 있는가?

### Scope

- `TerminalCanvas` 또는 equivalent 2D text buffer
- terminal width/height 기반 배치
- left/center/right alignment
- simple regions or panels
- ANSI escape sequence와 visible width 분리
- CJK/emoji width 계산 전략 도입
  - library 도입 또는 compatibility layer
- grapheme cluster 처리의 첫 형태
- reusable text-art primitive의 첫 형태
  - telephone
  - office panel
  - alert/signal marker
- small animation/state-change primitive
- terminal resize 시 최소 degrade strategy

### Non-Goals

- 완전한 typography engine
- 모든 terminal emulator 완전 동일 출력
- AI-generated art ingestion
- GUI-level layout system

### Success Criteria

- 한글/CJK/emoji/box drawing이 섞여도 기본 alignment가 크게 깨지지 않는다.
- text art를 거대한 raw string만이 아니라 primitive로 재사용할 수 있다.
- terminal size가 작을 때 scene이 폭발하지 않고 단순화되거나 crop/degrade된다.

## 5. Milestone 5: Tiny World Runtime and Event History

### Goal

hardcoded session state를 넘어, 작은 world runtime이 action, event, evidence, history를 다룰 수 있게 한다.

이 milestone의 질문은 다음이다.

> command가 단순 화면 반응이 아니라 world에서 발생한 action과 event로 남는가?

### Scope

- game vocabulary보다 일반적인 prototype model 도입
  - `WorldSession`
  - `Actor`
  - `Action`
  - `WorldEvent`
  - `Evidence` 또는 `Observation`
- command intent를 world action으로 변환
- action 결과로 event 생성
- in-memory event history
- `REMEMBER`, `LOG`, `STATUS` 같은 history-oriented command 후보
- event history를 scene material로 projection
- session-local world와 application-wide fake world의 경계 검토
- world runtime 단위 테스트

### Non-Goals

- durable persistence
- complete event sourcing framework
- authorization/governance
- AI agent delegation
- enterprise workflow model

### Success Criteria

- 사용자의 주요 command가 event로 기록된다.
- world state는 event 또는 action 결과로 설명 가능하다.
- history를 cinematic하게 재생하거나 요약하는 작은 command가 가능하다.
- `Quest`, `Location`, `Character`, `Item`이 core model을 지배하지 않는다.

## 6. Milestone 6: Scene IR Validation and AI Trust Boundary

### Goal

AI가 나중에 scene intent를 만들 수 있도록, deterministic renderer가 받아들일 수 있는 제한된 Scene IR과 validation boundary를 만든다.

이 milestone의 질문은 다음이다.

> 외부 생성자가 만든 scene intent를 안전하게 검증하고 terminal output으로 바꿀 수 있는가?

### Scope

- prototype Scene IR schema 초안
- JSON/YAML scene definition loading
- scene validation
  - unknown style
  - invalid timing
  - too-wide layout
  - unsupported art primitive
  - excessive duration
- fallback strategy
  - simpler Unicode
  - ASCII fallback
  - instant timing for test/accessibility
- renderer error scene
- golden sample scenes
- validation test suite
- documentation for allowed scene constructs

### Non-Goals

- LLM provider integration
- final schema freeze
- external scene marketplace/editor
- arbitrary ANSI passthrough

### Success Criteria

- Scene IR 파일을 로드해 렌더링할 수 있다.
- invalid scene은 안전하게 거부되거나 fallback된다.
- raw ANSI는 Scene IR에서 직접 허용하지 않는다.
- AI 연결 전에도 trust boundary가 코드로 표현된다.

## 7. Milestone 7: Human-Made Scene and Text-Art Library

### Goal

좋은 terminal world는 renderer만으로 생기지 않는다. 사람이 만든 reusable scene, text-art, timing, dialogue asset을 library로 축적한다.

이 milestone의 질문은 다음이다.

> 이 world만의 visual vocabulary가 반복 가능한 asset으로 쌓이기 시작했는가?

### Scope

- scene asset directory structure
- text-art primitive metadata
  - id
  - tags
  - minimum width
  - preferred width
  - states
  - semantic meaning
- reusable scene templates
- office, terminal, phone, queue, incident, document 같은 first asset set
- style/timing presets
- asset validation command or tests
- command responses를 asset 기반으로 일부 이전
- visual/manual review checklist

### Non-Goals

- AI-generated asset promotion workflow
- GUI scene editor
- huge content pack
- localization framework

### Success Criteria

- hardcoded Kotlin scene만이 아니라 asset files/templates에서 scene을 구성할 수 있다.
- 적어도 몇 개의 text-art primitive가 여러 scene에서 재사용된다.
- content change가 renderer code change를 요구하지 않는다.

## 8. Milestone 8: AI Director Prototype

### Goal

AI가 raw terminal을 제어하지 않고, world state와 asset library를 바탕으로 Scene IR 또는 scene intent를 생성하게 한다.

이 milestone의 질문은 다음이다.

> AI가 창의적 연출에 참여하되, renderer의 deterministic control을 침범하지 않는가?

### Scope

- AI Director port
- fake director와 real provider adapter 분리
- provider configuration via environment
- one LLM provider integration behind adapter
- world state summary input
- asset library reference input
- structured scene intent output
- validation before render
- recovery path on invalid AI output
- deterministic fallback scene
- prompt templates or instruction files
- AI-generated scene quality review notes

### Non-Goals

- autonomous agents
- tool-using business agents
- provider-specific domain leakage
- AI modifying world state directly
- AI emitting raw ANSI

### Success Criteria

- AI가 최소 하나의 response scene 또는 variant를 생성한다.
- renderer validation을 통과한 scene만 출력된다.
- invalid AI output이 session을 깨뜨리지 않는다.
- AI를 끄고도 fake/static director로 시스템이 작동한다.

### Current Implementation Notes

- `AI ...` command opens the first AI Director channel from the SSH world loop.
- The default director is deterministic/static, so local tests and demo deploys do not require a live provider.
- AI Director output is expressed as `SceneDefinition` and must pass `SceneDefinitionValidator` before rendering.
- Provider/runtime settings are prepared through `.env.local`, including `SPRING_IS_COOL_AI_*`, `GOOGLE_CLOUD_*`, `VERTEX_AI_MODEL_ID`, and `GOOGLE_APPLICATION_CREDENTIALS`.
- `aws-demo` and `dev-demo` mount `gcp-key.json` outside the Docker image and pass runtime settings through `--env-file`.

## 9. Milestone 9: Durable World Persistence

### Goal

세션이 끝나도 world history와 core state가 남도록 persistence boundary를 도입한다.

이 milestone의 질문은 다음이다.

> 이 world가 접속 중인 shell session을 넘어 지속되는가?

### Scope

- persistence port 정의
  - event repository
  - world snapshot repository 후보
- PostgreSQL adapter 또는 simple file-backed adapter 중 선택
- local development without mandatory DB 유지 방안
- migration strategy
- app container와 database container lifecycle 분리
- seed/dev data strategy
- history replay command
- session identity의 첫 형태
- persistence integration tests

### Non-Goals

- full account system
- enterprise multi-tenancy
- RDF store as primary persistence
- distributed event bus
- ledger persistence

### Success Criteria

- event history가 process restart 후에도 남는다.
- DB가 없어도 fake/in-memory adapter로 tests가 가능하다.
- persistence implementation이 world core에 새지 않는다.
- deployment script가 app redeploy 시 DB를 재생성하지 않는다.

### Current Implementation Direction

- Use a swappable `WorldEventStore` port so PostgreSQL can replace SQLite later.
- Start with a SQLite adapter, not a plain file log, because the future PostgreSQL shape should stay natural.
- Keep each instance's DB file outside the image under its runtime directory:
  - `dev-demo`: `/home/hchjeong/spring-is-cool/data/world.sqlite`
  - `aws-demo`: `/home/ubuntu/spring-is-cool/data/world.sqlite`
- Prepare DB files through target-specific init scripts before first deployment.
- App deploy scripts mount `data/` into the container and must not delete or recreate the DB.

## 10. Milestone 10: Presentation Adapter Expansion

### Goal

SSH가 첫 presentation일 뿐 core architecture가 SSH에 고정되지 않았음을 증명한다.

이 milestone의 질문은 다음이다.

> 같은 world behavior와 scene projection을 다른 interface에서도 사용할 수 있는가?

### Scope

- HTTP read/stream adapter 후보
- CLI adapter 후보
- simple local TUI adapter 후보 중 하나 선택
- application command boundary 정리
- scene output을 terminal events 또는 render events로 분리
- adapter-neutral command result model
- SSH와 신규 adapter가 같은 command handler 사용
- curl로 가능한 minimal backend product path

### Non-Goals

- full web GUI
- mobile client
- browser animation parity
- complete API product

### Success Criteria

- SSH 밖의 한 adapter에서 같은 command를 실행할 수 있다.
- core world behavior는 presentation adapter를 모른다.
- scene/render output은 adapter가 선택한 표현으로 변환 가능하다.

### Current Implementation Notes

- Added a small presentation boundary instead of a full UI or HTTP surface.
- `ScenePresenter` projects renderer `Scene` objects into adapter-neutral `PresentedScene` DTOs.
- `WorldInteractionService` returns both the renderer scene for SSH and the projected scene for future HTTP/TUI adapters.
- SSH now submits commands through the same interaction service, keeping the next adapter path open without changing world behavior.

## 11. Milestone 11: Agent, Delegation, Governance, and Evidence Prototype

### Goal

Human, AI agent, service, device가 같은 world 안에서 action 주체가 될 수 있는 방향을 작은 prototype으로 검증한다.

이 milestone의 질문은 다음이다.

> 누가, 누구를 대신해, 어떤 권한으로, 무엇을 했는지가 world history와 scene에 드러나는가?

### Scope

- `Actor`, `Principal`, `Agent` vocabulary 후보 검증
- delegation model prototype
- capability/policy check의 첫 형태
- action authorization result
- evidence attachment
- AI 또는 fake agent에게 작은 task 위임
- agent result를 world event로 기록
- `ASK`, `ASSIGN`, `REVIEW` command 후보
- governance failure를 cinematic하게 표현

### Non-Goals

- enterprise-grade policy engine
- real autonomous coding agent integration
- MCP dependency
- full workflow engine

### Success Criteria

- human action과 delegated/fake-agent action이 같은 history 안에 기록된다.
- denied action은 UI 숨김이 아니라 world authority의 결과로 표현된다.
- evidence가 later scene/history material로 사용된다.

### Current Implementation Notes

- `ASSIGN clerk check line` is the first delegation command.
- The prototype uses a deterministic fake agent named `clerk`; no autonomous agent runtime is introduced.
- Allowed delegation records operator assignment, clerk report, and evidence attachment as world events.
- Unsupported delegation records a denied authority result rather than silently hiding the action.
- `WorldEvent` now carries small nullable metadata for `targetActorId`, `authorityResult`, and `evidenceId`.
- SQLite persistence migrates the existing `world_events` table with nullable metadata columns, keeping PostgreSQL replacement feasible later.
- `LOG` projects actor, authority, target, and evidence metadata as scene material.

## 12. Milestone 12: Real Gemini Provider Integration

### Goal

static/fake AI Director를 실제 Gemini provider adapter로 교체할 수 있음을 검증한다.

이 milestone의 질문은 다음이다.

> 실제 LLM provider를 붙여도 scene trust boundary와 world authority가 유지되는가?

### Scope

- Gemini provider adapter 구현
- `VERTEX_AI_MODEL_ID` 기반 model 선택
  - 현재 smoke 대상 모델: `gemini-2.5-flash-lite`
- 기존 `AiDirector` port 뒤에 real provider 연결
- `GOOGLE_APPLICATION_CREDENTIALS`, `GOOGLE_CLOUD_PROJECT`, `GOOGLE_CLOUD_LOCATION` runtime 설정 사용
- provider timeout/retry/failure handling
- provider output을 `SceneDefinition` 또는 scene intent로만 수용
- `SceneDefinitionValidator` 통과 후 renderer 출력
- invalid provider output에 대한 deterministic fallback
- `aws-demo`와 `dev-demo` runtime env에서 실제 provider smoke test
- provider cost/latency/quality review notes

### Non-Goals

- autonomous agents
- provider가 world state를 직접 변경하는 구조
- provider가 raw ANSI 또는 terminal control을 생성하는 구조
- multi-provider routing
- production-grade LLM observability

### Success Criteria

- `spring-is-cool.ai.provider=gemini` 같은 설정으로 real provider adapter를 선택할 수 있다.
- 실제 Gemini 응답이 최소 하나의 response scene을 생성한다.
- generated scene은 validation을 통과한 뒤에만 출력된다.
- provider 장애, timeout, invalid JSON이 session을 깨뜨리지 않는다.
- provider를 끄면 static director로 다시 동작한다.

### Current Implementation Notes

- `GeminiAiDirector` is available behind the existing `AiDirector` port when `spring-is-cool.ai.provider=gemini`.
- `VertexGeminiTextClient` calls Vertex AI `generateContent` using the configured project, location, and model id.
- `ServiceAccountAccessTokenProvider` mints a short-lived OAuth token from `GOOGLE_APPLICATION_CREDENTIALS`.
- Provider text must parse as `SceneDefinition` JSON and pass existing validation before rendering.
- Tests use a fake Gemini client; real `dev-demo`/`aws-demo` smoke tests require target env changes and GCP access.

## 13. Milestone 13: MVP Semantic World And Demo Closure

### Goal

MVP가 단순 command demo가 아니라 event-backed semantic world처럼 느껴지도록 닫는다.

이 milestone은 OWL/RDF, ledger, external adapters를 본격 구현하는 단계가 아니다. 그 대신
나중에 그 층들이 붙을 수 있도록 현재 world event log에서 read-only semantic projection을
만든다.

이 milestone의 질문은 다음이다.

> 사용자가 명령을 입력할 때마다 같은 event log에서 계산된 현재 world state가 일관되게 보이는가?

### Scope

- `WorldProjection` read model 추가
  - source of truth는 `WorldEvent` log
  - projection은 현재 전화, line, instruction, delegation, evidence 상태를 계산
- incoming call flow 의미 완성
  - `ANSWER` 후 caller instruction 수신
  - caller goodbye 후 `OFFLINE` event 기록
  - `LOOK`, `STATUS`, `LOG`에서 offline/instruction 상태 확인
- AI world summary에 semantic facts 제공
  - Gemini/static director는 projected facts를 참고
  - AI는 world state를 직접 변경하지 않음
- MVP demo script 정리
  - `HELP`
  - `ANSWER`
  - `LOOK`
  - `STATUS`
  - `AI ...`
  - `ASSIGN clerk check line`
  - `LOG`
- implementation plan에 future semantic/ledger/external adapter 방향을 후속 단계로 분리

### Non-Goals

- complete ontology
- complete accounting system
- production factory control
- production personal finance product
- mandatory MCP/RDF/MQTT dependency
- outgoing user-to-user calls
- multi-user presence
- AI-driven mutation

### Success Criteria

- `ANSWER`는 caller message를 남긴 뒤 line을 offline으로 만든다.
- `LOOK`은 offline line과 남은 instruction을 보여준다.
- `STATUS`는 projected telephone/line/instruction/evidence state를 보여준다.
- `LOG`는 `ANSWER`, `OFFLINE`, delegation, evidence events를 설명 가능한 순서로 보여준다.
- `AI ...`는 projected semantic facts를 context로 받지만 world를 직접 변경하지 않는다.
- SQLite restore 후에도 projection이 같은 world state를 계산한다.

### Current Implementation Notes

- `WorldProjection` is the MVP semantic read model.
- `WorldEvent` remains the source of truth.
- The current projection covers telephone state, line state, active instruction, latest delegation, latest evidence, and event count.
- `LOOK`, `STATUS`, static AI, and Gemini AI now consume projected facts.
- The first incoming call now resolves into an `OFFLINE` event instead of leaving an ambiguous open line.

### Deferred Follow-Up

- OWL/RDF-compatible export of projected facts.
- Ledger/value/resource model.
- Simulated factory/device adapter.
- Outgoing calls to agents or users.
- Multi-user session presence.
- external technology는 adapter로 유지된다.

## 14. Cross-Cutting Rules

### Architecture

- Spring Boot/Kotlin backend가 제품의 중심이다.
- core behavior는 SSH, HTTP, GUI, MCP 같은 adapter를 몰라야 한다.
- DB, LLM, MQTT, RDF, MCP는 모두 optional adapter 후보로 둔다.
- game vocabulary는 presentation vocabulary로 사용할 수 있지만 core model을 지배하지 않는다.
- raw ANSI는 renderer/terminal abstraction 밖으로 흩어지지 않는다.

### Experience

- milestone마다 기술 checklist와 별도로 체감 질문을 둔다.
- 재미와 분위기가 약하면 platform feature를 늘리기 전에 writing, timing, spacing, layout, silence를 조정한다.
- text is visual medium이라는 원칙을 코드 구조에 반영한다.
- time is part of rendering이라는 원칙을 scene/timing model에 반영한다.

### Deployment

- deployment는 manual only다.
- Docker images는 local clean clone에서만 build한다.
- image tarball을 target host로 전송하고 target에서 load/run한다.
- `dev-demo`와 `aws-demo` scripts, paths, env files, credentials는 분리한다.
- env/secrets는 Docker image에 포함하지 않는다.
- app deployment script는 future database lifecycle을 건드리지 않는다.

### Testing

- ordinary backend correctness만으로 성공을 판단하지 않는다.
- renderer event/timeline/style tests를 둔다.
- Unicode/layout tests를 점진적으로 강화한다.
- Scene IR validation tests를 둔다.
- SSH/manual visual test를 milestone acceptance에 포함한다.
- AI-generated scene은 correctness뿐 아니라 readability, coherence, atmosphere, pacing, appropriateness를 검토한다.

## 15. Order Summary

```text
1. SSH Cinematic Intro Prototype
2. First Interactive World Loop
3. Renderer Core Hardening
4. Terminal Layout and Text Art System
5. Tiny World Runtime and Event History
6. Scene IR Validation and AI Trust Boundary
7. Human-Made Scene and Text-Art Library
8. AI Director Prototype
9. Durable World Persistence
10. Presentation Adapter Expansion
11. Agent, Delegation, Governance, and Evidence Prototype
12. Real Gemini Provider Integration
13. Semantic World, Ledger, and External System Pilots
```

이 순서는 고정된 연대기가 아니라 dependency와 risk의 우선순위다. 특히 3~5번에서 terminal experience가 약하다고 판단되면, persistence나 AI로 넘어가기 전에 renderer, writing, timing, layout을 먼저 다듬는다.
