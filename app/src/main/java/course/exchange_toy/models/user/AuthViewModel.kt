package course.exchange_toy.models.user

import androidx.lifecycle.ViewModel

/**
 * ViewModel для хранения состояния формы авторизации/регистрации.
 * 
 * Переживает поворот экрана и другие изменения конфигурации.
 * Хранит только данные, не знает о UI компонентах.
 */
class AuthViewModel : ViewModel() {
    // Состояние полей ввода
    var apiHost: String = ""
    var email: String = ""
    var password: String = ""
    var confirmPassword: String = ""
    var firstName: String = ""
    var lastName: String = ""
    var middleName: String = ""
    
    // Текущий режим (true = регистрация, false = вход)
    var isRegisterMode: Boolean = false
    
    /**
     * Очистить все поля формы (кроме apiHost)
     * Вызывается после успешной авторизации/регистрации
     */
    fun clearForm() {
        email = ""
        password = ""
        confirmPassword = ""
        firstName = ""
        lastName = ""
        middleName = ""
        // apiHost НЕ очищаем - он нужен для следующих входов
    }
}

