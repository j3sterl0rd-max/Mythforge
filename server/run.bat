@echo off
if not exist "ops\updates\beta-roster-pending.json" goto start_server
python "ops\apply-pending-beta-roster.py"
if errorlevel 1 exit /b 1
:start_server
REM Forge requires a configured set of both JVM and program arguments.
REM Add custom JVM arguments to the user_jvm_args.txt
REM Add custom program arguments {such as nogui} to this file in the next line before the %* or
REM  pass them to this script directly
start "" /HIGH /WAIT /B java @user_jvm_args.txt @libraries/net/neoforged/neoforge/21.1.263/win_args.txt %*
pause
