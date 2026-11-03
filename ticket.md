# TICKET-142: Kauforder aufgeben

**Typ:** Feature
**Komponente:** depot-order-service

## Beschreibung

Als Kunde möchte ich eine Kauforder für ein Wertpapier aufgeben können, damit ich
Positionen in meinem Depot aufbauen kann.

Aktuell kann man über die API nur Depots und bestehende Positionen *anzeigen*
(`GET /api/depots/{id}`, `GET /api/depots/{id}/positions`). Es gibt noch keine
Möglichkeit, eine Order auszuführen.

## Akzeptanzkriterien

- Ein Kunde kann für ein bestehendes Depot eine Kauforder für ein bestehendes
  Wertpapier aufgeben (Stückzahl + Wertpapier).
- Die Order wird zum aktuellen Preis des Wertpapiers ausgeführt.
- Nach einer erfolgreichen Order ist die gekaufte Stückzahl in den Positionen
  des Depots sichtbar.
- Das Guthaben des Depots wird um den Kaufbetrag reduziert.
- Nicht jede Order kann ausgeführt werden.

## Hinweise

- Bestehendes Datenmodell: `Depot` (Guthaben), `Wertpapier` (mit Preis),
  `Position` (Depot hält Stückzahl eines Wertpapiers).
- Es gibt noch keinen Endpoint für Orders.
