package com.example.sp500auth.data.api

import com.example.sp500auth.data.repository.FavoriteItem
import retrofit2.Response
import retrofit2.http.*

data class AccountBinding(val owner: String)
data class FavoritesPage(val schema_version: String? = null, val owner_id: String? = null, val results: List<FavoriteItem?>? = null)
data class FavoriteMutation(val schema_version: String? = null, val owner_id: String? = null, val favorite: FavoriteItem? = null)
data class AddFavoriteRequest(val ticker: String)
interface FavoritesApiService {
    @GET("favorites/") suspend fun list(@Header("Authorization") bearer: String, @Tag account: AccountBinding): Response<FavoritesPage>
    @POST("favorites/") suspend fun add(@Body request: AddFavoriteRequest, @Header("Authorization") bearer: String, @Tag account: AccountBinding): Response<FavoriteMutation>
    @DELETE("favorites/{id}/") suspend fun remove(@Path("id") id: Long, @Header("Authorization") bearer: String, @Tag account: AccountBinding): Response<Unit>
}
