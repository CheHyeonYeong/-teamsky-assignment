[CmdletBinding()]
param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$GradleArgs
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-SanitizedPath {
    param(
        [string]$RawPath
    )

    $entries = $RawPath -split ';'
    $sanitized = New-Object System.Collections.Generic.List[string]

    foreach ($entry in $entries) {
        if ([string]::IsNullOrWhiteSpace($entry)) {
            continue
        }

        try {
            $fullPath = [System.IO.Path]::GetFullPath($entry)
            if ([System.IO.Path]::IsPathRooted($fullPath)) {
                [void]$sanitized.Add($entry)
            }
        } catch {
            continue
        }
    }

    return ($sanitized | Select-Object -Unique) -join ';'
}

function Test-Java21Home {
    param(
        [string]$JavaHome
    )

    if ([string]::IsNullOrWhiteSpace($JavaHome)) {
        return $false
    }

    $javaExe = Join-Path $JavaHome "bin\java.exe"
    if (-not (Test-Path $javaExe)) {
        return $false
    }

    try {
        $versionOutput = & $javaExe --version | Out-String
    } catch {
        return $false
    }

    return $versionOutput -match "(?m)^openjdk 21(\.|$|\s)"
}

function Get-Java21Home {
    $candidates = New-Object System.Collections.Generic.List[string]

    foreach ($envName in @("JAVA21_HOME", "JDK21_HOME", "JAVA_HOME")) {
        $value = [Environment]::GetEnvironmentVariable($envName)
        if (-not [string]::IsNullOrWhiteSpace($value)) {
            [void]$candidates.Add($value)
        }
    }

    $searchRoots = @(
        (Join-Path $env:USERPROFILE ".jdks"),
        "C:\Program Files\Microsoft",
        "C:\Program Files\Eclipse Adoptium",
        "C:\Program Files\Java",
        "C:\Program Files\Amazon Corretto"
    )

    foreach ($root in $searchRoots) {
        if (-not (Test-Path $root)) {
            continue
        }

        Get-ChildItem -Path $root -Directory -ErrorAction SilentlyContinue |
                Where-Object { $_.Name -match "(?i)21" } |
                Sort-Object FullName -Descending |
                ForEach-Object { [void]$candidates.Add($_.FullName) }
    }

    foreach ($candidate in ($candidates | Select-Object -Unique)) {
        if (Test-Java21Home -JavaHome $candidate) {
            return $candidate
        }
    }

    return $null
}

$javaHome = Get-Java21Home
if (-not $javaHome) {
    throw "Java 21 installation not found. Set JAVA21_HOME or install JDK 21 before running Gradle."
}

$env:JAVA_HOME = $javaHome
$sanitizedPath = Get-SanitizedPath -RawPath $env:PATH
$env:PATH = "$($env:JAVA_HOME)\bin;$sanitizedPath"

$gradlew = Join-Path $PSScriptRoot "..\gradlew.bat"
if (-not (Test-Path $gradlew)) {
    throw "gradlew.bat not found at $gradlew"
}

Write-Host "Using JAVA_HOME=$javaHome"
& $gradlew @GradleArgs
exit $LASTEXITCODE
