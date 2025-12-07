package course.exchange_toy.models.remote

import course.exchange_toy.models.remote.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

class ExchangeToysClient(baseUrl: String) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }
    private val host = baseUrl
    
    companion object {
        private const val MAX_RETRIES = 3 // Количество попыток для idempotent запросов
    }
    
    /**
     * Генерация idempotency token (UUID)
     */
    private fun generateIdempotencyToken(): String = UUID.randomUUID().toString()
    
    /**
     * Retry обертка для запросов с idempotency token
     * Использует ОДИН И ТОТ ЖЕ токен для всех retry попыток
     * Новый токен создается только при следующем вызове функции
     */
    private suspend fun <T : Any> retryWithIdempotencyToken(
        block: suspend (token: String) -> ApiResult<T>
    ): ApiResult<T> {
        val token = generateIdempotencyToken() // Один токен для всех retry
        var lastError: ApiResult<T>? = null
        
        repeat(MAX_RETRIES) { attempt ->
            val result = block(token) // Используем ОДИНАКОВЫЙ токен
            
            when (result) {
                is ApiResult.Ok -> return result // Успех - возвращаем
                is ApiResult.Error -> {
                    // Ошибка от сервера - больше не пробуем (может быть логическая ошибка)
                    return result
                }
                is ApiResult.NetworkError -> {
                    lastError = result
                    if (attempt == MAX_RETRIES - 1) {
                        return result // Последняя попытка - возвращаем ошибку
                    }
                    // Иначе пробуем снова с ТЕМ ЖЕ токеном
                }
            }
        }
        
        return lastError ?: ApiResult.NetworkError(Exception("Unknown error"))
    }

    // Auth
    suspend fun register(req: RequestRegister): ApiResult<ResponseRegister> = safeApiCall<ResponseRegister> {
        client.post("$host/v1/register") {
            contentType(ContentType.Application.Json)
            setBody(req.body)
        }
    }
    
    suspend fun login(req: RequestLogin): ApiResult<ResponseLogin> = safeApiCall<ResponseLogin> {
        client.post("$host/v1/login") {
            contentType(ContentType.Application.Json)
            setBody(req.body)
        }
    }

    // Toys
    // POST /v1/toys/ - FormData: name, description (optional), file (optional) + headers: x_user_id, x_idempotency_token
    suspend fun createToy(req: RequestToyPost, file: File? = null): ApiResult<ReponseToyPost> = retryWithIdempotencyToken { token ->
        safeApiCall<ReponseToyPost> {
            client.submitFormWithBinaryData(
                url = "$host/v1/toys/",
                formData = formData {
                    append("name", req.toy.name)
                    req.toy.description?.let { append("description", it) }
                    file?.let {
                        append("file", it.readBytes(), Headers.build {
                            append(HttpHeaders.ContentDisposition, "filename=${it.name}")
                        })
                    }
                }
            ) {
                header("x_user_id", req.userId)
                header("x_idempotency_token", token) // Автоматически генерируемый токен (req.idempotencyToken игнорируется)
            }
        }
    }
    
    // PUT /v1/toys/ - FormData: name, description (optional), file (optional) + headers: x_user_id, toy_id
    suspend fun updateToy(req: RequestToyPut, file: File? = null): ApiResult<ResponseToyPut> = safeApiCall<ResponseToyPut> {
        client.submitFormWithBinaryData(
            url = "$host/v1/toys/change",
            formData = formData {
                append("name", req.toy.name)
                req.toy.description?.let { append("description", it) }
                file?.let {
                    append("file", it.readBytes(), Headers.build {
                        append(HttpHeaders.ContentDisposition, "filename=${it.name}")
                    })
                }
            }
        ) {
            header("x_user_id", req.userId)
            header("toy_id", req.toy.toyId)
        }
    }
    
    // GET /v1/toys/:toy_id + header: x_user_id
    suspend fun getToy(req: RequestToyGet): ApiResult<ResponseToyGet> = safeApiCall<ResponseToyGet> {
        client.get("$host/v1/toys/${req.toyId}") {
            header("x_user_id", req.userId)
        }
    }
    
    // DELETE /v1/toys/:toy_id + header: x_user_id
    suspend fun deleteToy(req: RequestToyDelete): ApiResult<Unit> = safeApiCall<Unit> {
        client.delete("$host/v1/toys/${req.toyId}") {
            header("x_user_id", req.userId)
        }
    }
    
    // PATCH /v1/toys/:toy_id + JSON body + header: x_user_id
    suspend fun patchToyStatus(req: RequestToyPatch): ApiResult<ReponseToyPatch> = safeApiCall<ReponseToyPatch> {
        client.patch("$host/v1/toys/${req.toyId}") {
            header("x_user_id", req.userId)
            contentType(ContentType.Application.Json)
            setBody(req.body)
        }
    }
    
    // POST /v1/toys/list + JSON body + header: x_user_id
    suspend fun getToysList(req: RequestToysList): ApiResult<ResponseToysList> = safeApiCall<ResponseToysList> {
        client.post("$host/v1/toys/list") {
            header("x_user_id", req.userId)
            contentType(ContentType.Application.Json)
            setBody(req.body)
        }
    }

    // Exchanges
    // POST /v1/exchange/ + JSON body + headers: x_user_id, x_idempotency_token
    suspend fun createExchange(req: RequestExchangePost): ApiResult<ResponseExchangePost> = retryWithIdempotencyToken { token ->
        safeApiCall<ResponseExchangePost> {
            client.post("$host/v1/exchange/") {
                header("x_user_id", req.userId)
                header("x_idempotency_token", token) // Автоматически генерируемый токен (req.idempotencyToken игнорируется)
                contentType(ContentType.Application.Json)
                setBody(req.body)
            }
        }
    }
    
    // GET /v1/exchange/:exchange_id + header: x_user_id
    suspend fun getExchange(req: RequestExchangeGet): ApiResult<ResponseExchangeGet> = safeApiCall<ResponseExchangeGet> {
        client.get("$host/v1/exchange/${req.exchangeId}") {
            header("x_user_id", req.userId)
        }
    }
    
    // PATCH /v1/exchange/:exchange_id + JSON body + header: x_user_id
    suspend fun patchExchange(req: RequestExchangePatch): ApiResult<ResponseExchangePatch> = safeApiCall<ResponseExchangePatch> {
        client.patch("$host/v1/exchange/${req.exchangeId}") {
            header("x_user_id", req.userId)
            contentType(ContentType.Application.Json)
            setBody(req.body)
        }
    }
    
    // POST /v1/exchange/list + JSON body + header: x_user_id
    suspend fun getExchangeList(req: RequestExchangeList): ApiResult<ResponseExchangeList> = safeApiCall<ResponseExchangeList> {
        client.post("$host/v1/exchange/list") {
            header("x_user_id", req.userId)
            contentType(ContentType.Application.Json)
            setBody(req.body)
        }
    }

    // ---- Получение статичного фото ----
    // GET /upload - <photo_url> - получение изображения игрушки
    suspend fun getPhoto(url: String): ApiResult<ByteArray> = safeApiCall<ByteArray> {
        // Заменяем все до /upload на baseUrl
        val photoUrl = if (url.contains("/upload")) {
            val pathFromUpload = url.substringAfter("/upload")
            "$host/upload$pathFromUpload"
        } else {
            url
        }
        client.get(photoUrl)
    }
}
