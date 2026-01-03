package xin.ctkqiang.huo_jian_qiang_android.pages

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext

class MySQLAttackPage {

    companion object {

        @OptIn(ExperimentalMaterial3Api::class)
        @Composable
        fun MySQLAttackForm(sectionName: String) {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val snackbarHostState = remember { SnackbarHostState() }

        }
    }
}