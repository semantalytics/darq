@echo off
@REM Script to run a command

if NOT "%DARQROOT%" == "" goto :okRoot
echo DARQROOT not set
exit /B

:okRoot
call %DARQROOT%\bin\make_classpath.bat %DARQROOT%


java -Xmx1500M  -cp %CP% darq.RDFstat %1 %2 %3 %4 %5 %6 %7 %8 %9
exit /B

