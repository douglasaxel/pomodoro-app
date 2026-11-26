package com.exemplo.pomodoro.dados

import androidx.annotation.StringRes
import com.exemplo.pomodoro.R

/**
 * Uma arte da Biblioteca (ADR-0003): um shader AGSL desenhado ao vivo.
 * [paleta] é a fonte única das cores: o shader só mistura essas cores (uniform `cores`),
 * e a Cor de destaque automática (ticket 09) sai dela.
 * [id] fica guardado no Perfil: não renomeie.
 */
class Arte(val id: String, @StringRes val nome: Int, val paleta: List<Int>, val shader: String)

/** A arte do [id], ou a primeira, se o id não existe mais (uma arte removida não quebra o Perfil). */
fun arte(id: String): Arte = ARTES.find { it.id == id } ?: ARTES.first()

private fun cores(vararg c: Long) = c.map { it.toInt() }

/*
 * Contrato de todo shader da Biblioteca, igual ao do Gradiente vivo:
 * `size` em pixels, `iTime` em segundos (dá a volta em 360 s) e `cores`, a paleta (até 6).
 * Movimento: só `fase(k)`, com k inteiro de 4 a 9. Períodos de 90 a 40 s, e tudo fecha a volta em 360 s, sem salto.
 * A arte só mistura (mix) as cores da paleta: o brilho nunca passa da cor mais clara, e o Véu automático vale.
 * Nenhum ponto anda mais que 2% da altura por segundo (DESIGN.md §7 Fundo).
 */
private const val PRELUDIO = """
uniform float2 size;
uniform float iTime;
uniform half4 cores[6];

float fase(float k) { return iTime * 0.017453292 * k; }

float hash(float2 p) {
    float3 p3 = fract(float3(p.x, p.y, p.x) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float ruido(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float2 u = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + float2(1.0, 0.0));
    float c = hash(i + float2(0.0, 1.0));
    float d = hash(i + float2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm(float2 p) {
    float s = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        s += a * ruido(p);
        p = p * 2.03 + float2(17.1, 9.2);
        a *= 0.5;
    }
    return s / 0.9375;
}

float ign(float2 fc) { return fract(52.9829189 * fract(dot(fc, float2(0.06711056, 0.00583715)))); }

// Pontilhado triangular de ±1/255: desfaz as faixas do gradiente em 8 bits.
half4 saida(float3 cor, float2 fc) {
    float t = ign(fc) + ign(fc + float2(5.588238, 5.588238)) - 1.0;
    return half4(half3(cor + t / 255.0), 1.0);
}
"""

/** Cortinas de luz sobre o céu da noite. A borda de baixo é nítida, e os raios somem para cima. */
private const val SRC_AURORA = PRELUDIO + """
half4 main(float2 fc) {
    float2 uv = fc / size.y;
    float x = uv.x - 0.5 * size.x / size.y;
    float y = uv.y;
    float3 cor = mix(cores[0].rgb, cores[1].rgb, smoothstep(0.0, 0.85, y));
    for (int i = 0; i < 3; i++) {
        float fi = float(i);
        float linha = 0.30 + 0.13 * fi
            + 0.05 * sin(x * 4.0 + fi * 2.1 + fase(4.0 + fi))
            + 0.02 * sin(x * 9.0 - fi * 1.3 + fase(7.0 - fi));
        float d = y - linha;
        float baixo = exp(-max(d, 0.0) / 0.02);
        float alto = exp(min(d, 0.0) / (0.15 + 0.05 * fi));
        float raios = 0.5 + 0.5 * ruido(float2(x * 24.0 + 2.5 * sin(y * 3.0 + fase(5.0) + fi), fi * 7.0 + y * 0.8));
        float trecho = smoothstep(0.25, 0.75, ruido(float2(x * 2.5 + fi * 5.0 + 0.5 * cos(fase(4.0)), fi * 3.0 + 0.5 * sin(fase(4.0)))));
        float forca = baixo * alto * raios * (0.35 + 0.65 * trecho) * (0.95 - 0.2 * fi);
        float3 tom = mix(cores[2].rgb, cores[3].rgb, smoothstep(0.0, 0.18, -d));
        tom = mix(tom, cores[4].rgb, smoothstep(0.12, 0.4, -d));
        cor = mix(cor, tom, clamp(forca, 0.0, 1.0));
    }
    // Colinas paradas no horizonte, com um resto de luz logo acima.
    float aa = 1.5 / size.y;
    float colina = 0.86 + 0.025 * sin(x * 5.0 + 1.2) + 0.01 * sin(x * 13.0 + 0.4);
    cor = mix(cor, cores[3].rgb, 0.22 * exp(-abs(y - colina) / 0.07));
    cor = mix(cor, cores[0].rgb, smoothstep(colina - aa, colina + aa, y));
    return saida(cor, fc);
}
"""

/** Dunas ao entardecer, em camadas. As do fundo somem na névoa, e cada crista tem um lado de sombra. */
private const val SRC_DUNAS = PRELUDIO + """
half4 main(float2 fc) {
    float2 uv = fc / size.y;
    float x = uv.x - 0.5 * size.x / size.y;
    float y = uv.y;
    float3 cor = mix(cores[0].rgb, cores[1].rgb, smoothstep(0.0, 0.6, y));
    float aa = 1.5 / size.y + 0.0015;
    for (int i = 0; i < 4; i++) {
        float fi = float(i);
        float f = 3.2 + fi * 1.6;
        float s = x * f + fi * 2.3 + 0.25 * sin(fase(4.0 + fi) + fi * 1.9);
        float crista = pow(0.5 + 0.5 * sin(s), 1.8);
        float topo = 0.50 + 0.13 * fi - (0.08 + 0.01 * fi) * crista
            - 0.012 * sin(x * f * 2.3 + fi + fase(5.0 + fi));
        float dentro = smoothstep(topo - aa, topo + aa, y);
        float3 tom = mix(cores[2].rgb, cores[4].rgb, fi / 3.0);
        tom = mix(tom, cores[1].rgb, 0.45 * (1.0 - fi / 3.0));
        float luz = smoothstep(-0.25, 0.25, cos(s) + (y - topo) * 3.0);
        tom = mix(mix(tom, cores[0].rgb, 0.45), tom, luz);
        tom = mix(tom, cores[3].rgb, 0.35 * exp(-max(y - topo, 0.0) / 0.02) * luz);
        tom = mix(tom, cores[4].rgb, 0.4 * smoothstep(0.0, 0.25, y - topo));
        cor = mix(cor, tom, dentro);
    }
    return saida(cor, fc);
}
"""

/** Ondas do mar em camadas. A luz bate na crista, e cada camada rola num sentido. */
private const val SRC_ONDAS = PRELUDIO + """
half4 main(float2 fc) {
    float2 uv = fc / size.y;
    float x = uv.x - 0.5 * size.x / size.y;
    float y = uv.y;
    float3 cor = mix(cores[2].rgb, cores[1].rgb, smoothstep(0.0, 0.25, y));
    float aa = 1.5 / size.y + 0.002;
    for (int i = 0; i < 6; i++) {
        float fi = float(i);
        float sentido = mod(fi, 2.0) * 2.0 - 1.0;
        float f = 5.0 + fi * 1.3;
        float linha = 0.16 + fi * 0.145
            + 0.035 * sin(x * f + fi * 1.7 + sentido * fase(4.0 + fi))
            + 0.015 * sin(x * f * 1.9 + fi * 2.9 - sentido * fase(5.0 + fi));
        float d = y - linha;
        float dentro = smoothstep(-aa, aa, d);
        float3 corpo = mix(cores[2].rgb, cores[0].rgb, fi / 5.0);
        corpo = mix(corpo, cores[1].rgb, 0.5);
        corpo = mix(corpo, cores[3].rgb, 0.85 * exp(-max(d, 0.0) / 0.025));
        corpo = mix(corpo, cores[4].rgb, 0.6 * exp(-max(d, 0.0) / 0.006));
        corpo = mix(corpo, cores[0].rgb, 0.7 * smoothstep(0.02, 0.2, d));
        cor = mix(cor, corpo, dentro);
    }
    return saida(cor, fc);
}
"""

/** Névoa que se dobra sobre si mesma (domínio dobrado por fbm), em tons de ardósia e lavanda. */
private const val SRC_NEVOA = PRELUDIO + """
half4 main(float2 fc) {
    float2 uv = fc / size.y;
    float2 p = float2(uv.x - 0.5 * size.x / size.y, uv.y) * 2.2;
    float2 deriva = 0.15 * float2(cos(fase(4.0)), sin(fase(5.0)));
    float2 q = float2(fbm(p + deriva), fbm(p + float2(5.2, 1.3) - deriva));
    float v = fbm(p + 1.4 * q + float2(1.7, 9.2) + 0.1 * float2(sin(fase(6.0)), cos(fase(7.0))));
    float3 cor = mix(cores[0].rgb, cores[1].rgb, smoothstep(0.15, 0.55, v));
    cor = mix(cor, cores[2].rgb, smoothstep(0.4, 0.75, v) * q.x);
    cor = mix(cor, cores[3].rgb, smoothstep(0.5, 0.85, v) * q.y);
    cor = mix(cor, cores[4].rgb, smoothstep(0.62, 0.95, v * (0.6 + 0.6 * q.x)));
    return saida(cor, fc);
}
"""

/** Dobras de seda: senos que dobram o espaço, com um brilho estreito nas cristas. */
private const val SRC_SEDA = PRELUDIO + """
half4 main(float2 fc) {
    float2 uv = fc / size.y;
    float2 p = float2(uv.x - 0.5 * size.x / size.y, uv.y) * 4.0;
    p += float2(0.8 * sin(p.y * 0.9 + fase(4.0) + 1.3), 0.6 * cos(p.x * 1.1 + fase(5.0) + 0.7));
    p += float2(0.35 * sin(p.y * 1.9 - fase(4.0) + 2.1), 0.3 * cos(p.x * 2.1 + 0.4));
    p += float2(0.15 * sin(p.y * 3.3 + 0.9), 0.12 * cos(p.x * 2.9 + 2.6));
    float s = p.x * 2.4 + p.y * 2.4;
    float altura = sin(s);
    float luz = 0.5 + 0.5 * cos(s - 0.9); // o lado da dobra virado para a luz
    float dobra = 0.5 + 0.5 * sin(p.x * 0.9 - p.y * 0.7 + 1.0);
    float3 cor = mix(cores[1].rgb, cores[2].rgb, luz);
    cor = mix(cor, cores[0].rgb, 0.65 * (1.0 - smoothstep(-1.0, 0.2, altura)));
    cor = mix(cor, cores[3].rgb, pow(luz, 4.0) * (0.4 + 0.6 * dobra));
    cor = mix(cor, cores[4].rgb, pow(luz, 18.0) * dobra * 0.85);
    return saida(cor, fc);
}
"""

/** Sol baixo sobre o mar, com faixas de nuvem e o reflexo na água. */
private const val SRC_CREPUSCULO = PRELUDIO + """
half4 main(float2 fc) {
    float2 uv = fc / size.y;
    float x = uv.x - 0.5 * size.x / size.y;
    float y = uv.y;
    float horizonte = 0.78;
    float2 sol = float2(0.04 * sin(fase(4.0)), 0.69 + 0.012 * sin(fase(5.0)));
    float3 cor = mix(cores[0].rgb, cores[1].rgb, smoothstep(0.0, 0.5, y));
    cor = mix(cor, cores[2].rgb, smoothstep(0.4, horizonte, y));
    float r = length(float2(x, y) - sol);
    cor = mix(cor, cores[3].rgb, 0.85 * exp(-r * r / 0.02));
    cor = mix(cor, cores[4].rgb, 1.0 - smoothstep(0.055, 0.07, r));
    float n = fbm(float2(x * 2.2 + 0.3 * cos(fase(4.0)), y * 12.0));
    float faixa = smoothstep(0.5, 0.75, n) * smoothstep(0.3, 0.6, y);
    cor = mix(cor, mix(cores[1].rgb, cores[3].rgb, exp(-r / 0.12)), faixa * 0.75);
    if (y > horizonte) {
        float fundo = (y - horizonte) / (1.0 - horizonte);
        float3 agua = mix(cores[2].rgb, cores[0].rgb, smoothstep(0.0, 0.7, fundo));
        float dx = x - sol.x;
        float brilho = exp(-dx * dx / (0.002 + 0.01 * fundo));
        float ondula = 0.5 + 0.5 * sin(y * 170.0 + 2.0 * sin(x * 25.0 + fase(6.0)));
        agua = mix(agua, cores[3].rgb, brilho * ondula * (0.9 - 0.6 * fundo));
        float aa = 1.5 / size.y;
        cor = mix(cor, agua, smoothstep(horizonte, horizonte + aa, y));
    }
    return saida(cor, fc);
}
"""

/** Nuvens de gás no espaço escuro, com poucas estrelas fixas. As estrelas não piscam: a calma vem antes. */
private const val SRC_NEBULOSA = PRELUDIO + """
half4 main(float2 fc) {
    float2 uv = fc / size.y;
    float2 p = float2(uv.x - 0.5 * size.x / size.y, uv.y) * 2.0 + 0.12 * float2(cos(fase(4.0)), sin(fase(4.0)));
    float n1 = fbm(p);
    float n2 = fbm(p * 1.7 + float2(n1 * 1.3, 3.1) + 0.1 * float2(sin(fase(5.0)), cos(fase(6.0))));
    float3 cor = mix(cores[0].rgb, cores[1].rgb, smoothstep(0.3, 0.75, n1));
    cor = mix(cor, cores[2].rgb, smoothstep(0.45, 0.85, n2) * smoothstep(0.3, 0.7, n1));
    cor = mix(cor, cores[3].rgb, smoothstep(0.6, 0.95, n2 * (0.5 + n1)));
    float2 g = uv * 90.0;
    float2 celula = floor(g);
    float h = hash(celula);
    float2 centro = celula + float2(hash(celula + 1.3), hash(celula + 7.7));
    float dpx = length(g - centro) * size.y / 90.0;
    float estrela = step(0.985, h) * exp(-dpx * dpx / 1.4) * (0.4 + 0.6 * (h - 0.985) / 0.015);
    cor = mix(cor, cores[4].rgb, estrela);
    return saida(cor, fc);
}
"""

/** A Biblioteca, na ordem da escolha de Fundo. As paletas vão do escuro ao claro, todas abaixo de L 0.183 (§3). */
val ARTES = listOf(
    Arte("aurora", R.string.arte_aurora, cores(0xFF05081A, 0xFF101A36, 0xFF1D8463, 0xFF247A80, 0xFF5A4A8E), SRC_AURORA),
    Arte("dunas", R.string.arte_dunas, cores(0xFF1A1030, 0xFF5A3048, 0xFF9A5A44, 0xFFA4673A, 0xFF2A141E), SRC_DUNAS),
    Arte("ondas", R.string.arte_ondas, cores(0xFF040E1C, 0xFF0B2A48, 0xFF135A72, 0xFF1E7E86, 0xFF4C7A8A), SRC_ONDAS),
    Arte("nevoa", R.string.arte_nevoa, cores(0xFF0C0E18, 0xFF22263F, 0xFF3C3964, 0xFF5E4676, 0xFF7C6C92), SRC_NEVOA),
    Arte("seda", R.string.arte_seda, cores(0xFF03100E, 0xFF0A2C28, 0xFF13524A, 0xFF1F7466, 0xFF467E6E), SRC_SEDA),
    Arte("crepusculo", R.string.arte_crepusculo, cores(0xFF0E0A1F, 0xFF3A1840, 0xFF7A2E40, 0xFFA2483A, 0xFFB45A38), SRC_CREPUSCULO),
    Arte("nebulosa", R.string.arte_nebulosa, cores(0xFF04040C, 0xFF1A1040, 0xFF5A1E5E, 0xFF2A5A8A, 0xFF726E88), SRC_NEBULOSA),
)
