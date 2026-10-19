param([switch]$Test)
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
# Tìm JDK 25: ưu tiên JAVA_HOME, sau đó C:\Program Files\Java\jdk-25*, cuối cùng PATH.
$jdkBin = $null
if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\jar.exe'))) { $jdkBin = Join-Path $env:JAVA_HOME 'bin' }
if (-not $jdkBin) {
    $found = Get-ChildItem 'C:\Program Files\Java' -Directory -Filter 'jdk-25*' -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending | Where-Object { Test-Path (Join-Path $_.FullName 'bin\jar.exe') } | Select-Object -First 1
    if ($found) { $jdkBin = Join-Path $found.FullName 'bin' }
}
if ($jdkBin) { $javac = Join-Path $jdkBin 'javac.exe'; $jar = Join-Path $jdkBin 'jar.exe'; $java = Join-Path $jdkBin 'java.exe' }
else { $javac = 'javac'; $jar = 'jar'; $java = 'java' }
Write-Output "JDK: $(if ($jdkBin) { $jdkBin } else { 'PATH' })"
try {
    New-Item -ItemType Directory -Force -Path build/classes, build/test-classes, build/tmp | Out-Null
    $mainSources = @(Get-ChildItem -Path src/main/java -Recurse -Filter *.java | ForEach-Object { $_.FullName })
    & $javac --release 25 -encoding UTF-8 -Xlint:all -Werror -d build/classes $mainSources
    if ($LASTEXITCODE -ne 0) { throw 'Biên dịch bản xem thử thất bại' }
    $previewTemp = Join-Path $PSScriptRoot 'build/tmp'
    & $jar "-J-Djava.io.tmpdir=$previewTemp" --create --file build/admin-desktop-preview.jar --main-class com.manage.nhakhoa.desktop.AdminPreview -C build/classes .
    if ($LASTEXITCODE -ne 0) { throw 'Đóng gói bản xem thử thất bại' }
    Write-Output 'ĐẠT: biên dịch Java 25 với -Xlint:all -Werror và đóng gói JAR độc lập'
    if ($Test) {
        $testSources = @(Get-ChildItem -Path src/test/java -Recurse -Filter *.java | ForEach-Object { $_.FullName })
        & $javac --release 25 -encoding UTF-8 -Xlint:all -Werror -cp build/classes -d build/test-classes $testSources
        if ($LASTEXITCODE -ne 0) { throw 'Biên dịch kiểm thử thất bại' }
        & $java '-Djava.awt.headless=true' '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' -cp 'build/classes;build/test-classes' com.manage.nhakhoa.desktop.PreviewTests
        if ($LASTEXITCODE -ne 0) { throw 'Kiểm thử bản xem thử thất bại' }
    }
} finally { Pop-Location }
