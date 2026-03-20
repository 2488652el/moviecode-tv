package com.moviecode.tv.data.repository

import kotlinx.serialization.Serializable

@Serializable
data class NasConfig(
    val id: String,
    val name: String,
    val nasType: String, // "smb", "nfs", "webdav", "local"
    val host: String? = null,
    val port: Int? = null,
    val share: String? = null,
    val username: String? = null,
    val password: String? = null,
    val basePath: String? = null
)

@Serializable
data class NasConnectionResult(
    val success: Boolean,
    val message: String,
    val connectionId: String? = null
)

data class FileInfo(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val modified: Long? = null
)
