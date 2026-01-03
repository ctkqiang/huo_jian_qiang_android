package xin.ctkqiang.huo_jian_qiang_android.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

class HttpAttackPage {

    companion object {

        @OptIn(ExperimentalMaterial3Api::class)
        @Composable
        fun HttpAttackForm(sectionName: String) {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val snackbarHostState = remember { SnackbarHostState() }

            var hostText by remember { mutableStateOf("http://") }

            Column (
                modifier = Modifier.padding(16.dp)
            ) {
                Text("主机地址 [Host]")
                TextField(
                    value = hostText,
                    onValueChange = { hostText = it },
                    label = { Text("输入URL") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

        }
    }
}