# Motor de treinamento

## Da entrada ao plano

`GenerationContext` agrega perfil, objetivos, disponibilidade, equipamento, perfil funcional, catálogo, histórico recente e readiness opcional. O `TrainingPlanGenerator`:

1. escolhe um template pela frequência (corpo inteiro em 2–3 dias, superior/inferior em 4, padrões específicos a partir de 5);
2. calcula cada nível funcional independentemente;
3. filtra exercícios ativos cujo equipamento obrigatório está disponível e cujo nível mínimo não excede o nível atual mais uma margem de entrada;
4. ordena candidatos deterministicamente, favorecendo adequação de dificuldade e continuidade recente;
5. cria séries/faixas e uma justificativa textual;
6. entrega cada sessão ao `SessionTimeOptimizer`.

O otimizador estima aquecimento/finalização, esforço por série, descanso e transições. Para um orçamento curto ele reconstrói a sessão: preserva primeiro os padrões prioritários, remove acessórios e então reduz séries. Readiness baixo reduz volume, nunca aumenta agressivamente a prescrição.

## Realizado → próxima prescrição

O plano e a execução usam tabelas distintas. Uma conclusão cria `WorkoutSession`, `ExerciseSession` e `SetLog`, guardando alvo e valor realizado, RIR, técnica e desconforto. O `ProgressionEngine` aplica dupla progressão por exercício:

- **progredir:** topo da faixa, técnica boa e ao menos uma repetição em reserva em duas ou mais exposições;
- **manter:** dentro da faixa ou evidência ainda insuficiente;
- **regredir/deload:** repetidamente abaixo do mínimo ou com técnica inadequada;
- **bloquear:** dor aguda, tontura ou falta de ar inesperada.

Uma única sessão excepcional não promove o exercício e falha muscular não é requisito. Cada decisão contém um motivo legível. A integração completa dessas decisões na geração seguinte está registrada no roadmap; neste incremento o histórico já é persistido e o motor é isoladamente testado.

## Reorganização semanal

`ScheduleRebalancer` mantém sessões passadas/concluídas e reordena apenas as oportunidades futuras, priorizando padrões perdidos. IDs históricos não são apagados. A próxima etapa adicionará persistência versionada de cada replanejamento e uma penalidade explícita para padrões redundantes em dias consecutivos.

## Invariantes testados

- equipamento ausente nunca é recomendado;
- exercícios muito acima do nível são excluídos;
- orçamento de 10 minutos preserva prioridades e fica próximo do alvo;
- progressão exige exposições repetidas;
- desempenho insuficiente pode regredir;
- sintomas críticos impedem progressão;
- reequilíbrio não exclui sessões anteriores.
