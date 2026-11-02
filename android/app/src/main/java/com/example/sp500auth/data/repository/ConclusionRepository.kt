package com.example.sp500auth.data.repository

import com.example.sp500auth.data.api.AccountBinding
import com.example.sp500auth.data.api.models.ComparisonConclusion
import com.example.sp500auth.ui.common.safeSourceUrl
import com.example.sp500auth.ui.compare.validTicker
import com.example.sp500auth.util.Resource
import retrofit2.Response
import retrofit2.http.*
import javax.inject.Inject

interface ConclusionApi {
    @GET("compare/conclusion/") suspend fun conclusion(@Query("a") a: String, @Query("b") b: String, @Query("fiscal_year") year: Int,
        @Header("Authorization") token: String, @Tag account: AccountBinding): Response<ComparisonConclusion>
}
interface ConclusionSource {
    fun owner(): String?
    suspend fun load(a: String, b: String, year: Int): Resource<ComparisonConclusion>
}
fun validConclusion(data: ComparisonConclusion, owner: String, a: String, b: String, year: Int): Boolean =
    data.schema_version == "comparison-conclusion-v1" && data.owner_id == owner && data.a == a && data.b == b && data.fiscal_year == year &&
    data.generation_mode in setOf("ai", "rules") && !data.generated_at.isNullOrBlank() && !data.data_version.isNullOrBlank() &&
    !data.paragraphs.isNullOrEmpty() && data.paragraphs.size <= 10 && data.paragraphs.all { !it.isNullOrBlank() && it.length <= 12000 } &&
    data.strengths_a != null && data.strengths_b != null && data.strengths_a.size <= 4 && data.strengths_b.size <= 4 &&
    (data.strengths_a + data.strengths_b).all { !it.isNullOrBlank() && it.length <= 3000 } &&
    !data.sources.isNullOrEmpty() && data.sources.all { it != null && safeSourceUrl(it.url) }

class ConclusionRepository @Inject constructor(private val remote: ScopedRemote) : ConclusionSource {
    private val api = remote.retrofit.create(ConclusionApi::class.java)
    override fun owner() = remote.owner()
    override suspend fun load(a: String, b: String, year: Int): Resource<ComparisonConclusion> {
        if (!validTicker(a) || !validTicker(b) || a == b || year !in 2000..2100) return Resource.Error("Revisa las empresas y el ejercicio.")
        return remote.call({ data, owner -> validConclusion(data, owner, a, b, year) }) { token, account -> api.conclusion(a, b, year, token, account) }
    }
}
