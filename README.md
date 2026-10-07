# VagaMatch

Microsserviços em Java que analisam descrições de vagas com IA, extraem as skills exigidas e calculam o match com o perfil do candidato.

> 🚧 Em desenvolvimento. README completo com diagrama de arquitetura na versão 1.0.

## Arquitetura

| Serviço | Porta | Responsabilidade |
|---|---|---|
| `vaga-service` | 8080 | CRUD de vagas e candidatos, cálculo de match e estatísticas (PostgreSQL) |
| `analise-service` | 8081 | Consome vagas novas, extrai skills com o Gemini e devolve o resultado |

Comunicação assíncrona via RabbitMQ (exchange `vagamatch.events`), com dead-letter queue para mensagens que falham.

## Stack

Java 21 · Spring Boot 4 · Spring Data JPA · PostgreSQL · Flyway · RabbitMQ · Docker · Gemini API

## Como rodar

```bash
cp .env.example .env
docker compose up -d

# em terminais separados
./mvnw -pl vaga-service spring-boot:run
./mvnw -pl analise-service spring-boot:run
```

No Windows, use `mvnw.cmd` no lugar de `./mvnw`.

- Health: http://localhost:8080/actuator/health e http://localhost:8081/actuator/health
- Painel do RabbitMQ: http://localhost:15672 (usuário/senha do `.env`)

## Fluxo de branches

Git Flow: `main` (versões estáveis), `develop` (integração) e `feature/*` para cada funcionalidade.
