package io.github.stevep99.scouty.llm

import android.content.Context
import android.content.SharedPreferences

class LlmSettings(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_BASE_URL, value.trim()).apply()

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_API_KEY, value.trim()).apply()

    var modelName: String
        get() = prefs.getString(KEY_MODEL_NAME, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_MODEL_NAME, value.trim()).apply()

    fun isConfigured(): Boolean = baseUrl.isNotBlank()

    private companion object {
        const val PREFS_NAME = "llm_settings"
        const val KEY_BASE_URL = "base_url"
        const val KEY_API_KEY = "api_key"
        const val KEY_MODEL_NAME = "model_name"
    }
}
