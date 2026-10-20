# TapAnchor - Spec

Um aperto de botão Bluetooth vira **um** toque num ponto fixo da tela.
Para quem joga no celular com controle, botão de selfie, apresentador ou teclado.

## Linguagem do domínio

| Termo | Significado |
|---|---|
| **Trigger** | O botão físico, identificado por `keyCode` + `scanCode` (o scanCode diferencia botões que o Android não reconhece, keyCode 0). |
| **Anchor** | O ponto fixo da tela (x, y em pixels de tela) onde o toque cai. |
| **Binding** | Um Trigger ligado a um Anchor. Um botão, um ponto. |
| **KeyPress** | Um evento de tecla que chegou: trigger, desceu/subiu, contagem de repetição, horário. |
| **TapGuard** | A regra que decide se um KeyPress vira toque. Só ela decide. |

## Invariantes de segurança (fixas no código, sem configuração)

- **I1 - Um aperto, um toque.** Só o evento de DESCIDA com repetição 0 gera toque. Segurar não repete.
- **I2 - Sem automação.** Não existe timer, fila, gravação, macro nem sequência. Todo toque nasce de um KeyPress.
- **I3 - Sem aleatoriedade.** O toque cai exatamente no Anchor.
- **I4 - Freio.** No máximo 10 toques por segundo, somando todos os botões; o excesso é descartado.
- **I5 - Um botão, um ponto.** Um Trigger tem no máximo um Binding; um KeyPress gera no máximo um toque.

## Comportamento

- Botão com Binding: o evento é consumido (a função original do botão some enquanto o serviço está ligado).
- Botão sem Binding: segue para o sistema normalmente.
- **Modo aprender:** o próximo botão apertado vira um Binding novo, com o Anchor no centro da tela, e o app entra no modo mover.
- **Modo mover:** os marcadores ficam arrastáveis e nenhum toque é disparado. "Travar" sai do modo.
- **Travado:** os marcadores ficam translúcidos e não recebem toque; o toque gerado atravessa para o jogo.
- **Detector:** a tela principal mostra o último botão recebido (nome, keyCode, scanCode).

## Fora do escopo (v1)

- Seguir a posição do cursor do mouse (o Android não expõe essa posição).
- Anchors diferentes por orientação de tela. Configure com a tela na orientação do jogo.
- Botões que já são clique de mouse (não chegam como tecla).

## Arquitetura

- `domain/` - Java puro, sem Android: Trigger, Anchor, Binding, KeyPress, TapGuard. Testado em `test/`.
- `infra/` - TapService (AccessibilityService: recebe teclas, desenha marcadores, dispara gesto) e BindingStore (SharedPreferences).
- `ui/` - MainActivity: estado do serviço, detector, lista de Bindings.
