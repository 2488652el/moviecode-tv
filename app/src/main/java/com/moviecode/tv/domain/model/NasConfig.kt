package com.moviecode.tv.domain.model

data class NasConfig(
    val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int = 445,
    val username: String,
    val password: String,
    val shareName: String = "",
    val protocol: NasProtocol = NasProtocol.SMB,
    val isDefault: Boolean = false
)

enum class NasProtocol {
    SMB,
    WEBDAV
}
