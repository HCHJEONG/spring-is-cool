# Ontoloffice

```text
┌────────────────────────────────────────────────────────────┐
│  ONTOLOFFICE MAINFRAME OPERATIONS ROOM                     │
│                                                            │
│  [ SSH TERMINAL ]  ->  [ CINEMATIC RENDERER ]              │
│          │                    │                            │
│          v                    v                            │
│  [ WORLD COMMANDS ] -> [ EVENT LOG / SQLITE ]              │
│          │                    │                            │
│          v                    v                            │
│  [ AI DIRECTOR ]  ->  [ VALIDATED SCENE DEFINITION ]       │
└────────────────────────────────────────────────────────────┘
```

`spring-is-cool`은 일반적인 CRUD 백엔드가 아니라, **백엔드 자체를 하나의 제품 화면으로 만드는 실험**입니다.

첫 번째 제품 표면은 웹 GUI가 아니라 SSH 터미널입니다. 사용자는 Spring Boot 서버에 SSH로 접속하고, 서버가 직접 제어하는 텍스트 시네마틱 장면과 작은 텍스트 어드벤처 세계를 경험합니다.

현재 게임의 잠정 이름은 **Ontoloffice**입니다.

## 핵심 아이디어

이 프로젝트의 질문은 단순합니다.

> 백엔드 서버에 SSH로 접속했을 뿐인데, 사용자가 그 안에 더 머무르고 싶어질 수 있을까?

그래서 이 프로젝트는 다음 원칙을 따릅니다.

- Backend is the product.
- Text adventure is the interaction model.
- AI may create intent, but deterministic renderer controls execution.
- Database, LLM, HTTP, SSH, future PostgreSQL, future OpenAI/Gemini providers are adapters.
- 세계 상태와 장면 렌더링은 분리한다.

즉, AI가 문장을 직접 터미널에 마구 쓰는 구조가 아닙니다. AI는 제한된 `SceneDefinition` 의도를 만들고, 서버의 deterministic renderer가 검증된 장면만 출력합니다.

## 현재 구현 상태

현재 MVP 범위에서는 다음을 구현했습니다.

- Spring Boot 4 + Kotlin + Java 21 기반 애플리케이션
- Apache MINA SSHD 기반 embedded SSH server
- SSH 접속 시 cinematic intro scene 재생
- `LOOK`, `STATUS`, `ANSWER`, `HELP`, `LOG`, `AI ...`, `ASSIGN AI clerk check line`, `QUIT` 명령
- HTTP JSON command adapter
- SQLite 기반 event persistence
- PostgreSQL 교체를 염두에 둔 persistence adapter 경계
- Gemini Vertex AI adapter
- provider-neutral AI director 추상화
- provider-neutral monthly call limiter
- AI clerk presence / authority projection
- SSH 접속자 presence tracking
- manual Docker deployment scripts for `dev-demo` and `aws-demo`

## 데모 포인트

면접관에게 보여주기 좋은 흐름은 다음입니다.

1. SSH로 서버에 접속한다.
2. 서버가 직접 렌더링하는 intro scene을 보여준다.
3. `LOOK`으로 현재 사무실 세계를 확인한다.
4. `STATUS`로 사용자, AI clerk, 권한, 전화 상태, 이벤트 수를 확인한다.
5. `ANSWER`로 걸려온 전화를 받고, line이 offline으로 바뀌는 것을 보여준다.
6. `ASSIGN AI clerk check line`으로 AI clerk에게 제한된 작업을 위임한다.
7. `AI describe the office in one strange sentence`처럼 자연어 장면 생성을 요청한다.
8. `LOG`로 event sourcing에 가까운 기록 흐름을 확인한다.
9. 다른 SSH 세션을 하나 더 열고 `STATUS`에서 active user count가 증가하는 것을 보여준다.
10. `QUIT`으로 terminal world를 떠나는 장면을 확인한다.

게임 안의 사용자-facing command는 영어로 유지합니다.

```text
HELP
LOOK
STATUS
ANSWER
ASSIGN AI clerk check line
AI describe the office
LOG
QUIT
```

## 아키텍처 개요

```text
SSH adapter
  └─ WelcomeShellFactory
       └─ WorldInteractionService
            ├─ CommandParser
            ├─ WorldCommandHandler
            ├─ WorldProjection
            ├─ WorldEventStore
            └─ AiDirector

HTTP adapter
  └─ WorldCommandController
       └─ WorldInteractionService
```

중요한 경계는 다음과 같습니다.

- `adapter/ssh`: SSH 접속, shell lifecycle, terminal input/output
- `cinematic/renderer`: ANSI, timing, text art, scene rendering
- `cinematic/scene`: AI 또는 static source가 반환할 수 있는 scene definition
- `world`: command parsing, world state, projection, actor authority
- `persistence`: event storage adapter
- `ai`: provider-neutral AI director와 Gemini adapter
- `presentation`: HTTP JSON presentation adapter

## AI 설계

AI는 현재 Gemini Vertex AI를 사용할 수 있지만, core world는 Gemini에 묶이지 않도록 구성했습니다.

```text
AiDirector
  ├─ StaticAiDirector
  └─ GeminiAiDirector

AiProviderCallLimiter
  └─ SQLite-backed monthly usage store
```

현재 정책은 다음과 같습니다.

- 기본 로컬 설정에서는 AI provider가 꺼져 있습니다.
- Gemini 사용 시 service account key는 Docker image에 포함하지 않습니다.
- provider call은 월 250회 제한을 둡니다.
- 사용량은 SQLite의 `ai_provider_usage` 테이블에 기록됩니다.
- 나중에 OpenAI provider를 추가해도 `AiDirector` 경계를 유지할 수 있도록 provider-neutral naming을 사용합니다.

## Persistence

현재 persistence는 SQLite입니다.

SQLite를 선택한 이유는 MVP에서는 운영 복잡도를 낮추되, 나중에 PostgreSQL로 갈아타기 위한 adapter 경계를 먼저 검증하기 위해서입니다.

```text
WorldEventStore
  └─ SQLiteWorldEventStore

AiProviderUsageStore
  └─ SQLiteAiProviderUsageStore
```

새 배포 환경에서는 runtime directory 아래에 instance별 SQLite 파일이 생성됩니다.

예시:

```text
/home/hchjeong/spring-is-cool/data/world.sqlite
/home/ubuntu/spring-is-cool/data/world.sqlite
```

DB 파일, env 파일, GCP key, SSH host key는 repository와 Docker image에 포함하지 않습니다.

## 로컬 실행

테스트:

```bash
./gradlew test
```

로컬 실행:

```bash
./gradlew bootRun
```

기본 SSH 접속:

```bash
ssh demo@127.0.0.1 -p 2222
```

기본 demo password는 local config 기준 `demo`입니다.

HTTP command adapter:

```bash
curl -X POST http://127.0.0.1:8080/world/commands \
  -H 'Content-Type: application/json' \
  -d '{"command":"look"}'
```

## Demo Deployment

배포는 의도적으로 수동 shell script 기반입니다.

Docker image는 현재 working tree에서 직접 빌드하지 않고, 별도 clean clone에서 빌드한 뒤 tar로 export하고 target host로 전송합니다.

자세한 내용은 [.fordeploy/README.md](.fordeploy/README.md)를 참고합니다.

Dev demo:

```bash
.fordeploy/deploy-dev-demo.sh
ssh demo@192.168.0.104 -p 2222
```

AWS demo:

```bash
.fordeploy/deploy-aws-demo.sh
ssh -J aws-bastion demo@172.31.76.194 -p 2222
```

AWS HTTP 접근은 private instance 특성상 bastion tunnel 방식을 권장합니다.

```bash
ssh -N -L 18080:172.31.76.194:8080 aws-bastion
```

다른 터미널에서:

```bash
curl -X POST http://127.0.0.1:18080/world/commands \
  -H 'Content-Type: application/json' \
  -d '{"command":"look"}'
```

## 면접에서 설명할 수 있는 포인트

이 프로젝트는 기능 수보다 구조적 의도를 보여주는 포트폴리오입니다.

- Spring Boot를 웹 API 서버로만 쓰지 않고, SSH 기반 product surface로 확장했습니다.
- terminal rendering을 단순 println이 아니라 timing, ANSI style, text art, prompt lifecycle이 있는 presentation layer로 다뤘습니다.
- AI output은 직접 실행하지 않고, validated scene definition으로 제한했습니다.
- Gemini는 adapter이며, core는 OpenAI 등 다른 provider로 교체 가능하게 유지했습니다.
- SQLite를 쓰지만 persistence port를 둬서 PostgreSQL 전환 여지를 남겼습니다.
- SSH와 HTTP가 같은 world interaction service를 공유합니다.
- DB, key, env, host key 같은 runtime 파일은 image/repository 밖에 둡니다.
- AWS demo는 private instance + bastion tunnel 기준으로 보수적으로 운영합니다.

## Roadmap

현재 MVP 이후의 자연스러운 다음 단계는 다음입니다.

- OpenAI provider adapter 추가
- Gemini/OpenAI 동시 선택 가능한 provider registry
- PostgreSQL adapter 추가
- scene definition schema 고도화
- AI clerk task 종류 확장
- outgoing call 기능
- multi-user session semantics 강화
- ontology/semantic model 도입
- terminal UI adapter 또는 web/TUI adapter 추가

## Project Documents

- [docs/implementation-plan.md](docs/implementation-plan.md): 현재 구현 계획의 source of truth
- [docs/implementation-plan-original.md](docs/implementation-plan-original.md): 초기 원칙과 방향성 보존 문서
- [.fordeploy/README.md](.fordeploy/README.md): 수동 배포 runbook

