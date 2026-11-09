# TODO: TICKET-142 — Kauforder aufgeben

Paket-Präfix: `src/main/java/de/cofinpro/training/depot/` (kurz `…/`).
Testklasse: `OrderControllerIntegrationTest` (kurz `OCIT`).
Testfilter: `./gradlew test --tests '*OrderControllerIntegrationTest.<methode>'`

---

## Task 1: Kauf auf bestehende Position (Walking Skeleton)

**Description:** Durchstich durch alle Schichten für den Kernfall: Request
annehmen, Depot und Wertpapier laden, `totalCost` (HALF_UP, 2 Stellen)
berechnen, Guthaben reduzieren, Stückzahl der bestehenden Position erhöhen,
Bestätigung zurückgeben. Fehlende Position oder Fehlerfälle werden hier noch
nicht behandelt.

**Acceptance criteria:**
- [ ] `POST /api/depots/{id}/orders` liefert 200 mit `wertpapierSymbol`, `quantity`, `pricePerUnit`, `totalCost`, `newCashBalance`
- [ ] `totalCost` ist auf 2 Stellen HALF_UP gerundet (0.25 × 150.10 → 37.53)
- [ ] Guthaben ist danach um `totalCost` reduziert, bestehende Position erhöht (keine zweite Zeile)

**Verification:**
- [ ] Grün: `OCIT.placeOrder_onExistingPosition_returnsConfirmation`, `…_reducesCashBalance`, `…_increasesQuantityWithoutDuplicatePosition`
- [ ] Grün: `./gradlew test --tests '*DepotControllerIntegrationTest'`

**Dependencies:** None

**Files likely touched:**
- `…/infra/persistence/PositionRepository.java` (+ `Optional<Position> findByDepotIdAndWertpapierId(Long, Long)`)
- `…/app/OrderService.java` (neu)
- `…/infra/web/OrderRequest.java` (neu, Record `wertpapierId`, `quantity`)
- `…/infra/web/OrderResponse.java` (neu, Record)
- `…/infra/web/DepotController.java` (+ `@PostMapping("/{depotId}/orders")`)

**Estimated scope:** M (5 Dateien, davon 3 neu und sehr klein)

**Commit:** `TICKET-142: Kauforder auf bestehende Position ausführen`

---

## Task 2: Erstkauf legt neue Position an

**Description:** Existiert für Depot+Wertpapier noch keine Position, legt
`OrderService` eine neue `Position` mit der gekauften Stückzahl an; andere
Positionen bleiben unverändert.

**Acceptance criteria:**
- [ ] Kauf von SAP ohne Position → 200, neue Position mit `quantity = 2`
- [ ] Bestehende AAPL-Position bleibt bei 10, Depot hat genau 2 Positionen

**Verification:**
- [ ] Grün: `OCIT.placeOrder_withoutExistingPosition_createsNewPosition`
- [ ] Tests aus Task 1 weiterhin grün

**Dependencies:** Task 1

**Files likely touched:**
- `…/app/OrderService.java`

**Estimated scope:** XS

**Commit:** `TICKET-142: Erstkauf legt neue Position an`

---

## Checkpoint A: Happy Path
- [ ] 4 von 10 `OCIT`-Fällen grün, `DepotControllerIntegrationTest` grün
- [ ] Manuell: `./gradlew bootRun`, dann
      `curl -s -X POST localhost:8080/api/depots/1/orders -H 'Content-Type: application/json' -d '{"wertpapierId":1,"quantity":5.0}'`
      → `totalCost 750.00`, `newCashBalance 9250.00`

---

## Task 3: Unbekanntes Depot / Wertpapier → 404

**Description:** Sicherstellen, dass fehlende Depots/Wertpapiere über
`NoSuchElementException` mit exakt den Texten `Depot not found: {id}` bzw.
`Wertpapier not found: {id}` auf 404 abgebildet werden, und dass das Laden
**vor** jeder Mutation passiert. Wahrscheinlich bereits durch Task 1 grün —
dann nur Texte/Reihenfolge prüfen.

**Acceptance criteria:**
- [ ] Unbekanntes Depot → 404, Body `Depot not found: 999999`
- [ ] Unbekanntes Wertpapier → 404, Body `Wertpapier not found: 999999`, Guthaben unverändert

**Verification:**
- [ ] Grün: `OCIT.placeOrder_unknownDepot_returns404`, `OCIT.placeOrder_unknownWertpapier_returns404WithoutSideEffects`

**Dependencies:** Task 1

**Files likely touched:**
- `…/app/OrderService.java` (nur falls Texte/Reihenfolge abweichen)

**Estimated scope:** XS (ggf. kein Code — dann kein eigener Commit)

**Commit:** `TICKET-142: 404 für unbekanntes Depot/Wertpapier`

---

## Task 4: Nicht-positive Stückzahl → 400

**Description:** `ApiExceptionHandler` um `IllegalArgumentException` → 400
(Body = Message) ergänzen. `OrderService` wirft bei `quantity <= 0`
`IllegalArgumentException("Quantity must be positive")` als **erste** Prüfung.

**Acceptance criteria:**
- [ ] `quantity` 0, -1, -0.5 → 400, Body `Quantity must be positive`
- [ ] Guthaben und Positionen unverändert

**Verification:**
- [ ] Grün: `OCIT.placeOrder_nonPositiveQuantity_returns400WithoutSideEffects` (alle 3 Parameter)
- [ ] Alle vorherigen Tests weiterhin grün

**Dependencies:** Task 1 (unabhängig von Task 2/3)

**Files likely touched:**
- `…/infra/web/ApiExceptionHandler.java`
- `…/app/OrderService.java`

**Estimated scope:** S

**Commit:** `TICKET-142: Nicht-positive Stückzahl mit 400 ablehnen`

---

## Task 5: Unzureichendes Guthaben → 400

**Description:** Nach Berechnung von `totalCost` und **vor** jeder Mutation
prüfen, ob `totalCost > cashBalance`; dann
`IllegalArgumentException("Insufficient balance: required " + totalCost + ", available " + cashBalance)`.

**Acceptance criteria:**
- [ ] 10 × 150.00 bei Guthaben 1000.00 → 400, Body `Insufficient balance: required 1500.00, available 1000.00`
- [ ] Guthaben und Positionen unverändert
- [ ] `totalCost == cashBalance` ist erlaubt (Grenzfall, `>` statt `>=`)

**Verification:**
- [ ] Grün: `OCIT.placeOrder_exceedingCashBalance_returns400WithoutSideEffects`
- [ ] `./gradlew test` komplett grün

**Dependencies:** Task 4 (nutzt den 400-Handler)

**Files likely touched:**
- `…/app/OrderService.java`

**Estimated scope:** XS

**Commit:** `TICKET-142: Order bei unzureichendem Guthaben ablehnen`

---

## Checkpoint B: Complete
- [ ] `./gradlew build` grün
- [ ] Alle Success Criteria in `docs/specs/ticket-142-kauforder.md` erfüllt
- [ ] Review mit Mensch (z. B. `/review`)

---

## Task 6: Doku nachziehen

**Description:** Neuen Endpoint in `CLAUDE.md` unter „Bestehende Endpoints“
aufnehmen; in der Spec Open Questions 1 (HALF_UP) und Testing Strategy
(eigene Testklasse, eigene Testdaten) als entschieden markieren.

**Acceptance criteria:**
- [ ] `CLAUDE.md` listet `POST /api/depots/{id}/orders`
- [ ] Spec spiegelt den tatsächlich umgesetzten Stand

**Verification:**
- [ ] Manuell: Diff lesen

**Dependencies:** Task 5

**Files likely touched:**
- `CLAUDE.md`
- `docs/specs/ticket-142-kauforder.md`

**Estimated scope:** XS

**Commit:** `TICKET-142: Doku für Kauforder-Endpoint`
