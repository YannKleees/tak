# Tak - A Strategic Board Game

Welcome to **Tak**, a digital rendition of the classic strategy board game. This implementation brings the timeless tactics and strategy of Tak to your computer, allowing you to play against another human player locally.


# Developed by Ken.

## Table of Contents

- [Introduction](#introduction)
- [Game Rules](#game-rules)
  - [Objective](#objective)
  - [Components](#components)
  - [Basic Rules](#basic-rules)
    - [Setup](#setup)
    - [First Move](#first-move)
    - [Turns](#turns)
    - [Placing a Piece](#placing-a-piece)
    - [Moving a Stack](#moving-a-stack)
    - [Stacking Rules](#stacking-rules)
    - [Capstone Movement](#capstone-movement)
    - [Winning the Game](#winning-the-game)
- [How to Play](#how-to-play)
  - [Controls](#controls)
    - [Selecting a Piece](#selecting-a-piece)
    - [Placing a Piece](#placing-a-piece-1)
    - [Moving a Stack](#moving-a-stack-1)
  - [Notes](#notes)
- [Installation and Running the Game](#installation-and-running-the-game)
  - [Prerequisites](#prerequisites)
  - [Steps](#steps)
    - [Clone the Repository](#clone-the-repository)
    - [Build the Project](#build-the-project)
    - [Run the Game](#run-the-game)
  - [Troubleshooting](#troubleshooting)
- [Game Mechanics and Features](#game-mechanics-and-features)
  - [Technology Stack](#technology-stack)
  - [Key Features](#key-features)
  - [Architecture Overview](#architecture-overview)
    - [Core Logic](#core-logic)
    - [Rendering and Input](#rendering-and-input)
- [Controls](#controls-1)
  - [Camera](#camera)
- [Credits](#credits)
- [Introduction](#introduction-1)

## Introduction

**Tak** is a two-player abstract strategy game that challenges players to build a road connecting opposite sides of the board. It combines simple rules with deep strategy, making it enjoyable for both casual players and serious tacticians.

This project is built using **LibGDX**, a cross-platform Java game development framework, providing smooth graphics and responsive controls.

## Game Rules

### Objective

- **Road Victory**: Be the first to create a continuous path (road) of your pieces connecting opposite sides of the board.
- **Flat Victory**: If the board is completely filled with no road victory, the player with the most flat stones on top of stacks wins.

### Components

- **Flat Stones**: Basic pieces that can be part of a road and can be stacked.
- **Standing Stones (Walls)**: Block opponent's road but cannot be part of your own road.
- **Capstones**: Special pieces that can flatten standing stones and can be part of a road.

### Basic Rules

#### Setup

- The game is played on a square board of size 5x5.
- Each player starts with 21 flat stones, 10 standing stones, and 1 capstone.

#### First Move

- Players take turns placing one of their opponent's flat stones on the board.
- No standing stones or capstones can be placed during the first two moves.

#### Turns

On your turn, you may either place a piece or move a stack.

#### Placing a Piece

- Place a flat stone, standing stone, or capstone on an empty square.
- Pieces are placed on your own side starting from the third move.

#### Moving a Stack

- You may move pieces from a stack you control (your piece is on top).
- Move pieces in a straight line (up, down, left, or right).
- You can move up to as many pieces as the board size (maximum carry limit).
- You must drop at least one piece on each square you move over.

#### Stacking Rules

- You can stack your pieces on top of flat stones.
- Standing stones cannot have pieces stacked on top of them unless by a capstone.
- Capstones cannot have any pieces stacked on top of them.

#### Capstone Movement

- Capstones can flatten standing stones (turn them into flat stones) when moved onto them.
- Capstones count as part of your road.

#### Winning the Game

- **Road Victory**: Achieved immediately when a player connects opposite sides with a continuous road.
- **Flat Victory**: If the board is filled and no road victory is declared, the player with the most flat stones on top wins.

## How to Play

### Controls

#### Selecting a Piece

- Use the **hotbar** in the middle of the screen to select a piece type (flat stone, standing stone, or capstone).
- The available pieces are shown along with the remaining count.

#### Placing a Piece

- Click on an empty square on the board to place your selected piece.

#### Moving a Stack

- Click on a stack where your piece is on top to select it.
- The selected stack will be highlighted.
- Click on a square in the direction you want to move (must be adjacent in a straight line).
- If moving more than one piece, you will be prompted to enter drop counts separated by commas.
  - **Example**: Enter `1,1` to drop one piece on the first square and one on the next.

### Notes

## What you can do
- You can play against a Minimax Heurisitic Agent or against an Agent that uses a ANN. You can also train nets     using two different trainers.

- **First Two Moves**: Only flat stones can be placed, and they must be your opponent's stones.
- **Piece Limits**: Keep an eye on the remaining pieces displayed next to the hotbar.
- **Current Player**: The current player is indicated on the right side at the top of the screen.
- **Starting Pieces**: Each player starts with 21 flat stones, 10 standing stones, and 1 capstone.

## Installation and Running the Game

### Prerequisites

- **Java Development Kit (JDK) 8** or higher installed on your system.
- **Gradle** is included with the project via the Gradle wrapper.

### Steps

#### Clone the Repository

git clone https://github.com/yourusername/TakGame.git
cd TakGame

# Run the game by entering in the terminal:

./gradlew build
./gradlew lwjgl3:run