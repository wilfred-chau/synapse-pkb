param(
    [string]$Summary,

    [string]$Description,

    [string]$SummaryFile,

    [string]$DescriptionFile,

    [ValidateSet('task', 'bug')]
    [string]$IssueType = 'task',

    [string]$ProjectKey = 'SPKB',

    [string]$ConfigPath = 'F:\synapse-pkb\.env.local'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
[Console]::InputEncoding = [System.Text.UTF8Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

if ($SummaryFile) {
    if (-not (Test-Path $SummaryFile)) {
        throw "Summary file not found: $SummaryFile"
    }
    $Summary = Get-Content -Raw -Encoding UTF8 $SummaryFile
}

if ($DescriptionFile) {
    if (-not (Test-Path $DescriptionFile)) {
        throw "Description file not found: $DescriptionFile"
    }
    $Description = Get-Content -Raw -Encoding UTF8 $DescriptionFile
}

if ([string]::IsNullOrWhiteSpace($Summary)) {
    throw "Summary is required. Provide -Summary or -SummaryFile."
}

if ([string]::IsNullOrWhiteSpace($Description)) {
    throw "Description is required. Provide -Description or -DescriptionFile."
}

$Summary = $Summary.Trim()
$Description = $Description.Trim()

if (-not (Test-Path $ConfigPath)) {
    throw "Jira config file not found: $ConfigPath"
}

$config = @{}
Get-Content $ConfigPath | Where-Object { $_ -and $_ -match '=' } | ForEach-Object {
    $parts = $_ -split '=', 2
    $config[$parts[0]] = $parts[1]
}

foreach ($required in @('JIRA_SITE', 'JIRA_EMAIL', 'JIRA_API_TOKEN')) {
    if (-not $config.ContainsKey($required) -or [string]::IsNullOrWhiteSpace($config[$required])) {
        throw "Missing required Jira config value: $required"
    }
}

$pair = '{0}:{1}' -f $config['JIRA_EMAIL'], $config['JIRA_API_TOKEN']
$basic = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes($pair))
$headers = @{
    Authorization = 'Basic ' + $basic
    Accept = 'application/json'
    'Content-Type' = 'application/json; charset=utf-8'
}

$project = Invoke-RestMethod -Uri ($config['JIRA_SITE'] + "/rest/api/3/project/$ProjectKey") -Headers $headers -Method Get

$resolvedType = $null
if ($IssueType -eq 'bug') {
    $resolvedType = $project.issueTypes |
        Where-Object { -not $_.subtask -and ($_.name -in @('Bug', '缺陷')) } |
        Select-Object -First 1
} else {
    $resolvedType = $project.issueTypes |
        Where-Object { -not $_.subtask -and ($_.name -in @('Task', '任务')) } |
        Select-Object -First 1
}

if (-not $resolvedType -and $IssueType -eq 'bug') {
    $resolvedType = $project.issueTypes |
        Where-Object { -not $_.subtask -and $_.name -notin @('Story', '故事', 'Epic', '长篇故事', 'Task', '任务') } |
        Select-Object -First 1
}

if (-not $resolvedType -and $IssueType -eq 'task') {
    $resolvedType = $project.issueTypes |
        Where-Object { -not $_.subtask -and $_.name -notin @('Story', '故事', 'Epic', '长篇故事', 'Bug', '缺陷') } |
        Select-Object -First 1
}

if (-not $resolvedType) {
    throw "Unable to resolve issue type '$IssueType' in project $ProjectKey"
}

$adfDescription = @{
    type = 'doc'
    version = 1
    content = @(
        @{
            type = 'paragraph'
            content = @(
                @{
                    type = 'text'
                    text = $Description
                }
            )
        }
    )
}

$payload = @{
    fields = @{
        project = @{
            key = $ProjectKey
        }
        summary = $Summary
        description = $adfDescription
        issuetype = @{
            id = $resolvedType.id
        }
    }
} | ConvertTo-Json -Depth 10

$payloadBytes = [System.Text.Encoding]::UTF8.GetBytes($payload)

$issue = Invoke-RestMethod -Uri ($config['JIRA_SITE'] + '/rest/api/3/issue') -Headers $headers -Method Post -Body $payloadBytes

[PSCustomObject]@{
    key = $issue.key
    id = $issue.id
    issueType = $resolvedType.name
    browseUrl = ($config['JIRA_SITE'] + '/browse/' + $issue.key)
} | ConvertTo-Json -Depth 5
