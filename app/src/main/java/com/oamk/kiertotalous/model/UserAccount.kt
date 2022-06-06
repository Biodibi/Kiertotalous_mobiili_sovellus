package com.oamk.kiertotalous.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class UserRole(val role: String) {
    STORE("store"),
    COURIER("courier"),
    ADMIN("admin");

    companion object {
        fun userRoleFromValue(value: String?): UserRole? {
            return values().find { userRole ->
                value == userRole.role
            }
        }
    }
}

@Serializable
data class UserAccount(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "",
    val siteId: String = "",
    val created: String = "",
    val modified: String = ""
) {
    fun userRole(): UserRole? {
        return UserRole.userRoleFromValue(role)
    }

    fun toJson(): String? {
        return Json {
            ignoreUnknownKeys = true
        }.encodeToString(this)
    }
}