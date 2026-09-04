$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$ReleaseDir = Join-Path $ProjectRoot "release"

Push-Location (Join-Path $ProjectRoot "frontend")
npm ci
npm run build
Pop-Location

Push-Location (Join-Path $ProjectRoot "backend")
mvn clean package
Pop-Location

New-Item -ItemType Directory -Force (Join-Path $ReleaseDir "frontend") | Out-Null
Copy-Item -Recurse -Force (Join-Path $ProjectRoot "frontend\.output\*") (Join-Path $ReleaseDir "frontend")
Copy-Item -Force (Join-Path $ProjectRoot "backend\target\personal-site-backend-0.1.0.jar") (Join-Path $ReleaseDir "app.jar")
Copy-Item -Force (Join-Path $ProjectRoot "deploy\nginx.conf") $ReleaseDir
Copy-Item -Force (Join-Path $ProjectRoot "deploy\personal-site-api.service") $ReleaseDir
Copy-Item -Force (Join-Path $ProjectRoot "deploy\personal-site-web.service") $ReleaseDir
Copy-Item -Force (Join-Path $ProjectRoot "deploy\install-release.sh") $ReleaseDir

Write-Host "Release prepared at $ReleaseDir"

