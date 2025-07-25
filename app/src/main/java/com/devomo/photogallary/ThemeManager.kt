package com.devomo.photogallary

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.devomo.data.Constants.Constans

object ThemeManager {

    fun getSavedThemeMode(context: Context): Int {
        val sharedPreferences =
            context.getSharedPreferences(Constans.PREF_KEY, Context.MODE_PRIVATE)
        val isDark = sharedPreferences.getBoolean(Constans.SWITCH_BUTTON_KEY, false)
        return if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
    }
}