package xin.ctkqiang.huo_jian_qiang_android.pages

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import xin.ctkqiang.huo_jian_qiang_android.R
import xin.ctkqiang.huo_jian_qiang_android.controller.HttpRequestController
import xin.ctkqiang.huo_jian_qiang_android.model.HTTPRequest
import xin.ctkqiang.huo_jian_qiang_android.model.RequestBody
import xin.ctkqiang.huo_jian_qiang_android.ui.theme.*

class HttpAttackPage {

    companion object {

        /**
         * HTTP攻击表单组件
         *
         * @param sectionName 表单标题
         */
        @OptIn(ExperimentalMaterial3Api::class)
        @Composable
        fun HttpAttackForm(sectionName: String) {
            var currentMethod by remember { mutableStateOf(HTTPRequest.POST) }
            var hostText by remember { mutableStateOf("http://") }
            var parameter by remember { mutableStateOf("username={{user}}&password={{pass}}") }
            var responseLogs by remember { mutableStateOf<List<String>>(emptyList()) }
            var isLoading by remember { mutableStateOf(false) }

            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val snackbarHostState = remember { SnackbarHostState() }

            Log.d(R.string.app_name.toString(), "当前文本: $hostText")
            Log.d(R.string.app_name.toString(), "当前参数: $parameter")
            Log.d(R.string.app_name.toString(), "当前请求方式: ${currentMethod.name}")

            Column(modifier = Modifier.padding(16.dp)) {
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
                    label = { Text("请求参数") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = getTextFieldColor()
                )

                Spacer(modifier = Modifier.height(16.dp))

                HTTPRequestDropdownOption(
                    selectedMethod = currentMethod,
                    onMethodChange = { newItem ->
                        currentMethod = newItem
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (hostText.isBlank()) {
                             scope.launch {
                                 snackbarHostState.showSnackbar("请输入URL")
                             }
                             return@Button
                         }

                        if (parameter.isBlank()) {
                            scope.launch {
                                snackbarHostState.showSnackbar("请输入请求参数")
                            }
                            return@Button
                        }

                        isLoading = true
                        scope.launch {
                            try {
                                val requestBody = parseParameters(parameter)
                                val result = when (currentMethod) {
                                    HTTPRequest.GET -> HttpRequestController.getRequest(hostText, requestBody)
                                    HTTPRequest.POST -> HttpRequestController.postRequest(hostText, requestBody)
                                    else -> "暂不支持 ${currentMethod.name} 请求方法"
                                }
                                
                                responseLogs = responseLogs + listOf(
                                    "=== 请求信息 ===",
                                    "URL: $hostText",
                                    "方法: ${currentMethod.name}",
                                    "参数: $parameter",
                                    "=== 响应结果 ===",
                                    result,
                                    "=== 请求结束 ===\n"
                                )
                            } catch (e: Exception) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("请求失败: ${e.message}")
                                }
                                responseLogs = responseLogs + listOf("错误: ${e.message}")
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("发送中...")
                    } else {
                        Text("发送请求")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (responseLogs.isNotEmpty()) {
                    OutputView.OutputView(logs = responseLogs)
                } else {
                    OutputView.OutputView(logs = listOf("等待请求..."))
                }
            }

            SnackbarHost(hostState = snackbarHostState)
        }

        /**
         * 解析参数字符串为RequestBody对象
         *
         * @param parameterString 参数字符串，格式为"key1=value1&key2=value2"
         * @return 解析后的RequestBody对象
         * @throws IllegalArgumentException 当参数不包含username和password字段时
         */
        private fun parseParameters(parameterString: String): RequestBody {
            val params = parameterString.split("&").associate { param ->
                val keyValue = param.split("=")
                if (keyValue.size == 2) {
                    keyValue[0] to keyValue[1]
                } else {
                    keyValue[0] to ""
                }
            }
            
            val username = params["username"] ?: ""
            val password = params["password"] ?: ""
            
            if (username.isBlank() || password.isBlank()) {
                throw IllegalArgumentException("参数必须包含 username 和 password 字段")
            }
            
            return RequestBody(username = username, password = password)
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