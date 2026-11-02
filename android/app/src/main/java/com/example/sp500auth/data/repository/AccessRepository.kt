package com.example.sp500auth.data.repository

import com.example.sp500auth.data.api.AccountBinding
import com.example.sp500auth.util.Resource
import retrofit2.Response
import retrofit2.http.*
import java.time.Instant
import javax.inject.Inject

data class AccessInfo(val schema_version: String? = null, val owner_id: String? = null, val plan: String? = null,
    val active: Boolean? = null, val features: List<String?>? = null, val valid_until: String? = null)
fun accessFresh(info: AccessInfo): Boolean = try { Instant.parse(info.valid_until).isAfter(Instant.now()) } catch (_: Exception) { false }
fun canCompare(info: AccessInfo) = accessFresh(info) && info.plan == "pro" && info.active == true && "compare" in info.features.orEmpty()
interface AccessApi { @GET("user/subscription/") suspend fun get(@Header("Authorization") token: String, @Tag account: AccountBinding): Response<AccessInfo> }
interface AccessSource { fun owner(): String?; suspend fun load(): Resource<AccessInfo> }
class AccessRepository @Inject constructor(private val remote: ScopedRemote) : AccessSource {
    private val api = remote.retrofit.create(AccessApi::class.java)
    override fun owner() = remote.owner()
    override suspend fun load() = remote.call({ data: AccessInfo, owner -> data.schema_version == "subscription-v1" && data.owner_id == owner && data.plan in setOf("free","pro") && data.active != null && data.features != null && accessFresh(data) }) { token, account -> api.get(token,account) }
}
