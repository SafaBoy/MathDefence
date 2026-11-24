package com.safaboy.mathdefence.data

import android.content.Context
import com.safaboy.mathdefence.game.MathCategory

class ScoreRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getHighScore(category: MathCategory): Int {
        return prefs.getInt(keyFor(category), 0)
    }

    fun getAllHighScores(): Map<MathCategory, Int> {
        return MathCategory.entries.associateWith { getHighScore(it) }
    }

    fun saveHighScoreIfBetter(category: MathCategory, score: Int): Boolean {
        val current = getHighScore(category)
        if (score > current) {
            prefs.edit().putInt(keyFor(category), score).apply()
            return true
        }
        return false
    }

    fun isSoundEnabled(): Boolean {
        return prefs.getBoolean(KEY_SOUND_ENABLED, true)
    }

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }

    private fun keyFor(category: MathCategory): String = "high_score_${category.name}"

    companion object {
        private const val PREFS_NAME = "math_defence_prefs"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
    }
}
