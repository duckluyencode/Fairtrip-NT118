# Runs shell commands on the TripFair EC2 host through SSM Run Command and prints the
# output, so no SSH port is ever opened. Run from Windows PowerShell:
#   .\backend\deploy\ssm-run.ps1 -InstanceId i-0123 -Commands @('docker ps')
param(
  [Parameter(Mandatory)][string]$InstanceId,
  [Parameter(Mandatory)][string[]]$Commands,
  [int]$TimeoutSeconds = 600
)
$ErrorActionPreference = 'Stop'
$aws = 'C:\Program Files\Amazon\AWSCLIV2\aws.exe'
$env:AWS_REGION = 'ap-southeast-1'
# Commands go through a file: PowerShell 5.1 mangles quotes in long arguments.
$paramsFile = Join-Path $env:TEMP 'tripfair-ssm-params.json'
@{ commands = $Commands; executionTimeout = @("$TimeoutSeconds") } | ConvertTo-Json -Depth 3 |
  Set-Content -Path $paramsFile -Encoding ascii
$id = & $aws ssm send-command --instance-ids $InstanceId --document-name AWS-RunShellScript `
  --parameters "file://$paramsFile" --timeout-seconds 60 --query 'Command.CommandId' --output text
Remove-Item $paramsFile
$deadline = (Get-Date).AddSeconds($TimeoutSeconds + 30)
do {
  Start-Sleep -Seconds 3
  $status = & $aws ssm get-command-invocation --command-id $id --instance-id $InstanceId `
    --query 'Status' --output text 2>$null
} while ($status -in @('Pending', 'InProgress', 'Delayed', '') -and (Get-Date) -lt $deadline)
$out = & $aws ssm get-command-invocation --command-id $id --instance-id $InstanceId --output json | ConvertFrom-Json
"status: $($out.Status) (exit $($out.ResponseCode))"
if ($out.StandardOutputContent) { $out.StandardOutputContent.TrimEnd() }
if ($out.StandardErrorContent) { 'stderr:'; $out.StandardErrorContent.TrimEnd() }
