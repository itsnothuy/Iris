# Repository Custom Instructions for GitHub Copilot
## Production-Ready On-Device AI Assistant Development Guide

### 📋 Table of Contents
1. [Overview & Critical Requirements](#overview--critical-requirements)
2. [Architecture Foundation](#architecture-foundation)
3. [Structure & Conventions](#structure--conventions)
4. [Planning Workflow](#planning-workflow)
5. [Build & Implementation](#build--implementation)
6. [Quality Standards](#quality-standards)
7. [Execution Guards](#execution-guards)
8. [CI/CD Integration](#cicd-integration)
9. [Secrets Management](#secrets-management)
10. [Documentation Guidelines](#documentation-guidelines)

---

## 🚨 Overview & Critical Requirements

These instructions define how **GitHub Copilot Pro** (Chat, Edits, Agent, and Coding Agent) should interact with this repository. They reflect the architectural decisions of our **on-device mission** to build a multimodal AI assistant and provide guardrails for automated planning and code generation.

**Model Configuration**: Always use **Claude Sonnet 4** as the primary model with auto-selection disabled.

### 🏗️ Architecture Foundation

> **⚠️ CRITICAL: Architecture Reference**
> 
> **ALWAYS consult `docs/architecture.md` before making ANY architectural decisions or implementing features.**
> 
> This document contains the complete technical blueprint for `iris_android` including:
> - Module responsibilities and interfaces
> - Data flows and hardware integration
> - API specifications and deployment strategies
> - Safety systems and performance management
> 
> **All code changes must align with the documented architecture.**

---

## 📁 Structure & Conventions
Monorepo layout: This repository contains a primary Android application (iris_android) and supporting modules. Future components (e.g. backend services or web UI) should be added under apps/ or packages/ as appropriate.
Languages: Kotlin (Android), TypeScript (for any web tooling), and Python (scripts/tests). Use Jetpack Compose for UI. Prefer coroutines and Flows for asynchronous work.
Build tools: Use Gradle (Android Gradle Plugin) for the Android app and Gradle Kotlin DSL. For cross‑language builds we considered Bazel, but selected Gradle due to existing integration, faster incremental builds on Android, easier IDE support, and smaller learning curve. Keep the Gradle wrapper up to date. Run ./gradlew assembleDebug for local builds and assembleRelease for CI.
Testing: Use JUnit 5 and Kotlin test DSL for unit tests. For UI testing, use Jetpack Compose’s androidx.ui.test APIs and Espresso for legacy views. Aim for 80 % coverage on critical modules and enforce coverage checks in CI. Consider using Robolectric for headless JVM tests to speed up CI.
Documentation: All planning artefacts should live under docs/. The comprehensive technical architecture is documented in docs/architecture.md and MUST be referenced for all implementation decisions. The high‑level plan is docs/PLAN.md; page‑level specs live in docs/pages/*.md. ADRs reside under docs/adr/ and follow the naming convention NNNN-title.md with sequential numbers. Use Markdown as the default format; embed diagrams using Mermaid when helpful.
Commit style: Follow Conventional Commits (feat:, fix:, docs:). Keep changes scoped to one feature or bug per commit. When Copilot generates a change set, ensure each PR is concise enough to be reviewed in one sitting.
Secrets & credentials: Never commit secrets. Use GitHub Actions OIDC for cloud credentials and encrypted repository variables for tokens. Do not hard‑code API keys or tokens. When you need secrets in tests, mock them or inject via environment variables.
Planning Workflow
Initial Planning
Use Copilot Edits in VS Code to create or update the global plan at docs/PLAN.md and per‑page specs under docs/pages/. When planning, reference existing code, docs/architecture.md, and these instructions. Include goals, user stories, UI states, data flow diagrams, risks, acceptance tests, and cross‑links. All implementations must follow the module architecture and interfaces defined in docs/architecture.md.
When splitting pages, follow a functional breakdown. Each page spec should be self‑contained and reference relevant modules. Avoid shallow outlines; provide detailed tasks down to component names and data contracts.
ADR Creation
Every significant architectural or tool decision must be captured as an ADR. Use the template from docs/adr/0001-use-copilot-sonnet4.md as a starting point. Include context, decision, alternatives considered, consequences, and citations to relevant research.
New ADR files should be created in sequential order (e.g. 0002-choice-of-build-tool.md) and linked from the plan.
Build & Implementation Workflow
Multi‑file changes
Copilot Edits and Agent mode are allowed to propose changes across multiple files. Prefer grouping related changes into a single PR limited to ~300 LOC to ease code review. Avoid mixing refactors with new features unless necessary.
Always run ./gradlew lint test before concluding an edit session. Tests must pass locally before opening a PR.
Agent mode vs. Coding Agent
Agent mode is used for interactive tasks inside VS Code. It can run commands in your terminal when permitted and can apply changes directly. Use it for rapid iterations.
Coding Agent (GitHub bot) should be used for hands‑off builds. It clones the repo, runs builds and tests in a sandbox, and raises a draft PR when ready. Use it for large or long‑running tasks. Provide clear goals via GitHub Issues and ensure those issues are assigned to the Copilot coding agent.
Always opt for PR‑based workflows. Never push to protected branches directly. Each PR should include a summary of changes and test results.
Quality Standards
Testing: All code must include unit tests with JUnit 5. UI code must include Compose UI tests. Achieve at least 80 % line coverage on modules with business logic. Write e2e tests only for critical flows.
Static analysis: Use ktlint or detekt for Kotlin code and eslint for JavaScript/TypeScript code. Fix all warnings before merging.
Security: Code must not access the network without user consent. All AI inference must remain on device. Validate inputs for tool calls and sanitise strings to prevent injection.
Performance: On‑device AI inference must respect thermal and energy budgets. Follow the guidelines from docs/architecture.md when implementing inference pipelines. All performance optimizations must align with the adaptive performance architecture documented therein.
Execution Guards for Copilot
Use pinned model: Always select Claude Sonnet 4 for Chat, Edits, Agent, and Coding Agent sessions unless specified otherwise. Do not use Auto selection.
Context limitations: Because Copilot’s exposed context window is smaller than the 1M token theoretical maximum, break very large codebases or documents into smaller chunks for analysis. Use summary files or README.md pointers to provide context when necessary.
Time limits: Limit each build session (Agent or Coding Agent) to approximately 40 minutes of continuous work. If tasks exceed this limit, break them into smaller issues (e.g., by feature or by page) and run multiple sessions.
Safety checks: Before executing high‑risk tool calls (e.g., sending messages, performing file operations), Copilot must request confirmation from the user. Never run destructive commands without explicit approval.
CI/CD Integration
Use GitHub Actions for CI because it integrates seamlessly with our repository and supports matrix builds across multiple Android API levels. If future cross‑language builds become complex, consider evaluating Bazel or GitLab CI, but for now GitHub Actions offers fast caching, wide ecosystem support, and built‑in OIDC for secrets.
The default workflow runs assembleDebug, lint, and testDebugUnitTest on pull requests. For release builds, add assembleRelease and integration tests. Each workflow must fetch dependencies with the Gradle cache and run on self‑hosted runners or GitHub's macos-latest to build Android.
Set up branch protection requiring at least one approved code review and passing CI before merge. The Copilot Coding Agent cannot bypass branch protection.
Secrets Management
Authentication: Use GitHub Actions OIDC to obtain short‑lived credentials from our cloud provider. This avoids storing long‑lived credentials in secrets.
Secrets: Store sensitive values in GitHub Secrets or encrypted environment variables, not in the repository. Use environment variables in workflows to supply tokens.
Permissions: Follow the principle of least privilege. Grant the GitHub Actions runner minimal scopes necessary to perform tasks (e.g., read/write to GitHub Packages, deploy to Play Store). Avoid personal access tokens.
Documentation & Knowledge Sharing
After completing a major feature or architectural change, update docs/PLAN.md, docs/architecture.md if needed, and create or update ADRs. Keep diagrams up to date. All architectural decisions must be reflected in docs/architecture.md to maintain consistency.
Encourage contributions to documentation. This repository uses Markdown for docs; contributions should follow the existing style, include citations to research when appropriate, and maintain consistent heading structures.
How to use these instructions
Developers and the Copilot agent should read and apply these guidelines for every change. When interacting with Copilot, reference the relevant sections (e.g., planning, ADR creation, testing) and ALWAYS check docs/architecture.md to ensure the suggestions align with our architecture and quality requirements.

 