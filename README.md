# expense-tracker

A CLI tool for tracking money carried and spent for grocery shopping.
Built on top of [baby-redis](https://github.com/mariusflores/baby-redis) 
as the data store.

## Status

🚧 **In development.**

## Features

- Track money carried and spent with timestamped entries
- Running balance updated on every transaction
- Monthly expense history with dates and notes
- Period-based tracking (monthly)

## Usage

```bash
carry 2000                   # Add funds to balance
spent 350 groceries          # Record an expense
spent 200 online-shopping    # Record another expense
balance                      # Show current balance
history                      # Show expenses for current month
help                         # Show available commands
```

## Prerequisites

- Java 21+
- Maven
- [baby-redis-client](https://github.com/mariusflores/baby-redis-client) installed locally
- A running [baby-redis](https://github.com/mariusflores/baby-redis) server

## Building

```bash
git clone https://github.com/mariusflores/expense-tracker.git
cd expense-tracker
mvn clean package
```

## Running

```bash
java -jar target/expense-tracker.jar <command> [args]
```

## Related

- [baby-redis](https://github.com/mariusflores/baby-redis) — the server
- [baby-redis-client](https://github.com/mariusflores/baby-redis-client) — the client library
- [rant-logger](https://github.com/mariusflores/rant-logger) — another CLI tool built on baby-redis