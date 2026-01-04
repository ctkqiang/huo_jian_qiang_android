package xin.ctkqiang.huo_jian_qiang_android.ui.theme

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

class StatusBar {
    companion object {
        @Composable
        fun SetStatusBarColor(darkIcons: Boolean) {
            val view = LocalView.current
            val context = LocalContext.current

            if (!view.isInEditMode) {
                SideEffect {
                    val window = (context as? Activity)?.window ?: return@SideEffect
                    window.statusBarColor = android.graphics.Color.WHITE

                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkIcons
                }
            }
        }
    }
}