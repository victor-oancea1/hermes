# Hermes Go

This is a Go implementation of the Hermes project, a reactive RESTful tool for Kafka messaging.

## Project Structure

The project follows standard Go project layout:

- `cmd/hermes`: Main application entry point
- `internal/domain`: Core domain models
- `internal/application`: Application services and interfaces
- `internal/message`: Message handling implementation
- `internal/core`: Core business logic
- `internal/web`: Web layer (RESTful API)

## Features

- Reactive Kafka message consumption
- Kafka message production
- RESTful API for interacting with Kafka

## API Endpoints

- `GET /messages`: Retrieve messages from Kafka
  - Query parameters:
    - `topic` (required): Kafka topic to consume from
    - `expression` (optional): Filter expression for message keys
    - `limit` (optional): Maximum number of messages to retrieve
    - `offsetType` (optional): START, END, or SPECIFIC
    - `offsetValue` (required if offsetType is SPECIFIC): Specific offset value

- `POST /messages`: Send messages to Kafka
  - Request body: Array of Event objects

## Building and Running

```bash
cd hermes-go
go build -o hermes ./cmd/hermes
./hermes
```

## Environment Variables

- `KAFKA_BOOTSTRAP_SERVERS`: Kafka bootstrap servers (default: localhost:9092)
- `PORT`: HTTP server port (default: 8080)
