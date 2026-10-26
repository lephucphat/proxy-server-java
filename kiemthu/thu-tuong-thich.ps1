param(
    [string]$Chrome = "C:\Program Files\Google\Chrome\Application\chrome.exe",
    [string]$Jar = "target\littleproxy-1.1.3-SNAPSHOT-littleproxy-shade.jar",
    [int]$Port = 18710
)
$work = Join-Path $env:TEMP "proxyapp-thu-tuong-thich"
$results = @()
$jarFull = (Resolve-Path $Jar).Path
$env:Path = "C:\OpenSSL-Win64\bin;" + $env:Path

function Start-Proxy($mode, $mitm) {
    $dir = Join-Path $work $mode
    if (Test-Path $dir) { Remove-Item $dir -Recurse -Force }
    New-Item -ItemType Directory $dir | Out-Null
    Set-Content "$dir\proxy.properties" "port=$Port`nmitmEnabled=$mitm`nlogToFile=true`nblockInternal=false" -Encoding ASCII
    $p = Start-Process -FilePath "java" -ArgumentList "-cp", "`"$jarFull`"", "proxyapp.ConsoleMain", "--home", "`"$dir`"" `
        -PassThru -WindowStyle Hidden -RedirectStandardOutput "$dir\console.txt" -RedirectStandardError "$dir\loi.txt"
    Start-Sleep -Seconds 6
    return @{ proc = $p; dir = $dir }
}

function Get-Spki($dir) {
    if (-not (Test-Path "$dir\proxy-ca.crt")) { return "" }
    & openssl x509 -in "$dir\proxy-ca.crt" -pubkey -noout -out "$dir\pub.pem" 2>&1 | Out-Null
    & openssl pkey -pubin -in "$dir\pub.pem" -outform der -out "$dir\pub.der" 2>&1 | Out-Null
    & openssl dgst -sha256 -binary -out "$dir\h.bin" "$dir\pub.der" 2>&1 | Out-Null
    return (& openssl base64 -in "$dir\h.bin").Trim()
}

function Test-Page($dir, $spki, $name, $url, $expect, $budget = 15000) {
    $out = "$dir\$name.html"
    $args2 = @("--headless=new", "--disable-gpu", "--no-first-run", "--user-data-dir=$dir\hoso-$name",
        "--proxy-server=http://127.0.0.1:$Port", "--virtual-time-budget=$budget", "--dump-dom", $url)
    if ($spki) { $args2 += "--ignore-certificate-errors-spki-list=$spki" }
    $run = Start-Process -FilePath $Chrome -ArgumentList $args2 -PassThru -RedirectStandardOutput $out -RedirectStandardError "$dir\$name-loi.txt"
    if (-not $run.WaitForExit(60000)) { Stop-Process -Id $run.Id -Force }
    $text = ""
    if (Test-Path $out) { $text = Get-Content $out -Raw -Encoding UTF8 }
    $ok = $text -and $text.Length -gt 300 -and ($text -match $expect)
    return [pscustomobject]@{ mode = (Split-Path $dir -Leaf); thu = $name; ketqua = $(if ($ok) { "DAT" } else { "LOI" }); kichthuoc = $(if ($text) { $text.Length } else { 0 }) }
}

function Test-Download($dir, $name, $url, $minBytes) {
    $caArgs = @()
    if (Test-Path "$dir\proxy-ca.crt") { $caArgs = @("--cacert", "$dir\proxy-ca.crt") }
    $r = & curl.exe -x "http://127.0.0.1:$Port" @caArgs --ssl-no-revoke -s -L -o NUL -w "%{http_code} %{size_download}" --max-time 40 $url
    $parts = "$r".Split(" ")
    $bytes = 0
    if ($parts.Length -ge 2) { [long]::TryParse($parts[1], [ref]$bytes) | Out-Null }
    $ok = $parts[0] -eq "200" -and $bytes -ge $minBytes
    return [pscustomobject]@{ mode = (Split-Path $dir -Leaf); thu = $name; ketqua = $(if ($ok) { "DAT" } else { "LOI" }); kichthuoc = $bytes }
}

New-Item -ItemType Directory $work -Force | Out-Null

$sites = @(
    @{ ten = "wikipedia"; url = "https://en.wikipedia.org/wiki/Proxy_server"; mong = "Proxy server" },
    @{ ten = "youtube"; url = "https://www.youtube.com/"; mong = "YouTube" },
    @{ ten = "facebook"; url = "https://www.facebook.com/"; mong = "Facebook" },
    @{ ten = "zalo"; url = "https://chat.zalo.me/"; mong = "Zalo" },
    @{ ten = "meet"; url = "https://meet.google.com/"; mong = "Meet" },
    @{ ten = "google"; url = "https://www.google.com/"; mong = "Google" }
)

foreach ($cfg in @(@{ mode = "khong-giai-ma"; mitm = "false" }, @{ mode = "giai-ma-https"; mitm = "true" })) {
    $px = Start-Proxy $cfg.mode $cfg.mitm
    try {
        $spki = ""
        if ($cfg.mitm -eq "true") { $spki = Get-Spki $px.dir }
        foreach ($s in $sites) {
            $results += Test-Page $px.dir $spki $s.ten $s.url $s.mong
        }
        $results += Test-Download $px.dir "tai-file-lon" "https://proof.ovh.net/files/10Mb.dat" 9000000
        $results += Test-Download $px.dir "http-thuong" "http://example.com/" 500
    } finally {
        & taskkill /PID $px.proc.Id /T /F 2>&1 | Out-Null
        Start-Sleep -Seconds 3
    }
}
$results | Format-Table -AutoSize | Out-String -Width 160 | Write-Output
Write-Output "Chi tiet o: $work"
