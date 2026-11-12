# ConRunner Helm packaging

Kubernetes packaging is pending. The [Compose setup](../compose/README.md) documents local
infrastructure, and the [web container guide](../../apps/web/README.md#container-build) documents
the current image and API routing.

A future chart needs to define HTTP and WebSocket routing, health probes, secrets and OIDC
configuration, and the PostgreSQL migration and backup lifecycle.
