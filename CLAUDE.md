# VagaMatch

Projeto de portfólio: microsserviços em Java que analisam descrições de vagas com IA (Gemini), extraem as skills exigidas e calculam o match com o perfil do candidato.

## Stack

Java 21, Spring Boot 4.1, Spring Data JPA, PostgreSQL 17, Flyway, RabbitMQ 4, springdoc (Swagger), Docker Compose, Maven multi-módulo (wrapper na raiz).

## Estrutura

- `vaga-service` (porta 8080): CRUD de vagas e candidatos, match e estatísticas. Dono do banco.
- `analise-service` (porta 8081): consome vagas novas, chama o Gemini e devolve as skills extraídas. Sem banco.
- Mensageria: exchange topic `vagamatch.events` e DLX `vagamatch.dlx`.
  - `vaga.criada` → fila `analise.vaga-criada` (consumida pelo analise-service)
  - `vaga.analisada` → fila `vaga.vaga-analisada` (consumida pelo vaga-service)
- Cada fila tem DLQ com sufixo `.dlq`. As constantes ficam no `RabbitConfig` de cada serviço.

## Convenções

- Código e nomes de domínio em português (Vaga, Candidato, criadaEm), seguindo o que já existe.
- Pacotes por camada: `domain`, `repository`, `dto`, `service`, `controller`, `exception`, `config`, `messaging`.
- `messaging`: listeners (`@RabbitListener`), publishers e recoverers do RabbitMQ. Ficam finos e delegam a regra pro `service`. Os eventos trafegados são `record` no `dto` (`VagaCriadaEvent`, `VagaAnalisadaEvent`).
- DTOs como `record`; controllers nunca expõem entidades.
- Mudança de schema sempre em nova migration Flyway (`V2__...`, `V3__...`). Nunca editar a V1.
- Erros via `GlobalExceptionHandler` no formato ProblemDetail.
- `ddl-auto: validate`: as entidades precisam bater com as migrations.
- Configs sensíveis só por variável de ambiente. Nunca commitar `.env` nem chave de API.

## Git Flow

- `main`: só versões estáveis. `develop`: integração. Uma branch `feature/*` por etapa, saindo da `develop`.
- Commits no padrão Conventional Commits em português (`feat:`, `fix:`, `test:`, `docs:`, `chore:`).
- Antes de cada commit: `./mvnw -B verify` passando e o serviço subindo.
- Não fazer merge nem push sem eu confirmar.

## Como rodar

```
docker compose up -d
./mvnw -pl vaga-service spring-boot:run
./mvnw -pl analise-service spring-boot:run
```

Windows: `.\mvnw.cmd`. Swagger em http://localhost:8080/swagger-ui.html.

Os serviços leem o `.env` da raiz via `spring.config.import` (`.env` e `../.env`, porque o `spring-boot:run` roda na pasta do módulo). Variável de ambiente real tem prioridade sobre o `.env`.

Retry do listener: no Boot 4 a propriedade é `retry.max-retries` (o `max-attempts` foi depreciado e é ignorado).

## Status das etapas

- [x] 1. Multi-módulo + RabbitMQ (`feature/estrutura-microsservicos`)
- [x] 2. Entidades JPA, CRUD e Swagger (`feature/crud-vagas`)
- [x] 3. `feature/analise-gemini`: migration V2 com status da vaga (PENDENTE, ANALISADA, ERRO); vaga-service publica `vaga.criada` ao cadastrar; analise-service consome, chama o Gemini (`GEMINI_API_KEY`) pedindo JSON com skills, categoria e se é obrigatória ou diferencial, e publica `vaga.analisada`; vaga-service consome, salva as skills e atualiza o status. Falha no Gemini vai pra DLQ após retry.
- [x] 4. `feature/match-estatisticas`: `GET /candidatos/{id}/match/{vagaId}` (% de match ponderando obrigatórias, skills que faltam), ranking de vagas por match e `GET /estatisticas/skills` (mais pedidas). Métricas customizadas com Micrometer.
  - Regra na `CalculadoraMatch`: obrigatória pesa 2, diferencial 1, nível fora do cálculo. Ranking e estatísticas são queries nativas agregadas (pesos passados por parâmetro). Vaga não ANALISADA → 409.
  - Métricas: `vagamatch.vagas.analisadas{resultado}`, `vagamatch.vagas.pendentes` (gauge), `vagamatch.match.calculos{tipo}`.
  - Limitação conhecida: sinônimos não casam ("API REST" x "REST API"). Ideia futura: tabela de aliases.
- [x] 5. `feature/testes`: JUnit 5 + Mockito nos services (principalmente cálculo de match e consumers), cobrindo casos de erro.
  - Não fazer retry em erro permanente do Gemini (4xx: chave inválida, request malformado, modelo inexistente, além de chave ausente): ir direto pro recoverer (DLQ + vaga em ERRO). Exceção: 429 (rate limit) é transitório e continua com retry, assim como 5xx e timeout.
  - Feito com `GeminiPermanenteException` + `RabbitListenerRetrySettingsCustomizer` no `RabbitConfig` (predicate que procura a exceção na cadeia de causas).
  - Fallback de modelo: 503 no modelo principal tenta `gemini.fallback-models` em ordem (`GEMINI_FALLBACK_MODELS`, padrão `gemini-3.5-flash-lite,gemini-2.5-flash`). Todos com 503 → erro transitório (retry).
  - `GeminiService` testado com `MockRestServiceServer` (construtor package-private recebe o `RestClient.Builder`). Entidades nos testes do vaga-service vêm do `Fixtures` (id setado por reflexão).
- [ ] 6. Release: README completo com diagrama Mermaid da arquitetura, exemplos de uso, merge na `main` e tag `v1.0.0`.

Atualize este checklist ao fim de cada etapa.

## Como trabalhar comigo

- Uma etapa por vez. Antes de codar, me mostra o plano da etapa em poucas linhas.
- No fim, me explica o que mudou de forma curta, porque preciso conseguir falar sobre o código em entrevista.
- Respostas em português, diretas.
