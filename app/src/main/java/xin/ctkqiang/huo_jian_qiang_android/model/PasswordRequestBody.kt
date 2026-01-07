package xin.ctkqiang.huo_jian_qiang_android.model

import kotlinx.serialization.Serializable

@Serializable
data class PasswordRequestBody(
    val password: String
)

fun PasswordRequestBody.toJsonString(): String {
    return "{\"password\": \"${password.replace("\"", "\\\"").replace("\\", "\\\\")}\"}"
}

fun String.toPasswordRequestBody(): PasswordRequestBody {
    return PasswordRequestBody(this)
}