package pe.edu.upeu.solshiran.platform

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import org.json.JSONObject
import java.time.*

actual fun tokenVigente(token: String): Boolean = runCatching {
    val claims = JSONObject(String(Base64.decode(token.split('.')[1], Base64.URL_SAFE or Base64.NO_WRAP)))
    claims.getLong("exp") > System.currentTimeMillis() / 1000
}.getOrDefault(false)
actual fun fechaIso(milisegundos: Long): String = Instant.ofEpochMilli(milisegundos).atZone(ZoneOffset.UTC).toLocalDate().toString()
actual fun hoy(): String = LocalDate.now(ZoneId.of("America/Lima")).toString()
actual fun dentroDeUnAno(): String = LocalDate.now(ZoneId.of("America/Lima")).plusDays(366).toString()
actual fun nuevoId(): String = java.util.UUID.randomUUID().toString()
@Composable actual fun ImagenBytes(bytes: ByteArray, descripcion: String, modifier: Modifier) {
    val bitmap = remember(bytes) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
            val options = BitmapFactory.Options().apply {
                inSampleSize = (maxOf(bounds.outWidth,bounds.outHeight) / 1200).coerceAtLeast(1)
            }
            BitmapFactory.decodeByteArray(bytes,0,bytes.size,options)?.asImageBitmap()
        }.getOrNull()
    }
    if (bitmap != null) Image(bitmap,descripcion,modifier,contentScale=ContentScale.Crop)
}
