## Installation instructions
copy pilogger.service from the pi-logger directory into /etc/systemd/system/pilogger.service

```bash
sudo systemctl daemon-reload
sudo systemctl enable pilogger.service
sudo systemctl start pilogger.service
```

## Systemctl reference

Start the service
`sudo systemctl start pilogger.service`

Verify the service is running
`sudo systemctl status pilogger.service`


## Troubleshooting
``` bash
     pilogger.service - PiLogger
     Loaded: loaded (/etc/systemd/system/pilogger.service; enabled; preset: enabled)
     Active: failed (Result: start-limit-hit) since Sat 2026-01-31 10:29:18 EST; 38s ago
   Duration: 73ms
 Invocation: 51790f631bc04ab3b0efa412799dbfb0
    Process: 1454 ExecStart=/home/frc302/PiLogger-linuxarm64-cross.jar.sh (code=exited, status=0/SUCCESS)
   Main PID: 1454 (code=exited, status=0/SUCCESS)
```

If active failed, then reset and apply changes
```bash
    sudo systemctl reset-failed pilogger.service
    sudo systemctl daemon-reload
    sudo systemctl start pilogger.service
    #verify service started
    sudo systemctl status pilogger.service
    ```