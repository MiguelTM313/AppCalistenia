# Calistenia em Casa

Aplicativo Android nativo, offline-first, que transforma perfil funcional, objetivos, disponibilidade, equipamento e histórico em uma prescrição explicável. O produto não usa um “nível geral”: `PUSH`, `PULL`, `LEGS`, `CORE`, `MOBILITY` e `CONDITIONING` evoluem independentemente.

> **Segurança:** o aplicativo não é dispositivo médico, não diagnostica lesões ou doenças e não substitui médico, fisioterapeuta ou profissional de Educação Física. Dor aguda, dor torácica, desmaio/tontura importante ou falta de ar incomum exigem interrupção da atividade e avaliação adequada. Nunca é recomendado treinar através da dor.

## Estado do primeiro incremento

Esta base entrega o caminho persistente principal: triagem → perfil → avaliação por padrão → plano semanal → sessão → registro de séries → histórico. Há 36 exercícios em seed Room, seleção compatível com nível/equipamento, otimização de tempo e motores puros testáveis. Consulte [Arquitetura](docs/ARCHITECTURE.md) e [Motor de treino](docs/TRAINING_ENGINE.md).

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

## Compilar e testar

Pré-requisitos: JDK 17, Gradle 8.14.x e Android SDK 35 configurado em `ANDROID_HOME` ou `local.properties`.

O Gradle Wrapper não é versionado porque a interface de pull requests utilizada neste projeto não aceita arquivos binários e `gradle-wrapper.jar` é binário. Instale a versão indicada de Gradle e execute:

```bash
gradle test
gradle :app:assembleDebug
```

O ambiente inicial deste repositório não possuía Android SDK e bloqueava download de artefatos externos; portanto uma máquina de desenvolvimento deve sincronizar as dependências antes da primeira compilação Android.

## Roadmap explícito

Este incremento prioriza a fundação e não declara o produto completo. Próximos blocos:

1. ampliar onboarding para sexo opcional, objetivos secundários, preferências, limitações, local, frequência e seleção completa de dias/equipamentos;
2. substituir os sliders de autoavaliação por protocolos guiados, escolha automática de testes e bloqueios detalhados da triagem;
3. carregar todo o histórico detalhado no contexto do gerador e aplicar de fato decisões do `ProgressionEngine` à próxima prescrição;
4. completar readiness pré-treino, desconforto por série, substituição, descanso cronometrado e retomada de sessão interrompida;
5. persistir versões imutáveis de replanejamento e integrar `ScheduleRebalancer` à navegação semanal;
6. telas dedicadas de detalhe de exercício, perfil, configurações, resultados de avaliação e dashboard completo;
7. milestones, reavaliações periódicas, exportação/importação e acessibilidade/testes instrumentados;
8. integração opcional futura com Health Connect e, separadamente, análise de movimento por câmera.

WorkManager, backend, autenticação, internet, Health Connect e câmera não fazem parte desta fase. O manifesto não solicita permissão de internet.
