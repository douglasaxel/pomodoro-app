package com.exemplo.pomodoro.dados

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.view.WindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

private const val PASTA = "fundos"

fun arquivoDaFoto(ctx: Context, caminho: String) = File(ctx.filesDir, caminho)

/**
 * Copia a foto do Photo Picker para o armazenamento interno, reduzida para cobrir a tela.
 * A URI do Picker é temporária: a cópia mantém o Fundo mesmo se o original sumir da galeria.
 */
suspend fun importarFoto(ctx: Context, uri: Uri): Fundo.Foto = withContext(Dispatchers.IO) {
    val tela = ctx.getSystemService(WindowManager::class.java).maximumWindowMetrics.bounds
    val bmp = ImageDecoder.decodeBitmap(ImageDecoder.createSource(ctx.contentResolver, uri)) { dec, info, _ ->
        // Escala que ainda cobre a tela (Crop). Nunca amplia. O ImageDecoder já aplica a rotação EXIF.
        val s = maxOf(1f, minOf(info.size.width / tela.width().toFloat(), info.size.height / tela.height().toFloat()))
        dec.setTargetSize((info.size.width / s).toInt(), (info.size.height / s).toInt())
        dec.allocator = ImageDecoder.ALLOCATOR_SOFTWARE // getPixels não lê bitmap HARDWARE
    }
    val amostra = Bitmap.createScaledBitmap(bmp, 48, maxOf(1, 48 * bmp.height / bmp.width), true)
    val pixels = IntArray(amostra.width * amostra.height).also { amostra.getPixels(it, 0, amostra.width, 0, 0, amostra.width, amostra.height) }
    val caminho = "$PASTA/${UUID.randomUUID()}.jpg"
    arquivoDaFoto(ctx, caminho).apply { parentFile!!.mkdirs() }.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
    Fundo.Foto(caminho, percentil90(pixels))
}

/** Todas as fotos copiadas, como caminhos relativos a `filesDir`. */
fun fotosGuardadas(ctx: Context): List<String> =
    File(ctx.filesDir, PASTA).list()?.map { "$PASTA/$it" }.orEmpty()

suspend fun apagarFotos(ctx: Context, caminhos: List<String>) = withContext(Dispatchers.IO) {
    caminhos.forEach { arquivoDaFoto(ctx, it).delete() }
}
