# Sol de Shirán móvil

Aplicación Android nativa con Kotlin y Compose Multiplatform, basada en la estructura de PharmaMobil. Consume el backend de **Sol-de-Shiran6** a través de su API Gateway. Tiene un identificador de aplicación distinto (`pe.edu.upeu.solshiran`) y su propia base SQLite para la caché pública del catálogo.

## Abrir y ejecutar

1. En Android Studio, selecciona **Open** y abre esta carpeta `SolShiranMobil`, donde está `settings.gradle.kts`.
2. Configura Gradle con **JDK 21**, SDK Android **36** y sincroniza.
3. Inicia MySQL y los microservicios de Sol-de-Shiran6, incluido el gateway en **18061**. Reinicia los servicios que incorporan la carga de fotos.
4. Selecciona un emulador Android API 24 o superior (recomendado API 34/36) y ejecuta la configuración **androidApp**.

Desde PowerShell, en esta carpeta:

```powershell
.\gradlew.bat :androidApp:assembleDebug
```

APK de depuración: `androidApp/build/outputs/apk/debug/androidApp-debug.apk`.

## Conexión

La dirección está en `shared/src/commonMain/kotlin/pe/edu/upeu/solshiran/data/remote/ApiConfig.kt`.

- Emulador: `http://10.0.2.2:18061/api/v1` (configurada).
- Celular: `http://IP_DE_TU_COMPUTADORA:18061/api/v1`, con ambos dispositivos en la misma red y acceso al puerto 18061.
- Se usa HTTP para desarrollo local. Antes de distribuir en producción, configura HTTPS y desactiva `usesCleartextTraffic` en el manifiesto.
- No se inicia Docker desde esta app. Usa los mismos servicios y bases MySQL del sitio web.
- No hay cuentas ni cabañas de ejemplo. Inicia sesión con las cuentas creadas en tu backend. El registro público crea clientes; el administrador crea las otras cuentas.

## Funciones

| Perfil | Pantallas |
|---|---|
| Visitante | Catálogo, fotos y detalle de cabañas, disponibilidad, registro e inicio de sesión |
| Cliente | Reserva temporal, adelanto del 50 %, historial paginado, opiniones de estancias finalizadas, perfil y foto |
| Administrador | Cabañas con portada y galería, creación/edición/deshabilitación, usuarios por rol, reportes de ingresos y reservas, panel operativo y perfil |
| Trabajador | Panel de reservas, registro del saldo presencial, check-in, check-out con daños opcionales y perfil |
| Dueño | La cuenta se reconoce; el acceso a su perfil depende de los permisos del backend existente. El backend actual restringe varias operaciones de dueños. |

El menú se adapta al rol. Los microservicios siguen siendo quienes autorizan cada operación.

## Pagos

El botón de pago solicita una orden al backend y abre el `checkoutUrl` HTTPS en el navegador. La app no pide ni guarda números de tarjeta. Al regresar, pulsa **Consultar estado del pago** o actualiza las reservas. La aprobación depende del webhook y de Mercado Pago, nunca de una pantalla de éxito local.

El retorno de Mercado Pago mantiene la configuración web del backend. Para probar desde un celular, `MP_FRONTEND_URL` debe ser una dirección accesible desde ese celular si deseas ver la página web de retorno; la consulta del estado en la app sigue disponible. No incluye enlaces profundos Android ni SDK nativo de pago.

El registro presencial del saldo es una declaración del trabajador/administrador de que ya recibió el dinero. La clave de idempotencia se conserva al reintentar desde el mismo diálogo. El backend controla el importe y el resultado del check-in.

## Arquitectura

```text
androidApp/
  MainActivity.kt               arranque, selector de fotos y navegador externo
shared/
  src/commonMain/kotlin/pe/edu/upeu/solshiran/
    domain/model/              modelos del negocio
    domain/repository/         contratos de repositorio y sesión
    domain/usecase/            validación de estancia
    data/remote/               URL y mapeo JSON del contrato existente
    data/repository/           HTTP, persistencia y manejo de errores
    di/                        módulos Koin
    platform/                  abstracciones de plataforma
    presentation/
      auth/ catalogo/ reservas/ perfil/ gestion/ components/
      AppViewModel.kt          estado de pantalla y operaciones
      SolShiranApp.kt          navegación y tema
  src/androidMain/             Keystore, driver SQLite, motor HTTP y funciones Android
  src/commonMain/sqldelight/   esquema del catálogo local
  src/commonTest/              pruebas de reglas y mapeo
```

La sesión se cifra con AES-GCM y una clave de Android Keystore; las contraseñas no se guardan. Al cerrar sesión se limpian los datos privados de pantalla. La caché SQLDelight contiene exclusivamente el catálogo público. Se puede leer sin conexión; reservas, cobros, modificaciones y consultas privadas requieren internet. Las imágenes se cargan desde el backend y no se almacenan para uso sin conexión.

Esta entrega habilita **Android**. El módulo compartido mantiene la organización multiplataforma; iOS requiere agregar su target, motor HTTP y adaptaciones de plataforma.

## Pruebas manuales

1. Ver el catálogo sin iniciar sesión y abrir una cabaña.
2. Crear un cliente y entrar; probar contraseña incorrecta.
3. Seleccionar fechas con el calendario, consultar disponibilidad y reservar.
4. Pagar el adelanto con el entorno de prueba configurado del backend y consultar el estado.
5. Cambiar la foto de perfil y volver a entrar.
6. Entrar como ADMIN, crear/editar una cabaña, añadir fotos y administrar usuarios.
7. Entrar como trabajador; registrar un cobro recibido y realizar check-out.
8. Cortar internet: el catálogo previamente descargado debe permanecer visible; las operaciones de escritura deben mostrar error.

Las funciones administrativas de bloqueo de fechas y asignación de trabajadores permanecen en el panel web; no están incluidas en esta primera interfaz móvil.
