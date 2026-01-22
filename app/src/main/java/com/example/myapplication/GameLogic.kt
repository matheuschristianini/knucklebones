package com.example.myapplication

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlin.random.Random

data class Board(val columns: List<List<Int>> = List(3) { emptyList() }) {
    fun isFull(): Boolean = columns.all { it.size == 3 }
    fun isColumnFull(colIndex: Int): Boolean = columns[colIndex].size == 3
    
    fun calculateColumnScore(colIndex: Int): Int {
        val column = columns[colIndex]
        val counts = column.groupingBy { it }.eachCount()
        return column.sumOf { it * (counts[it] ?: 1) }
    }
    
    fun calculateTotalScore(): Int {
        return (0..2).sumOf { calculateColumnScore(it) }
    }

    fun addDie(colIndex: Int, value: Int): Board {
        if (isColumnFull(colIndex)) return this
        val newColumns = columns.toMutableList()
        newColumns[colIndex] = newColumns[colIndex] + value
        return Board(newColumns)
    }

    fun removeDiceWithValue(colIndex: Int, value: Int): Board {
        val newColumns = columns.toMutableList()
        newColumns[colIndex] = newColumns[colIndex].filter { it != value }
        return Board(newColumns)
    }
}

enum class Player { Player1, Player2 }

enum class Difficulty(val reductionFactor: Int, val order: Int) {
    Easy(0, 0),
    Medium(2, 1),
    Hard(4, 2),
    Expert(6, 3);

    companion object {
        fun fromOrder(order: Int): Difficulty = values().find { it.order == order } ?: Easy
    }
}

data class GameState(
    val player1Board: Board = Board(),
    val player2Board: Board = Board(),
    val currentPlayer: Player = Player.Player1,
    val currentRoll: Int = 1,
    val gameOver: Boolean = false,
    val vsAI: Boolean = false,
    val difficulty: Difficulty = Difficulty.Easy,
    val diceWeight: Map<Int, Int> = mapOf(1 to 100, 2 to 100, 3 to 100, 4 to 100, 5 to 100, 6 to 100)
)

class KnucklebonesViewModel : ViewModel() {
    var state by mutableStateOf(GameState())
        private set

    init {
        state = state.copy(currentRoll = rollWeightedDie(state.diceWeight))
    }

    fun initGame(vsAI: Boolean, difficulty: Difficulty = Difficulty.Easy) {
        val initialWeights = mapOf(1 to 100, 2 to 100, 3 to 100, 4 to 100, 5 to 100, 6 to 100)
        state = GameState(
            vsAI = vsAI, 
            difficulty = difficulty,
            diceWeight = initialWeights
        ).copy(currentRoll = rollWeightedDie(initialWeights))
    }

    private fun rollWeightedDie(weights: Map<Int, Int>): Int {
        val totalWeight = weights.values.sum()
        var randomValue = Random.nextInt(totalWeight)
        
        for ((dieValue, weight) in weights) {
            if (randomValue < weight) return dieValue
            randomValue -= weight
        }
        return Random.nextInt(1, 7)
    }

    private fun getUpdatedWeights(rolledValue: Int, currentWeights: Map<Int, Int>): Map<Int, Int> {
        if (state.difficulty == Difficulty.Easy) return currentWeights
        
        val newWeights = currentWeights.toMutableMap()
        val weight = newWeights[rolledValue] ?: 100
        
        newWeights[rolledValue] = (weight / state.difficulty.reductionFactor).coerceAtLeast(1)
        
        return newWeights
    }

    fun placeDie(colIndex: Int, context: Context? = null) {
        if (state.gameOver) return
        
        val currentPlayer = state.currentPlayer
        val currentRoll = state.currentRoll
        
        val ownBoard = if (currentPlayer == Player.Player1) state.player1Board else state.player2Board
        if (ownBoard.isColumnFull(colIndex)) return

        val nextWeights = getUpdatedWeights(currentRoll, state.diceWeight)

        var newP1Board = state.player1Board
        var newP2Board = state.player2Board

        if (currentPlayer == Player.Player1) {
            newP1Board = newP1Board.addDie(colIndex, currentRoll)
            newP2Board = newP2Board.removeDiceWithValue(colIndex, currentRoll)
        } else {
            newP2Board = newP2Board.addDie(colIndex, currentRoll)
            newP1Board = newP1Board.removeDiceWithValue(colIndex, currentRoll)
        }

        val isGameOver = newP1Board.isFull() || newP2Board.isFull()
        
        if (isGameOver && state.vsAI && currentPlayer == Player.Player1 && context != null) {
            val p1Score = newP1Board.calculateTotalScore()
            val p2Score = newP2Board.calculateTotalScore()
            if (p1Score > p2Score) {
                unlockNextDifficulty(context, state.difficulty)
            }
        }

        state = state.copy(
            player1Board = newP1Board,
            player2Board = newP2Board,
            currentPlayer = if (currentPlayer == Player.Player1) Player.Player2 else Player.Player1,
            diceWeight = nextWeights,
            currentRoll = if (isGameOver) 0 else rollWeightedDie(nextWeights),
            gameOver = isGameOver
        )
    }

    private fun unlockNextDifficulty(context: Context, currentDifficulty: Difficulty) {
        val prefs = context.getSharedPreferences("game_prefs", Context.MODE_PRIVATE)
        val currentMax = prefs.getInt("max_unlocked_difficulty", 0)
        if (currentDifficulty.order == currentMax && currentMax < 3) {
            prefs.edit().putInt("max_unlocked_difficulty", currentMax + 1).apply()
        }
    }

    fun aiTurn(context: Context) {
        if (state.gameOver || state.currentPlayer != Player.Player2) return
        
        val roll = state.currentRoll
        val p1Board = state.player1Board
        val aiBoard = state.player2Board
        
        val availableCols = (0..2).filter { !aiBoard.isColumnFull(it) }
        if (availableCols.isEmpty()) return
        
        var bestCol = availableCols.random()
        
        for (col in availableCols) {
            if (p1Board.columns[col].contains(roll)) {
                bestCol = col
                break
            }
        }
        
        if (!p1Board.columns[bestCol].contains(roll)) {
             for (col in availableCols) {
                if (aiBoard.columns[col].contains(roll)) {
                    bestCol = col
                    break
                }
            }
        }

        placeDie(bestCol, context)
    }

    fun resetGame() {
        val difficulty = state.difficulty
        val vsAI = state.vsAI
        val initialWeights = mapOf(1 to 100, 2 to 100, 3 to 100, 4 to 100, 5 to 100, 6 to 100)
        state = GameState(
            vsAI = vsAI, 
            difficulty = difficulty,
            diceWeight = initialWeights
        ).copy(currentRoll = rollWeightedDie(initialWeights))
    }
}
