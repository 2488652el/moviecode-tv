package com.moviecode.tv.data.repository

import android.content.Context
import android SSDPDiscover
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import org.json.JSONObject
import java.io.StringReader
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import javax.inject.Singleton
import javax.xml.parsers.DocumentBuilderFactory

/**
 * DLNA 设备信息
 */
data class DLNADevice(
    val id: String,
    val name: String,
    val type: String, // tv, speaker, renderer
    val manufacturer: String,
    val model: String,
    val controlUrl: String,
    val presentationUrl: String?,
    val services: List<String>
)

/**
 * 投屏状态
 */
data class CastState(
    val deviceId: String? = null,
    val deviceName: String? = null,
    val isCasting: Boolean = false,
    val mediaTitle: String? = null,
    val mediaUrl: String? = null,
    val isPlaying: Boolean = false,
    val position: Long = 0,
    val duration: Long = 0
)

/**
 * 投屏配置
 */
data class CastConfig(
    val title: String,
    val url: String,
    val thumbnail: String? = null,
    val mimeType: String = "video/*",
    val duration: Long = 0
)

/**
 * DLNA 投屏仓库
 */
@Singleton
class DLNARepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val _devices = MutableStateFlow<List<DLNADevice>>(emptyList())
    val devices: StateFlow<List<DLNADevice>> = _devices.asStateFlow()

    private val _castState = MutableStateFlow(CastState())
    val castState: StateFlow<CastState> = _castState.asStateFlow()

    private var currentDevice: DLNADevice? = null
    private var castJob: Job? = null

    companion object {
        private const val TAG = "DLNARepo"
        private const val SSDP_MULTICAST = "239.255.255.250"
        private const val SSDP_PORT = 1900
        private const val SEARCH_REQUEST = 
            "M-SEARCH * HTTP/1.1\r\n" +
            "HOST: 239.255.255.250:1900\r\n" +
            "MAN: \"ssdp:discover\"\r\n" +
            "MX: 3\r\n" +
            "ST: urn:schemas-upnp-org:device:MediaRenderer:1\r\n\r\n"
    }

    /**
     * 发现局域网内的 DLNA 设备
     */
    suspend fun discoverDevices(): List<DLNADevice> = withContext(Dispatchers.IO) {
        val foundDevices = mutableListOf<DLNADevice>()
        
        try {
            // 使用 Android 的 SSDP 发现
            // 注意：实际需要使用 Native 代码或第三方库
            // 这里模拟一些常见的设备
            simulateDevices().forEach { device ->
                addDevice(device)
                foundDevices.add(device)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Discovery failed", e)
        }
        
        _devices.value = foundDevices
        foundDevices
    }

    /**
     * 模拟设备发现（实际需要 SSDP 协议）
     */
    private fun simulateDevices(): List<DLNADevice> {
        // 实际应用中，这里会通过 UDP Socket 发送 SSDP M-SEARCH
        // 并解析 HTTP NOTIFY 响应
        return listOf(
            DLNADevice(
                id = "smart-tv-001",
                name = "智能电视",
                type = "tv",
                manufacturer = "Samsung",
                model = "Smart TV",
                controlUrl = "http://192.168.1.100:9197/upnp/control/AVTransport",
                presentationUrl = "http://192.168.1.100:9197/",
                services = listOf("AVTransport", "RenderingControl")
            ),
            DLNADevice(
                id = "chromecast-001",
                name = "Chromecast",
                type = "tv",
                manufacturer = "Google",
                model = "Chromecast",
                controlUrl = "http://192.168.1.101:8008/ssdp/notfound",
                presentationUrl = null,
                services = listOf("AVTransport")
            )
        )
    }

    /**
     * 添加设备
     */
    private fun addDevice(device: DLNADevice) {
        val current = _devices.value.toMutableList()
        current.removeAll { it.id == device.id }
        current.add(device)
        _devices.value = current
    }

    /**
     * 获取设备
     */
    fun getDevice(id: String): DLNADevice? {
        return _devices.value.find { it.id == id }
    }

    /**
     * 投送媒体
     */
    suspend fun cast(deviceId: String, config: CastConfig): Boolean = withContext(Dispatchers.IO) {
        val device = getDevice(deviceId) ?: return@withContext false
        currentDevice = device

        try {
            // 1. 设置媒体 URI
            setAVTransportURI(device, config)
            
            // 2. 开始播放
            delay(500)
            play(device)
            
            // 3. 更新状态
            _castState.value = CastState(
                deviceId = deviceId,
                deviceName = device.name,
                isCasting = true,
                mediaTitle = config.title,
                mediaUrl = config.url,
                isPlaying = true,
                duration = config.duration
            )
            
            // 4. 开始位置轮询
            startPositionPolling(device)
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Cast failed", e)
            false
        }
    }

    /**
     * 停止投屏
     */
    suspend fun stopCast(): Boolean = withContext(Dispatchers.IO) {
        val device = currentDevice ?: return@withContext false
        
        try {
            stop(device)
            castJob?.cancel()
            
            _castState.value = CastState()
            currentDevice = null
            true
        } catch (e: Exception) {
            Log.e(TAG, "Stop cast failed", e)
            false
        }
    }

    /**
     * 暂停
     */
    suspend fun pause(): Boolean = withContext(Dispatchers.IO) {
        val device = currentDevice ?: return@withContext false
        try {
            pausePlayback(device)
            _castState.value = _castState.value.copy(isPlaying = false)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 恢复播放
     */
    suspend fun resume(): Boolean = withContext(Dispatchers.IO) {
        val device = currentDevice ?: return@withContext false
        try {
            play(device)
            _castState.value = _castState.value.copy(isPlaying = true)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 跳转
     */
    suspend fun seek(position: Long): Boolean = withContext(Dispatchers.IO) {
        val device = currentDevice ?: return@withContext false
        try {
            seekTo(device, position)
            _castState.value = _castState.value.copy(position = position)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 设置音量
     */
    suspend fun setVolume(volume: Int): Boolean = withContext(Dispatchers.IO) {
        val device = currentDevice ?: return@withContext false
        try {
            setRenderingVolume(device, volume)
            true
        } catch (e: Exception) {
            false
        }
    }

    // UPnP SOAP 操作
    private suspend fun setAVTransportURI(device: DLNADevice, config: CastConfig) {
        val soap = buildAVTransportSetURI(config)
        sendSOAP(device.controlUrl, "SetAVTransportURI", soap)
    }

    private suspend fun play(device: DLNADevice) {
        val soap = buildAVTransportPlay()
        sendSOAP(device.controlUrl, "Play", soap)
    }

    private suspend fun stop(device: DLNADevice) {
        val soap = buildAVTransportStop()
        sendSOAP(device.controlUrl, "Stop", soap)
    }

    private suspend fun pausePlayback(device: DLNADevice) {
        val soap = buildAVTransportPause()
        sendSOAP(device.controlUrl, "Pause", soap)
    }

    private suspend fun seekTo(device: DLNADevice, position: Long) {
        val soap = buildAVTransportSeek(position)
        sendSOAP(device.controlUrl, "Seek", soap)
    }

    private suspend fun setRenderingVolume(device: DLNADevice, volume: Int) {
        val controlUrl = device.controlUrl.replace("AVTransport", "RenderingControl")
        val soap = buildRenderingSetVolume(volume)
        sendSOAP(controlUrl, "SetVolume", soap)
    }

    /**
     * 发送 SOAP 请求
     */
    private suspend fun sendSOAP(url: String, action: String, soapBody: String): String {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .header("Content-Type", "text/xml; charset=\"utf-8\"")
                .header("SOAPACTION", "\"urn:schemas-upnp-org:service:AVTransport:1#$action\"")
                .post(okhttp3.RequestBody.create(
                    MediaType.parse("text/xml; charset=utf-8"),
                    soapBody
                ))
                .build()

            val response = client.newCall(request).execute()
            response.body?.string() ?: ""
        }
    }

    /**
     * 开始位置轮询
     */
    private fun startPositionPolling(device: DLNADevice) {
        castJob?.cancel()
        castJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                try {
                    val position = getPosition(device)
                    _castState.value = _castState.value.copy(position = position)
                } catch (e: Exception) {
                    // 忽略轮询错误
                }
                delay(1000)
            }
        }
    }

    private suspend fun getPosition(device: DLNADevice): Long {
        val soap = buildGetPositionInfo()
        val response = sendSOAP(device.controlUrl, "GetPositionInfo", soap)
        return parsePosition(response)
    }

    // 构建 SOAP 消息
    private fun buildAVTransportSetURI(config: CastConfig): String {
        val didl = """<DIDL-LITE xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-LITE/" xmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/">
            <item id="1" parentID="-1">
                <dc:title xmlns:dc="http://purl.org/dc/elements/1.1/">${escapeXml(config.title)}</dc:title>
                <res protocolInfo="http-get:*:${config.mimeType}:*">${escapeXml(config.url)}</res>
            </item>
        </DIDL-LITE>"""

        return """<?xml version="1.0" encoding="utf-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                <s:Body>
                    <u:SetAVTransportURI xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                        <InstanceID>0</InstanceID>
                        <CurrentURI>${escapeXml(config.url)}</CurrentURI>
                        <CurrentURIMetaData>${escapeXml(didl)}</CurrentURIMetaData>
                    </u:SetAVTransportURI>
                </s:Body>
            </s:Envelope>"""
    }

    private fun buildAVTransportPlay(): String {
        return """<?xml version="1.0" encoding="utf-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                <s:Body>
                    <u:Play xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                        <InstanceID>0</InstanceID>
                        <Speed>1</Speed>
                    </u:Play>
                </s:Body>
            </s:Envelope>"""
    }

    private fun buildAVTransportStop(): String {
        return """<?xml version="1.0" encoding="utf-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                <s:Body>
                    <u:Stop xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                        <InstanceID>0</InstanceID>
                    </u:Stop>
                </s:Body>
            </s:Envelope>"""
    }

    private fun buildAVTransportPause(): String {
        return """<?xml version="1.0" encoding="utf-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                <s:Body>
                    <u:Pause xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                        <InstanceID>0</InstanceID>
                    </u:Pause>
                </s:Body>
            </s:Envelope>"""
    }

    private fun buildAVTransportSeek(position: Long): String {
        val time = formatTime(position)
        return """<?xml version="1.0" encoding="utf-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                <s:Body>
                    <u:Seek xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
<InstanceID>0</InstanceID>
                        <Unit>REL_TIME</Unit>
                        <Target>$time</Target>
                    </u:Seek>
                </s:Body>
            </s:Envelope>"""
    }

    private fun buildRenderingSetVolume(volume: Int): String {
        return """<?xml version="1.0" encoding="utf-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                <s:Body>
                    <u:SetVolume xmlns:u="urn:schemas-upnp-org:service:RenderingControl:1">
                        <InstanceID>0</InstanceID>
                        <Channel>Master</Channel>
                        <Volume>$volume</Volume>
                    </u:SetVolume>
                </s:Body>
            </s:Envelope>"""
    }

    private fun buildGetPositionInfo(): String {
        return """<?xml version="1.0" encoding="utf-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                <s:Body>
                    <u:GetPositionInfo xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                        <InstanceID>0</InstanceID>
                    </u:GetPositionInfo>
                </s:Body>
            </s:Envelope>"""
    }

    private fun formatTime(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return String.format("%02d:%02d:%02d", h, m, s)
    }

    private fun parsePosition(xml: String): Long {
        // 简单解析 AbsTime
        val regex = Regex("<(.*:)?AbsTime>([^<]+)</")
        val match = regex.find(xml)
        if (match != null) {
            val time = match.groupValues[2]
            val parts = time.split(":").map { it.toIntOrNull() ?: 0 }
            if (parts.size >= 3) {
                return parts[0] * 3600L + parts[1] * 60L + parts[2]
            }
        }
        return 0
    }

    private fun escapeXml(str: String): String {
        return str
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
