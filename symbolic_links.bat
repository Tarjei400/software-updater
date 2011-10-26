; only for Windows Vista or up (need mklink)

set workingDirectory=%CD%

;;;;;;;;;;;;;;;;;;;;;;;
;  Software Launcher  ;
;;;;;;;;;;;;;;;;;;;;;;;

set softwareDirectory=SoftwareLauncher

mkdir ..\SoftwareLauncher\src\org\apache\commons\
mklink /J ..\SoftwareLauncher\src\org\apache\commons\codec\ %workingDirectory%\src\org\apache\commons\codec\

mklink /J ..\SoftwareLauncher\src\watne\ %workingDirectory%\src\watne\

mkdir ..\SoftwareLauncher\src\updater\gui\
set file=src\updater\gui\JTitledPanel.java
call:linkFile
set file=src\updater\gui\UpdaterWindow.java
call:linkFile

mkdir ..\SoftwareLauncher\src\updater\script\
set file=src\updater\script\Client.java
call:linkFile
set file=src\updater\script\InvalidFormatException.java
call:linkFile
set file=src\updater\script\Patch.java
call:linkFile

mkdir ..\SoftwareLauncher\src\updater\patch\
set file=src\updater\patch\PatchReadUtil.java
call:linkFile
set file=src\updater\patch\PatchLogReader.java
call:linkFile
set file=src\updater\patch\PatchLogWriter.java
call:linkFile
set file=src\updater\patch\Patcher.java
call:linkFile
set file=src\updater\patch\PatcherListener.java
call:linkFile

mkdir ..\SoftwareLauncher\src\updater\util\
set file=src\updater\util\CommonUtil.java
call:linkFile
set file=src\updater\util\InterruptibleInputStream.java
call:linkFile
set file=src\updater\util\InterruptibleOutputStream.java
call:linkFile
set file=src\updater\util\XMLUtil.java
call:linkFile
set file=src\updater\util\SeekableFile.java
call:linkFile

mkdir ..\SoftwareLauncher\test\updater\
set file=test\updater\TestCommon.java
call:linkFile


;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;  Software Patch Builder  ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;

set softwareDirectory=SoftwarePatchBuilder

mkdir ..\SoftwarePatchBuilder\src\org\apache\commons\
mklink /J ..\SoftwarePatchBuilder\src\org\apache\commons\codec\ %workingDirectory%\src\org\apache\commons\codec\

mklink /J ..\SoftwarePatchBuilder\src\watne\ %workingDirectory%\src\watne\

mkdir ..\SoftwarePatchBuilder\src\updater\script\
set file=src\updater\script\Catalog.java
call:linkFile
set file=src\updater\script\Client.java
call:linkFile
set file=src\updater\script\InvalidFormatException.java
call:linkFile
set file=src\updater\script\Patch.java
call:linkFile

mkdir ..\SoftwarePatchBuilder\src\updater\patch\
set file=src\updater\patch\PatchReadUtil.java
call:linkFile
set file=src\updater\patch\PatchWriteUtil.java
call:linkFile
set file=src\updater\patch\PatchLogReader.java
call:linkFile
set file=src\updater\patch\PatchLogWriter.java
call:linkFile
set file=src\updater\patch\Patcher.java
call:linkFile
set file=src\updater\patch\PatcherListener.java
call:linkFile

mkdir ..\SoftwarePatchBuilder\src\updater\util\
set file=src\updater\util\CommonUtil.java
call:linkFile
set file=src\updater\util\InterruptibleInputStream.java
call:linkFile
set file=src\updater\util\InterruptibleOutputStream.java
call:linkFile
set file=src\updater\util\XMLUtil.java
call:linkFile
set file=src\updater\util\SeekableFile.java
call:linkFile

mkdir ..\SoftwarePatchBuilder\test\updater\
set file=test\updater\TestCommon.java
call:linkFile


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;  Software Patch Downloader  ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

set softwareDirectory=SoftwarePatchDownloader

mkdir ..\SoftwarePatchDownloader\src\updater\gui\
set file=src\updater\gui\JTitledPanel.java
call:linkFile
set file=src\updater\gui\UpdaterWindow.java
call:linkFile

mkdir ..\SoftwarePatchDownloader\src\updater\script\
set file=src\updater\script\Catalog.java
call:linkFile
set file=src\updater\script\Client.java
call:linkFile
set file=src\updater\script\InvalidFormatException.java
call:linkFile
set file=src\updater\script\Patch.java
call:linkFile

mkdir ..\SoftwarePatchDownloader\src\updater\util\
set file=src\updater\util\CommonUtil.java
call:linkFile
set file=src\updater\util\XMLUtil.java
call:linkFile

mkdir ..\SoftwarePatchDownloader\test\updater\
set file=test\updater\TestCommon.java
call:linkFile



:linkFile
del %softwareDirectory%\%file%
mklink %softwareDirectory%\%file% %workingDirectory%\SoftwareUpdaterCommon\%file%
goto:eof