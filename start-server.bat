@echo off
echo ========================================
echo   Запуск сервера Загородного клуба "Дача"
echo ========================================
echo.

cd /d "%~dp0"
echo Текущая директория: %CD%
echo.

echo Запускаю сервер на порту 8081...
echo.
echo После запуска сервер будет доступен по адресу:
echo   http://localhost:8081
echo.
echo Для остановки нажмите Ctrl+C
echo.
echo ========================================
echo.

echo.
echo Сборка проекта...
call gradlew.bat :server:build
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ========================================
    echo   ОШИБКА СБОРКИ!
    echo ========================================
    echo Проверьте ошибки выше и исправьте их.
    pause
    exit /b 1
)

echo.
echo Сборка успешна! Запускаю сервер...
echo.
call gradlew.bat :server:run

pause



