package com.example.shizukuproxy

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlin.OptIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import rikka.shizuku.Shizuku
import java.net.URLEncoder

class MainActivity : ComponentActivity(), Shizuku.OnRequestPermissionResultListener {

    private val requestCode = 1001
    private var isShizukuAuthorized = mutableStateOf(false)
    private var isShizukuBinderAlive = mutableStateOf(false)

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        runOnUiThread {
            checkShizukuPermissionStatus()
        }
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        runOnUiThread {
            checkShizukuPermissionStatus()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Listen to Shizuku permission and binder events
        Shizuku.addRequestPermissionResultListener(this)
        Shizuku.addBinderReceivedListener(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        checkShizukuPermissionStatus()

        setContent {
            ShizukuProxyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ComposeColor(0xFF0F172A) // slate-900
                ) {
                    ProxyStudioScreen(
                        isShizukuAuthorized = isShizukuAuthorized.value,
                        isShizukuBinderAlive = isShizukuBinderAlive.value,
                        onRequestShizukuPermission = { requestShizukuPermission() }
                    )
                }
            }
        }
    }

    private fun checkShizukuPermissionStatus() {
        val binderAlive = Shizuku.pingBinder()
        isShizukuBinderAlive.value = binderAlive
        if (binderAlive) {
            val granted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            isShizukuAuthorized.value = granted
        } else {
            isShizukuAuthorized.value = false
        }
    }

    private fun requestShizukuPermission() {
        if (Shizuku.pingBinder()) {
            Shizuku.requestPermission(requestCode)
        } else {
            Toast.makeText(this, "Shizuku Engine is not active! Please start Shizuku app first.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        if (requestCode == this.requestCode) {
            isShizukuAuthorized.value = grantResult == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onResume() {
        super.onResume()
        checkShizukuPermissionStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(this)
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProxyStudioScreen(
    isShizukuAuthorized: Boolean,
    isShizukuBinderAlive: Boolean,
    onRequestShizukuPermission: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    var ipAddress by remember { mutableStateOf("192.168.43.1") }
    var portString by remember { mutableStateOf("12334") }
    var ruleCountry by remember { mutableStateOf("ru") }
    var finalOutbound by remember { mutableStateOf("proxy") }
    
    var isLoading by remember { mutableStateOf(false) }
    var adbFeedback by remember { mutableStateOf("Ready to resolve gateway...") }

    var connectedDevices by remember { mutableStateOf(emptyList<ConnectedDevice>()) }
    var isFetchingDevices by remember { mutableStateOf(false) }

    LaunchedEffect(isShizukuAuthorized) {
        if (isShizukuAuthorized) {
            ShizukuShellExecutor.getConnectedDevices { devices ->
                connectedDevices = devices
            }
        }
    }

    val scrollState = rememberScrollState()

    var jsonString by remember { mutableStateOf("") }
    var qrCodeBitmap by remember { mutableStateOf<Bitmap?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "ShizuNet Controls",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = ComposeColor.White
        )
        Text(
            text = "Translates SoftAP gateway IP configurations to modern sing-box config formats with absolute ease.",
            fontSize = 12.sp,
            color = ComposeColor.LightGray
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ComposeColor(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Shizuku Privilege Service",
                        fontWeight = FontWeight.Bold,
                        color = ComposeColor.White
                    )
                    
                    val statusText = when {
                        isShizukuAuthorized -> "Authorized"
                        isShizukuBinderAlive -> "Unauthorized"
                        else -> "Not Active"
                    }
                    val statusColor = when {
                        isShizukuAuthorized -> ComposeColor(0xFF10B981) // Emerald
                        isShizukuBinderAlive -> ComposeColor(0xFFF59E0B) // Amber/Orange
                        else -> ComposeColor(0xFFEF4444) // Red
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusColor)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ComposeColor.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (!isShizukuAuthorized) {
                    Button(
                        onClick = onRequestShizukuPermission,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ComposeColor(0xFF3B82F6))
                    ) {
                        Text(if (isShizukuBinderAlive) "Grant Shizuku Privilege Shell" else "Check / Connect Shizuku")
                    }
                } else {
                    Button(
                        onClick = {
                            isLoading = true
                            adbFeedback = "Executing 'ip r' via privileged bash shell..."
                            ShizukuShellExecutor.getHotspotGatewayIp { ip ->
                                isLoading = false
                                if (ip != null) {
                                    ipAddress = ip
                                    adbFeedback = "Success! SoftAP gateway resolved from routing tables: $ip"
                                    Toast.makeText(context, "Gateway resolved: $ip", Toast.LENGTH_SHORT).show()
                                } else {
                                    adbFeedback = "Command completed. Ensure hotspot (ap0/wlan1) is active on device."
                                    Toast.makeText(context, "Could not resolve hotspot gateway automatically.", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ComposeColor(0xFF10B981)),
                        enabled = !isLoading
                    ) {
                        Text(if (isLoading) "Running shell..." else "Auto-Detect Hotspot Gateway IP")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = adbFeedback,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = ComposeColor.Gray,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ComposeColor(0xFF0F172A))
                        .padding(8.dp)
                )
            }
        }

        Text(
            text = "Connected Hotspot Clients",
            fontWeight = FontWeight.Bold,
            color = ComposeColor.White,
            fontSize = 16.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ComposeColor(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Hotspot Neighbors",
                        fontWeight = FontWeight.Bold,
                        color = ComposeColor.White,
                        fontSize = 14.sp
                    )
                    
                    Button(
                        onClick = {
                            if (isShizukuAuthorized) {
                                isFetchingDevices = true
                                ShizukuShellExecutor.getConnectedDevices { devices ->
                                    connectedDevices = devices
                                    isFetchingDevices = false
                                }
                            } else {
                                Toast.makeText(context, "Authorize Shizuku first!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ComposeColor(0xFF3B82F6)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp),
                        enabled = !isFetchingDevices
                    ) {
                        Text(
                            text = if (isFetchingDevices) "Fetching..." else "Refresh List",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ComposeColor.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (connectedDevices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ComposeColor(0xFF0F172A), RoundedCornerShape(8.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No clients detected. Tap Refresh to scan routing tables.",
                            color = ComposeColor.Gray,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        connectedDevices.forEach { device ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, ComposeColor(0xFF334155), RoundedCornerShape(8.dp)),
                                colors = CardDefaults.cardColors(containerColor = ComposeColor(0xFF0F172A))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = device.ip,
                                                fontWeight = FontWeight.Bold,
                                                color = ComposeColor.White,
                                                fontSize = 14.sp
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(ComposeColor(0xFF1E3A8A))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = device.interfaceName,
                                                    fontSize = 9.sp,
                                                    color = ComposeColor(0xFF93C5FD),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "MAC: ${device.mac}",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = ComposeColor.Gray
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Vendor: ${device.manufacturer}",
                                            fontSize = 12.sp,
                                            color = ComposeColor(0xFF60A5FA),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    if (device.status.uppercase() == "REACHABLE") 
                                                        ComposeColor(0xFF065F46) 
                                                    else 
                                                        ComposeColor(0xFF374151)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = device.status.uppercase(),
                                                fontSize = 9.sp,
                                                color = if (device.status.uppercase() == "REACHABLE") 
                                                    ComposeColor(0xFF34D399) 
                                                else 
                                                    ComposeColor.LightGray,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {


                                            Button(
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(device.ip))
                                                    Toast.makeText(context, "Copied IP: ${device.ip}", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = ComposeColor(0xFF3B82F6)),
                                                modifier = Modifier.height(26.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text("COPY IP", fontSize = 8.sp, color = ComposeColor.White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = "Proxy Router Configurations",
            fontWeight = FontWeight.Bold,
            color = ComposeColor.White,
            fontSize = 16.sp
        )

        OutlinedTextField(
            value = ipAddress,
            onValueChange = { ipAddress = it },
            label = { Text("Active Gateway IP") },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = ComposeColor(0xFF3B82F6),
                unfocusedBorderColor = ComposeColor(0xFF475569)
            )
        )

        OutlinedTextField(
            value = portString,
            onValueChange = { portString = it },
            label = { Text("HTTP Proxy Port") },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = ComposeColor(0xFF3B82F6),
                unfocusedBorderColor = ComposeColor(0xFF475569)
            )
        )

        Button(
            onClick = {
                val port = portString.toIntOrNull() ?: 12334
                val generatedJson = ProfileGenerator.generateJson(
                    ip = ipAddress,
                    port = port,
                    geoip = ruleCountry,
                    finalOutbound = finalOutbound
                )
                jsonString = generatedJson
                try {
                    qrCodeBitmap = generateQrCode(generatedJson, 512)
                    Toast.makeText(context, "Hiddify JSON Profile & QR generated successfully!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    qrCodeBitmap = null
                    Toast.makeText(context, "Error generating QR payload", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = ComposeColor(0xFF10B981))
        ) {
            Text("Generate Hiddify Profile & QR", fontWeight = FontWeight.Bold)
        }

        Text(
            text = "Hiddify Profile QR Code",
            color = ComposeColor.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ComposeColor(0xFF1E293B))
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (qrCodeBitmap == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .background(ComposeColor(0xFF0F172A), RoundedCornerShape(8.dp))
                            .border(1.dp, ComposeColor(0xFF334155), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No profile generated.\nConfigure options and click 'Generate Hiddify Profile & QR' above.",
                            color = ComposeColor.Gray,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    Image(
                        bitmap = qrCodeBitmap!!.asImageBitmap(),
                        contentDescription = "Hiddify Config QR",
                        modifier = Modifier
                            .size(240.dp)
                            .background(ComposeColor.White)
                            .border(1.dp, ComposeColor.White)
                            .padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(jsonString))
                            Toast.makeText(context, "Hiddify Profile JSON Copied!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ComposeColor(0xFF3B82F6)),
                        enabled = jsonString.isNotEmpty()
                    ) {
                        Text("Copy Profile JSON")
                    }
                }
            }
        }
    }
}

fun generateQrCode(text: String, size: Int): Bitmap? {
    return try {
        val bitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}

@Composable
fun ShizukuProxyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = ComposeColor(0xFF3B82F6),
            background = ComposeColor(0xFF0F172A),
            surface = ComposeColor(0xFF1E293B)
        ),
        content = content
    )
}
