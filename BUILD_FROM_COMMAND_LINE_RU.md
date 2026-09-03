# Сборка VK Music TV APK из командной строки

Эта инструкция описывает сборку проекта **без Android Studio** на Windows, macOS и Linux. Android Studio не требуется: достаточно установить **JDK**, **Android SDK Command-Line Tools** и **Gradle**, затем выполнить несколько команд.

## 1. Что получится в результате

После успешной сборки появится отладочный APK-файл:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Это **debug APK** для установки на собственный Android TV, эмулятор, NVIDIA Shield, Mi Box или другое совместимое устройство. Для публикации в Google Play потребуется отдельная release-сборка, ключ подписи и подготовка метаданных.

## 2. Требования

| Компонент | Рекомендуемая версия | Назначение |
|---|---:|---|
| JDK | 17 или 21 | Компиляция Kotlin и Android Gradle Plugin |
| Gradle | 8.10.2 | Запуск сборки проекта |
| Android SDK Platform | 35 | Компиляция проекта, заданная в `app/build.gradle.kts` |
| Android SDK Build-Tools | 35.0.0 | Создание APK |
| Android SDK Platform-Tools | Актуальная версия | Установка APK через `adb` |

Проект использует `minSdk = 21`, поэтому он рассчитан на Android 5.0 и новее. Для сборки используется `compileSdk = 35`.

## 3. Установка JDK

### Windows

Установите JDK 17 или 21, например Temurin, Microsoft Build of OpenJDK или Oracle JDK. После установки откройте новое окно PowerShell и проверьте:

```powershell
java -version
```

В выводе должна быть версия 17 или 21.

### macOS

Если используется Homebrew:

```bash
brew install --cask temurin@17
java -version
```

Если macOS использует несколько JDK, задайте JDK 17 для текущего терминала:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
java -version
```

### Ubuntu/Debian Linux

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk unzip wget
java -version
```

Для Fedora можно установить `java-17-openjdk-devel` через `dnf`.

## 4. Установка Android SDK Command-Line Tools

Скачайте архив **Command line tools only** со страницы [Android Studio downloads][1]. Android Studio устанавливать не нужно.

Распакуйте архив в каталог SDK. Важно сохранить структуру `cmdline-tools/latest/bin/sdkmanager`.

### Windows PowerShell

```powershell
$Sdk = "$env:LOCALAPPDATA\Android\Sdk"
New-Item -ItemType Directory -Force "$Sdk\cmdline-tools" | Out-Null

# После скачивания commandlinetools-win-*.zip замените путь к архиву:
Expand-Archive -Force "$HOME\Downloads\commandlinetools-win-latest.zip" "$env:TEMP\android-cmdline"
Move-Item -Force "$env:TEMP\android-cmdline\cmdline-tools" "$Sdk\cmdline-tools\latest"

[Environment]::SetEnvironmentVariable("ANDROID_HOME", $Sdk, "User")
[Environment]::SetEnvironmentVariable("ANDROID_SDK_ROOT", $Sdk, "User")
[Environment]::SetEnvironmentVariable("Path", $env:Path + ";$Sdk\platform-tools;$Sdk\cmdline-tools\latest\bin", "User")

$env:ANDROID_HOME = $Sdk
$env:ANDROID_SDK_ROOT = $Sdk
$env:Path += ";$Sdk\platform-tools;$Sdk\cmdline-tools\latest\bin"
```

Если имя скачанного архива отличается, укажите его фактическое имя вместо `commandlinetools-win-latest.zip`.

### macOS и Linux

```bash
mkdir -p "$HOME/android-sdk/cmdline-tools"

# Скачайте архив Command line tools со страницы [1], затем укажите фактический путь:
unzip -q "$HOME/Downloads/commandlinetools-*.zip" -d /tmp/android-cmdline
mv /tmp/android-cmdline/cmdline-tools "$HOME/android-sdk/cmdline-tools/latest"

export ANDROID_HOME="$HOME/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
```

Чтобы переменные сохранялись после перезапуска терминала, добавьте последние три строки в `~/.bashrc` или `~/.zshrc`, а затем выполните `source ~/.bashrc` либо `source ~/.zshrc`.

Проверьте установку:

```bash
sdkmanager --version
```

В Windows используйте `sdkmanager.bat --version`, если команда `sdkmanager` не распознаётся.

## 5. Установка SDK-пакетов и принятие лицензий

Выполните команду в новом терминале с настроенными переменными окружения:

### macOS и Linux

```bash
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```

### Windows PowerShell

```powershell
cmd /c "yes | sdkmanager.bat --licenses"
sdkmanager.bat "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```

Если `yes` отсутствует в Windows, запускайте `sdkmanager.bat --licenses` и отвечайте `y` на каждый вопрос.

Проверьте, что каталог `platforms;android-35` установлен внутри SDK. Для Linux/macOS это обычно:

```text
$ANDROID_HOME/platforms/android-35
```

Для Windows:

```text
%ANDROID_HOME%\platforms\android-35
```

## 6. Установка Gradle без Android Studio

Скачайте бинарный ZIP Gradle 8.10.2 со страницы [Gradle Releases][2]. Распакуйте его, например, в:

```text
Windows: C:\Gradle\gradle-8.10.2
macOS/Linux: $HOME/gradle/gradle-8.10.2
```

Добавьте каталог `bin` в `PATH`.

### Windows PowerShell

```powershell
[Environment]::SetEnvironmentVariable("GRADLE_HOME", "C:\Gradle\gradle-8.10.2", "User")
[Environment]::SetEnvironmentVariable("Path", $env:Path + ";C:\Gradle\gradle-8.10.2\bin", "User")
$env:GRADLE_HOME = "C:\Gradle\gradle-8.10.2"
$env:Path += ";C:\Gradle\gradle-8.10.2\bin"
gradle --version
```

### macOS/Linux

```bash
mkdir -p "$HOME/gradle"
unzip -q "$HOME/Downloads/gradle-8.10.2-bin.zip" -d "$HOME/gradle"
export GRADLE_HOME="$HOME/gradle/gradle-8.10.2"
export PATH="$GRADLE_HOME/bin:$PATH"
gradle --version
```

В выводе должны быть видны Gradle 8.10.2 и JVM 17 или 21.

## 7. Распаковка исходного проекта

Распакуйте архив проекта и перейдите в его корень. Корнем считается каталог, в котором находятся `settings.gradle.kts`, `build.gradle.kts` и каталог `app`.

### Windows PowerShell

```powershell
Expand-Archive -Force "$HOME\Downloads\VKMusicTV-source.zip" "$HOME\VKMusicTV"
Set-Location "$HOME\VKMusicTV"
Get-ChildItem
```

### macOS/Linux

```bash
mkdir -p "$HOME/VKMusicTV"
unzip -q "$HOME/Downloads/VKMusicTV-source.zip" -d "$HOME/VKMusicTV"
cd "$HOME/VKMusicTV"
ls
```

Если архив распакован во вложенный каталог, перейдите именно туда, где лежит `settings.gradle.kts`.

## 8. Создание Gradle Wrapper и сборка

В архиве исходников нет сгенерированных файлов Wrapper. После установки Gradle создайте Wrapper один раз:

### Windows

```powershell
gradle wrapper --gradle-version 8.10.2 --distribution-type bin
.\gradlew.bat :app:assembleDebug
```

### macOS/Linux

```bash
gradle wrapper --gradle-version 8.10.2 --distribution-type bin
chmod +x ./gradlew
./gradlew :app:assembleDebug
```

При первом запуске Gradle скачает Android Gradle Plugin и Kotlin-плагин из Google Maven и Maven Central. Поэтому компьютеру нужен доступ в интернет.

Проверка файла:

```bash
# macOS/Linux
ls -lh app/build/outputs/apk/debug/app-debug.apk

# Windows PowerShell
Get-Item .\app\build\outputs\apk\debug\app-debug.apk
```

## 9. Установка APK на Android TV через ADB

### Вариант A: USB

Включите на телевизоре режим разработчика и отладку по USB. Подключите устройство, затем выполните:

```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

На экране телевизора подтвердите разрешение отладки, если оно появится.

### Вариант B: сеть

Телевизор и компьютер должны находиться в одной сети. Узнайте IP-адрес телевизора в настройках сети, затем выполните:

```bash
adb connect 192.168.1.50:5555
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Замените `192.168.1.50` на фактический IP-адрес. На Android TV может потребоваться подтверждение RSA-ключа.

Запустить приложение можно из лаунчера телевизора. Оно отображается как **VK Music TV**.

## 10. Частые ошибки

| Ошибка | Что сделать |
|---|---|
| `JAVA_HOME is not set` | Установить JDK и задать `JAVA_HOME` на каталог JDK, затем открыть новый терминал. |
| `sdkmanager: command not found` | Проверить `PATH` и наличие `cmdline-tools/latest/bin`. В Windows использовать `sdkmanager.bat`. |
| `Failed to find target with hash string android-35` | Повторно выполнить `sdkmanager "platforms;android-35"`. |
| `License ... not accepted` | Выполнить `sdkmanager --licenses` и принять лицензии. |
| `Could not resolve ...` | Проверить интернет, корпоративный proxy и доступ к Google Maven/Maven Central. Повторить сборку. |
| `adb devices` показывает `unauthorized` | Разрешить отладку на телевизоре и снова выполнить `adb devices`. |
| Приложение не появилось в TV launcher | Проверить, что установка завершилась без ошибки и устройство поддерживает Android TV/Leanback launcher. |
| APK устанавливается, но VK не открывается | Проверить интернет на телевизоре, дату и время устройства, а также доступность `m.vk.com` в вашей сети. |

## 11. Важные ограничения текущей реализации

Приложение является WebView-оболочкой мобильной версии ВК. Авторизация, поиск, плейлисты, избранное и фактическое воспроизведение зависят от текущей версии сайта ВК и условий аккаунта. Приложение не сохраняет пароль отдельно: сессия сохраняется стандартными cookies WebView.

Для управления используется виртуальный курсор. Стрелки перемещают курсор, **OK** выполняет клик, а **Back** возвращает на предыдущую страницу. MediaSession передаёт media-кнопки пульта в веб-плеер в режиме best-effort, но поведение фонового воспроизведения может зависеть от версии WebView, прошивки телевизора и ограничений ВК.

## References

[1]: https://developer.android.com/studio "Download Android Studio and command-line tools"
[2]: https://gradle.org/releases/ "Gradle releases"
[3]: https://docs.gradle.org/current/userguide/gradle_wrapper_basics.html "Gradle Wrapper basics"
