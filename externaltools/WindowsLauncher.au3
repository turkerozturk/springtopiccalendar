#NoTrayIcon ; Başlangıçta kendi tray ikonu gizli. Ama isterseniz tray ikonu da ekleyebilir, menü yapabilirsiniz.

#include <GUIConstantsEx.au3>
#include <MsgBoxConstants.au3>
#include <WindowsConstants.au3>

Global $g_iPID = 0 ; Java prosesinin PID'si
Global $g_sPort = 8080 ; server.port değeri

; ---- Fonksiyon: application.properties içinden port oku ----
Func ReadPortNumber($sPropFilePath)
    Local $hFile = FileOpen($sPropFilePath, 0)
    If $hFile = -1 Then
        ; Dosya yok veya açılamadı -> varsayılan 8080
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

; ---- Fonksiyon: Spring Boot başlat ----
Func StartSpringBoot()
    If $g_iPID <> 0 Then
        MsgBox($MB_ICONINFORMATION, "Uyarı", "Uygulama zaten çalışıyor!")
        Return
    EndIf

    $g_iPID = Run("java -jar daily-topic-tracker-1.0.0.jar", "", @SW_HIDE)
    If $g_iPID = 0 Then
        MsgBox($MB_ICONERROR, "Hata", "Uygulama başlatılamadı!")
        Return
    EndIf

    ; Portu kullanarak tarayıcı aç
    ShellExecute("http://localhost:" & $g_sPort)
EndFunc

; ---- Fonksiyon: Spring Boot durdur ----
Func StopSpringBoot()
    If $g_iPID <> 0 Then
        ProcessClose($g_iPID)
        $g_iPID = 0
    EndIf
EndFunc

; ---- Program tamamen kapanırken yapılacaklar ----
Func OnClose()
    ; Spring Boot'u kapat
    StopSpringBoot()
    Exit
EndFunc

; ---- Giriş Noktası ----

; 1) Portu oku (application.properties hangi dizindeyse orayı belirtmelisiniz)
$g_sPort = ReadPortNumber("C:\path\to\application.properties")

; 2) GUI oluştur
Opt("GUIOnEventMode", 1) ; Butonlar için event-based mod
Local $hGUI = GUICreate("Daily Topic Tracker Control", 300, 120)
GUISetOnEvent($GUI_EVENT_CLOSE, "OnClose")

; Start/Stop Buton
Local $idStartStop = GUICtrlCreateButton("Start/Stop", 30, 30, 100, 30)
GUICtrlSetOnEvent($idStartStop, "ToggleApp")

; Exit Buton
Local $idExit = GUICtrlCreateButton("Exit", 160, 30, 100, 30)
GUICtrlSetOnEvent($idExit, "OnClose")

GUISetState(@SW_SHOW, $hGUI)

; Tray simgesi isterseniz:
; Tray menü (ana menü) oluşturuyoruz:
Global $hMainMenu = TrayCreateMenu("Daily Tracker Menu")  ; Parametre ekledik
TraySetToolTip("Daily Topic Tracker")
TraySetState()  ; tepsi (tray) simgesini görünür hale getirir

; Menü item'ı oluşturalım:
Global $idShowHide = TrayCreateItem("Show/Hide", $hMainMenu)
TrayItemSetOnEvent($idShowHide, "ShowHideFunction")

; Boş satır ekleyelim (ayraç gibi)
TrayCreateItem("", $hMainMenu)

Global $idExitTray = TrayCreateItem("Exit", $hMainMenu)
TrayItemSetOnEvent($idExitTray, "OnClose")


; Tray simgesine tıklandığında pencereyi göstermek veya gizlemek isterseniz ek event'lar ekleyebilirsiniz.
; Örneğin:
; TrayItemSetOnEvent(TrayCreateItem("Show/Hide"), "ShowHideFunction")

; Sonsuz döngü
While 1
    Sleep(100)
WEnd

Func ToggleApp()
    If $g_iPID = 0 Then
        StartSpringBoot()
    Else
        StopSpringBoot()
    EndIf
EndFunc
