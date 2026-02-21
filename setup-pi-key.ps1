$ErrorActionPreference = "Stop"

$user = "frc302"
$hostName = "dragondataloggerz.local"  # or ip if mDNS fails
$key = ".\keys\id_ed25519"
$authFile = ".\deploy\pi-logger\home\frc302\.ssh\authorized_keys"

$login = "$user@$hostName"

# Create .ssh directory and set permissions
ssh $login "mkdir -p ~/.ssh && chmod 700 ~/.ssh"
if ($LASTEXITCODE -ne 0) { throw "Failed to create .ssh directory" }

# Copy authorized_keys file 
scp $authFile "${login}:~/.ssh/authorized_keys"
if ($LASTEXITCODE -ne 0) { throw "Failed to copy authorized_keys" }

# Set permissions
ssh $login "chmod 600 ~/.ssh/authorized_keys"
if ($LASTEXITCODE -ne 0) { throw "Failed to set permissions on authorized_keys" }

Write-Host "Pi key setup complete."


# SIG # Begin signature block
# MIIFXgYJKoZIhvcNAQcCoIIFTzCCBUsCAQExCzAJBgUrDgMCGgUAMGkGCisGAQQB
# gjcCAQSgWzBZMDQGCisGAQQBgjcCAR4wJgIDAQAABBAfzDtgWUsITrck0sYpfvNR
# AgEAAgEAAgEAAgEAAgEAMCEwCQYFKw4DAhoFAAQUGAJl7oIt5v8HUWXRAibDA3w+
# vhqgggL+MIIC+jCCAeKgAwIBAgIQdCBf4a6Zb7lBH19DbDbPKjANBgkqhkiG9w0B
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
# KwYBBAGCNwIBFTAjBgkqhkiG9w0BCQQxFgQUP0yxQ+fCk44enq1EaF1QJm+uRU4w
# DQYJKoZIhvcNAQEBBQAEggEAVOkIIPNR9ZAzmGf2+CVBiK8bDzO/sVrbjPf/wyAc
# cZFlcEchc/2dNzt5E3TnVcnRdPyKx57pbeDT8voOIZ0kyjo8PzGcE8MUslKQbEaK
# 7lA7JLdHfgKMnuNWpN6y8faakAHdqUQVEJbTOJ12kyRQcXKRCIbo8QYRNbJgQyPg
# Kw6pz+uVDi+SX8ORE8GCXe+fTjnLL7/IwZEeD6lV8ojKEZg9f6wR60o7grsEcBCU
# 5amf6W2Uj5xB8Cx0aPZf2nMB1PqCuZBfasGEQN6xL+Kj9CVQcwc2a1GeISjz8Ter
# eBRMUrN+q3Spp/4DhrPBjmC/VDHloNm5SZgsuiMJYlrJMA==
# SIG # End signature block
