# ConRunner database migrations

Each backend module owns the Liquibase change sets in its matching directory.
[`db.changelog-master.yaml`](db.changelog-master.yaml) explicitly includes the changelogs for
`identity`, `platform`, `eventcatalog`, `audit`, `shift`, and `communications`, in that order.

Add a new change set to the owning module's changelog. Preserve applied change sets and append
follow-up changes so existing databases can advance through the same history. When a module gains
its first changelog, include it in the master after the modules it depends on.

## Local development schema

The [`local` Spring profile](../../application-local.yaml) selects
[`db.changelog-local.yaml`](db.changelog-local.yaml), which applies the audit changelog for the
JDBC audit store. Hibernate updates the rest of the local schema from entity mappings.
A successful local startup exercises this combination. To verify the full set of migrations,
apply the master changelog to PostgreSQL.
