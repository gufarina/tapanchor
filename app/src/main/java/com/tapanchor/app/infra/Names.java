package com.tapanchor.app.infra;

/** Nome legível, em português, de um botão lido do hardware (códigos de tecla do Linux). */
public final class Names {
    private Names() {}

    public static String of(int code) {
        switch (code) {
            case 272: return "Clique esquerdo";
            case 273: return "Clique direito";
            case 274: return "Clique do meio";
            case 275: return "Lateral (voltar)";
            case 276: return "Lateral (avançar)";
            case 304: return "Botão A";
            case 305: return "Botão B";
            case 306: return "Botão C";
            case 307: return "Botão X";
            case 308: return "Botão Y";
            case 310: return "Gatilho L1";
            case 311: return "Gatilho R1";
            case 312: return "Gatilho L2";
            case 313: return "Gatilho R2";
            case 314: return "Select";
            case 315: return "Start";
            case 316: return "Botão Home";
            case 317: return "Analógico esquerdo";
            case 318: return "Analógico direito";
            case 103: return "Seta cima";
            case 108: return "Seta baixo";
            case 105: return "Seta esquerda";
            case 106: return "Seta direita";
            case 115: return "Volume +";
            case 114: return "Volume -";
            case 113: return "Mudo";
            case 158: return "Voltar";
            case 172: return "Início";
            case 164: return "Play/Pause";
            case 163: return "Próxima faixa";
            case 165: return "Faixa anterior";
            case 28: return "Enter";
            case 57: return "Espaço";
            case 212: return "Câmera";
            default: return "Botão #" + code;
        }
    }
}
