package xin.ctkqiang.huo_jian_qiang_android.model

import kotlinx.serialization.Serializable

@Suppress("PLUGIN_IS_NOT_ENABLED")
@Serializable
data class RequestHeader(
    val key: String,
    val value: String
)
