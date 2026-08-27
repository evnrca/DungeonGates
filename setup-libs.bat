@echo off
REM setup-libs.bat - Copy required JARs from your Minecraft server
REM Run this from the DungeonDoors project directory

set PROJECT_DIR=%~dp0
set LIBS_DIR=%PROJECT_DIR%libs

if not exist "%LIBS_DIR%" mkdir "%LIBS_DIR%"

echo.
echo ============================================
echo  DungeonGates - Copy Server JARs to libs/
echo ============================================
echo.

REM Get server path from user
set /p SERVER_PLUGINS="Enter path to server plugins folder (e.g., C:\server\plugins): "

if not exist "%SERVER_PLUGINS%" (
    echo Error: Folder not found: %SERVER_PLUGINS%
    pause
    exit /b 1
)

echo.
echo Copying JARs from %SERVER_PLUGINS% ...
echo.

REM Find and copy Paper API (paper-*.jar or paper-*.jar)
for %%f in ("%SERVER_PLUGINS%\paper-*.jar") do (
    if exist "%%f" (
        echo Found: %%~nxf
        copy "%%f" "%LIBS_DIR%\paper-api.jar" >nul
    )
)

REM Find and copy WorldGuard
for %%f in ("%SERVER_PLUGINS%\WorldGuard-*.jar") do (
    if exist "%%f" (
        echo Found: %%~nxf
        copy "%%f" "%LIBS_DIR%\worldguard.jar" >nul
    )
)

REM Find and copy MythicMobs
for %%f in ("%SERVER_PLUGINS%\MythicMobs-*.jar") do (
    if exist "%%f" (
        echo Found: %%~nxf
        copy "%%f" "%LIBS_DIR%\mythicmobs.jar" >nul
    )
)

REM Find and copy PlaceholderAPI
for %%f in ("%SERVER_PLUGINS%\PlaceholderAPI-*.jar") do (
    if exist "%%f" (
        echo Found: %%~nxf
        copy "%%f" "%LIBS_DIR%\placeholderapi.jar" >nul
    )
)

echo.
echo Done! JARs copied to %LIBS_DIR%:
dir /b "%LIBS_DIR%\*.jar" 2>nul || echo (none found)
echo.
pause