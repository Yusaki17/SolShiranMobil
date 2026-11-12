param([Parameter(Mandatory=$true)][string]$Url)
$ErrorActionPreference = 'Stop'
$uri = [Uri]$Url
if (!$uri.IsAbsoluteUri -or $uri.Scheme -ne 'https' -or $uri.UserInfo -or $uri.Query -or $uri.Fragment -or $uri.AbsolutePath -ne '/') {
    throw 'Indica únicamente el origen HTTPS público, por ejemplo https://tu-dominio.ngrok-free.dev'
}
$origen = $uri.GetLeftPart([System.UriPartial]::Authority)
$archivo = Join-Path $PSScriptRoot '../shared/src/commonMain/kotlin/pe/edu/upeu/solshiran/data/remote/ApiConfig.kt'
$contenido = "package pe.edu.upeu.solshiran.data.remote`n`n// URL pública del gateway, sin credenciales dentro del APK.`nconst val URL_BASE = `"$origen/api/v1`"`n"
[IO.File]::WriteAllText($archivo, $contenido)
Write-Output "App configurada: $origen/api/v1"
Write-Output "Pagos-service: MP_FRONTEND_URL=$origen"
Write-Output "Webhook de Mercado Pago: $origen/api/v1/pagos/webhook"
