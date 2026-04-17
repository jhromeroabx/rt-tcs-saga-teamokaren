# Matriz de alineación con el perfil

Este documento sirve para explicar, durante una entrevista técnica, cómo este repositorio evidencia competencias del perfil Backend / Java solicitado.

| Requisito del perfil | Evidencia en el proyecto | Cómo defenderlo en entrevista |
| --- | --- | --- |
| POO con Java 11+ | El proyecto usa Java 17, separación por capas `domain`, `application`, `infrastructure` y puertos/adaptadores. | Explicar encapsulación del dominio, inversión de dependencias y modelado orientado a casos de uso. |
| Spring Boot | Proyecto montado sobre Spring Boot 3.2.5 con configuración externa y arranque productivo. | Mostrar cómo Boot simplifica seguridad, configuración, testing y exposición de APIs. |
| Spring Security | Ya existe base de seguridad con JWT y configuración central en `src/main/java/.../infrastructure/config`. | Defender cómo se endurecería hacia OAuth2 Resource Server o API Gateway. |
| Spring Cloud | La solución está modelada como microservicio y deja espacio para integrar Config Server, Gateway y service discovery. | Aclarar que el siguiente paso natural sería externalizar configuración y resiliencia con componentes Spring Cloud. |
| Microservicios | El bounded context es `payments`, con contrato OpenAPI, persistencia propia, Docker y esquema aislado. | Explicar autonomía de datos, despliegue independiente y responsabilidad única del servicio. |
| Quarkus base | No está implementado en este repo, pero la arquitectura hexagonal y contract-first facilita migración conceptual. | Comentar diferencias de arranque nativo, GraalVM, CDI vs Spring y cuándo elegir Quarkus. |
| Programación reactiva | Se usa WebFlux y el contrato OpenAPI está generado en modo `reactive`. | Explicar `Mono`, backpressure conceptual, composición y cuándo encapsular componentes bloqueantes. |
| Retrofit / cliente HTTP | El repo no consume aún servicios externos, pero el flujo Saga está diseñado para integraciones con pago, inventario y delivery. | Proponer `WebClient`, Retrofit o Feign según stack del ecosistema y necesidad de observabilidad/reintentos. |
| REST / teoría de APIs | `src/main/resources/openapi/openapi.yml` define recursos, status codes, idempotencia, errores y semántica HTTP. | Defender contract-first, versionado, status `202 Accepted` y diseño orientado a recursos. |
| Swagger / OpenAPI | Se usa `springdoc-openapi` y `openapi-generator-maven-plugin`. | Explicar generación de interfaces, validación de contrato y documentación viva. |
| OAuth2 | No está implementado aún; hoy la base es JWT y `Spring Security`. | Presentarlo como evolución natural: Resource Server + scopes por operación sensible. |
| GitHub / DevOps | Se agregó pipeline en `.github/workflows/ci.yml`, contenedorización y archivo `sonar-project.properties`. | Explicar CI, calidad continua, análisis estático y trazabilidad de despliegue. |
| SonarQube / Fortify | Se deja configuración base para SonarQube y puntos de integración con seguridad. | Explicar cómo se incorporarían quality gates y escaneo SAST en CI/CD. |
| Docker | Existen `Dockerfile` y `docker-compose.yml` para ejecución local. | Explicar empaquetado, portabilidad y paso posterior a Kubernetes. |
| Azure / AKS / Key Vault | Se agregan manifiestos en `k8s/` y ejemplo de `SecretProviderClass` para Azure Key Vault CSI. | Defender despliegue en AKS, secretos externos y configuración cloud-native. |
| JUnit / Mockito | Ya existe test unitario con JUnit 5, Mockito y Reactor Test. | Explicar estrategia: pruebas de casos de uso, mocks de puertos y pruebas por contrato después. |
| SQL | `schema.sql` modela el dominio de pagos, pasos Saga, eventos e idempotencia. | Explicar normalización, claves únicas, FKs y trazabilidad transaccional. |
| Consultas complejas | `docs/sql-showcase.sql` incluye queries operativas y analíticas. | Usarlo para demostrar lectura transversal del modelo y troubleshooting productivo. |
| Saga / orquestación | El contrato y el esquema están diseñados para saga forward + compensations. | Explicar transacciones locales, pasos, estados, idempotencia y rollback compensatorio. |
| Single-thread / Temporal / RxJava | No están implementados. | Mencionarlos honestamente como conocimientos complementarios o extensiones futuras sin sobredimensionar experiencia. |

## Mensaje recomendado para la entrevista

Este proyecto demuestra un enfoque realista de microservicio backend con Java moderno: contrato OpenAPI, WebFlux, seguridad base, persistencia SQL, patrón Saga, idempotencia, pruebas unitarias, contenedorización y preparación para CI/CD y AKS. Lo importante es explicar claramente qué ya está implementado y qué está diseñado como siguiente paso natural.
