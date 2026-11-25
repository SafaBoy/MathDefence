package com.safaboy.mathdefence.game

import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

class MathProblemGenerator(
    private val random: Random = Random.Default
) {

    private data class ScaledProblem(
        val opA: Long,
        val opB: Long,
        val answer: Long,
        val scale: Int,
        val operation: MathOperation
    )

    fun generate(
        category: MathCategory,
        skillLevel: Int,
        choiceCount: Int = 4
    ): MathProblem {
        val safeLevel = max(1, skillLevel)
        val safeChoices = choiceCount.coerceAtLeast(2)
        val operation = pickOperation(category)

        val scaled = when (operation) {
            MathOperation.ADD -> generateAddition(safeLevel)
            MathOperation.SUBTRACT -> generateSubtraction(safeLevel)
            MathOperation.MULTIPLY -> generateMultiplication(safeLevel)
            MathOperation.DIVIDE -> generateDivision(safeLevel)
        }

        val correctStr = formatScaled(scaled.answer, scaled.scale)
        val choices = generateChoices(scaled, safeChoices)

        return MathProblem(
            displayOperandA = formatScaled(scaled.opA, scaled.scale),
            displayOperandB = formatScaled(scaled.opB, scaled.scale),
            operation = operation,
            correctAnswer = correctStr,
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

    /**
     * Skill Level progression for Addition:
     * L1: 2-digit + 1-digit (10..99 + 1..9)
     * L2: 2-digit + 2-digit (10..99 + 10..99)
     * L3: 3-digit + 2-digit (100..999 + 10..99)
     * L4: 4-digit + 2-digit (1000..9999 + 10..99)
     * L5: 1 d.p. decimals, reduced digits (1.1..9.9 + 0.5..9.9)
     * L6: 1 d.p. decimals, 2d.1 + 1d.1 (10.1..49.9 + 1.1..9.9)
     * L7: 1 d.p. decimals, 2d.1 + 2d.1 (10.1..99.9 + 10.1..99.9)
     * L8: 2 d.p. decimals, reduced digits (1.05..9.95 + 0.15..9.95)
     * L9+: 2 d.p. decimals, scaling digits
     */
    private fun generateAddition(level: Int): ScaledProblem = when (level) {
        1 -> {
            val a = random.nextInt(10, 100).toLong()
            val b = random.nextInt(1, 10).toLong()
            ScaledProblem(a, b, a + b, scale = 0, MathOperation.ADD)
        }
        2 -> {
            val a = random.nextInt(10, 100).toLong()
            val b = random.nextInt(10, 100).toLong()
            ScaledProblem(a, b, a + b, scale = 0, MathOperation.ADD)
        }
        3 -> {
            val a = random.nextInt(100, 1000).toLong()
            val b = random.nextInt(10, 100).toLong()
            ScaledProblem(a, b, a + b, scale = 0, MathOperation.ADD)
        }
        4 -> {
            val a = random.nextInt(1000, 10000).toLong()
            val b = random.nextInt(10, 100).toLong()
            ScaledProblem(a, b, a + b, scale = 0, MathOperation.ADD)
        }
        5 -> {
            val a = nextNonWholeDecimal(11, 99, 10)
            val b = nextNonWholeDecimal(5, 99, 10)
            ScaledProblem(a, b, a + b, scale = 1, MathOperation.ADD)
        }
        6 -> {
            val a = nextNonWholeDecimal(101, 499, 10)
            val b = nextNonWholeDecimal(11, 99, 10)
            ScaledProblem(a, b, a + b, scale = 1, MathOperation.ADD)
        }
        7 -> {
            val a = nextNonWholeDecimal(101, 999, 10)
            val b = nextNonWholeDecimal(101, 999, 10)
            ScaledProblem(a, b, a + b, scale = 1, MathOperation.ADD)
        }
        8 -> {
            val a = nextNonWholeDecimal(105, 995, 100)
            val b = nextNonWholeDecimal(15, 995, 100)
            ScaledProblem(a, b, a + b, scale = 2, MathOperation.ADD)
        }
        else -> {
            val cap = (level * 12).coerceAtMost(250) * 100
            val a = nextNonWholeDecimal(1005, cap, 100)
            val b = nextNonWholeDecimal(105, 2995, 100)
            ScaledProblem(a, b, a + b, scale = 2, MathOperation.ADD)
        }
    }

    /**
     * Skill Level progression for Subtraction (always non-negative a >= b):
     * L1: 2-digit - 1-digit (10..99 - 1..9)
     * L2: 2-digit - 2-digit (20..99 - 10..a)
     * L3: 3-digit - 2-digit (100..999 - 10..99)
     * L4: 4-digit - 2-digit (1000..9999 - 10..99)
     * L5: 1 d.p. decimals, reduced digits (2.1..9.9 - 0.5..a)
     * L6: 1 d.p. decimals, 2d.1 - 1d.1 (10.1..49.9 - 1.1..9.9)
     * L7: 1 d.p. decimals, 2d.1 - 2d.1 (20.1..99.9 - 10.1..a)
     * L8: 2 d.p. decimals, reduced digits (2.05..9.95 - 0.15..a)
     * L9+: 2 d.p. decimals, scaling digits
     */
    private fun generateSubtraction(level: Int): ScaledProblem = when (level) {
        1 -> {
            val a = random.nextInt(10, 100).toLong()
            val b = random.nextInt(1, 10).toLong()
            ScaledProblem(a, b, a - b, scale = 0, MathOperation.SUBTRACT)
        }
        2 -> {
            val a = random.nextInt(20, 100).toLong()
            val b = random.nextInt(10, a.toInt() + 1).toLong()
            ScaledProblem(a, b, a - b, scale = 0, MathOperation.SUBTRACT)
        }
        3 -> {
            val a = random.nextInt(100, 1000).toLong()
            val b = random.nextInt(10, 100).toLong()
            ScaledProblem(a, b, a - b, scale = 0, MathOperation.SUBTRACT)
        }
        4 -> {
            val a = random.nextInt(1000, 10000).toLong()
            val b = random.nextInt(10, 100).toLong()
            ScaledProblem(a, b, a - b, scale = 0, MathOperation.SUBTRACT)
        }
        5 -> {
            val a = nextNonWholeDecimal(21, 99, 10)
            val b = nextNonWholeDecimal(5, (a - 1).toInt().coerceAtLeast(6), 10)
            ScaledProblem(a, b, a - b, scale = 1, MathOperation.SUBTRACT)
        }
        6 -> {
            val a = nextNonWholeDecimal(101, 499, 10)
            val b = nextNonWholeDecimal(11, 99, 10)
            ScaledProblem(a, b, a - b, scale = 1, MathOperation.SUBTRACT)
        }
        7 -> {
            val a = nextNonWholeDecimal(201, 999, 10)
            val b = nextNonWholeDecimal(101, (a - 1).toInt().coerceAtLeast(102), 10)
            ScaledProblem(a, b, a - b, scale = 1, MathOperation.SUBTRACT)
        }
        8 -> {
            val a = nextNonWholeDecimal(205, 995, 100)
            val b = nextNonWholeDecimal(15, (a - 1).toInt().coerceAtLeast(16), 100)
            ScaledProblem(a, b, a - b, scale = 2, MathOperation.SUBTRACT)
        }
        else -> {
            val cap = (level * 12).coerceAtMost(250) * 100
            val a = nextNonWholeDecimal(1505, cap, 100)
            val b = nextNonWholeDecimal(105, minOf(2995, (a - 1).toInt()), 100)
            ScaledProblem(a, b, a - b, scale = 2, MathOperation.SUBTRACT)
        }
    }

    /**
     * Skill Level progression for Multiplication tables:
     * L1: 1-5x tables
     * L2: adds 6 and 7 (1-7x)
     * L3: adds 8, 9, 10 (1-10x)
     * L4: adds 11 and 12 (1-12x)
     * L5: adds 13 (1-13x)
     * L6+: adds 14, 15, ... (12 + (level - 4))
     */
    private fun generateMultiplication(level: Int): ScaledProblem {
        val table = pickTableNumber(level)
        val maxMultiplier = when {
            level <= 3 -> 10
            level == 4 -> 12
            else -> 12
        }
        val multiplier = random.nextInt(1, maxMultiplier + 1)
        val (a, b) = if (random.nextBoolean()) {
            table.toLong() to multiplier.toLong()
        } else {
            multiplier.toLong() to table.toLong()
        }
        return ScaledProblem(a, b, a * b, scale = 0, MathOperation.MULTIPLY)
    }

    /**
     * Skill Level progression for Division (exact integer quotients matching the same times tables):
     * L1: 1-5 tables
     * L2: adds 6 and 7
     * L3: adds 8, 9, 10
     * L4: adds 11 and 12
     * L5+: adds 13, 14, ...
     */
    private fun generateDivision(level: Int): ScaledProblem {
        val divisor = pickTableNumber(level)
        val maxQuotient = if (level <= 3) 10 else 12
        val quotient = random.nextInt(1, maxQuotient + 1)
        val dividend = (divisor * quotient).toLong()
        return ScaledProblem(dividend, divisor.toLong(), quotient.toLong(), scale = 0, MathOperation.DIVIDE)
    }

    private fun pickTableNumber(level: Int): Int {
        val allTables = allowedTablesForLevel(level)
        val newlyUnlocked = newlyUnlockedTablesForLevel(level)
        // Bias slightly toward newly unlocked tables so each Skill Level jump feels distinct
        return if (newlyUnlocked.isNotEmpty() && level > 1 && random.nextFloat() < 0.60f) {
            newlyUnlocked[random.nextInt(newlyUnlocked.size)]
        } else {
            allTables[random.nextInt(allTables.size)]
        }
    }

    private fun nextNonWholeDecimal(minScaled: Int, maxScaled: Int, factor: Int): Long {
        if (maxScaled <= minScaled) return minScaled.toLong()
        repeat(8) {
            val candidate = random.nextInt(minScaled, maxScaled + 1)
            if (candidate % factor != 0) return candidate.toLong()
        }
        val fallback = random.nextInt(minScaled, maxScaled + 1)
        return if (fallback % factor == 0 && fallback + 1 <= maxScaled) {
            (fallback + 1).toLong()
        } else {
            fallback.toLong()
        }
    }

    private fun generateChoices(
        problem: ScaledProblem,
        choiceCount: Int
    ): List<String> {
        val correctStr = formatScaled(problem.answer, problem.scale)
        val candidates = linkedSetOf(correctStr)
        val unitStep = when (problem.scale) {
            0 -> 1L
            1 -> 1L // 0.1
            else -> 5L // 0.05
        }
        val majorStep = when (problem.scale) {
            0 -> 10L
            1 -> 10L // 1.0
            else -> 100L // 1.00
        }

        val plausible = mutableListOf<Long>()
        when (problem.operation) {
            MathOperation.ADD, MathOperation.SUBTRACT -> {
                plausible += listOf(
                    problem.answer + unitStep,
                    problem.answer - unitStep,
                    problem.answer + unitStep * 2,
                    problem.answer - unitStep * 2,
                    problem.answer + majorStep,
                    problem.answer - majorStep,
                    problem.answer + unitStep * 3,
                    problem.answer - unitStep * 3,
                    problem.answer + majorStep * 2,
                    problem.answer - majorStep * 2
                )
                if (problem.scale == 0 && problem.answer >= 100) {
                    plausible += listOf(problem.answer + 100, problem.answer - 100)
                }
            }
            MathOperation.MULTIPLY -> {
                plausible += listOf(
                    problem.opA * (problem.opB + 1),
                    problem.opA * (problem.opB - 1),
                    (problem.opA + 1) * problem.opB,
                    (problem.opA - 1) * problem.opB,
                    problem.answer + 2,
                    problem.answer - 2,
                    problem.answer + 10,
                    problem.answer - 10,
                    problem.opA + problem.opB,
                    problem.answer + 3,
                    problem.answer - 3
                )
            }
            MathOperation.DIVIDE -> {
                plausible += listOf(
                    problem.answer + 1,
                    problem.answer - 1,
                    problem.answer + 2,
                    problem.answer - 2,
                    problem.answer + 3,
                    problem.answer - 3,
                    problem.answer + 4,
                    problem.answer + 5,
                    problem.answer + 6
                )
            }
        }

        plausible
            .filter { it >= 0L && it != problem.answer }
            .shuffled(random)
            .forEach { value ->
                if (candidates.size < choiceCount) {
                    candidates += formatScaled(value, problem.scale)
                }
            }

        var spread = max(6L, abs(problem.answer) / 4L + 4L)
        var guard = 0
        while (candidates.size < choiceCount && guard < 120) {
            val rawDelta = random.nextLong(-spread, spread + 1)
            val delta = if (problem.scale == 2) (rawDelta / 5L) * 5L else rawDelta
            val fallback = problem.answer + delta
            if (fallback >= 0L && fallback != problem.answer) {
                candidates += formatScaled(fallback, problem.scale)
            }
            guard++
            if (guard % 20 == 0) spread += max(4L, majorStep)
        }

        var seq = 1L
        while (candidates.size < choiceCount) {
            candidates += formatScaled(problem.answer + seq * unitStep, problem.scale)
            seq++
        }

        return candidates.toList().shuffled(random)
    }

    companion object {
        fun maxTableForLevel(level: Int): Int = when (max(1, level)) {
            1 -> 5
            2 -> 7
            3 -> 10
            4 -> 12
            else -> 12 + (level - 4) // L5 -> 13, L6 -> 14, L7 -> 15...
        }

        fun allowedTablesForLevel(level: Int): List<Int> = (1..maxTableForLevel(level)).toList()

        fun newlyUnlockedTablesForLevel(level: Int): List<Int> = when (max(1, level)) {
            1 -> (1..5).toList()
            2 -> listOf(6, 7)
            3 -> listOf(8, 9, 10)
            4 -> listOf(11, 12)
            else -> listOf(maxTableForLevel(level))
        }

        fun describeSkillLevel(category: MathCategory, skillLevel: Int): String {
            val lvl = max(1, skillLevel)
            return when (category) {
                MathCategory.MULTIPLICATION, MathCategory.DIVISION -> {
                    "1–${maxTableForLevel(lvl)}× Tables"
                }
                MathCategory.ADDITION, MathCategory.SUBTRACTION -> {
                    when (lvl) {
                        1 -> "2-Digit ± 1-Digit"
                        2 -> "2-Digit ± 2-Digit"
                        3 -> "3-Digit ± 2-Digit"
                        4 -> "4-Digit ± 2-Digit"
                        5 -> "Decimals (1 d.p.)"
                        6 -> "Decimals (2d.1 ± 1d.1)"
                        7 -> "Decimals (2d.1 ± 2d.1)"
                        8 -> "Decimals (2 d.p.)"
                        else -> "Advanced Decimals"
                    }
                }
                MathCategory.MIXTURE -> {
                    val addDesc = when (lvl) {
                        1 -> "2d±1d"
                        2 -> "2d±2d"
                        3 -> "3d±2d"
                        4 -> "4d±2d"
                        5, 6, 7 -> "1dp"
                        else -> "2dp"
                    }
                    "1–${maxTableForLevel(lvl)}× & $addDesc"
                }
            }
        }

        fun formatScaled(value: Long, scale: Int): String {
            if (scale <= 0) return value.toString()
            val factor = if (scale == 1) 10L else 100L
            val negative = value < 0
            val absVal = abs(value)
            val whole = absVal / factor
            val frac = absVal % factor
            val sign = if (negative) "-" else ""
            if (frac == 0L) return "$sign$whole"
            val fracPadded = frac.toString().padStart(scale, '0').trimEnd('0')
            return "$sign$whole.$fracPadded"
        }
    }
}
