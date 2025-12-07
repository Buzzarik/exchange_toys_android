package course.exchange_toy.controller

import course.exchange_toy.models.remote.ApiResult
import course.exchange_toy.models.remote.ExchangeToysClient
import course.exchange_toy.models.remote.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Контроллер для работы с обменами
 */
class ExchangeController(private val client: ExchangeToysClient) {
    
    /**
     * Callback для списка обменов
     */
    interface ExchangeListCallback {
        fun onLoading()
        fun onSuccess(exchanges: List<ExchangeInfo>, cursor: String?)
        fun onError(message: String)
    }
    
    /**
     * Callback для детального просмотра обмена
     */
    interface ExchangeDetailCallback {
        fun onLoading()
        fun onSuccess(exchange: ExchangeInfo)
        fun onError(message: String)
    }
    
    /**
     * Callback для изменения статуса обмена
     */
    interface ExchangeActionCallback {
        fun onLoading()
        fun onSuccess()
        fun onError(message: String)
    }
    
    /**
     * Получить список обменов пользователя
     */
    fun getMyExchanges(
        scope: CoroutineScope,
        userId: String,
        cursor: String? = null,
        limit: Int = 20,
        callback: ExchangeListCallback
    ) {
        scope.launch {
            callback.onLoading()
            
            // Показываем все обмены включая отмененные
            val query = QueryExchanges(statuses = listOf("created", "confirm", "success", "failed"))
            val body = RequestExchangeListBody(query, limit.toLong(), cursor)
            val request = RequestExchangeList(userId, body)
            
            when (val result = client.getExchangeList(request)) {
                is ApiResult.Ok -> {
                    callback.onSuccess(result.response.exchanges, result.response.cursor)
                }
                is ApiResult.Error -> {
                    callback.onError("Ошибка загрузки: ${result.error.message}")
                }
                is ApiResult.NetworkError -> {
                    callback.onError("Ошибка сети: ${result.throwable.message}")
                }
            }
        }
    }
    
    /**
     * Получить детали обмена
     */
    fun getExchangeDetails(
        scope: CoroutineScope,
        userId: String,
        exchangeId: String,
        callback: ExchangeDetailCallback
    ) {
        scope.launch {
            callback.onLoading()
            
            val request = RequestExchangeGet(userId, exchangeId)
            
            when (val result = client.getExchange(request)) {
                is ApiResult.Ok -> {
                    callback.onSuccess(result.response.exchange)
                }
                is ApiResult.Error -> {
                    callback.onError("Ошибка загрузки: ${result.error.message}")
                }
                is ApiResult.NetworkError -> {
                    callback.onError("Ошибка сети: ${result.throwable.message}")
                }
            }
        }
    }
    
    /**
     * Подтвердить обмен (confirm_1 или confirm_2)
     */
    fun confirmExchange(
        scope: CoroutineScope,
        userId: String,
        exchangeId: String,
        status: String, // "confirm_1" или "confirm_2"
        callback: ExchangeActionCallback
    ) {
        scope.launch {
            callback.onLoading()
            
            val body = RequestExchangePatchBody(status)
            val request = RequestExchangePatch(userId, exchangeId, body)
            
            when (val result = client.patchExchange(request)) {
                is ApiResult.Ok -> {
                    callback.onSuccess()
                }
                is ApiResult.Error -> {
                    callback.onError("Ошибка подтверждения: ${result.error.message}")
                }
                is ApiResult.NetworkError -> {
                    callback.onError("Ошибка сети: ${result.throwable.message}")
                }
            }
        }
    }
    
    /**
     * Отменить обмен (failed)
     */
    fun cancelExchange(
        scope: CoroutineScope,
        userId: String,
        exchangeId: String,
        callback: ExchangeActionCallback
    ) {
        scope.launch {
            callback.onLoading()
            
            val body = RequestExchangePatchBody("failed")
            val request = RequestExchangePatch(userId, exchangeId, body)
            
            when (val result = client.patchExchange(request)) {
                is ApiResult.Ok -> {
                    callback.onSuccess()
                }
                is ApiResult.Error -> {
                    callback.onError("Ошибка отмены: ${result.error.message}")
                }
                is ApiResult.NetworkError -> {
                    callback.onError("Ошибка сети: ${result.throwable.message}")
                }
            }
        }
    }
}

