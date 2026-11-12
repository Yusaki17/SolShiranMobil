package pe.edu.upeu.solshiran.di

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import org.koin.core.context.startKoin
import org.koin.dsl.module
import pe.edu.upeu.solshiran.data.local.AlmacenSeguroAndroid
import pe.edu.upeu.solshiran.db.SolDb
import pe.edu.upeu.solshiran.domain.repository.AlmacenSesion

fun iniciarKoin(context: Context) = startKoin {
    modules(appModule,module {
        single<AlmacenSesion> { AlmacenSeguroAndroid(context.applicationContext) }
        single<SqlDriver> { AndroidSqliteDriver(SolDb.Schema,context.applicationContext,"sol_shiran.db") }
        single { HttpClient(OkHttp) {
            followRedirects = false
            install(HttpTimeout) { requestTimeoutMillis=20000; connectTimeoutMillis=8000; socketTimeoutMillis=20000 }
        } }
    })
}
