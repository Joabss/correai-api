# Status do projeto CorreAI

Atualizado em 2026-11-28 a partir das notas em
`/data/Obsidian/MeuSegundoCerebro/CorreAI` e do codigo presente neste repositorio.

## Resumo

O projeto esta na **Fase 1 - Backend**, com o nucleo inicial parcialmente
concluido. A API ja registra e lista atividades, calcula o resumo estatistico e
resolve um usuario anonimo ou nomeado. A migracao estrutural para arquitetura hexagonal foi
realizada e o build em Java 25 foi validado.

O MVP descrito nas notas ainda nao esta completo: planejamento, mobile e
validacao ponta a ponta permanecem pendentes. Evolucao, metas, desafios,
recordes GPS e badges de conquistas ja tem implementacao no backend.

## Progresso por fase

| Fase | Estado | Evidencia ou pendencia |
| --- | --- | --- |
| 0 - Preparacao | Parcial | Produto e escopo documentados; nao ha repositorio mobile neste workspace. |
| 1 - Backend | Em andamento | User, Activity, atividades, stats basicos e usuario anonimo implementados. |
| 2 - Mobile | Nao iniciado aqui | Nenhum projeto Flutter neste workspace. |
| 3 - Qualidade | Parcial | Suites unitarias e de integracao passam no Java 25; faltam fluxo ponta a ponta. |
| 4 - Pre-lancamento | Nao iniciado | Deploy, release mobile e materiais de loja pendentes. |
| 5 - MVP pronto | Nao atingido | Depende das capacidades e validacoes abaixo. |

## Capacidades do backend

| Capacidade | Estado |
| --- | --- |
| `POST /activities` | Implementada |
| `GET /activities` | Implementada |
| `GET /stats/summary` | Implementada |
| Calculo de pace | Implementado |
| Km semanal e mensal | Implementado |
| Streak e maior distancia | Implementados em versao basica |
| Usuario anonimo | Implementado com JWT assinado (`POST /auth/anonymous`) |
| Usuario com e-mail/senha | Implementado (`POST /users` cadastra com nome, e-mail e senha BCrypt e retorna JWT; `POST /auth/login` autentica; migracoes `V3` e `V4`; `GET /users/me` retorna o usuario autenticado; migracao `V3__add_user_name.sql`) |
| Edicao de perfil, fotos e percurso | Implementados (`PUT /users/me`; `PUT/GET /users/me/photo`, `PUT/GET /activities/{id}/photo` e `/activities/{id}/route`; arquivos privados em PostgreSQL, migracao `V6`; GPX 1.0/1.1) |
| Varias fotos por atividade | Implementadas em 2026-11-27: `GET /activities/{id}/photos` e `PUT/GET /activities/{id}/photos/{photoId}`; UUID por foto, nova tentativa idempotente e fotos legadas preservadas; reutiliza `media_files`, sem nova migracao |
| Remocao de fotos da atividade | Implementada em 2026-11-27: `DELETE /activities/{id}/photos/{photoId}`; apenas proprietario, inclui foto legada, idempotente e sem alterar percurso |
| Erros padronizados (ProblemDetail), CORS e OpenAPI | Implementados |
| Refresh token, recuperacao de senha e conversao de anonimo em conta | Implementados (`POST /auth/refresh`, `POST /auth/password-reset/request|confirm`, `POST /users/me/account`; tokens de uso unico em `one_time_tokens`, migracao `V5`); login com protecao contra enumeracao por tempo de resposta |
| Migracoes Flyway | Implementadas (`V1` a `V10`; inicio da atividade em `V7`/`V8`, conquistas de desafio em `V9`, catalogo de badges em `V10`) |
| `GET /activities/{id}` | Implementado |
| Excluir atividade (`DELETE /activities/{id}`) | Implementado |
| Editar atividade | Implementado em 2026-11-27 (`PUT /activities/{id}`): apenas o proprietario; recalcula pace e preserva inicio, fotos, percurso e identificador |
| Metas (`/goals`): km, atividades e pace, semanal/mensal, varias ativas | Implementado |
| Desafios com conquistas permanentes | Implementados em 2026-11-27: snapshot em `goals` (`V9`), `GET /goals/badges`; exclusao protegida com `409` e conquistas preservadas apos editar/apagar atividades. Imagem PNG de medalha removida. |
| PRs por trecho GPS | Implementados em 2026-11-27: `GET /stats/records`, top 3 por 5 km/10 km/21,0975 km/42,195 km, qualquer tipo de atividade; um melhor trecho por atividade, sem atravessar pausas; GPX antigo sem horario nao participa |
| `GET /stats/evolution` | Implementado (semanal, `weeks` de 1 a 52) |
| PlannedActivity | Pendente |
| Badges de conquistas (`/achievements/me`) | Implementados em 2026-11-28: catalogo fixo de 19 badges (`BadgeCode`), independente dos desafios; 15 com criterio computavel (distancias, sequencia de dias, km/mes, horario, recordes pessoais, metas semanais consecutivas, rotas exploradas), 4 bloqueados sem criterio ainda (`rei_da_montanha`, `chuva`, `em_grupo`, `campeao`); avaliados a cada criacao/edicao/exclusao de atividade; conquista e permanente (`user_badges`, `V10`) |

## Arquitetura e plataforma

- Estrutura atual: `domain/model`, `domain/port/in`, `domain/port/out` (ambos por
  feature), `application`, `config`, `adapter/in` e `adapter/out`.
- Dominio sem dependencias de Spring, JPA ou Lombok; `User` e `Activity` sao records
  (`User` tem `name` e `email` opcionais: e-mail nulo indica usuario anonimo).
- Services de aplicacao sem anotacoes Spring, registrados em `config/BeanConfig`.
- `Clock` injetado e calculo de streak unificado em `StreakCalculator`.
- Conquistas ficam no desafio, nao na atividade; atualizacoes atomicas impedem apagar ou
  substituir um badge registrado. As operacoes REST de atividade e conquista compartilham transacao.
- Calculo de melhores trechos no dominio (`BestEffortCalculator`); leitura GPX compartilhada
  em `application.media.GpxReader`, com protecao contra DTD/entidades externas.
- Criterios de conquistas e PRs confirmados pelo usuario em 2026-11-27; detalhes e limites
  documentados no README. O ranking e calculado sob demanda; seu custo cresce com o historico GPS.
- Regras ArchUnit (`HexagonalArchitectureTest`) e teste de persistencia com Testcontainers/PostgreSQL.
- `GET /activities` paginado (`page`, `size` ate 100).
- Persistencia separada em entidades e mapeadores JPA.
- Java 25 declarado no Maven e usado nos estagios de build e runtime do Docker.
- Spring Boot 4.1.1 (Spring Framework 7, Hibernate 7, Tomcat 11, Jackson 3), com
  overrides de `tomcat.version` e `jackson-bom.version` para CVEs abertas.

## Riscos prioritarios

1. A identidade anonima usa JWT HS256 com segredo unico; em producao, definir
   `JWT_SECRET` forte e planejar rotacao/refresh de tokens.
2. Os testes de persistencia usam H2 e nao cobrem diferencas do PostgreSQL.
3. `avgPaceSeconds` e persistido, embora as notas definam pace como derivado.

## Limitacoes conhecidas da autenticacao (ajustes futuros)

1. **Sem envio de e-mail:** `PasswordResetNotifierPort` tem apenas um adapter de log; o
   token so e logado se `app.security.password-reset.log-token=true`. Integrar um
   provedor (SMTP/SES/etc.) antes de usar a recuperacao em producao.
2. **Sem limpeza de tokens:** registros expirados/abandonados de `one_time_tokens` nao
   sao removidos; criar job agendado.
3. **JWT de acesso ainda com validade de 30 dias:** o refresh token traz pouco ganho
   ate reduzir o TTL do JWT (ex.: 15-60 min).
4. **App Flutter nao integrado:** o app ainda usa so `POST /auth/anonymous`; falta
   login, cadastro, refresh automatico, recuperacao de senha e conversao de conta.
5. **Senha limitada a 72 bytes (BCrypt):** a validacao usa numero de caracteres (8-72),
   entao senhas multibyte podem ser truncadas silenciosamente.
6. **Protecao de timing parcial:** o login e protegido, mas
   `/auth/password-reset/request` ainda pode diferir em tempo entre e-mail conhecido e
   desconhecido (ex.: envio sincrono); tornar assincrono.
7. **Sem rate limiting:** login, refresh e reset nao limitam tentativas por IP/conta.

## Proximo marco recomendado

Concluir a fundacao do backend antes de iniciar novas telas:

1. Implementar planejamento.
2. Iniciar o mobile com onboarding, registro, home e historico conectados a API.

## Criterio para encerrar a Fase 1

- Build e testes executados em Java 25 no CI e no Docker.
- Fronteiras hexagonais verificadas automaticamente (feito com ArchUnit).
- Banco criado e atualizado por migracoes versionadas (feito com Flyway).
- Identidade anonima nao permite acesso por UUID arbitrario (feito com JWT).
- Persistencia validada contra PostgreSQL (feito com Testcontainers).
- Endpoints de atividades, resumo, evolucao e metas implementados (feito) e
  documentados.