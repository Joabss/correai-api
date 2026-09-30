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

Não há autenticação tradicional: a identificação do usuário é feita via header `X-User-Id`. Caso o header não seja enviado, a API cria automaticamente um usuário anônimo e devolve o novo `X-User-Id` na resposta, que deve ser reutilizado pelo cliente nas próximas chamadas.

## Stack Tecnológica

- **Java 25 LTS**
- **Spring Boot 4.1.1** (Spring Framework 7, Jakarta EE 11, Hibernate 7, Tomcat 11, Jackson 3)
  - Spring Web MVC (`spring-boot-starter-webmvc`, REST controllers)
  - Spring Data JPA (persistência)
  - Spring Validation (Bean Validation / `jakarta.validation`)
  - Spring Boot Actuator (health checks / observabilidade)
- **PostgreSQL 16** (banco de dados principal)
- **H2 Database** (em memória, usado apenas nos testes)
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

Um `HandlerInterceptor` (`UserContextInterceptor`) intercepta todas as requisições para `/activities/**` e `/stats/**`:

1. Verifica se o header `X-User-Id` foi enviado.
2. Se não foi enviado, cria um `User` anônimo no banco e retorna o novo id no header `X-User-Id` da resposta.
3. Se foi enviado, apenas propaga o `userId` como atributo da requisição (`@RequestAttribute("userId")` nos controllers).

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
| GET    | `/activities`       | Lista paginada das atividades (`?page=0&size=20`, máx. 100) |
| POST   | `/activities`       | Cria uma nova atividade                      |
| GET    | `/stats/summary`    | Retorna o resumo estatístico do usuário      |
| GET    | `/actuator/health`  | Health check da aplicação                    |

Todas as rotas de negócio exigem (ou geram automaticamente) o header `X-User-Id`.

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
3. Execute primeiro qualquer request de `Activities` ou `Stats` sem preencher a variável `userId` — a API criará um usuário anônimo automaticamente e um script de teste na request "Criar atividade (RUN)" já captura o `X-User-Id` da resposta e o salva na variável `userId` da collection.
4. As próximas requisições reutilizarão esse `userId` automaticamente.

A collection contém as pastas:
- **Activities**: listar, criar atividade (RUN/WALK) e um exemplo de payload inválido para testar as validações.
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

- **dev** (padrão): conecta em `jdbc:postgresql://localhost:5432/correai`, com `ddl-auto: update` e SQL logado no console.
- **prod**: espera as variáveis de ambiente `DB_URL`, `DB_USER` e `DB_PASSWORD`, com `ddl-auto: validate`.

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
