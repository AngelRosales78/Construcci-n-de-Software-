package com.example.sp500auth.data.ops

import com.example.sp500auth.data.api.AccountBinding
import retrofit2.Response
import retrofit2.http.*
import javax.inject.Inject

/** HU13_01 to HU13_03. Role and plan are independent: an administrator is not Pro because of the role. */
data class AdminUser(
    val id: Long? = null, val username: String? = null, val first_name: String? = null, val last_name: String? = null,
    val email: String? = null, val role: String? = null, val plan: String? = null, val is_active: Boolean? = null,
    val blocked_reason: String? = null
) {
    val fullName get() = listOf(first_name, last_name).mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }.joinToString(" ")
}

fun validAdminUser(user: AdminUser?) = user?.id != null && user.id > 0 && !user.username.isNullOrBlank() &&
    user.role in setOf("admin", "user") && user.plan in setOf("free", "pro") && user.is_active != null

interface UsersApi {
    @GET("admin/users/")
    suspend fun list(@Query("q") q: String, @Query("plan") plan: String, @Query("state") state: String, @Query("page") page: Int,
        @Header("Authorization") token: String, @Tag account: AccountBinding): Response<Page<AdminUser>>

    @PATCH("admin/users/{id}/")
    suspend fun update(@Path("id") id: Long, @Body change: Map<String, @JvmSuppressWildcards Any>,
        @Header("Authorization") token: String, @Tag account: AccountBinding): Response<AdminUser>
}

interface UsersSource {
    suspend fun list(q: String, plan: String, state: String, page: Int): Outcome<Page<AdminUser>>
    suspend fun update(id: Long, change: Map<String, Any>): Outcome<AdminUser>
}

class UsersRepository @Inject constructor(private val remote: OpsRemote) : UsersSource {
    private val api = remote.retrofit.create(UsersApi::class.java)

    private fun valid(data: Page<AdminUser>, user: String): Boolean {
        val rows = data.results ?: return false
        return user.isNotBlank() && rows.size <= 20 && rows.all(::validAdminUser) && rows.map { it.id }.distinct().size == rows.size
    }

    override suspend fun list(q: String, plan: String, state: String, page: Int) =
        remote.call(::valid) { token, account -> api.list(q.trim().take(120), plan, state, page, token, account) }

    override suspend fun update(id: Long, change: Map<String, Any>) =
        remote.call({ data: AdminUser, _ -> validAdminUser(data) && data.id == id }) { token, account -> api.update(id, change, token, account) }
}
