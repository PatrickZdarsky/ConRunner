# ConRunner database migrations

Each backend module owns the Liquibase change sets in its matching directory.
[`db.changelog-master.yaml`](db.changelog-master.yaml) explicitly includes the changelogs for
`identity`, `platform`, `eventcatalog`, `audit`, `shift`, and `communications`, in that order.

## Current development policy

We currently do not support migrating data from older versions. Schema and stored-data changes
can and will break existing development databases. There is no supported upgrade path or guarantee
that existing data will be preserved during this phase.

Update existing change sets directly when changing the schema; do not add follow-up migrations
solely to preserve compatibility with older development versions. Liquibase defines the current
schema for a fresh database. Recreate development databases and test data when schema changes or
Liquibase checksum changes make an existing database incompatible. Verify the current changelog
against a fresh PostgreSQL database rather than assuming an older database can be upgraded.

Keep schema definitions in the owning module's changelog. When a module gains its first changelog,
include it in the master after the modules it depends on. A supported upgrade and data migration
policy will need to be introduced explicitly when this development phase ends.

## Local development schema

The [`local` Spring profile](../../application-local.yaml) selects
[`db.changelog-local.yaml`](db.changelog-local.yaml), which applies the audit changelog for the
JDBC audit store. Hibernate updates the rest of the local schema from entity mappings.
A successful local startup exercises this combination. To verify the full set of migrations,
apply the master changelog to PostgreSQL.
