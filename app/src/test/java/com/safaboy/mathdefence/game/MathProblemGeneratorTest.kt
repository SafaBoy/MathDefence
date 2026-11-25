package com.safaboy.mathdefence.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import kotlin.random.Random

class MathProblemGeneratorTest {

    @Test
    fun multiplicationSkillLevelsFollowTimesTableProgression() {
        val generator = MathProblemGenerator(Random(42))

        // L1: 1-5x tables
        repeat(60) {
            val p = generator.generate(MathCategory.MULTIPLICATION, skillLevel = 1)
            val a = p.displayOperandA.toInt()
            val b = p.displayOperandB.toInt()
            assertTrue("L1 must have a factor in 1..5, got $a x $b", a in 1..5 || b in 1..5)
            assertTrue("L1 factors <= 10", a <= 10 && b <= 10)
            assertEquals((a * b).toString(), p.correctAnswer)
            assertEquals(4, p.choices.distinct().size)
            assertTrue(p.choices.contains(p.correctAnswer))
        }

        // L2: adds 6 and 7
        val l2Tables = mutableSetOf<Int>()
        repeat(80) {
            val p = generator.generate(MathCategory.MULTIPLICATION, skillLevel = 2)
            val a = p.displayOperandA.toInt()
            val b = p.displayOperandB.toInt()
            assertTrue("L2 must use 1..7 table, got $a x $b", a in 1..7 || b in 1..7)
            if (a in 6..7) l2Tables += a
            if (b in 6..7) l2Tables += b
        }
        assertTrue("L2 should include 6 and 7 tables", l2Tables.containsAll(listOf(6, 7)))

        // Verify table progression ceilings
        assertEquals(5, MathProblemGenerator.maxTableForLevel(1))
        assertEquals(7, MathProblemGenerator.maxTableForLevel(2))
        assertEquals(10, MathProblemGenerator.maxTableForLevel(3))
        assertEquals(12, MathProblemGenerator.maxTableForLevel(4))
        assertEquals(13, MathProblemGenerator.maxTableForLevel(5))
        assertEquals(14, MathProblemGenerator.maxTableForLevel(6))
    }

    @Test
    fun divisionSkillLevelsAlwaysProduceExactQuotientsInUnlockedTables() {
        val generator = MathProblemGenerator(Random(123))

        for (level in 1..5) {
            val maxTable = MathProblemGenerator.maxTableForLevel(level)
            repeat(50) {
                val p = generator.generate(MathCategory.DIVISION, skillLevel = level)
                assertEquals(MathOperation.DIVIDE, p.operation)
                val dividend = p.displayOperandA.toInt()
                val divisor = p.displayOperandB.toInt()
                val quotient = p.correctAnswer.toInt()
                assertTrue("Divisor $divisor out of 1..$maxTable for L$level", divisor in 1..maxTable)
                assertEquals(0, dividend % divisor)
                assertEquals(dividend / divisor, quotient)
                assertEquals(4, p.choices.distinct().size)
                assertTrue(p.choices.contains(p.correctAnswer))
            }
        }
    }

    @Test
    fun additionAndSubtractionSkillLevelsScaleDigitsAndIntroduceDecimals() {
        val generator = MathProblemGenerator(Random(77))

        // L1: 2-digit +/- 1-digit
        repeat(40) {
            val add = generator.generate(MathCategory.ADDITION, skillLevel = 1)
            assertTrue(add.displayOperandA.toInt() in 10..99)
            assertTrue(add.displayOperandB.toInt() in 1..9)
            assertEquals((add.displayOperandA.toInt() + add.displayOperandB.toInt()).toString(), add.correctAnswer)

            val sub = generator.generate(MathCategory.SUBTRACTION, skillLevel = 1)
            assertTrue(sub.displayOperandA.toInt() in 10..99)
            assertTrue(sub.displayOperandB.toInt() in 1..9)
            assertEquals((sub.displayOperandA.toInt() - sub.displayOperandB.toInt()).toString(), sub.correctAnswer)
        }

        // L2: 2-digit +/- 2-digit
        repeat(30) {
            val add = generator.generate(MathCategory.ADDITION, skillLevel = 2)
            assertTrue(add.displayOperandA.toInt() in 10..99)
            assertTrue(add.displayOperandB.toInt() in 10..99)
        }

        // L3: 3-digit +/- 2-digit
        repeat(30) {
            val add = generator.generate(MathCategory.ADDITION, skillLevel = 3)
            assertTrue(add.displayOperandA.toInt() in 100..999)
            assertTrue(add.displayOperandB.toInt() in 10..99)
        }

        // L4: 4-digit +/- 2-digit
        repeat(30) {
            val sub = generator.generate(MathCategory.SUBTRACTION, skillLevel = 4)
            assertTrue(sub.displayOperandA.toInt() in 1000..9999)
            assertTrue(sub.displayOperandB.toInt() in 10..99)
        }

        // L5: 1 decimal place with reduced digits
        var sawDecimal = false
        repeat(40) {
            val add = generator.generate(MathCategory.ADDITION, skillLevel = 5)
            val a = BigDecimal(add.displayOperandA)
            val b = BigDecimal(add.displayOperandB)
            val ans = BigDecimal(add.correctAnswer)
            assertEquals(0, a.add(b).compareTo(ans))
            assertTrue("L5 should reduce digits (< 10)", a < BigDecimal("10.0") && b < BigDecimal("10.0"))
            if (add.displayOperandA.contains('.') || add.displayOperandB.contains('.')) {
                sawDecimal = true
            }
            assertEquals(4, add.choices.distinct().size)
            assertTrue(add.choices.contains(add.correctAnswer))
        }
        assertTrue("L5 must introduce decimal places", sawDecimal)

        // L8: 2 decimal places
        var sawTwoDp = false
        repeat(40) {
            val sub = generator.generate(MathCategory.SUBTRACTION, skillLevel = 8)
            val a = BigDecimal(sub.displayOperandA)
            val b = BigDecimal(sub.displayOperandB)
            val ans = BigDecimal(sub.correctAnswer)
            assertEquals(0, a.subtract(b).compareTo(ans))
            assertTrue(ans >= BigDecimal.ZERO)
            if (a.scale() == 2 || b.scale() == 2) sawTwoDp = true
        }
        assertTrue("L8 should produce 2 decimal place values", sawTwoDp)
    }

    @Test
    fun variableChoiceCountWorksForBossComponentRounds() {
        val generator = MathProblemGenerator(Random(555))
        for (count in 2..6) {
            val problem = generator.generate(MathCategory.MIXTURE, skillLevel = 5, choiceCount = count)
            assertEquals(count, problem.choices.size)
            assertEquals(count, problem.choices.distinct().size)
            assertTrue(problem.choices.contains(problem.correctAnswer))
        }
    }

    @Test
    fun mixtureCategoryCoversAllFourOperations() {
        val generator = MathProblemGenerator(Random(2026))
        val seen = mutableSetOf<MathOperation>()
        repeat(100) { idx ->
            val problem = generator.generate(MathCategory.MIXTURE, skillLevel = (idx % 5) + 1)
            seen += problem.operation
            assertEquals(4, problem.choices.distinct().size)
            assertTrue(problem.choices.contains(problem.correctAnswer))
        }
        assertEquals(MathOperation.entries.toSet(), seen)
    }
}
