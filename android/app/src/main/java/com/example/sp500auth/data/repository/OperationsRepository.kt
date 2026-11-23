package com.example.sp500auth.data.repository

import com.example.sp500auth.data.api.AccountBinding
import com.example.sp500auth.data.local.TokenManager
import com.google.gson.JsonObject
import kotlinx.coroutines.CancellationException
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.*
import javax.inject.Inject

fun boundedBytes(input:java.io.InputStream,maximum:Int):ByteArray {
    val output=java.io.ByteArrayOutputStream()
    val buffer=ByteArray(8192)
    while(output.size()<maximum){val read=input.read(buffer,0,minOf(buffer.size,maximum-output.size()));if(read<0)break;output.write(buffer,0,read)}
    return output.toByteArray()
}

interface OperationsApi {
    @GET("plans/") suspend fun plans(@Header("Authorization") authorization:String,@Tag account:AccountBinding):Response<JsonObject>
    @GET("auth/profile/avatar/") suspend fun picture(@Header("Authorization") authorization:String,@Tag account:AccountBinding):Response<okhttp3.ResponseBody>
    @GET suspend fun get(@Url path: String, @Header("Authorization") authorization: String, @Tag account: AccountBinding): Response<JsonObject>
    @POST suspend fun post(@Url path: String, @Body body: JsonObject, @Header("Authorization") authorization: String, @Tag account: AccountBinding): Response<JsonObject>
    @PATCH suspend fun patch(@Url path: String, @Body body: JsonObject, @Header("Authorization") authorization: String, @Tag account: AccountBinding): Response<JsonObject>
    @PUT suspend fun put(@Url path: String, @Body body: JsonObject, @Header("Authorization") authorization: String, @Tag account: AccountBinding): Response<JsonObject>
    @HTTP(method="DELETE",hasBody=true) suspend fun delete(@Url path: String, @Body body: JsonObject, @Header("Authorization") authorization: String, @Tag account: AccountBinding): Response<JsonObject>
    @Multipart @PATCH("auth/profile/") suspend fun avatar(@Part image: MultipartBody.Part, @Header("Authorization") authorization: String, @Tag account: AccountBinding): Response<JsonObject>
}

class OperationsFailure(message: String): Exception(message)

class OperationsRepository internal constructor(retrofit: Retrofit, private val accessToken:()->String?) {
    @Inject constructor(retrofit:Retrofit,tokens:TokenManager):this(retrofit,{tokens.getAccessToken()})
    private val api=retrofit.create(OperationsApi::class.java)
    fun owner()=favoritesOwner(accessToken())
    suspend fun request(path:String, method:String="GET", body:JsonObject=JsonObject()):JsonObject {
        require(!path.startsWith("http") && !path.startsWith("/") && !path.contains(".."))
        val token=accessToken() ?: throw OperationsFailure("Inicia sesión para continuar.")
        val owner=favoritesOwner(token) ?: throw OperationsFailure("Vuelve a iniciar sesión.")
        return try {
            val binding=AccountBinding(owner)
            val response=when(method) {
                "POST"->api.post(path,body,"Bearer $token",binding)
                "PATCH"->api.patch(path,body,"Bearer $token",binding)
                "PUT"->api.put(path,body,"Bearer $token",binding)
                "DELETE"->api.delete(path,body,"Bearer $token",binding)
                else->api.get(path,"Bearer $token",binding)
            }
            if(owner()!=owner) throw OperationsFailure("La cuenta cambió. Vuelve a abrir la pantalla.")
            if(!response.isSuccessful) {
                val status=response.code(); response.errorBody()?.close()
                throw OperationsFailure(when(status) {
                    401->"La sesión venció. Vuelve a iniciar sesión."
                    403->"Tu cuenta no tiene permiso para esta operación."
                    400->"Revisa los campos y vuelve a intentarlo."
                    409->"La información cambió o la operación afecta al último administrador. Actualiza antes de continuar."
                    429->"Se alcanzó el límite de consultas. Los datos guardados siguen disponibles."
                    else->"No se pudo confirmar la operación (HTTP $status)."
                })
            }
            response.body() ?: JsonObject()
        } catch(e:CancellationException) { throw e }
        catch(e:OperationsFailure) { throw e }
        catch(_:Exception) { throw OperationsFailure("No se pudo conectar. Conserva tus cambios y reintenta.") }
    }
    suspend fun uploadAvatar(part:MultipartBody.Part):JsonObject {
        val token=accessToken() ?: throw OperationsFailure("Inicia sesión.")
        val owner=owner() ?: throw OperationsFailure("Inicia sesión.")
        val response=api.avatar(part,"Bearer $token",AccountBinding(owner))
        if(owner()!=owner || !response.isSuccessful) { response.errorBody()?.close(); throw OperationsFailure("No se pudo guardar la foto. Usa una imagen válida de menos de 2 MB.") }
        return response.body() ?: JsonObject()
    }
    suspend fun picture():ByteArray? {
        val token=accessToken() ?: return null
        val owner=owner() ?: return null
        val response=api.picture("Bearer $token",AccountBinding(owner))
        if(!response.isSuccessful || owner()!=owner){response.errorBody()?.close();return null}
        return response.body()?.use{boundedBytes(it.byteStream(),1024*1024)}
    }
}
