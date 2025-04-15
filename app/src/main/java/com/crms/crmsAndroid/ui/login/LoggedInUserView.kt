package com.crms.crmsAndroid.ui.login

/**
 * User details post authentication that is exposed to the UI
 */
// LoggedInUserView.kt
data class LoggedInUserView(
    val displayName: String,
    val accessLevel: Int,
    val accessPage: Int
)