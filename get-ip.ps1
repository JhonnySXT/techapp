# Скрипт для получения IP адреса компьютера
Write-Host "=== IP адреса вашего компьютера ===" -ForegroundColor Green
Write-Host ""

# Получаем все сетевые адаптеры
$adapters = Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.IPAddress -notlike "127.*" -and $_.IPAddress -notlike "169.254.*" }

if ($adapters) {
    Write-Host "Используйте один из этих адресов для доступа с Android устройства:" -ForegroundColor Yellow
    Write-Host ""
    foreach ($adapter in $adapters) {
        $interface = Get-NetAdapter | Where-Object { $_.ifIndex -eq $adapter.InterfaceIndex }
        Write-Host "  Интерфейс: $($interface.Name)" -ForegroundColor Cyan
        Write-Host "  IP адрес:  $($adapter.IPAddress)" -ForegroundColor White
        Write-Host "  URL для Android: http://$($adapter.IPAddress):8081/api/v1" -ForegroundColor Green
        Write-Host ""
    }
} else {
    Write-Host "Не удалось найти IP адреса" -ForegroundColor Red
}

Write-Host "=== Для эмулятора используйте: ===" -ForegroundColor Yellow
Write-Host "  http://10.0.2.2:8081/api/v1" -ForegroundColor Green
Write-Host ""
Write-Host "Нажмите любую клавишу для выхода..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")

