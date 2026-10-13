package com.example.sp500auth.util

sealed class Resource<out T>(val data: T? = null, val message: String? = null) {
    class Empty : Resource<Nothing>()
    class Loading : Resource<Nothing>()
    data class Success<T>(val successData: T) : Resource<T>(data = successData)
    data class Error<T>(val errorMessage: String, val errorData: T? = null) : Resource<T>(data = errorData, message = errorMessage)
}
