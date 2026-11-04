<#
.SYNOPSIS
    Gerenciador de Ambientes (Dev, Staging, Prod) para Gerenciador Financeiro.
.DESCRIPTION
    Script para inicializar, parar e inspecionar os ambientes locais e em contêineres.
.EXAMPLE
    .\scripts\manage-env.ps1 -Environment dev -Action up
    .\scripts\manage-env.ps1 -Environment staging -Action up
    .\scripts\manage-env.ps1 -Environment prod -Action up
#>

param (
    [Parameter(Mandatory = $true)]
    [ValidateSet("dev", "staging", "prod")]
    [string]$Environment,

    [Parameter(Mandatory = $true)]
    [ValidateSet("up", "down", "restart", "logs", "status")]
    [string]$Action,

    [switch]$FullDev
)

$ErrorActionPreference = "Stop"

function Check-EnvFile([string]$file, [string]$example) {
    if (-not (Test-Path $file)) {
        if (Test-Path $example) {
            Write-Host "Arquivo '$file' não encontrado. Criando a partir de '$example'..." -ForegroundColor Yellow
            Copy-Item $example $file
        } else {
            Write-Warning "Arquivo de exemplo '$example' não encontrado!"
        }
    }
}

switch ($Environment) {
    "dev" {
        Check-EnvFile ".env" ".env.dev.example"
        $composeFile = "docker-compose.yml"
        $envFile = ".env"
        $cmdProfile = if ($FullDev) { "--profile full" } else { "" }

        switch ($Action) {
            "up" {
                Write-Host "Iniciando ambiente de DESENVOLVIMENTO (Dev)..." -ForegroundColor Cyan
                if ($FullDev) {
                    docker compose -f $composeFile --env-file $envFile --profile full up -d
                } else {
                    docker compose -f $composeFile --env-file $envFile up -d
                }
                Write-Host "Ambiente Dev iniciado com sucesso!" -ForegroundColor Green
                Write-Host "- MySQL Dev: localhost:3306 (Database: financeiro_dev_db)" -ForegroundColor Gray
                if ($FullDev) {
                    Write-Host "- App Dev: http://localhost:8080/swagger-ui.html" -ForegroundColor Gray
                } else {
                    Write-Host "- App pode ser iniciada via IDE (IntelliJ / VS Code) com profile 'dev'." -ForegroundColor Gray
                }
            }
            "down" {
                Write-Host "Parando ambiente de DESENVOLVIMENTO..." -ForegroundColor Cyan
                docker compose -f $composeFile --profile full down
            }
            "restart" {
                docker compose -f $composeFile down
                docker compose -f $composeFile up -d
            }
            "logs" {
                docker compose -f $composeFile logs -f
            }
            "status" {
                docker compose -f $composeFile ps
            }
        }
    }

    "staging" {
        Check-EnvFile ".env.staging" ".env.staging.example"
        $composeFile = "docker-compose.staging.yml"
        $envFile = ".env.staging"

        switch ($Action) {
            "up" {
                Write-Host "Iniciando ambiente de STAGING (Homologação / Pré-produção)..." -ForegroundColor Yellow
                docker compose -f $composeFile --env-file $envFile up -d --build
                Write-Host "Ambiente Staging iniciado com sucesso!" -ForegroundColor Green
                Write-Host "- App Staging: http://localhost:8081" -ForegroundColor Gray
                Write-Host "- Swagger Staging: http://localhost:8081/swagger-ui.html" -ForegroundColor Gray
                Write-Host "- Healthcheck: http://localhost:8081/actuator/health" -ForegroundColor Gray
            }
            "down" {
                Write-Host "Parando ambiente de STAGING..." -ForegroundColor Yellow
                docker compose -f $composeFile --env-file $envFile down
            }
            "restart" {
                docker compose -f $composeFile --env-file $envFile down
                docker compose -f $composeFile --env-file $envFile up -d --build
            }
            "logs" {
                docker compose -f $composeFile --env-file $envFile logs -f
            }
            "status" {
                docker compose -f $composeFile --env-file $envFile ps
            }
        }
    }

    "prod" {
        Check-EnvFile ".env.prod" ".env.prod.example"
        $composeFile = "docker-compose.prod.yml"
        $envFile = ".env.prod"

        switch ($Action) {
            "up" {
                Write-Host "Iniciando ambiente de PRODUÇÃO (Prod)..." -ForegroundColor Red
                docker compose -f $composeFile --env-file $envFile up -d --build
                Write-Host "Ambiente Produção iniciado com sucesso!" -ForegroundColor Green
                Write-Host "- App Prod: http://localhost:80 (ou porta configurada)" -ForegroundColor Gray
                Write-Host "- Healthcheck: http://localhost:80/actuator/health" -ForegroundColor Gray
                Write-Host "- Swagger / Documentação: DESATIVADA por segurança" -ForegroundColor Gray
            }
            "down" {
                Write-Host "Parando ambiente de PRODUÇÃO..." -ForegroundColor Red
                docker compose -f $composeFile --env-file $envFile down
            }
            "restart" {
                docker compose -f $composeFile --env-file $envFile down
                docker compose -f $composeFile --env-file $envFile up -d --build
            }
            "logs" {
                docker compose -f $composeFile --env-file $envFile logs -f
            }
            "status" {
                docker compose -f $composeFile --env-file $envFile ps
            }
        }
    }
}
