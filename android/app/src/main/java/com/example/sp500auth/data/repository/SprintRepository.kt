package com.example.sp500auth.data.repository
import com.example.sp500auth.data.api.SprintApiService
import com.example.sp500auth.util.Resource
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import retrofit2.Retrofit
import javax.inject.Inject

suspend fun <T : Any> remoteCall(block: suspend () -> Response<T>): Resource<T> = try {
    val response=block(); val body=response.body()
    if(response.isSuccessful && body!=null) Resource.Success(body)
    else Resource.Error(when(response.code()) {
        400 -> "Revisa los parámetros de la consulta."
        401,403 -> "La sesión no permite consultar estos datos. Vuelve a iniciar sesión."
        404,501 -> "La empresa o este servicio todavía no están disponibles en el servidor."
        503 -> "El proveedor no está disponible. Inténtalo nuevamente."
        else -> "No se pudo obtener la respuesta (HTTP ${response.code()})."
    })
} catch(e: CancellationException) { throw e
} catch(_: Exception) { Resource.Error("No se pudo conectar o interpretar la respuesta. Puedes reintentar.") }

class SprintRepository @Inject constructor(retrofit: Retrofit) {
    private val api=retrofit.create(SprintApiService::class.java)
    suspend fun search(query: String,page: Int)=remoteCall{api.search(query,page)}
    suspend fun summary(ticker: String)=remoteCall{api.summary(ticker)}
    suspend fun history(ticker: String,range: String)=remoteCall{api.history(ticker,range)}
}
