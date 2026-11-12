package pe.edu.upeu.solshiran.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject
import pe.edu.upeu.solshiran.domain.model.Sesion
import pe.edu.upeu.solshiran.domain.repository.AlmacenSesion

class AlmacenSeguroAndroid(context: Context) : AlmacenSesion {
    private val preferencias = context.getSharedPreferences("sesion_cifrada",Context.MODE_PRIVATE)
    private fun clave(): SecretKey {
        val almacen = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (almacen.getKey("sol_shiran_sesion",null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("sol_shiran_sesion",KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    override fun leer(): Sesion? = runCatching {
        val valor = preferencias.getString("datos",null) ?: return null
        val partes = valor.split(':')
        val cifrador = Cipher.getInstance("AES/GCM/NoPadding")
        cifrador.init(Cipher.DECRYPT_MODE,clave(),GCMParameterSpec(128,Base64.decode(partes[0],Base64.NO_WRAP)))
        val json = JSONObject(String(cifrador.doFinal(Base64.decode(partes[1],Base64.NO_WRAP))))
        Sesion(json.getString("token"),json.getLong("id"),json.getString("nombre"),json.getString("email"),json.getString("rol"))
    }.getOrNull()
    override fun guardar(sesion: Sesion?) {
        if(sesion == null) { preferencias.edit().clear().commit(); return }
        val texto = JSONObject().put("token",sesion.token).put("id",sesion.id).put("nombre",sesion.nombre)
            .put("email",sesion.email).put("rol",sesion.rol).toString()
        val cifrador = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE,clave()) }
        val datos = cifrador.doFinal(texto.toByteArray())
        preferencias.edit().putString("datos",Base64.encodeToString(cifrador.iv,Base64.NO_WRAP)+":"+Base64.encodeToString(datos,Base64.NO_WRAP)).commit()
    }
}
