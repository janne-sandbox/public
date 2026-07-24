# Code Review Agent Suite

This example provides a requirements-driven code review tool for six language targets:

- TypeScript
- Java
- Rust
- React, including JavaScript, JSX, TypeScript, and TSX files
- C and C++
- C#

Each agent combines deterministic pattern checks with requirement coverage analysis. Optional Deepseek analysis runs offline through Ollama or online through the hosted Deepseek API. The C++ and C# agents also send bounded code samples to the selected Deepseek provider for semantic review. Every run produces a Markdown report with findings, suggested changes, provider status, and a verdict.

## Prerequisites

- Bash
- Node.js 18 or newer
- Optional for offline Deepseek: Ollama with `deepseek-coder:1.3b-base`
- Optional for online Deepseek: a `DEEPSEEK_API_KEY`

The deterministic checks have no npm dependencies and do not require internet access.

## Tests

Run provider routing, language checks, and all seven sample-application review
tests:

```bash
npm test
```

## Quick Start

Run all six review agents from the repository root:

```bash
./run-review.sh
```

Run one agent:

```bash
LANGUAGE=java ./run-review.sh
LANGUAGE=typescript ./run-review.sh
LANGUAGE=rust ./run-review.sh
LANGUAGE=react ./run-review.sh
LANGUAGE=cpp ./run-review.sh
LANGUAGE=csharp ./run-review.sh
```

Force deterministic checks without the optional local model:

```bash
DEEPSEEK=false ./run-review.sh
```

## Configuration

Configuration is supplied through environment variables before the command.

| Variable | Values | Default | Purpose |
| --- | --- | --- | --- |
| `LANGUAGE` | `all`, `java`, `typescript`, `rust`, `react`, `cpp`, `csharp` | `all` | Selects the agents to run. Aliases: `c++`/`cxx` and `c#`/`cs`. |
| `DEEPSEEK` | `true`, `false` | `true` | Enables optional Deepseek requirement filtering and semantic C++/C# review. Unavailable providers fall back to deterministic checks. |
| `INTERNET` | `true`, `false` | `false` | Selects the Deepseek provider: local Ollama when false, hosted Deepseek API when true. |
| `DEEPSEEK_OLLAMA_MODEL` | Ollama model name | `deepseek-coder:1.3b-base` | Overrides the offline model. |
| `DEEPSEEK_ONLINE_MODEL` | Deepseek API model name | `deepseek-v4-flash` | Overrides the hosted model. |

Offline Deepseek never sends source code outside the workstation:

```bash
ollama pull deepseek-coder:1.3b-base
ollama serve
LANGUAGE=cpp DEEPSEEK=true INTERNET=false ./run-review.sh
```

Online Deepseek requires an API key and sends sampled source code and requirement headings to the hosted API:

```bash
export DEEPSEEK_API_KEY='your-key'
LANGUAGE=csharp DEEPSEEK=true INTERNET=true ./run-review.sh
```

Force deterministic-only review in either mode:

```bash
LANGUAGE=all DEEPSEEK=false INTERNET=false ./run-review.sh
```

## Reports

Each run creates a timestamped directory:

```text
review/results/YYYYMMDDHHMM/
```

If a directory for the same minute already exists, the runner adds a numeric suffix. Report names contain the language, verdict, and timestamp:

```text
review-typescript-approved-with-changes-YYYYMMDDHHMM.md
```

The possible verdicts are:

| Verdict | Meaning |
| --- | --- |
| `APPROVED` | No critical or major findings or suggested changes were produced. |
| `APPROVED WITH CHANGES` | At least one major finding or suggested change was produced. |
| `DENIED` | At least one critical finding was produced. |

Minor findings are advisory and appear in the report without blocking approval.

## Requirements

Requirements are Markdown files loaded from:

```text
requirements/
|-- functional/
`-- non-functional/
```

The `functional` directory is optional. Add project requirements there when using this example as a starting point. Requirement file names become the identifiers displayed in reports.

The agents filter requirements by language, then compare selected requirement checkpoints with the scanned source. Keep requirements concrete and use headings, lists, and code blocks for checks that should be visible to the reviewer.

## Agent Coverage

| Agent | Primary checks |
| --- | --- |
| TypeScript | Strict compiler mode, explicit type safety, test-file ratio, requirement coverage |
| Java | Modern Java patterns, Jackson usage, SQL query safety, test-file ratio, requirement coverage |
| Rust | Unsafe blocks, error handling, async patterns, test-file ratio, requirement coverage |
| React | Accessibility, browser security, component size, lazy loading, project organization, requirement coverage |
| C++ | Unsafe C functions, raw ownership, C-style casts, tests, semantic Deepseek review |
| C# | Async blocking, SQL construction, nullable context, tests, semantic Deepseek review |

The agents use regex and file-based heuristics. They do not replace compilers, linters, dependency scanners, test runners, or a human review.

## Sample Applications

The [`sample-apps/`](sample-apps/) directory contains C++, C#, Angular
TypeScript, browser JavaScript, Java, Rust, and React applications. Each
contains one documented, intentional review issue. The integration suite runs
the matching agent against each application and verifies that its report
contains the expected finding.

These fixtures intentionally demonstrate unsafe or discouraged patterns. Do
not copy those patterns into production code.

## Project Layout

```text
.
|-- run-review.sh          Review orchestrator
|-- review/
|   |-- agents/            Shared base class and six language agents
|   |-- tests/             Unit and sample-application integration tests
|   `-- results/           Generated reports
|-- sample-apps/           Seven intentionally reviewable applications
|-- requirements/          Review requirements
`-- documentation/         Browser-based review documentation
```

## Documentation

[Example](https://janne-sandbox.github.io/public/example/documentation/)

Open [`documentation/index.html`](documentation/index.html) in a browser for the complete guide:

- [Overview](documentation/review-overview.html)
- [Usage instructions](documentation/review-instructions.html)
- [Architecture](documentation/review-architecture.html)
- [TypeScript agent](documentation/review-agent-typescript.html)
- [Java agent](documentation/review-agent-java.html)
- [Rust agent](documentation/review-agent-rust.html)
- [React agent](documentation/review-agent-react.html)
- [C++ agent](documentation/review-agent-cpp.html)
- [C# agent](documentation/review-agent-csharp.html)
- [Sample applications](documentation/review-samples.html)
- [AI development overview](documentation/ai-development.html)
- [VS Code setup for macOS, Windows, WSL, Ubuntu, and RHEL](documentation/ai-development-vscode.html)
- [VS Code JSON settings, profiles, shortcuts, and AI permissions](documentation/ai-development-vscode-settings.html)
- [VS Code with Codex](documentation/ai-development-codex.html)
- [VS Code with GitHub Copilot](documentation/ai-development-copilot.html)
- [VS Code with Claude Code](documentation/ai-development-claude.html)
- [Local AI with Ollama](documentation/ai-development-ollama.html)
- [Continue extension for VS Code](documentation/ai-development-continue-vscode.html)
- [AI models, capabilities, roles, and operating modes](documentation/ai-development-models-modes.html)
- [Continue work across chats and tools](documentation/ai-development-continue.html)
- [What Model Context Protocol (MCP) means](documentation/ai-development-mcp.html)
- [What model temperature means](documentation/ai-development-temperature.html)
- [Responsible AI development workflow](documentation/ai-development-workflow.html)
- [Development process overview](documentation/process.html)
- [PDCA delivery cycle](documentation/process-pdca.html)
- [Governance and quality gates](documentation/process-governance.html)
- [Milestones and operations](documentation/process-milestones.html)
