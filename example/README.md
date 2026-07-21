# Code Review Agent Suite

This example provides a local, requirements-driven code review tool for four language targets:

- TypeScript
- Java
- Rust
- React, including JavaScript, JSX, TypeScript, and TSX files

Each agent combines deterministic pattern checks with requirement coverage analysis. An optional local Deepseek model can help filter requirements for the selected language. Every run produces a Markdown report with findings, suggested changes, and a verdict.

## Prerequisites

- Bash
- Node.js 18 or newer
- Optional: Ollama with `deepseek-coder:1.3b-base`

The deterministic checks have no npm dependencies and do not require internet access.

## Quick Start

Run all four review agents from the repository root:

```bash
./run-review.sh
```

Run one agent:

```bash
LANGUAGE=java ./run-review.sh
LANGUAGE=typescript ./run-review.sh
LANGUAGE=rust ./run-review.sh
LANGUAGE=react ./run-review.sh
```

Force deterministic checks without the optional local model:

```bash
DEEPSEEK=false ./run-review.sh
```

## Configuration

Configuration is supplied through environment variables before the command.

| Variable | Values | Default | Purpose |
| --- | --- | --- | --- |
| `LANGUAGE` | `all`, `java`, `typescript`, `rust`, `react` | `all` | Selects the agents to run. |
| `DEEPSEEK` | `true`, `false` | `true` | Attempts to use a locally available Deepseek model through Ollama. The tool falls back to deterministic filtering when it is unavailable. |
| `INTERNET` | `true`, `false` | `false` | Records the requested access mode in generated reports. The current agents do not make internet requests. |

Example with multiple settings:

```bash
LANGUAGE=typescript DEEPSEEK=false INTERNET=false ./run-review.sh
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

The agents use regex and file-based heuristics. They do not replace compilers, linters, dependency scanners, test runners, or a human review.

## Project Layout

```text
.
|-- run-review.sh          Review orchestrator
|-- review/
|   |-- agents/            Shared base class and four language agents
|   `-- results/           Generated reports
|-- requirements/          Review requirements
`-- documentation/         Browser-based review documentation
```

## Documentation

Open [`documentation/index.html`](documentation/index.html) in a browser for the complete guide:

- [Overview](documentation/review-overview.html)
- [Usage instructions](documentation/review-instructions.html)
- [Architecture](documentation/review-architecture.html)
- [TypeScript agent](documentation/review-agent-typescript.html)
- [Java agent](documentation/review-agent-java.html)
- [Rust agent](documentation/review-agent-rust.html)
- [React agent](documentation/review-agent-react.html)
- [Development process overview](documentation/process.html)
- [PDCA delivery cycle](documentation/process-pdca.html)
- [Governance and quality gates](documentation/process-governance.html)
- [Milestones and operations](documentation/process-milestones.html)
