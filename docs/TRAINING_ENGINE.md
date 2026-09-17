# Motor de treinamento

## Da entrada ao plano

`GenerationContext` agrega perfil, objetivos, disponibilidade, equipamento, perfil funcional, catálogo, histórico recente e readiness opcional. O `TrainingPlanGenerator`:

1. escolhe um template pela frequência (corpo inteiro em 2–3 dias, superior/inferior em 4, padrões específicos a partir de 5);
2. calcula cada nível funcional independentemente;
3. filtra exercícios ativos cujo equipamento obrigatório está disponível e cujo nível mínimo não excede o nível atual mais uma margem de entrada;
4. ordena candidatos deterministicamente, favorecendo adequação de dificuldade e continuidade recente;
5. avalia o exercício atual e centraliza a validação de qualquer avanço/regressão (atividade, padrão, equipamento, nível mínimo e salto máximo de dificuldade), mantendo a variação atual com justificativa quando inválida;
6. cria séries/faixas e uma justificativa textual com a decisão;
7. entrega cada sessão ao `SessionTimeOptimizer`.

O otimizador estima aquecimento/finalização, esforço por série, descanso e transições. Para um orçamento curto ele reconstrói a sessão: preserva primeiro os padrões prioritários, remove acessórios e então reduz séries. Readiness baixo reduz volume, nunca aumenta agressivamente a prescrição.

## Realizado → próxima prescrição

O plano e a execução usam tabelas distintas. Uma conclusão cria `WorkoutSession`, `ExerciseSession` e `SetLog`, guardando alvo e valor realizado, RIR, técnica e desconforto. O `ProgressionEngine` aplica dupla progressão por exercício:

- **progredir:** topo da faixa, técnica boa e ao menos uma repetição em reserva em duas ou mais exposições;
- **manter:** dentro da faixa ou evidência ainda insuficiente;
- **regredir/deload:** repetidamente abaixo do mínimo ou com técnica inadequada;
- **bloquear:** dor aguda, tontura ou falta de ar inesperada.

Uma única sessão excepcional não promove o exercício e falha muscular não é requisito. Cada decisão contém um motivo legível e alimenta a geração seguinte. Progressões incompatíveis com equipamento ou nível funcional são rejeitadas, mantendo a variação atual e explicando a razão.

Séries são persistidas na confirmação, antes de avançar o player. Pular depois de executar parte do exercício marca somente as posições restantes como não realizadas; volume considera apenas logs `COMPLETED`, enquanto dor aguda/tontura/falta de ar presentes nesses logs continuam bloqueando progressão.

Readiness é aplicado uma única vez, na transição `PLANNED → IN_PROGRESS`. Score baixo limita séries por regras determinísticas do `SessionTimeOptimizer` e registra a justificativa; normal ou alto não aumenta a dificuldade. Retomadas reutilizam readiness, prescrição e `startedAt` persistidos, sem nova adaptação; sessões concluídas não são mutáveis.

## Treino rápido

`GenerateQuickWorkoutUseCase` recebe as sessões do plano semanal e limites explícitos de segunda a domingo. Primeiro prioriza padrões planejados ainda sem cobertura, depois menor volume efetivamente executado na semana, e finalmente ordem/prioridade e orçamento do `SessionTimeOptimizer`. A sessão rápida é persistida nas mesmas tabelas e soma ao mesmo histórico, mas seu plano separado não substitui nem altera o plano semanal.

## Reorganização semanal

`ScheduleRebalancer` preserva concluídas, marca oportunidades passadas não realizadas como `SKIPPED` e reordena apenas as futuras, priorizando padrões perdidos. IDs históricos não são apagados. A integração visual e versões imutáveis do replanejamento permanecem futuras.

## Invariantes testados

- equipamento ausente nunca é recomendado;
- exercícios muito acima do nível são excluídos;
- orçamento de 10 minutos preserva prioridades e fica próximo do alvo;
- progressão exige exposições repetidas;
- desempenho insuficiente pode regredir;
- sintomas críticos impedem progressão;
- pulo parcial preserva séries realizadas e sintomas;
- readiness baixo reduz somente a sessão iniciada e não é reaplicado;
- treino rápido usa cobertura da semana programada, não uma janela móvel aproximada;
- reequilíbrio não exclui sessões anteriores.

## Limitações restantes

Ainda não há substituição avançada de exercícios nem cancelamento completo de uma sessão em andamento. Health Connect, câmera, backend, autenticação e replanejamento visual/versionado continuam fora deste incremento.
