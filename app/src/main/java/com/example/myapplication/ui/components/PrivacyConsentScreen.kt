package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 首次启动的隐私合规页。
 * 高德 SDK 要求在初始化前向用户告知并取得同意，这里是最小可用的实现：
 * 说明收集哪些信息、用于什么目的、由谁处理，并给出高德隐私政策的入口。
 * 正式上架前需要替换成完整的《隐私政策》并托管一个可访问的 URL。
 */
@Composable
fun PrivacyConsentScreen(
    onAgree: () -> Unit,
    onExit: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 48.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text("欢迎使用「我们的记录」", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "为了在记录里带上「拍摄地点」，本 App 会使用高德地图/定位 SDK 获取你的设备位置" +
                    "（GPS、Wi‑Fi、基站信息），并把坐标换算成中文地址。位置信息只会保存在这台手机本地，" +
                    "不会上传到我们的服务器（本 App 目前没有服务器）。\n\n" +
                    "高德 SDK 还会收集设备标识等必要信息用于服务可用性统计，详见《高德隐私政策》。" +
                    "你可以随时在系统设置里关闭定位权限，此时仍可手动选择地点或只记录文字。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onAgree, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("同意并继续")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
                Text("不同意，退出 App")
            }
        }
    }
}
