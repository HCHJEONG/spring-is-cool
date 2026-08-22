# Codex 작업지시서 — Spring SSH 3D ASCII/Unicode Interactive World Runtime

## 1. 프로젝트 목표

Spring 기반 백엔드 서버에 사용자가 SSH로 접속하면 일반적인 line-oriented CLI가 아니라, **서버가 직접 상태·입력·렌더링을 관리하는 interactive 3D ASCII/Unicode World**에 들어가도록 구현한다.

이 프로젝트에서 SSH Terminal은 임시 디버깅 UI가 아니라 **정식 presentation adapter/client**다. 동시에 향후 Web UI, AI Agent, 다른 client를 추가할 수 있도록 World/Application Core와 Terminal 구현을 강하게 분리한다.

최종적으로 다음 경험을 구현한다.

```text
$ ssh server

┌──────────────────────────────────────────────────────────────┐
│                    3D WORLD VIEWPORT                         │
│                                                              │
│                       ╱████████╲                             │
│              ╱█████╲ │ ▫ ▫ ▫ ▫│                             │
│             ╱ ▫ ▫ ▫ ╲│ ▫ ▫ ▫ ▫│       ╱████╲                │
│            │ ▫ ▫ ▫ ▫ │ ▫ ▫ ▫ ▫│      │ ▫ ▫ │                │
│────────────┴─────────┴─────────┴──────┴─────┴──────────────│
│                                                              │
├──────────────────────────────────────────────────────────────┤
│ System: You entered the office district.                     │
│ Alice: The deployment finished five minutes ago.             │
│                                                              │
│ > inspect building_                                          │
└──────────────────────────────────────────────────────────────┘
```

상단은 고정된 3D World Viewport, 하단은 독립적으로 scroll되는 Console/Prompt다.

사용자는 prompt를 작성하는 도중에도 키보드 또는 마우스로 World 안을 이동하거나 시점을 변경할 수 있어야 한다.

`quit` 시에는 마지막 World 화면을 지우지 않고 terminal의 특수 상태만 해제하여 정상 terminal/shell 상태로 자연스럽게 복귀시킨다.

---

## 2. 최상위 아키텍처 원칙

### 2.1 World/Application Core는 SSH를 몰라야 한다

다음 의존 방향을 유지한다.

```text
                       ┌──────────────────┐
                       │    DOMAIN CORE   │
                       │                  │
                       │ World / Actor    │
                       │ Action / Object  │
                       │ Event / Scene    │
                       └────────┬─────────┘
                                │
                       Application Ports
                                │
            ┌───────────────────┼───────────────────┐
            │                   │                   │
            ▼                   ▼                   ▼
       SSH Adapter         Web Adapter         Agent Adapter
       (현재 구현)          (향후 구현)          (향후 구현)
            │                   │                   │
        PTY / ANSI         HTTP / WS          REST/MCP/etc.
```

**SSH/Terminal을 Core로 만들지 말 것.**

TerminalSession, ANSI, PTY, mouse escape sequence 등이 World/Application package로 침투해서는 안 된다.

### 2.2 Hexagonal / Ports-and-Adapters 성격을 유지한다

입력 측 application port는 개념적으로 다음 성격을 갖는다.

```text
Command Port
├── move(...)
├── look(...)
├── select(...)
├── interact(...)
└── executeCommand(...)

Query Port
├── getWorldState(...)
├── getActorState(...)
└── getScene(...)

Event Port
├── WorldChanged
├── ActorMoved
├── ObjectSelected
├── DialogueOccurred
└── ActionCompleted
```

구체적인 이름과 API는 기존 repository 구조를 조사한 뒤 결정한다.

### 2.3 Web UI 추가가 아키텍처 변경이 되어서는 안 된다

향후 Web UI가 추가될 경우 다음과 같이 확장 가능해야 한다.

```text
                    World / Application
                           │
                        Scene IR
                           │
              ┌────────────┴────────────┐
              │                         │
              ▼                         ▼
       Terminal Adapter             Web Adapter
              │                         │
       ASCII Renderer              JSON / WebSocket
              │                         │
         CellBuffer              Canvas/WebGL/etc.
              │
             ANSI
```

Web UI 추가 때문에 World, Action, Camera, Scene의 핵심 의미를 다시 설계해야 한다면 현재 구조가 실패한 것으로 본다.

---

## 3. Presentation-neutral Scene / World Model

3D World와 Scene을 Terminal glyph에 종속시키지 않는다.

예:

```text
Scene
├── Camera
│   ├── position
│   ├── yaw
│   ├── pitch
│   └── fov
│
├── Objects
│   ├── id
│   ├── transform
│   ├── geometry
│   ├── material
│   └── interaction metadata
│
├── Actors
├── Effects
└── Timeline
```

Scene 수준에서는 ANSI color나 `#`, `@` 같은 glyph를 핵심 의미로 사용하지 않는다.

Terminal renderer가 Scene을 ASCII/Unicode로 표현한다.

향후 Web renderer는 같은 Scene/World state를 다른 방식으로 표현할 수 있어야 한다.

---

## 4. SSH / PTY Runtime

실제 SSH + PTY interactive session에서 동작해야 한다.

```text
Keyboard / Mouse
        ↓
Terminal Emulator
        ↓
       SSH
        ↓
       PTY
        ↓
Spring Terminal Adapter
        ↓
Application Ports
```

구현 전에 현재 repository의 SSH library와 PTY 지원 수준을 조사한다.

새 SSH/TUI dependency를 무조건 추가하지 않는다.

---

## 5. Terminal 화면 구조

Terminal을 두 개의 논리적 영역으로 나눈다.

```text
┌─────────────────────────────┐
│                             │
│       WORLD VIEWPORT        │
│                             │
│          FIXED              │
│                             │
├─────────────────────────────┤
│                             │
│          CONSOLE            │
│                             │
│         SCROLLING           │
│                             │
└─────────────────────────────┘
```

예를 들어 terminal이 `120 x 50`이면:

```text
Scene   rows 1..30
Console rows 31..50
```

정확한 비율은 `TerminalLayoutCalculator` 같은 별도 책임에서 계산한다.

숫자를 renderer 여러 곳에 hard coding하지 않는다.

---

## 6. Persistent Viewport + Scroll Region

ANSI/VT scrolling region을 이용하여 하단 Console만 scroll되게 한다.

```text
rows 1..N
    FIXED WORLD VIEWPORT

rows N+1..H
    SCROLLING CONSOLE
```

Console output이 아무리 많아도 World Viewport가 위로 밀려 올라가서는 안 된다.

ANSI sequence는 application code에 직접 작성하지 않는다.

예:

```text
TerminalControl
├── setScrollRegion(...)
├── resetScrollRegion()
├── moveCursor(...)
├── clearLine(...)
├── clearRegion(...)
├── hideCursor()
├── showCursor()
├── resetStyle()
├── enableMouseReporting(...)
└── disableMouseReporting()
```

실제 escape sequence encoding은 Terminal adapter 내부에 격리한다.

---

## 7. Primary Screen 전략

기본적으로 `vim`, `less`, `htop` 스타일의 alternate screen을 사용하지 않는다.

원하는 UX는 다음이다.

```text
normal terminal
      ↓
interactive 3D world
      ↓
quit
      ↓
last world frame remains
      ↓
normal terminal scrolling resumes
```

따라서 우선 **primary screen + controlled scroll region** 전략을 사용한다.

호환성 때문에 다른 전략이 필요하면 구현 전에 이유와 trade-off를 문서화한다.

---

## 8. Unified Terminal Input Model

입력을 keyboard-only 구조로 만들지 않는다.

```text
TerminalInputEvent
├── KeyEvent
├── MouseEvent
├── ResizeEvent
└── DisconnectEvent
```

입력 pipeline:

```text
PTY raw bytes
     ↓
TerminalInputDecoder
     ↓
TerminalInputEvent
     ↓
InputRouter
     ↓
InteractionMapper
     ↓
Application Command / Camera Action
```

raw escape sequence를 application/domain logic에 전달하지 않는다.

---

## 9. Keyboard Input

논리적인 KeyEvent를 만든다.

```text
KeyEvent {
    key
    shift
    ctrl
    alt
}
```

최소 지원:

- CHARACTER
- ENTER
- BACKSPACE
- ARROW_UP
- ARROW_DOWN
- ARROW_LEFT
- ARROW_RIGHT
- ESCAPE
- CTRL_C

초기 movement binding:

```text
Shift + ↑  → MOVE_FORWARD
Shift + ↓  → MOVE_BACKWARD
Shift + ←  → STRAFE_LEFT 또는 LOOK_LEFT
Shift + →  → STRAFE_RIGHT 또는 LOOK_RIGHT
```

binding 자체는 쉽게 교체할 수 있어야 한다.

Terminal emulator마다 modifier sequence가 다를 수 있으므로 raw sequence를 추측하지 말고 실제 diagnostic으로 확인한다.

---

## 10. Mouse Reporting

지원하는 terminal emulator에서는 mouse reporting을 활성화한다.

가능하면 modern terminal에서 널리 지원되는 SGR mouse encoding을 우선 검토한다.

논리 event 예:

```text
MouseEvent
├── type
│   ├── PRESS
│   ├── RELEASE
│   ├── MOVE
│   ├── DRAG
│   └── WHEEL
├── button
├── column
├── row
├── shift
├── ctrl
└── alt
```

Mouse는 capability로 취급한다.

```text
TerminalCapabilities
├── color
├── unicode
├── mouseClick
├── mouseRelease
├── mouseDrag
├── mouseMotion
├── mouseWheel
└── modifierKeys
```

Mouse를 지원하지 않는 terminal에서도 keyboard fallback으로 핵심 기능을 사용할 수 있어야 한다.

---

## 11. Mouse Interaction Mapping

World Viewport에서는 초기값으로 다음 mapping을 구현한다.

```text
LEFT CLICK
    → SELECT

LEFT DRAG
    → CAMERA LOOK

WHEEL UP
    → MOVE_FORWARD

WHEEL DOWN
    → MOVE_BACKWARD
```

향후 binding 변경이 가능해야 한다.

일반 terminal mouse reporting을 FPS 게임의 pointer lock과 동일하게 가정하지 않는다.

---

## 12. Prompt와 공간 Interaction의 공존

매우 중요한 요구사항이다.

사용자가 다음을 입력했다고 하자.

```text
> inspect serv_
```

Enter는 아직 누르지 않았다.

이 상태에서:

- Shift+Arrow 이동
- mouse drag 시점 회전
- mouse wheel 이동
- mouse click object selection

을 수행해도 prompt는 그대로 유지되어야 한다.

```text
> inspect serv_
```

그 후 `er`를 계속 입력하면:

```text
> inspect server_
```

가 되어야 한다.

즉 world interaction과 text editing은 mode를 서로 빼앗지 않는다.

```text
TerminalInputEvent
       │
       ▼
   InputRouter
       │
 ┌─────┴──────────────┐
 │                    │
 ▼                    ▼
World Interaction   Prompt Editing
 │                    │
 ▼                    ▼
Immediate Action    PromptBuffer
```

---

## 13. Console

Console에는 다음 유형을 표현할 수 있다.

- SYSTEM
- DIALOGUE
- COMMAND
- COMMAND_RESULT
- WORLD_EVENT
- ERROR

예:

```text
System: You entered the office.

> inspect server

Server status: healthy.

Alice: Good evening.

> _
```

Console history는 terminal emulator의 scrollback에만 의존하지 않는다.

서버 session에서도 상태로 관리한다.

```text
ConsoleState
├── history
├── promptBuffer
└── cursorPosition
```

향후 자체 PageUp/PageDown navigation을 추가할 수 있도록 한다.

---

## 14. 최소 3D World

이번 작업에는 **실제 3D renderer가 포함된다.**

미리 만든 ASCII frame을 교체하는 pseudo-3D 방식으로 완료 처리하지 않는다.

처음부터 범용 game engine을 만들지는 않는다.

최소 primitive:

```text
World
├── GroundPlane
├── Box
├── Wall
├── Door
└── SimpleObject
```

가장 중요한 primitive는 Box다.

```text
Box
├── objectId
├── transform
├── width
├── height
├── depth
└── material
```

작은 office/street scene 하나를 만든다.

---

## 15. 3D 수학과 좌표계

최소 개념:

```text
Vec2
Vec3
Vec4
Mat4
Transform
```

좌표계 convention을 명확히 문서화한다.

예:

```text
X = left/right
Y = up/down
Z = forward/backward
```

right-handed / left-handed 여부도 정하고 일관되게 사용한다.

작은 math dependency를 사용할 수 있지만 거대한 graphics/game framework는 도입하지 않는다.

---

## 16. Camera

최소 Camera state:

```text
Camera
├── position: Vec3
├── yaw
├── pitch
├── fieldOfView
├── nearPlane
└── farPlane
```

초기 action:

```text
MOVE_FORWARD
MOVE_BACKWARD
STRAFE_LEFT
STRAFE_RIGHT
LOOK_LEFT
LOOK_RIGHT
LOOK_UP
LOOK_DOWN
```

Keyboard/Mouse가 Camera를 직접 알지 않도록 InteractionMapper를 거친다.

---

## 17. 실제 Perspective Projection

Orthographic pseudo-3D가 아니라 실제 perspective projection을 구현한다.

```text
Object Geometry
      ↓
Model Transform
      ↓
World Space
      ↓
View Transform
      ↓
Clip Space
      ↓
Perspective Divide
      ↓
Screen Space
      ↓
Rasterization
```

카메라가 물체에 접근하면 같은 geometry가 커지고, 멀어지면 작아져야 한다.

좌우 이동/회전 시 perspective와 보이는 면이 실제로 달라져야 한다.

---

## 18. Terminal Cell Aspect Ratio

Terminal cell은 정사각형 pixel이 아니다.

```text
cellWidth != cellHeight
```

따라서 projection에서 terminal geometry를 고려할 수 있어야 한다.

```text
TerminalGeometry
├── columns
├── rows
└── estimatedCellAspectRatio
```

초기에는 합리적인 기본값을 사용하되 설정/Capability로 조정 가능하게 한다.

---

## 19. Clipping

최소한 정상적인 walking experience에 필요한 clipping을 구현한다.

특히 near plane과 camera 뒤 geometry를 처리한다.

다음이 발생해서는 안 된다.

- NaN
- Infinity
- 화면 전체를 가로지르는 잘못된 선
- 카메라가 벽에 접근할 때 projection 폭발

완전한 범용 graphics clipping library까지 만들 필요는 없다.

---

## 20. Depth Handling

앞 물체가 뒤 물체를 가려야 한다.

Z-buffer와 Painter's Algorithm을 비교하고 현재 renderer에 적절한 방식을 선택한다.

solid surface와 object selection을 고려하면 depth buffer를 우선 검토한다.

선택 이유를 문서화한다.

---

## 21. ASCII/Unicode Rasterizer

최종 framebuffer의 pixel 역할은 terminal Cell이 담당한다.

```text
Cell
├── glyph
├── semanticStyle
├── depth
└── objectId
```

`objectId`는 mouse hit testing을 위해 중요하다.

단순 Java `char` 하나가 항상 terminal cell 하나라고 가정하지 않는다.

---

## 22. Shading

초기에는 복잡한 lighting engine이 필요 없다.

surface orientation, depth, material 등에 따라 문자 density를 선택하는 간단한 shading을 구현한다.

예:

```text
' '
'.'
':'
'-'
'='
'+'
'*'
'#'
'%'
'@'
```

character ramp는 renderer 곳곳에 hard coding하지 않는다.

Theme/AsciiShader configuration으로 분리한다.

---

## 23. Semantic Style / Color

World domain에 ANSI 색상을 직접 넣지 않는다.

예:

```text
SemanticStyle
├── STRUCTURE
├── WINDOW
├── FLOOR
├── ACTOR
├── SELECTED
├── SYSTEM
├── DIALOGUE
└── WARNING
```

Terminal theme이 이를 ANSI color/style로 변환한다.

향후 Web UI는 동일한 의미를 CSS/WebGL material 등으로 표현할 수 있다.

---

## 24. Mouse Drag → Camera

World Viewport에서 mouse drag:

```text
Mouse Δx
   ↓
Camera yaw

Mouse Δy
   ↓
Camera pitch
```

그 결과:

```text
Camera update
     ↓
View Matrix update
     ↓
Perspective Projection
     ↓
New CellBuffer
     ↓
Terminal update
```

가 발생해야 한다.

---

## 25. Mouse Hit Testing

Renderer가 각 visible cell에 `objectId`를 보존할 수 있도록 한다.

그러면:

```text
Mouse click(x,y)
      ↓
Viewport coordinate
      ↓
CellBuffer[x,y]
      ↓
objectId
      ↓
SELECT_OBJECT
```

으로 처리할 수 있다.

첫 버전에서는 복잡한 3D ray casting보다 이 screen-space object buffer 방식을 우선 사용한다.

---

## 26. Rendering Architecture

권장 흐름:

```text
World State
     ↓
Scene / Camera
     ↓
PerspectiveSceneRenderer
     ↓
RenderedScene
     ├── CellBuffer
     └── interaction/object metadata
             ↓
TerminalRenderer
             ↓
ANSI
             ↓
PTY
```

전체 terminal rendering은 개념적으로:

```text
TerminalRenderer
├── WorldViewportRenderer
├── ConsoleRenderer
└── PromptRenderer
```

로 분리한다.

---

## 27. Unicode-aware 확장 가능성

향후 다음을 정확히 처리할 수 있어야 한다.

- Hangul
- CJK
- emoji
- grapheme cluster
- combining character
- variation selector
- ZWJ sequence
- wide character
- ANSI zero-width sequence

이번 단계에서 완성형 Unicode layout engine을 만들 필요는 없다.

그러나 다음 전제를 core architecture에 넣지 않는다.

```text
String.length == terminal width
char == one terminal cell
```

---

## 28. Rendering Loop와 Concurrency

동시에 발생할 수 있는 event:

- keyboard input
- mouse input
- resize
- command result
- world event
- animation tick
- disconnect

여러 thread가 PTY output에 동시에 ANSI sequence를 write하지 않도록 한다.

권장 구조:

```text
Input / Events
      ↓
Session Event Queue
      ↓
Single Session Loop
      ↓
State Update
      ↓
Render
      ↓
Output
```

Terminal output ownership을 명확히 serialize한다.

필요 이상으로 복잡한 reactive framework를 도입하지 않는다.

---

## 29. Frame Rate / Frame Diff

처음부터 60 FPS를 목표로 하지 않는다.

SSH 환경에서 다음을 우선한다.

- 충분한 입력 반응성
- 이동 시 연속적인 perspective 변화
- 낮은 flicker
- 과도하지 않은 network output

초기 correctness 단계에서는 viewport full redraw를 허용한다.

그러나 구조적으로 향후:

```text
Previous CellBuffer
        +
Current CellBuffer
        ↓
FrameDiff
        ↓
Changed Cells Only
        ↓
ANSI updates
```

로 발전 가능해야 한다.

매 frame 전체 terminal clear를 수행하는 구조는 피한다.

---

## 30. Terminal Resize

PTY resize event를 처리한다.

```text
ResizeEvent
     ↓
TerminalLayout recalculation
     ↓
Scene viewport resize
     ↓
Camera aspect ratio update
     ↓
Projection update
     ↓
Console resize
     ↓
Full redraw
```

resize 후 mouse hit testing coordinate도 새로운 layout과 일치해야 한다.

너무 작은 terminal에서는 graceful degradation한다.

예:

```text
Terminal too small for 3D world.
Recommended minimum: 80 x 24
```

---

## 31. Quit UX

사용자가:

```text
> quit
```

을 실행하면 다음 lifecycle을 수행한다.

```text
stop animation/render loop
        ↓
stop world interaction
        ↓
disable mouse reporting
        ↓
reset scroll region
        ↓
restore cursor
        ↓
reset ANSI style
        ↓
restore input mode
        ↓
position cursor below final UI
        ↓
flush output
        ↓
terminate interactive runtime
```

**화면을 clear하지 않는다.**

종료 직전 마지막 3D frame과 console이 그대로 남아야 한다.

종료 후 shell이 이어지는 환경이라면:

```text
┌──────────────────────────────────────────────┐
│           last 3D world frame                │
│                                              │
├──────────────────────────────────────────────┤
│ > quit                                       │
└──────────────────────────────────────────────┘

user@server:~$ _
```

이 되어야 한다.

이후:

```text
user@server:~$ ls
README.md  src  build.gradle.kts
```

같은 출력이 발생하면 기존 World 화면까지 포함하여 화면 전체가 정상 terminal history처럼 위로 scroll되어야 한다.

---

## 32. Cleanup 안전성

정상 `quit`뿐 아니라 가능한 범위에서 다음을 처리한다.

- Ctrl+C
- exception
- SSH disconnect
- server-side failure

cleanup 시 최소한:

```text
mouse reporting OFF
scroll region RESET
cursor VISIBLE
ANSI style RESET
input mode RESTORED
output FLUSHED
```

를 보장한다.

사용자가 종료 후 `reset`이나 `stty sane`을 실행해야만 terminal을 사용할 수 있는 상태를 만들지 않는다.

---

## 33. Terminal Diagnostic Mode

Terminal emulator별 입력 차이를 실제로 확인할 수 있도록 개발용 diagnostic mode를 제공한다.

예:

```text
--terminal-diagnostics
```

Keyboard:

```text
RAW: ...
DECODED: SHIFT + ARROW_UP
```

Mouse:

```text
RAW: ...
DECODED:
  type: DRAG
  button: LEFT
  x: 42
  y: 8
```

Resize:

```text
RESIZE: 120x50 -> 100x35
```

Arrow/Shift+Arrow/Mouse escape sequence를 추측해서 구현하지 않는다.

---

## 34. 권장 Package / Module Boundary

정확한 package 구조는 기존 repository를 조사한 뒤 조정하되 개념적으로 다음 경계를 유지한다.

```text
world/
├── domain/
│   ├── World
│   ├── Actor
│   ├── WorldObject
│   ├── Transform
│   ├── Material
│   └── WorldEvent
│
├── application/
│   ├── port/
│   │   ├── command/
│   │   ├── query/
│   │   └── event/
│   └── service/
│
├── scene/
│   ├── Scene
│   ├── Camera
│   └── presentation-neutral scene model
│
└── adapters/
    ├── ssh/
    │   ├── session/
    │   ├── input/
    │   ├── layout/
    │   ├── ansi/
    │   ├── capability/
    │   └── rendering/
    │
    └── web/
        └── reserved / future
```

3D terminal renderer 내부는 예를 들어:

```text
adapters/ssh/rendering/
├── math/
│   ├── Vec2
│   ├── Vec3
│   ├── Vec4
│   └── Mat4
├── geometry/
├── Projection
├── Clipper
├── Rasterizer
├── DepthBuffer
├── AsciiShader
├── Cell
├── CellBuffer
├── FrameDiff
└── PerspectiveSceneRenderer
```

단, `Camera`나 presentation-neutral geometry가 향후 Web renderer에서도 필요하다면 SSH adapter 내부에 가두지 말고 적절한 neutral layer로 올린다.

---

## 35. 향후 Web Adapter

이번 작업에서 Web UI 자체를 구현할 필요는 없다.

하지만 architecture상 다음이 가능해야 한다.

```text
Browser
   ↓
REST / WebSocket
   ↓
Web Adapter
   ↓
Application Ports
   ↓
World Core
```

출력도:

```text
World / Scene
     │
     ├── SSH → ASCII/Unicode → ANSI
     │
     └── Web → JSON/state → Canvas/WebGL/DOM
```

형태가 가능해야 한다.

REST 하나만을 architecture의 중심으로 만들 필요는 없다.

Command/Query/Event semantics를 먼저 정의하고 HTTP/WebSocket은 adapter protocol로 취급한다.

---

## 36. 향후 AI Agent Adapter

AI Agent 역시 특별한 World가 아니라 동일한 application port를 사용하는 또 다른 Actor/client가 될 수 있어야 한다.

```text
AI Agent
   ↓
REST / MCP / direct adapter
   ↓
Application Ports
   ↓
World
```

MCP를 Core dependency로 만들지 않는다.

---

## 37. 최소 Command

PoC command는 다음 정도면 충분하다.

```text
help
look
status
clear-log
quit
```

Command framework를 과도하게 만들지 않는다.

---

## 38. 구현 전 Repository 조사

대규모 코드를 작성하기 전에 반드시 기존 repository를 조사한다.

확인 사항:

1. Spring Boot 버전
2. Spring Framework 버전
3. Java/Kotlin 버전
4. JVM 버전
5. SSH server 구현 여부와 방식
6. SSH library
7. PTY 접근 방식
8. raw input 지원 방식
9. resize event 지원
10. 현재 session abstraction
11. concurrency model
12. package/module architecture
13. test infrastructure
14. ANSI/terminal 관련 dependency
15. 기존 domain/application port 구조 존재 여부

기존 architecture를 최대한 존중한다.

---

## 39. Dependency 원칙

새 dependency를 무조건 추가하지 않는다.

특히 다음을 피한다.

- OpenGL
- Vulkan
- Unity
- Unreal
- 대형 game engine
- 3D 때문에 도입하는 거대한 graphics framework
- architecture 전체를 장악하는 TUI framework

필요한 것이 작은 vector/matrix 연산이라면 직접 구현과 작은 dependency를 비교한다.

새 dependency 추가 시 다음을 기록한다.

- 왜 필요한가
- 현재 dependency로 불가능한가
- 더 작은 대안은 무엇인가
- Domain/Application Core가 해당 library에 종속되는가

---

## 40. 구현 순서

다음 순서로 진행한다.

```text
Phase 1
Repository / architecture 조사

Phase 2
SSH + PTY interactive session 검증

Phase 3
Terminal diagnostic mode

Phase 4
Raw keyboard decoding

Phase 5
ANSI cursor + primary screen + scroll region

Phase 6
Persistent viewport + scrolling console

Phase 7
Prompt editor

Phase 8
Mouse reporting + MouseEvent decoding

Phase 9
Unified TerminalInputEvent / InputRouter

Phase 10
Domain/Application Port boundary 정리

Phase 11
Presentation-neutral Scene / Camera model

Phase 12
3D math primitives

Phase 13
Box / Ground / minimal geometry

Phase 14
View transformation + Perspective projection

Phase 15
Clipping

Phase 16
Rasterization + Depth handling

Phase 17
ASCII/Unicode CellBuffer + shading

Phase 18
CellBuffer → ANSI Terminal rendering

Phase 19
Keyboard → World/Camera movement

Phase 20
Mouse drag → Camera yaw/pitch

Phase 21
Mouse wheel → movement

Phase 22
objectId buffer + click hit testing

Phase 23
Prompt + 3D interaction integration

Phase 24
Console scrolling integration

Phase 25
Resize + projection/layout recalculation

Phase 26
Frame diff / flicker / output optimization

Phase 27
Quit lifecycle + primary-screen restoration

Phase 28
Abnormal termination cleanup

Phase 29
Actual SSH acceptance tests

Phase 30
Architecture refactoring + documentation
```

---

## 41. Acceptance Tests

### A. SSH 접속

실제 SSH + PTY로 접속하면 3D World Viewport와 Console이 나타난다.

### B. 실제 Perspective

동일한 3D object에 접근하면 커지고 멀어지면 작아진다.

회전하면 보이는 면과 위치가 실제 projection에 따라 변한다.

미리 만든 ASCII frame 교체 방식은 허용하지 않는다.

### C. Console Scroll

많은 command/dialogue가 발생해도 하단 Console만 scroll되고 상단 World Viewport는 고정된다.

### D. Keyboard Movement

Shift+Arrow가 prompt에 이상한 escape 문자를 삽입하지 않고 World/Camera action으로 처리된다.

### E. Prompt Preservation

```text
> inspect serv_
```

상태에서 이동/회전을 여러 번 해도 prompt가 그대로 유지된다.

### F. Mouse Camera

World Viewport에서 drag하면 Camera yaw/pitch가 변경되고 perspective frame이 다시 렌더링된다.

### G. Mouse Wheel

World Viewport의 wheel이 movement로 매핑된다.

Console 영역의 wheel이 World movement를 발생시키지 않아야 한다.

### H. Mouse Selection

보이는 object를 click하면 해당 rendered cell/objectId를 통해 WorldObject가 선택된다.

### I. Resize

Terminal resize 시:

- layout 재계산
- viewport resize
- camera aspect ratio update
- projection update
- console resize
- mouse coordinate/hit testing update

가 정상적으로 이루어진다.

### J. Quit

`quit` 후:

- 마지막 3D frame 유지
- 화면 clear 안 함
- mouse reporting OFF
- scroll region RESET
- cursor 복구
- ANSI style 복구
- input mode 복구
- 이후 화면 전체 정상 scrolling

을 확인한다.

### K. Abnormal Cleanup

Ctrl+C, exception, disconnect 등의 가능한 종료 경로에서도 terminal이 raw/mouse/scroll-region 상태에 갇히지 않는다.

### L. Architecture Boundary

SSH adapter를 제거하거나 mock adapter로 대체해도 Domain/Application 핵심 use case가 동작해야 한다.

Terminal-specific type이 Domain/Application Core public API에 노출되지 않아야 한다.

---

## 42. 실제 테스트 환경

최소한 다음 환경을 검증한다.

```text
Linux terminal
WSL
Windows Terminal → SSH → Linux server
```

특히 실제 raw event를 확인한다.

- Arrow
- Shift+Arrow
- Character
- Enter
- Backspace
- Ctrl+C
- Mouse Click
- Mouse Release
- Mouse Drag
- Mouse Move
- Mouse Wheel
- PTY Resize

Terminal emulator 차이가 발견되면 capability/fallback으로 처리한다.

---

## 43. 이번 작업에서 하지 않을 것

이번 vertical slice의 범위를 흐리지 않기 위해 다음은 구현하지 않는다.

- Web UI 자체
- GUI
- OpenGL/Vulkan
- Unity/Unreal
- full physics engine
- 복잡한 collision engine
- 대규모 procedural city
- LLM integration
- AI Agent orchestration
- Neo4j
- OWL
- Ledger
- MQTT
- 완성형 enterprise domain
- 완성형 Unicode layout engine

단, 향후 이들이 adapter 또는 domain extension으로 추가되는 것을 막는 구조를 만들지 않는다.

---

## 44. 최종 End-to-End 완료 정의

다음 흐름이 실제 SSH session에서 동작해야 한다.

```text
SSH CONNECT
     ↓
3D OFFICE / STREET APPEARS
     ↓
REAL PERSPECTIVE RENDERING
     ↓
Shift+Arrow
     ↓
CAMERA MOVES
     ↓
PERSPECTIVE CHANGES
     ↓
Mouse Drag
     ↓
CAMERA ROTATES
     ↓
Mouse Wheel
     ↓
CAMERA MOVES
     ↓
Mouse Click
     ↓
VISIBLE 3D OBJECT SELECTED
     ↓
Text Input Continues Independently
     ↓
COMMAND / DIALOGUE
     ↓
CONSOLE SCROLLS
while
3D VIEWPORT REMAINS FIXED
     ↓
Terminal Resize
     ↓
LAYOUT + 3D PROJECTION ADAPT
     ↓
quit
     ↓
LAST 3D FRAME REMAINS
     ↓
MOUSE / RAW / SCROLL REGION RESTORED
     ↓
NORMAL TERMINAL
     ↓
ENTIRE SCREEN SCROLLS NORMALLY
```

---

## 45. 핵심 성공 조건

### 45.1 실제 공간감

사용자가 이동하거나 시점을 돌렸을 때 단순 ASCII animation이 아니라 **실제 perspective projection을 통해 공간 안을 이동하고 있다는 느낌**이 나야 한다.

### 45.2 Terminal-native Interaction

Keyboard, mouse, prompt, command, dialogue가 서로 배타적인 mode가 아니라 하나의 SSH terminal session 안에서 자연스럽게 공존해야 한다.

### 45.3 Persistent Viewport + Streaming Console

World는 화면 상단에서 살아 있고, command/dialogue/event history는 하단에서 독립적으로 흐른다.

### 45.4 자연스러운 World 진입과 이탈

`quit` 시 마지막 World 화면이 terminal history에 남고 화면 전체가 다시 일반 terminal stream으로 돌아와야 한다.

### 45.5 Presentation Independence

현재 구현의 핵심 client는 SSH Terminal이지만, **World/Application Core는 Terminal에 종속되지 않아야 한다.**

향후 Web UI를 추가하는 작업은 Core 재작성 작업이 아니라 새로운 adapter를 추가하는 작업이어야 한다.

### 45.6 Renderer 교체 가능성

Terminal에서는:

```text
Scene → Perspective ASCII/Unicode Renderer → CellBuffer → ANSI
```

Web에서는 향후:

```text
Scene → Web Renderer → Canvas/WebGL/DOM
```

같은 식으로 presentation 기술을 교체할 수 있어야 한다.

---

## 46. Codex 작업 원칙

1. 먼저 repository를 읽고 현재 구조를 파악한다.
2. 대규모 rewrite를 하지 않는다.
3. 기존 코드를 최대한 보존한다.
4. Domain/Application과 SSH adapter의 경계를 먼저 확인한다.
5. 작은 vertical slice를 실제 SSH에서 계속 실행 검증한다.
6. terminal escape sequence는 diagnostic으로 실제 확인한다.
7. 3D 수학은 테스트 가능한 작은 단위로 구현한다.
8. renderer correctness를 optimization보다 우선한다.
9. 각 Phase 완료 후 build/test를 수행한다.
10. terminal restoration은 기능 구현과 동급의 필수 요구사항으로 취급한다.
11. Web/Agent 같은 미래 adapter를 이유로 현재 구현을 과도하게 일반화하지 않는다.
12. 반대로 현재 SSH 구현 편의를 위해 Core를 Terminal에 종속시키지도 않는다.

최종 결과는 **Spring 서버 안에 존재하는 World Runtime과, 그 Runtime을 SSH Terminal을 통해 3D ASCII/Unicode 공간으로 경험하는 하나의 완전한 presentation adapter**여야 한다.
