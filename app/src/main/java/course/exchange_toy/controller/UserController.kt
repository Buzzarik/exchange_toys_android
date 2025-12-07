package course.exchange_toy.controller

import course.exchange_toy.models.remote.ApiResult
import course.exchange_toy.models.remote.ExchangeToysClient
import course.exchange_toy.models.remote.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Контроллер для работы с авторизацией и регистрацией
 */
class UserController(private val client: ExchangeToysClient) {
    
    /**
     * Callback для обработки состояний
     */
    interface AuthCallback {
        fun onLoading()
        fun onSuccess(userId: String)
        fun onError(message: String)
    }
    
    /**
     * Регистрация нового пользователя с callback
     */
    fun register(
        scope: CoroutineScope,
        email: String,
        password: String,
        confirmPassword: String,
        firstName: String,
        lastName: String,
        middleName: String? = null,
        callback: AuthCallback
    ) {
        scope.launch {
            callback.onLoading()
            
            val userName = UserName(
                firstName = firstName,
                lastName = lastName,
                middleName = middleName
            )
            val body = RequestRegisterBody(
                userName = userName,
                password = password,
                confirmPassword = confirmPassword,
                email = email
            )
            
            when (val result = client.register(RequestRegister(body))) {
                is ApiResult.Ok -> {
                    callback.onSuccess(result.response.userId)
                }
                is ApiResult.Error -> {
                    callback.onError("Ошибка регистрации: ${result.error.message}")
                }
                is ApiResult.NetworkError -> {
                    callback.onError("Ошибка сети: ${result.throwable.message}")
                }
            }
        }
    }
    
    /**
     * Вход пользователя с callback
     */
    fun login(
        scope: CoroutineScope,
        email: String,
        password: String,
        callback: AuthCallback
    ) {
        scope.launch {
            callback.onLoading()
            
            val body = RequestLoginBody(
                password = password,
                email = email
            )
            
            when (val result = client.login(RequestLogin(body))) {
                is ApiResult.Ok -> {
                    callback.onSuccess(result.response.userId)
                }
                is ApiResult.Error -> {
                    callback.onError("Ошибка входа: ${result.error.message}")
                }
                is ApiResult.NetworkError -> {
                    callback.onError("Ошибка сети: ${result.throwable.message}")
                }
            }
        }
    }
}

