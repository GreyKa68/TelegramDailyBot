# Telegram Daily Bot

## Table of Contents

- [Introduction](#introduction)
- [Features](#features)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Usage](#usage)
- [Contributing](#contributing)
- [License](#license)

## Introduction

This is a backend application for a Telegram bot built with Spring Boot. The bot helps manage daily tasks and notifications for groups, allowing users to add, delete, and edit users, notifications, and chats. It also features interaction with ChatGPT3 for quick Q&A sessions.

## Features

- **User Management:**
    - Add, delete, and edit users
- **Notification Management:**
    - Add, delete, and edit notifications
    - Send time-based notifications
    - Show list of notifications
- **Chat Management:**
    - Add, delete, and edit chats
    - Show list of users
    - Assign winners for daily tasks
    - Reset winners
- **ChatGPT3 Interaction:**
    - Interact with ChatGPT3

## Prerequisites

- Java 17
- Spring Boot 3.0.5
- Maven 3.x
- PostgreSQL 13.x

## Getting Started

Follow these steps to set up and run the Telegram Daily Bot:

### Step 1: Clone the repository

Clone this repository to your local machine:

```bash
git clone https://github.com/yourusername/telegramdailybot.git
```
### Step 2: Create a PostgreSQL database

Create a PostgreSQL database using `src/main/resources/db/schema.sql`.

### Step 3: Adjust the configuration
Set your:
- Telegram bot usename and token
- Connection to the database
- (optionally) GPT-3 API key

Open the `src/main/resources/application.properties` file and adjust the following properties:

```nano
telegrambot.botUsername=YOUR_TELEGRAM_BOT_USERNAME
telegrambot.botToken:=YOUR_TELEGRAM_BOT_TOKEN
spring.datasource.url=jdbc:postgresql://localhost:5432/YOUR_DATABASE_NAME
spring.datasource.username=YOUR_DATABASE_USERNAME
spring.datasource.password=YOUR_DATABASE_PASSWORD
openai.token=YOUR_GPT3_API_KEY
```
application.properties should be placed in the same folder as the jar file.

### Step 4: Build the project

Change your current working directory to the `telegramdailybot` folder:

```bash
cd telegramdailybot
```

Use Maven to build the project:

```bash
mvn clean install
```

### Step 5: Run the application

After the build is successful, run the Spring Boot application:

```bash
mvn spring-boot:run
```
Now the Telegram Daily Bot is up and running. You can start interacting with the bot by sending commands and messages.

### Step 6: Add first admin user

Get you Telegram user ID by sending `/getchatid` command to the bot.

Add first admin user manually in the database. You can do this by running the following SQL query:

```sql
INSERT INTO public.chats(name, telegramchatid, role)
VALUES ('<YourName>', <your telegram user id>, 'admin');
```

## Usage

- **/start:** Initialize the bot
- **/getchatid:** Get the chat ID
- **/next:** Select the next winner
- **/resetwinners:** Reset the winners list
- **/showusers:** Show the list of users
- **/shownotifications:** Show the list of notifications
- **/editusers:** Edit users
- **/editnotifications:** Edit notifications
- **/editchats:** Edit group chats - *this command is only available to admin users*
- **/askchatgpt3:** Ask a question to ChatGPT3

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

## License

[MIT](https://opensource.org/licenses/MIT)
