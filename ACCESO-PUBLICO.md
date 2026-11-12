# Acceso desde cualquier red (desarrollo)

URL del APK 1.1: https://banana-backlight-parade.ngrok-free.dev

1. Mantén MySQL y los microservicios de Sol-de-Shiran6 encendidos; el gateway debe escuchar en 18061.
2. Desde esta carpeta ejecuta `./tools/iniciar-acceso-publico.ps1` y deja la terminal abierta. Si ya está activo el túnel iniciado por Codex, no abras otro.
3. Instala `androidApp/build/outputs/apk/debug/androidApp-debug.apk` como actualización. Usa datos móviles para comprobar el acceso externo.

El adaptador local escucha exclusivamente en 127.0.0.1:18100. Publica `/api/v1/` hacia el gateway y `/pago/resultado` como retorno móvil. No publica Eureka, Config Server ni MySQL. Los permisos y la firma del webhook siguen siendo validados por el backend. No contiene tokens.

## Mercado Pago

En Environment variables de la configuración de pagos-service en IntelliJ, conserva tus credenciales y añade:

```text
MP_FRONTEND_URL=https://banana-backlight-parade.ngrok-free.dev
MP_WEBHOOK_URL=https://banana-backlight-parade.ngrok-free.dev/api/v1/pagos/webhook
```

Reinicia pagos-service. Las órdenes creadas antes del reinicio pueden conservar el retorno anterior; consulta primero su estado, no repitas un pago incierto.

En Mercado Pago, configura Webhooks del entorno correspondiente con `https://banana-backlight-parade.ngrok-free.dev/api/v1/pagos/webhook`, evento Order (Mercado Pago), y conserva su firma en MP_WEBHOOK_SECRET. La URL pública no resuelve una autorización 403 del proveedor: las credenciales deben ser válidas para la integración.

La página de retorno nunca toma el resultado de la URL como prueba de pago. Regresa a la app y consulta el estado; el backend lo verifica con Mercado Pago.

## Actualizaciones

La versión actual tiene versionCode 2. Para actualizar aumenta versionCode, conserva applicationId y la misma clave de firma. Este APK de desarrollo usa la clave debug actual de esta computadora: conserva esa clave para actualizar estas instalaciones. Antes de distribuir una versión de producción, usa un keystore release propio y respaldado. No se ha cambiado la firma para evitar obligar a desinstalar la versión que ya tienes.

Para cambiar de dominio ejecuta `./tools/configurar-url.ps1 -Url https://tu-dominio` y recompila. Para conservar el dominio al iniciar nuevamente, el script de inicio pasa --url explícitamente a ngrok. Su disponibilidad depende de tu cuenta ngrok.

Comandos de compilación:

```powershell
./gradlew.bat :androidApp:assembleDebug :shared:testAndroidHostTest
```

Esto es acceso temporal de desarrollo, no alojamiento permanente. Cuando la computadora, el adaptador, ngrok o el backend se apagan, el servicio deja de estar disponible. Un servidor permanente se puede configurar después conservando la misma arquitectura.
