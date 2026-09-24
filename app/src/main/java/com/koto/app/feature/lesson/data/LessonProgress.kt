package com.koto.app.feature.lesson.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class LessonProgress(context: Context) {
    // Placeholder completion belongs to different lessons; retain its file for rollback.
    private val preferences = context.applicationContext.getSharedPreferences("koto_foundation_v1", Context.MODE_PRIVATE)
    val srsTracker = SrsTracker(context).also { SrsTracker.defaultInstance = it }
    var completed by mutableStateOf(preferences.getStringSet("completed", emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toSet())
        private set
    fun complete(id: Int) {
        if (id in completed || FoundationLessons.levels.none { it.id == id }) return
        completed = completed + id
        preferences.edit().putStringSet("completed", completed.map { it.toString() }.toSet()).apply()
    }
}
