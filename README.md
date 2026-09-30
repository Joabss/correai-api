# correai-api

API REST para um aplicativo de corrida/caminhada (CorreAI). O usuário registra suas atividades (corrida ou caminhada) e a API calcula estatísticas de desempenho, como distância percorrida na semana/mês, sequência de dias treinados (streak) e maior distância já percorrida.

Este é um **MVP inicial** do projeto.

O andamento detalhado esta em
[`docs/STATUS_DO_PROJETO.md`](docs/STATUS_DO_PROJETO.md). A decisao de arquitetura
esta registrada em
[`docs/adr/0001-arquitetura-hexagonal-e-java-25.md`](docs/adr/0001-arquitetura-hexagonal-e-java-25.md).

## Visão Geral

A aplicação permite que um usuário (identificado de forma anônima via header, sem necessidade de cadastro/login):

- Registre atividades de corrida (`RUN`) ou caminhada (`WALK`), informando distância, duração, tipo de treino, esforço percebido e notas.
- Liste o histórico de atividades registradas.
- Consulte um resumo estatístico (`/stats/summary`) com:
  - Quilometragem da semana atual (`kmWeek`)
  - Quilometragem do mês atual (`kmMonth`)
  - Quantidade de atividades na semana (`activitiesWeek`)
  - Sequência de dias consecutivos com atividade (`streak`)
  - Maior distância já percorrida (`longestDistance`)

Não há cadastro/login: o cliente chama `POST /auth/anonymous`, que cria um usuário anônimo e devolve um **JWT** assinado (HS256). Esse token deve ser enviado em todas as chamadas seguintes no header `Authorization: Bearer <token>`.

## Stack Tecnológica

- **Java 25 LTS**
- **Spring Boot 4.1.1** (Spring Framework 7, Jakarta EE 11, Hibernate 7, Tomcat 11, Jackson 3)
  - Spring Web MVC (`spring-boot-starter-webmvc`, REST controllers)
  - Spring Data JPA (persistência)
  - Spring Validation (Bean Validation / `jakarta.validation`)
  - Spring Boot Actuator (health checks / observabilidade)
- **PostgreSQL 16** (banco de dados principal)
- **H2 Database** (em memória, usado apenas nos testes)
- **Spring Security não é usado**: a autenticação é um interceptor com JWT (Nimbus JOSE + JWT)
- **springdoc-openapi** (Swagger UI e OpenAPI 3)
- **Flyway** (migrações de banco)
- **Lombok** (redução de boilerplate em DTOs e entidades JPA; o domínio não usa Lombok)
- **Maven** (build e gerenciamento de dependências)
- **Docker / Docker Compose** (containerização da API e do banco)
- **JUnit 5 + Mockito** (testes unitários) e `spring-boot-starter-webmvc-test` (`@WebMvcTest`)

## Arquitetura

O projeto segue arquitetura hexagonal dentro do pacote `com.correai.api`:

```
com.correai.api
├── domain/
│   ├── model/      # Records de domínio (User, Activity), paginação (PageQuery/PageResult) e regras de negócio
│   └── port/
│       ├── in/     # Casos de uso, organizados por feature (activity, stats, user)
│       └── out/    # Portas de persistência, organizadas por feature (activity, user)
├── application/    # Implementação e orquestração dos casos de uso (sem anotações Spring)
├── config/         # BeanConfig: registra os services de aplicação como beans
└── adapter/
  ├── in/web/     # Controllers, configuração web, DTOs REST e PageResponse
  └── out/persistence/ # Entidades JPA, mapeadores e adapters de repositório
```

As dependencias apontam para o dominio: adapters de entrada chamam portas de
entrada, servicos de aplicacao usam portas de saida e os adapters de persistencia
implementam essas portas. O dominio nao conhece Spring, HTTP ou JPA.

- Os modelos `User` e `Activity` são `record`s imutáveis, acessados por `id()`, `distanceKm()` etc. (sem getters `getX()`).
- Os services de `application` não usam `@Service`; são instanciados em `config/BeanConfig`.

### Fluxo de identificação do usuário

`POST /auth/anonymous` cria um `User` anônimo e devolve `{accessToken, tokenType, userId, expiresAt}`. O token é emitido e verificado pelo adapter `JwtTokenAdapter` (porta `TokenPort`, biblioteca Nimbus JOSE + JWT).

Um `HandlerInterceptor` (`UserContextInterceptor`) intercepta as requisições para `/activities/**` e `/stats/**`:

1. Exige o header `Authorization: Bearer <token>`.
2. Valida assinatura e expiração do token e confirma que o usuário ainda existe.
3. Propaga o `userId` como atributo da requisição (`@RequestAttribute("userId")` nos controllers).

Token ausente, inválido ou expirado resulta em `401`. O segredo é configurado em `app.security.jwt.secret` (mínimo de 32 bytes; em `prod`, variável `JWT_SECRET`) e a validade em `app.security.jwt.expiration` (padrão 30 dias).

### Tratamento de erros

Os erros seguem o formato RFC 7807 (`ProblemDetail`, `application/problem+json`): `400` para parâmetros e payloads inválidos (com o mapa `errors` por campo na validação do body), `401` para autenticação e `500` para falhas inesperadas.

### Regras de negócio principais

- `Activity` calcula automaticamente o **pace médio** (`avgPaceSeconds`) a partir da distância e duração informadas.
- Distância e duração devem ser maiores que zero (validado tanto via Bean Validation no DTO quanto na entidade de domínio).
- `StatsApplicationService` calcula:
  - Soma de distâncias da semana (segunda-feira até hoje) e do mês (dia 1 até hoje).
  - Streak: dias consecutivos, a partir de hoje, com pelo menos uma atividade registrada.
  - Maior distância entre todas as atividades do usuário.

### Endpoints

| Método | Rota                | Descrição                                   |
|--------|---------------------|----------------------------------------------|
| POST   | `/auth/anonymous`   | Cria um usuário anônimo e retorna o JWT      |
| GET    | `/activities`       | Lista paginada das atividades (`?page=0&size=20`, máx. 100) |
| POST   | `/activities`       | Cria uma nova atividade                      |
| GET    | `/stats/summary`    | Retorna o resumo estatístico do usuário      |
| GET    | `/actuator/health`  | Health check da aplicação                    |
| GET    | `/swagger-ui.html`  | Documentação interativa (OpenAPI em `/v3/api-docs`) |

As rotas `/activities` e `/stats` exigem o header `Authorization: Bearer <token>`.

**Paginação de `GET /activities`:**

- Parâmetros: `page` (padrão `0`) e `size` (padrão `20`, entre `1` e `100`). Valores inválidos retornam `400`.
- Ordenação: `activityDate` decrescente e, em caso de empate, `createdAt` decrescente.
- A resposta é um objeto paginado, não mais um array:

```json
{
  "content": [
    { "id": "...", "type": "RUN", "date": "2026-09-30", "distanceKm": 5.2, "avgPace": "04:48", "durationSeconds": 1500 }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

As consultas por intervalo de datas usadas nas estatísticas não são paginadas, pois alimentam somas.

**Exemplo de payload para `POST /activities`:**
```json
{
  "type": "RUN",
  "distanceKm": 5.2,
  "durationSeconds": 1500,
  "trainingType": "EASY",
  "perceivedEffort": "OK",
  "notes": "Treino leve de recuperação"
}
```

### Collection do Postman

Uma collection pronta com todos os endpoints está disponível em [`postman/correai-api.postman_collection.json`](postman/correai-api.postman_collection.json), junto com um environment em [`postman/correai-api.postman_environment.json`](postman/correai-api.postman_environment.json).

**Como importar:**
1. Abra o Postman → `Import` → selecione os dois arquivos da pasta `postman/`.
2. Selecione o environment **"correai-api - Local (Docker)"** no canto superior direito.
3. Execute primeiro `Auth > Criar usuário anônimo` — o script de teste salva o token nas variáveis `accessToken` e `userId` da collection.
4. As próximas requisições enviam `Authorization: Bearer {{accessToken}}` automaticamente.

A collection contém as pastas:
- **Auth**: criar usuário anônimo e obter o token.
- **Activities**: listar (paginado), criar atividade (RUN/WALK) e um exemplo de payload inválido para testar as validações.
- **Stats**: resumo estatístico do usuário.
- **Actuator**: health check da aplicação.

## Configuração e Setup

### Pré-requisitos

- Docker e Docker Compose (forma recomendada de execução)
- Alternativamente, para rodar localmente sem Docker:
  - JDK 25
  - Maven (ou usar o wrapper `./mvnw` incluso no projeto)
  - PostgreSQL 16 rodando localmente

### Perfis de configuração (`application.yaml`)

O projeto usa profiles do Spring. Com Jackson 3, a propriedade de datas é `spring.jackson.datatype.datetime.write-dates-as-timestamps` (antes ficava em `spring.jackson.serialization`).

- **dev** (padrão): conecta em `jdbc:postgresql://localhost:5432/correai`, com `ddl-auto: validate` (esquema gerido pelo Flyway) e SQL logado no console. Usa um segredo JWT de desenvolvimento; não use em produção.
- **prod**: espera as variáveis de ambiente `DB_URL`, `DB_USER`, `DB_PASSWORD` e `JWT_SECRET`, com `ddl-auto: validate`.

Variável opcional `CORS_ALLOWED_ORIGINS` (lista separada por vírgulas; padrão `http://localhost:3000,http://localhost:5173`) define as origens permitidas pelo CORS.

### Migrações de banco (Flyway)

O esquema é versionado em `src/main/resources/db/migration` (`V1__init_schema.sql`). Bancos já existentes criados pelo `ddl-auto` são adotados automaticamente (`baseline-on-migrate`). Alterações de esquema devem virar novas migrações `V<n>__descricao.sql`. Os testes usam H2 com `create-drop` e o Flyway desabilitado.

### Rodando com Docker Compose (recomendado)

```bash
# build + subir os containers (API + Postgres)
docker compose up --build

# subir em background
docker compose up -d

# subir apenas o banco
docker compose up postgres
```

A API ficará disponível em `http://localhost:8080` e o Postgres do container é exposto na porta `5432` do host.

> **Nota (Linux):** dependendo da configuração do Docker no seu sistema, pode ser necessário usar `sudo docker compose ...`.

**Outros comandos úteis:**
```bash
# parar os containers
docker compose down

# parar e remover volumes (apaga os dados do banco)
docker compose down -v

# reiniciar todos os serviços
docker compose restart

# reiniciar apenas um serviço
docker compose restart api

# ver logs
docker compose logs
docker compose logs -f
docker compose logs -f api
docker compose logs -f postgres

# acessar o shell do container da API
docker compose exec api sh

# acessar o psql dentro do container do Postgres
docker compose exec postgres psql -U correai

# limpeza geral (containers, volumes e imagens não usadas)
docker compose down -v
docker system prune -a
```

### Rodando localmente sem Docker

1. Suba um Postgres 16 local (ou use o container apenas do banco: `docker compose up postgres`).
2. Ajuste, se necessário, as credenciais em `src/main/resources/application.yaml` (profile `dev`).
3. Execute a aplicação:
   ```bash
   ./mvnw spring-boot:run
   ```

### Build do projeto

```bash
./mvnw clean package
```

O artefato gerado ficará em `target/api-0.0.1-SNAPSHOT.jar`.

O ambiente local precisa apontar `JAVA_HOME` para um JDK 25. O build da imagem
Docker executa `mvn clean verify` com Temurin 25 antes de criar a imagem de
runtime.

### Segurança das dependências

O projeto está no Spring Boot 4.1.1, que corrige as CVEs do Spring Framework 6.2, Spring Data JPA e Micrometer que não tinham patch na linha 3.5. O `pom.xml` mantém overrides de versões gerenciadas pelo BOM para corrigir CVEs ainda abertas nelas:

- `tomcat.version` = `11.0.26`
- `jackson-bom.version` = `3.1.7`
- `jackson-2-bom.version` = `2.21.7` (Jackson 2 trazido pelo springdoc)
- `logback.version` = `1.6.5`

Revise esses overrides a cada atualização do Spring Boot e remova-os quando o BOM já trouxer versões corrigidas. A última varredura no OSV.dev das 121 dependências resolvidas não encontrou vulnerabilidades.

### Rodando os testes

```bash
./mvnw test
```

Os testes utilizam **H2** em memória, isolando-os de uma instância real do Postgres.

## Estrutura de pastas (resumo)

```
correai-api/
├── docker-compose.yml
├── Dockerfile
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/correai/api/...
│   │   └── resources/application.yaml
│   └── test/
│       ├── java/com/correai/api/...
│       └── resources/application.yaml
```
