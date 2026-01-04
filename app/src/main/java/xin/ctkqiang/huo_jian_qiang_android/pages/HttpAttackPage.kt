package xin.ctkqiang.huo_jian_qiang_android.pages

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xin.ctkqiang.huo_jian_qiang_android.R
import xin.ctkqiang.huo_jian_qiang_android.model.HTTPRequest
import xin.ctkqiang.huo_jian_qiang_android.ui.theme.Black
import xin.ctkqiang.huo_jian_qiang_android.ui.theme.DarkRed
import xin.ctkqiang.huo_jian_qiang_android.ui.theme.Gray
import xin.ctkqiang.huo_jian_qiang_android.ui.theme.Red
import xin.ctkqiang.huo_jian_qiang_android.ui.theme.White

class HttpAttackPage {

    companion object {

        @OptIn(ExperimentalMaterial3Api::class)
        @Composable
        fun HttpAttackForm(sectionName: String) {
            var currentMethod by remember { mutableStateOf(HTTPRequest.POST) }

            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val snackbarHostState = remember { SnackbarHostState() }

            var hostText by remember { mutableStateOf("http://") }
            var parameter by remember { mutableStateOf("username={{user}}&password={{pass}}") }

            Log.d(R.string.app_name.toString(), "当前文本: $hostText")
            Log.d(R.string.app_name.toString(), "当前参数: $parameter")
            Log.d(R.string.app_name.toString(), "当前请求方式: ${currentMethod.name}")

            Column (
                modifier = Modifier.padding(16.dp)
            ) {
                TextField(
                    value = hostText,
                    onValueChange = { hostText = it },
                    label = { Text("输入URL") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = getTextFieldColor()
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = parameter,
                    onValueChange = { parameter = it },
                    label = { Text("附加用户输入") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = getTextFieldColor()
                )

                Spacer(modifier = Modifier.height(16.dp))

                HTTPRequestDropdownOption(
                    selectedMethod = currentMethod,
                    onMethodChange = { newItem ->
                        currentMethod = newItem
                        println("用户选择了: ${newItem.name}")
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        @OptIn(ExperimentalMaterial3Api::class)
        @Composable
        fun HTTPRequestDropdownOption(selectedMethod: HTTPRequest, onMethodChange: (HTTPRequest) -> Unit) {
            var expanded by remember { mutableStateOf(false) }

            Column {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        value = selectedMethod.name,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        colors = getTextFieldColor(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        HTTPRequest.entries.forEach { method ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = method.name,
                                        fontWeight = FontWeight.Bold,
                                        color = when (method) {
                                            HTTPRequest.GET -> Color(0xFF4CAF50)
                                            HTTPRequest.POST -> Color(0xFFFFC107)
                                            HTTPRequest.DELETE -> Color(0xFFF44336)
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                },
                                onClick = {
                                    onMethodChange(method)
                                    expanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
            }
        }
        
        @Composable
        fun getTextFieldColor() : TextFieldColors {
            return TextFieldDefaults.colors(
                unfocusedContainerColor = Gray,
                focusedTextColor = DarkRed,
                unfocusedTextColor = DarkRed,
                focusedContainerColor = White,
                focusedIndicatorColor = Red,
                unfocusedIndicatorColor = Black,
            )
        }
    }
}