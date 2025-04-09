package com.crms.crmsAndroid.utils

enum class ErrorCode(val description: String) {
    E("wtf"),
    E01("Argument Missing"),
    E02("Argument Wrong"),
    E03("No Permission"),
    E04("Token Expired"),
    E05("Database Connection Error"),
    E06("Query Error"),
    E07("User Not Found"),
    E08("Invalid Password"),
    E09("Invalid email format"),
    E10("Token Invalid"),
    E11("Duplicate Key"),
    ;

    override fun toString(): String {
        return """{ "errorCode": "$name", "description": "$description" }"""
    }

    companion object {
        fun toErrorCode(code: String): ErrorCode {
            return entries.find { it.name == code } ?: E // Default to E if not found
        }
    }
}