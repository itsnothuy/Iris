ADR 0001: Adopt GitHub Copilot Pro with Claude Sonnet 4 for Planning and Building
Status
Approved

Context
We are embarking on a mission to build an on‑device, multimodal AI assistant for Android (Iris). The project combines a large Kotlin/Android codebase with future components (potentially TypeScript, Python and infrastructure scripts). We need a structured, reliable way to plan complex features, generate code across multiple files, and ensure high quality while accommodating our existing technical architecture (see the Technical Architecture & Engineering Plan). Recent advances in AI tooling—especially Anthropic’s Claude Sonnet 4 model integrated into GitHub Copilot Pro—offer powerful agentic capabilities similar to the workflow described in the “Claude Code turned my planning into reality” article. The article shows a 15‑minute planning prompt that generates multiple Markdown files and a subsequent build prompt that produces a complete codebase, demonstrating the efficiency of an agentic, long‑context assistant.

To replicate and extend this capability in a production environment, we must decide:

Which Copilot plan and model we will use. There are multiple plans (Pro, Pro+, Business, Enterprise) and multiple models (GPT‑4.1/5, Claude Sonnet 3.5/4/4.5). We need deterministic behavior and maximum context, but premium requests come with cost considerations.
How we will integrate Copilot into our development workflow: using Edits and Agent mode in VS Code and/or the Coding Agent on GitHub. We need to choose between direct edits vs. PR‑based workflows and how tasks are chunked.
How to structure planning artefacts: file formats, splitting by page vs. other criteria.
How to record our decisions so that future engineers understand the rationale.
Decision
Plan selection: We will use GitHub Copilot Pro (or equivalent) with the Claude Sonnet 4 model pinned across Chat, Edits, Agent mode, and the Coding Agent. We will not use the Auto model selector. This ensures deterministic outputs and access to Claude’s strong reasoning in long contexts. Although Copilot may offer GPT‑4.1/5 or Sonnet 4.5, we choose Sonnet 4 because it balances reasoning ability, latency, and availability.
Premium request budgeting: We will not set a strict budget limit on premium requests for this project because the schedule is critical and we require consistent performance. We will monitor usage via GitHub billing dashboards and adjust later if necessary.
Workflow: We will employ both Agent mode in VS Code and the Coding Agent on GitHub. Agent mode is used for interactive sessions where we plan and accept or reject changes. The Coding Agent is used for longer‑running tasks that require building, testing, and opening PRs autonomously. All changes must be delivered via PRs to preserve code review and CI gating.
Planning artefacts: We will store planning documents in Markdown under docs/PLAN.md and split detailed specifications into docs/pages/*.md. This page‑by‑page approach allows us to reuse the article’s “bubble away” pattern: planning tasks for each page individually increases the depth and reduces the chance of shallow outputs. We will also adopt the ADR pattern and store architecture decisions in docs/adr/.
copilot-instructions: We will add a comprehensive .github/copilot-instructions.md file defining our repository structure, build tooling, testing standards, secrets management, and Copilot usage guidelines. This ensures that Copilot sessions produce consistent and secure outputs aligned with our architecture.
Alternatives Considered
Use Auto model selection. Auto allows Copilot to pick between GPT‑4.1/5 and Claude Sonnet models based on load and usage quotas. This could reduce cost and rate‑limit issues but sacrifices deterministic behavior and may produce lower reasoning quality. We rejected this because our planning sessions require high consistency and we want to exploit the full context and reasoning power of Sonnet 4.
Use only Agent mode. Sticking with interactive Agent sessions in VS Code would simplify our workflow but would require the developer to keep VS Code open and may limit session length. We require asynchronous, long‑running tasks (e.g., building and testing multiple modules), so we adopted the Coding Agent to offload these tasks into a secure, sandboxed environment on GitHub’s servers.
Single‑file planning. Generating a single, monolithic plan document could simplify maintenance but would lead to unwieldy files and shallow analysis. The article demonstrates that splitting into page‑level specs produces deep, reusable content. We adopted the page‑by‑page approach for better modularity and clarity.
Alternative code generation tools. Tools like Cursor or Replit Ghostwriter offer similar planning/build flows, but we selected Copilot because of its deep integration with GitHub, support for Claude models, and availability within our organisation.
Consequences
We must maintain a paid Copilot plan with sufficient premium request allowance and monitor usage. Sonnet 4 consumes premium requests; running long sessions could incur additional costs.
All developers must configure their editors to pin Claude Sonnet 4 and refer to the .github/copilot-instructions.md. Training may be required to adopt new workflows.
Splitting planning documents into separate files increases the number of files but improves clarity and reusability. It also aligns with the RAG design of our LLM, which benefits from focused documents.
Using PRs for every change will slow down merges but ensures code review, branch protection, and CI compliance.
Recording decisions via ADRs increases documentation overhead but ensures traceability. Future decisions should follow this pattern.
References
Technical Architecture & Engineering Plan – baseline architecture for the Iris project.
Claude Code article – demonstration of long‑context planning and build capabilities.
GitHub Copilot documentation – details on Copilot plans, Agent mode, and the Coding Agent.
 