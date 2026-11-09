# agentic-sdlc-demo

Spring Boot REST-Service zur Verwaltung von Wertpapierdepots.

## Architektur

Onion Architecture (siehe [`docs/architecture.md`](docs/architecture.md) für
Hintergrund und Quellen): Abhängigkeiten zeigen nach innen, `infra` → `app` →
`domain`.

- `domain/` – JPA-Entities: `Depot`, `Wertpapier`, `Position`
- `app/` – Fachlogik / Anwendungsfälle (z.B. `DepotQueryService`, `OrderService`)
- `infra/web/` – REST-Controller und Response-DTOs
- `infra/persistence/` – Spring Data JPA Repositories
- `infra/` (root) – `DemoDataLoader` (Demo-Datenbefüllung)

## Datenmodell

- Ein `Depot` hat einen Besitzer und ein `cashBalance` (BigDecimal).
- Ein `Wertpapier` hat einen `symbol`, `name` und einen statischen `price`
  (kein echter Marktdaten-Feed in diesem Service).
- Eine `Position` verknüpft ein `Depot` mit einem `Wertpapier` über eine `quantity`.

`DemoDataLoader` befüllt beim Start eine In-Memory-H2-Datenbank mit Demo-Daten
(ein Depot, drei Wertpapiere, eine bestehende Position).

## Bestehende Endpoints

- `GET /api/depots/{id}` – Depot-Stammdaten
- `GET /api/depots/{id}/positions` – Positionen eines Depots
- `POST /api/depots/{id}/orders` – Kauforder sofort ausführen (TICKET-142)

## Konventionen

- Geldbeträge und Stückzahlen werden als `BigDecimal` modelliert, nie als
  `double`/`float`.
- Response-Objekte sind Java Records unter `infra/web/`. Services in `app/`
  geben eigene Records zurück (z.B. `OrderConfirmation`), das Mapping auf
  Response-DTOs passiert im Controller.
- Fehlerfälle werden über `ApiExceptionHandler` auf passende HTTP-Status
  abgebildet.
