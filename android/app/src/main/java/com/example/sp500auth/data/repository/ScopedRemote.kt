package com.example.sp500auth.data.repository

import com.example.sp500auth.data.api.AccountBinding
import com.example.sp500auth.data.local.TokenManager
import com.example.sp500auth.util.Resource
import retrofit2.Response
import retrofit2.Retrofit
import javax.inject.Inject

/** Every personal request is bound to its account, including authenticated retries. */
class ScopedRemote internal constructor(val retrofit: Retrofit, private val token: () -> String?) {
    @Inject constructor(retrofit: Retrofit, tokens: TokenManager) : this(retrofit, { tokens.getAccessToken() })
    fun owner() = favoritesOwner(token())
    suspend fun <T : Any> call(validate: (T, String) -> Boolean, block: suspend (String, AccountBinding) -> Response<T>): Resource<T> {
        val access = token(); val user = favoritesOwner(access) ?: return Resource.Error("Inicia sesión para consultar este servicio.")
        val result = remoteCall { block("Bearer $access", AccountBinding(user)) }
        if (owner() != user) return Resource.Error("La cuenta cambió. Vuelve a consultar.")
        return if (result is Resource.Success && !validate(result.successData, user)) Resource.Error("El servidor devolvió datos incompletos o de otra consulta.") else result
    }
}
