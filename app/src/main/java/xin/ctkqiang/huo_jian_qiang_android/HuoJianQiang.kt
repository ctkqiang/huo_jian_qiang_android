package xin.ctkqiang.huo_jian_qiang_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import xin.ctkqiang.huo_jian_qiang_android.pages.HttpAttackPage
import xin.ctkqiang.huo_jian_qiang_android.pages.MySQLAttackPage
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
fun MainPreview() {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("HTTP攻击", "MYSQL攻击")

    StatusBar.SetStatusBarColor(darkIcons = true)

    Huo_jian_qiang_androidTheme {
        Scaffold (
            contentColor = White,
            containerColor = White,
            topBar = {
                Column {
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        contentColor = Red,
                        containerColor = White,
                        divider = {
                            HorizontalDivider()
                        },
                        indicator = { tabPositions ->
                            if (selectedTabIndex < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                    color = Red
                                )
                            }
                        }
                    ) {
                        tabTitles.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = { Text(text = title) },
                                selectedContentColor = Red,
                                icon = { }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                when (selectedTabIndex) {
                    0 -> HttpAttackPage.HttpAttackForm("HTTP Engine")
                    1 -> MySQLAttackPage.MySQLAttackForm("MYSQL Engine")
                }
            }
        }
    }
}