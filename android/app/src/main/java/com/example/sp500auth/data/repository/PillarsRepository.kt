package com.example.sp500auth.data.repository

import com.example.sp500auth.data.api.PillarsApiService
import com.example.sp500auth.data.api.models.PillarsResponse
import com.example.sp500auth.data.local.TokenManager
import kotlinx.coroutines.CancellationException
import retrofit2.Retrofit
import java.io.IOException
import javax.inject.Inject

sealed class PillarsResult {
    data class Success(val data: PillarsResponse) : PillarsResult()
    data class Failure(val message: String) : PillarsResult()
}

interface PillarsDataSource {
    suspend fun load(ticker: String, fiscalYear: Int): PillarsResult
}

class PillarsRepository internal constructor(retrofit: Retrofit, private val accessToken: () -> String?) : PillarsDataSource {
    @Inject constructor(retrofit: Retrofit, tokens: TokenManager) : this(retrofit, { tokens.getAccessToken() })
    private val api = retrofit.create(PillarsApiService::class.java)

    override suspend fun load(ticker: String, fiscalYear: Int): PillarsResult {
        val access = accessToken()
            ?: return PillarsResult.Failure("Inicia sesión para consultar los pilares financieros.")
        return try {
            val response = api.getPillars(ticker, fiscalYear, "Bearer $access")
            val body = response.body()
            if (response.isSuccessful && body != null) {
                when {
                    body.schema_version != "pillars-v1" -> PillarsResult.Failure("El formato del servidor no es compatible con pillars-v1.")
                    body.ticker != ticker || body.fiscal_year != fiscalYear ->
                        PillarsResult.Failure("El servidor devolvió otra empresa o ejercicio. No se mostrarán esos datos.")
                    else -> PillarsResult.Success(body)
                }
            } else PillarsResult.Failure(when (response.code()) {
                400 -> "Revisa el símbolo y el ejercicio fiscal solicitado."
                401, 403 -> "Tu sesión no permite esta consulta. Vuelve a iniciar sesión."
                404 -> "No se encontró la empresa o el servicio de pilares todavía no está habilitado."
                503 -> "El proveedor de datos no está disponible. Inténtalo nuevamente."
                else -> "No se pudieron cargar los pilares (HTTP ${response.code()})."
            })
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            PillarsResult.Failure("No se pudo conectar con el servidor. Revisa tu conexión e inténtalo nuevamente.")
        } catch (e: Exception) {
            PillarsResult.Failure("La respuesta del servidor no se pudo interpretar.")
        }
    }
}
