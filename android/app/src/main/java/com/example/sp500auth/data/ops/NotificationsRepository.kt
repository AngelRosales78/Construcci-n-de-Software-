package com.example.sp500auth.data.ops

import com.example.sp500auth.data.api.AccountBinding
import com.example.sp500auth.ui.compare.validTicker
import retrofit2.Response
import retrofit2.http.*
import javax.inject.Inject

/** Épica 12. Categories come from the server; the phone never recalculates them. */
data class RatingSide(val category: String? = null, val label: String? = null, val score: Double? = null, val as_of: String? = null, val source: String? = null)
data class Notice(
    val id: Long? = null, val type: String? = null, val created_at: String? = null, val read_at: String? = null, val ticker: String? = null,
    val company_name: String? = null, val event_at: String? = null, val is_demo: Boolean? = null, val title: String? = null, val message: String? = null,
    val before: RatingSide? = null, val after: RatingSide? = null
) { val unread get() = read_at == null }
data class NoticePage(val count: Int? = null, val next: String? = null, val results: List<Notice>? = null, val owner_id: String? = null,
    val unread_count: Int? = null, val read_through: Long? = null)
data class ReadAnswer(val owner_id: String? = null, val updated: Int? = null, val unread_count: Int? = null)
data class Channels(val web: Boolean = false, val email: Boolean = false, val push: Boolean = false)
data class Preferences(
    val owner_id: String? = null, val version: Int? = null, val rating_changes: Boolean? = null, val channels: Channels? = null,
    val available_channels: Channels? = null, val channel_messages: Map<String, String>? = null, val updated_at: String? = null
)
data class PreferencesChange(val version: Int, val rating_changes: Boolean, val channels: Channels)
data class InstallationInput(val installation_id: String, val token: String, val permission_granted: Boolean)
data class InstallationAnswer(val owner_id: String? = null, val installation_id: String? = null, val configured: Boolean? = null)

fun validNotice(notice: Notice?) = notice?.id != null && notice.id > 0 && !notice.created_at.isNullOrBlank() && validTicker(notice.ticker.orEmpty())

fun validNoticePage(page: NoticePage, user: String): Boolean {
    val rows = page.results ?: return false
    return page.owner_id == user && rows.all(::validNotice) && (page.unread_count ?: -1) >= 0 && page.read_through != null &&
        rows.mapNotNull { it.id }.zipWithNext().all { (a, b) -> a > b }
}

interface NotificationsApi {
    @GET("notifications/")
    suspend fun list(@Query("page") page: Int, @Query("through_id") through: Long?, @Header("Authorization") token: String, @Tag account: AccountBinding): Response<NoticePage>

    @POST("notifications/mark-read/")
    suspend fun read(@Body body: Map<String, @JvmSuppressWildcards Any>, @Header("Authorization") token: String, @Tag account: AccountBinding): Response<ReadAnswer>

    @GET("user/notification-preferences/")
    suspend fun preferences(@Header("Authorization") token: String, @Tag account: AccountBinding): Response<Preferences>

    @PUT("user/notification-preferences/")
    suspend fun save(@Body body: PreferencesChange, @Header("Authorization") token: String, @Tag account: AccountBinding): Response<Preferences>

    @POST("user/push-installations/")
    suspend fun register(@Body body: InstallationInput, @Header("Authorization") token: String, @Tag account: AccountBinding): Response<InstallationAnswer>

    @DELETE("user/push-installations/{id}/")
    suspend fun unregister(@Path("id") id: String, @Header("Authorization") token: String, @Tag account: AccountBinding): Response<Unit>
}

interface NotificationsSource {
    fun owner(): String?
    suspend fun list(page: Int, through: Long?): Outcome<NoticePage>
    suspend fun markRead(id: Long): Outcome<ReadAnswer>
    suspend fun markAll(through: Long): Outcome<ReadAnswer>
    suspend fun preferences(): Outcome<Preferences>
    suspend fun save(change: PreferencesChange): Outcome<Preferences>
}

/** Registration of this phone for Push; separate so it can be tested without Firebase. */
interface PushInstallationsSource {
    fun owner(): String?
    suspend fun register(id: String, token: String, granted: Boolean): Outcome<InstallationAnswer>
    suspend fun unregister(id: String): Outcome<Unit>
}

class NotificationsRepository @Inject constructor(private val remote: OpsRemote) : NotificationsSource, PushInstallationsSource {
    private val api = remote.retrofit.create(NotificationsApi::class.java)
    override fun owner() = remote.owner()

    override suspend fun list(page: Int, through: Long?) =
        remote.call(::validNoticePage) { token, account -> api.list(page, through, token, account) }

    private suspend fun read(body: Map<String, Any>) = remote.call({ data: ReadAnswer, user ->
        data.owner_id == user && (data.unread_count ?: -1) >= 0
    }) { token, account -> api.read(body, token, account) }
    override suspend fun markRead(id: Long) = read(mapOf("ids" to listOf(id)))
    override suspend fun markAll(through: Long) = read(mapOf("all" to true, "through_id" to through))

    private fun mine(data: Preferences, user: String) = data.owner_id == user && data.version != null &&
        data.rating_changes != null && data.channels != null && data.available_channels != null
    override suspend fun preferences() = remote.call(::mine) { token, account -> api.preferences(token, account) }
    override suspend fun save(change: PreferencesChange) = remote.call(::mine) { token, account -> api.save(change, token, account) }

    override suspend fun register(id: String, token: String, granted: Boolean) = remote.call({ data: InstallationAnswer, user ->
        data.owner_id == user && data.installation_id.equals(id, ignoreCase = true)
    }) { auth, account -> api.register(InstallationInput(id, token, granted), auth, account) }
    override suspend fun unregister(id: String) = remote.command { token, account -> api.unregister(id, token, account) }
}
