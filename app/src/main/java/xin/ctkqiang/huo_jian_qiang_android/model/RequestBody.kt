package xin.ctkqiang.huo_jian_qiang_android.model

import kotlinx.serialization.Serializable

@Suppress("PLUGIN_IS_NOT_ENABLED")
@Serializable
data class RequestBody(
    val username :String,
    val password :String
)
