#!/bin/sh
# Cria o usuário somente leitura do painel de observabilidade. Executado pelo PostgreSQL apenas na criação
# do volume de dados. As permissões de leitura são concedidas pela migração que cria as views de tempo de
# execução; este script só cria o usuário, sem acesso a nenhuma tabela.
set -e

psql -v ON_ERROR_STOP=1 -v reader_password="$GRAFANA_DB_PASSWORD" \
    --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<'EOSQL'
CREATE ROLE grafana_reader LOGIN PASSWORD :'reader_password';
EOSQL
