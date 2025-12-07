package course.exchange_toy.models.remote.models
import kotlinx.serialization.*

@Serializable
enum class ToyStatus { 
    @SerialName("created") CREATED, 
    @SerialName("removed") REMOVED, 
    @SerialName("exchanging") EXCHANGING,
    @SerialName("exchanged") EXCHANGED
}

@Serializable
data class Toy(
    @SerialName("toy_id") val toyId: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    @SerialName("idempotency_token") val idempotencyToken: String,
    val description: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    val status: ToyStatus,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class ToyInfo(
    @SerialName("toy_id") val toyId: String, 
    @SerialName("user_id") val userId: String, 
    val name: String, 
    val description: String? = null, 
    @SerialName("photo_url") val photoUrl: String? = null
) {
    // Alias для совместимости
    val id: String get() = toyId
}

// Request models (для FormData используются обычные классы, не @Serializable)
data class RequestToyPostBody(val name: String, val description: String? = null)
data class RequestToyPost(
    val userId: String, 
    val idempotencyToken: String, 
    val toy: RequestToyPostBody
)

data class RequestToyPutBody(val toyId: String, val name: String, val description: String? = null)
data class RequestToyPut(val userId: String, val toy: RequestToyPutBody)

data class RequestToyGet(val toyId: String, val userId: String)
data class RequestToyDelete(val toyId: String, val userId: String)

@Serializable 
data class RequestToyPatchBody(val status: String) // На сервере это строка "created" или "exchanging"

data class RequestToyPatch(val toyId: String, val userId: String, val body: RequestToyPatchBody)

@Serializable
data class QueryToys(
    val statuses: List<String>? = null, // На сервере это массив строк
    @SerialName("user_ids") val userIds: List<String>? = null,
    @SerialName("exclude_user_ids") val excludeUserIds: List<String>? = null
)

@Serializable 
data class RequestToysListBody(
    val query: QueryToys, 
    val limit: Long? = null, 
    val cursor: String? = null
)

data class RequestToysList(val userId: String, val body: RequestToysListBody)

// Response models
@Serializable data class ReponseToyPost(val toy: Toy)
@Serializable data class ResponseToyPut(val toy: Toy)
@Serializable data class ResponseToyGet(val toy: Toy)
@Serializable data class ReponseToyPatch(val toy: Toy)
@Serializable data class ResponseToysList(val toys: List<Toy>, val cursor: String? = null)
