package course.exchange_toy.models.remote.models
import kotlinx.serialization.*

@Serializable
data class ResponseError(val code: String, val message: String)
