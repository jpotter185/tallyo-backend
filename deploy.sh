#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}Starting deployment...${NC}"
# Ensure SSH agent is running and key is loaded
echo -e "${YELLOW}Loading SSH key...${NC}"
eval "$(ssh-agent -s)" > /dev/null
ssh-add ~/.ssh/id_ed25519 2>/dev/null || ssh-add ~/.ssh/id_rsa 2>/dev/null

# Git pull
echo -e "${YELLOW}Pulling latest changes from Git...${NC}"
git pull

# Check if git pull was successful
if [ $? -ne 0 ]; then
    echo -e "${RED}Git pull failed!${NC}"
    exit 1
fi

echo -e "${GREEN}Git pull successful!${NC}"

# Check if .env exists
if [ ! -f .env ]; then
    echo -e "${RED}Error: .env file not found!${NC}"
    exit 1
fi

# Maven build
echo -e "${YELLOW}Building with Maven...${NC}"
./mvnw clean package -DskipTests

# Check if build was successful
if [ $? -ne 0 ]; then
    echo -e "${RED}Maven build failed!${NC}"
    exit 1
fi

echo -e "${GREEN}Maven build successful!${NC}"

# Zero-downtime deployment using docker compose up with --no-deps and --build
echo -e "${YELLOW}Starting new containers (zero-downtime deployment)...${NC}"

# Build new images
docker compose build

# Start new containers in detached mode (old containers still running)
docker compose up -d --no-deps --build

# Wait for new containers to be healthy
echo -e "${YELLOW}Waiting for new containers to be healthy...${NC}"
sleep 10

# Check if new containers are running
NEW_CONTAINERS=$(docker compose ps --services --filter "status=running" | wc -l)

if [ "$NEW_CONTAINERS" -eq 0 ]; then
    echo -e "${RED}New containers failed to start! Rolling back...${NC}"
    docker compose down
    exit 1
fi

# Remove old containers
echo -e "${YELLOW}Removing old containers...${NC}"
docker compose down --remove-orphans

echo -e "${GREEN}Deployment successful!${NC}"

# Show container status
docker compose ps

# Show logs
echo -e "${YELLOW}Showing logs (Ctrl+C to exit)...${NC}"
docker compose logs -f