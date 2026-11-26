"""Gera o ícone adaptativo do app (o blob) em app/src/main/res.

Só usa a stdlib do Python. A borda sai da mesma fórmula de ruído do shader do blob (SRC_BLOB em
ui/EstiloVisual.kt), lida num instante fixo (FASE). Mude a fórmula lá e aqui juntas.

Uso, na raiz do repositório:  python ferramentas/gerar_icone.py

Camadas: foreground (o blob com luz na borda e miolo profundo, mais o halo), background
(a cor base_escura de themes.xml) e monochrome (o blob chapado, para os ícones temáticos do Android 13). O monochrome
é menor e ondula mais: os glifos temáticos do sistema são pequenos, e um blob grande e quase redondo lia como círculo.
"""

import math
from pathlib import Path

RES = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "res"

# O instante e a forma. Ruído e frequência da Pausa longa (DESIGN.md §7): a borda mais solta,
# a que ainda se lê como blob em 48 dp. FASE é o uniform ruidoFase do shader.
FASE = 100.0  # um instante com três lobos largos: orgânico em 48 dp, sem virar círculo
RUIDO_AMP = 0.065
RUIDO_FREQ = 0.8

# Viewport do ícone adaptativo: 108 × 108, com a zona segura num círculo de raio 33 no centro.
CENTRO = 54.0
RAIO_MAX = 25.0  # o ponto mais distante da borda; folga para as máscaras circular, quadrada e squircle
ZONA_SEGURA = 33.0
PONTOS = 72

# Monochrome: o glifo do tamanho dos glifos temáticos dos outros apps, com a borda mais funda para ler como blob.
RAIO_MONO = 17.0
RUIDO_AMP_MONO = 0.15

DESTAQUE_FOCO = 0xA092DC  # Cor de destaque de Foco do Perfil padrão: "Do Fundo" sobre a Aurora (DESIGN.md §3)


def fract(x):
    return x - math.floor(x)


def hash2(px, py):
    # Igual ao hash do RUIDO_AGSL: p3 = fract(p.xyx * 0.1031); p3 += dot(p3, p3.yzx + 33.33).
    x, y, z = fract(px * 0.1031), fract(py * 0.1031), fract(px * 0.1031)
    d = x * (y + 33.33) + y * (z + 33.33) + z * (x + 33.33)
    x, y, z = x + d, y + d, z + d
    return fract((x + y) * z)


def ruido(px, py):
    ix, iy = math.floor(px), math.floor(py)
    fx, fy = px - ix, py - iy
    ux = fx * fx * fx * (fx * (fx * 6 - 15) + 10)
    uy = fy * fy * fy * (fy * (fy * 6 - 15) + 10)
    a, b = hash2(ix, iy), hash2(ix + 1, iy)
    c, d = hash2(ix, iy + 1), hash2(ix + 1, iy + 1)
    return (a + (b - a) * ux) + ((c + (d - c) * ux) - (a + (b - a) * ux)) * uy


def borda(ang, amp=RUIDO_AMP):
    """Raio relativo da borda no ângulo, como em SRC_BLOB: duas oitavas de ruído lidas num círculo (a alta fraca)."""
    dx, dy = math.cos(ang), math.sin(ang)
    fase = FASE / 40
    ox, oy = 40 * math.cos(fase), 40 * math.sin(fase)
    n = (0.85 * ruido(dx * RUIDO_FREQ + ox, dy * RUIDO_FREQ + oy)
         + 0.15 * ruido(dx * RUIDO_FREQ * 2.1 + ox * 1.3 + 17, dy * RUIDO_FREQ * 2.1 + oy * 1.3 + 5))
    return 1 + amp * (n - 0.5) * 2


def caminho(raio_max=RAIO_MAX, amp=RUIDO_AMP):
    """A borda como curva fechada: Catmull-Rom em cúbicas, sem quina. O ponto mais distante fica em [raio_max]."""
    rel = [borda(2 * math.pi * i / PONTOS, amp) for i in range(PONTOS)]
    raio = raio_max / max(rel)
    pts = [(CENTRO + raio * r * math.cos(2 * math.pi * i / PONTOS), CENTRO + raio * r * math.sin(2 * math.pi * i / PONTOS))
           for i, r in enumerate(rel)]
    assert max(math.dist(p, (CENTRO, CENTRO)) for p in pts) <= ZONA_SEGURA, "o blob passa da zona segura"
    assert min(rel) / max(rel) > 0.75, "a borda ondula demais para ler como blob em 48 dp"

    def f(v):
        return f"{v:.2f}".rstrip("0").rstrip(".")

    partes = [f"M{f(pts[0][0])},{f(pts[0][1])}"]
    for i in range(PONTOS):
        p0, p1, p2, p3 = (pts[(i + k) % PONTOS] for k in (-1, 0, 1, 2))
        c1 = (p1[0] + (p2[0] - p0[0]) / 6, p1[1] + (p2[1] - p0[1]) / 6)
        c2 = (p2[0] - (p3[0] - p1[0]) / 6, p2[1] - (p3[1] - p1[1]) / 6)
        partes.append(f"C{f(c1[0])},{f(c1[1])} {f(c2[0])},{f(c2[1])} {f(p2[0])},{f(p2[1])}")
    return "".join(partes) + "Z", raio


# Oklab, como em dados/Cor.kt: a borda com L 0.80 e o miolo com L 0.42 (DESIGN.md §3).
def linear(c):
    c /= 255
    return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4


def srgb(c):
    return 12.92 * c if c <= 0.0031308 else 1.055 * c ** (1 / 2.4) - 0.055


def oklab(rgb):
    r, g, b = (linear(rgb >> s & 0xFF) for s in (16, 8, 0))
    l = (0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b) ** (1 / 3)
    m = (0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b) ** (1 / 3)
    s = (0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b) ** (1 / 3)
    return (0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
            1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
            0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s)


def de_oklab(L, a, b):
    for _ in range(40):  # fora do gamut: tira croma, como oklabParaArgb
        l = (L + 0.3963377774 * a + 0.2158037573 * b) ** 3
        m = (L - 0.1055613458 * a - 0.0638541728 * b) ** 3
        s = (L - 0.0894841775 * a - 1.2914855480 * b) ** 3
        rgb = (4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
               -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
               -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s)
        if all(-0.0001 <= v <= 1.0001 for v in rgb):
            break
        a, b = a * 0.95, b * 0.95
    return tuple(round(srgb(min(max(v, 0), 1)) * 255) for v in rgb)


def tom(rgb, L):
    _, a, b = oklab(rgb)
    return de_oklab(L, a, b)


def hexa(rgb, alfa=1.0):
    return "#{:02X}{:02X}{:02X}{:02X}".format(round(alfa * 255), *rgb)


def misturar(x, y, t):
    return tuple(round(a + (b - a) * t) for a, b in zip(x, y))


def smoothstep(a, b, x):
    t = min(max((x - a) / (b - a), 0), 1)
    return t * t * (3 - 2 * t)


def foreground(dados, raio):
    luz, fundo = tom(DESTAQUE_FOCO, 0.80), tom(DESTAQUE_FOCO, 0.42)
    # O miolo profundo do shader: pow(smoothstep(0.3, 1, q), 2.6), em paradas.
    paradas = "\n".join(
        f'                    <item android:offset="{q}" android:color="{hexa(misturar(fundo, luz, smoothstep(0.3, 1, q) ** 2.6))}" />'
        for q in (0, 0.3, 0.55, 0.7, 0.8, 0.88, 0.94, 1))
    # O halo de fora: exp(-d / (0.18 r)) · 0.4, de r até 1.5 r.
    alcance = 1.5
    halo = "\n".join(
        f'                    <item android:offset="{o:.3f}" android:color="{hexa(luz, 0.4 * math.exp(-max(o * alcance - 1, 0) / 0.18))}" />'
        for o in (0, 1 / alcance, 0.75, 0.82, 0.9))
    halo += f'\n                    <item android:offset="1" android:color="{hexa(luz, 0)}" />'
    r = raio * alcance
    return f"""<?xml version="1.0" encoding="utf-8"?>
<!-- Gerado por ferramentas/gerar_icone.py. Não edite à mão. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:pathData="M{CENTRO - r:.2f},{CENTRO}a{r:.2f},{r:.2f} 0 1,0 {2 * r:.2f},0a{r:.2f},{r:.2f} 0 1,0 {-2 * r:.2f},0Z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:type="radial"
                android:centerX="{CENTRO}"
                android:centerY="{CENTRO}"
                android:gradientRadius="{r:.2f}">
{halo}
            </gradient>
        </aapt:attr>
    </path>
    <path android:pathData="{dados}">
        <aapt:attr name="android:fillColor">
            <gradient
                android:type="radial"
                android:centerX="{CENTRO}"
                android:centerY="{CENTRO}"
                android:gradientRadius="{raio:.2f}">
{paradas}
            </gradient>
        </aapt:attr>
    </path>
</vector>
"""


def monochrome(dados):
    return f"""<?xml version="1.0" encoding="utf-8"?>
<!-- Gerado por ferramentas/gerar_icone.py. Não edite à mão. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="{dados}" />
</vector>
"""


ADAPTATIVO = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/base_escura" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
"""

def gravar(caminho_rel, texto):
    p = RES / caminho_rel
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_bytes(texto.encode("utf-8"))  # LF, sem conversão
    print(p.relative_to(RES.parent.parent.parent.parent))


if __name__ == "__main__":
    dados, raio = caminho()
    gravar("drawable/ic_launcher_foreground.xml", foreground(dados, raio))
    gravar("drawable/ic_launcher_monochrome.xml", monochrome(caminho(RAIO_MONO, RUIDO_AMP_MONO)[0]))
    gravar("mipmap-anydpi/ic_launcher.xml", ADAPTATIVO)
