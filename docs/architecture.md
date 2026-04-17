# Arquitectura de referencia

## Objetivo

Este microservicio modela el inicio y seguimiento de pagos distribuidos bajo patrón Saga, con foco en resiliencia, idempotencia y separación clara de responsabilidades.

## Capas

- `domain`: entidades, reglas y puertos del negocio.
- `application`: casos de uso y coordinación del flujo.
- `infrastructure`: controladores, adaptadores de persistencia, seguridad y configuración.
- `resources/openapi`: contrato API-first para generar interfaces y modelos.

## Flujo Saga propuesto

1. `CREATE_ORDER`
2. `PROCESS_PAYMENT`
3. `UPDATE_INVENTORY`
4. `DELIVER_ORDER`

Si un paso falla, se activan compensaciones:

1. `CANCEL_DELIVERY`
2. `REVERSE_INVENTORY`
3. `REVERSE_PAYMENT`
4. `CANCEL_ORDER`

## Idempotencia

La API exige `X-Request-Id` para creación y compensación.

El diseño de persistencia contempla:

- huella del request (`request_hash`)
- recurso afectado (`resource_type`)
- cuerpo cacheado de respuesta (`response_body`)
- estado HTTP devuelto (`http_status`)

Esto permite reintentos seguros y detección de conflictos cuando llega la misma clave con un payload diferente.

## Reactividad

La solución usa WebFlux para exponer endpoints reactivos. El punto clave a defender es que la API puede orquestar múltiples integraciones sin bloquear hilos del servidor, encapsulando cualquier adaptador bloqueante detrás de puertos o schedulers dedicados.

## Seguridad

Actualmente la base gira alrededor de Spring Security y JWT. La evolución natural para un entorno corporativo es:

- usar OAuth2 Resource Server
- delegar autenticación a un Identity Provider
- propagar scopes/claims a las operaciones de compensación

## Persistencia SQL

El esquema separa:

- cabecera de la saga (`payment_saga`)
- detalle de ítems (`payment_item`)
- pasos forward/compensation (`payment_saga_step`)
- eventos publicados (`payment_saga_event`)
- tabla de idempotencia (`idempotency_request`)

Este diseño soporta auditoría, troubleshooting y trazabilidad operativa.

## DevOps y nube

El repositorio incluye:

- `Dockerfile` para empaquetar el servicio
- `docker-compose.yml` para entorno local
- `.github/workflows/ci.yml` para integración continua
- `sonar-project.properties` para análisis de calidad
- manifiestos `k8s/` pensados para AKS
- ejemplo de Azure Key Vault CSI vía `SecretProviderClass`

## Cómo vender la solución

Presenta este proyecto como una base profesional de microservicio backend orientado a integraciones. No hace falta afirmar que todo está productivo; basta con demostrar que las decisiones técnicas están bien encaminadas, justificadas y listas para evolucionar.
