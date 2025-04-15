package com.crms.crmsAndroid

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.crms.crmsAndroid.data.LoginDataSource
import com.crms.crmsAndroid.data.LoginRepository

class SharedViewModel : ViewModel() {
    // all the data that needs to be shared between fragments
    val loginRepository: LoginRepository = LoginRepository(LoginDataSource())

    val token: String
        get() = loginRepository.user?.token ?: throw IllegalStateException("Token is not available")

    val accessLevel: Int
        get() = loginRepository.user?.accessLevel
            ?: throw IllegalStateException("Access level is not available")

    private val _accessPage = MutableLiveData<Int>()
    val accessPage: LiveData<Int> get() = _accessPage

    init {
        // 初始化時更新權限
        _accessPage.value = loginRepository.user?.accessPage ?: 0
    }

    fun logout() {
        loginRepository.logout()
        _accessPage.value = 0
    }

    fun updateAccessPage(value: Int) {
        _accessPage.value = value
    }


}