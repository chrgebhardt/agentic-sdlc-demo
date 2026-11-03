# Architektur: Onion Architecture

Domäne im Zentrum, Abhängigkeiten zeigen immer nach innen. (nach [Oliver
Drotbohm](https://odrotbohm.de/2023/07/sliced-onion-architecture/))

In diesem Repo: Presentation Layer = `infra/web/`, Application Layer = `app/`,
Domain Services + Domain Model = `domain/`.

## Ordnerstruktur

```
de.cofinpro.training.depot
├─ domain/            Depot, Wertpapier, Position
├─ app/                DepotQueryService
└─ infra/
   ├─ web/             DepotController, Response-DTOs, ApiExceptionHandler
   └─ persistence/     Spring-Data-JPA-Repositories
```

| Schicht | Package | Inhalt |
|---|---|---|
| Domain | `domain/` | Entities + fachliche Regeln, keine Abhängigkeit nach außen |
| Application | `app/` | Anwendungsfälle, orchestriert `domain` + `infra` |
| Infrastructure | `infra/web/` | REST-Controller, Response-DTOs, Fehler-Mapping |
| Infrastructure | `infra/persistence/` | Spring-Data-JPA-Repositories |

Vereinfachung: JPA-Annotationen liegen direkt auf den Domain-Entities, Repository-Interfaces liegen direkt in `infra` — kein zusätzlicher Port/Adapter-Layer.
