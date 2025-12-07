package course.exchange_toy.models.remote

import course.exchange_toy.models.remote.models.ResponseError
import io.ktor.client.call.*
import io.ktor.client.statement.*
import kotlinx.serialization.SerializationException

sealed class ApiResult<out T> {
    data class Ok<T>(val response: T) : ApiResult<T>()
    data class Error(val error: ResponseError) : ApiResult<Nothing>()
    data class NetworkError(val throwable: Throwable) : ApiResult<Nothing>()
}

suspend inline fun <reified T:Any> safeApiCall(block: () -> HttpResponse): ApiResult<T> {
    return try {
        val response = block()
        if (response.status.value in 200..299) {
            ApiResult.Ok(response.body())
        } else {
            val error = try {
                response.body<ResponseError>()
            } catch (e: SerializationException) {
                ResponseError(code = response.status.value.toString(), message = response.bodyAsText())
            }
            ApiResult.Error(error)
        }
    } catch (e: Exception) {
        ApiResult.NetworkError(e)
    }
}
