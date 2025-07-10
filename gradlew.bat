@echo off
@rem -----------------------------------------------------------------------------
@rem Gradle startup script for Windows
@rem -----------------------------------------------------------------------------

set DIR=%~dp0
set APP_BASE_NAME=%~n0
set APP_HOME=%DIR%

@rem Execute Gradle
"%APP_HOME%\gradle\wrapper\gradle-wrapper.jar" %*
