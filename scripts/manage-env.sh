#!/usr/bin/env bash
set -e

# ==============================================================================
# Script de Gerenciamento de Ambientes (Dev, Staging, Prod)
# ==============================================================================

ENV=$1
ACTION=$2
FULL_DEV=$3

function check_env_file() {
    local target=$1
    local template=$2
    if [ ! -f "$target" ]; then
        if [ -f "$template" ]; then
            echo "Aviso: Arquivo $target não encontrado. Copiando de $template..."
            cp "$template" "$target"
        fi
    fi
}

if [ -z "$ENV" ] || [ -z "$ACTION" ]; then
    echo "Uso: ./scripts/manage-env.sh [dev|staging|prod] [up|down|restart|logs|status] [--full]"
    exit 1
fi

case "$ENV" in
    dev)
        check_env_file ".env" ".env.dev.example"
        COMPOSE_FILE="docker-compose.yml"
        ENV_FILE=".env"
        if [ "$ACTION" == "up" ]; then
            if [ "$FULL_DEV" == "--full" ]; then
                docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" --profile full up -d
            else
                docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d
            fi
            echo "Ambiente Dev iniciado!"
        elif [ "$ACTION" == "down" ]; then
            docker compose -f "$COMPOSE_FILE" --profile full down
        elif [ "$ACTION" == "logs" ]; then
            docker compose -f "$COMPOSE_FILE" logs -f
        elif [ "$ACTION" == "status" ]; then
            docker compose -f "$COMPOSE_FILE" ps
        fi
        ;;

    staging)
        check_env_file ".env.staging" ".env.staging.example"
        COMPOSE_FILE="docker-compose.staging.yml"
        ENV_FILE=".env.staging"
        if [ "$ACTION" == "up" ]; then
            docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d --build
            echo "Ambiente Staging iniciado em http://localhost:8081 !"
        elif [ "$ACTION" == "down" ]; then
            docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" down
        elif [ "$ACTION" == "logs" ]; then
            docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" logs -f
        elif [ "$ACTION" == "status" ]; then
            docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps
        fi
        ;;

    prod)
        check_env_file ".env.prod" ".env.prod.example"
        COMPOSE_FILE="docker-compose.prod.yml"
        ENV_FILE=".env.prod"
        if [ "$ACTION" == "up" ]; then
            docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d --build
            echo "Ambiente Prod iniciado em http://localhost:80 !"
        elif [ "$ACTION" == "down" ]; then
            docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" down
        elif [ "$ACTION" == "logs" ]; then
            docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" logs -f
        elif [ "$ACTION" == "status" ]; then
            docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps
        fi
        ;;

    *)
        echo "Ambiente inválido. Escolha entre: dev, staging, prod"
        exit 1
        ;;
esac
