# Spec: TICKET-142 — Kauforder aufgeben

## Objective

Ein Kunde kann über die API eine Kauforder für ein Wertpapier in einem
bestehenden Depot aufgeben, um Positionen aufzubauen.

- **User:** Depot-Kunde (API-Konsument)
- **Why now:** Es gibt bisher nur lesende Endpoints
  (`GET /api/depots/{id}`, `GET /api/depots/{id}/positions`); es fehlt die
  Möglichkeit, tatsächlich zu kaufen.
- **Success:** Ein `POST`-Request mit Wertpapier-ID und Stückzahl führt die
  Order zum aktuellen `Wertpapier`-Preis sofort aus: Guthaben wird reduziert,
  Position wird erhöht/angelegt, und der Kunde erhält eine Bestätigung mit
  Stückzahl, Preis pro Stück, Gesamtkosten und neuem Guthaben zurück.
- **Explizit stateless:** Es gibt kein persistiertes `Order`-Objekt, keine
  Order-Historie und keinen Order-Status. Die Order ist eine Operation, kein
  Domain-Objekt.

## Tech Stack

Übernommen aus dem bestehenden Projekt (siehe `CLAUDE.md`, `build.gradle`):

- Java 21, Spring Boot 4.1.2, Spring Data JPA, Spring Web MVC, Bean Validation
- H2 In-Memory-Datenbank (Demo-Daten via `DemoDataLoader`)
- Lombok (`@Getter`/`@Setter`/`@NoArgsConstructor`/`@AllArgsConstructor` auf
  Entities)
- Onion Architecture: `infra` → `app` → `domain`

## Commands

```
Build: ./gradlew build
Test:  ./gradlew test
Run:   ./gradlew bootRun
```

## Project Structure (betroffene/neue Dateien)

```
src/main/java/.../app/OrderService.java          → neu: Fachlogik Kauforder
src/main/java/.../app/OrderConfirmation.java     → neu: Ergebnis-Record des Service
src/main/java/.../infra/web/OrderRequest.java     → neu: Request-DTO
src/main/java/.../infra/web/OrderResponse.java    → neu: Response-DTO
src/main/java/.../infra/web/OrderController.java  → neu: POST-Endpoint
src/main/java/.../infra/web/ApiExceptionHandler.java → ergänzt: 400-Mapping
src/test/java/.../infra/web/OrderControllerIntegrationTest.java → neu
```

Kein neues Repository, keine neue Entity — passend zum bestehenden
Datenmodell (`Depot`, `Wertpapier`, `Position`).

## API-Contract

```
POST /api/depots/{depotId}/orders

Request Body:
{
  "wertpapierId": 1,
  "quantity": 5.0
}

Response 200 OK:
{
  "wertpapierSymbol": "AAPL",
  "quantity": 5.0,
  "pricePerUnit": 150.00,
  "totalCost": 750.00,
  "newCashBalance": 9250.00
}
```

**Fehlerfälle:**

| Fall | Status | Beispieltext |
|---|---|---|
| Depot nicht gefunden | 404 | `Depot not found: {id}` |
| Wertpapier nicht gefunden | 404 | `Wertpapier not found: {id}` |
| `wertpapierId` bzw. `quantity` fehlt | 400 | `wertpapierId must not be null` / `quantity must not be null` |
| `quantity <= 0` | 400 | `Quantity must be positive` |
| `quantity` mit mehr als 2 Nachkommastellen | 400 | `quantity must have at most 2 decimal places` |
| Guthaben reicht nicht | 400 | `Insufficient balance: required {totalCost}, available {cashBalance}` |

Abgelehnte Orders dürfen keine Seiteneffekte haben (kein teilweiser Abzug vom
Guthaben, keine Positionsänderung).

**Position-Verhalten:** Existiert bereits eine Position für
Depot+Wertpapier, wird deren `quantity` erhöht; sonst wird eine neue
`Position` angelegt.

## Code Style

- Records für DTOs, analog zu `PositionResponse`:
  ```java
  public record PositionResponse(String wertpapierSymbol, String wertpapierName,
                                  BigDecimal quantity, BigDecimal currentPrice) {
  }
  ```
- Services per Konstruktor-Injection (kein Field-Injection), analog
  `DepotQueryService`.
- Geldbeträge/Stückzahlen ausschließlich `BigDecimal` (siehe `CLAUDE.md`).
- Fehlerfälle als Exceptions, gemappt über `ApiExceptionHandler`:
  - `NoSuchElementException` → 404 (bereits vorhanden, wiederverwendet für
    Depot/Wertpapier-Lookup)
  - `IllegalArgumentException` → 400 (neu zu ergänzen, für Stückzahl- und
    Guthaben-Validierung)

## Testing Strategy

- Integrationstests via `@SpringBootTest` + `MockMvc` in
  `OrderControllerIntegrationTest`. Jeder Test legt seine Daten selbst an und
  läuft in einer zurückgerollten Transaktion (unabhängig vom `DemoDataLoader`).
- Abzudeckende Fälle:
  1. Erfolgreiche Order auf bestehende Position (AAPL, Depot 1) → 200,
     korrekte Response-Werte, Guthaben und Position in der DB aktualisiert.
  2. Erfolgreiche Order auf Wertpapier ohne bestehende Position (SAP oder
     DTE) → neue Position wird angelegt.
  3. Unbekanntes Depot → 404.
  4. Unbekanntes Wertpapier → 404.
  5. `quantity = 0` bzw. negativ → 400, keine Seiteneffekte.
  6. Bestellwert übersteigt Guthaben → 400, keine Seiteneffekte.
- Vorgehen gemäß `test-driven-development`: Tests vor Implementierung
  schreiben.

## Boundaries

- **Always:** `./gradlew test` vor Abschluss grün; Onion-Schichtung
  einhalten (`infra` → `app` → `domain`); `BigDecimal` für Geld/Stückzahl;
  Fehlerfälle über `ApiExceptionHandler`.
- **Ask first:** Neue Abhängigkeiten; Änderung bestehender
  Response-Contracts (`DepotResponse`, `PositionResponse`); Einführung einer
  persistierten `Order`-Entity (aktuell explizit außerhalb des Scopes).
- **Never:** Sell-Orders, Limit-Orders, Order-Historie/-Status,
  asynchrone/aufgeschobene Ausführung, echter Marktdaten-Feed — alles
  außerhalb des Scopes dieses Tickets.

## Success Criteria

- [x] `POST /api/depots/1/orders` mit gültiger `wertpapierId`+`quantity`
      liefert 200 mit korrektem `pricePerUnit`, `totalCost` und
      `newCashBalance`.
- [x] Guthaben des Depots ist nach der Order um `totalCost` reduziert.
- [x] Position zeigt die erhöhte Stückzahl (bzw. neue Position bei
      Erstkauf eines Wertpapiers).
- [x] Unbekanntes Depot/Wertpapier → 404.
- [x] `quantity <= 0` → 400, keine Seiteneffekte.
- [x] Bestellwert > Guthaben → 400, keine Seiteneffekte.
- [x] Alle Tests grün (`./gradlew test`).

## Open Questions

1. **Rundung von `totalCost`:** Soll `quantity * price` auf 2 Nachkommastellen
   gerundet werden (HALF_UP, wie für Geldbeträge üblich), oder wird die
   rohe `BigDecimal`-Multiplikation ohne explizites Rounding
   zurückgegeben? **Entschieden:** Rundung auf 2 Nachkommastellen, HALF_UP
   (durch Test abgesichert).
2. **Mehrere Positionen pro Wertpapier:** Angenommen wird, dass pro
   Depot+Wertpapier maximal eine `Position`-Zeile existiert (Merge statt
   Duplikat). Das entspricht dem Demo-Datenmodell, wurde aber nicht
   explizit mit dem Kunden bestätigt.
3. **Endpoint-Ort:** **Entschieden:** eigener `OrderController` unter
   `/api/depots/{id}/orders` (Nesting-Konvention bleibt erhalten).
