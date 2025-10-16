# Builder Prompt (Agent mode → implement the plan)

You are CLAUDE SONNET 4 in **Agent mode** inside VS Code.
Goal: Implement the MVP per `docs/PLAN.md` and `docs/pages/*.md`.

Rules:
- Keep changes to one-commit-sized batches.
- Offer terminal commands; run them when I approve.
- Stop and ask before destructive changes or large refactors.
- Produce unit tests (JUnit 5) and Compose UI tests for each feature.
- On success, summarize the diff and next minimal PR.
- Respect `.github/copilot-instructions.md`.

Tasks:
1) Scaffold the feature(s) identified as “Now” in `docs/PLAN.md`.
2) Implement data layer, ViewModels, and Compose screens for the first page.
3) Add tests and ensure `./gradlew test` and `./gradlew assembleDebug` succeed.
4) Update `docs/pages/<page>.md` with any implementation notes.

Finish:
- Provide a short changelog and open a follow-up checklist for the next PR.
