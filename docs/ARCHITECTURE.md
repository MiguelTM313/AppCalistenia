# Arquitetura

## Decisão central

O motor vive em `:domain`, um módulo Kotlin/JVM sem Android, Compose, Room ou Activity. Isso mantém regras determinísticas e executáveis em testes rápidos. `:app` contém adaptadores de persistência e apresentação.

## Fluxo de dependências

```text
Compose → AppViewModel → AppRepository → Room/DataStore
                    ↘ casos de uso/motores → modelos de domínio
```

A UI observa um único `StateFlow<AppUiState>` e envia intenções ao ViewModel. Composables não acessam DAO nem tomam decisões de treinamento. O repositório executa transações que preservam a separação entre planejado e realizado.

## Persistência

- **Room:** perfil, níveis/avaliações, exercícios, planos, sessões planejadas, sessões executadas, exercícios executados, séries e medições.
- **DataStore:** tema e unidade; nunca histórico complexo.
- **Seed:** `ExerciseSeed` é idempotente e separado do motor. Progressões são ligadas por IDs.

O banco está na versão 3. `MIGRATION_1_2` continua criando `app_setup` e as quatro dimensões de readiness. `MIGRATION_2_3` acrescenta `completionStatus` ao exercício executado, `status`/`recordedAt` às séries e unicidade para `(workoutId, orderIndex)` e `(exerciseSessionId, setIndex)`. As migrations apenas alteram tabelas/índices: histórico e readiness não são apagados. Schemas exportados são versionados em `app/schemas`.

Iniciar uma sessão é uma transação que adapta uma sessão ainda `PLANNED`, grava readiness/`startedAt`, cria as execuções de exercícios e muda o status. Confirmar ou editar uma série faz `upsert` pelo índice; pular grava cada posição restante como `SKIPPED`; finalizar apenas fecha a execução já acumulada. `AppViewModel` seleciona a sessão e observa `WorkoutWithExercises` por `Flow`, portanto o Compose mantém somente campos ainda não confirmados.

`completionStatus` distingue `PENDING`, `PARTIAL`, `COMPLETED` e `FULLY_SKIPPED`. Um exercício parcial mantém seus logs concluídos no histórico; séries `SKIPPED` não contam como volume, mas sintomas das séries concluídas continuam chegando ao motor.

## Segurança

Triagem e mensagens não produzem diagnóstico. `ProgressionEngine` bloqueia progressão após sintomas críticos registrados. O player registra desconforto por série; dor aguda, tontura ou falta de ar inesperada bloqueiam progressão automática.

## Escolhas deliberadas

- injeção manual pequena em `Application`, evitando framework DI antes de haver complexidade que o justifique;
- IDs estáveis de catálogo e datas no domínio com `java.time` (minSdk 28);
- regras explicáveis: toda prescrição inclui `rationale` e todo resultado de progressão inclui `reason`;
- sem WorkManager: ainda não há trabalho confiável em segundo plano;
- sem permissão `INTERNET`: todas as funções essenciais são locais.
