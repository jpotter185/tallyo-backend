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
./mvnw clean package

# Check if build was successful
if [ $? -ne 0 ]; then
    echo -e "${RED}Maven build failed!${NC}"
    exit 1
fi

echo -e "${GREEN}Maven build successful!${NC}"

# Apply new database migrations (db/migrations) while the current app is still up, so a
# failure leaves it running. Already-applied ones are skipped.
echo -e "${YELLOW}Running database migrations...${NC}"
docker compose run --rm migrate

if [ $? -ne 0 ]; then
    echo -e "${RED}Database migration failed! The running app was left as is.${NC}"
    exit 1
fi

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

# Show recent logs
echo -e "${YELLOW}Recent logs:${NC}"
docker compose logs --tail=50