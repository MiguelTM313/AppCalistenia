# Demonstrações — versão 1.1

O catálogo acompanha o app e associa os 36 IDs existentes a vídeos específicos. Não depende de reinserir o seed do Room. Não há downloads nem cópias dos vídeos: a reprodução usa o player oficial do YouTube, com autoria e acesso ao original.

## Comportamento

- Biblioteca e treino usam o mesmo componente em “Como fazer”.
- Nenhuma requisição ao YouTube é feita até abrir o vídeo. Reprodução exige tocar no play.
- Instruções, séries e histórico continuam disponíveis offline. Abrir/fechar o vídeo não grava nem avança séries.
- O player pausa ao ir para segundo plano e é destruído ao fechar; voltar não reinicia automaticamente a reprodução.
- Falhas do player e de conexão mostram orientação, nova tentativa e acesso ao original. A espera inicial tem limite de 20 segundos.
- Os vídeos externos podem exibir anúncios, usar dados de navegação e ter áudio em outro idioma. Não há monetização própria.

## Integração

WebView do Android com IFrame API; HTTPS, sem acesso a arquivos/conteúdo local nem tráfego misto. A ponte JavaScript recebe apenas sinais de prontidão/erro, sem ler registros ou executar ações do app. IDs são validados antes de entrar no HTML. Navegação da página principal é bloqueada; o acesso externo é uma ação explícita.

O HTML local usa loadDataWithBaseURL com https://com.calistenia.app/ para enviar a identidade do app no Referer/origin, conforme a [documentação oficial do YouTube](https://developers.google.com/youtube/terms/required-minimum-functionality#embedded-youtube-player-and-identity). O player tem 220dp de altura, controles e branding originais, sem autoplay. Nenhuma dependência de player ou serviço pago foi adicionada.

## Catálogo

Títulos/autores conferidos no oEmbed oficial e duração conferida nos metadados públicos do YouTube em 18/09/2026: aproximadamente 11–127 segundos. Disponibilidade futura e anúncios dependem do provedor. As variantes com banco, parede, elástico e fitas estão identificadas na interface.

| ID do exercício | Vídeo original | Autor |
| --- | --- | --- |
| wall_push_up | [Demonstração](https://www.youtube.com/watch?v=QpMTk21EmaM) | HASfit |
| incline_push_up | [Demonstração](https://www.youtube.com/watch?v=nptMG5hV90c) | Calixpert |
| knee_push_up | [Demonstração](https://www.youtube.com/watch?v=ybdJn-OwSww) | HASfit |
| push_up | [Demonstração](https://www.youtube.com/watch?v=chj5koXqjcw) | HASfit |
| decline_push_up | [Demonstração](https://www.youtube.com/watch?v=qhEfTHPO3aI) | HASfit |
| diamond_push_up | [Demonstração](https://www.youtube.com/watch?v=UiIMMoKnkWc) | HASfit |
| band_row | [Demonstração](https://www.youtube.com/watch?v=LSkyinhmA8k) | Get Healthy U · Chris Freytag |
| ring_row | [Demonstração](https://www.youtube.com/watch?v=sEAOZc77wk8) | CrossFit |
| australian_row | [Demonstração](https://www.youtube.com/watch?v=vXmX6u_raTs) | Wright Training |
| dead_hang | [Demonstração](https://www.youtube.com/watch?v=vG159HkLrhY) | Calixpert |
| scapular_pull_up | [Demonstração](https://www.youtube.com/watch?v=W7bcEoXlmOg) | Calixpert |
| pull_up | [Demonstração](https://www.youtube.com/watch?v=HRV5YKKaeVw) | CrossFit |
| chest_to_bar | [Demonstração](https://www.youtube.com/watch?v=PmdNNN8nLGI) | CrossFit |
| chair_squat | [Demonstração](https://www.youtube.com/watch?v=b7I5_cCeYY8) | Rehab Hero |
| bodyweight_squat | [Demonstração](https://www.youtube.com/watch?v=eAFSpUExcwc) | Calixpert |
| split_squat | [Demonstração](https://www.youtube.com/watch?v=tE1QpUMzo2w) | Rehab Hero |
| reverse_lunge | [Demonstração](https://www.youtube.com/watch?v=hwdGTe09_18) | Calixpert |
| bulgarian_split_squat | [Demonstração](https://www.youtube.com/watch?v=pfRlldgfGRQ) | Calixpert |
| assisted_pistol | [Demonstração](https://www.youtube.com/watch?v=LrA-UKXoQ4o) | Rehab Hero |
| pistol_squat | [Demonstração](https://www.youtube.com/watch?v=rJY-zB9h6YM) | HASfit |
| calf_raise | [Demonstração](https://www.youtube.com/watch?v=Bir4geFDeJw) | HASfit |
| dead_bug | [Demonstração](https://www.youtube.com/watch?v=UKOwvzv1zuw) | Calixpert |
| forearm_plank | [Demonstração](https://www.youtube.com/watch?v=MypRN5Q754o) | HASfit |
| side_plank | [Demonstração](https://www.youtube.com/watch?v=E5koNMd2Ic0) | Rehab Hero |
| hollow_hold | [Demonstração](https://www.youtube.com/watch?v=TuLnKCIf5xI) | Calixpert |
| bird_dog | [Demonstração](https://www.youtube.com/watch?v=pBOXOVDDiUM) | Calixpert |
| hanging_knee_raise | [Demonstração](https://www.youtube.com/watch?v=PpmXveEoNOI) | HASfit |
| shoulder_mobility | [Demonstração](https://www.youtube.com/watch?v=AVFrTWQKHJA) | Rehab Hero |
| ankle_dorsiflexion | [Demonstração](https://www.youtube.com/watch?v=dr1FYumuGC4) | Rehab Hero |
| hip_90_90 | [Demonstração](https://www.youtube.com/watch?v=F5owFeKgpoc) | Rehab Hero |
| thoracic_rotation | [Demonstração](https://www.youtube.com/watch?v=hMQqwTkGwvs) | Rehab Hero |
| wrist_prep | [Demonstração](https://www.youtube.com/watch?v=YdZ57sqKqqk) | Rehab Hero |
| deep_squat_hold | [Demonstração](https://www.youtube.com/watch?v=IHApHfNA2Ag) | Hinge Health |
| march_in_place | [Demonstração](https://www.youtube.com/watch?v=WmXNILMVHbY) | Diabetes.co.uk |
| low_impact_jacks | [Demonstração](https://www.youtube.com/watch?v=Bqy1xIXX2nc) | HASfit |
| mountain_climber | [Demonstração](https://www.youtube.com/watch?v=eJllA-pZlb8) | Calixpert |

## Verificação

Testes automatizados cobrem os 36 IDs, identificadores inválidos, configuração do embed, sinais de erro, abertura/fechamento nas duas telas, nova tentativa e retorno ao registro de séries em tela pequena e fonte ampliada. Testes Room e do motor de treino continuam obrigatórios no CI.

Robolectric não reproduz streaming real. Antes de distribuir em loja, conferir em aparelho/emulador com WebView atualizado: play/pausa, segundo plano, voltar, modo avião, nova tentativa, link externo e bloqueios regionais/incorporação. Esta versão é para teste pessoal; o APK debug precisa da mesma chave da instalação anterior para atualizar sem desinstalação.
