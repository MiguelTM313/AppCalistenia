# Calistenia em Casa

Aplicativo Android nativo, offline-first, que transforma perfil funcional, objetivos, disponibilidade, equipamento e histórico em uma prescrição explicável. O produto não usa um “nível geral”: `PUSH`, `PULL`, `LEGS`, `CORE`, `MOBILITY` e `CONDITIONING` evoluem independentemente.

> **Segurança:** o aplicativo não é dispositivo médico, não diagnostica lesões ou doenças e não substitui médico, fisioterapeuta ou profissional de Educação Física. Dor aguda, dor torácica, desmaio/tontura importante ou falta de ar incomum exigem interrupção da atividade e avaliação adequada. Nunca é recomendado treinar através da dor.

## Estado do ciclo adaptativo

O caminho persistente fecha o ciclo: triagem → perfil → avaliação → plano → início real → séries com RIR, técnica e desconforto → histórico reconstruído → decisão de progressão → próxima prescrição. Treinos rápidos de 10–30 minutos usam o mesmo histórico sem substituir a semana. Há 36 exercícios em seed Room. Consulte [Arquitetura](docs/ARCHITECTURE.md) e [Motor de treino](docs/TRAINING_ENGINE.md).

## Stack e arquitetura

- Kotlin, Jetpack Compose e Material 3;
- Navigation Compose, ViewModel, Coroutines e StateFlow;
- Room para perfil, avaliações, catálogo, planos e histórico;
- DataStore somente para preferências simples;
- Gradle Kotlin DSL e Version Catalog;
- módulo `domain` Kotlin/JVM, independente de Android; módulo `app` em camadas `ui` e `data`.

```text
domain/                         modelos, motores e casos de uso puros
app/src/main/kotlin/.../data/  Room, seed, repositório e DataStore
app/src/main/kotlin/.../ui/    Compose, navegação e ViewModel
docs/                          decisões e regras do motor
```

## Entidades principais

`UserProfile`, `FunctionalProfile`, `Exercise`, `TrainingPlan`/`PlannedSession`, `WorkoutSession`, `ExerciseSession`, `SetLog`, `AssessmentResult` e `Readiness`. O histórico realizado é armazenado separadamente do plano, de modo que uma futura reprogramação não o sobrescreva.

### Persistência incremental e schema Room v3

O início cria a execução e seus exercícios; cada confirmação de série, edição confirmada e pulo do restante é uma transação Room. A chave única `(exerciseSessionId, setIndex)` torna a gravação idempotente. O player observado pelo ViewModel recompõe séries, próxima série, readiness e `startedAt` do banco, inclusive após encerramento do processo.

O schema v3 acrescenta estado explícito a exercícios e séries, horário do registro e índices únicos. `MIGRATION_2_3` preserva os logs/readiness existentes, e `MIGRATION_1_2` permanece disponível para o caminho `1 → 2 → 3`; não há migração destrutiva. Os snapshots textuais ficam em `app/schemas`.

## Compilar e testar

Pré-requisitos: JDK 17, Gradle 8.14.x e Android SDK 35 configurado em `ANDROID_HOME` ou `local.properties`.

O Gradle Wrapper não é versionado porque a interface de pull requests utilizada neste projeto não aceita arquivos binários e `gradle-wrapper.jar` é binário. Instale a versão indicada de Gradle e execute:

```bash
gradle :domain:test
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
```

O ambiente inicial deste repositório não possuía Android SDK e bloqueava download de artefatos externos; portanto uma máquina de desenvolvimento deve sincronizar as dependências antes da primeira compilação Android.

O CI instala JDK 17, Android SDK e Gradle 8.14.3. O Wrapper continua ausente porque o fluxo atual não aceita seu JAR binário; deverá ser restaurado por um fluxo Git/GitHub que aceite binários, sem ignorar genericamente `*.jar`.

## Limitações e roadmap explícito

Este incremento prioriza a fundação e não declara o produto completo. Próximos blocos:

1. ampliar onboarding para sexo opcional, objetivos secundários, preferências, limitações, local, frequência e seleção completa de dias/equipamentos;
2. substituir os sliders de autoavaliação por protocolos guiados, escolha automática de testes e bloqueios detalhados da triagem;
3. oferecer substituição avançada e controles completos de cancelamento da sessão (a confirmação de uma série já pode sobrescrever idempotentemente o mesmo índice);
4. persistir versões imutáveis de replanejamento e expor `ScheduleRebalancer` com confirmação na navegação semanal;
6. telas dedicadas de detalhe de exercício, perfil, configurações, resultados de avaliação e dashboard completo;
7. milestones, reavaliações periódicas, exportação/importação e acessibilidade/testes instrumentados;
8. integração opcional futura com Health Connect e, separadamente, análise de movimento por câmera.

WorkManager, backend, autenticação, internet, Health Connect e câmera não fazem parte desta fase. O manifesto não solicita permissão de internet.
