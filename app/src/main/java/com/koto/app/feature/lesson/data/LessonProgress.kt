package com.koto.app.feature.lesson.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class LessonProgress(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("koto_lessons", Context.MODE_PRIVATE)
    var completed by mutableStateOf(preferences.getStringSet("completed", emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toSet())
        private set
    fun complete(id: Int) {
        if (id in completed) return
        completed = completed + id
        preferences.edit().putStringSet("completed", completed.map { it.toString() }.toSet()).apply()
    }
}
