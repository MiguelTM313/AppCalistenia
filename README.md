# Calistenia em Casa

Versão **1.1** (`versionCode = 3`), para uso pessoal. Após instalar, o app aparece como **Calistenia em Casa**, com ícone verde de um atleta sob um telhado. O ícone é adaptativo e oferece camada monocromática para os temas do Android 13 ou superior.

### Novidade da versão 1.1

Cada seção “Como fazer”, na biblioteca e no treino, oferece uma demonstração em vídeo do exercício. São 36 vídeos curtos, com autoria identificada, carregados somente ao tocar em “Ver vídeo”. É necessário acesso à internet; os vídeos são reproduzidos pelo YouTube e podem conter anúncios e áudio em outro idioma. Há nova tentativa e opção de abrir o vídeo original se o player não carregar. Ao fechar ou deixar o app em segundo plano, a reprodução para.

O catálogo de vídeos usa os IDs existentes dos exercícios e funciona também em instalações com planos já salvos, sem migrar ou recriar o banco. Instruções, treino e histórico continuam locais e funcionam sem internet. Consulte [Vídeos e validação](docs/EXERCISE_VIDEOS.md).

Para atualizar preservando os treinos, compile usando a mesma chave da instalação anterior. APKs debug gerados em máquinas/execuções diferentes do CI podem ter assinaturas diferentes; nesse caso o Android recusa a atualização. **Não desinstale o app para contornar esse erro se já houver registros que queira manter.** O pacote continua `com.calistenia.app` e o banco permanece no schema v3.

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

### Interface para treino pessoal

A navegação principal separa Hoje, Plano, Histórico e Exercícios. O cadastro tem três etapas com validação numérica, escolha dos dias da semana e equipamentos; os rascunhos de tela usam estado restaurável. A avaliação inicial continua sendo uma autoestimativa, agora com referências do catálogo.

O player mostra a série atual, alvo, contador de repetições/segundos, cronômetro opcional para séries por tempo, RIR explicado e desconfortos em português. O descanso deriva do horário persistido da última série e não reinicia ao retomar. Encerrar antecipadamente preserva as séries realizadas e marca o restante como não realizado, com confirmação. Não é permitido iniciar uma segunda sessão enquanto outra estiver em andamento.

O histórico permite abrir as séries, técnica e sintomas. A biblioteca tem busca, filtros e detalhes textuais. Falhas de operações aparecem em todas as telas; a inicialização oferece nova tentativa. O CI executa também testes Compose/Robolectric em tela de 360dp e fonte ampliada, e disponibiliza o APK debug e relatórios como artefatos por 7 dias. Capturas de UI ficam em `app/build/ui-screenshots` durante os testes. A instalação em aparelho real continua sendo uma verificação complementar.

Os registros de treino permanecem locais; desinstalar ou limpar os dados pode apagar o histórico. Os vídeos carregam conteúdo externo do YouTube, sujeito aos anúncios e à política de privacidade do serviço. Esta versão não inclui monetização própria, serviços pagos ou publicação em loja.

Este incremento prioriza a fundação e não declara o produto completo. Próximos blocos:

1. ampliar perfil para sexo opcional, objetivos secundários, preferências e limitações (dias, frequência e equipamentos já selecionáveis);
2. substituir a autoestimativa por protocolos guiados e reavaliações;
3. oferecer substituição avançada e edição visual de séries antigas (encerramento antecipado já está disponível);
4. persistir versões imutáveis de replanejamento e expor `ScheduleRebalancer` com confirmação na navegação semanal;
6. edição de perfil/configurações (vídeos, biblioteca, resumo e histórico detalhado já disponíveis);
7. milestones, reavaliações periódicas, exportação/importação e acessibilidade/testes instrumentados;
8. integração opcional futura com Health Connect e, separadamente, análise de movimento por câmera.

WorkManager, backend, autenticação, Health Connect e câmera não fazem parte desta fase. A permissão de internet é usada para os vídeos; o app não envia perfil ou histórico ao player.
