# Aparelhos compatíveis

A regra é simples: se o Android enxerga o aparelho como teclado ou controle HID, e ele manda eventos de tecla, dá para ancorar. HID é o nome técnico do que o Android enxerga como teclado ou controle.

O TapAnchor usa um aparelho por vez. Para trocar, escolha outro na lista do app.

| Tipo | Exemplos concretos | Como o Android vê | O que acontece com o TapAnchor | O que observar |
|---|---|---|---|---|
| Botão de selfie | Pequeno controle Bluetooth de clipe, vendido como disparador de câmera | Teclado, muitas vezes enviando volume ou enter | Cada aperto vira um toque no ponto ancorado | Alguns modelos só funcionam dentro do app de câmera. Se o Android vê como teclado, ancora. |
| Virador de página de e-reader | Controle Bluetooth de página para Kindle, com botões de anterior e próxima | Teclado, enviando page up e page down | Cada aperto vira um toque no ponto ancorado | Serve para ler, para virar página em app de revista e para qualquer app que aceite toque. |
| Clicker de apresentação | Avançador USB ou Bluetooth com botões de próximo e voltar | Teclado | Próximo e voltar viram toques nos pontos que você escolher | O laser do clicker não tem relação com o app. Só os botões importam. |
| Controle de videogame | Controle Bluetooth no modo HID, no estilo Xbox ou PlayStation | Controle HID, com eventos de tecla | Os botões viram toques. Cada botão pode ter o seu ponto. | Analógico e direcional que mandam movimento não movem ponteiro. Só eventos de tecla viram toque. |
| Pedal USB de transcrição | Pedal de um ou três botões, ligado por adaptador OTG | Teclado | Cada pedal vira um toque no ponto ancorado | Precisa de adaptador OTG compatível com o celular. |
| Teclado | Teclado USB ou Bluetooth comum | Teclado | As teclas escolhidas viram toques | Enquanto ativo, o teclado inteiro fica tomado. As outras teclas também não chegam ao sistema. |
| Switch de acessibilidade | Switch Bluetooth que emula espaço ou enter | Teclado | Cada acionamento vira um toque no ponto ancorado | Indicado para acionar um botão grande na tela com um switch físico. |
| Mouse | Mouse USB ou Bluetooth | Botões chegam como eventos de tecla | Os botões podem ser ancorados | O cursor não é seguido. O app não move ponteiro. |

## O que fica de fora

- Celulares de 32 bits. O leitor do TapAnchor usa o formato de evento de 64 bits.
- Aparelhos que o Android não vê como teclado ou HID. Eles não aparecem na lista.
- Movimento de ponteiro. O app não segue cursor de mouse nem analógico.

## O que acontece com o botão original

Enquanto o TapAnchor está ativo, o aparelho escolhido fica tomado por inteiro. O botão ancorado deixa de fazer a função original e passa a disparar o toque. Os botões sem âncora também não chegam ao sistema, porque o aparelho inteiro está tomado.

Quando o app para de usar o aparelho, ou quando o celular reinicia, o controle volta ao normal.
