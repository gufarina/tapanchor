# TapAnchor — Spec v2 (Produto + Técnico)

> Versão do app: **2.6** (`versionCode 9`) · APK: `TapAnchor-2.6.apk`
> `minSdk 24` · `targetSdk 35` · Java 17 · Shizuku `13.1.5`
> Este documento substitui o `SPEC.md` v1. Ele descreve o que o app faz (produto) e como
> ele faz por dentro (técnico), incluindo a tela de posicionamento.

---

## 1. TL;DR

Um aperto de botão Bluetooth vira **um** toque exatamente num ponto escolhido da tela.
Qualquer aparelho que o Android enxergue como controle/teclado (controle de videogame, botão
de selfie, apresentador, pedal USB/Bluetooth, teclado) passa a funcionar como um **clicker
com posição** — igual clicar com o mouse num pixel fixo, mas disparado por um botão físico.

Não é automação: **cada toque nasce de um aperto real**. Um botão, um ponto, um toque.

---

## 2. O que é (produto)

### 2.1 O problema

A maioria dos jogos e apps de celular só reage a **toque na tela**. Controles, pedais e botões
físicos mandam **teclas** (ou cliques de mouse), não toques. Resultado: o botão físico não serve
para tocar no jogo. Pior: sem tratamento, o Android transforma botão de controle em "voltar" ou
move a setinha na tela.

Um pedal USB (aquele de transcrição) funciona porque o sistema operacional de PC aceita o clique
dele em qualquer lugar. No Android essa ponte não existe — o TapAnchor é essa ponte.

### 2.2 O que o app entrega

- Lê o botão direto do hardware, **antes** do Android usá-lo.
- Cada botão vira um **disparo** para um ponto fixo `(x, y)` da tela.
- O disparo é um **toque de verdade**, injetado no sistema no nível mais baixo (não é um clique
  "falso" de acessibilidade; é o mesmo caminho que o dedo percorre).
- Enquanto ativo, o botão **deixa de fazer a função original** (não vira "voltar", não mexe o
  cursor), porque o aparelho é tomado exclusivamente para o TapAnchor.

### 2.3 Para quem

Jogador de celular com controle · quem usa botão de selfie · quem usa apresentador/clicker ·
quem usa pedal ou teclado e quer acionar um botão na tela.

### 2.4 Analogia (a régua do produto)

> **É o mapeamento de botão de gamepad no Android, mas o destino do botão é um toque com
> coordenada** — em vez de destravar/inventar uma tecla do sistema, ele clica no ponto escolhido.

Igualzinho a: você escolhe uma posição (como posicionaria o mouse), o app planta ali uma zona de
toque, e cada acionamento no aparelho cai naquela zona — um clique único, legítimo, no lugar
certo.

---

## 3. Como se usa (fluxo do usuário)

1. **Instalar o Shizuku** (app gratuito e de código aberto) e iniciá-lo via Depuração sem fio.
   É o único pré-requisito — uma vez, e depois é só reabrir após reiniciar o celular.
2. **Permitir** o acesso pelo TapAnchor.
3. **Escolher o controle**: o app lista os aparelhos conectados; toque no seu.
4. **Ancorar um botão**: toque em `+ ANCORAR BOTÃO`, aperte o botão físico. Surge um círculo no
   centro da tela — o novo *Anchor*.
5. **Mover**: arraste o círculo com o dedo até o ponto desejado (o app pode estar sobre o jogo).
6. **Travar posição**: o círculo vira discreto e deixa de captar toque. A partir daí, cada aperto
   = um toque naquele ponto.
7. Para reposicionar, toque na etiqueta rosa `01 ✎` ao lado do círculo.

O app não lê a tela, não lê mensagens e não usa internet.

---

## 4. A tela de posicionamento (o "modo mover")

Esta é a parte que dá ao app o comportamento "de mouse": escolher um ponto com o dedo e fixá-lo.

### 4.1 Modelo de estados

| Estado | O que aparece | Marcador recebe toque? | Toques são disparados? | Como entra / sai |
|---|---|---|---|---|
| **Aprendendo** | Sinal "APERTE O BOTÃO" | — | Não (leitor pausado) | `+ ANCORAR BOTÃO` → aperta botão |
| **Movendo** (`editing`) | Círculos grandes, opacos, etiquetas `NN ✎`, botão `TRAVAR POSIÇÃO` | Sim (arrastável) | Não (leitor pausado) | aprende um botão novo, ou toca a etiqueta |
| **Travado** (padrão) | Círculos pequenos, translúcidos | Não (toque atravessa) | Sim | `TRAVAR POSIÇÃO` |

Regra dura: **enquanto `editing` ou `learning`, o leitor fica pausado** — nenhum toque é disparado
enquanto você posiciona. Assim o gesto de arrastar nunca vira clique acidental.

### 4.2 Desenho dos marcadores

- O marcador é um `View` circular (`GradientDrawable` oval, anel) adicionado via `WindowManager`.
- **Movendo**: diâmetro `40dp`, cor `#66000000`, anel rosa (`#FF2E88`), opacidade 100%, arrastável.
- **Travado**: diâmetro `28dp`, cor `#22000000`, anel âmbar (`#FFA51F`), opacidade 60%, **não
  tocável** (`FLAG_NOT_TOUCHABLE`) — é isso que deixa o toque gerado passar para o jogo embaixo.
- O centro do marcador é o Anchor: o marcador é desenhado em `x - size/2, y - size/2`, então o
  ponto tocado é exatamente o centro do círculo.

### 4.3 Arrastar (a matemática do movimento)

No `ACTION_DOWN` guarda o ponto bruto do dedo e a posição inicial da janela. No `ACTION_MOVE`:

```
lp.x = startX + (rawX - downRawX)
lp.y = startY + (rawY - downRawY)
```

Ou seja, o marcador acompanha o deslocamento do dedo. No `ACTION_UP`, lê a posição **real** do
marcador na tela com `getLocationOnScreen()` (que já contabiliza recorte, barras e a diferença de
coordenadas da janela) e grava o novo Anchor como `centro = loc[0] + size/2, loc[1] + size/2`.

Usar `getLocationOnScreen()` em vez de confiar no `lp.x/lp.y` é o que corrige o deslocamento de
janelas de sobreposição — a posição salva é o pixel que o usuário vê, não a coordenada interna da
janela.

### 4.4 Coordenadas

O Anchor é guardado em **pixels de tela** `(x, y)`, origem no canto superior esquerdo, no sistema
de coordenadas de sobreposição (`FLAG_LAYOUT_NO_LIMITS` + `FLAG_LAYOUT_IN_SCREEN`, com recorte e
barras contabilizados). O jogo precisa estar na mesma orientação em que você posicionou — o app
detecta rotação para redesenhar a interface, mas os Anchors são fixos em pixels e **não migram
sozinhos** entre retrato e paisagem.

---

## 5. Linguagem do domínio

| Termo | Significado |
|---|---|
| **Trigger** | O botão físico, pelo código lido do hardware (código de tecla do Linux/evdev). |
| **Anchor** | O ponto fixo da tela `(x, y)`, em pixels de tela, onde o toque cai. |
| **Binding** | Um Trigger ligado a um Anchor. Um botão, um ponto. |
| **KeyPress** | Um evento de botão que chegou: Trigger, desceu/subiu, contagem de repetição, horário. |
| **TapGuard** | A única regra que decide se um KeyPress vira toque. |
| **Grab** | A tomada exclusiva do aparelho (`EVIOCGRAB`): o Android deixa de vê-lo. |
| **Reader** | O processo privilegiado (via Shizuku) que lê o hardware e injeta o toque. |

> **Nota de nomenclatura (v1 → v2).** No código atual, o campo `Trigger.keyCode` guarda o
> **código de evento do kernel (evdev)**, não o `KeyEvent` do Android. `Names.of()` traduz esses
> códigos (ex.: `272` = clique esquerdo, `304` = botão A, `158` = voltar). O campo `scanCode`
> continua existindo no domínio, mas **no caminho de hardware ele é sempre `0`** — ver §8.6.

---

## 6. Invariantes de segurança (fixas no código, sem configuração)

- **I1 — Um aperto, um toque.** Só a **descida** com repetição `0` gera toque. Segurar não repete
  (a repetição automática `value=2` é filtrada já na leitura do hardware).
- **I2 — Sem automação.** Não existe timer, fila, gravação, macro nem sequência. Todo toque nasce
  de um KeyPress.
- **I3 — Sem aleatoriedade.** O toque cai exatamente no Anchor.
- **I4 — Freio.** No máximo **10 toques por segundo**, somando todos os botões; o excesso é
  descartado.
- **I5 — Um botão, um ponto.** Um Trigger tem no máximo um Binding; um KeyPress gera no máximo um
  toque.

Estas invariantes são decididas por uma única classe (`TapGuard`) e provadas em `test/TapGuardTest.java`.

---

## 7. Comportamento

- **Botão com Binding**: o evento é consumido (a função original do botão some enquanto ativo).
- **Botão sem Binding**: segue normalmente (na prática, o aparelho está "tomado", então nada
  chega ao sistema — ver §8.4).
- **Detector**: a tela principal mostra o último botão recebido, com nome legível e código.
- **Aprendizado**: o próximo botão apertado vira um Binding novo, com Anchor no centro da tela, e
  o app entra em modo mover.
- **Freio**: 10 toques/s no total, somando todos os botões.

---

## 8. Arquitetura técnica

### 8.0 Visão geral das camadas

```
app/src/main/
  domain/    Java puro, sem Android (testável no PC)
             Anchor · Binding · KeyPress · Trigger · TapGuard
  hw/        Ponte privilegiada (Shizuku)
             Reader (IReader.Stub) · AIDL IReader / IKeyListener
  infra/     Serviço Android + persistência + nomes
             AnchorService · BindingStore · Names
  ui/        Console
             MainActivity · Ui (sistema visual)
```

- `domain/` não importa nada de Android — as invariantes são testadas com o JDK comum
  (`test/run.sh`).
- `hw/Reader` roda no **processo do Shizuku** (uid `shell`), não no processo do app.
- `infra/AnchorService` é um **serviço em primeiro plano** no processo do app; ele conversa com o
  `Reader` por **IPC (AIDL)**.
- `ui/MainActivity` só desenha o console e manda comandos para o serviço.

### 8.1 Por que o Shizuku é necessário

Um app comum **não pode** ler um controle antes do sistema nem injetar toques no nível raiz do
entrada. Isso exige o uid `shell` (o mesmo da depuração ADB). O Shizuku é um app aberto que sobe
um processo com essa credencial, **com a autorização do usuário**, e deixa o TapAnchor rodar um
pedaço de código dentro dele. Nada de root.

- O app declara o `ShizukuProvider` no manifesto
  (`android:permission="android.permission.INTERACT_ACROSS_USERS_FULL"`).
- A permissão é pedida na tela (`Shizuku.requestPermission`) e conferida com
  `Shizuku.checkSelfPermission()`.
- `AnchorService.connect()` só tenta conectar se `Shizuku.pingBinder()` e a permissão estiverem
  concedidos.

### 8.2 O serviço de usuário do Shizuku

`AnchorService` sobe o `Reader` como **UserService**:

```java
new Shizuku.UserServiceArgs(new ComponentName("com.tapanchor.app", Reader.class.getName()))
    .daemon(false)
    .processNameSuffix("reader")
    .version(9);
```

- `version(9)` = `versionCode`. Se o app for atualizado, o Shizuku sobe um processo novo e mata o
  antigo (evita mistura de versões).
- O `Reader` implementa `IReader.Stub`; o app segura um `IReader` via `ServiceConnection`.
- O contrato AIDL `destroy()` usa a transação reservada `16777114` (convenção do Shizuku para
  encerrar o UserService).

### 8.3 Captura do hardware (o `Reader`)

Roda numa thread própria (`tapanchor-reader`) enquanto houver um aparelho escolhido:

1. **Achar o nó do aparelho.** `findNode(nome)` roda `getevent -p` (via shell), varre as linhas
   `add device /dev/input/eventN` e casa pelo `name:` do aparelho. O shell não lê o nome de
   sistema de outro jeito, então `getevent -p` é o caminho.
2. **Abrir** `Os.open(node, O_RDONLY)`.
3. **Tomar o aparelho (grab).** `EVIOCGRAB` (`0x40044590`) via `ioctlInt` (com *reflect* para
   cobrir as assinaturas do Android 7–9). A partir daqui, **o Android deixa de receber o
   controle** — nenhum botão vira "voltar", nada mexe o cursor.
4. **Ler eventos.** `Os.poll` + `Os.read` de **24 bytes** (um `struct input_event` em 64 bits:
   `timeval` de 16 + `type` + `code` + `value`). Só interessam `type == EV_KEY (1)`.
5. **Fechar** libera o aparelho (`Os.close`) e, se cair, tenta reabrir a cada ~1,5 s (controle
   dormiu/desligou).

Interpretação do evento:

```
value == 1  → desceu  (down)
value == 0  → subiu   (up)
value == 2  → repetição automática → IGNORADA (I1)
```

### 8.4 Decisão (TapGuard) e o efeito do grab

Cada botão lido vira um `KeyPress(Trigger.key(code, 0), down, 0, agora)` e passa pelo `TapGuard`:

1. `find(trigger, bindings)` — procura um Binding do botão. Sem Binding → `PASS` (não faz nada).
2. `!down || repeat > 0` → `SWALLOW` (I1).
3. Freio: mantém os toques do último 1 s; se já houver `10`, `SWALLOW` (I4).
4. Caso contrário → `Decision(consume=true, tap=anchor)` (I3, I5).

Como o aparelho está tomado por `EVIOCGRAB`, mesmo o `PASS`/`SWALLOW` não "volta" para o sistema:
o evento já foi lido direto do nó do kernel.

Enquanto `paused` (aprendendo/movendo), o `Reader` ainda **avisa** o detector (`listener.onKey`),
mas **não** roda o `TapGuard` e **não** dispara toque.

### 8.5 Injeção do toque (o coração do produto)

Quando a decisão traz um Anchor, `Reader.tap(anchor)` injeta um toque real:

```java
// InputManagerGlobal (Android 14+) ou InputManager (mais antigo), via reflexão
inputManager.injectInputEvent(evento, 0);   // mode 0 = assíncrono
```

O evento é montado como um toque comum:

```java
MotionEvent.obtain(downTime, eventTime, ACTION_DOWN, x, y, 0);  // 1º
MotionEvent.obtain(downTime, eventTime, ACTION_UP,   x, y, 0);  // 2º
evento.setSource(InputDevice.SOURCE_TOUCHSCREEN);
```

Dois pontos que importam:

- **É um `injectInputEvent` de verdade**, no mesmo duto do sistema que o dedo usa — não é
  `dispatchGesture` de acessibilidade, não é `performClick` de widget. Por isso funciona dentro de
  jogos que só aceitam toque bruto.
- `SOURCE_TOUCHSCREEN` é o que faz o app/jogo enxergar como **dedo**, não como mouse. O toque cai
  em `(x, y)` e o `ACTION_UP` fecha o clique.

O `injectInputEvent` exige a permissão de injetar eventos, que o uid `shell` tem — daí tudo rodar
dentro do `Reader`.

### 8.6 Sobre o `scanCode` (limite real da v2)

O domínio prevê `Trigger(keyCode, scanCode)` para diferenciar botões que o Android reportaria com
`keyCode 0`. **No caminho de hardware atual isso não é usado:** o `Reader` lê o código evdev
direto do kernel (`Trigger.key(code, 0)`), e o `BindingStore` serializa apenas
`id,keyCode,x,y;` — o `scanCode` **não é persistido**. Na prática não dói, porque o código evdev já
identifica o botão de forma única. Fica registrado como divergência entre a intenção do domínio e
o que a v2 grava.

### 8.7 Sobreposição (os marcadores)

`AnchorService.redraw()` recria os marcadores. Detalhes de janela:

- Tipo `TYPE_APPLICATION_OVERLAY` (API ≥ 26) ou `TYPE_PHONE` (abaixo).
- Flags: `FLAG_NOT_FOCUSABLE | FLAG_LAYOUT_NO_LIMITS | FLAG_LAYOUT_IN_SCREEN`, mais
  `FLAG_NOT_TOUCHABLE` **quando travado** (o toque atravessa o círculo).
- Recorte de tela (`layoutInDisplayCutoutMode`) tratado por versão.
- **A permissão de sobreposição é concedida pelo próprio app** sem o usuário ir nas configurações:
  `Reader.allowOverlay(pkg)` roda `appops set <pkg> SYSTEM_ALERT_WINDOW allow` (shell).
- No modo mover há um botão flutuante `TRAVAR POSIÇÃO` no topo; no modo travado, uma etiqueta
  `NN ✎` ao lado de cada marcador destrava.

### 8.8 Persistência

`BindingStore` usa `SharedPreferences("anchors")`:

- `all` → Bindings serializados: `id,keyCode,x,y;` (um ponto e vírgula por Binding).
- `device` → nome do aparelho escolhido (ou ausente = nenhum).

Ao reconectar, o serviço reenvia os Bindings ao `Reader` (`reader.setBindings`).

### 8.9 Ciclo de vida e conexão

- `MainActivity` → garante o `AnchorService` (foreground).
- `AnchorService` → `connect()` ao Shizuku; ao conectar, libera a sobreposição, empurra Bindings e,
  se houver aparelho escolhido, `reader.start(device, listener)`.
- Notificação fixa "TapAnchor ativo" mantém o serviço vivo com o jogo aberto
  (`FOREGROUND_SERVICE_TYPE_SPECIAL_USE`).
- `onChange` é o canal simples de "algo mudou, redesenhe o console".

### 8.10 Fluxo de dados (resumo)

```
botão físico
   │  (kernel /dev/input/eventN)
   ▼
Reader (processo Shizuku, uid shell)          AnchorService (processo do app)
   ├─ EVIOCGRAB: Android deixa de ver o controle
   ├─ lê input_event (EV_KEY, ignora repetição)
   ├─ onKey ──IPC──► IKeyListener ──► detector na tela (apenas exibe)
   └─ TapGuard.decide ──► Anchor
         └─ injectInputEvent(ACTION_DOWN/UP, SOURCE_TOUCHSCREEN, x, y)
                 └─► app/jogo recebe UM toque no ponto
```

---

## 9. Comparação (por que não é o que já existe)

| Caminho comum | O que faz | Por que não serve |
|---|---|---|
| Mapeamento de gamepad do Android | Troca o botão por uma **tecla** do sistema | Não gera **toque com posição**. |
| Acessibilidade (`dispatchGesture`) | Desenha um gesto por cima | Não injeta toque bruto; jogos que ignoram acessibilidade não recebem. |
| Emular mouse (cursor) | Move um ponteiro | Jogo de toque não reage a cursor. |
| **TapAnchor** | **Um toque real na coordenada escolhida** | — |

---

## 10. Limites e fora do escopo

- **Não segue o cursor do mouse** (o Android não expõe essa posição de forma útil para toque).
- **Anchors não migram por orientação.** Configure com a tela na orientação do jogo.
- **Botões que já são clique de mouse** chegam como `EV_KEY`/código de mouse e são tratados como
  botão — mas não há suporte a movimento de ponteiro.
- **64 bits apenas** no `Reader`: o `struct input_event` é lido como 24 bytes (formato de 64 bits).
  Em aparelhos 32 bits o offset do evento seria 16 bytes e a leitura não bateria.
- **Não há macro, sequência, timer ou gravação** — por decisão de produto e de segurança (I2).
- **Freio de 10 toques/s** não é configurável.

---

## 11. Compatibilidade

| Item | Valor |
|---|---|
| `minSdk` | 24 (Android 7) |
| `targetSdk` / `compileSdk` | 35 |
| Linguagem | Java 17 |
| Build | AGP 8.7.3, Gradle |
| Dependências | `dev.rikka.shizuku:api` e `provider` 13.1.5 |
| Pré-requisito | Shizuku instalado e iniciado, permissão concedida |

Ajustes por versão já tratados no código: `TYPE_APPLICATION_OVERLAY` (≥ 26), notificação em canal
(≥ 26), `startForeground` com tipo (≥ 34), `LAYOUT_IN_DISPLAY_CUTOUT_MODE` (≥ 28/30),
`isExternal()` para filtrar aparelhos (≥ 29), `InputManagerGlobal` vs `InputManager` (Android 14+),
e as duas assinaturas de `ioctlInt` (Android 7–9 vs 10+).

---

## 12. Prova e verificação

- `test/TapGuardTest.java` roda o domínio puro no JDK (`test/run.sh`) e prova I1, I3, I4, I5 e a
  passagem de botão sem Binding.
- Verificação manual em aparelho: escolher controle → ancorar botão → mover → travar → conferir
  que o aperto toca no ponto e que a função original do botão sumiu.

---

## 13. Riscos e pontos de atenção

- **`EVIOCGRAB` é tudo ou nada**: enquanto ativo, o aparelho inteiro fica invisível para o
  Android. Se o app cair sem `stop()`, o controle volta a aparecer (o grab cai junto com o
  processo/nó).
- **Latência** depende do laço de leitura (`Os.poll` de 400 ms só no timeout; eventos chegam
  assíncronos) e da injeção — adequada para clique, não para movimento contínuo.
- **Dependência do Shizuku**: sem ele, o app não passa do passo "escolha o controle".
- **`scanCode` não persistido** (§8.6) é dívida conhecida, não bug em uso.
