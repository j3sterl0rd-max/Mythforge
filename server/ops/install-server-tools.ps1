$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$serverRoot = Split-Path $PSScriptRoot -Parent
$manifest = [Collections.Generic.List[object]]::new()
$installed = [Collections.Generic.HashSet[string]]::new()
function Install-Modrinth([string]$Project) {
    if (!$installed.Add($Project)) { return }
    $versions = Invoke-RestMethod ('https://api.modrinth.com/v2/project/' + $Project + '/version?loaders=%5B%22neoforge%22%5D&game_versions=%5B%221.21.1%22%5D')
    $version = $versions | Where-Object version_type -eq 'release' | Select-Object -First 1
    if (!$version) { throw "No compatible stable release: $Project" }
    foreach ($dependency in $version.dependencies) {
        if ($dependency.dependency_type -eq 'required') { Install-Modrinth $dependency.project_id }
    }
    $file = $version.files | Where-Object primary | Select-Object -First 1
    if (!$file) { $file = $version.files | Where-Object filename -NotMatch 'sources' | Select-Object -First 1 }
    $target = Join-Path $serverRoot ('mods/' + $file.filename)
    Invoke-WebRequest $file.url -OutFile $target
    $actual = (Get-FileHash -LiteralPath $target -Algorithm SHA512).Hash.ToLowerInvariant()
    if ($actual -ne $file.hashes.sha512) { Remove-Item -LiteralPath $target; throw "Hash mismatch: $Project" }
    $jar = [IO.Compression.ZipFile]::OpenRead($target); $jar.Dispose()
    $manifest.Add(@{project=$Project;version=$version.version_number;filename=$file.filename;url=$file.url;sha512=$actual})
    Write-Output ('Installed ' + $file.filename)
}
foreach ($project in @('neruina','grieflogger','glra','advanced-backups','modded-votifier')) { Install-Modrinth $project }
$ftbFile = 'ftb-ranks-neoforge-2101.1.5.jar'
$ftbUrl = 'https://maven.ftb.dev/releases/dev/ftb/mods/ftb-ranks-neoforge/2101.1.5/' + $ftbFile
$ftbTarget = Join-Path $serverRoot ('mods/' + $ftbFile)
Invoke-WebRequest $ftbUrl -OutFile $ftbTarget
$jar = [IO.Compression.ZipFile]::OpenRead($ftbTarget); $jar.Dispose()
$manifest.Add(@{project='FTB Ranks';version='2101.1.5';filename=$ftbFile;url=$ftbUrl;sha512=(Get-FileHash $ftbTarget -Algorithm SHA512).Hash.ToLowerInvariant()})
$manifest | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $PSScriptRoot 'evidence/installed-mods.json')
