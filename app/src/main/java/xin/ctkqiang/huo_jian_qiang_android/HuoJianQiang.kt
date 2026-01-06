package xin.ctkqiang.huo_jian_qiang_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import xin.ctkqiang.huo_jian_qiang_android.controller.Downloader
import xin.ctkqiang.huo_jian_qiang_android.model.AttackViewModel
import xin.ctkqiang.huo_jian_qiang_android.pages.HttpAttackPage
import xin.ctkqiang.huo_jian_qiang_android.pages.MySQLAttackPage
import xin.ctkqiang.huo_jian_qiang_android.pages.OutputView
import xin.ctkqiang.huo_jian_qiang_android.ui.theme.Black
import xin.ctkqiang.huo_jian_qiang_android.ui.theme.Huo_jian_qiang_androidTheme
import xin.ctkqiang.huo_jian_qiang_android.ui.theme.Pink
import xin.ctkqiang.huo_jian_qiang_android.ui.theme.Red
import xin.ctkqiang.huo_jian_qiang_android.ui.theme.StatusBar
import xin.ctkqiang.huo_jian_qiang_android.ui.theme.White

class HuoJianQiang : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            Huo_jian_qiang_androidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        MainPreview()
                    }
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun MainPreview(viewModel: AttackViewModel = viewModel()) {
    val context = LocalContext.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("HTTP攻击", "MYSQL攻击")

    val url : String = stringResource(R.string.url_rock_you)

    LaunchedEffect(Unit) {
        if (Downloader.isFileExists(context)) {
            viewModel.addLog("字典文件已存在，跳过下载。")
        } else {
            viewModel.addLog("检测到字典缺失，开始自动下载...")
            val success = Downloader.downloadRockYou(context, url)
            if (success) {
                viewModel.addLog("字典自动下载成功！")
            } else {
                viewModel.addLog("字典下载失败，请在设置中手动重试。")
            }
        }
    }

    StatusBar.SetStatusBarColor(darkIcons = true)

    Huo_jian_qiang_androidTheme {
        Scaffold (
            contentColor = White,
            containerColor = White,
            bottomBar = {
                NavigationBar(
                    containerColor = White,
                    contentColor = Red,
                    tonalElevation = 8.dp
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        NavigationBarItem(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            label = { Text(text = title) },
                            icon = {
                                Icon(
                                    imageVector = if (index == 0) Icons.AutoMirrored.Filled.List else Icons.Default.Build,
                                    contentDescription = title
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Black,
                                selectedTextColor = Black,
                                indicatorColor = Pink.copy(alpha = 0.2f),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                // 页面内容
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTabIndex) {
                        0 -> HttpAttackPage.HttpAttackForm(viewModel.toString())
                        1 -> MySQLAttackPage.MySQLAttackForm(viewModel.toString())
                    }
                }

                // 日志输出区域 (建议固定在底部或占一定比例)
                OutputView()
            }
        }

    }
}