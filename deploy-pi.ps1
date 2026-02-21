$user = "frc302"
$hostName = "dragondataloggerz.local"  # or ip if mDNS fails
$login = "$user@$hostName"
$key = "./keys/id_ed25519"

Write-Host "Deploying to $hostName..."

# Copy service file
scp -i $key ./build/libs/PiLogger-linuxarm64-cross.jar `
    ${login}:/tmp/PiLogger-linuxarm64-cross.jar
scp -i $key ./deploy/pi-logger/home/frc302/deploy-pi.sh `
    ${login}:/home/frc302/deploy-pi.sh

ssh -i $key $login "chmod +x /home/frc302/deploy-pi.sh && /home/frc302/deploy-pi.sh"

Write-Host "$hostName deploy complete."

# SIG # Begin signature block
# MIIFXgYJKoZIhvcNAQcCoIIFTzCCBUsCAQExCzAJBgUrDgMCGgUAMGkGCisGAQQB
# gjcCAQSgWzBZMDQGCisGAQQBgjcCAR4wJgIDAQAABBAfzDtgWUsITrck0sYpfvNR
# AgEAAgEAAgEAAgEAAgEAMCEwCQYFKw4DAhoFAAQU+NtaZwK3nGhyFoQTPPZ3MO9w
# C0SgggL+MIIC+jCCAeKgAwIBAgIQdCBf4a6Zb7lBH19DbDbPKjANBgkqhkiG9w0B
# AQsFADAVMRMwEQYDVQQDDApGUkNEZXZDZXJ0MB4XDTI2MDIyMTE4MDIzM1oXDTI3
# MDIyMTE4MjIzM1owFTETMBEGA1UEAwwKRlJDRGV2Q2VydDCCASIwDQYJKoZIhvcN
# AQEBBQADggEPADCCAQoCggEBAMFnR+P68YFywkrNQ/Kmt2xZ4coBzoAFrzIFsJLZ
# P8zpMIPI6jhFF1ClIMuPC6yvRw49AyEwUduudgI+jOZUv9HVjgdNR2LB4F3Jv8/1
# kfHxJEqz1YvDN2qe4lASdN63HaheYOvbZx6d59Tc/LEF7RexLTvFb5qBMDsOgWs9
# gUKFSU5NNA66ep3xU4e7HFzTiDw1W6L1CVJQaRTUhwffavghd4sgihaVVC89zPXU
# V6yXRNEfF/eiZ8zfaF5H05+GsP9DyXdTbL8cTkgdE1gKzStLf9PZ9zUeCqJXe6EG
# fz0DaJOlwoqHE5KE2jmku0/rpMtRiQqvGS0w/g1DK6vmCm0CAwEAAaNGMEQwDgYD
# VR0PAQH/BAQDAgeAMBMGA1UdJQQMMAoGCCsGAQUFBwMDMB0GA1UdDgQWBBTtZa+6
# hXiMBsUlqYMVD/OdBYdzkDANBgkqhkiG9w0BAQsFAAOCAQEAtBUnR67+OQfwtfBD
# HyzYSQm0gDRo72Nn5SvBkEOt5q/UL9lvUSaaJdtF2CNfjsDCBnl7PBPBU60inhxa
# Z+pvHvNZoTH0mAt+lKD+yF4+ktCbNgmBgPK5arPK90anGD8BmR9ezo5vwoZOpDzS
# xCiBiiPXgt11hLtDwS/YL6LNgEL2i+4tKoqVooVT94KCnDFp6tyeof4x/ZJ2sMIH
# 2lQTXymD+Sq57UqPz5soln2Aer09t4XltH1RpZtPgpKghNQXtc4/PC4nRX9fp/I2
# SWrBGsGJDYnfy8Qzy1WvbhBGSaJaKlBVMN+4/wOd2BeaCLLx0ISwH0XkZkOkmLG3
# qmF2hjGCAcowggHGAgEBMCkwFTETMBEGA1UEAwwKRlJDRGV2Q2VydAIQdCBf4a6Z
# b7lBH19DbDbPKjAJBgUrDgMCGgUAoHgwGAYKKwYBBAGCNwIBDDEKMAigAoAAoQKA
# ADAZBgkqhkiG9w0BCQMxDAYKKwYBBAGCNwIBBDAcBgorBgEEAYI3AgELMQ4wDAYK
# KwYBBAGCNwIBFTAjBgkqhkiG9w0BCQQxFgQUrzNBUo3k2Qeq5ilOhjHLfiidV5Yw
# DQYJKoZIhvcNAQEBBQAEggEAFebZ7VJHbIiGdXdQWshiEWmF9B4e45bubjxGC/rX
# uyKYI8/BqrOGCTb2SBiVdaunNkEVOP6TYezfrJneyG0yzIU2QvS1qHy1nj582573
# pWCgzK/QENMssq5lOHZiO3d3rducVcHm9DXcBASVBYy9GbkkudaIxOVOvNrjI6SV
# yIigch/cO4Qgcb+dxW/KBOTITMI9c5fylRQG7uYToNfsOq9950ZiO16k4vIfDhoR
# M2sLLL3gQMn/OuLQcvrd7nsXIW2kilzKEAljeVzKDmDTQrRRD64iU+IJKGRdYLX/
# JtpxFepoLPU5v1NHa2fJurN5rWAeijAqeyfhzpgl72NcfA==
# SIG # End signature block
