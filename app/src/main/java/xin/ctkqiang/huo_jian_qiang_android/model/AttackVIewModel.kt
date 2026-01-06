package xin.ctkqiang.huo_jian_qiang_android.model

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel

class AttackViewModel : ViewModel() {
    val logs = mutableStateListOf<String>()

    fun addLog(message: String) {
        logs.add(message)
    }
}