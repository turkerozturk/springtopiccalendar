; This file is part of the DailyTopicTracker project.
; Please refer to the project's README.md file for additional details.
; https://github.com/turkerozturk/springtopiccalendar
;
; Copyright (c) 2025 Turker Ozturk
;
; This program is free software: you can redistribute it and/or modify
; it under the terms of the GNU General Public License as published by
; the Free Software Foundation, either version 3 of the License, or
; (at your option) any later version.
;
; This program is distributed in the hope that it will be useful,
; but WITHOUT ANY WARRANTY; without even the implied warranty of
; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
; GNU General Public License for more details.
;
; You should have received a copy of the GNU General Public License
; along with this program. If not, see <https://www.gnu.org/licenses/gpl-3.0.en.html>.
#NoTrayIcon

#include <GUIConstantsEx.au3>
#include <WindowsConstants.au3>
#include <MsgBoxConstants.au3>
#include <EditConstants.au3> ; Edit kontrolü sabitleri (ES_...)

; ===== Renk Kodları (BGR formatı) =====
Global Const $COLOR_RED     = 0xFFCCCC ; Kırmızı (B=FF)
Global Const $COLOR_GREEN   = 0xCCFFCC ; Yeşil
Global Const $COLOR_ORANGE  = 0xFFA5CC ; Turuncu (RGB(255,165,0) -> BGR(0,165,255))
Global Const $COLOR_MAGENTA = 0xFFCCFF ; Mor / Magenta

; ===== Uygulama Durumları =====
Global Const $STATUS_STOPPED  = 0
Global Const $STATUS_STARTING = 1
Global Const $STATUS_RUNNING  = 2
Global Const $STATUS_STOPPING = 3

; ===== Global Değişkenler =====
Global $g_iPID = 0               ; Java proses PID
Global $g_sConsoleOutput = ""    ; Log metninin birikeceği yer
Global $g_sPort = 8080           ; Port, application.properties içinden okuyoruz
Global $g_iAppStatus = $STATUS_STOPPED ; Başlangıçta durdurulmuş kabul ediyoruz
Global $g_bLogWindowVisible = True ; Log penceresi başta açık

; ===== GUI Pencereleri ve Kontroller =====
Global $hMainGUI, $hLogGUI
Global $idStartStop, $idShowLogs, $idExit
Global $idConsoleEdit

; --------------------------------------------------------------------------------
; application.properties içinden port numarasını okur (yoksa 8080 varsayar)
Func ReadPortNumber($sPropFilePath)
    Local $hFile = FileOpen($sPropFilePath, 0)
    If $hFile = -1 Then
        Return 8080
    EndIf

    Local $sPort = 8080
    While 1
        Local $sLine = FileReadLine($hFile)
        If @error = -1 Then ExitLoop
        If StringLeft($sLine, 12) = "server.port=" Then
            $sPort = StringTrimLeft($sLine, 12)
            ExitLoop
        EndIf
    WEnd
    FileClose($hFile)
    Return $sPort
EndFunc

; --------------------------------------------------------------------------------
; GUI buton arkaplan rengini ve metnini, $g_iAppStatus'e göre günceller
Func UpdateStartStopButtonColor()
    Switch $g_iAppStatus
        Case $STATUS_STOPPED
            GUICtrlSetBkColor($idStartStop, $COLOR_RED)
            GUICtrlSetData($idStartStop, "Start")
        Case $STATUS_STARTING
            GUICtrlSetBkColor($idStartStop, $COLOR_ORANGE)
            GUICtrlSetData($idStartStop, "Starting...")
        Case $STATUS_RUNNING
            GUICtrlSetBkColor($idStartStop, $COLOR_GREEN)
            GUICtrlSetData($idStartStop, "Stop")
        Case $STATUS_STOPPING
            GUICtrlSetBkColor($idStartStop, $COLOR_MAGENTA)
            GUICtrlSetData($idStartStop, "Stopping...")
    EndSwitch
EndFunc

; --------------------------------------------------------------------------------
; Sunucu UP mı kontrolü için /actuator/health isteği atıyoruz.
; Dönen metin içinde  "status":"UP"  varsa, True döner.
Func IsServerUpViaActuator($iPort)
    Local $sUrl = "http://localhost:" & $iPort & "/actuator/health"

    ; 1) Binary okuma
    Local $bBinary = InetRead($sUrl, 0) ; 0 => Binary mode
    If @error Then
        $g_sConsoleOutput &= "[DEBUG] InetRead failed. @error=" & @error & @CRLF
        GUICtrlSetData($idConsoleEdit, $g_sConsoleOutput)
        Return False
    EndIf

    ; 2) Binary'den string'e dönüştür (UTF-8 varsayımı)
    Local $sRet = BinaryToString($bBinary, 4) ; 4 => $SB_UTF8
    ; Alternatif: 1 => $SB_ANSI, 2 => $SB_UNICODE, 3 => $SB_UTF16LE
    ; Spring Boot, genelde JSON'u UTF-8 olarak döndürür.

    $g_sConsoleOutput &= "[DEBUG] Actuator response (decoded): " & $sRet & @CRLF
    GUICtrlSetData($idConsoleEdit, $g_sConsoleOutput)

    ; 3) "status":"UP" arıyor muyuz?
    If StringInStr($sRet, '"status":"UP"') > 0 Then
        Return True
    EndIf

    Return False
EndFunc



; --------------------------------------------------------------------------------
; Spring Boot'u başlat (stdout/stderr yakalayacak şekilde).
; Durumu STARTING yapar, eğer zamanında UP alırsa RUNNING'e geçer ve tarayıcı açar.
Func StartSpringBoot()
    If $g_iPID <> 0 Then
        MsgBox($MB_ICONINFORMATION, "Uyarı", "Uygulama zaten çalışıyor!")
        Return
    EndIf

    ; Durum: STARTING => turuncu
    $g_iAppStatus = $STATUS_STARTING
    UpdateStartStopButtonColor()

    ; Komutu başlat: 2=STDOUT,4=STDERR,32=NoConsole => 2+4+32=38
    $g_iPID = Run("java -jar daily-topic-tracker.jar", "", @SW_HIDE, 38)
    If $g_iPID = 0 Then
        $g_sConsoleOutput &= "[Hata: Uygulama başlatılamadı!]" & @CRLF
        GUICtrlSetData($idConsoleEdit, $g_sConsoleOutput)
        $g_iAppStatus = $STATUS_STOPPED
        UpdateStartStopButtonColor()
        Return
    EndIf

    $g_sConsoleOutput &= "[Uygulama başlatılıyor - PID: " & $g_iPID & " ]" & @CRLF
    GUICtrlSetData($idConsoleEdit, $g_sConsoleOutput)
EndFunc

; --------------------------------------------------------------------------------
; Spring Boot'u durdur. Durumu STOPPING -> STOPPED.
Func StopSpringBoot()
    If $g_iPID <> 0 Then
        $g_iAppStatus = $STATUS_STOPPING
        UpdateStartStopButtonColor()

        ProcessClose($g_iPID)
        $g_iPID = 0
        $g_sConsoleOutput &= "[Uygulama durduruldu]" & @CRLF
        GUICtrlSetData($idConsoleEdit, $g_sConsoleOutput)

        $g_iAppStatus = $STATUS_STOPPED
        UpdateStartStopButtonColor()
    EndIf
EndFunc

; --------------------------------------------------------------------------------
; Ana Pencere X veya Exit ile kapatılırken
Func OnClose()
    StopSpringBoot()
    Exit
EndFunc

; --------------------------------------------------------------------------------
; Start/Stop butonuna tıklanınca
Func ToggleApp()
    If $g_iAppStatus = $STATUS_STOPPED Then
        StartSpringBoot()
    ElseIf $g_iAppStatus = $STATUS_RUNNING Then
        StopSpringBoot()
    Else
        ; STARTING veya STOPPING durumundaysa tıklandıysa ne yapsın?
        ; İsterseniz iptal edin ya da bir uyarı verin.
        MsgBox($MB_ICONINFORMATION, "Bilgi", "İşlem sürüyor, lütfen bekleyin.")
    EndIf
EndFunc

; --------------------------------------------------------------------------------
; Log Penceresi aç/kapa
Func ShowHideLogs()
    If Not $g_bLogWindowVisible Then
        GUISetState(@SW_SHOW, $hLogGUI)
        $g_bLogWindowVisible = True
    Else
        GUISetState(@SW_HIDE, $hLogGUI)
        $g_bLogWindowVisible = False
    EndIf
EndFunc

Func OnCloseLogGUI()
    GUISetState(@SW_HIDE, $hLogGUI)
    $g_bLogWindowVisible = False
EndFunc

; --------------------------------------------------------------------------------
; Tray menüsünde ana GUI'yi göster/gizle
Func TrayShowHideMain()
    If BitAND(GUIGetState($hMainGUI), $GUI_SHOW) Then
        GUISetState(@SW_HIDE, $hMainGUI)
    Else
        GUISetState(@SW_SHOW, $hMainGUI)
    EndIf
EndFunc

; --------------------------------------------------------------------------------
; Giriş Noktası
Opt("GUIOnEventMode", 1)

; 1) Port numarasını oku
$g_sPort = ReadPortNumber("C:\path\to\application.properties")

; 2) Ana Pencere
$hMainGUI = GUICreate("Daily Topic Tracker Control", 360, 130)
GUISetOnEvent($GUI_EVENT_CLOSE, "OnClose")

$idStartStop = GUICtrlCreateButton("Start", 30, 30, 100, 30)
GUICtrlSetOnEvent($idStartStop, "ToggleApp")

$idShowLogs = GUICtrlCreateButton("Hide Logs", 140, 30, 100, 30) ; ilk açılışta logs açık => buton "Hide Logs" yazabilir
GUICtrlSetOnEvent($idShowLogs, "ShowHideLogs")

$idExit = GUICtrlCreateButton("Exit", 250, 30, 70, 30)
GUICtrlSetOnEvent($idExit, "OnClose")

; Başlangıçta Durum STOPPED => Kırmızı
UpdateStartStopButtonColor()
GUISetState(@SW_SHOW, $hMainGUI)

; 3) Log Penceresi (ilk başta visible, çünkü $g_bLogWindowVisible=True)
$hLogGUI = GUICreate("Console Output", 1000, 400, -1, 600, BitOR($WS_CAPTION, $WS_SYSMENU))
GUISetOnEvent($GUI_EVENT_CLOSE, "OnCloseLogGUI", $hLogGUI)

$idConsoleEdit = GUICtrlCreateEdit("", 10, 10, 980, 350, _
   BitOR($ES_WANTRETURN, $ES_AUTOVSCROLL, $WS_VSCROLL, $ES_READONLY))
GUICtrlSetFont($idConsoleEdit, 9, 400, 0, "Courier New")
GUICtrlSetData($idConsoleEdit, "[Uygulama log'ları burada görünecek]" & @CRLF)

If $g_bLogWindowVisible Then
    GUISetState(@SW_SHOW, $hLogGUI)
Else
    GUISetState(@SW_HIDE, $hLogGUI)
EndIf

; 4) Tray Menüsü (isteğe bağlı)
TrayCreateMenu("Daily Tracker Menu")
TraySetToolTip("Daily Topic Tracker")
TraySetState()

Global $idTrayShowHide = TrayCreateItem("Show/Hide Main GUI")
TrayItemSetOnEvent($idTrayShowHide, "TrayShowHideMain")

TrayCreateItem("") ; ayraç
Global $idTrayExit = TrayCreateItem("Exit")
TrayItemSetOnEvent($idTrayExit, "OnClose")

; --------------------------------------------------------------------------------
; Ana Döngü: Hem logları okuyoruz hem de actuator/health'i kontrol edip durum değiştirebiliriz.
While True
    ; (A) Log çekme
    If $g_iPID <> 0 Then
        ; Proses var mı hâlâ?
        If ProcessExists($g_iPID) = 0 Then
            ; Proses kendiliğinden kapandı
            $g_sConsoleOutput &= "[Uygulama kendiliğinden kapandı]" & @CRLF
            GUICtrlSetData($idConsoleEdit, $g_sConsoleOutput)

            $g_iPID = 0
            $g_iAppStatus = $STATUS_STOPPED
            UpdateStartStopButtonColor()
        Else
            ; Yeni veri (stdout+stderr) var mı?
            Local $sOut = StdoutRead($g_iPID)
            If @error = 0 And $sOut <> "" Then
                $g_sConsoleOutput &= $sOut
                GUICtrlSetData($idConsoleEdit, $g_sConsoleOutput)
            EndIf
        EndIf
    EndIf

    ; (B) eğer STARTING durumundaysak actuator/health'e bakıp RUNNING'e geçebiliriz
    If $g_iAppStatus = $STATUS_STARTING And $g_iPID <> 0 Then
        ; check /actuator/health
        If IsServerUpViaActuator($g_sPort) Then
            ; uygulama artık UP
            $g_iAppStatus = $STATUS_RUNNING
            UpdateStartStopButtonColor()
            ; Tarayıcı açalım
            $g_sConsoleOutput &= "[Sunucu UP. Tarayıcı açılıyor...]" & @CRLF
            GUICtrlSetData($idConsoleEdit, $g_sConsoleOutput)
            ShellExecute("http://localhost:" & $g_sPort)
        EndIf
    EndIf

    Sleep(200)
WEnd
