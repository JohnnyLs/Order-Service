#!/bin/bash
docker-compose up -d
sleep 10  # Espera a que inicien
mvn clean install
mvn spring-boot:run