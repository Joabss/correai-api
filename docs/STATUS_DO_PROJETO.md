# Status do projeto CorreAI

Atualizado em 2026-11-24 a partir das notas em
`/data/Obsidian/MeuSegundoCerebro/CorreAI` e do codigo presente neste repositorio.

## Resumo

O projeto esta na **Fase 1 - Backend**, com o nucleo inicial parcialmente
concluido. A API ja registra e lista atividades, calcula o resumo estatistico e
resolve um usuario anonimo ou nomeado. A migracao estrutural para arquitetura hexagonal foi
realizada e o build em Java 25 foi validado.

O MVP descrito nas notas ainda nao esta completo: evolucao, metas, planejamento,
badges, mobile e validacao ponta a ponta permanecem pendentes.

## Progresso por fase

| Fase | Estado | Evidencia ou pendencia |
| --- | --- | --- |
| 0 - Preparacao | Parcial | Produto e escopo documentados; nao ha repositorio mobile neste workspace. |
| 1 - Backend | Em andamento | User, Activity, atividades, stats basicos e usuario anonimo implementados. |
| 2 - Mobile | Nao iniciado aqui | Nenhum projeto Flutter neste workspace. |
| 3 - Qualidade | Parcial | 89 testes passam em Java 25; faltam fluxo ponta a ponta. |
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
| Erros padronizados (ProblemDetail), CORS e OpenAPI | Implementados |
| Refresh token, recuperacao de senha e conversao de anonimo em conta | Implementados (`POST /auth/refresh`, `POST /auth/password-reset/request|confirm`, `POST /users/me/account`; tokens de uso unico em `one_time_tokens`, migracao `V5`); login com protecao contra enumeracao por tempo de resposta |
| Migracoes Flyway | Implementadas (`V1` a `V5`) |
| `GET /activities/{id}` | Implementado |
| Excluir atividade (`DELETE /activities/{id}`) | Implementado |
| Editar atividade | Pendente |
| Metas (`/goals`): km, atividades e pace, semanal/mensal, varias ativas | Implementado |
| `GET /stats/evolution` | Implementado (semanal, `weeks` de 1 a 52) |
| PlannedActivity | Pendente |
| Badge/UserBadge | Pendente |

## Arquitetura e plataforma

- Estrutura atual: `domain/model`, `domain/port/in`, `domain/port/out` (ambos por
  feature), `application`, `config`, `adapter/in` e `adapter/out`.
- Dominio sem dependencias de Spring, JPA ou Lombok; `User` e `Activity` sao records
  (`User` tem `name` e `email` opcionais: e-mail nulo indica usuario anonimo).
- Services de aplicacao sem anotacoes Spring, registrados em `config/BeanConfig`.
- `Clock` injetado e calculo de streak unificado em `StreakCalculator`.
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

1. Implementar planejamento e badges.
2. Iniciar o mobile com onboarding, registro, home e historico conectados a API.

## Criterio para encerrar a Fase 1

- Build e testes executados em Java 25 no CI e no Docker.
- Fronteiras hexagonais verificadas automaticamente (feito com ArchUnit).
- Banco criado e atualizado por migracoes versionadas (feito com Flyway).
- Identidade anonima nao permite acesso por UUID arbitrario (feito com JWT).
- Persistencia validada contra PostgreSQL (feito com Testcontainers).
- Endpoints de atividades, resumo, evolucao e metas implementados (feito) e
  documentados.