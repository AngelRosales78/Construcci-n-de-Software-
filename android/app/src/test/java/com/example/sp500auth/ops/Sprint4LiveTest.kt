package com.example.sp500auth.ops

import com.example.sp500auth.data.api.AuthApiService
import com.example.sp500auth.data.api.models.LoginRequest
import com.example.sp500auth.data.api.models.RefreshRequest
import com.example.sp500auth.data.ops.*
import com.google.gson.JsonParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

/**
 * Real integration with the shared Sprint 4 server. Runs only when S4_BASE_URL and S4_ACCESS (a local file with
 * trial accounts, outside Git) are set; otherwise it is skipped and must not be counted as passed.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class Sprint4LiveTest {
    private val base = System.getenv("S4_BASE_URL").orEmpty()
    private val file = System.getenv("S4_ACCESS").orEmpty()
    private val access get() = JsonParser.parseString(File(file).readText()).asJsonObject
    private val retrofit by lazy { Retrofit.Builder().baseUrl(base).addConverterFactory(GsonConverterFactory.create()).build() }
    private val auth by lazy { retrofit.create(AuthApiService::class.java) }

    @Before fun configured() = assumeTrue(base.isNotBlank() && File(file).isFile)

    private inner class Session(val name: String) {
        var token: String? = null; var refresh: String? = null
        val remote = OpsRemote(retrofit) { token }
        suspend fun open(): Int {
            val answer = auth.login(LoginRequest(name, access.get(name).asString))
            token = answer.body()?.accessToken; refresh = answer.body()?.refreshToken
            return answer.code()
        }
    }
    private fun session(name: String) = runBlocking { Session(name).also { assertEquals(200, it.open()) } }
    private fun <T> ok(outcome: Outcome<T>): T = (outcome as? Outcome.Ok)?.value ?: throw AssertionError((outcome as Outcome.Fail).let { "${it.code} ${it.message} ${it.fields}" })
    private fun <T> fail(outcome: Outcome<T>) = outcome as? Outcome.Fail ?: throw AssertionError("Se esperaba un rechazo")

    @Test fun t13_usersPagesFiltersAndPermissions() = runBlocking {
        val users = UsersRepository(session("s4_admin").remote)
        val first = ok(users.list("", "all", "all", 1)); assertEquals(20, first.results!!.size); assertNotNull(first.next)
        val second = ok(users.list("", "all", "all", 2)); assertTrue(first.results!!.map { it.id }.intersect(second.results!!.map { it.id }.toSet()).isEmpty())
        assertEquals("s4_user", ok(users.list("s4_user", "pro", "active", 1)).results!!.single().username)
        assertTrue(ok(users.list("nadie-con-este-nombre", "all", "all", 1)).results!!.isEmpty())
        assertEquals(404, fail(users.list("", "all", "all", 99)).code)
        assertTrue(fail(UsersRepository(session("s4_user").remote).list("", "all", "all", 1)).forbidden)
    }

    @Test fun t13_roleAndPlanAreIndependent() = runBlocking {
        val admin = session("s4_admin"); val users = UsersRepository(admin.remote)
        val target = ok(users.list("s4_page_00", "all", "all", 1)).results!!.single()
        val pro = ok(users.update(target.id!!, mapOf("plan" to "pro"))); assertEquals("user" to "pro", pro.role to pro.plan)
        assertTrue("Pro no da permisos", fail(UsersRepository(session("s4_user").remote).update(target.id!!, mapOf("plan" to "free"))).forbidden)
        assertEquals("free", ok(users.update(target.id!!, mapOf("plan" to "free"))).plan)
        val role = ok(users.update(target.id!!, mapOf("role" to "admin"))); assertEquals("admin" to "free", role.role to role.plan)
        assertEquals("user", ok(users.update(target.id!!, mapOf("role" to "user"))).role)
        assertEquals("Indica un motivo para bloquear la cuenta.", fail(users.update(target.id!!, mapOf("is_active" to false))).fields["reason"])
    }

    @Test fun t13_lastAdministratorIsProtected() = runBlocking {
        val admin = session("s4_admin"); val users = UsersRepository(admin.remote)
        val second = ok(users.list("s4_second_admin", "all", "all", 1)).results!!.single(); val me = ok(users.list("s4_admin", "all", "all", 1)).results!!.single()
        ok(users.update(second.id!!, mapOf("role" to "user")))
        val rejected = fail(users.update(me.id!!, mapOf("role" to "user"))); assertEquals(409, rejected.code); assertTrue(rejected.message.contains("administrador"))
        assertEquals("admin", ok(users.update(second.id!!, mapOf("role" to "admin"))).role)
    }

    @Test fun t13_blockingRevokesSessionsAndUnblockingNeedsANewLogin() = runBlocking {
        val users = UsersRepository(session("s4_admin").remote); val blocked = session("s4_blocked")
        val target = ok(users.list("s4_blocked", "all", "all", 1)).results!!.single()
        assertFalse(ok(users.update(target.id!!, mapOf("is_active" to false, "reason" to "Prueba"))).is_active!!)
        assertEquals(401, fail(AccountRepository(blocked.remote).profile()).code)
        assertEquals(401, auth.refreshAccessToken(RefreshRequest(blocked.refresh!!)).code())
        assertEquals(401, Session("s4_blocked").open())
        assertTrue(ok(users.update(target.id!!, mapOf("is_active" to true))).is_active!!)
        assertEquals("desbloquear no recupera la sesión", 401, fail(AccountRepository(blocked.remote).profile()).code)
        assertEquals(200, Session("s4_blocked").open())
    }

    /** Minimal valid PNG built without Android classes. */
    private fun png(): ByteArray {
        val out = ByteArrayOutputStream(); val data = java.io.DataOutputStream(out)
        data.write(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
        fun chunk(name: String, body: ByteArray) {
            val bytes = name.toByteArray() + body
            data.writeInt(body.size); data.write(bytes); data.writeInt(java.util.zip.CRC32().also { it.update(bytes) }.value.toInt())
        }
        chunk("IHDR", ByteArrayOutputStream().also { java.io.DataOutputStream(it).apply { writeInt(32); writeInt(32); write(byteArrayOf(8, 2, 0, 0, 0)) } }.toByteArray())
        chunk("IDAT", ByteArrayOutputStream().also { raw -> java.util.zip.DeflaterOutputStream(raw).use { it.write(ByteArray(32 * (1 + 32 * 3))) } }.toByteArray())
        chunk("IEND", ByteArray(0))
        return out.toByteArray()
    }

    @Test fun t13_profileAndPrivatePhoto() = runBlocking {
        val account = AccountRepository(session("s4_user").remote)
        val saved = ok(account.save("Nombre", "Apellido", AvatarFile("image/png", png())))
        assertEquals("Nombre", saved.first_name); assertNotNull(saved.avatar_version)
        assertEquals("PNG", String(ok(account.avatar()).copyOfRange(1, 4)))
        val rejected = fail(account.save("Otro", "Nombre", AvatarFile("image/png", "no es imagen".toByteArray()))); assertNotNull(rejected.fields["avatar"])
        assertEquals("una imagen inválida no cambia nombres", "Nombre", ok(account.profile()).first_name)
        assertEquals("otra cuenta no ve la foto", 404, fail(AccountRepository(session("s4_admin").remote).avatar()).code)
        assertNull(ok(account.removeAvatar()).avatar_version)
    }

    @Test fun t13_zzAccountDeletionOnlyWithTheRightPassword() = runBlocking {
        val disposable = Session("s4_disposable"); assumeTrue("la cuenta desechable ya se usó", disposable.open() == 200)
        val account = AccountRepository(disposable.remote)
        assertEquals("La contraseña actual no coincide.", fail(account.delete("incorrecta")).message); ok(account.profile())
        ok(account.delete(access.get("s4_disposable").asString))
        assertEquals(401, fail(account.profile()).code); assertEquals(401, Session("s4_disposable").open())
    }

    @Test fun t14_activationHidesFromSearchAndDescriptiveEditionPersists() = runBlocking {
        val admin = session("s4_admin"); val catalog = CatalogRepository(admin.remote)
        val company = ok(catalog.list("ZTS", "all", "all", "all", 1)).results!!.single()
        assertFalse(ok(catalog.update(company.id!!, mapOf("is_active" to false))).is_active!!)
        assertEquals("ZTS", ok(catalog.list("ZTS", "inactive", "all", "all", 1)).results!!.single().ticker)
        val search = retrofit.create(com.example.sp500auth.data.api.SprintApiService::class.java)
        ok(catalog.update(company.id!!, mapOf("is_active" to true))); ok(catalog.update(company.id!!, mapOf("is_active" to false)))
        val client = okhttp3.OkHttpClient()
        fun found() = client.newCall(okhttp3.Request.Builder().url(base + "companies/search/?q=ZTS").header("Authorization", "Bearer ${admin.token}").build()).execute().use { it.body!!.string().contains("\"ZTS\"") }
        assertNotNull(search); assertFalse("inactiva no aparece", found())
        ok(catalog.update(company.id!!, mapOf("is_active" to true))); assertTrue(found())
        assertNotNull(fail(catalog.update(company.id!!, mapOf("website_url" to "http://inseguro.test"))).fields["website_url"])
        assertNotNull(fail(catalog.update(company.id!!, mapOf("sector" to "inventado"))).fields["sector"])
        assertNotNull(fail(catalog.update(company.id!!, mapOf("description" to "x".repeat(3001)))).fields["description"])
        assertEquals("Salud animal", ok(catalog.update(company.id!!, mapOf("description" to "Salud animal", "website_url" to "https://www.zoetis.com"))).description)
        assertTrue(fail(CatalogRepository(session("s4_user").remote).update(company.id!!, mapOf("description" to "x"))).forbidden)
    }

    @Test fun t14_pilotDeclaresRealCoverage() = runBlocking {
        val page = ok(CatalogRepository(session("s4_admin").remote).list("", "all", "pilot", "all", 1))
        assertTrue(page.pilot!!.selected!! in 3..15); assertEquals(15, page.pilot!!.target); assertEquals("proposal", page.pilot!!.selection_status)
        assertEquals(page.pilot!!.selected, page.count); assertTrue("la cobertura se declara aparte", page.results!!.all { it.is_pilot == true && it.coverage != null })
        assertTrue(fail(CatalogRepository(session("s4_user").remote).list("", "all", "pilot", "all", 1)).forbidden)
    }

    @Test fun t14_priceUpdateIsIdempotentOrKeepsDataWhenTheProviderFails() = runBlocking {
        val catalog = CatalogRepository(session("s4_admin").remote)
        val company = ok(catalog.list("KO", "all", "all", "all", 1)).results!!.single { it.ticker == "KO" }
        val id = UUID.randomUUID().toString()
        fun operation(outcome: Outcome<OperationAnswer>) = ((outcome as? Outcome.Ok)?.value ?: failedOperation(outcome as Outcome.Fail))!!.operation!!
        val first = operation(catalog.refresh(company.id!!, id)); val again = operation(catalog.refresh(company.id!!, id))
        println("S4 refresh KO: ${first.status} ${first.error_code} ${first.message}")
        assertEquals("el mismo identificador no repite la consulta", first.id, again.id); assertTrue(first.status in setOf("succeeded", "failed"))
        if (first.status == "succeeded") assertNotNull(ok(catalog.list("KO", "all", "all", "all", 1)).results!!.first { it.ticker == "KO" }.coverage!!.price_as_of)
    }

    @Test fun t14_tickerIsVerifiedBeforeBeingAdded() = runBlocking {
        val catalog = CatalogRepository(session("s4_admin").remote)
        assertEquals(409, fail(catalog.verify(UUID.randomUUID().toString(), "MSFT", "technology")).code)
        delay(1200)
        val missing = fail(catalog.verify(UUID.randomUUID().toString(), "ZZZZQQ", "technology")); println("S4 ticker inexistente: ${missing.code} ${missing.message}")
        delay(1200)
        val answer = catalog.verify(UUID.randomUUID().toString(), "SHOP", "technology")
        println("S4 verificar SHOP: " + ((answer as? Outcome.Ok)?.value?.operation?.let { "${it.status} ${it.text("name")}" } ?: (answer as Outcome.Fail).message))
        if (answer is Outcome.Ok) {
            assertEquals("ready", answer.value.operation!!.status)
            val created = ok(catalog.create(answer.value.operation!!.id!!)); assertEquals("succeeded", created.operation!!.status)
            assertEquals("repetir no duplica", created.operation!!.company_id, ok(catalog.create(answer.value.operation!!.id!!)).operation!!.company_id)
            assertEquals(1, ok(catalog.list("SHOP", "all", "all", "all", 1)).results!!.size)
        }
        assertEquals(404, fail(catalog.create(UUID.randomUUID().toString())).code)
    }

    @Test fun t12_noticesPagesReadingAndPrivacy() = runBlocking {
        val mine = NotificationsRepository(session("s4_admin").remote); val other = NotificationsRepository(session("s4_user").remote)
        val first = ok(mine.list(1, null)); assertEquals(10, first.results!!.size); assertNotNull(first.next)
        assertEquals("Muy sólido" to "Moderado", first.results!!.first().let { it.before!!.label to it.after!!.label })
        val second = ok(mine.list(2, first.read_through)); assertTrue(first.results!!.map { it.id }.intersect(second.results!!.map { it.id }.toSet()).isEmpty()); assertNull(second.next)
        assertTrue("otra cuenta no lee mis avisos", ok(other.list(1, null)).results!!.isEmpty())
        val id = first.results!!.first().id!!
        assertEquals(404, fail(other.markRead(id)).code)
        val before = first.unread_count!!
        assertTrue(ok(mine.markRead(id)).unread_count!! <= before); val repeated = ok(mine.markRead(id)); assertEquals(0, repeated.updated)
        assertEquals(0, ok(mine.markAll(first.read_through!!)).unread_count); assertEquals(0, ok(mine.list(1, null)).unread_count)
    }

    @Test fun t12_preferencesPersistByAccountAndPushIsNotPretended() = runBlocking {
        val mine = NotificationsRepository(session("s4_user").remote); val start = ok(mine.preferences())
        val saved = ok(mine.save(PreferencesChange(start.version!!, !start.rating_changes!!, Channels(web = true))))
        assertEquals(saved.rating_changes, ok(mine.preferences()).rating_changes)
        assertEquals(409, fail(mine.save(PreferencesChange(start.version!!, true, Channels(web = false)))).code)
        assertFalse(start.available_channels!!.push); assertEquals(400, fail(mine.save(PreferencesChange(saved.version!!, true, Channels(push = true)))).code)
        ok(mine.save(PreferencesChange(saved.version!!, start.rating_changes!!, Channels(web = true))))
        assertTrue("cada cuenta tiene las suyas", ok(NotificationsRepository(session("s4_admin").remote).preferences()).owner_id != start.owner_id)
        val phone = UUID.randomUUID().toString()
        assertEquals("sin credencial de servidor no se aparenta Push", false, ok(mine.register(phone, "token-de-prueba-no-real-000", true)).configured); ok(mine.unregister(phone))
    }

    @Test fun t15_monitoringIsCoherentAndPrivate() = runBlocking {
        val admin = MonitoringRepository(session("s4_admin").remote); val normal = MonitoringRepository(session("s4_user").remote)
        val today = java.time.LocalDate.now().toString()
        val ai = ok(admin.ai(today, today, null, null)); assertEquals(0, ai.calls); assertTrue(ai.estimated_costs!!.isEmpty())
        val plans = ok(admin.plans()); assertEquals(plans.total, plans.results!!.sumOf { it.count!! }); assertTrue(plans.results!!.first { it.plan == "pro" }.count!! >= 1)
        val budgets = ok(admin.budgets()); assertEquals(2, budgets.results!!.size)
        val row = budgets.results!!.first { it.provider == "local_ai" }; val changed = ok(admin.saveBudget("local_ai", BudgetChange(row.version!!, 77, row.timezone!!))); assertEquals(77, changed.limit)
        assertEquals(409, fail(admin.saveBudget("local_ai", BudgetChange(row.version!!, 50, row.timezone!!))).code); ok(admin.saveBudget("local_ai", BudgetChange(changed.version!!, row.limit!!, row.timezone!!)))
        val month = java.time.YearMonth.now().toString(); val value = ok(admin.monthly(month)); assertEquals(true, value.simulated); assertEquals("una asignación administrativa no suma importe", 0, value.plans_included)
        val logs = ok(admin.errors("all", "all", 1)); assertEquals(15, logs.results!!.size); assertNotNull(logs.next)
        assertTrue(ok(admin.errors("warning", "email", 1)).results!!.all { it.level == "warning" && it.component == "email" })
        assertEquals(mapOf("Estado HTTP" to "503"), safeContext(ok(admin.error(logs.results!!.last().id!!))))
        listOf(normal.ai(today, today, null, null), normal.plans(), normal.budgets(), normal.monthly(month), normal.errors("all", "all", 1)).forEach { assertTrue(fail(it).forbidden) }
    }
}
