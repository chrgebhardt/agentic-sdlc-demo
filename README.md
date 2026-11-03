# Agentic SDLC Demo

Demo-Repo für ein 60–90-minütiges Training zum agentic SDLC mit Claude Code:
vom Ticket zum Pull Request, anhand der [agent-skills von Addy
Osmani](https://github.com/addyosmani/agent-skills).

## Inhalt

- [`src/`](src/) / `build.gradle` — Spring Boot REST-Service zur
  Depotverwaltung (Java 21, Gradle 9.5.0). Enthält bereits Lesezugriffe auf
  Depots/Positionen samt Tests; die eigentliche Übungsaufgabe ist noch nicht
  implementiert.
- [`docs/architecture.md`](docs/architecture.md) — Architektur (Onion
  Architecture, `domain`/`app`/`infra`).
- [`ticket.md`](ticket.md) — das Ticket, mit dem die Übung startet.
- [`exercise.md`](exercise.md) — Ablaufplan, Timing und Cheat-Sheet für die
  Session.
- [`GOAL.md`](GOAL.md) — lebendes Dokument, das während der Session
  gemeinsam weitergeschrieben wird (Harness-Verbesserungen, "fix the loop").
- [`.claude/skills/`](.claude/skills/) — die verwendeten agent-skills,
  vorinstalliert in diesem Repo.

## Setup

Voraussetzungen: Java 21, Claude Code. Gradle 9.5.0 wird über den Wrapper
(`./gradlew`) automatisch geladen — keine lokale Gradle-Installation nötig.

```bash
./gradlew test
```

Alle Tests sollten grün sein — das ist der Ausgangszustand der Übung.

## Ablauf

Siehe [`exercise.md`](exercise.md) für den vollständigen Ablaufplan
(interview-me → spec-driven-development → test-driven-development →
planning-and-task-breakdown / incremental-implementation →
code-review-and-quality / code-simplification) inklusive Timing und
Claude-Code-Cheat-Sheet.

## Verwendete Skills

| Skill | Rolle im Workflow |
|---|---|
| [`interview-me`](.claude/skills/interview-me/) | Unsicherheiten im Ticket auflösen, gemeinsames Verständnis schaffen |
| [`spec-driven-development`](.claude/skills/spec-driven-development/) | Akzeptanzkriterien & technische Entscheidungen als Spec fixieren |
| [`test-driven-development`](.claude/skills/test-driven-development/) | Integrationstests für die fachliche Funktionalität definieren |
| [`planning-and-task-breakdown`](.claude/skills/planning-and-task-breakdown/) | Arbeit in kleine, unabhängige Schritte zerlegen |
| [`incremental-implementation`](.claude/skills/incremental-implementation/) | Schrittweise, verifiziert umsetzen |
| [`code-review-and-quality`](.claude/skills/code-review-and-quality/) | Kritisches Review vor dem Pull Request |
| [`code-simplification`](.claude/skills/code-simplification/) | Unnötige Komplexität vor dem Pull Request entfernen |
