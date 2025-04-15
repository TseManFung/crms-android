package com.crms.crmsAndroid.api.exception

import com.crms.crmsAndroid.utils.ErrorCode

class ErrorCodeException(val errorCode: ErrorCode) : RuntimeException(errorCode.toString())