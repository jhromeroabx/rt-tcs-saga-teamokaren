# Payments Saga Microservice
Microservicio desarrollado con Spring Boot 3 + WebFlux + JPA bajo arquitectura hexagonal para orquestación de pagos con patrón Saga e idempotencia.

## ¿Qué demuestra este proyecto?
- Backend con `Java 21` y principios de POO
- `Spring Boot`, `Spring Security`, `WebFlux` y contrato `OpenAPI`
- Diseño de microservicio con patrón `Saga` e idempotencia
- Persistencia SQL y consultas orientadas a operación
- Base DevOps con `Docker`, `GitHub Actions`, `SonarQube` y manifiestos `AKS`

## Documentación para entrevista técnica
- `docs/skill-matrix.md`: mapea el proyecto contra el perfil solicitado
- `docs/architecture.md`: explica la arquitectura, Saga, reactividad e idempotencia
- `docs/sql-showcase.sql`: consultas SQL para demostrar modelado y análisis operativo
- `k8s/`: manifiestos de despliegue y ejemplo de integración con Azure Key Vault

## Tecnologías
- Java 21
- Spring Boot 3
- WebFlux
- Spring Security
- JPA / Hibernate
- H2 Database (in-memory)
- OpenAPI (Contract First)
- JWT
- Docker
- GitHub Actions
- SonarQube
- Kubernetes / AKS (manifiestos de referencia)

## Arquitectura
El proyecto sigue arquitectura hexagonal (Ports & Adapters):
- domain: modelos y contratos (ports)
- application: casos de uso y lógica de aplicación
- infrastructure: adaptadores (web, persistencia, seguridad)
- config: configuración transversal

## Diagrama Arquitectura Hexagonal
![Diagrama Arquitectura Hexagonal](Diagrama_arquitectura_hexagonal.png)

## Diagrama Secuencia Saga de Pago
El flujo objetivo del dominio es: crear orden, procesar pago, actualizar inventario y coordinar entrega, con compensaciones ante fallos.

## Funcionalidades
- Inicio de pagos orquestados con patrón Saga
- Consulta del estado de la saga y sus pasos
- Solicitud de compensaciones manuales
- Persistencia del estado de la saga, pasos y eventos
- Idempotencia mediante header `X-Request-Id`
- Manejo global de errores

## Cómo venderlo en la postulación
- Como microservicio backend orientado a integraciones distribuidas
- Como evidencia de dominio en `REST`, `OpenAPI`, `WebFlux`, seguridad y SQL
- Como base preparada para CI/CD, calidad continua y despliegue cloud en `AKS`
- Como ejercicio honesto: muestra lo ya implementado y deja clara la evolución hacia `OAuth2`, `Spring Cloud` y clientes HTTP externos

## Dominio del contrato
- `POST /api/v1/payments`: inicia la saga del pago
- `GET /api/v1/payments/{paymentId}`: consulta el estado consolidado
- `POST /api/v1/payments/{paymentId}/compensations`: solicita compensación manual

## Propósito de la API
- `POST /api/v1/payments`: crea una solicitud de pago y arranca la orquestación Saga. Responde `202 Accepted` porque el flujo distribuido fue aceptado para procesamiento y luego debe consultarse su estado.
- `GET /api/v1/payments/{paymentId}`: consulta el resultado consolidado de la saga, incluyendo estado general, paso actual, pasos ejecutados y compensaciones si existieron.
- `POST /api/v1/payments/{paymentId}/compensations`: fuerza una reversión manual cuando una operación ya creada necesita deshacerse por decisión operativa o inconsistencia externa.

## Flujo esperado
1. El cliente llama a `POST /api/v1/payments` con `X-Request-Id`.
2. La API devuelve `202 Accepted` y el header `Location` con la URL de consulta.
3. El cliente usa `GET /api/v1/payments/{paymentId}` para revisar si la saga quedó `COMPLETED`, `FAILED`, `COMPENSATING` o `COMPENSATED`.
4. Si negocio u operaciones necesitan revertir manualmente una saga ya registrada, se usa `POST /api/v1/payments/{paymentId}/compensations`.

## Semántica de respuestas
- `202 Accepted`: la solicitud fue aceptada y el recurso quedó listo para seguimiento.
- `200 OK` en `GET`: consulta exitosa del estado actual.
- `200 OK` con header `X-Idempotent-Replay: true`: reintento idempotente; la API devolvió una respuesta ya registrada para la misma clave y el mismo payload.

## Endpoint principal
POST /api/v1/payments
```
curl -X 'POST' \
  'http://localhost:8080/api/v1/payments' \
  -H 'accept: application/json' \
  -H 'X-Request-Id: req-payment-0001' \
  -H 'Content-Type: application/json' \
  -d '{
  "orderId": "ORD-100045",
  "customerId": "CUS-9001",
  "currency": "PEN",
  "totalAmount": 149.90,
  "paymentMethod": {
    "type": "CARD",
    "token": "tok_01HZX9J4P3A1",
    "cardLast4": "4242",
    "installments": 1
  },
  "items": [
    {
      "sku": "SKU-1000",
      "name": "Audífonos Bluetooth",
      "quantity": 2,
      "unitPrice": 74.95
    }
  ],
  "shippingAddress": {
    "street": "Av. Primavera 123",
    "city": "Lima",
    "state": "Lima",
    "country": "PE",
    "postalCode": "15046",
    "reference": "Torre 2, recepción"
  },
  "metadata": {
    "channel": "web"
  }
}'
```

### Casos esperados en creación

| Escenario | `X-Request-Id` | Payload | Resultado esperado | HTTP |
|---|---|---|---|---|
| Creación inicial | Nuevo | Nuevo | La saga se acepta y devuelve `Location` para seguimiento | `202 Accepted` |
| Replay idempotente | El mismo de la primera llamada | Igual al primero | La API devuelve la misma respuesta ya registrada | `200 OK` |
| Conflicto idempotente | El mismo de la primera llamada | Distinto al primero | La API rechaza la solicitud por reutilizar la clave con otro body | `409 Conflict` |

Ejemplo de error esperado en conflicto idempotente:

```json
{
  "code": "IDEMPOTENCY_CONFLICT",
  "mensaje": "La clave X-Request-Id ya fue utilizada con un payload distinto.",
  "timestamp": "2026-04-16T23:48:53.83838-05:00"
}
```

## Consultar estado del pago
GET /api/v1/payments/{paymentId}
```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/payments/d141be5b-7d99-4996-8403-c39808eb0d72' \
  -H 'accept: application/json'
```

## Solicitar compensación manual
Usa este endpoint cuando ya existe un pago/saga y necesitas revertirlo manualmente, por ejemplo por inconsistencia de inventario, anulación operativa o rollback solicitado por backoffice.

POST /api/v1/payments/{paymentId}/compensations
```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/payments/d141be5b-7d99-4996-8403-c39808eb0d72/compensations' \
  -H 'accept: application/json' \
  -H 'X-Request-Id: compensation-0001' \
  -H 'Content-Type: application/json' \
  -d '{
  "reason": "Anulación manual por incidencia operativa",
  "requestedBy": "backoffice-ops"
}'
```

Resultado esperado: la saga cambia a estado `COMPENSATED` y se agregan pasos compensatorios como `CANCEL_DELIVERY`, `REVERSE_INVENTORY`, `REVERSE_PAYMENT` y `CANCEL_ORDER`, según los pasos forward ya completados.

### Casos esperados en compensación

| Escenario | `X-Request-Id` | Resultado esperado | HTTP |
|---|---|---|---|
| Primera compensación válida | Nuevo | La saga pasa a `COMPENSATED` y registra pasos compensatorios | `202 Accepted` |
| Reintento idempotente | El mismo de la primera llamada | La API devuelve la misma respuesta ya registrada | `200 OK` |
| Nueva compensación sobre saga ya compensada | Distinto al primero | La operación se rechaza por estado inválido | `422 Unprocessable Entity` |

Ejemplo de error esperado en el último caso:

```json
{
  "code": "INVALID_SAGA_STATE",
  "mensaje": "La saga ya fue compensada previamente",
  "timestamp": "2026-04-16T23:48:53.83838-05:00"
}
```

## Base de datos
- H2 en memoria
- Tablas principales: `payment_saga`, `payment_item`, `payment_saga_step`, `payment_saga_event`, `idempotency_request`
- Consultas de demostración en `docs/sql-showcase.sql`

## Headers
`X-Request-Id`: identificador para idempotencia en creación y compensación

## Ejecución
```bash
mvn clean install
mvn spring-boot:run
```

## Integración continua y calidad
- Pipeline base en `.github/workflows/ci.yml`
- Configuración de análisis estático en `sonar-project.properties`
- Empaquetado local con `Dockerfile` y `docker-compose.yml`

## Despliegue cloud de referencia
- `k8s/deployment.yaml`: despliegue para Kubernetes / AKS
- `k8s/service.yaml`: exposición interna del servicio
- `k8s/secretproviderclass.yaml`: ejemplo de consumo de secretos desde Azure Key Vault vía CSI Driver

## Swagger en Codespaces

Si ejecutas la app detrás del proxy de GitHub Codespaces, Spring debe procesar los headers reenviados para que OpenAPI no publique `http://localhost:8080` como servidor.

La configuración ya queda habilitada con:

```yaml
server:
	forward-headers-strategy: framework
```

Con eso, Swagger/OpenAPI toma automáticamente la URL pública del Codespace cuando accedes desde el navegador.
