package pe.edu.upeu.solshiran

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.koin.core.context.GlobalContext
import pe.edu.upeu.solshiran.di.iniciarKoin
import pe.edu.upeu.solshiran.domain.model.FotoElegida
import pe.edu.upeu.solshiran.platform.Plataforma
import pe.edu.upeu.solshiran.presentation.*

class MainActivity : ComponentActivity(), Plataforma {
    override fun leerModoOscuro(): Boolean? {
        val preferencias = getSharedPreferences("apariencia", MODE_PRIVATE)
        return if (preferencias.contains("oscuro")) preferencias.getBoolean("oscuro", false) else null
    }
    override fun guardarModoOscuro(oscuro: Boolean) {
        getSharedPreferences("apariencia", MODE_PRIVATE).edit().putBoolean("oscuro", oscuro).apply()
    }
    private var completarFoto: ((FotoElegida?) -> Unit)? = null
    private val selector = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val callback=completarFoto; completarFoto=null
        if(uri==null) { callback?.invoke(null); return@registerForActivityResult }
        try {
            val tipo=contentResolver.getType(uri) ?: ""
            require(tipo in listOf("image/jpeg","image/png")) { "Selecciona una foto JPG o PNG." }
            val bytes=contentResolver.openInputStream(uri)?.use { entrada ->
                val salida=java.io.ByteArrayOutputStream()
                val buffer=ByteArray(8192)
                while(true) {
                    val leidos=entrada.read(buffer)
                    if(leidos<0) break
                    require(salida.size()+leidos<=2*1024*1024) { "La foto supera los 2 MB." }
                    salida.write(buffer,0,leidos)
                }
                salida.toByteArray()
            } ?: error("No se pudo leer la foto.")
            callback?.invoke(FotoElegida(bytes,tipo,"foto"))
        } catch(e: Exception) {
            Toast.makeText(this,e.message ?: "No se pudo leer la foto.",Toast.LENGTH_LONG).show()
            callback?.invoke(null)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(GlobalContext.getOrNull()==null) iniciarKoin(applicationContext)
        val vm=ViewModelProvider(this,object: ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T: ViewModel> create(modelClass: Class<T>): T = GlobalContext.get().get<AppViewModel>() as T
        })[AppViewModel::class.java]
        setContent { SolShiranApp(vm,this) }
    }
    override fun elegirFoto(resultado: (FotoElegida?) -> Unit) {
        completarFoto=resultado
        selector.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    override fun abrirPago(url: String) {
        try {
            require(url.startsWith("https://")) { "El enlace de pago no es válido." }
            startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE))
        } catch(e: Exception) { Toast.makeText(this,"No se pudo abrir la pasarela.",Toast.LENGTH_LONG).show() }
    }
}
