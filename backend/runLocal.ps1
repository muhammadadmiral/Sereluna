$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$projectRoot = Split-Path -Parent $scriptDir
$serviceAccountPath = Join-Path $scriptDir "firebase-service-account.json"

if (-not (Test-Path $serviceAccountPath)) {
    throw "Place your Firebase service account JSON at $serviceAccountPath"
}

$env:GEMINI_API_KEY = "AIzaSyCw7pHCl__CDR9ZHSo-nUOWno0wKpZg1lM"
$env:GOOGLE_APPLICATION_CREDENTIALS = $serviceAccountPath

Push-Location $projectRoot
try {
    ./gradlew :backend:run
}
finally {
    Pop-Location
}
