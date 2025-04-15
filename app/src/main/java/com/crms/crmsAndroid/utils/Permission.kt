package com.fyp.crms_backend.utils

enum class Permission(val level: Int, val displayName: String) {
    ADMIN(0, "Administrator"),
    TEACHER(100, "Teacher"),
    TECHNICIAN(100, "Technician"),
    STUDENT(1000, "Student"),
    GUEST(10000, "Guest");

    companion object {
        // 根据数据库的 accessLevel 获取对应的权限
        fun fromLevel(level: Int): Permission? {
            return values().find { it.level == level }
        }

        // 获取可读的权限名称列表（例如用于 UI 展示）
        fun getDisplayNames(): List<String> {
            return values().map { it.displayName }
        }

        fun getLevelByName(name: String): Int {
            return values().find { it.displayName == name }?.level ?: -1
        }

        fun getNameByLevel(level: Int): String {
            return values().find { it.level == level }?.displayName ?: "Unknown"
        }

        fun maxLevel(): Int = values().maxOf { it.level }

        fun getAllByLevel(level: Int): List<Permission> {
            return values().filter { it.level == level }
        }
    }
}