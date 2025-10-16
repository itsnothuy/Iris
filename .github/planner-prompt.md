# Planner Prompt (Edits → multi-file plan)

You are CLAUDE SONNET 4 running inside GitHub Copilot **Edits**. 
Goal: Propose a single multi-file change that creates a complete planning set:
- Create `docs/INDEX.md` (overview and links).
- Create `docs/PLAN.md` (scope, milestones, risks, testing strategy, CI gates).
- Create one spec file per UI/page/module under `docs/pages/*.md`. Each page file must include:
  1) business goals,
  2) user stories + acceptance tests,
  3) UI states & navigation,
  4) data flow and boundaries,
  5) non-functionals (perf, i18n, accessibility),
  6) test plan (unit/UI/instrumented),
  7) telemetry **not** collected (privacy-first),
  8) merge checklist.

Constraints:
- Keep it “one-commit-sized” but allow many files; I will review diffs in place.
- Obey `.github/copilot-instructions.md`.
- Reference current code via `#workspace` and align to the repo’s packages, Gradle modules, and Compose structure.
- Use Mermaid for sequence diagrams when it clarifies flows.
- Do NOT edit build scripts yet.

Now propose the single multi-file change and show the complete diff preview.
