# Order Service Microservicio

[![Java Version](https://img.shields.io/badge/Java-21-blue.svg)](https://www.oracle.com/java/technologies/javase-jdk21-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-green.svg)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.9+-purple.svg)](https://maven.apache.org/)
[![MongoDB](https://img.shields.io/badge/MongoDB-7.0-blue.svg)](https://www.mongodb.com/)
[![Redis](https://img.shields.io/badge/Redis-7.2-red.svg)](https://redis.io/)
[![Kafka](https://img.shields.io/badge/Kafka-3.7-yellow.svg)](https://kafka.apache.org/)

Microservicio para la gestión de órdenes de entrega en una empresa de logística. Implementado en **Java con Spring Boot 3.2+**, persiste datos en **MongoDB**, cachea consultas en **Redis**, y emite eventos asíncronos en **Kafka**. Sigue arquitectura de capas (controller/service/repository) con DTOs para desacoplamiento.

## 🎯 Descripción del Proyecto
Este microservicio permite:
- Registrar nuevas órdenes (POST /orders).
- Consultar órdenes por ID (GET /orders/{id}, cacheado 60s en Redis).
- Listar órdenes filtradas por estado y cliente (GET /orders?status=NEW&customerId=123).
- Actualizar estado de una orden (PATCH /orders/{id}/status), invalidando cache y publicando evento en Kafka (topic `orders.events`).

**Estructura de Orden** (en MongoDB):
```json
{
  "_id": "uuid",
  "customerId": "string",
  "status": "NEW|IN_PROGRESS|DELIVERED|CANCELLED",
  "items": [{"sku": "string", "quantity": 1, "price": 100.0}],
  "createdAt": "2025-10-19T22:10:25Z",
  "updatedAt": "2025-10-19T22:10:25Z"
}
```
Evento en Kafka (JSON):json
```json
{
  "orderId": "uuid",
  "oldStatus": "NEW",
  "newStatus": "DELIVERED",
  "timestamp": "2025-10-19T22:10:25Z"
}
```
## Tecnologías y Decisiones 
- TécnicasBackend: Spring Boot 3.2.0 + Java 21 (para features modernas como virtual threads).
- Persistencia: Spring Data MongoDB 4.2.0 (repositorios automáticos, queries nativas).
- Cache: Spring Cache + Redis 7.2 (Lettuce client; @Cacheable
- en GET por ID, @CacheEvict
- en PATCH; TTL 60s).
- Mensajería: Spring Kafka 3.1.0 (producer simple con JSON serializer; async send para no bloquear HTTP).
- Build/Testing: Maven 3.9+; JUnit 5 + Mockito (unitarios); Testcontainers (integración con Mongo/Redis/Kafka).
- Otras: Lombok (boilerplate), Actuator (health checks), Docker (contenedores locales).
Buenas Prácticas:Capas separadas, DTOs para API (desacoplamiento).
- Manejo de errores (try-catch en Kafka, validación de status).
- Transaccionalidad (@Transactional
- en create/update).
- Seguridad: Vars en .env (no commiteadas), SSL para cloud services.

Bonus Implementados:Endpoint /orders/health (valida conexiones Mongo/Redis/Kafka).
- No tracing (OpenTelemetry), pero listo para agregar.

## Requisitos
- Java 21+.
- Maven 3.9+.
- Docker & Docker Compose (para MongoDB/Redis/Kafka locales).
- Opcional: MongoDB Atlas, Redis Cloud, Confluent Cloud (cloud configs en .env).
## Ejecución Local
Clona el Repo:

git clone https://github.com/tu-usuario/order-service.git
cd order-service

Configura Entorno:
Copia .env.example a .env y edita vars (e.g., MONGO_URI para Atlas, REDIS_HOST para cloud).
Ejemplo .env (sensibles ocultos):

MONGO_URI=mongodb+srv://user:pass@cluster0.mongodb.net/orderdb?...
REDIS_HOST=
REDIS_PORT=
KAFKA_BOOTSTRAP_SERVERS=
KAFKA_TOPIC=orders.events
SERVER_PORT=
LOGGING_LEVEL_COM_EXAMPLE=DEBUG

Inicia Servicios (Docker):

docker-compose up -d  # Levanta MongoDB, Redis, Kafka (KRaft mode)

Build y Run:

mvn clean install
mvn spring-boot:run

App en http://localhost:8080.
Health: curl http://localhost:8080/actuator/health.

Pruebas:
Unitarios: mvn test -Dtest=OrderServiceTest.
Integración: mvn test -Dtest=OrderIntegrationTest (usa Testcontainers).

Stop:

mvn spring-boot:stop
docker-compose down

## Ejemplos de Uso
Usa curl o Postman. Base URL: http://localhost:8080.1. Crear Orden (POST /orders)bash

curl -X POST http://localhost:8080/orders \
-H "Content-Type: application/json" \
-d '{
  "customerId": "123",
  "items": [
    {"sku": "ABC", "quantity": 2, "price": 10.5}
  ]
}'

Respuesta: {"id": "uuid", "status": "NEW", "createdAt": "2025-10-19T22:10:25Z"}.

2. Obtener Orden por ID (GET /orders/{id})bash

curl http://localhost:8080/orders/abdb7549-380f-49a1-90e0-cbe15e580a90

Respuesta: JSON completo (cacheado en Redis para llamadas subsiguientes).

3. Listar Órdenes Filtradas (GET /orders)bash

curl "http://localhost:8080/orders?status=NEW&customerId=123"

Respuesta: Array de órdenes.

4. Actualizar Estado (PATCH /orders/{id}/status)bash

curl -X PATCH http://localhost:8080/orders/abdb7549-380f-49a1-90e0-cbe15e580a90/status \
-H "Content-Type: application/json" \
-d '{"status": "DELIVERED"}'

Respuesta: JSON actualizado. Evento publicado en Kafka orders.events.

## Postman Collection
Importa esta colección JSON (link-a-tu-collection.postman.json) para tests rápidos (incluye variables para baseUrl y orderId). 
### Testing
Cobertura: >80% (unitarios con Mockito, integración con Testcontainers para Mongo/Redis/Kafka).
Ejecutar: mvn test (o específicos: -Dtest=OrderControllerTest).

### Notas de Desarrollo
Cloud: Configs para Atlas (Mongo), Redis Cloud, Confluent (Kafka) en .env.
Seguridad: No commitees .env; usa .env.example como template.
Commits: Usa Conventional Commits (feat:, fix:, chore:).
Deploy: Listo para Kubernetes (Dockerfile incluido); agrega CI/CD con GitHub Actions.

### ContribucionesFork > Branch > PR con descripción clara. 
### Licencia
MIT License – ver LICENSE.¡Gracias por revisar! Si necesitas ajustes, abre issue.

¡Gracias por revisar!


