package course.exchange_toy.models.user

import android.content.Context

/**
 * Менеджер для сохранения user_id и настроек
 */
class UserSession(context: Context) {
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "exchange_toy_prefs"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_API_HOST = "api_host"
        private const val DEFAULT_HOST = "http://10.26.130.32:5001"
    }
    
    /**
     * Сохранить user_id после успешной авторизации
     */
    fun saveUserId(userId: String) {
        prefs.edit().putString(KEY_USER_ID, userId).apply()
    }
    
    /**
     * Получить сохраненный user_id
     */
    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }
    
    /**
     * Проверить, авторизован ли пользователь
     */
    fun isLoggedIn(): Boolean {
        return getUserId() != null
    }
    
    /**
     * Выйти (удалить user_id)
     */
    fun logout() {
        prefs.edit().remove(KEY_USER_ID).apply()
    }
    
    /**
     * Сохранить хост API
     */
    fun saveApiHost(host: String) {
        prefs.edit().putString(KEY_API_HOST, host).apply()
    }
    
    /**
     * Получить сохраненный хост API
     */
    fun getApiHost(): String {
        return prefs.getString(KEY_API_HOST, DEFAULT_HOST) ?: DEFAULT_HOST
    }
}

