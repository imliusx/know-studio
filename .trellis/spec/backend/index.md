# Backend Development Guidelines

> Best practices for backend development in this project.

---

## Overview

This directory contains guidelines for backend development. Fill in each file with your project's specific conventions.

---

## Guidelines Index

| Guide | Description | Status |
|-------|-------------|--------|
| [Directory Structure](./directory-structure.md) | Module organization and file layout | To fill |
| [Database Guidelines](./database-guidelines.md) | MyBatis-Plus, explicit KnowledgeBase and user isolation, migrations | Active |
| [Error Handling](./error-handling.md) | Error types, handling strategies | To fill |
| [Quality Guidelines](./quality-guidelines.md) | Lombok usage and Maven quality gate | Active |
| [Logging Guidelines](./logging-guidelines.md) | SLF4J, Logback, JSON rolling, traceId | Active |
| [Agent Conversation Guidelines](./agent-conversation-guidelines.md) | Conversation isolation, summary cursors, Agent SSE and MCP contracts | Active |
| [Prompt Engineering Guidelines](./prompt-engineering-guidelines.md) | Scenario prompts, typed model messages, glm-5 routing and safe AI telemetry | Active |
| [Observability and Evaluation](./observability-evaluation-guidelines.md) | Rate limits, reactive tracing, OTLP, Langfuse and retrieval ablation | Active |

---

## How to Fill These Guidelines

For each guideline file:

1. Document your project's **actual conventions** (not ideals)
2. Include **code examples** from your codebase
3. List **forbidden patterns** and why
4. Add **common mistakes** your team has made

The goal is to help AI assistants and new team members understand how YOUR project works.

---

**Language**: All documentation should be written in **English**.
