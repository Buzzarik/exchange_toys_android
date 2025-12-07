package course.exchange_toy.controller

import course.exchange_toy.models.remote.ApiResult
import course.exchange_toy.models.remote.ExchangeToysClient
import course.exchange_toy.models.remote.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

/**
 * Контроллер для работы с игрушками и обменами
 */
class ToyController(private val client: ExchangeToysClient) {
    
    /**
     * Callback для операций с игрушками
     */
    interface ToyCallback {
        fun onLoading()
        fun onSuccess()
        fun onError(message: String)
    }
    
    /**
     * Callback для получения списка игрушек
     */
    interface ToysListCallback {
        fun onLoading()
        fun onSuccess(toys: List<Toy>, cursor: String?)
        fun onError(message: String)
    }
    
    /**
     * Callback для получения одной игрушки
     */
    interface ToyDetailCallback {
        fun onLoading()
        fun onSuccess(toy: Toy)
        fun onError(message: String)
    }
    
    /**
     * Callback для создания обмена
     */
    interface ExchangeCallback {
        fun onLoading()
        fun onSuccess()
        fun onError(message: String)
    }
    
    /**
     * Получить список игрушек пользователя
     */
    fun getMyToys(
        scope: CoroutineScope,
        userId: String,
        cursor: String? = null,
        limit: Int = 20,
        callback: ToysListCallback
    ) {
        scope.launch {
            callback.onLoading()
            
            val query = QueryToys(userIds = listOf(userId), statuses = listOf("created", "exchanging"))
            val body = RequestToysListBody(query, limit.toLong(), cursor)
            val request = RequestToysList(userId, body)
            
            when (val result = client.getToysList(request)) {
                is ApiResult.Ok -> {
                    callback.onSuccess(result.response.toys, result.response.cursor)
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
     * Получить список игрушек других пользователей (магазин)
     * Исключает игрушки текущего пользователя
     * Показывает только игрушки со статусом "exchanging"
     */
    fun getShopToys(
        scope: CoroutineScope,
        currentUserId: String,
        cursor: String? = null,
        limit: Int = 20,
        callback: ToysListCallback
    ) {
        scope.launch {
            callback.onLoading()
            
            val query = QueryToys(
                excludeUserIds = listOf(currentUserId),
                statuses = listOf("exchanging")
            )
            val body = RequestToysListBody(query, limit.toLong(), cursor)
            val request = RequestToysList(currentUserId, body)
            
            when (val result = client.getToysList(request)) {
                is ApiResult.Ok -> {
                    callback.onSuccess(result.response.toys, result.response.cursor)
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
     * Получить детальную информацию об игрушке
     */
    fun getToyDetails(
        scope: CoroutineScope,
        userId: String,
        toyId: String,
        callback: ToyDetailCallback
    ) {
        scope.launch {
            callback.onLoading()
            
            val request = RequestToyGet(toyId, userId)
            
            when (val result = client.getToy(request)) {
                is ApiResult.Ok -> {
                    callback.onSuccess(result.response.toy)
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
     * Создать новую игрушку
     * Idempotency token генерируется автоматически в ApiClient
     */
    fun createToy(
        scope: CoroutineScope,
        userId: String,
        name: String,
        description: String?,
        photoFile: File?,
        callback: ToyCallback
    ) {
        scope.launch {
            callback.onLoading()
            
            val toyBody = RequestToyPostBody(name, description)
            val request = RequestToyPost(userId, "", toyBody) // idempotencyToken будет заменен в ApiClient
            
            when (val result = client.createToy(request, photoFile)) {
                is ApiResult.Ok -> {
                    callback.onSuccess()
                }
                is ApiResult.Error -> {
                    callback.onError("Ошибка создания: ${result.error.message}")
                }
                is ApiResult.NetworkError -> {
                    callback.onError("Ошибка сети: ${result.throwable.message}")
                }
            }
        }
    }
    
    /**
     * Обновить игрушку
     */
    fun updateToy(
        scope: CoroutineScope,
        userId: String,
        toyId: String,
        name: String,
        description: String?,
        photoFile: File?,
        callback: ToyCallback
    ) {
        scope.launch {
            callback.onLoading()
            
            val toyBody = RequestToyPutBody(toyId, name, description)
            val request = RequestToyPut(userId, toyBody)
            
            when (val result = client.updateToy(request, photoFile)) {
                is ApiResult.Ok -> {
                    callback.onSuccess()
                }
                is ApiResult.Error -> {
                    callback.onError("Ошибка обновления: ${result.error.message}")
                }
                is ApiResult.NetworkError -> {
                    callback.onError("Ошибка сети: ${result.throwable.message}")
                }
            }
        }
    }
    
    /**
     * Удалить игрушку
     */
    fun deleteToy(
        scope: CoroutineScope,
        userId: String,
        toyId: String,
        callback: ToyCallback
    ) {
        scope.launch {
            callback.onLoading()
            
            val request = RequestToyDelete(toyId, userId)
            
            when (val result = client.deleteToy(request)) {
                is ApiResult.Ok -> {
                    callback.onSuccess()
                }
                is ApiResult.Error -> {
                    callback.onError("Ошибка удаления: ${result.error.message}")
                }
                is ApiResult.NetworkError -> {
                    callback.onError("Ошибка сети: ${result.throwable.message}")
                }
            }
        }
    }
    
    /**
     * Изменить статус игрушки (created <-> exchanging)
     */
    fun changeToyStatus(
        scope: CoroutineScope,
        userId: String,
        toyId: String,
        newStatus: String,
        callback: ToyCallback
    ) {
        scope.launch {
            callback.onLoading()
            
            val body = RequestToyPatchBody(newStatus)
            val request = RequestToyPatch(toyId, userId, body)
            
            when (val result = client.patchToyStatus(request)) {
                is ApiResult.Ok -> {
                    callback.onSuccess()
                }
                is ApiResult.Error -> {
                    callback.onError("Ошибка изменения статуса: ${result.error.message}")
                }
                is ApiResult.NetworkError -> {
                    callback.onError("Ошибка сети: ${result.throwable.message}")
                }
            }
        }
    }
    
    /**
     * Создать предложение обмена
     */
    fun createExchange(
        scope: CoroutineScope,
        currentUserId: String,
        myToyId: String,
        targetUserId: String,
        targetToyId: String,
        callback: ExchangeCallback
    ) {
        scope.launch {
            callback.onLoading()
            
            val userToy1 = UserIdToyId(userId = currentUserId, toyId = myToyId)
            val userToy2 = UserIdToyId(userId = targetUserId, toyId = targetToyId)
            val body = RequestExchangePostBody(userToy1 = userToy1, userToy2 = userToy2)
            val request = RequestExchangePost(
                userId = currentUserId,
                idempotencyToken = UUID.randomUUID().toString(),
                body = body
            )
            
            when (val result = client.createExchange(request)) {
                is ApiResult.Ok -> {
                    callback.onSuccess()
                }
                is ApiResult.Error -> {
                    callback.onError("Ошибка создания обмена: ${result.error.message}")
                }
                is ApiResult.NetworkError -> {
                    callback.onError("Ошибка сети: ${result.throwable.message}")
                }
            }
        }
    }
}

