# Project Notes

This repository is for a backend-first product implemented with Spring Boot and
Java.

The initial product surface is not a web GUI. The first milestone is an
embedded SSH experience: a user connects to the Spring Boot server through SSH
and sees a server-controlled cinematic Unicode/text intro scene. The goal is to
prove that the backend itself can feel like a complete product surface before
building a broader platform.

This repository should also become a repeatable backend baseline and manual
deployment runbook. A fresh clone should eventually be able to create the
Spring Boot baseline, run the SSH intro prototype locally, validate it through
Docker, and then follow the same conservative pattern for `dev-demo` and
`aws-demo`.

The first experience proof is more important than the number of features. The
question for the first milestone is:

> Do users want to stay in this terminal world?

## Product Direction

- Backend is the product. The Spring Boot server should be usable as a product
  surface through SSH and simple terminal clients, without requiring a GUI.
- Text adventure is the interaction model. Do not treat XP, levels, quests, or
  game vocabulary as decorative gamification on top of a normal business tool.
- Presentation is replaceable. SSH is the first presentation adapter, not the
  permanent core architecture. Future CLI/TUI, GUI, mobile, or HTTP streaming
  adapters should remain possible.
- Text is a visual medium. Unicode, CJK, emoji, box drawing, whitespace,
  alignment, silence, timing, cursor movement, and layout are part of the
  product experience.
- Time is part of rendering. Delay, reveal speed, punctuation timing, pauses,
  interruption, animation, and cursor behavior matter.
- The cinematic renderer is the first differentiator. Spring, DB, OWL, MQTT,
  MCP, and agent orchestration are not the initial differentiator.
- AI may create scene intent later, but deterministic renderer code must control
  terminal output.
- Scene IR should separate creativity from execution, but the final schema must
  not be frozen before the prototype teaches us what it needs.
- The world model should be more fundamental than game vocabulary. Avoid
  prematurely making `Quest`, `Location`, `Character`, or `Item` the core model.
- External technologies are adapter candidates. DB, LLM providers, MQTT,
  OPC-UA, HTTP, MCP, agents, and devices should not become mandatory core
  dependencies early.

## First Milestone Direction

- Build the smallest Spring Boot application that can host an embedded SSH
  server and play a cinematic intro scene on connection.
- Use the first milestone to validate mood, presence, timing, text layout, and
  terminal feel.
- Start with a fake world and a hardcoded intro scene.
- Keep the first intro in the 30-60 second range.
- After the intro, show a minimal `> _` prompt so the world feels like it can
  continue.
- The first milestone should work without a database, ontology, external AI
  provider, MQTT, MCP, workflow engine, ledger, or GUI.
- Prefer a small, inspectable renderer over a broad feature set.
- Keep renderer core separate from the SSH adapter so other presentation
  adapters can be added later.
- Treat the first SceneModel as prototype data, not a final Scene IR contract.

## Development Direction

- Prefer simple, explicit Spring Boot code over framework-heavy abstractions.
- Keep runtime configuration external to the application image.
- Treat local development, local Docker image creation, and remote deployment
  as separate steps.
- The default local workflow must not require a local database.
- Runtime hosts such as `aws-demo` and `dev-demo` should not manage Java
  dependencies directly.
- Docker images should contain the built application artifact and runtime, not
  local development files or secrets.
- Prefer a conventional Java build tool and project layout once the skeleton is
  created. Do not introduce multiple build systems.
- Use Apache MINA SSHD as the preferred initial embedded SSH server candidate
  unless the implementation discovers a concrete reason to choose otherwise.
- ANSI escape sequence generation should be centralized in renderer or terminal
  helper code. Do not scatter raw terminal control strings through unrelated
  application logic.
- Unicode width and layout handling may start simple, but the code structure
  should leave room for CJK, emoji, and box drawing correctness.
- Blocking timing logic is acceptable for the first single-scene prototype, but
  sessions must clean up correctly when a client disconnects or interrupts.
- Do not introduce a database, persistence layer, ontology, AI provider, or
  message broker before the SSH intro milestone requires it.
- Prefer standard logging first. Add heavier observability frameworks only when
  the application has a concrete need for them.

## Deployment Rules

- Deployment is manual only.
- Deployment must be performed with shell scripts only.
- Runtime deployment must use Docker.
- Docker images are built locally only.
- Do not build Docker images from this repository working tree.
- For Docker builds, clone the repository into a separate build location and
  build from that clean clone.
- The local build clone root is `~/hchjeong/deploy_remote_repo`.
- Shell scripts running from WSL should refer to that build clone root as
  `~/hchjeong/deploy_remote_repo`.
- A separate local clone may also be used for source preparation, local Docker
  image builds, migrations, or seed commands.
- Do not use a persistent local Spring Boot process as the normal deployment
  validation path; validate the running application on `dev-demo` through
  Docker when deployment scripts exist.
- Export the built image to a `.tar` file locally.
- Transfer the image tarball to the target host, such as `aws-demo` or
  `dev-demo`.
- Load and run the image on the target host with explicit Docker commands or
  shell scripts.
- Keep deployment scripts target-specific. Use separate scripts for `aws-demo`
  and `dev-demo`, or separate target wrappers around shared conservative logic.
- Prepare target env files before deploying the app.
- `aws-demo` scripts must preserve persistent state.
- `dev-demo` scripts may include development-only reset or recreate workflows,
  but they must never reuse `aws-demo` paths, credentials, volumes, or SSH host
  aliases.
- If a database is added later, the application container and database
  container must have separate lifecycles. Frequent application redeploys must
  not recreate or reset the database container.

## Secrets And Runtime Files

- Never include environment variable files in Docker images.
- Never include credentials or secret files in Docker images.
- Keep persistent runtime files outside containers unless there is a deliberate
  reason and it is documented.
- Keep local development data outside this repository and outside Docker images.
- On `aws-demo`, create dedicated root-level directories for service-owned
  runtime files, databases, uploads, logs, and similar persistent data if they
  become necessary.
- On `dev-demo`, use separate dedicated directories from `aws-demo`, even when
  the directory layout is intentionally similar.
- Mount required runtime files or directories into containers at run time.
- `aws-demo` env setup scripts may create directories and template files, but
  must not generate or overwrite production-like secrets.
- `dev-demo` env setup scripts may generate development-only credentials, but
  must preserve existing env files.
- SSH host keys, passwords, demo credentials, and private keys must not be
  committed to the repository.

## Operational Assumptions

- The production-like target is the `aws-demo` instance.
- The development server target is the `dev-demo` instance.
- `dev-demo` currently maps to the SSH alias `yoga` on the trusted LAN at
  `hchjeong@192.168.0.104`.
- The first milestone is local-first, then Docker-validated, then remote-demo
  capable.
- The application may eventually communicate with sibling applications,
  services, agents, or machines through adapters, but none of those integrations
  are required for the first milestone.
- Scripts should make paths, image names, container names, ports, volumes, and
  env-file locations easy to inspect before execution.
- If the embedded SSH server is exposed beyond localhost, host binding,
  credentials, allowed users, and demo safety must be explicit.

## Agent Guidance

- Read `docs/implementation-plan-original.md` for project principles and
  source context before making broad architectural changes.
- Read `docs/implementation-plan.md` before implementing milestone work.
- Do not replace the first milestone with a generic CRUD API, web dashboard, or
  ordinary business backend skeleton.
- Do not add automated deployment systems, CI/CD deployment flows, hosted build
  steps, or registry-based deployment unless explicitly requested.
- Do not assume credentials can be committed, copied into images, or generated
  into the repository.
- When adding scripts, keep them readable and conservative.
- When changing Docker behavior, preserve the local-build, tar-transfer,
  remote-run workflow.
- App deployment scripts must not stop, remove, or recreate a future database
  unless the script is explicitly dedicated to database administration.
- Leave app deployment scripts minimal until the app skeleton, Dockerfile,
  health check strategy, SSH port strategy, and image tag strategy are decided.
- Future app deployment scripts should consume a prebuilt local image tarball,
  load it on the target host, replace only the app container, and verify the
  service.
- Clearly encode the target host in script names or target config files to
  reduce accidental cross-environment deployment.
- Do not prematurely implement OWL, MQTT, MCP, Ledger, governance, workflow, or
  multi-agent orchestration.
- Do not connect an LLM provider for the first milestone unless explicitly
  requested. Use hardcoded scene data to prove the renderer first.
- Do not let AI-generated text directly control raw ANSI output. Renderer code
  must validate and translate scene intent into terminal output.
- Do not scatter raw ANSI strings or terminal cursor logic outside renderer and
  terminal abstraction modules.
- Do not freeze a final Scene IR schema until after at least one SSH intro scene
  has been implemented and evaluated.
- Prioritize the feel of the SSH intro over feature count. If the scene does
  not feel compelling, improve writing, timing, spacing, color, silence, and
  prompt behavior before adding platform features.

## Reference Documents

- `docs/implementation-plan-original.md` preserves the original project
  principles, including the appendix of early source notes.
- `docs/implementation-plan.md` defines the milestone roadmap and is the current
  implementation plan source of truth.
