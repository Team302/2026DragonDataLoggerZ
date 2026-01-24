# Simple integration test for sender/receiver
cd c:/Users/hardw/GitHub/2026DragonDataLoggerZ

# Start receiver in background
$receiver = Start-Process -FilePath "java" -ArgumentList @(
    "-cp", "build/libs/pi-logger.jar",
    "pi.logger.ReceiverMain",
    "logs/integration_test.csv",
    "6666"
) -NoNewWindow -PassThru -RedirectStandardOutput "logs/receiver_output.txt" -RedirectStandardError "logs/receiver_error.txt"

# Wait a moment for receiver to start
Start-Sleep -Milliseconds 1000

# Send packets
Write-Host "Sending 5 test packets..."
& java -cp build/libs/pi-logger.jar pi.logger.SenderMain 127.0.0.1 6666 5 200 false

# Wait for processing
Start-Sleep -Milliseconds 2000

# Stop receiver
Write-Host "Stopping receiver..."
$receiver | Stop-Process -Force

# Check results
Write-Host "`nReceiver output:"
Get-Content logs/receiver_output.txt

Write-Host "`nReceiver errors:"
Get-Content logs/receiver_error.txt

Write-Host "`nCSV file contents:"
Get-Content logs/integration_test.csv
