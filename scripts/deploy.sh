#!/bin/bash
# ==============================================================================
# Deploy Script - Run on EC2 to update the application
# ==============================================================================
# Usage: chmod +x scripts/deploy.sh && ./scripts/deploy.sh
# ==============================================================================

set -e

APP_DIR="/home/$USER/task-manager-api"
cd "$APP_DIR"

echo "=========================================="
echo " Deploying Task Manager API"
echo "=========================================="

# 1. Pull latest code
echo "[1/4] Pulling latest code..."
git pull origin master

# 2. Build and restart
echo "[2/4] Building Docker image..."
docker-compose -f docker-compose.prod.yml build --no-cache

# 3. Restart with zero-downtime (stop old, start new)
echo "[3/4] Restarting application..."
docker-compose -f docker-compose.prod.yml up -d

# 4. Cleanup old images
echo "[4/4] Cleaning up old images..."
docker image prune -f

echo ""
echo "=========================================="
echo " Deploy complete!"
echo "=========================================="
echo " Check logs: docker logs -f taskmanager-api"
echo " Health:     curl http://localhost:8080/v3/api-docs"
echo ""
