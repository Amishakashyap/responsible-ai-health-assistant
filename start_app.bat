@echo off
setlocal EnableDelayedExpansion

title AI Health Trainer - Launch Script
color 0A

echo ============================================================
echo   AI Health Trainer - Full Launch Script
echo ============================================================
echo.

:: ── Paths ────────────────────────────────────────────────────
set "OLLAMA_EXE=%LOCALAPPDATA%\Programs\Ollama\ollama.exe"
set "ADB_EXE=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "PROJECT_DIR=%~dp0"
set "APK_PATH=%PROJECT_DIR%app\build\outputs\apk\debug\app-debug.apk"
set "OLLAMA_MODEL=llama3.2:3b"

:: ── Step 1: Verify Ollama is installed ───────────────────────
echo [1/5] Checking Ollama...
if not exist "%OLLAMA_EXE%" (
    echo ERROR: Ollama not found at: %OLLAMA_EXE%
    echo        Download from https://ollama.com/download
    pause
    exit /b 1
)
echo       Found: %OLLAMA_EXE%

:: ── Step 2: Check GPU driver and start Ollama with GPU ───────
echo.
echo [2/5] Checking GPU and starting Ollama server...

:: Check NVIDIA driver version (need >= 522 for CUDA on Ollama 0.18+)
set "GPU_OK=0"
nvidia-smi --query-gpu=name,memory.total --format=csv,noheader >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    for /f "tokens=3 delims=, " %%V in ('nvidia-smi "--query-gpu=driver_version" "--format=csv,noheader" 2^>nul') do set "DRIVER_VER=%%V"
    for /f "tokens=1 delims=." %%M in ("!DRIVER_VER!") do set "DRIVER_MAJOR=%%M"
    for /f "tokens=*" %%G in ('nvidia-smi "--query-gpu=name,memory.total" "--format=csv,noheader" 2^>nul') do echo       GPU detected: %%G
    if !DRIVER_MAJOR! GEQ 522 (
        echo       Driver !DRIVER_VER! ^>= 522. GPU acceleration ENABLED.
        set "GPU_OK=1"
    ) else (
        echo.
        echo  *** WARNING: NVIDIA driver !DRIVER_VER! is too old for GPU acceleration ***
        echo  *** Ollama needs driver ^>= 522 ^(supports CUDA 11.8+^)                 ***
        echo  *** Download latest driver: https://www.nvidia.com/drivers             ***
        echo  *** Ollama will fall back to CPU ^(SLOW^) until you update the driver   ***
        echo.
    )
) else (
    echo       WARNING: nvidia-smi not found. GPU acceleration unavailable.
)

:: Force Ollama to use GPU when available
set "OLLAMA_NUM_GPU=1"
set "CUDA_VISIBLE_DEVICES=0"

curl -s http://localhost:11434/api/tags >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo       Ollama is already running.
) else (
    start "" /B "%OLLAMA_EXE%" serve
    echo       Waiting for Ollama to start...
    :wait_ollama
    timeout /t 2 /nobreak >nul
    curl -s http://localhost:11434/api/tags >nul 2>&1
    if !ERRORLEVEL! NEQ 0 goto wait_ollama
    echo       Ollama server started successfully.
)

:: ── Step 3: Ensure model is pulled ───────────────────────────
echo.
echo [3/5] Checking model: %OLLAMA_MODEL%...
"%OLLAMA_EXE%" list 2>&1 | findstr /I "%OLLAMA_MODEL%" >nul
if %ERRORLEVEL% EQU 0 (
    echo       Model already available.
) else (
    echo       Pulling model (this may take a few minutes on first run)...
    "%OLLAMA_EXE%" pull %OLLAMA_MODEL%
    if !ERRORLEVEL! NEQ 0 (
        echo ERROR: Failed to pull model %OLLAMA_MODEL%
        pause
        exit /b 1
    )
    echo       Model ready.
)

:: ── Step 4: Build the Android app ────────────────────────────
echo.
echo [4/5] Building Android app...
set "PATH=%JAVA_HOME%\bin;%PATH%"
pushd "%PROJECT_DIR%"
call gradlew.bat :app:assembleDebug --quiet
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Build failed. Run gradlew.bat :app:assembleDebug manually to see errors.
    popd
    pause
    exit /b 1
)
popd
echo       Build successful.

:: ── Step 5: Install APK on connected device ───────────────────
echo.
echo [5/5] Installing APK on device...
if not exist "%ADB_EXE%" (
    echo WARNING: ADB not found at: %ADB_EXE%
    echo          APK is at: %APK_PATH%
    echo          Install manually via Android Studio or drag-and-drop.
    goto done
)

"%ADB_EXE%" devices 2>&1 | findstr /R "device$" >nul
if %ERRORLEVEL% NEQ 0 (
    echo WARNING: No Android device/emulator connected.
    echo          Connect a device with USB debugging enabled, then run:
    echo          "%ADB_EXE%" install -r "%APK_PATH%"
    goto done
)

"%ADB_EXE%" install -r "%APK_PATH%"
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: APK install failed.
    pause
    exit /b 1
)
echo       APK installed successfully.

:: Launch the app on the device
echo       Launching app...
"%ADB_EXE%" shell am start -n com.example.foodtracker/.MainActivity >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo       App launched on device.
) else (
    echo       Could not auto-launch. Open the app manually on your device.
)

:: Show GPU vs CPU status
echo.
echo       Checking inference processor...
for /f "tokens=*" %%L in ('"%OLLAMA_EXE%" ps 2^>nul ^| findstr /V "NAME"') do (
    echo       %%L
)

:done
echo.
echo ============================================================
echo   Done! Ollama is running at http://localhost:11434
echo   Model:  %OLLAMA_MODEL%
echo   APK:    %APK_PATH%
if "%GPU_OK%"=="1" (
    echo   GPU:    GTX 1650 - CUDA ENABLED
) else (
    echo   GPU:    NOT ACTIVE - Update driver ^>= 522 for 3-5x speedup
    echo          https://www.nvidia.com/drivers
)
echo ============================================================
echo.
pause
endlocal
