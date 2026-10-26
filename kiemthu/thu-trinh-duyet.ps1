param(
    [string]$Chrome = "C:\Program Files\Google\Chrome\Application\chrome.exe",
    [string]$Jar = "target\littleproxy-1.1.3-SNAPSHOT-littleproxy-shade.jar",
    [int]$Port = 18700
)
$work = Join-Path $env:TEMP "proxyapp-thu-trinh-duyet"
if (Test-Path $work) { Remove-Item $work -Recurse -Force }
New-Item -ItemType Directory $work | Out-Null
Set-Content "$work\proxy.properties" "port=$Port`nmitmEnabled=true`nlogToFile=true" -Encoding ASCII
Set-Content "$work\url-rules.txt" "iana.org path /help`n* keyword bimat" -Encoding ASCII
$env:Path = "C:\OpenSSL-Win64\bin;" + $env:Path
$jarFull = (Resolve-Path $Jar).Path
$proxy = Start-Process -FilePath "java" -ArgumentList "-cp", "`"$jarFull`"", "proxyapp.ConsoleMain", "--home", "`"$work`"" `
    -PassThru -WindowStyle Hidden -RedirectStandardOutput "$work\console.txt" -RedirectStandardError "$work\loi.txt"
try {
    Start-Sleep -Seconds 6
    & openssl x509 -in "$work\proxy-ca.crt" -pubkey -noout -out "$work\pub.pem" 2>&1 | Out-Null
    & openssl pkey -pubin -in "$work\pub.pem" -outform der -out "$work\pub.der" 2>&1 | Out-Null
    & openssl dgst -sha256 -binary -out "$work\h.bin" "$work\pub.der" 2>&1 | Out-Null
    $spki = (& openssl base64 -in "$work\h.bin").Trim()
    $sites = @(
        @{ ten = "01-http-bi-chan"; url = "http://doubleclick.net/" },
        @{ ten = "02-https-giai-ma"; url = "https://example.com/" },
        @{ ten = "03-https-quy-tac-duong-dan"; url = "https://www.iana.org/help/example-domains" },
        @{ ten = "04-https-trang-thuong"; url = "https://www.iana.org/domains/reserved" },
        @{ ten = "05-https-tu-khoa"; url = "https://en.wikipedia.org/wiki/Bimat" }
    )
    foreach ($s in $sites) {
        $chromeArgs = @("--headless=new", "--disable-gpu", "--no-first-run", "--user-data-dir=$work\hoso-$($s.ten)",
            "--proxy-server=http://127.0.0.1:$Port", "--ignore-certificate-errors-spki-list=$spki",
            "--screenshot=$work\$($s.ten).png", "--window-size=1000,640", "--virtual-time-budget=8000", $s.url)
        $run = Start-Process -FilePath $Chrome -ArgumentList $chromeArgs -PassThru -Wait:$false -RedirectStandardError "$work\chrome-loi.txt" -RedirectStandardOutput "$work\chrome-ra.txt"
        if (-not $run.WaitForExit(40000)) { Stop-Process -Id $run.Id -Force }
        Write-Output ("{0}: {1}" -f $s.ten, $(if (Test-Path "$work\$($s.ten).png") { "có ảnh" } else { "KHÔNG có ảnh" }))
    }
    Write-Output "--- Nhật ký proxy ---"
    Get-Content "$work\proxy-log.csv" -Encoding UTF8 | Select-String "GET" | ForEach-Object { $_.Line }
    Write-Output "Ảnh chụp và nhật ký nằm trong: $work"
} finally {
    & taskkill /PID $proxy.Id /T /F 2>&1 | Out-Null
}
