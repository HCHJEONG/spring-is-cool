# First Milestone Implementation Plan

이 문서는 `implementation-plan-original.md`의 최상위 원칙을 바탕으로, 실제 Spring Boot 프로젝트의 첫 번째 milestone을 최소 구현 범위로 정리한 실행 계획이다.

첫 번째 milestone의 목적은 플랫폼을 완성하는 것이 아니다. **Spring Boot 서버에 SSH로 접속했을 때 cinematic terminal intro가 우리가 원하는 느낌으로 나오는지 검증하는 것**이다.

## 1. 프로젝트 목표

첫 번째 제품 목표는 다음 한 문장으로 정의한다.

> 사용자가 SSH로 Spring Boot 서버에 접속하면, 서버가 직접 제어하는 cinematic Unicode/text intro scene이 재생되고, 사용자가 이 터미널 세계에 계속 머물고 싶다는 감각을 얻는다.

이 milestone에서 증명하려는 것은 기능의 양이 아니라 경험의 밀도다. 특히 다음을 확인한다.

- Spring 서버 자체가 실행 가능한 제품 표면이 될 수 있는가.
- GUI나 별도 TUI 없이 SSH 접속만으로 첫인상이 성립하는가.
- Unicode, 공백, 위치, 색, 타이밍, pause, cursor가 장면을 만들 수 있는가.
- AI가 나중에 생성할 수 있는 Scene을 deterministic renderer가 통제하는 구조가 타당한가.
- 30~60초 정도의 아주 작은 intro만으로도 제품의 정체성이 전달되는가.

## 2. 비목표

첫 milestone에서는 의도적으로 다음을 만들지 않는다.

- 완전한 text adventure game loop
- Quest, Location, Character, Item 등의 영구 도메인 모델
- OWL ontology
- RDB schema 및 영속 저장소
- MQTT, OPC-UA, MCP, 외부 agent 연동
- LLM provider 연동
- 복잡한 workflow, governance, ledger
- GUI, 모바일 앱, 웹 프론트엔드
- 범용 scene editor
- 완성된 Scene IR schema
- production-grade SSH 계정/권한/보안 모델

이 항목들은 버리는 것이 아니라, 첫 experience proof 이후로 미룬다. 첫 번째 검증 전에 플랫폼이 커지면 핵심 질문인 "이 터미널 세계에 머물고 싶은가?"가 흐려진다.

## 3. 첫 번째 프로토타입 범위

첫 번째 프로토타입은 단 하나의 흐름만 제공한다.

```text
사용자 SSH 접속
     ↓
세션 생성
     ↓
intro scene 선택
     ↓
서버측 cinematic renderer 실행
     ↓
terminal에 timed Unicode/text output 전송
     ↓
scene 종료 후 prompt 표시
```

초기 intro scene은 hardcoded data로 작성한다. 예시는 다음과 같은 분위기를 기준으로 삼는다.

```text
An empty office.


                  ☎


               RING...


                  ☎


            RING... RING...


Someone is calling.


> _
```

최소 구현에 포함할 기능은 다음과 같다.

- Spring Boot 애플리케이션 실행
- SSH 서버 endpoint 제공
- SSH 접속 시 intro scene 자동 재생
- 줄 단위, 문장 단위, 문자 단위 출력 timing
- pause 및 delay
- ANSI color/style 출력
- Unicode 및 CJK 폭을 고려한 기초 layout
- 장면 종료 후 입력 prompt 표시
- 사용자가 `Ctrl+C` 또는 접속 종료를 했을 때 세션 정리

가능하면 포함하지만 필수는 아닌 기능은 다음과 같다.

- terminal size 감지
- 간단한 clear screen
- cursor hide/show
- typewriter effect
- 사용자의 Enter 입력으로 일부 pause skip

## 4. 핵심 컴포넌트 후보

첫 milestone의 컴포넌트는 작고 명확하게 유지한다.

### SshSessionAdapter

SSH 접속을 받아 내부 application flow로 넘기는 adapter다.

역할:

- SSH server 시작
- 접속 세션 생성 및 종료 감지
- terminal input/output stream 제공
- terminal size 등 환경 정보 수집

이 컴포넌트는 presentation adapter이며 core experience에 필요한 I/O 경계만 담당한다.

### IntroSceneProvider

첫 intro scene을 제공하는 임시 provider다.

역할:

- hardcoded intro scene 반환
- 나중의 AI Director 또는 scene generation을 대체하는 fake component 역할

첫 milestone에서는 AI를 연결하지 않는다. 대신 AI가 만들 법한 Scene을 사람이 직접 작성해 renderer 경계를 검증한다.

### SceneModel

아직 최종 Scene IR이 아니다. renderer를 실험하기 위한 최소 scene data structure다.

포함 후보:

- text block
- x/y position 또는 alignment
- style
- delay before/after
- reveal mode
- clear screen 여부

이 구조는 prototype용이며, 이후 실제 Scene IR로 발전하거나 폐기될 수 있다.

### CinematicTextRenderer

첫 milestone의 중심 컴포넌트다.

역할:

- SceneModel을 terminal output으로 변환
- timing 제어
- ANSI escape sequence 생성
- Unicode/text layout 처리
- cursor, clear, color, reveal 같은 terminal 표현 제어

이 컴포넌트가 첫 번째 차별화 검증 대상이다.

### TerminalCanvas

텍스트를 2D 화면처럼 다루기 위한 내부 abstraction이다.

역할:

- terminal width/height 기준 좌표 계산
- 문자열 배치
- CJK, emoji, box drawing 등 폭 문제를 점진적으로 처리
- 최종적으로 terminal에 쓸 frame 또는 line sequence 생성

첫 구현에서는 단순해도 된다. 다만 "텍스트는 visual medium"이라는 원칙을 반영하기 위해 문자열 append만 하는 구조로 고정하지 않는다.

### PromptLoop

intro 이후 사용자에게 `> _` prompt를 보여주는 최소 loop다.

역할:

- scene 종료 후 prompt 표시
- 한 줄 입력 수신
- 아직 명령 해석은 하지 않거나, `look`, `quit` 정도만 처리

첫 milestone에서 prompt는 게임 시스템이 아니라 "세계가 계속될 수 있다"는 감각을 주는 장치다.

## 5. 기술 선택 기준

기술 선택은 다음 기준을 따른다.

- Spring Boot 애플리케이션 안에서 실행될 것
- SSH 접속이 제품의 primary interface로 동작할 것
- renderer가 raw terminal output을 deterministic하게 통제할 것
- AI, DB, ontology, external system 없이도 실행될 것
- 나중에 HTTP streaming, CLI/TUI, GUI adapter를 붙일 수 있도록 core rendering logic이 SSH에 강하게 묶이지 않을 것
- prototype이 빠르게 움직일 수 있도록 library 선택은 단순할 것

검토할 주요 선택지는 다음과 같다.

### SSH

Apache MINA SSHD를 우선 후보로 둔다.

이유:

- Java/Spring Boot 애플리케이션에 embedded SSH server로 넣기 쉽다.
- shell/session abstraction을 제공한다.
- 첫 milestone의 "backend is the product" 검증에 적합하다.

### Terminal Output

ANSI escape sequence를 직접 생성하되, 작은 helper layer를 둔다.

이유:

- 첫 milestone에서는 renderer의 제어권을 명확히 보는 것이 중요하다.
- 너무 큰 TUI framework를 도입하면 text adventure/cinematic renderer의 핵심 판단이 framework에 가려질 수 있다.

### Unicode Width

초기에는 단순 구현으로 시작하되, CJK/emoji 폭 계산을 위한 library 도입 가능성을 열어둔다.

후보:

- `com.ibm.icu:icu4j`
- Java 기반 wcwidth 구현체

첫 milestone에서 완전한 Unicode layout engine을 만들지는 않는다. 대신 한글/CJK와 특수문자가 깨지지 않고, 향후 확장 지점이 보이도록 한다.

### Timing

Java/Spring의 scheduler 또는 coroutine-style loop 없이 단순 blocking delay로 시작해도 된다.

이유:

- 첫 milestone은 concurrent world simulation이 아니라 단일 intro scene 검증이다.
- 단, 세션 종료와 interruption은 처리해야 한다.

## 6. 첫 milestone 성공 기준

첫 milestone은 다음 조건을 만족하면 성공으로 본다.

- Spring Boot 앱을 실행할 수 있다.
- 사용자가 SSH로 로컬 서버에 접속할 수 있다.
- 접속 직후 intro scene이 자동으로 재생된다.
- scene은 단순 print가 아니라 timing, spacing, Unicode, color 중 최소 세 가지 이상을 사용한다.
- intro는 30~60초 안에 끝난다.
- intro 종료 후 `> _` prompt가 표시된다.
- 사용자가 접속을 끊거나 중단해도 서버가 죽지 않는다.
- 코드 구조상 renderer core와 SSH adapter가 분리되어 있다.
- 첫 실행 후 "더 많은 기능이 있다"보다 "이 분위기가 계속되면 좋겠다"를 판단할 수 있다.

성공 판단은 기술 checklist만으로 하지 않는다. 실제로 SSH 접속 화면을 보고 다음 질문에 답한다.

> 이 터미널 세계에 계속 머물고 싶은가?

이 질문에 "아직 그렇지는 않다"라고 답하게 되면, 다음 milestone로 기능을 늘리기 전에 renderer, timing, layout, writing, silence, prompt 감각을 먼저 개선한다.

## Suggested First Work Items

1. Spring Boot skeleton 생성
2. Apache MINA SSHD 기반 embedded SSH server 추가
3. SSH 접속 시 shell session 열기
4. hardcoded intro SceneModel 작성
5. CinematicTextRenderer 최소 구현
6. ANSI color, delay, clear screen, cursor 제어 helper 작성
7. intro 종료 후 prompt loop 연결
8. 로컬 SSH 접속으로 직접 체감 테스트

첫 번째 구현은 작게 끝나야 한다. 여기서 얻고 싶은 것은 완성도가 아니라 방향 감각이다.
