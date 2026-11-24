package com.example.sp500auth.ops

import com.example.sp500auth.data.api.AccountBinding
import com.example.sp500auth.data.ops.*
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.Base64

/** Contract tests: paths, parameters, bodies and errors exactly as the shared Sprint 4 API publishes them. */
class OpsHttpTest {
    private val requests = mutableListOf<Request>()
    private val bodies = mutableListOf<String>()
    private var account: String? = "1"
    private var code = 200
    private var answer = "{}"
    private var type = "application/json"
    private var offline = false
    private var during: () -> Unit = {}
    private fun token() = account?.let { "e30." + Base64.getUrlEncoder().withoutPadding().encodeToString("""{"user_id":$it}""".toByteArray()) + ".x" }
    private val remote = OpsRemote(Retrofit.Builder().baseUrl("https://fixture.invalid/api/v1/").client(OkHttpClient.Builder().addInterceptor { chain ->
        val request = chain.request(); requests += request
        assertEquals(account, request.tag(AccountBinding::class.java)?.owner)
        assertEquals("Bearer ${token()}", request.header("Authorization"))
        bodies += request.body?.let { Buffer().also(it::writeTo).readByteString().utf8() }.orEmpty()
        if (offline) throw IOException("offline")
        during()
        Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(code).message("fixture").apply {
            if (code == 204) body("".toResponseBody(null)) else body(answer.toResponseBody(type.toMediaType()))
        }.build()
    }.build()).addConverterFactory(GsonConverterFactory.create()).build()) { token() }
    private fun path() = requests.last().url.encodedPath.removePrefix("/api/v1/")
    private fun query(name: String) = requests.last().url.queryParameter(name)
    private fun json() = JsonParser.parseString(bodies.last()).asJsonObject
    private val user = """{"id":7,"username":"ana","first_name":"Ana","last_name":"P","email":"a@example.test","role":"admin","plan":"free","is_active":true,"blocked_reason":""}"""

    @Test fun usersSendFiltersAndKeepRoleApartFromPlan() = runBlocking {
        val users = UsersRepository(remote)
        answer = """{"count":1,"next":null,"previous":null,"results":[$user]}"""
        val page = (users.list(" ana ", "free", "active", 3) as Outcome.Ok).value
        assertEquals("admin/users/", path())
        assertEquals(listOf("ana", "free", "active", "3"), listOf("q", "plan", "state", "page").map(::query))
        assertEquals("admin" to "free", page.results!!.single().let { it.role to it.plan })
        answer = """{"count":0,"results":[]}"""
        assertTrue((users.list("", "all", "all", 1) as Outcome.Ok).value.results!!.isEmpty())
        answer = """{"results":[{"id":7,"username":"x","role":"owner","plan":"free","is_active":true}]}"""
        assertTrue(users.list("", "all", "all", 1) is Outcome.Fail)
    }

    @Test fun userChangesArePartialAndErrorsAreUnderstandable() = runBlocking {
        val users = UsersRepository(remote)
        answer = user
        assertTrue(users.update(7, mapOf("role" to "admin")) is Outcome.Ok)
        assertEquals("PATCH", requests.last().method); assertEquals("admin/users/7/", path())
        assertEquals(setOf("role"), json().keySet())
        assertTrue("otra cuenta no se acepta", users.update(8, mapOf("plan" to "pro")) is Outcome.Fail)
        code = 403; answer = """{"detail":"You do not have permission to perform this action."}"""
        assertEquals("Tu cuenta no tiene permisos para esta acción.", (users.list("", "all", "all", 1) as Outcome.Fail).message)
        code = 409; answer = """{"detail":"Debe conservarse al menos un administrador activo."}"""
        assertEquals("Debe conservarse al menos un administrador activo.", (users.update(7, mapOf("role" to "user")) as Outcome.Fail).message)
        code = 400; answer = """{"reason":["Indica un motivo para bloquear la cuenta."]}"""
        assertEquals("Indica un motivo para bloquear la cuenta.", (users.update(7, mapOf("is_active" to false)) as Outcome.Fail).fields["reason"])
    }

    @Test fun missingServiceNetworkFailureAndRevokedSessionAreDifferent() = runBlocking {
        val users = UsersRepository(remote)
        code = 404; type = "text/html"; answer = "<h1>Not Found</h1>"
        assertEquals(SERVICE_MISSING, (users.list("", "all", "all", 1) as Outcome.Fail).message)
        offline = true
        assertTrue((users.list("", "all", "all", 1) as Outcome.Fail).uncertain)
        offline = false; code = 401
        assertEquals(401, (users.list("", "all", "all", 1) as Outcome.Fail).code)
        account = null; val sent = requests.size
        assertEquals(401, (users.list("", "all", "all", 1) as Outcome.Fail).code); assertEquals(sent, requests.size)
    }

    @Test fun answerForAnAccountThatChangedIsDiscarded() = runBlocking {
        answer = """{"count":0,"results":[]}"""
        during = { account = "2" }
        assertEquals(409, (UsersRepository(remote).list("", "all", "all", 1) as Outcome.Fail).code)
    }

    private val profile = """{"id":1,"username":"ana","email":"a@example.test","first_name":"Ana","last_name":"P","user_type":"retail","role":"user","avatar_url":"/x","avatar_version":"v1"}"""
    @Test fun profileTravelsAsMultipartAndPhotoIsReadWithAuthentication() = runBlocking {
        val repository = AccountRepository(remote)
        answer = profile
        assertEquals("v1", (repository.save(" Ana ", "P", AvatarFile("image/png", byteArrayOf(1, 2, 3))) as Outcome.Ok).value.avatar_version)
        assertEquals("PATCH", requests.last().method); assertTrue(requests.last().body!!.contentType().toString().startsWith("multipart/form-data"))
        assertTrue(bodies.last().contains("name=\"first_name\"") && bodies.last().contains("filename=\"avatar.png\"") && bodies.last().contains("Content-Type: image/png"))
        repository.save("Ana", "P", null); assertFalse(bodies.last().contains("name=\"avatar\""))
        repository.removeAvatar(); assertTrue(json().get("avatar").isJsonNull)
        type = "image/png"; answer = "PNGDATA"
        assertEquals("PNGDATA", String((repository.avatar() as Outcome.Ok).value)); assertEquals("auth/profile/avatar/", path())
        type = "application/json"; answer = profile.replace("\"id\":1", "\"id\":2")
        assertTrue("el perfil de otra cuenta se rechaza", repository.profile() is Outcome.Fail)
        code = 400; answer = """{"avatar":["La imagen debe ocupar menos de 2 MB."]}"""
        assertEquals("La imagen debe ocupar menos de 2 MB.", (repository.save("Ana", "P", null) as Outcome.Fail).fields["avatar"])
    }

    @Test fun avatarLimitsMatchTheServer() {
        assertNull(avatarProblem("image/jpeg", 1000)); assertNull(avatarProblem("image/webp", AVATAR_LIMIT - 1))
        assertNotNull(avatarProblem("image/png", AVATAR_LIMIT)); assertNotNull(avatarProblem("image/gif", 10)); assertNotNull(avatarProblem(null, 10)); assertNotNull(avatarProblem("image/png", 0))
    }

    @Test fun deletionSendsOnlyThePasswordAndReportsAWrongOne() = runBlocking {
        val repository = AccountRepository(remote)
        code = 204
        assertTrue(repository.delete("clave") is Outcome.Ok)
        assertEquals("DELETE", requests.last().method); assertEquals("auth/account/", path()); assertEquals(setOf("password"), json().keySet())
        code = 403; answer = """{"detail":"La contraseña actual no coincide."}"""
        assertEquals("La contraseña actual no coincide.", (repository.delete("otra") as Outcome.Fail).message)
    }

    private val company = """{"id":5,"ticker":"MSFT","name":"Microsoft","sector":"technology","sector_display":"Tecnología","is_active":true,"is_pilot":true,"coverage":{"summary_fields":3,"summary_total":5,"fiscal_years":[2024],"price_as_of":"2026-11-20","sources":[],"has_data":true}}"""
    @Test fun catalogUsesServerFiltersAndPartialUpdates() = runBlocking {
        val catalog = CatalogRepository(remote)
        answer = """{"count":1,"results":[$company],"pilot":{"selected":15,"target":15,"selection_status":"proposal"},"market_provider":{"provider":"Yahoo Finance","calls":1,"limit":100}}"""
        val page = (catalog.list("ms", "active", "pilot", "loaded", 2) as Outcome.Ok).value
        assertEquals(listOf("ms", "active", "pilot", "loaded", "2"), listOf("q", "state", "scope", "coverage", "page").map(::query))
        assertEquals(15, page.pilot!!.selected); assertEquals(CoverageLevel.PARTIAL, coverageLevel(page.results!!.single().coverage))
        answer = company; catalog.update(5, mapOf("is_active" to false))
        assertEquals("admin/companies/5/", path()); assertEquals(setOf("is_active"), json().keySet())
        code = 400; answer = """{"website_url":["Utiliza una dirección HTTPS válida."]}"""
        assertEquals("Utiliza una dirección HTTPS válida.", (catalog.update(5, mapOf("website_url" to "x")) as Outcome.Fail).fields["website_url"])
    }

    @Test fun marketOperationsKeepTheFailedOperationInsideTheError() = runBlocking {
        val catalog = CatalogRepository(remote)
        answer = """{"operation":{"id":"op","status":"succeeded","kind":"refresh","ticker":"MSFT","message":"Listo"},"provider":{"calls":2,"limit":100}}"""
        assertEquals("succeeded", (catalog.refresh(5, "r-1") as Outcome.Ok).value.operation!!.status)
        assertEquals("admin/companies/5/refresh/", path()); assertEquals("r-1", json().get("request_id").asString)
        catalog.operation("op"); assertEquals("admin/market-operations/op/", path())
        catalog.verify("r-2", "SHOP", "technology"); assertEquals(setOf("request_id", "ticker", "sector"), json().keySet())
        catalog.create("op"); assertEquals("admin/companies/", path()); assertEquals("op", json().get("verification_id").asString)
        code = 429; answer = """{"operation":{"id":"op","status":"failed","message":"Se agotó el límite interno de consultas de hoy."},"provider":{"calls":100,"limit":100},"detail":"Se agotó el límite interno de consultas de hoy."}"""
        val failed = failedOperation(catalog.refresh(5, "r-3") as Outcome.Fail)!!
        assertEquals("failed", failed.operation!!.status); assertEquals(100, failed.provider!!.calls)
    }

    private fun notice(id: Int, read: String = "null") = """{"id":$id,"type":"rating_change","created_at":"2026-11-23T10:00:00Z","read_at":$read,"ticker":"MSFT","company_name":"Microsoft","before":{"category":"strong","label":"Muy sólido"},"after":{"category":"moderate","label":"Moderado"}}"""
    @Test fun noticesArePrivateOrderedAndPagedWithTheSameLimit() = runBlocking {
        val repository = NotificationsRepository(remote)
        answer = """{"count":2,"next":null,"results":[${notice(9)},${notice(4)}],"owner_id":"1","unread_count":2,"read_through":9}"""
        assertTrue(repository.list(1, null) is Outcome.Ok); assertNull(query("through_id"))
        repository.list(2, 9); assertEquals("9", query("through_id")); assertEquals("2", query("page"))
        answer = answer.replace("\"owner_id\":\"1\"", "\"owner_id\":\"2\"")
        assertTrue("avisos de otra cuenta", repository.list(1, null) is Outcome.Fail)
        answer = """{"results":[${notice(4)},${notice(9)}],"owner_id":"1","unread_count":2,"read_through":9}"""
        assertTrue("orden inestable", repository.list(1, null) is Outcome.Fail)
    }

    @Test fun readingPreferencesAndInstallationsFollowTheContract() = runBlocking {
        val repository = NotificationsRepository(remote)
        answer = """{"owner_id":"1","updated":1,"unread_count":0}"""
        repository.markRead(9); assertEquals(9, json().getAsJsonArray("ids").single().asInt)
        repository.markAll(9); assertEquals(setOf("all", "through_id"), json().keySet())
        code = 404; answer = """{"detail":"Aviso no encontrado."}"""
        assertEquals("Aviso no encontrado.", (repository.markRead(99) as Outcome.Fail).message)
        code = 200; answer = """{"owner_id":"1","version":3,"rating_changes":true,"channels":{"web":true,"email":false,"push":false},"available_channels":{"web":true,"email":false,"push":false},"channel_messages":{"push":"No disponible"}}"""
        assertEquals(3, (repository.save(PreferencesChange(2, true, Channels(web = true))) as Outcome.Ok).value.version)
        assertEquals("PUT", requests.last().method); assertEquals(setOf("version", "rating_changes", "channels"), json().keySet())
        code = 400; answer = """{"channels":{"push":["Activa este canal cuando esté disponible."]}}"""
        assertEquals("Activa este canal cuando esté disponible.", (repository.save(PreferencesChange(2, true, Channels(push = true))) as Outcome.Fail).fields.values.single())
        code = 200; answer = """{"owner_id":"1","installation_id":"abc","configured":true}"""
        assertTrue(repository.register("abc", "t", true) is Outcome.Ok); assertEquals(setOf("installation_id", "token", "permission_granted"), json().keySet())
        code = 204; assertTrue(repository.unregister("abc") is Outcome.Ok); assertEquals("user/push-installations/abc/", path())
    }

    @Test fun monitoringRejectsIncoherentFiguresAndHidesUnsafeContext() = runBlocking {
        val repository = MonitoringRepository(remote)
        answer = """{"schema_version":"ai-usage-v1","start":"2026-11-01","end":"2026-11-23","calls":0,"breakdown":[],"estimated_costs":[],"unpriced_calls":0}"""
        assertEquals(0, (repository.ai("2026-11-01", "2026-11-23", null, null) as Outcome.Ok).value.calls)
        assertTrue("otro periodo", repository.ai("2026-10-01", "2026-10-31", null, null) is Outcome.Fail)
        answer = """{"schema_version":"plan-distribution-v1","total":0,"blocked":0,"results":[{"plan":"free","count":0,"percentage":"0.00"},{"plan":"pro","count":0,"percentage":"0.00"}]}"""
        assertTrue(repository.plans() is Outcome.Ok)
        answer = answer.replace("\"total\":0", "\"total\":5"); assertTrue(repository.plans() is Outcome.Fail)
        answer = """{"schema_version":"monthly-subscription-value-v1","month":"2026-11","simulated":true,"plans_included":0,"results":[]}"""
        assertTrue(repository.monthly("2026-11") is Outcome.Ok)
        answer = answer.replace("\"simulated\":true", "\"simulated\":false"); assertTrue("nunca dinero cobrado", repository.monthly("2026-11") is Outcome.Fail)
        answer = """{"id":3,"created_at":"2026-11-23T10:00:00Z","level":"error","component":"api","code":"x","summary":"s","context":{"http_status":503,"retryable":true,"password":"p","token":"t"}}"""
        val log = (repository.error(3) as Outcome.Ok).value
        assertEquals(mapOf("Estado HTTP" to "503", "Admite reintento" to "Sí"), safeContext(log))
        answer = """{"count":0,"results":[],"retention_days":30}"""
        repository.errors("error", "email", 2); assertEquals(listOf("error", "email", "2"), listOf("level", "component", "page").map(::query))
        code = 409; answer = """{"detail":"El límite cambió. Actualiza antes de guardar."}"""
        assertEquals(409, (repository.saveBudget("local_ai", BudgetChange(1, 10, "UTC")) as Outcome.Fail).code)
    }
}
