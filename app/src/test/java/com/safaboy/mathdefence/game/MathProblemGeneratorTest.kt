package com.safaboy.mathdefence.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MathProblemGeneratorTest {

    @Test
    fun additionCategoryGeneratesValidSumsAndUniqueChoices() {
        val generator = MathProblemGenerator(Random(42))
        repeat(50) { wave ->
            val problem = generator.generate(MathCategory.ADDITION, wave + 1)
            assertEquals(MathOperation.ADD, problem.operation)
            assertEquals(problem.operandA + problem.operandB, problem.correctAnswer)
            assertEquals(4, problem.choices.size)
            assertEquals(4, problem.choices.distinct().size)
            assertTrue(problem.choices.contains(problem.correctAnswer))
            assertTrue(problem.choices.all { it >= 0 })
        }
    }

    @Test
    fun subtractionCategoryGeneratesNonNegativeDifferences() {
        val generator = MathProblemGenerator(Random(99))
        repeat(50) { wave ->
            val problem = generator.generate(MathCategory.SUBTRACTION, wave + 1)
            assertEquals(MathOperation.SUBTRACT, problem.operation)
            assertEquals(problem.operandA - problem.operandB, problem.correctAnswer)
            assertTrue(problem.correctAnswer >= 0)
            assertEquals(4, problem.choices.distinct().size)
            assertTrue(problem.choices.contains(problem.correctAnswer))
        }
    }

    @Test
    fun multiplicationCategoryGeneratesValidProducts() {
        val generator = MathProblemGenerator(Random(7))
        repeat(50) { wave ->
            val problem = generator.generate(MathCategory.MULTIPLICATION, wave + 1)
            assertEquals(MathOperation.MULTIPLY, problem.operation)
            assertEquals(problem.operandA * problem.operandB, problem.correctAnswer)
            assertEquals(4, problem.choices.distinct().size)
            assertTrue(problem.choices.contains(problem.correctAnswer))
        }
    }

    @Test
    fun divisionCategoryAlwaysProducesExactIntegerQuotients() {
        val generator = MathProblemGenerator(Random(123))
        repeat(80) { wave ->
            val problem = generator.generate(MathCategory.DIVISION, wave + 1)
            assertEquals(MathOperation.DIVIDE, problem.operation)
            assertTrue("Divisor must be positive", problem.operandB >= 2)
            assertEquals(0, problem.operandA % problem.operandB)
            assertEquals(problem.operandA / problem.operandB, problem.correctAnswer)
            assertEquals(4, problem.choices.distinct().size)
            assertTrue(problem.choices.contains(problem.correctAnswer))
        }
    }

    @Test
    fun mixtureCategoryCoversAllFourOperations() {
        val generator = MathProblemGenerator(Random(2026))
        val seen = mutableSetOf<MathOperation>()
        repeat(100) { idx ->
            val problem = generator.generate(MathCategory.MIXTURE, idx % 15 + 1)
            seen += problem.operation
            assertEquals(4, problem.choices.distinct().size)
            assertTrue(problem.choices.contains(problem.correctAnswer))
        }
        assertEquals(MathOperation.entries.toSet(), seen)
    }
}
