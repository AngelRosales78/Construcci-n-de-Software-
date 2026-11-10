package com.example.sp500auth.favorites

import com.example.sp500auth.data.api.AccountBinding
import com.example.sp500auth.data.repository.*
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.ArrayDeque
import java.util.Base64

class FavoritesRepositoryTest {
    private var token = jwt("1")
    private val queue = ArrayDeque<Pair<Int, String>>()
    private val requests = mutableListOf<Request>()
    private val repo = FavoritesRepository(Retrofit.Builder().baseUrl("https://fixture.invalid/api/v1/").client(OkHttpClient.Builder().addInterceptor { chain ->
        requests.add(chain.request()); val (code, body) = queue.removeFirst()
        Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(code).message("Fixture").body(body.toResponseBody("application/json".toMediaType())).build()
    }.build()).addConverterFactory(GsonConverterFactory.create()).build()) { token }
    private fun list(owner: String = "1", results: String = "[]") = """{"schema_version":"favorites-v1","owner_id":"$owner","results":$results}"""
    private val item = """{"id":7,"ticker":"AAPL","name":"Apple"}"""
    @Test fun listAndAddRequireConfirmedServerObjectsAndBearer() = runBlocking {
        queue.add(200 to list()); assertTrue(repo.list() is FavoriteResult.Success)
        queue.add(201 to """{"schema_version":"favorites-v1","owner_id":"1","favorite":$item}""")
        assertEquals("AAPL", (repo.add("AAPL") as FavoriteResult.Success).value.ticker)
        assertEquals("POST", requests.last().method); assertEquals("/api/v1/favorites/", requests.last().url.encodedPath)
        assertEquals("Bearer $token", requests.last().header("Authorization")); assertEquals("1", requests.last().tag(AccountBinding::class.java)?.owner)
    }
    @Test fun rejectsOtherOwnersDuplicatesInvalidRowsAndMalformedReplies() = runBlocking {
        for (body in listOf(list("2"), list(results = "[$item,$item]"), list(results = "[null]"), "{}", "{bad")) {
            queue.add(200 to body); assertTrue(repo.list() is FavoriteResult.Failure)
        }
    }
    @Test fun missingServiceOrForbiddenNeverPretendsSaved() = runBlocking {
        for (code in listOf(401, 403, 404, 500)) {
            queue.add(code to "{}"); val result = repo.add("AAPL") as FavoriteResult.Failure
            assertEquals(code in setOf(401, 403), result.clearPrivate)
        }
    }
    @Test fun duplicateConflictReadsServerRatherThanCreatingLocalSuccess() = runBlocking {
        queue.add(409 to "{}"); queue.add(200 to list(results = "[$item]"))
        assertEquals(7L, (repo.add("AAPL") as FavoriteResult.Success).value.id)
        assertEquals(listOf("POST", "GET"), requests.map { it.method })
    }
    @Test fun bindingStopsSendingOrRetryingOldOperationsUnderNewAccount() {
        val request = Request.Builder().url("https://fixture.invalid/").header("Authorization", "Bearer $token").tag(AccountBinding::class.java, AccountBinding("1")).build()
        enforceFavoriteAccount(request, token)
        assertThrows(java.io.IOException::class.java) { enforceFavoriteAccount(request, jwt("2")) }
        assertThrows(java.io.IOException::class.java) { enforceFavoriteAccount(request.newBuilder().header("Authorization", "Bearer ${jwt("2")}").build(), token) }
    }
    @Test fun deleteUsesFavoriteIdAndOnlyConfirms204() = runBlocking {
        queue.add(204 to "")
        assertTrue(repo.remove(FavoriteItem(7, "AAPL")) is FavoriteResult.Success)
        assertEquals("DELETE", requests.single().method)
        assertEquals("/api/v1/favorites/7/", requests.single().url.encodedPath)
        queue.add(403 to "{}"); assertTrue((repo.remove(FavoriteItem(7, "AAPL")) as FavoriteResult.Failure).clearPrivate)
    }
    @Test fun repeatedDeleteRequiresFreshListToProveAbsence() = runBlocking {
        queue.add(404 to "{}"); queue.add(200 to list())
        assertTrue(repo.remove(FavoriteItem(7, "AAPL")) is FavoriteResult.Success)
        queue.add(404 to "{}"); queue.add(200 to list(results = "[$item]"))
        assertTrue(repo.remove(FavoriteItem(7, "AAPL")) is FavoriteResult.Failure)
        queue.add(404 to "{}"); queue.add(404 to "{}")
        assertTrue(repo.remove(FavoriteItem(7, "AAPL")) is FavoriteResult.Failure)
    }
    @Test fun missingSessionOrInvalidDeleteNeverSendsRequest() = runBlocking {
        assertTrue(repo.remove(FavoriteItem(-1, "AAPL")) is FavoriteResult.Failure)
        token = ""; assertTrue(repo.remove(FavoriteItem(7, "AAPL")) is FavoriteResult.Failure)
        assertTrue(requests.isEmpty())
    }
    companion object { fun jwt(id: String) = "e30." + Base64.getUrlEncoder().withoutPadding().encodeToString("""{"user_id":$id}""".toByteArray()) + ".fixture" }
}
