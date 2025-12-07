package course.exchange_toy.models.remote.models
import kotlinx.serialization.*

@Serializable
data class UserName(
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    @SerialName("middle_name") val middleName: String? = null
)

@Serializable
data class RequestRegisterBody(
    @SerialName("user_name") val userName: UserName, 
    val password: String, 
    @SerialName("confirm_password") val confirmPassword: String, 
    val email: String
)

@Serializable 
data class RequestRegister(val body: RequestRegisterBody)

@Serializable 
data class ResponseRegister(@SerialName("user_id") val userId: String)

@Serializable 
data class RequestLoginBody(val password: String, val email: String)

@Serializable 
data class RequestLogin(val body: RequestLoginBody)

@Serializable 
data class ResponseLogin(@SerialName("user_id") val userId: String)
