#!/bin/bash
# ==============================================================================
# EC2 Initial Setup Script
# ==============================================================================
# Run this script ONCE on a fresh Amazon Linux 2023 / Ubuntu EC2 instance
# Usage: chmod +x scripts/ec2-setup.sh && ./scripts/ec2-setup.sh
# ==============================================================================

set -e

echo "=========================================="
echo " Task Manager API - EC2 Setup"
echo "=========================================="

# ── 1. Update system ────────────────────────────────────────────────
echo "[1/5] Updating system..."
sudo yum update -y 2>/dev/null || sudo apt-get update -y

# ── 2. Install Docker ───────────────────────────────────────────────
echo "[2/5] Installing Docker..."
if ! command -v docker &> /dev/null; then
    sudo yum install -y docker 2>/dev/null || sudo apt-get install -y docker.io
    sudo systemctl start docker
    sudo systemctl enable docker
    sudo usermod -aG docker $USER
    echo "Docker installed. You may need to re-login for group changes."
else
    echo "Docker already installed."
fi

# ── 3. Install Docker Compose ───────────────────────────────────────
echo "[3/5] Installing Docker Compose..."
if ! command -v docker-compose &> /dev/null; then
    sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" \
        -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose
    echo "Docker Compose installed."
else
    echo "Docker Compose already installed."
fi

# ── 4. Install Git ──────────────────────────────────────────────────
echo "[4/5] Installing Git..."
sudo yum install -y git 2>/dev/null || sudo apt-get install -y git

# ── 5. Clone repository ────────────────────────────────────────────
echo "[5/5] Cloning repository..."
APP_DIR="/home/$USER/task-manager-api"
if [ ! -d "$APP_DIR" ]; then
    git clone https://github.com/reazew/task-manager-api.git "$APP_DIR"
    echo "Repository cloned to $APP_DIR"
else
    echo "Repository already exists at $APP_DIR"
    cd "$APP_DIR" && git pull origin master
fi

echo ""
echo "=========================================="
echo " Setup complete!"
echo "=========================================="
echo ""
echo " Next steps:"
echo " 1. cd $APP_DIR"
echo " 2. cp .env.production.example .env"
echo " 3. nano .env  (fill in real values)"
echo " 4. docker-compose -f docker-compose.prod.yml up -d --build"
echo " 5. docker logs -f taskmanager-api"
echo ""
