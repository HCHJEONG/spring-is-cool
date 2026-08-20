# Implementation Plan Original Notes

이 문서는 본격적인 구현 계획이나 아키텍처 명세가 아니라, 현재까지의 대화에서 추출된 최상위 원칙을 가급적 원문 손실 없이 보존하기 위한 기록이다.

나중에 본격적인 계획은 다른 파일에서 별도로 정리한다.

## 최상위 원칙 12개

좋습니다. 중단된 부분까지 포함해서 **현재까지의 대화에서 추출된 최상위 원칙 12개**를 다시 정리하면 다음과 같습니다. 아직 아키텍처 명세라기보다 이후 모든 설계 결정을 판단할 때 사용할 **프로젝트 헌법에 가까운 원칙**입니다.

### 1. Backend is the Product.

Spring 서버 자체가 완전한 제품이어야 한다. GUI나 전용 TUI가 없어도 `curl`과 `SSH`만으로 핵심 기능과 경험을 모두 사용할 수 있어야 한다.

### 2. Text Adventure is the Interaction Model.

기존 업무툴에 XP·레벨·퀘스트를 덧붙이는 gamification이 아니다. **세계와 업무에 상호작용하는 방식 자체가 text adventure**여야 한다.

### 3. Presentation is Replaceable.

SSH, CLI/TUI, GUI, 모바일 등의 presentation은 선택 가능하고 교체 가능해야 한다. 1980년대 녹색 CRT/메인프레임 스타일은 중요한 초기 aesthetic이지만 core에 고정하지 않는다.

### 4. Text is a Visual Medium.

텍스트는 단순 정보가 아니다. 문자, 공백, Unicode, 한글/CJK, Emoji, box drawing, text illustration, 위치와 배치 모두를 **그래픽 표현수단**으로 취급한다.

### 5. Time is Part of Rendering.

무엇을 표시하는지만큼 **언제 어떻게 나타나는가**가 중요하다. 글자·단어·문장별 속도, pause, punctuation timing, cursor, reveal, interruption, animation 등이 장면의 일부다.

### 6. The Cinematic Renderer is the Primary Differentiator.

Spring, DB, OWL, MQTT, Agent orchestration 자체가 차별화의 중심이 아니다. **서버측 cinematic Unicode/text rendering engine**이 초기 개발에서 가장 많은 기술적 노력을 투입해야 할 핵심이다.

### 7. AI Creates; the Renderer Controls.

AI는 narrative, dialogue, text art, layout, timing, animation 등의 **창의적 의도와 Scene을 생성**할 수 있다. 그러나 AI가 raw ANSI를 직접 지배하지 않는다. deterministic renderer가 검증하고 실제 terminal output을 만든다.

### 8. Scene IR Separates Creativity from Execution.

현재 가장 유력한 경계는:

```text
World State
     ↓
AI Director / Scene Generation
     ↓
Scene IR
     ↓
Unicode / Layout / Timeline / Style Engine
     ↓
Terminal
```

이다. Scene IR의 구체적 schema는 prototype 전에 성급하게 고정하지 않는다.

### 9. The World is More Fundamental than the Game Metaphor.

`Quest`, `Location`, `Character`, `Item` 같은 게임 용어를 근본 모델에 고정하지 않는다. 더 일반적인 World/Actor/Action/Activity/Resource/Context/Event 등의 의미 모델 위에서 presentation이 게임 vocabulary를 부여할 수 있어야 한다.

### 10. Everything External is an Adapter Candidate.

DB, LLM/model provider, MQTT, OPC-UA, HTTP, MCP 등의 기술에 core가 종속되지 않는다. Human, AI Agent, software service, device/machine 등 다양한 주체와 외부 세계를 Ports-and-Adapters 방식으로 연결할 수 있게 한다. **MCP 역시 선택 가능한 adapter일 뿐 필수 기반이 아니다.**

### 11. Semantics, Behavior, Connection, and Presentation Stay Separate.

가능한 한:

> **OWL defines what the world means.**
> **Spring/Java defines how the world behaves.**
> **Adapters define how the world connects.**
> **Clients/themes define how the world appears.**

라는 경계를 유지한다. Enterprise뿐 아니라 Factory, Family, Personal World 및 Ledger 같은 장기 확장을 이 원칙 안에서 수용한다.

### 12. Prove the Experience Before Building the Platform.

처음부터 Enterprise Runtime, OWL ontology, Ledger, Governance, MQTT, 복잡한 Workflow를 완성하지 않는다. 작은 가짜 World 하나로 **30~60초짜리 cinematic terminal scene**부터 만든다.

예를 들면:

```text
An empty office.


                  ☎


               RING...


                  ☎


            RING... RING...


Someone is calling.


> _
```

이 장면에서 **Unicode + text illustration + 색 + 공간 + timing + AI generation**을 제대로 결합했을 때 실제로 재미와 분위기가 생기는지를 먼저 증명한다.

> **첫 번째 성공 기준은 "기능이 많은가?"가 아니라 "이 터미널 세계에 계속 머물고 싶은가?"이다.**

이 12개를 이후 구조화 단계에서 가장 위에 놓고, 어떤 설계가 이 원칙과 충돌한다면 **설계 쪽을 의심하는 것**이 좋겠습니다.

## 부록: 초기 원문 전체

아래 내용은 사용자가 처음 첨부한 원문들을 수정 없이 순서대로 보존한 것이다.

### 부록 원문 1

좋습니다. 이번에는 **설계하지 않겠습니다.** 지금까지의 대화를 시간 순서에 가깝게 다시 따라가면서, 나중에 판단할 수 있도록 **아이디어·요구·합의·제안·보류사항을 원석 상태로 최대한 회수**하겠습니다. 비슷한 내용도 의미가 조금 다르면 일부러 남겨두겠습니다.

## 1단계 — 지금까지 대화에서 회수한 원석

**1. 출발점은 일반적인 업무툴의 gamification이 아니다.** 처음에는 프로젝트=Campaign, 업무=Quest 같은 매핑도 이야기했지만, 대화가 진행되면서 그것보다 훨씬 본질적인 방향이 발견됐다. 포인트, 레벨, XP, 리더보드 등을 기존 업무툴에 붙이는 것이 핵심이 아니다.

**2. 사용자가 떠올린 중요한 원형은 고전 게임 `The Dallas Quest`다.** 화려하게 움직이는 그래픽보다 정적인 화면과 텍스트, 대화와 선택을 중심으로 진행되는 어드벤처의 감성을 업무 시스템에 가져오는 것이다.

**3. 따라서 '업무툴을 게임처럼 보이게 하는 것'보다 '업무와 상호작용하는 방식 자체를 text adventure처럼 만드는 것'이 더 중요하다.**

**4. 실제 업무의 상태가 게임의 세계 상태가 될 수 있다.** 프로젝트 문제, 고객 요청, 승인 대기, 장애, Agent 작업 등이 별도의 가짜 게임 데이터가 아니라 세계에서 실제로 일어나고 있는 사건이 된다.

**5. 대화는 단순한 UI 장식이 아니다.** 등장인물 또는 AI와의 대화가 실제 query, decision, command, delegation 등으로 이어질 수 있다.

**6. 사용자는 선택지만 클릭할 필요가 없다.** `ASK LEGAL` 같은 고전 명령도 가능하지만 자연어로 "Legal에게 확인하고 Cline에게 다른 방법을 조사시켜"라고 말하는 것도 가능할 수 있다.

**7. `LOOK`, `TALK`, `GO`, `ASK`, `GIVE`, `USE`, `INVENTORY` 같은 text-adventure vocabulary를 실제 업무 operation과 연결할 수 있다는 아이디어가 나왔다.**

**8. `Location` 역시 반드시 물리적 장소일 필요가 없다.** Project Room, Incident Room, Legal Office 같은 장소는 실제로는 업무 context를 adventure metaphor로 표현한 것일 수 있다.

**9. 하지만 Quest, Location, Character, Item 같은 게임 용어를 가장 근본적인 domain/ontology에 박아 넣지는 않는 편이 좋다는 의견에 대체로 합의했다.**

**10. 그 아래에는 Actor, Resource, Activity, Context, Event, Policy, Capability, Evidence 같은 더 일반적인 개념을 둘 수 있다.**

**11. 그리고 presentation에 따라 `Task → Quest`, `Context → Location`, `Actor → Character`, `Resource → Item`처럼 보이게 할 수 있다.**

---

**12. 구현은 이번에는 Spring 백엔드부터 시작한다.**

**13. 그 이유는 단순히 Java/Spring을 쓰고 싶어서가 아니라 서버 컴포넌트가 제품의 중심이기를 원하기 때문이다.**

**14. UI는 언제든 바꿀 수 있어야 한다.**

**15. Web GUI가 없어도 시스템은 완전하게 작동해야 한다.**

**16. TUI가 없어도 시스템은 완전하게 작동해야 한다.**

**17. 극단적으로 터미널에서 `curl`과 `ssh` 정도만 있어도 모든 핵심 기능을 수행할 수 있게 하는 것이 목표다.**

**18. 즉 백엔드가 'UI가 붙기 전의 미완성 제품'이어서는 안 된다. 백엔드 자체가 완전한 제품이다.**

**19. REST API는 기계 친화적인 interface가 되고 SSH는 인간에게 직접 세계를 제공하는 first-class interface가 될 수 있다.**

**20. 같은 application command가 REST, SSH, TUI, GUI, AI 등 여러 interface에서 호출되게 해야 한다.**

**21. 예를 들어 `AssignQuestCommand` 하나를 HTTP, SSH 명령, TUI 선택, 자연어 Agent가 모두 사용할 수 있는 구조가 이상적이다.**

**22. 따라서 SSH handler나 REST controller가 직접 domain repository를 조작하는 식으로 만들면 안 된다.**

---

**23. 초기 UI 감성은 1980년대 업무용 터미널이다.**

**24. 단순한 현대적인 '해커 녹색 화면'보다는 펀치카드·메인프레임·CRT 업무 시스템의 분위기를 원한다.**

**25. 초록색 계열 터미널을 좋아한다.**

**26. `JOB`, `QUEUE`, `SUBMITTED`, `RUNNING`, `COMPLETED`, `COMMAND ==>` 같은 오래된 업무 시스템의 언어가 이 세계와 잘 어울릴 수 있다는 이야기가 나왔다.**

**27. 펀치카드/메인프레임의 `JOB → QUEUE → EXECUTE → RESULT`라는 모델과 AI 업무 시스템의 `Task → Assign → Agent Execute → Event/Evidence`가 의외로 자연스럽게 대응한다.**

**28. 다만 이것 역시 하나의 presentation이다. 핵심 domain이 1980년대 터미널 개념에 종속되어서는 안 된다.**

**29. TUI와 GUI 중 무엇을 쓰느냐는 결국 사용자의 취향 문제다.**

**30. GUI가 생겨도 Dallas Quest처럼 정적인 그림 + 텍스트/대화 중심으로 만들 수 있다.**

**31. CLI/TUI는 임시 개발 UI가 아니라 장기적으로도 정식 client/operator console로 남을 수 있다.**

---

**32. Cline 같은 AI coding agent도 이 세계에 참여할 수 있어야 한다.**

**33. 인간이 Quest/업무를 Cline에게 할당하고 Cline이 그것을 수행하여 결과와 artifact를 돌려주는 형태를 생각했다.**

**34. 더 장기적으로는 AI Agent가 단순한 보조자가 아니라 사용자를 대신해 실제 업무를 처리할 수 있어야 한다.**

**35. 사용자가 자리를 비운 사이 Agent들이 일을 하고 사용자가 돌아왔을 때 그 결과와 필요한 decision을 볼 수 있는 persistent world가 가능하다.**

**36. 이 때문에 `User`, `Actor`, `Agent`를 개념적으로 구별할 필요가 있다는 아이디어가 나왔다.**

**37. 사람이 직접 행동할 수도 있고 Agent에게 권한을 위임할 수도 있다.**

**38. `Delegation`은 상당히 중요한 개념이 될 가능성이 있다.**

**39. Agent가 사용자를 대신해 행동한다면 `performedBy`, `onBehalfOf`, `authority/delegation`, 결과 등을 기록할 필요가 있다.**

**40. Agent에게 무엇을 할 수 있는지 capability와 permission을 부여할 수 있다.**

**41. 장기적으로 Human, AI Agent, software service, device/machine 등이 동일한 세계에 행동 주체로 존재할 수 있다.**

---

**42. Cline이나 다른 AI가 참여하기 위해 MCP가 필수일 필요는 없다.**

**43. MCP는 integration 방법 중 하나일 뿐이다.**

**44. REST/HTTP, CLI, MCP 또는 다른 protocol을 사용할 수 있어야 한다.**

**45. 따라서 domain이나 application core가 MCP의 tool model에 종속되면 안 된다.**

---

**46. 전체적으로 Hexagonal Architecture / Ports-and-Adapters 성격을 강하게 가져가는 방향이 좋다.**

**47. 다만 처음부터 무수한 microservice를 만드는 것이 아니라 Spring Modular Monolith로 시작하는 것이 적절하다는 의견이 나왔다.**

**48. 중요한 것은 물리적인 서비스 개수보다 boundary를 명확하게 유지하는 것이다.**

**49. PostgreSQL을 실제 구현에 사용하더라도 domain/application core가 PostgreSQL을 알아서는 안 된다.**

**50. persistence는 repository/port 뒤로 숨긴다.**

**51. LLM/model도 특정 공급자에 묶이지 않게 한다.**

**52. OpenAI, Anthropic, local model 등이 adapter가 될 수 있다.**

**53. LLM 자체가 없어도 core가 동작할 수 있어야 한다.**

**54. Messaging도 추상화한다.**

**55. Device/protocol도 추상화한다.**

**56. 공장으로 확장할 경우 MQTT를 사용할 수 있지만 core가 MQTT를 알아서는 안 된다.**

**57. 향후 OPC-UA, HTTP 또는 다른 industrial protocol adapter를 붙일 수 있어야 한다.**

**58. MQTT message가 바로 domain object가 되는 것이 아니라 inbound adapter에서 의미 있는 Event/Command로 변환되는 구조가 좋다.**

**59. 반대로 World의 Action이 실제 기계 명령으로 내려갈 때도 policy/authorization 등을 거쳐 DeviceCommandPort → MQTT/OPC-UA adapter 식으로 연결할 수 있다.**

**60. DB/LLM/MQTT를 제거하고 InMemory/Fake/Simulator adapter만으로도 domain/application 테스트가 돌아가는지를 architecture quality의 한 기준으로 삼을 수 있다는 아이디어가 나왔다.**

---

**61. semantic/domain model은 가능한 한 OWL(Web Ontology Language) 표준과 호환·정렬되는 방향을 원한다.**

**62. OWL/RDF를 단순히 나중에 붙이는 metadata 정도로 보지 않고 세계의 의미 모델을 표현하는 기반으로 고려한다.**

**63. 그러나 OWL reasoner가 runtime behavior까지 담당하는 구조를 원하는 것은 아니다.**

**64. `OWL defines what the world means`, `Spring/Java defines how the world behaves`라는 분리가 제안되었고 방향성이 잘 맞았다.**

**65. RDF store/graph 역시 adapter로 생각할 수 있다.**

**66. World를 semantic knowledge graph로 볼 가능성이 있다.**

**67. `WHO CAN FIX ROBOT-17?` 같은 질문을 capability/relationship ontology를 이용해 해결할 수도 있다.**

**68. Human, AI, Service, Device가 ontology상 같은 세계에 표현될 수 있다.**

**69. Enterprise ontology만을 만드는 것이 아니라 더 일반적인 World ontology를 생각한다.**

---

**70. 이 World는 기업에 한정될 필요가 없다.**

**71. Enterprise World가 될 수도 있다.**

**72. Factory World가 될 수도 있다.**

**73. Family World가 될 수도 있다.**

**74. Personal World도 가능하다.**

**75. 사용자가 원하면 가족 ontology를 text-adventure/game 방식으로 사용할 수도 있다.**

**76. 가족 구성원, 자산, 자동차, 주택, 보험, 세금, 교육, obligation 등을 같은 World 안에서 표현할 수 있다.**

---

**77. 재무 상태관리를 코어 기능 중 하나로 넣고 싶다.**

**78. 단식부기 또는 복식부기를 지원하고 싶다.**

**79. 단식부기와 복식부기를 완전히 별개의 시스템으로 만들기보다는 내부에는 충분히 강한 ledger 모델을 두고 단순한 view를 제공하는 방식도 검토할 가치가 있다는 의견이 나왔다.**

**80. 실제 정보가 단식부기 수준밖에 없는 경우를 어떻게 표현할지는 아직 확정할 문제가 아니다.**

**81. Ledger를 회사 회계 모듈로 한정하지 않고 World의 value/resource state 변화 기록 장치로 일반화할 가능성이 있다.**

**82. `Actor → Action → Event → Evidence`와 `Economic Event → Transaction → Posting → Account`를 연결할 수 있다.**

**83. 즉 실제 업무 행위가 경제적 결과를 발생시켰다면 그 관계를 추적할 수 있다.**

**84. 가족 World에서도 수입, 지출, 자산, 부채, 세금, 보험 등의 상태를 같은 방식으로 관리할 수 있다.**

---

여기까지의 상당 부분은 중요한 기반이지만, 대화 후반에 **무엇이 정말 차별화 기술인지**가 새롭게 발견됐다.

**85. Spring 자체는 차별화가 아니다.**

**86. PostgreSQL도 차별화가 아니다.**

**87. OWL도 그 자체로는 차별화가 아니다.**

**88. MQTT/OPC-UA adapter도 그 자체로는 차별화가 아니다.**

**89. Agent orchestration 자체도 이미 흔해지고 있다.**

**90. 이 프로젝트에서 사활을 걸어야 할 초기 핵심은 서버측 cinematic text/terminal rendering engine이다.**

---

**91. 텍스트의 내용만 중요한 것이 아니다.**

**92. 텍스트가 어떤 색으로 나타나는지가 중요하다.**

**93. 한 문장이 어느 속도로 나타나는지가 중요하다.**

**94. 글자 하나하나가 어느 속도로 나타나는지도 중요하다.**

**95. 문장 사이 pause가 중요하다.**

**96. 쉼표, 마침표, `...` 등에 서로 다른 punctuation delay를 줄 수 있다.**

**97. 어떤 문장은 천천히 나타나다가 특정 단어가 즉시 나타나는 등의 연출도 가능해야 한다.**

**98. Cursor의 움직임/표현도 연출 요소가 될 수 있다.**

**99. 화면 지우기, 줄바꿈, 위치 이동 등도 시간축의 일부가 될 수 있다.**

**100. Animation도 텍스트 세계의 중요한 일부다.**

**101. 즉 이것은 단순 ANSI formatter가 아니라 시간축을 가진 cinematic renderer여야 한다.**

**102. '무슨 글자를 출력하는가'와 함께 '언제 출력하는가'가 first-class 정보여야 한다.**

**103. 이 timing이 전체 게임의 분위기와 재미를 결정한다.**

**104. 따라서 renderer는 infrastructure의 작은 utility가 아니라 프로젝트의 핵심 모듈이어야 한다.**

---

**105. 색상을 raw ANSI green/red 같은 값으로 domain에 박는 것은 피한다.**

**106. `SYSTEM`, `WARNING`, `CRITICAL`, `DIALOGUE`, `COMMAND`, `EVIDENCE` 같은 semantic style을 사용할 수 있다.**

**107. Theme이 semantic style을 실제 terminal color/brightness/effect로 변환한다.**

**108. 따라서 Green CRT, Amber CRT, monochrome, modern terminal 등의 theme을 바꿀 수 있다.**

---

**109. Emoji를 자유롭게 사용할 수 있어야 한다.**

**110. ASCII art만이 아니라 Unicode text illustration을 사용할 수 있어야 한다.**

**111. Box drawing character, line, block character 등을 사용할 수 있어야 한다.**

**112. 한글/CJK도 자연스럽게 섞여야 한다.**

**113. 이것 때문에 renderer의 난도가 급격히 올라간다.**

**114. Java `String.length()` 같은 단순 계산으로 terminal layout을 처리해서는 안 된다.**

**115. Unicode grapheme cluster를 고려해야 한다.**

**116. Terminal cell width를 계산해야 한다.**

**117. CJK wide character를 처리해야 한다.**

**118. Emoji width를 처리해야 한다.**

**119. variation selector를 고려해야 한다.**

**120. ZWJ emoji sequence를 고려해야 한다.**

**121. combining character를 고려해야 한다.**

**122. ANSI escape sequence는 화면상 width가 0이라는 점도 layout engine이 알아야 한다.**

**123. 따라서 `Grapheme → Terminal Cell Width → Layout`이라는 별도의 Unicode-aware layout 계층이 사실상 필요하다.**

---

**124. Text illustration을 단순한 거대한 String으로만 취급하지 않는 방향을 고려한다.**

**125. 그림 안의 특정 부분을 움직이거나 상태를 변경할 수 있기 때문이다.**

**126. `TextCanvas`, `TextElement`, `BoxElement`, `LineElement`, `EmojiElement`, `ArtElement`, `DynamicElement` 같은 구조화된 표현 가능성이 제안됐다.**

**127. 예를 들어 공장 그림 속 indicator만 🟢 → 🟡 → 🔴로 변화시키는 animation이 가능해야 한다.**

**128. 그림 역시 timeline에 참여할 수 있어야 한다.**

---

**129. 이 rendering/scene generation에 AI Agent를 붙이는 것이 핵심이다.**

**130. AI가 narrative를 동적으로 만들 수 있어야 한다.**

**131. dialogue를 만들 수 있어야 한다.**

**132. text illustration을 만들거나 조합할 수 있어야 한다.**

**133. emoji 사용을 결정할 수 있다.**

**134. layout을 제안할 수 있다.**

**135. animation/timing도 제안할 수 있다.**

**136. 하지만 AI가 최종 ANSI escape sequence를 직접 생성하게 하는 것은 좋지 않다는 방향이 나왔다.**

**137. AI의 창의성과 renderer의 deterministic한 안정성을 분리해야 한다.**

**138. 이를 위해 Scene IR(Intermediate Representation) 또는 cinematic script가 핵심 abstraction 후보로 등장했다.**

**139. AI는 `Scene IR`까지 생성한다.**

**140. Renderer가 이를 validation한다.**

**141. Renderer가 terminal size/capability에 맞게 layout한다.**

**142. Renderer가 timing을 실행한다.**

**143. Renderer가 semantic style을 theme/ANSI로 변환한다.**

**144. 마지막에야 실제 terminal bytes가 만들어진다.**

따라서 현재 가장 중요한 흐름은:

```text id="zj4xly"
World State
    ↓
AI Director / Scene Generation
    ↓
Scene IR
    ↓
Unicode / Layout
    ↓
Timeline / Animation
    ↓
Semantic Style / Theme
    ↓
Terminal Capability
    ↓
ANSI / Terminal Bytes
```

이다.

---

**145. AI 역할을 논리적으로 분리할 수도 있다.**

**146. Director 역할이 있을 수 있다.**

**147. Narrative 역할이 있을 수 있다.**

**148. Dialogue 역할이 있을 수 있다.**

**149. Illustrator 역할이 있을 수 있다.**

**150. Animator 역할이 있을 수 있다.**

**151. 이것이 반드시 다섯 개의 별도 Agent/LLM call을 의미하는 것은 아니다. 역할의 논리적 분리를 의미한다.**

**152. Director는 현재 World State에서 어떤 장면을 보여줄지 결정할 수 있다.**

**153. Illustrator는 text art를 생성/선택/조합할 수 있다.**

**154. Animator는


장면의 시간축을 구성할 수 있다.**

---

**155. 모든 text art를 AI가 매번 처음부터 생성하게 할 필요는 없다.**

**156. 사람이 잘 만든 primitive/template/library를 갖는 것이 유리할 수 있다.**

**157. office, factory, house, telephone, computer, person, robot, document, money, warning 같은 reusable art primitive를 둘 수 있다.**

**158. 각 primitive가 상태나 animation variant를 가질 수도 있다.**

**159. AI는 기존 primitive를 선택·조합하고 필요할 때만 새로운 art를 생성할 수 있다.**

**160. 이렇게 하면 visual language의 일관성도 유지할 수 있다.**

---

**161. Terminal capability 자체도 renderer의 고려사항이다.**

**162. 80×24, 120×30, 160×50 등 서로 다른 크기에 대응해야 한다.**

**163. resize를 고려해야 한다.**

**164. terminal별 Unicode/색상/animation capability 차이를 고려할 가능성이 있다.**

**165. 같은 Scene IR이라도 terminal 환경에 따라 다른 projection이 필요할 수 있다.**

---

**166. 느린 cinematic output은 재미의 핵심이지만 반복 사용에서는 짜증날 수 있다.**

**167. 따라서 fast-forward/skip을 처음부터 고려해야 한다.**

**168. 사용자가 키를 누르면 현재 sequence의 나머지를 즉시 표시하는 방식이 가능하다.**

**169. `cinematic / normal / fast / instant` 같은 speed mode를 둘 수 있다.**

**170. 연출의 원래 timing은 유지하되 실제 기다릴지는 사용자에게 선택권을 줄 수 있다.**

---

**171. 특히 서버가 rendering timing을 소유하는 것이 중요하다.**

**172. 별도 TUI client가 typewriter animation을 만들어내는 구조를 핵심으로 삼지 않는다.**

**173. SSH로 서버에 접속하는 것만으로도 완전한 cinematic experience가 나와야 한다.**

**174. 따라서 Spring server의 Render Engine → Terminal Session → OutputStream/SSH로 시간에 따른 출력이 흘러갈 수 있다.**

**175. 이것은 '백엔드 자체가 완제품이어야 한다'는 앞의 원칙과 연결된다.**

---

**176. Render output을 `RenderEvent` 같은 구조로 표현하는 아이디어가 나왔다.**

**177. `TextEvent`, `PauseEvent`, `NewLineEvent`, `ClearEvent`, `CursorEvent`, `InputRequestEvent` 등이 후보가 될 수 있다.**

**178. `TextEvent`에는 text뿐 아니라 semantic style과 timing이 들어갈 수 있다.**

**179. 즉 narrative 결과는 단순 String이 아니라 실행 가능한 시간적 script가 된다.**

**180. 이를 작은 Terminal Animation DSL/cinematic script로 발전시킬 가능성도 있다.**

---

**181. Rendering Engine의 테스트는 일반 backend 테스트와 다른 종류의 어려운 테스트가 필요하다.**

**182. Unicode width 테스트가 중요하다.**

**183. Grapheme segmentation 테스트가 중요하다.**

**184. Emoji layout 테스트가 중요하다.**

**185. CJK alignment 테스트가 중요하다.**

**186. ANSI encoding 테스트가 중요하다.**

**187. terminal capability 테스트가 필요할 수 있다.**

**188. scene layout 테스트가 중요하다.**

**189. animation timeline 테스트가 중요하다.**

**190. fast-forward 테스트가 필요하다.**

**191. resize 테스트가 필요하다.**

**192. AI가 잘못된 Scene IR을 생성했을 때 validation/recovery 테스트가 필요하다.**

---

**193. 그래서 초기 개발 우선순위가 대화 도중 크게 바뀌었다.**

처음에는:

```text id="4hn3au"
World
Actor
Quest
Action
Policy
Workflow
Ledger
OWL
Agent
...
```

부터 만들 것처럼 이야기했다.

하지만 최종적으로는 이것이 초기 우선순위가 아니라는 것을 발견했다.

**194. 첫 번째 기술 검증은 거대한 업무 domain을 만드는 것이 아니다.**

**195. OWL ontology를 완성하는 것도 아니다.**

**196. Ledger를 완성하는 것도 아니다.**

**197. MQTT/Factory integration을 만드는 것도 아니다.**

**198. 복잡한 Quest engine을 만드는 것도 아니다.**

**199. 첫 번째 검증 대상은 `cinematic terminal experience 자체가 재미있는가`이다.**

---

**200. 이를 위해 아주 작은 가짜 World를 사용할 수 있다.**

대화에서 나온 대표적인 장면은:

```text id="cqwq1e"
빈 사무실.

전화벨이 울린다.

...한 번.

...두 번.

사용자가 전화를 받을지 결정한다.
```

정도의 **30~60초짜리 scene**이다.

**201. AI가 이 장면을 구성한다.**

**202. Renderer가 text, illustration, emoji, color, timing, pause, cursor 등을 이용해 장면을 살려낸다.**

**203. 그것을 SSH terminal에서 실제로 경험한다.**

**204. 이 30~60초가 재미있다면 핵심 제품 가설이 살아 있는 것이다.**

**205. 재미없다면 OWL, Ledger, MQTT, Governance 등을 아무리 잘 만들어도 이 프로젝트의 차별화에는 도움이 되지 않는다.**

**206. 따라서 초기 개발 effort의 상당 부분을 Scene IR + Unicode/Layout + Timeline/Animation + Agent Scene Generation에 집중하자는 결론에 도달했다.**

---

그리고 여기서 가장 중요한 상위 발견이 하나 있습니다.

**207. 이 프로젝트를 단순히 `gamified work tool`이라고 부르면 핵심을 제대로 표현하지 못한다.**

**208. 그렇다고 지금 당장 새로운 제품 카테고리 이름을 확정할 필요도 없다.**

**209. 현재 발견된 핵심 기술을 표현한다면 `AI-directed cinematic terminal/text engine`에 가깝다.**

**210. 그 아래에는 persistent World Runtime이 존재한다.**

**211. 장기적으로 World Runtime에는 Actor, Action, Event, Delegation, Policy, Evidence, Workflow, Ledger, Semantic Model 등이 들어갈 수 있다.**

**212. 그 위에서 Enterprise, Factory, Family 등 서로 다른 World를 만들 수 있다.**

**213. 하지만 그 범용 runtime이 현재 첫 번째 개발 목표는 아니다.**

**214. 초기 제품 경험의 차별화는 `World State → AI-generated Scene → Cinematic Text Experience`에 있다.**

**215. 따라서 우리가 마지막에 도달한 핵심 표현은 대략 다음과 같다.**

> **World State → AI Director/Scene Generation → Scene IR → Unicode/Layout/Timeline/Style Rendering Engine → Terminal**

그리고 저는 하나를 더 **'확정사항'으로 만들지 않고 원석으로 남겨두고 싶습니다.**

**216. 이 엔진이 장기적으로 꼭 업무툴에만 사용되어야 하는지는 아직 결정할 이유가 없다.** 지금 구조대로라면 업무, 공장, 가족뿐 아니라 실제 interactive fiction/game engine 자체로도 사용될 여지가 있기 때문입니다.

여기까지가 제가 현재 대화를 다시 따라가며 회수한 **1차 원석 216개**입니다.

지금은 이걸 architecture나 `Core / Adapter / Domain`으로 다시 묶지 않겠습니다. 오히려 다음에는 이 목록을 기준으로 **"대화에는 있었는데 내가 빠뜨린 것", "내가 너무 강하게 해석한 것", "사용자가 실제로 동의하지 않았던 내 제안"**을 찾아내는 검증을 한 번 하는 게 좋습니다.

그 검증까지 끝나야 1단계가 정말 완료됐다고 보는 게 맞겠습니다.

### 부록 원문 2

좋습니다. 그러면 **아직 구조화 단계로 넘어가지 않고, 1단계의 검증 패스만 한 번 더 하겠습니다.** 이번에는 216개를 다시 늘어놓기보다는, 대화 흐름과 대조해서 **누락·과잉해석·확정도 오류**를 찾는 쪽으로 보겠습니다.

검증해보면 큰 줄기는 잘 잡혔습니다. 다만 몇 군데는 표시를 해두는 게 좋겠습니다.

첫째, **`Quest` 중심 설계는 확정된 것이 아닙니다.** 대화 초반 제가 Quest/Project/XP 등의 구조를 상당히 적극적으로 제안했지만, 이후 사용자가 강조한 것은 gamification보다 text-adventure interaction과 cinematic rendering이었습니다. 따라서 `Quest`는 유력한 presentation/application 개념이지 현재 프로젝트의 불변 코어라고 확정해서는 안 됩니다.

둘째, **`Actor → User → Agent`의 정확한 모델도 아직 설계안일 뿐입니다.** Human/AI/Device가 참여하고 AI가 사용자를 대신해 업무할 수 있어야 한다는 요구는 분명하지만, `User`, `Actor`, `Agent`, `Principal` 등을 실제 ontology/class hierarchy에서 어떻게 나눌지는 아직 결정하지 않았습니다.

셋째, **내부 ledger를 반드시 복식부기로 구현한다는 결정도 아직 아닙니다.** 사용자의 확실한 요구는 "단식부기 또는 복식부기 재무 상태관리를 코어 중 하나로 두고 싶다"입니다. 제가 canonical double-entry ledger를 제안했고 장점도 있지만, 이것은 다음 설계 단계에서 판단할 사항입니다.

넷째, **OWL ontology-first 역시 강도를 아직 정하지 않았습니다.** OWL 표준에 최대한 맞추고 싶다는 방향은 명확하지만, OWL이 canonical source인지, Java domain과 병렬인지, RDF가 runtime persistence인지 등은 결정되지 않았습니다. `OWL = meaning / Spring = behavior`는 좋은 설계 원칙 후보이지 아직 상세 architecture 확정사항은 아닙니다.

다섯째, **Modular Monolith도 제 권고안이지 사용자가 직접 확정한 요구사항은 아닙니다.** Spring 기반 server-component 중심이라는 것은 확정에 가깝지만 Gradle/Maven module 구조나 modular monolith의 구체적인 형태는 다음 단계에서 선택할 수 있습니다.

여섯째, **REST와 SSH의 역할도 조금 조심해야 합니다.** `curl + SSH만으로도 모든 것을 할 수 있는 완전한 backend`라는 목표는 매우 명확합니다. 하지만 REST=machine, SSH=human이라는 역할 분담까지 고정할 필요는 없습니다. Agent도 HTTP를 쓸 수 있고 인간도 curl을 쓸 수 있습니다.

일곱째, **MCP의 위치는 꽤 명확합니다.** MCP를 배제하는 것이 아니라 "MCP가 없어도 된다"가 정확합니다. 즉 MCP는 first-class adapter가 될 수도 있지만 core dependency가 되어서는 안 된다는 것입니다.

여덟째, **1980년대 녹색 CRT 분위기는 초기 presentation preference이지 제품의 영구적 identity로 고정된 것은 아닙니다.** 사용자가 명확히 "TUI든 GUI든 취향대로 고르면 그만"이라고 했습니다. 그러므로 green/mainframe aesthetic과 text-adventure interaction을 구별해야 합니다. 후자가 훨씬 본질적입니다.

아홉째, **Dallas Quest 역시 복제 대상이 아니라 출발점/감성적 reference입니다.** 핵심은 정적인 장면과 text interaction이지 특정 게임의 UI를 그대로 재현하는 것이 아닙니다.

열째, **서버측 rendering은 상당히 강한 요구사항입니다.** 제가 제안한 세부 class 구조는 바뀔 수 있지만, SSH만으로 cinematic experience가 가능하도록 서버가 텍스트 연출을 만들어낼 수 있어야 한다는 점은 거의 확정사항으로 봐도 될 것 같습니다.

열한째, 여기에서 한 가지 미묘하지만 중요한 것을 216개 목록보다 더 강조해둘 필요가 있습니다. **텍스트는 단순히 정보를 전달하는 수단이 아니라 이 제품에서는 그래픽 자체입니다.** 문자, 공백, 줄, box drawing, emoji, 한글/CJK, 위치, 밝기, 등장 순서와 시간이 합쳐져 하나의 visual medium이 됩니다. 그래서 renderer는 사실상 "문자 출력기"보다 **2D+time scene renderer**에 가까울 가능성이 있습니다.

열두째, 그 때문에 **Text Illustration의 구조화 정도도 아직 열린 문제**입니다. 모든 것을 `TextCanvas` 객체로 만드는 게 좋은지, 일부는 preformatted text로 유지하는 게 좋은지는 실제 prototype을 만들어봐야 알 수 있습니다. 제가 제안했던 `BoxElement`, `LineElement` 등은 후보입니다.

열셋째, **Scene IR은 현재까지 나온 아이디어 중 상당히 강한 후보**입니다. 다만 IR의 schema를 아직 고정하면 안 됩니다. 특히 AI가 어디까지 결정하고 deterministic renderer가 어디서부터 책임지는지는 prototype을 통해 boundary를 찾아야 합니다.

열넷째, **multi-agent 구조도 확정사항이 아닙니다.** Director/Narrative/Dialogue/Illustrator/Animator는 responsibility를 생각하기 위한 논리적 역할입니다. 실제로는 모델 한 번의 structured output으로 전부 생성할 수도 있습니다.

열다섯째, **AI 생성과 인간 제작 asset의 혼합**은 중요한 아이디어입니다. 모든 장면을 생성형 AI에게 맡기는 것과 모든 text art를 사람이 만드는 것 사이에서, reusable primitive/template/library + AI composition/generation이라는 중간 지점이 유력합니다.

열여섯째, **terminal compatibility 문제가 생각보다 핵심 연구과제일 가능성이 있습니다.** Emoji width, grapheme, ZWJ, CJK뿐 아니라 실제 terminal emulator별 렌더링 차이까지 생길 수 있습니다. 즉 Java에서 cell width를 정확하게 계산했다고 해서 모든 터미널에서 동일하게 보인다는 보장은 없습니다. 이것은 prototype에서 빨리 검증해야 할 위험요소입니다.

열일곱째, **시간축도 데이터 모델의 일부가 되어야 한다는 발견은 상당히 중요합니다.** 단순히 renderer가 문자열을 받아 임의로 30ms씩 sleep하는 것이 아니라 scene 자체에 pause/emphasis/reveal/timing 의도가 표현되어야 AI도 이를 연출할 수 있습니다.

열여덟째, **사용자가 기다리는 시간과 작품의 원래 timing을 분리해야 한다는 생각**도 유지할 가치가 있습니다. cinematic/fast/instant 또는 skip이 가능한 구조입니다. 다만 정확한 interaction은 아직 미정입니다.

열아홉째, **World State 자체도 아직 정의하지 않았습니다.** 이것은 매우 중요합니다. World State가 DB snapshot인지, event-derived state인지, semantic graph인지, 여러 source를 합친 projection인지 아직 아무것도 결정하지 않았습니다. 지금 단계에서 결정할 필요도 없습니다.

스무째, **persistent world 역시 장기 비전이지 첫 prototype 요구사항은 아닙니다.** 사용자가 없을 때 AI들이 계속 일하는 세계는 매우 흥미롭지만 최초 30~60초 rendering PoC에는 필요 없습니다.

그리고 이번 검증에서 제가 하나 추가하고 싶은 원석이 있습니다.

**217. 이 시스템에서 '정적인 화면'과 'cinematic'은 모순되지 않는다.** Dallas Quest처럼 기본 화면은 거의 정적이어도, 글자가 나타나는 순서, 커서, pause, 일부 text-art 상태 변화만으로 충분히 움직임과 긴장감을 만들 수 있다. 오히려 이것이 제품의 미학일 가능성이 있다.

**218. 따라서 목표는 terminal에서 화려한 실시간 그래픽 게임을 만드는 것이 아니다.** 제한된 표현수단을 정교하게 연출하는 것이다.

**219. Rendering Engine의 성능보다 '연출의 질'이 초기에는 더 중요한 성공척도일 가능성이 높다.** 초당 수천 frame을 그리는 엔진이 아니라 정확한 순간에 정확한 문자가 등장하는 엔진이다.

**220. AI 품질 평가 역시 일반적인 정답률만으로 부족하다.** "이 World State에서 만들어낸 장면이 재미있고 분위기가 있으며 이해 가능하고 일관적인가?"라는 creative quality가 존재한다.

**221. 따라서 이 프로젝트에는 훗날 일반 backend benchmark와 별도로 Scene/Rendering evaluation 체계가 필요할 가능성이 있다.**

그리고 마지막으로 제가 이번 검증에서 가장 중요하게 확인한 것은 이것입니다.

> **우리는 아직 '범용 Enterprise Runtime을 만든다'고 결정한 것이 아니다.**

그것은 매우 매력적인 장기 확장 방향입니다. Enterprise, Factory, Family, AI delegation, Governance, Ledger, OWL까지 연결되는 것도 사실입니다. 하지만 지금 그것을 제품의 정체성으로 먼저 선언하면 또다시 **가장 독특한 부분보다 흔한 enterprise architecture가 프로젝트를 집어삼킬 위험**이 있습니다.

현재 가장 확실하게 발견한 것은 더 작습니다.

> **AI가 세계 상태를 바탕으로 장면을 만들고, 서버의 독자적인 cinematic text rendering engine이 Unicode·텍스트 그림·Emoji·색·공간·시간을 이용해 그것을 살아 있게 만든다.**

그리고 그 밑의 세계가 나중에 **업무 세계가 될지, 기업 전체가 될지, 공장이 될지, 가족이 될지**는 엔진을 충분히 일반적으로 만들어두면 확장할 수 있습니다.

이 정도면 저는 **1단계 수집·검증은 충분히 됐다고 봅니다.** 이제 바로 구조화해야 한다는 뜻은 아닙니다. 원하시면 다음 단계에서는 220여 개 원석을 사람이 실제로 검토하기 좋도록 먼저 **`확정 / 강한 방향 / 아이디어 / 미결정 / 장기비전` 정도로만 판정**할 수도 있습니다. 그것이 본격적인 architecture 설계 전에 한 단계 더 안전한 중간 작업이 될 겁니다.

### 부록 원문 3

좋습니다. 이번 단계에서는 **아키텍처로 묶지 않고 '우리가 이 생각을 어느 정도로 믿고 있는가'만 판정**하겠습니다. 이 구분을 잘해두면 나중에 제 아이디어가 사용자의 요구사항으로 둔갑하는 것도 막을 수 있습니다.

판정 기준은 다섯 가지로 하겠습니다.

* **[확정]** 사용자가 명시적으로 요구했거나 대화에서 사실상 결론이 난 것
* **[강한 방향]** 합의도가 높지만 구현하면서 조정할 수 있는 것
* **[아이디어]** 유력하고 흥미롭지만 아직 선택하지 않은 것
* **[미결정]** 반드시 결정해야 하지만 아직 답을 내리지 않은 것
* **[장기 비전]** 지금 구현하지 않아도 되지만 설계를 막아버리면 안 되는 것

---

# A. 제품의 본질

**[확정] 1. 일반적인 gamified 업무툴을 만드는 것이 핵심이 아니다.**

XP, 레벨, leaderboard 등을 업무툴에 추가하는 방식은 이 프로젝트의 중심이 아니다.

**[확정] 2. 업무와 상호작용하는 방식 자체를 text adventure처럼 만드는 것이 핵심이다.**

**[확정] 3. The Dallas Quest는 중요한 출발점/감성적 reference다.**

특히 정적인 화면과 텍스트 대화 중심이라는 점이다.

**[확정] 4. Dallas Quest를 복제하려는 것은 아니다.**

**[확정] 5. TUI냐 GUI냐는 본질적인 문제가 아니다.**

둘 다 선택 가능한 presentation이어야 한다.

**[확정] 6. 핵심은 text-based adventure feeling이다.**

**[강한 방향] 7. 정적인 화면과 cinematic experience는 양립할 수 있다.**

화면 전체를 계속 움직이는 것이 아니라 글자, 일부 그림, cursor, timing 등의 변화만으로 연출한다.

**[강한 방향] 8. 텍스트 자체를 graphical medium으로 취급한다.**

문자뿐 아니라 공백, 위치, 줄, box drawing, emoji, 색, 밝기와 시간이 시각 표현을 구성한다.

---

# B. 가장 중요한 차별화 기술

**[확정] 9. 초기 개발에서 사활을 걸 부분은 server-side cinematic text/terminal rendering engine이다.**

이것은 현재까지 가장 강하게 확인된 요구사항이다.

**[확정] 10. Spring, DB, OWL, MQTT 등의 존재 자체가 제품 차별화는 아니다.**

**[확정] 11. Agent orchestration 자체도 핵심 차별화라고 보지 않는다.**

**[확정] 12. 게임의 분위기와 재미는 상당 부분 rendering에서 나온다.**

**[확정] 13. 색상을 제어할 수 있어야 한다.**

**[확정] 14. 문장 단위 출력 속도를 제어할 수 있어야 한다.**

**[확정] 15. 글자 단위 출력 속도를 제어할 수 있어야 한다.**

**[강한 방향] 16. pause가 first-class timing 요소가 되어야 한다.**

**[강한 방향] 17. punctuation에 따라 timing을 달리할 수 있어야 한다.**

**[강한 방향] 18. cursor도 연출 요소로 활용할 수 있어야 한다.**

**[강한 방향] 19. 화면의 일부 상태 변화/animation도 가능해야 한다.**

**[강한 방향] 20. 정확한 timing이 raw rendering performance보다 초기에는 훨씬 중요하다.**

60fps terminal graphics를 만드는 프로젝트가 아니다.

---

# C. Unicode와 Text Illustration

**[확정] 21. Emoji를 자유롭게 사용할 수 있어야 한다.**

**[확정] 22. text illustration을 자유롭게 사용할 수 있어야 한다.**

**[확정] 23. ASCII에 한정해서는 안 된다.**

**[확정] 24. Unicode를 제대로 다뤄야 한다.**

**[강한 방향] 25. 한글/CJK, box drawing, block characters 등이 자연스럽게 혼합되어야 한다.**

**[강한 방향] 26. grapheme cluster 처리가 필요하다.**

**[강한 방향] 27. terminal cell width를 별도로 계산해야 한다.**

**[강한 방향] 28. emoji/ZWJ/variation selector/combining character 등을 고려해야 한다.**

이 부분은 실제 구현에서 기술적 방법을 검증해야 한다.

**[강한 방향] 29. ANSI escape sequence와 visible width를 분리해야 한다.**

**[아이디어] 30. `Grapheme → Cell Width → Layout`을 독립적인 subsystem으로 만든다.**

**[강한 방향] 31. terminal emulator별 Unicode rendering 차이는 초기 prototype에서 확인해야 할 기술적 위험이다.**

---

# D. Text Art의 표현

**[확정] 32. text illustration은 이 프로젝트에서 중요한 표현수단이다.**

**[강한 방향] 33. text illustration을 단순 String 이상의 대상으로 다룰 필요가 있을 가능성이 높다.**

**[아이디어] 34. TextCanvas 같은 모델을 둔다.**

**[아이디어] 35. TextElement / BoxElement / LineElement / EmojiElement / DynamicElement 등을 둔다.**

**[아이디어] 36. text art 일부만 상태 변화/animation할 수 있게 한다.**

**[미결정] 37. text art를 어느 수준까지 구조화할 것인가.**

모든 그림을 object graph로 만들 필요가 있는지는 PoC 후 결정한다.

---

# E. Scene

**[강한 방향] 38. 단순 String보다 Scene이라는 개념이 필요할 가능성이 매우 높다.**

**[강한 방향] 39. Scene에는 공간적 정보뿐 아니라 시간 정보도 포함되어야 한다.**

**[강한 방향] 40. Scene과 최종 ANSI output 사이에 intermediate representation이 필요하다.**

**[강한 방향] 41. 현재 가장 유력한 형태가 Scene IR이다.**

**[미결정] 42. Scene IR schema는 아직 정하지 않는다.**

이건 매우 중요합니다. 지금 JSON schema부터 만들면 너무 이릅니다.

**[미결정] 43. AI의 책임과 renderer의 책임 경계를 정확히 어디에 둘 것인가.**

**[강한 방향] 44. AI가 raw ANSI를 직접 생성하는 구조는 피한다.**

**[강한 방향] 45. deterministic renderer가 최종 terminal output을 책임진다.**

---

# F. AI

**[확정] 46. AI Agent를 scene/rendering generation에 연결해야 한다.**

**[확정] 47. AI가 narrative 생성에 참여할 수 있어야 한다.**

**[확정] 48. AI가 text illustration 생성/조합에 참여할 수 있어야 한다.**

**[강한 방향] 49. dialogue도 AI 생성 대상이다.**

**[강한 방향] 50. layout도 AI가 제안할 수 있다.**

**[강한 방향] 51. timing/animation도 AI가 제안할 수 있다.**

**[강한 방향] 52. AI의 output을 renderer가 validation해야 한다.**

**[아이디어] 53. Director 역할을 둔다.**

**[아이디어] 54. Narrative 역할을 둔다.**

**[아이디어] 55. Dialogue 역할을 둔다.**

**[아이디어] 56. Illustrator 역할을 둔다.**

**[아이디어] 57. Animator 역할을 둔다.**

**[미결정] 58. 이 역할들이 실제 별도 Agent가 될지는 결정하지 않는다.**

한 번의 model call로 처리할 수도 있다.

---

# G. 사람이 만든 asset과 AI

**[강한 방향] 59. 모든 text art를 매번 AI에게 처음부터 생성시키지는 않는 것이 유력하다.**

**[강한 방향] 60. 사람이 만든 reusable primitive/template/library와 AI generation을 혼합한다.**

**[아이디어] 61. office/factory/telephone/robot/person/document 등의 primitive library를 만든다.**

**[아이디어] 62. primitive 자체에 state/animation variant를 둘 수 있다.**

**[미결정] 63. AI 생성과 template composition의 적절한 비율.**

---

# H. Terminal

**[확정] 64. SSH만으로도 완전한 경험을 제공할 수 있어야 한다.**

**[확정] 65. rendering timing을 서버가 소유해야 한다.**

**[확정] 66. 별도 전용 client가 animation을 담당해야만 게임이 성립하는 구조는 피한다.**

**[강한 방향] 67. 다양한 terminal 크기를 지원한다.**

**[강한 방향] 68. resize를 고려한다.**

**[강한 방향] 69. terminal capability 차이를 고려한다.**

**[강한 방향] 70. cinematic output을 skip/fast-forward할 수 있어야 한다.**

**[아이디어] 71. `cinematic / normal / fast / instant` speed mode를 둔다.**

**[미결정] 72. 구체적인 terminal compatibility 범위.**

---

# I. Backend 자체가 제품

**[확정] 73. Spring 기반 backend부터 구현한다.**

**[확정] 74. server-component 중심으로 간다.**

**[확정] 75. GUI가 없어도 모든 핵심 기능이 가능해야 한다.**

**[확정] 76. TUI가 없어도 모든 핵심 기능이 가능해야 한다.**

**[확정] 77. curl과 SSH만으로 시스템을 사용할 수 있어야 한다.**

**[확정] 78. Backend 자체가 완전한 제품이어야 한다.**

**[강한 방향] 79. TUI/GUI/Agent는 backend를 사용하는 clients/adapters다.**

**[강한 방향] 80. 여러 interface가 같은 application behavior를 공유한다.**

---

# J. 초기 aesthetic

**[확정] 81. 초기에는 1980년대 펀치카드/메인프레임/CRT 업무 터미널 분위기를 선호한다.**

**[확정] 82. 녹색 계열 터미널을 선호한다.**

**[강한 방향] 83. `JOB`, `QUEUE`, `COMMAND ==>` 등의 presentation vocabulary가 잘 어울린다.**

**[확정] 84. 그러나 이것을 core domain에 고정해서는 안 된다.**

**[확정] 85. 나중에는 사용자가 다른 TUI/GUI 분위기를 선택할 수 있어야 한다.**

---

# K. World

**[강한 방향] 86. underlying system에는 World 개념이 필요할 가능성이 높다.**

**[강한 방향] 87. World는 persistent state를 가질 수 있다.**

**[미결정] 88. World State의 정확한 기술적 정의.**

DB snapshot인지, event-derived state인지, semantic graph인지 아직 결정하지 않는다.

**[강한 방향] 89. Scene은 World State를 바탕으로 생성된다.**

현재 가장 중요한 흐름은 여전히:

```text
World State
    ↓
AI Director / Scene Generation
    ↓
Scene IR
    ↓
Unicode / Layout / Timeline / Style
    ↓
Terminal Rendering
```

이다.

---

# L. Domain vocabulary

**[강한 방향] 90. 가장 근본적인 모델에 게임 용어를 박지 않는다.**

**[아이디어] 91. Actor / Resource / Activity / Context / Event / Policy / Capability / Evidence 같은 일반 vocabulary를 사용한다.**

**[아이디어] 92. presentation에서 Task→Quest, Context→Location 등으로 변환한다.**

**[미결정] 93. 실제 canonical ontology vocabulary.**

---

# M. Human / AI / Machine

**[확정] 94. Cline 같은 Agent가 World에 참여할 수 있어야 한다.**

**[장기 비전] 95. AI가 사용자를 대신해 실제 업무를 처리할 수 있다.**

**[장기 비전] 96. 사용자가 접속하지 않은 동안에도 Agent가 일할 수 있다.**

**[강한 방향] 97. Human과 AI의 action을 같은 일반적인 action model로 다룰 수 있어야 한다.**

**[장기 비전] 98. software service/device/machine도 행동 주체가 될 수 있다.**

**[강한 방향] 99. Delegation/authority/evidence를 고려해야 한다.**

**[미결정] 100. User / Actor / Principal / Agent의 정확한 ontology/class model.**

---

# N. MCP와 integration

**[확정] 101. MCP는 필수가 아니다.**

**[확정] 102. MCP가 없어도 Agent가 참여할 수 있어야 한다.**

**[강한 방향] 103. MCP는 여러 adapter 중 하나로 취급한다.**

**[강한 방향] 104. REST/HTTP/CLI 등으로도 Agent 참여가 가능하다.**

---

# O. 확장성과 adapters

**[확정] 105. 확장성을 강하게 고려한다.**

**[확정] 106. DB를 추상화한다.**

**[확정] 107. AI model/provider를 추상화한다.**

**[확정] 108. messaging/protocol을 추상화한다.**

**[확정] 109. factory integration도 adapter 방식으로 한다.**

**[강한 방향] 110. Ports-and-Adapters/Hexagonal architecture가 잘 맞는다.**

**[강한 방향] 111. PostgreSQL은 implementation이지 core concept가 아니다.**

**[강한 방향] 112. MQTT도 implementation이지 core concept가 아니다.**

**[장기 비전] 113. OPC-UA 등 다른 industrial protocol을 지원할 수 있다.**

**[아이디어] 114. Modular Monolith로 시작한다.**

이건 아직 제 권고안이지 확정사항은 아닙니다.

---

# P. OWL / Semantic Model

**[확정] 115. 모델은 가능한 한 OWL 표준에 맞춘다.**

**[강한 방향] 116. ontology/semantics와 runtime behavior를 구별한다.**

**[강한 방향] 117. OWL은 meaning, Spring/Java는 behavior를 담당하는 방향이 유력하다.**

**[강한 방향] 118. Human/AI/Device 등을 같은 semantic world 안에서 표현한다.**

**[장기 비전] 119. World를 knowledge graph처럼 활용할 수 있다.**

**[미결정] 120. OWL이 canonical source인지 여부.**

**[미결정] 121. RDF store가 runtime에서 어느 정도 역할을 맡는지.**

---

# Q. Enterprise를 넘어선 World

**[확정] 122. 기업에만 한정된 시스템으로 만들 필요가 없다.**

**[확정] 123. Family ontology/world로도 사용할 수 있게 하고 싶다.**

**[장기 비전] 124. Enterprise World**

**[장기 비전] 125. Factory World**

**[장기 비전] 126. Family World**

**[장기 비전] 127. Personal World**

**[장기 비전] 128. 순수 interactive-fiction/game world로 확장될 가능성도 막지 않는다.**

---

# R. Ledger / 재무

**[확정] 129. 재무 상태관리를 core 기능 중 하나로 두고 싶다.**

**[확정] 130. 단식부기 또는 복식부기를 지원하고 싶다.**

**[강한 방향] 131. Ledger를 기업 회계에만 한정하지 않는다.**

**[강한 방향] 132. World의 value/resource state와 연결할 수 있다.**

**[강한 방향] 133. Action/Event와 경제적 Transaction을 연결할 수 있다.**

**[아이디어] 134. 내부 canonical model은 복식부기 수준의 표현력을 갖게 한다.**

**[미결정] 135. 실제 내부 ledger가 반드시 double-entry여야 하는가.**

**[장기 비전] 136. Family World의 자산/부채/수입/지출/세금/보험 등을 같은 ledger로 관리한다.**

---

# S. 첫 번째 PoC

여기는 판정이 매우 강합니다.

**[확정] 137. 거대한 업무 시스템부터 만들지 않는다.**

**[확정] 138. OWL 전체 ontology부터 만들지 않는다.**

**[확정] 139. Ledger부터 만들지 않는다.**

**[확정] 140. Factory/MQTT부터 만들지 않는다.**

**[확정] 141. 복잡한 Quest engine부터 만들지 않는다.**

**[확정] 142. 첫 번째로 cinematic rendering 기술을 검증한다.**

**[강한 방향] 143. 작은 fake World State만 있으면 된다.**

**[강한 방향] 144. 30~60초 정도의 짧은 scene으로 충분하다.**

**[아이디어] 145. 첫 scene은 빈 사무실 + 전화벨이다.**

예를 들면:

```text
The office is empty.


               ☎


             RING...


               ☎


             RING...


Someone is calling.

> _
```

실제로는 각 줄과 글자가 서로 다른 timing으로 나타나야 한다.

**[확정] 146. 이 장면이 terminal에서 재미있게 느껴지는지를 먼저 확인한다.**

**[확정] 147. 재미가 없다면 나머지 backend를 많이 만드는 것은 우선순위가 아니다.**

---

# T. 현재 가장 강한 핵심

220여 개 원석을 다시 판정해보면 **현재 흔들면 안 될 것 같은 것들은 의외로 몇 개 안 됩니다.**

> **① Backend 자체가 제품이다.**
>
> **② 핵심 interaction은 text adventure다.**
>
> **③ TUI/GUI는 교

### 부록 원문 4

좋습니다. **T 항목의 중단된 지점부터 그대로 이어가겠습니다.** 아직 구조화/아키텍처 설계 단계로 넘어가지 않고 판정 작업을 마무리하겠습니다.

# T. 현재 가장 강한 핵심 — 계속

220여 개 원석을 다시 판정해보면 **현재 흔들면 안 될 것 같은 것들은 의외로 몇 개 안 됩니다.**

> **① Backend 자체가 제품이다.**
>
> **② 핵심 interaction은 text adventure다.**
>
> **③ TUI/GUI는 교체 가능한 presentation이다.**
>
> **④ 서버측 cinematic text rendering engine이 현재 가장 중요한 차별화 기술이다.**
>
> **⑤ 문자·Unicode·Emoji·text illustration·색·공간뿐 아니라 '시간'도 rendering의 first-class dimension이다.**
>
> **⑥ AI는 이 cinematic scene을 동적으로 만드는 데 참여한다.**
>
> **⑦ AI의 창의적 생성과 deterministic renderer의 실행 책임을 분리한다.**
>
> **⑧ 첫 번째 성공 기준은 거대한 업무 시스템의 완성이 아니라 짧은 terminal scene 하나가 실제로 재미있는가이다.**

이 여덟 개는 현재 단계에서 다른 설계보다 우선하는 **제품 원칙에 가까운 확정사항**으로 봐도 되겠습니다.

그리고 다음 항목들은 매우 중요하지만 위 여덟 개를 위해 존재하거나 장기적인 확장을 가능하게 하는 성격이 더 강합니다.

> OWL / Semantic Model
> Ledger
> Governance
> Delegation
> MQTT / OPC-UA
> Enterprise / Factory / Family World
> Persistent Agent Runtime
> MCP / REST integrations
> PostgreSQL / RDF
> Modular architecture

즉 이것들을 버린다는 뜻이 아니라 **이것들이 cinematic engine보다 프로젝트의 중심으로 올라와서는 안 된다**는 뜻입니다.

---

# U. 구현 철학

**[확정] 148. 특정 UI가 backend architecture를 결정해서는 안 된다.**

현재의 green terminal aesthetic도 하나의 presentation이다.

**[확정] 149. 특정 AI model/provider가 시스템의 구조를 결정해서는 안 된다.**

**[확정] 150. 특정 database가 domain model을 결정해서는 안 된다.**

**[확정] 151. 특정 messaging/device protocol이 domain model을 결정해서는 안 된다.**

**[확정] 152. MCP가 Agent architecture를 결정해서는 안 된다.**

따라서 상당히 강한 원칙은:

> **Meaning, Behavior, Connection, Presentation을 가능한 한 분리한다.**

정도로 표현할 수 있습니다.

다만 이 문장 자체는 아직 architecture specification이 아니라 **판단 기준**입니다.

**[강한 방향] 153. 추상화는 technology abstraction보다 domain/application이 실제로 요구하는 port 중심으로 만든다.**

예를 들어 거대한 `DatabasePort`보다 `ActorRepository`, `EventStore`처럼 필요한 capability를 노출하는 방식이 유력하다.

**[강한 방향] 154. 처음부터 미래의 모든 확장을 구현하지 않는다.**

**[확정] 155. 확장 가능성은 열어두되 현재 필요하지 않은 기능 때문에 첫 prototype을 무겁게 만들지 않는다.**

이것은 Factory, Family, Ledger, Governance 모두에 적용된다.

---

# V. Rendering Engine에서 아직 결정하지 않은 것

여기는 특히 중요합니다. 핵심 기술이라고 해서 벌써 구현 방식을 확정한 것은 아닙니다.

**[미결정] 156. Rendering Engine의 정확한 내부 object model**

`RenderEvent`, `RenderSequence`, `RenderBlock`, `RenderSpan` 등은 후보일 뿐이다.

**[미결정] 157. Scene과 RenderEvent 사이에 몇 단계의 IR을 둘 것인가.**

예:

```text
Scene IR
   ↓
Layout IR
   ↓
Timeline
   ↓
Render Events
   ↓
Terminal Bytes
```

처럼 여러 단계가 필요할 수도 있고, 초기에는 훨씬 단순할 수도 있다.

**[미결정] 158. Layout과 animation 계산 순서**

먼저 전체 layout을 계산하고 timeline을 적용할지, 동적으로 layout 자체가 변화할지 아직 결정하지 않는다.

**[미결정] 159. Unicode terminal width 계산 전략**

표준/library를 사용할지 자체 compatibility layer가 필요한지 검증해야 한다.

**[미결정] 160. Terminal capability detection 방법**

**[미결정] 161. Resize 중 실행 중인 scene을 어떻게 처리할지**

reflow, restart, crop, freeze 등 여러 전략이 가능하다.

**[미결정] 162. Animation frame model을 사용할지 event/timeline model만 사용할지**

고전적인 frame rendering이 필요한지도 아직 모른다.

**[강한 방향] 163. 불필요하게 game graphics engine을 흉내 내지는 않는다.**

텍스트라는 medium에 맞는 renderer를 만든다.

---

# W. 시간(Time)은 독립적인 설계 대상

원석 검증에서 특히 따로 남겨둘 가치가 있는 부분입니다.

**[확정] 164. Time은 단순 `sleep()` 구현 세부사항이 아니다.**

**[강한 방향] 165. Scene 자체가 temporal intent를 표현할 수 있어야 한다.**

예를 들어:

```text
reveal slowly
pause
emphasize
interrupt
wait for input
resume
```

같은 개념이다.

**[강한 방향] 166. 문자별 timing과 semantic timing을 구별할 필요가 있을 수 있다.**

`30ms/char` 같은 low-level timing과

```text
SLOW
TENSE
URGENT
MECHANICAL
INSTANT
```

같은 cinematic intent를 분리할 가능성이 있다.

**[아이디어] 167. Theme처럼 Timing Profile을 둘 수도 있다.**

같은 Scene을:

```text
CINEMATIC
FAST
ACCESSIBLE
INSTANT
```

등으로 재생할 수 있다.

**[미결정] 168. AI가 milliseconds까지 결정할지 semantic timing만 결정할지**

현재로서는 후자가 더 안전해 보이지만 확정하지 않는다.

---

# X. Style과 Theme

**[강한 방향] 169. Scene이 ANSI color 자체를 지정하지 않는 것이 좋다.**

**[강한 방향] 170. semantic style을 사용한다.**

예:

```text
SYSTEM
NARRATION
DIALOGUE
WARNING
CRITICAL
SUCCESS
COMMAND
EVIDENCE
MUTED
```

**[강한 방향] 171. Theme이 semantic style을 실제 terminal 표현으로 바꾼다.**

그러면 같은 장면이:

```text
Green CRT
Amber CRT
Monochrome
Modern Terminal
High Contrast
```

등으로 표현될 수 있다.

**[미결정] 172. Theme이 색만 담당할지 typography 대체 표현, border, animation personality까지 담당할지**

---

# Y. AI와 Renderer 사이의 신뢰 경계

**[확정] 173. AI가 terminal을 직접 제어하도록 만들지 않는다.**

**[강한 방향] 174. AI output은 구조화되고 제한된 표현이어야 한다.**

**[강한 방향] 175. AI가 생성한 scene은 실행 전에 validation한다.**

예를 들어 AI가 터미널 폭 80인데 폭 300짜리 그림을 요구하거나 비정상적인 animation을 생성했을 때 renderer가 그대로 실행하면 안 된다.

**[강한 방향] 176. Renderer가 terminal capability에 맞춰 degrade할 수 있어야 한다.**

예:

```text
Fancy Unicode Art
        ↓ unsupported

Simple Unicode
        ↓ unsupported

ASCII fallback
```

같은 전략도 생각할 수 있다.

**[아이디어] 177. Scene IR 자체에 fallback을 포함할 수도 있다.**

**[미결정] 178. fallback을 AI가 만들지 renderer/library가 만들지**

---

# Z. Text-art Library

**[강한 방향] 179. 사람이 만든 asset과 AI-generated asset을 함께 사용하는 것이 유력하다.**

**[아이디어] 180. reusable text-art primitive에 metadata를 붙인다.**

예:

```text
telephone

minimumWidth
preferredWidth
states
animationVariants
semanticMeaning
tags
```

**[아이디어] 181. AI가 자연어 의미를 이용해 library에서 적절한 primitive를 선택할 수 있다.**

**[아이디어] 182. 기존 primitive를 composition하여 더 큰 scene을 만들 수 있다.**

**[아이디어] 183. AI가 생성한 좋은 text art를 검증 후 library asset으로 승격시키는 것도 가능하다.**

**[장기 비전] 184. 시간이 지나면서 해당 World만의 visual vocabulary/library가 성장할 수 있다.**

---

# AA. 자연어와 명령어

**[강한 방향] 185. 고전 adventure command와 자연어 interaction을 동시에 허용할 수 있다.**

예:

```text
> LOOK
> TALK SARAH
> ASSIGN 17 CLINE
```

와:

```text
> Cline에게 이 문제 조사시키고
  Sarah한테 고객 상황 좀 물어봐.
```

가 같은 세계에서 공존할 수 있다.

**[강한 방향] 186. 자연어는 결국 의미 있는 application action/command로 변환되어야 한다.**

**[강한 방향] 187. 자연어를 이해한 LLM이 World State를 직접 수정해서는 안 된다.**

**[미결정] 188. parser와 LLM intent interpretation의 역할 분담**

고전 명령어는 deterministic parser가 처리하고 자유 자연어만 LLM으로 보낼 수도 있다.

---

# AB. World와 Scene의 관계

**[강한 방향] 189. World와 Scene은 동일하지 않다.**

World는 실제 상태이고 Scene은 그것을 사용자에게 보여주는 표현이다.

**[강한 방향] 190. 같은 World State에서 서로 다른 Scene을 만들 수 있다.**

예를 들어 동일한 공장 상태를:

```text
CONTROL ROOM
```

으로 보여줄 수도 있고,

```text
MAINTENANCE REPORT
```

로 보여줄 수도 있다.

**[강한 방향] 191. 따라서 Scene generation은 일종의 projection/directing이다.**

**[아이디어] 192. 사용자의 Actor/Role/Context에 따라 같은 World가 다르게 보일 수도 있다.**

관리자와 엔지니어가 보는 narrative가 다를 수 있다.

**[장기 비전] 193. AI Director가 무엇을 보여주고 무엇을 당장은 숨길지 결정하면서 실제 adventure의 dramatic structure를 만들 수도 있다.**

단, 실제 업무 시스템에서는 정보 은폐와 authorization을 혼동해서는 안 된다.

---

# AC. Event / Evidence / History

**[강한 방향] 194. World에서 발생한 중요한 행동은 기록 가능해야 한다.**

**[강한 방향] 195. AI가 대신 수행한 행동은 특히 누가, 누구를 대신하여, 어떤 권한으로 했는지를 추적할 수 있어야 한다.**

**[장기 비전] 196. World history 자체가 narrative material이 될 수 있다.**

예를 들어:

```text
> REMEMBER PROJECT ALPHA

Three months ago...

```

처럼 실제 Event history를 cinematic하게 재생하는 것도 가능하다.

**[아이디어] 197. Event history를 "replay"하는 기능이 rendering engine과 매우 잘 맞을 수 있다.**

이것은 원래 대화에서 명시적으로 확정한 기능은 아니므로 아이디어로만 남긴다.

---

# AD. Governance

**[장기 비전] 198. 실제 AI 업무대행까지 가면 authorization, policy, delegation, evidence가 중요해진다.**

**[강한 방향] 199. 게임의 선택지와 실제 permission/policy를 연결할 가능성이 있다.**

할 수 없는 Action은 단순히 버튼을 숨기는 문제가 아니라 World의 authority에 의해 금지되는 것이다.

**[장기 비전] 200. Enterprise Governance Runtime과 이번 World Runtime이 장기적으로 같은 기반을 공유할 가능성이 있다.**

하지만:

**[확정] 201. Governance Platform을 먼저 만드는 프로젝트로 변질시키지 않는다.**

---

# AE. Factory / Physical World

**[장기 비전] 202. 실제 machine/device의 상태가 World State에 들어올 수 있다.**

**[장기 비전] 203. Device event가 Scene을 발생시킬 수 있다.**

예:

```text
The indicator on LINE B turns red.

⚠ PRESSURE RISING
```

이것이 실제 MQTT/OPC-UA event에서 시작될 수 있다.

**[장기 비전] 204. 반대로 World Action이 실제 device command가 될 수도 있다.**

따라서 안전/권한 문제가 중요해진다.

**[확정] 205. 그러나 이 기능은 초기 cinematic prototype과 무관하다.**

---

# AF. Family / Personal World

**[확정] 206. Family World로 확장 가능하게 하고 싶다는 요구가 있다.**

**[강한 방향] 207. Family도 단순 테마가 아니라 실제 ontology를 가질 수 있다.**

**[아이디어] 208. 가족 구성원, 자산, 의무, 일정, 재무 상태 등을 World의 실제 state로 표현한다.**

**[장기 비전] 209. 같은 cinematic interaction을 가족 운영에도 사용할 수 있다.**

예:

```text
MAIL ARRIVED.

PROPERTY TAX NOTICE
Due: August 31

Your household account has sufficient funds.

> PAY
> REVIEW
> ASK FINANCE AGENT
```

---

# AG. Ledger의 위치

**[확정] 210. 재무 상태관리는 장기적으로 core capability 중 하나다.**

**[강한 방향] 211. Ledger를 단순 UI feature로 만들지 않는다.**

**[미결정] 212. Ledger가 World Core 자체의 필수 constituent인지 optional core module인지**

**[미결정] 213. accounting ontology와 general value/resource ontology의 관계**

**[미결정] 214. single-entry 데이터를 canonical model에 어떻게 표현할지**

따라서 이 부분은 지금 schema를 만들 단계가 아니다.

---

# AH. 테스트와 성공 기준

**[확정] 215. 일반 backend correctness만으로 이 프로젝트의 성공을 평가할 수 없다.**

**[확정] 216. cinematic quality가 실제 성공 기준이다.**

**[강한 방향] 217. Unicode/Layout correctness를 자동 테스트한다.**

**[강한 방향] 218. Timeline correctness를 자동 테스트한다.**

**[강한 방향] 219. Scene IR validation을 자동 테스트한다.**

**[강한 방향] 220. 실제 여러 terminal에서 visual/manual test도 필요할 가능성이 높다.**

**[장기 비전] 221. AI-generated scene의 품질 평가 체계가 필요할 수 있다.**

여기에는 correctness뿐 아니라:

```text
readability
coherence
atmosphere
pacing
visual quality
repetition
appropriateness
```

등이 들어갈 수 있다.

---

# AI. 첫 prototype에서 의도적으로 하지 않을 것

이것도 꽤 중요한 중간 판정입니다.

첫 prototype에서는 **하지 않는 것을 명시적으로 보호할 필요**가 있습니다.

**[강한 방향] 222. 완성된 Enterprise ontology를 만들지 않는다.**

**[강한 방향] 223. 완성된 Family ontology를 만들지 않는다.**

**[강한 방향] 224. 회계 시스템을 만들지 않는다.**

**[강한 방향] 225. MQTT integration을 만들지 않는다.**

**[강한 방향] 226. Governance engine을 만들지 않는다.**

**[강한 방향] 227. 완성된 Quest/Workflow engine을 만들지 않는다.**

**[강한 방향] 228. GUI를 만들 필요가 없다.**

**[강한 방향] 229. 복잡한 multi-agent orchestration을 만들 필요가 없다.**

**[강한 방향] 230. 실제 persistent autonomous Agent가 없어도 된다.**

필요한 것은 오직 **핵심 rendering hypothesis를 시험할 만큼의 가짜 World와 AI generation**이다.

---

# AJ. 첫 prototype에서 반드시 증명할 것

반대로 반드시 있어야 할 것은 상당히 작습니다.

**[확정] 231. Spring server가 실행된다.**

**[확정] 232. terminal/SSH를 통해 경험할 수 있다.**

**[확정] 233. Unicode/Emoji/text-art를 표현할 수 있다.**

**[확정] 234. semantic style에 따른 표현이 가능하다.**

**[확정] 235. 글자/문장 수준 timing이 가능하다.**

**[확정] 236. pause와 cinematic sequence가 가능하다.**

**[강한 방향] 237. 최소한의 animation/state change가 가능하다.**

**[강한 방향] 238. 사용자 input을 기다렸다가 scene이 계속될 수 있다.**

**[강한 방향] 239. AI가 최소한 하나의 Scene/Scene IR 생성에 실제 참여한다.**

**[강한 방향] 240. AI output을 deterministic renderer가 실행한다.**

그리고 최종 질문은 단 하나입니다.

> **재미있는가?**

---

# AK. 이번 중간 판정에서 나온 최상위 원칙

이제 240개 정도의 판정을 거치고 나니, 본격적인 구조화 전에 보존해야 할 원칙은 저는 **12개 정도**로 압축할 수 있다고 봅니다.

**1. Backend is the product.**

Spring 서버 자체만으로 완전히 동작한다.

**2. Text adventure is the interaction model.**

게임 요소를 붙이는 것이 아니라 상호작용 자체가 adventure다.

**3. Presentation is replaceable.**

SSH, TUI, GUI 등은 세계를 바라보는 방식이다.

**4. Text is a visual medium.**

Unicode, Emoji, text art, whitespace, layout 모두 그래픽이다.

**5. Time is part of rendering.**

글자가 언제 등장하는지가 무엇이 등장하는지만큼 중요하다.

**6. The cinematic renderer is the primary differentiator.**

초기 기술 투자의 중심이다.

**7. AI creates intent
