@echo off
@REM Script to run a command

if NOT "%DARQROOT%" == "" goto :okRoot
echo ARQROOT not set
exit /B

:okRoot
call %DARQROOT%\bin\make_classpath.bat %DARQROOT%

java -cp %CP% darq.darq %1 %2 %3 %4 %5 %6 %7 %8 %9
exit /B
