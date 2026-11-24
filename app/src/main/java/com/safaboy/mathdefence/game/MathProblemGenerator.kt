package com.safaboy.mathdefence.game

import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

class MathProblemGenerator(
    private val random: Random = Random.Default
) {

    fun generate(
        category: MathCategory,
        wave: Int,
        choiceCount: Int = 4
    ): MathProblem {
        val safeWave = max(1, wave)
        val operation = pickOperation(category)
        val (a, b, answer) = when (operation) {
            MathOperation.ADD -> generateAddition(safeWave)
            MathOperation.SUBTRACT -> generateSubtraction(safeWave)
            MathOperation.MULTIPLY -> generateMultiplication(safeWave)
            MathOperation.DIVIDE -> generateDivision(safeWave)
        }

        val choices = generateChoices(
            a = a,
            b = b,
            operation = operation,
            correctAnswer = answer,
            choiceCount = choiceCount
        )

        return MathProblem(
            operandA = a,
            operandB = b,
            operation = operation,
            correctAnswer = answer,
            choices = choices
        )
    }

    private fun pickOperation(category: MathCategory): MathOperation = when (category) {
        MathCategory.ADDITION -> MathOperation.ADD
        MathCategory.SUBTRACTION -> MathOperation.SUBTRACT
        MathCategory.MULTIPLICATION -> MathOperation.MULTIPLY
        MathCategory.DIVISION -> MathOperation.DIVIDE
        MathCategory.MIXTURE -> MathOperation.entries[random.nextInt(MathOperation.entries.size)]
    }

    private fun generateAddition(wave: Int): Triple<Int, Int, Int> {
        val (minVal, maxVal) = when {
            wave <= 3 -> 2 to 15
            wave <= 8 -> 7 to 35
            wave <= 14 -> 12 to 65
            else -> 18 to 95
        }
        val a = random.nextInt(minVal, maxVal + 1)
        val b = random.nextInt(minVal, maxVal + 1)
        return Triple(a, b, a + b)
    }

    private fun generateSubtraction(wave: Int): Triple<Int, Int, Int> {
        val (minA, maxA) = when {
            wave <= 3 -> 6 to 22
            wave <= 8 -> 16 to 55
            wave <= 14 -> 25 to 85
            else -> 35 to 120
        }
        val a = random.nextInt(minA, maxA + 1)
        val minB = if (wave <= 3) 1 else 4
        val b = random.nextInt(minB, max(minB + 1, a))
        return Triple(a, b, a - b)
    }

    private fun generateMultiplication(wave: Int): Triple<Int, Int, Int> {
        val (minF, maxF) = when {
            wave <= 3 -> 2 to 9
            wave <= 8 -> 3 to 12
            wave <= 14 -> 4 to 13
            else -> 5 to 15
        }
        val a = random.nextInt(minF, maxF + 1)
        val b = random.nextInt(2, maxF + 1)
        return Triple(a, b, a * b)
    }

    private fun generateDivision(wave: Int): Triple<Int, Int, Int> {
        val (maxDivisor, maxQuotient) = when {
            wave <= 3 -> 9 to 9
            wave <= 8 -> 12 to 12
            wave <= 14 -> 12 to 15
            else -> 15 to 15
        }
        val divisor = random.nextInt(2, maxDivisor + 1)
        val quotient = random.nextInt(2, maxQuotient + 1)
        val dividend = divisor * quotient
        return Triple(dividend, divisor, quotient)
    }

    private fun generateChoices(
        a: Int,
        b: Int,
        operation: MathOperation,
        correctAnswer: Int,
        choiceCount: Int
    ): List<Int> {
        val candidates = linkedSetOf(correctAnswer)
        val plausible = mutableListOf<Int>()

        when (operation) {
            MathOperation.ADD -> {
                plausible += listOf(
                    correctAnswer + 1,
                    correctAnswer - 1,
                    correctAnswer + 2,
                    correctAnswer - 2,
                    correctAnswer + 10,
                    correctAnswer - 10,
                    correctAnswer + 3,
                    correctAnswer - 3
                )
            }
            MathOperation.SUBTRACT -> {
                plausible += listOf(
                    correctAnswer + 1,
                    correctAnswer - 1,
                    correctAnswer + 2,
                    correctAnswer - 2,
                    correctAnswer + 10,
                    correctAnswer - 10,
                    correctAnswer + 4,
                    correctAnswer - 4
                )
            }
            MathOperation.MULTIPLY -> {
                plausible += listOf(
                    a * (b + 1),
                    a * (b - 1),
                    (a + 1) * b,
                    (a - 1) * b,
                    correctAnswer + 2,
                    correctAnswer - 2,
                    correctAnswer + 10,
                    correctAnswer - 10,
                    a + b
                )
            }
            MathOperation.DIVIDE -> {
                plausible += listOf(
                    correctAnswer + 1,
                    correctAnswer - 1,
                    correctAnswer + 2,
                    correctAnswer - 2,
                    correctAnswer + 3,
                    correctAnswer - 3,
                    correctAnswer + 4,
                    correctAnswer + 5
                )
            }
        }

        plausible
            .filter { it >= 0 && it != correctAnswer }
            .shuffled(random)
            .forEach { value ->
                if (candidates.size < choiceCount) {
                    candidates += value
                }
            }

        var spread = max(5, abs(correctAnswer) / 3 + 4)
        var guard = 0
        while (candidates.size < choiceCount && guard < 100) {
            val delta = random.nextInt(-spread, spread + 1)
            val fallback = correctAnswer + delta
            if (fallback >= 0 && fallback != correctAnswer) {
                candidates += fallback
            }
            guard++
            if (guard % 15 == 0) spread += 4
        }

        var fallbackSeq = 1
        while (candidates.size < choiceCount) {
            candidates += (correctAnswer + fallbackSeq)
            fallbackSeq++
        }

        return candidates.toList().shuffled(random)
    }
}
