"""Sintetiza os sons embutidos do Alerta e grava em app/src/main/res/raw.

Só usa a stdlib do Python. Gera WAV (mono, 16 bits, 44,1 kHz). Se o ffmpeg com libvorbis
estiver no PATH, converte para OGG, que fica bem menor, e apaga o WAV.

Uso, na raiz do repositório:  python ferramentas/gerar_sons.py

Todo som começa e termina em silêncio (ataque e saída com rampa de cosseno), para não ter clique.
Os nomes dos arquivos são os nomes guardados no Perfil: mude junto com SONS em Sons.kt.
"""

import array
import math
import shutil
import subprocess
import wave
from pathlib import Path

TAXA = 44_100
SAIDA = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "res" / "raw"


def nota(midi):
    return 440.0 * 2 ** ((midi - 69) / 12)


def batida(buf, inicio, freq, parciais, ataque=0.008, volume=1.0, tremolo=0.0):
    """Soma uma nota em buf. parciais: (razão da frequência, amplitude, tempo de decaimento em s)."""
    i0 = int(inicio * TAXA)
    duracao = max(d for _, _, d in parciais) * 6  # até ~ -52 dB
    n = min(int(duracao * TAXA), len(buf) - i0)
    na = max(1, int(ataque * TAXA))
    for razao, amp, decai in parciais:
        w = 2 * math.pi * freq * razao / TAXA
        if w >= math.pi:  # acima de Nyquist
            continue
        k = math.exp(-1 / (decai * TAXA))
        env = amp * volume
        for j in range(n):
            a = env
            if j < na:
                a *= 0.5 - 0.5 * math.cos(math.pi * j / na)
            if tremolo:
                a *= 1 - tremolo * (0.5 - 0.5 * math.cos(2 * math.pi * 5.5 * j / TAXA))
            buf[i0 + j] += a * math.sin(w * j)
            env *= k


def sino(buf):
    # Sino pequeno: parciais inarmônicos, os graves duram mais.
    p = [(0.5, 0.35, 1.6), (1.0, 1.0, 1.2), (1.19, 0.35, 0.9), (1.5, 0.3, 0.7),
         (2.0, 0.25, 0.5), (2.51, 0.12, 0.35), (3.01, 0.06, 0.25)]
    batida(buf, 0.0, nota(76), p, ataque=0.006)
    batida(buf, 0.9, nota(71), p, ataque=0.006, volume=0.7)


def sino_duplo(buf):
    # Vibrafone em duas notas descendo (terça maior), com tremolo leve.
    p = [(1.0, 1.0, 0.9), (4.0, 0.25, 0.25), (10.0, 0.05, 0.08)]
    batida(buf, 0.0, nota(79), p, ataque=0.01, tremolo=0.25)
    batida(buf, 0.45, nota(75), p, ataque=0.01, tremolo=0.25)


def marimba(buf):
    # Arpejo de Dó maior. O parcial da marimba fica perto de 4×, e o som some rápido.
    p = [(1.0, 1.0, 0.35), (3.93, 0.2, 0.08), (9.2, 0.04, 0.03)]
    for i, m in enumerate([72, 76, 79, 84]):
        batida(buf, i * 0.16, nota(m), p, ataque=0.004, volume=0.85)


def tigela(buf):
    # Tigela tibetana: ataque suave, batimento lento entre dois parciais quase iguais.
    f = nota(57)
    p = [(1.0, 0.8, 1.6), (1.004, 0.6, 1.6), (2.71, 0.45, 1.1), (2.716, 0.3, 1.1),
         (5.15, 0.15, 0.6)]
    batida(buf, 0.0, f, p, ataque=0.03)


def kalimba(buf):
    # Lamelas: fundamental forte e um parcial agudo curto. Três notas pentatônicas.
    p = [(1.0, 1.0, 0.6), (5.4, 0.15, 0.05), (2.0, 0.1, 0.2)]
    for t, m in [(0.0, 74), (0.22, 79), (0.44, 81), (0.66, 86)]:
        batida(buf, t, nota(m), p, ataque=0.004, volume=0.8)


def carrilhao(buf):
    # Carrilhão de vento: notas pentatônicas que se sobrepõem, em ordem fixa.
    p = [(1.0, 1.0, 0.9), (2.76, 0.3, 0.4), (5.4, 0.1, 0.15)]
    for t, m in [(0.0, 88), (0.18, 84), (0.31, 91), (0.55, 86), (0.8, 93), (1.05, 88)]:
        batida(buf, t, nota(m), p, ataque=0.005, volume=0.6)


SONS = {
    "sino": (sino, 3.2),
    "sino_duplo": (sino_duplo, 2.6),
    "marimba": (marimba, 2.0),
    "tigela": (tigela, 4.0),
    "kalimba": (kalimba, 2.4),
    "carrilhao": (carrilhao, 3.2),
}


def finalizar(buf, pico=0.6, saida=0.3):
    """Normaliza o pico (sons suaves, abaixo de 0 dBFS) e zera o fim com uma rampa."""
    maximo = max(abs(x) for x in buf) or 1.0
    ns = int(saida * TAXA)
    out = array.array("h")
    for i, x in enumerate(buf):
        resto = len(buf) - i
        if resto < ns:
            x *= 0.5 - 0.5 * math.cos(math.pi * resto / ns)
        out.append(int(round(x / maximo * pico * 32767)))
    out[0] = out[-1] = 0
    return out


def main():
    SAIDA.mkdir(parents=True, exist_ok=True)
    ffmpeg = shutil.which("ffmpeg")
    for nome, (gerar, duracao) in SONS.items():
        buf = [0.0] * int(duracao * TAXA)
        gerar(buf)
        amostras = finalizar(buf)
        wav = SAIDA / f"{nome}.wav"
        ogg = SAIDA / f"{nome}.ogg"
        for velho in (wav, ogg):
            velho.unlink(missing_ok=True)
        with wave.open(str(wav), "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(TAXA)
            w.writeframes(amostras.tobytes())
        if ffmpeg:
            r = subprocess.run(
                [ffmpeg, "-y", "-loglevel", "error", "-i", str(wav), "-c:a", "libvorbis", "-q:a", "5", str(ogg)]
            )
            if r.returncode == 0:
                wav.unlink()
        print(ogg if ogg.exists() else wav)


if __name__ == "__main__":
    main()
