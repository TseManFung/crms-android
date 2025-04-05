package com.crms.crmsAndroid

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.crms.crmsAndroid.data.LoginDataSource
import com.crms.crmsAndroid.data.LoginRepository

class SharedViewModel : ViewModel() {
    // all the data that needs to be shared between fragments
    val loginRepository: LoginRepository = LoginRepository(LoginDataSource())

    val token:String
        get() =loginRepository.user?.token ?: throw IllegalStateException("Token is not available")

    fun logout() {
        loginRepository.logout()

    }


}