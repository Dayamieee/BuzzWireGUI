# Buzz Wire Game

This is an interactive Java Swing GUI application for a Buzz Wire game that interfaces with an Arduino through serial communication. The game tracks the player's time and attempts, and maintains a leaderboard of the best players.

## Features

- Real-time tracking of game time and attempts
- Interactive GUI with game and leaderboard views
- Leaderboard system that ranks players based on time and lives remaining
- Serial communication with Arduino for game state updates
- Automatic game restart after game over

## Requirements

- Java 11 or higher
- Maven for dependency management
- Arduino connected to COM3 (configurable in the code)
- jSerialComm library (automatically managed by Maven)

## Setup

1. Connect your Arduino to your computer via USB
2. Upload the appropriate sketch to your Arduino (not included)
3. Build the Java application using Maven:

```
mvn clean package
```

4. Run the application:

```
java -jar target/BuzzWireGame-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## Game Rules

- Players start with 9 lives
- Each time the wire is touched ("buzzed"), a life is lost
- When all lives are lost, the game is over
- Players are ranked based on lives remaining and completion time
- Faster times with more lives remaining result in higher scores

## Arduino Communication

The Arduino should send the following commands over serial:

- Numbers (0-9): Indicates the number of attempts remaining
- "Restart": Restarts the game
- "Reset Timer": Resets the timer without resetting attempts

## Leaderboard

The leaderboard is stored in a CSV file named `leaderboard.csv` in the application directory. Players are ranked based on:

1. Lives remaining (more is better)
2. Time taken (less is better)

A score is calculated as: (lives_left * 1000) - time_in_seconds

## Customization

You can modify the following aspects of the game:

- COM port: Change the `SerialPort.getCommPort("COM3")` line to match your Arduino's port
- Game duration: Modify the `elapsedSeconds >= 180` condition to change the maximum game time
- Scoring formula: Adjust the calculation in the `loadLeaderboard()` method