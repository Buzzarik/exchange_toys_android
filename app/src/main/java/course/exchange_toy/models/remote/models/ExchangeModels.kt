package course.exchange_toy.models.remote.models
import kotlinx.serialization.*

@Serializable
enum class ExchangeStatus { 
    @SerialName("created") CREATED, 
    @SerialName("confirm") CONFIRM, 
    @SerialName("success") SUCCESS, 
    @SerialName("failed") FAILED 
}

@Serializable
enum class ExchangeDetailsStatus {
    @SerialName("created") CREATED, 
    @SerialName("confirm_1") CONFIRM_1,
    @SerialName("confirm_2") CONFIRM_2, 
    @SerialName("success") SUCCESS, 
    @SerialName("failed") FAILED
}

// Request models
@Serializable
data class UserIdToyId(
    @SerialName("user_id") val userId: String, 
    @SerialName("toy_id") val toyId: String
)

@Serializable 
data class RequestExchangePostBody(
    @SerialName("user_toy_1") val userToy1: UserIdToyId, 
    @SerialName("user_toy_2") val userToy2: UserIdToyId
)

data class RequestExchangePost(
    val userId: String, 
    val idempotencyToken: String, 
    val body: RequestExchangePostBody
)

data class RequestExchangeGet(val userId: String, val exchangeId: String)

@Serializable 
data class RequestExchangePatchBody(
    val status: String // На сервере валидация: "confirm_1", "confirm_2", "failed"
)

data class RequestExchangePatch(
    val userId: String, 
    val exchangeId: String, 
    val body: RequestExchangePatchBody
)

@Serializable
data class QueryExchanges(
    val statuses: List<String>? = null // На сервере это массив строк
)

@Serializable 
data class RequestExchangeListBody(
    val query: QueryExchanges, 
    val limit: Long? = null, 
    val cursor: String? = null
)

data class RequestExchangeList(val userId: String, val body: RequestExchangeListBody)

// Response models
@Serializable
data class Exchange(
    @SerialName("exchange_id") val exchangeId: String,
    @SerialName("src_toy_id") val srcToyId: String,
    @SerialName("dst_toy_id") val dstToyId: String,
    @SerialName("idempotency_token") val idempotencyToken: String,
    val status: ExchangeStatus, 
    @SerialName("created_at") val createdAt: String, 
    @SerialName("updated_at") val updatedAt: String
)

@Serializable 
data class ExchangeDetailsInfo(
    val toy: ToyInfo, 
    val user: UserName, 
    val status: ExchangeDetailsStatus
)

@Serializable
data class ExchangeInfo(
    @SerialName("exchange_id") val exchangeId: String,
    @SerialName("exchange_details") val details: List<ExchangeDetailsInfo>,
    @SerialName("idempotency_token") val idempotencyToken: String,
    val status: ExchangeStatus,
    @SerialName("created_at") val createdAt: String, 
    @SerialName("updated_at") val updatedAt: String
)

@Serializable data class ResponseExchangePost(val exchange: Exchange)
@Serializable data class ResponseExchangeGet(val exchange: ExchangeInfo)
@Serializable data class ResponseExchangePatch(val exchange: ExchangeInfo)
@Serializable data class ResponseExchangeList(val exchanges: List<ExchangeInfo>, val cursor: String? = null)
