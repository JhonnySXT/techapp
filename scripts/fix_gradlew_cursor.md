# Исправление проблем с gradlew в Cursor

## Диагностика

### Что проверить:

1. **Gradle работает в терминале?**
   ```powershell
   .\gradlew.bat --version
   ```
   Если работает - проблема в настройках Cursor, а не в Gradle.

2. **Проверка Java:**
   ```powershell
   java -version
   $env:JAVA_HOME
   ```

3. **Проверка gradle.properties:**
   - Должна быть строка: `org.gradle.java.home=C:/Program Files/Android/Android Studio/jbr`

## Решения проблем

### Проблема 1: Cursor не находит gradlew

**Решение:**
1. Откройте настройки Cursor: `Ctrl + ,`
2. Найдите "Terminal"
3. Убедитесь, что используется PowerShell (не cmd)
4. Проверьте "Terminal > Integrated > Shell: Windows"
   - Должно быть: `C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe`

### Проблема 2: Gradle не находит Java

**Решение 1 (через gradle.properties):**
Убедитесь, что в `gradle.properties` есть:
```properties
org.gradle.java.home=C:/Program Files/Android/Android Studio/jbr
```

**Решение 2 (через переменные окружения):**
```powershell
[System.Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Android\Android Studio\jbr', 'User')
```
После этого перезапустите Cursor.

### Проблема 3: Проблемы с путями (пробелы, кириллица)

**Решение:**
- Убедитесь, что путь к проекту не содержит пробелов или спецсимволов
- Если есть пробелы, используйте кавычки: `.\gradlew.bat` вместо `.\gradlew.bat`

### Проблема 4: Cursor использует неправильную оболочку

**Решение:**
1. Откройте терминал в Cursor
2. Проверьте, какая оболочка используется (должна быть PowerShell)
3. Если cmd - измените в настройках

### Проблема 5: Права доступа

**Решение:**
```powershell
# Проверьте права на gradlew.bat
Get-Acl gradlew.bat

# Если нужно, дайте права на выполнение
icacls gradlew.bat /grant Everyone:RX
```

## Быстрое исправление

Создайте файл `.vscode/settings.json` в корне проекта:

```json
{
  "terminal.integrated.defaultProfile.windows": "PowerShell",
  "terminal.integrated.profiles.windows": {
    "PowerShell": {
      "source": "PowerShell",
      "icon": "terminal-powershell",
      "path": "C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe"
    }
  },
  "java.configuration.runtimes": [
    {
      "name": "JavaSE-17",
      "path": "C:\\Program Files\\Android\\Android Studio\\jbr",
      "default": true
    }
  ]
}
```

## Проверка после исправления

1. Закройте и откройте Cursor заново
2. Откройте терминал в Cursor (`Ctrl + ~`)
3. Выполните: `.\gradlew.bat --version`
4. Если видите версию Gradle - всё работает!

## Альтернатива: Использование полного пути

Если ничего не помогает, используйте полный путь:

```powershell
& "C:\Users\Jhonny\AndroidStudioProjects\Techapp\gradlew.bat" --version
```

Или создайте алиас в PowerShell профиле:

```powershell
# Откройте профиль
notepad $PROFILE

# Добавьте:
function gradlew { & "$PSScriptRoot\gradlew.bat" $args }
```

## Если ничего не помогло

1. Проверьте логи Cursor: `Help` → `Toggle Developer Tools` → вкладка `Console`
2. Попробуйте запустить Cursor от имени администратора
3. Переустановите Gradle Wrapper:
   ```powershell
   .\gradlew.bat wrapper --gradle-version 8.13
   ```



