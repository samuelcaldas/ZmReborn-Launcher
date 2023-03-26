@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  Gradle startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

@rem Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

@rem End local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" endlocal

@rem Escape application args
set CMD_LINE_ARGS=
set _SKIP=2

:loop
if "x%~1" == "x" goto execute
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto loop

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

@rem Execute Gradle
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %CMD_LINE_ARGS%

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
echo.
echo ERROR: Unable to start the daemon process.
echo.
echo This problem might be caused by incorrect configuration of the daemon.
echo For example, an unrecognized jvm option is used.
echo Please refer to the user guide chapter on the daemon at https://docs.gradle.org/4.2.1/userguide/gradle_daemon.html
echo Please read the following process output to find out more:
echo -----------------------
echo.

:endfail
if "%OS%"=="Windows_NT" endlocal

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
