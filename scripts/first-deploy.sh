#!/bin/bash
# ==============================================================================
# First Deploy - Creates database schema on RDS
# ==============================================================================
# Run ONLY ONCE on the first deploy to create tables in RDS.
# After this, the app uses ddl-auto=validate (never modifies schema).
#
# Usage: chmod +x scripts/first-deploy.sh && ./scripts/first-deploy.sh
# ==============================================================================

set -e

APP_DIR="/home/$USER/task-manager-api"
cd "$APP_DIR"

echo "=========================================="
echo " First Deploy - Creating database schema"
echo "=========================================="
echo ""
echo "This will temporarily use ddl-auto=update to create tables,"
echo "then switch back to validate for safety."
echo ""

# Build the image
echo "[1/3] Building Docker image..."
docker-compose -f docker-compose.prod.yml build --no-cache

# Run with ddl-auto=update to create tables
echo "[2/3] Starting app with schema creation..."
docker-compose -f docker-compose.prod.yml run --rm \
  -e SPRING_JPA_HIBERNATE_DDL_AUTO=update \
  app

echo "[3/3] Restarting in production mode (validate)..."
docker-compose -f docker-compose.prod.yml up -d

echo ""
echo "=========================================="
echo " First deploy complete!"
echo "=========================================="
echo " Tables created in RDS."
echo " App is running with ddl-auto=validate."
echo " Check: docker logs -f taskmanager-api"
echo ""
