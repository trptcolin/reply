@echo off

REM NOTE: assumes dependencies in lib directory (leiningen 1 style).
REM You can put these there with:
REM   mvn dependency:copy-dependencies -DoutputDirectory=lib
REM See README for normal Usage.

SETLOCAL enabledelayedexpansion

SET CURRENT_DIR=%~dp0
SET BASE_DIR=%CURRENT_DIR%..

SET cp="%BASE_DIR%\src\clj";"%BASE_DIR%\classes";"%BASE_DIR%\target\classes"

FOR %%x IN ("%BASE_DIR%\lib\*.jar") DO (
  SET cp=!cp!;"%%~pfx%"
)

java -cp %cp% reply.ReplyMain %*

ENDLOCAL
