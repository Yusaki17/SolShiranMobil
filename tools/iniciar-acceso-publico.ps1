$ErrorActionPreference = 'Stop'
$servidor = Join-Path $PSScriptRoot 'acceso-publico.cjs'
$proceso = Start-Process -FilePath 'node' -ArgumentList @($servidor) -WindowStyle Hidden -PassThru
try {
    & ngrok http 18100 --url https://banana-backlight-parade.ngrok-free.dev
} finally {
    if (!$proceso.HasExited) { Stop-Process -Id $proceso.Id }
}
