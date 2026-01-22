# Knucklebones: Ancient Game of Chance

A modern Android implementation of the classic dice game "Knucklebones," featuring a thematic "ancient" aesthetic, turn-based multiplayer, and a progressive AI challenge system.

## 🛠 Tech Stack

*   **Platform**: Android
*   **Language**: Kotlin
*   **UI Framework**: Jetpack Compose
*   **Design System**: Material 3
*   **Architecture**: MVVM (Model-View-ViewModel)
*   **Persistence**: SharedPreferences (for difficulty progression)
*   **Graphics**: Compose Canvas (for custom dice and shadow effects)

## 📜 Game Rules

### Objective
The game is played on two 3x3 boards. The goal is to have a higher total score than your opponent when one of the boards is completely filled.

### Gameplay
1.  **Turns**: Players take turns rolling a single 6-sided die.
2.  **Placement**: The rolled die must be placed in one of the three columns on the player's own board. A column that already has 3 dice cannot accept more.
3.  **Destruction**: When you place a die, any dice of the **same value** in the **opponent's corresponding column** are destroyed and removed from their board.

### Scoring
*   **Basic Score**: The score of a column is typically the sum of its dice.
*   **Multiplier Bonus**: If a column contains multiple dice of the same value, the score for those dice is multiplied by the number of matches.
    *   *Example*: A column with `4 - 1 - 4` scores `(4 * 2) + (1 * 1) + (4 * 2) = 17`.
    *   *Example*: A column with `6 - 6 - 6` scores `(6 * 3) + (6 * 3) + (6 * 3) = 54`.
*   **Total Score**: The sum of all three column scores.

## 🤖 AI & Difficulty Progression

The game features an AI opponent with four distinct difficulty levels. To progress, you must prove your skill!

### Unlocking System
*   **Easy**: Unlocked by default.
*   **Medium**: Beat the AI on **Easy** to unlock.
*   **Hard**: Beat the AI on **Medium** to unlock.
*   **Expert**: Beat the AI on **Hard** to unlock.

### Dynamic Probability (Dice Weighting)
Higher difficulties implement a "luck-thinning" mechanic. When a number is rolled, its probability of appearing again decreases:
*   **Easy**: Fair dice (16.6% for every number).
*   **Medium**: Rolled number's weight decreases by **2x**.
*   **Hard**: Rolled number's weight decreases by **4x**.
*   **Expert**: Rolled number's weight decreases by **6x**.

## 🎨 Visual Identity

The app uses a carefully curated color palette to evoke an ancient, tabletop gaming feel:
*   **Background**: Deep Dark Brown (`#2A1A10`)
*   **Typography**: Gold Brown (`#9E6A38`) & Light Cream (`#E5D5A0`)
*   **Game Surface**: Dark Forest Green (`#305039`)
*   **Dice**: Aged Cream (`#F2E7C3`) with Tan shading (`#D8C18E`)

## 📱 Features

*   **Player vs Player**: Local turn-based matching.
*   **Player vs AI**: Progressive challenge mode.
*   **Immersive Experience**: Full-screen gameplay with hidden system bars.
*   **Safety Prompts**: Confirmation dialogs when exiting active games.
*   **Persistent Progress**: Your unlocked levels are saved automatically.
