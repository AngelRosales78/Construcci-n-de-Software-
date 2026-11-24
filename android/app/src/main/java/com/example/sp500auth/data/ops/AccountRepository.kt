package com.example.sp500auth.data.ops

import com.example.sp500auth.data.api.AccountBinding
import com.example.sp500auth.data.api.models.UserProfile
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*
import javax.inject.Inject

const val AVATAR_LIMIT = 2 * 1024 * 1024
val AVATAR_TYPES = setOf("image/jpeg", "image/png", "image/webp")

/** HU13_04: the same limits the server applies, checked before sending. */
fun avatarProblem(type: String?, size: Int): String? = when {
    type !in AVATAR_TYPES -> "Elige una imagen JPEG, PNG o WebP."
    size <= 0 -> "La imagen está vacía o no se pudo leer."
    size >= AVATAR_LIMIT -> "La imagen debe ocupar menos de 2 MB."
    else -> null
}

class AvatarFile(val type: String, val bytes: ByteArray)
data class DeleteAccount(val password: String)

interface AccountApi {
    @GET("auth/profile/")
    suspend fun profile(@Header("Authorization") token: String, @Tag account: AccountBinding): Response<UserProfile>

    @Multipart @PATCH("auth/profile/")
    suspend fun update(@Part("first_name") first: RequestBody, @Part("last_name") last: RequestBody, @Part avatar: MultipartBody.Part?,
        @Header("Authorization") token: String, @Tag account: AccountBinding): Response<UserProfile>

    @PATCH("auth/profile/")
    suspend fun patch(@Body body: RequestBody, @Header("Authorization") token: String, @Tag account: AccountBinding): Response<UserProfile>

    @GET("auth/profile/avatar/")
    suspend fun avatar(@Header("Authorization") token: String, @Tag account: AccountBinding): Response<ResponseBody>

    @HTTP(method = "DELETE", path = "auth/account/", hasBody = true)
    suspend fun delete(@Body body: DeleteAccount, @Header("Authorization") token: String, @Tag account: AccountBinding): Response<Unit>
}

interface AccountSource {
    fun owner(): String?
    suspend fun profile(): Outcome<UserProfile>
    suspend fun save(first: String, last: String, avatar: AvatarFile?): Outcome<UserProfile>
    suspend fun removeAvatar(): Outcome<UserProfile>
    suspend fun avatar(): Outcome<ByteArray>
    suspend fun delete(password: String): Outcome<Unit>
}

class AccountRepository @Inject constructor(private val remote: OpsRemote) : AccountSource {
    private val api = remote.retrofit.create(AccountApi::class.java)
    private val text = "text/plain; charset=utf-8".toMediaType()
    private fun mine(profile: UserProfile, user: String) = profile.id.toString() == user

    override fun owner() = remote.owner()
    override suspend fun profile() = remote.call(::mine) { token, account -> api.profile(token, account) }

    override suspend fun save(first: String, last: String, avatar: AvatarFile?) = remote.call(::mine) { token, account ->
        val part = avatar?.let {
            MultipartBody.Part.createFormData("avatar", "avatar." + it.type.substringAfter('/'), it.bytes.toRequestBody(it.type.toMediaType()))
        }
        api.update(first.trim().toRequestBody(text), last.trim().toRequestBody(text), part, token, account)
    }

    override suspend fun removeAvatar() = remote.call(::mine) { token, account ->
        api.patch("""{"avatar":null}""".toRequestBody("application/json".toMediaType()), token, account)
    }

    override suspend fun avatar(): Outcome<ByteArray> =
        when (val answer = remote.call<ResponseBody> { token, account -> api.avatar(token, account) }) {
            is Outcome.Fail -> answer
            is Outcome.Ok -> runCatching { answer.value.use { it.bytes() } }.fold(
                { if (it.isEmpty()) Outcome.Fail("La foto está vacía.", 0) else Outcome.Ok(it) },
                { Outcome.Fail("No se pudo descargar la foto.") })
        }

    override suspend fun delete(password: String) = remote.command { token, account -> api.delete(DeleteAccount(password), token, account) }
}
