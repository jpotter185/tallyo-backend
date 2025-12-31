#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}Starting deployment...${NC}"

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

# Stop existing containers
echo -e "${YELLOW}Stopping existing containers...${NC}"
docker compose down

# Start containers with rebuild
echo -e "${YELLOW}Starting Docker containers...${NC}"
docker compose up --build -d

# Check if containers started successfully
if [ $? -ne 0 ]; then
    echo -e "${RED}Failed to start Docker containers!${NC}"
    exit 1
fi

# Wait a moment for containers to initialize
sleep 3

# Show container status
echo -e "${GREEN}Containers started successfully!${NC}"
docker compose ps

# Show logs
echo -e "${YELLOW}Showing logs (Ctrl+C to exit)...${NC}"
docker compose logs -f