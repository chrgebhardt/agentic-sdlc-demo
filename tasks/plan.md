# Implementation Plan: TICKET-142 — Kauforder aufgeben

## Overview

Neuer Endpoint `POST /api/depots/{depotId}/orders`, der eine Kauforder sofort
zum statischen `Wertpapier`-Preis ausführt: Guthaben reduzieren, Position
erhöhen bzw. anlegen, Bestätigung zurückgeben. Stateless — keine neue Entity,
kein neues Repository-Interface.

Quellen:
- Spec: `docs/specs/ticket-142-kauforder.md`
- Ausführbare Akzeptanzkriterien (bereits geschrieben, aktuell rot):
  `src/test/java/de/cofinpro/training/depot/infra/web/OrderControllerIntegrationTest.java`

**Die Tests sind maßgeblich.** Wo Spec und Tests abweichen, gilt der Test
(siehe „Abweichungen Spec ↔ Tests“).

## Ausgangslage

| Datei | Zustand |
|---|---|
| `domain/Depot`, `Position`, `Wertpapier` | vorhanden, Lombok `@Setter` → mutierbar, keine Änderung nötig |
| `infra/persistence/PositionRepository` | hat nur `findByDepotId` — Lookup Depot+Wertpapier fehlt |
| `infra/web/DepotController` | nur GET-Endpoints |
| `infra/web/ApiExceptionHandler` | nur `NoSuchElementException` → 404 |
| `app/DepotQueryService` | Vorlage für Konstruktor-Injection und 404-Meldungen (`"Depot not found: " + id`) |

## Abhängigkeitsgraph

```
PositionRepository.findByDepotIdAndWertpapierId   ApiExceptionHandler: IllegalArgumentException → 400
        │                                                   │
        ▼                                                   │
OrderService.placeOrder  ◄──────────────────────────────────┘ (Validierungen werfen IAE)
        │
        ▼
OrderRequest / OrderResponse  →  POST-Mapping im DepotController
```

## Architecture Decisions

- **Eigener `OrderController`** unter `@RequestMapping("/api/depots")`, passend
  zur Testklasse `OrderControllerIntegrationTest` (entschieden, weicht von Spec
  Open Question 3 ab).
- **`OrderService` in `app/`, `@Transactional`**, Konstruktor-Injection.
  Signatur `placeOrder(Long depotId, Long wertpapierId, BigDecimal quantity)` —
  nimmt keine Web-DTOs entgegen.
- **Rückgabetyp:** `OrderService` liefert den app-Record `OrderConfirmation`;
  `OrderController` bildet ihn auf `OrderResponse` ab. Damit kennt `app` keine
  Web-DTOs (onion-konform, entschieden).
- **Validieren vor Mutieren:** Reihenfolge im Service ist
  1. `quantity > 0` (400) → 2. Depot laden (404) → 3. Wertpapier laden (404)
  → 4. `totalCost` berechnen und Guthaben prüfen (400) → 5. erst dann
  Guthaben abziehen und Position schreiben. Damit sind abgelehnte Orders ohne
  Seiteneffekte, unabhängig vom Rollback.
- **Rundung:** `totalCost = quantity × price`, `setScale(2, RoundingMode.HALF_UP)`
  (Spec Open Question 1; Test `returnsConfirmation` erzwingt 37.525 → 37.53).
- **Fehler-Mapping:** `IllegalArgumentException` → 400 mit `ex.getMessage()`
  als Body, analog zum 404-Handler. Kein Bean Validation (`@Positive`), da
  deren Fehlertext nicht `Quantity must be positive` wäre.

## Abweichungen Spec ↔ Tests

| Thema | Spec | Tests (gilt) |
|---|---|---|
| Testklasse | `DepotControllerIntegrationTest` ergänzen | eigene `OrderControllerIntegrationTest` |
| Testdaten | Demo-Daten aus `DemoDataLoader` | jeder Test legt eigene Daten an, `@Transactional`-Rollback |
| Rundung | offene Frage | festgelegt: HALF_UP auf 2 Stellen |

## Task List

Jeder Task = ein Commit. Ausgangszustand: alle 10 Testfälle in
`OrderControllerIntegrationTest` rot, alle übrigen Tests grün. Jeder Task macht
eine definierte Teilmenge grün und darf keinen bisher grünen Test rot machen.

### Phase 1: Happy Path

- [ ] Task 1: Kauf auf bestehende Position (Walking Skeleton)
- [ ] Task 2: Erstkauf legt neue Position an

### Checkpoint A
- [ ] 4 von 10 Order-Tests grün, `DepotControllerIntegrationTest` grün
- [ ] Manuell: `./gradlew bootRun` + `curl -X POST .../api/depots/1/orders`

### Phase 2: Fehlerfälle

- [ ] Task 3: Unbekanntes Depot / Wertpapier → 404
- [ ] Task 4: Nicht-positive Stückzahl → 400
- [ ] Task 5: Unzureichendes Guthaben → 400

### Checkpoint B (Complete)
- [ ] `./gradlew build` grün (alle Tests)
- [ ] Alle Success Criteria der Spec abgehakt
- [ ] Review mit Mensch

### Phase 3: Abschluss

- [ ] Task 6: Doku nachziehen (`CLAUDE.md`, Spec)

Details zu jedem Task: `tasks/todo.md`.

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Zwischen-Commits haben rote Tests in `OrderControllerIntegrationTest` → `./gradlew build` schlägt fehl | Med | Bewusst akzeptiert (TDD-Übung, Tests sind die Vorgabe). Verifikation pro Task über `--tests`-Filter. Alternativ noch nicht adressierte Tests per `@Disabled` markieren und im jeweiligen Task entfernen. |
| H2/Hibernate-Default für `BigDecimal` ist `numeric(38,2)` — Stückzahlen mit >2 Nachkommastellen würden still gerundet | Low | Für die Tests irrelevant (max. 0.25). Nicht in diesem Ticket lösen, als Folge-Thema notieren. |
| Vergleich `jsonPath(...).value(10)` vs. `15.0` gegen `BigDecimal` mit Scale 2 | Low | Läuft im bestehenden `DepotControllerIntegrationTest` bereits mit `value(10)`; im Task 1 früh verifiziert. |
| Task 3 ist nach Task 1/2 evtl. schon grün (`orElseThrow` aus Task 1) | Low | Dann ist Task 3 nur Verifikation + Prüfung der Prüfreihenfolge; Commit entfällt ggf. oder wird mit Task 1 zusammengelegt. |

## Entschiedene Fragen

1. Onion: eigener app-Record `OrderConfirmation`, Mapping im Controller.
2. Rote Zwischen-Commits sind für die TDD-Übung in Ordnung.
3. Endpoint in eigenem `OrderController`.
