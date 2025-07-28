@echo off
REM このバッチファイルは、指定したディレクトリ内にプロジェクト構造を作成します。

REM ★ 作成先のルートフォルダを変数に設定
SET "TARGET_DIR=src\main\java\jp\kobe_u\cs\daikibo\MARUNAGE"

echo ディレクトリを作成しています...
echo 作成先: %TARGET_DIR%
echo.

REM ★ 変数を使って、指定したフォルダ内にディレクトリを作成
mkdir "%TARGET_DIR%\app\controller"
mkdir "%TARGET_DIR%\app\service"
mkdir "%TARGET_DIR%\app\repository"
mkdir "%TARGET_DIR%\app\entity"
mkdir "%TARGET_DIR%\resources\static\css"
mkdir "%TARGET_DIR%\resources\static\js"
mkdir "%TARGET_DIR%\resources\templates"
mkdir "%TARGET_DIR%\data"

echo ファイルを作成しています...

REM ★ 変数を使って、指定したフォルダ内に空のファイルを作成
type nul > "%TARGET_DIR%\app\controller\ShiftController.java"
type nul > "%TARGET_DIR%\app\service\ShiftService.java"
type nul > "%TARGET_DIR%\app\repository\ShiftRepository.java"
type nul > "%TARGET_DIR%\app\entity\Shift.java"
type nul > "%TARGET_DIR%\resources\static\css\style.css"
type nul > "%TARGET_DIR%\resources\static\js\script.js"
type nul > "%TARGET_DIR%\resources\templates\index.html"
type nul > "%TARGET_DIR%\resources\templates\shift_confirmation.html"
type nul > "%TARGET_DIR%\data\shift_requests.csv"
type nul > "%TARGET_DIR%\pom.xml"

echo.
echo 作成が完了しました。
echo.

REM 作成後のディレクトリ構造を確認
tree "%TARGET_DIR%" /F

echo.
echo 何かキーを押すとウィンドウを閉じます。
pause > nul