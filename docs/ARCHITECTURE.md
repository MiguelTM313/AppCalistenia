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

O banco começa na versão 1. Alterações futuras exigirão migrations explícitas e schemas versionados. Dados pessoais permanecem locais. A interface `HealthDataGateway` é um ponto opcional de integração futura e sua ausência não afeta o funcionamento.

## Segurança

Triagem e mensagens não produzem diagnóstico. `ProgressionEngine` bloqueia progressão após sintomas críticos registrados. O próximo incremento deve tornar o registro de todos os desconfortos parte obrigatória do fluxo do player e persistir a decisão de bloqueio para revisão.

## Escolhas deliberadas

- injeção manual pequena em `Application`, evitando framework DI antes de haver complexidade que o justifique;
- IDs estáveis de catálogo e datas no domínio com `java.time` (minSdk 28);
- regras explicáveis: toda prescrição inclui `rationale` e todo resultado de progressão inclui `reason`;
- sem WorkManager: ainda não há trabalho confiável em segundo plano;
- sem permissão `INTERNET`: todas as funções essenciais são locais.
