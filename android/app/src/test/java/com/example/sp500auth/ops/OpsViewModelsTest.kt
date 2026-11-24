package com.example.sp500auth.ops

import androidx.lifecycle.SavedStateHandle
import com.example.sp500auth.data.api.models.UserProfile
import com.example.sp500auth.data.ops.*
import com.example.sp500auth.data.push.*
import com.example.sp500auth.ui.ops.*
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class OpsViewModelsTest {
    private fun exercise(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler)); try { block() } finally { Dispatchers.resetMain() }
    }
    private val offline = Outcome.Fail("Sin red")
    private val forbidden = Outcome.Fail("Sin permisos", 403)

    // ---- Épica 13
    private val ana = AdminUser(7, "ana", "Ana", "P", "a@example.test", "user", "free", true, "")
    private class Users(var rows: List<AdminUser>) : UsersSource {
        val lists = mutableListOf<String>(); val changes = mutableListOf<Map<String, Any>>()
        var list: Outcome<Page<AdminUser>>? = null; var update: Outcome<AdminUser>? = null
        override suspend fun list(q: String, plan: String, state: String, page: Int): Outcome<Page<AdminUser>> { lists += "$q/$plan/$state/$page"; return list ?: Outcome.Ok(Page(rows.size, null, null, rows)) }
        override suspend fun update(id: Long, change: Map<String, Any>): Outcome<AdminUser> { changes += change; delay(100); return update!! }
    }

    @Test fun usersSearchIsDebouncedAndFiltersSurviveRecreation() = exercise {
        val source = Users(listOf(ana)); val saved = SavedStateHandle(); val vm = UsersViewModel(source, saved)
        vm.search("a"); vm.search("an"); vm.search("ana"); advanceUntilIdle()
        assertEquals(listOf("ana/all/all/1"), source.lists)
        vm.filterPlan("pro"); vm.filterState("blocked"); advanceUntilIdle()
        val again = UsersViewModel(source, saved)
        assertEquals(listOf("ana", "pro", "blocked"), listOf(again.query, again.plan, again.state))
    }

    @Test fun networkErrorKeepsUsersAndLosingPermissionRemovesThem() = exercise {
        val source = Users(listOf(ana)); val vm = UsersViewModel(source, SavedStateHandle())
        vm.load(); advanceUntilIdle()
        source.list = offline; vm.load(); advanceUntilIdle()
        assertEquals(1, vm.users.value!!.results!!.size); assertNotNull(vm.users.error)
        source.list = forbidden; vm.load(); advanceUntilIdle()
        assertNull(vm.users.value); assertTrue(vm.users.error!!.forbidden)
        source.list = null; vm.load(); advanceUntilIdle(); assertNull(vm.users.error)
    }

    @Test fun roleChangeNeedsConfirmationAndIsSentOnce() = exercise {
        val source = Users(listOf(ana)); val vm = UsersViewModel(source, SavedStateHandle()); vm.load(); advanceUntilIdle()
        vm.propose(UserDraft.Role(ana, "admin")); vm.dismiss(); vm.confirm(); advanceUntilIdle()
        assertTrue(source.changes.isEmpty())
        source.update = Outcome.Ok(ana.copy(role = "admin"))
        vm.propose(UserDraft.Role(ana, "admin")); vm.confirm(); vm.confirm(); advanceUntilIdle()
        assertEquals(listOf(mapOf("role" to "admin")), source.changes)
        assertEquals("admin" to "free", vm.users.value!!.results!!.single().let { it.role to it.plan })
    }

    @Test fun lastAdministratorStaysUnchangedAndExplainsWhy() = exercise {
        val boss = ana.copy(role = "admin"); val source = Users(listOf(boss)); val vm = UsersViewModel(source, SavedStateHandle()); vm.load(); advanceUntilIdle()
        source.update = Outcome.Fail("Debe conservarse al menos un administrador activo.", 409)
        vm.propose(UserDraft.Role(boss, "user")); vm.confirm(); advanceUntilIdle()
        assertEquals("Debe conservarse al menos un administrador activo.", vm.draftError)
        assertEquals("admin", vm.users.value!!.results!!.single().role); assertNotNull(vm.draft)
    }

    @Test fun blockingRequiresAReason() = exercise {
        val source = Users(listOf(ana)); val vm = UsersViewModel(source, SavedStateHandle()); vm.load(); advanceUntilIdle()
        vm.propose(UserDraft.Block(ana)); vm.writeReason("   "); vm.confirm(); advanceUntilIdle()
        assertTrue(source.changes.isEmpty()); assertNotNull(vm.draftError)
        source.update = Outcome.Ok(ana.copy(is_active = false, blocked_reason = "Abuso"))
        vm.writeReason(" Abuso "); vm.confirm(); advanceUntilIdle()
        assertEquals(mapOf("is_active" to false, "reason" to "Abuso"), source.changes.single()); assertFalse(vm.users.value!!.results!!.single().is_active!!)
        source.update = Outcome.Ok(ana); vm.propose(UserDraft.Unblock(ana)); vm.confirm(); advanceUntilIdle()
        assertEquals(mapOf("is_active" to true), source.changes.last())
    }

    private class Account : AccountSource {
        var user: String? = "1"; var version: String? = null; var saveAnswer: Outcome<UserProfile>? = null; var deleteAnswer: Outcome<Unit> = Outcome.Ok(Unit)
        var photos = 0; val deletions = mutableListOf<String>(); val saved = mutableListOf<AvatarFile?>()
        fun profileOf(first: String = "Ana", role: String = "user") = UserProfile(user!!.toInt(), "ana", "a@example.test", first, "P", "retail", "", role, null, version)
        override fun owner() = user
        override suspend fun profile(): Outcome<UserProfile> = Outcome.Ok(profileOf())
        override suspend fun save(first: String, last: String, avatar: AvatarFile?): Outcome<UserProfile> { saved += avatar; delay(50); return saveAnswer ?: Outcome.Ok(profileOf(first)) }
        override suspend fun removeAvatar(): Outcome<UserProfile> { version = null; return Outcome.Ok(profileOf()) }
        override suspend fun avatar(): Outcome<ByteArray> { photos++; return Outcome.Ok(byteArrayOf(photos.toByte())) }
        override suspend fun delete(password: String): Outcome<Unit> { deletions += password; delay(50); return deleteAnswer }
    }

    @Test fun profileShowsConfirmedNameAndPhotoAndIsHiddenFromAnotherAccount() = exercise {
        val source = Account().apply { version = "v1" }; val vm = AccountViewModel(source)
        vm.load(); advanceUntilIdle(); vm.load(); advanceUntilIdle()
        assertEquals("la foto no se descarga dos veces", 1, source.photos); assertNotNull(vm.photo)
        vm.startEditing(); vm.writeFirst("María"); vm.pick("image/png", byteArrayOf(1)); source.version = "v2"
        vm.save(); vm.save(); advanceUntilIdle()
        assertEquals(1, source.saved.size); assertEquals("María", vm.profile.value!!.first_name); assertEquals(2, source.photos); assertNull(vm.picked)
        source.user = "2"
        assertNull(vm.profile.value); assertNull(vm.photo); assertFalse(vm.isAdmin)
    }

    @Test fun invalidImageKeepsTheTypedNames() = exercise {
        val source = Account(); val vm = AccountViewModel(source); vm.load(); advanceUntilIdle(); vm.startEditing(); vm.writeFirst("Nuevo")
        vm.pick("image/gif", byteArrayOf(1)); assertNull(vm.picked); assertNotNull(vm.errors["avatar"])
        vm.pick("image/png", null); assertNotNull(vm.errors["avatar"])
        source.saveAnswer = Outcome.Fail("Revisa los campos marcados.", 400, mapOf("avatar" to "Imagen no válida"))
        vm.pick("image/png", byteArrayOf(1)); vm.save(); advanceUntilIdle()
        assertEquals("Imagen no válida", vm.errors["avatar"]); assertEquals("Nuevo", vm.first); assertEquals("Ana", vm.profile.value!!.first_name)
    }

    @Test fun deletionNeedsPhraseAndPasswordAndOnlyEndsAfterSuccess() = exercise {
        val source = Account(); val vm = AccountViewModel(source); vm.load(); advanceUntilIdle()
        vm.deleteAccount("clave", "eliminar"); vm.deleteAccount("", DELETE_PHRASE); advanceUntilIdle()
        assertTrue(source.deletions.isEmpty())
        source.deleteAnswer = Outcome.Fail("La contraseña actual no coincide.", 403); vm.deleteAccount("mala", DELETE_PHRASE); advanceUntilIdle()
        assertFalse(vm.deleted); assertEquals("La contraseña actual no coincide.", vm.deleteError)
        source.deleteAnswer = offline; vm.deleteAccount("clave", DELETE_PHRASE); advanceUntilIdle(); assertFalse(vm.deleted)
        source.deleteAnswer = Outcome.Ok(Unit); vm.deleteAccount("clave", DELETE_PHRASE); vm.deleteAccount("clave", DELETE_PHRASE); advanceUntilIdle()
        assertTrue(vm.deleted); assertEquals(3, source.deletions.size)
    }

    // ---- Épica 14
    private val msft = AdminCompany(5, "MSFT", "Microsoft", "technology", "Tecnología", true, true, "Texto", "https://www.microsoft.com")
    private fun answerOf(status: String, message: String = status) = OperationAnswer(MarketOperation("op", status, "refresh", "MSFT", message = message), ProviderUsage("Yahoo", 1, 100))
    private fun failure(code: Int, text: String) = Outcome.Fail(text, code, body = JsonParser.parseString("""{"operation":{"id":"op","status":"failed","message":"$text"},"provider":{"calls":100,"limit":100}}""").asJsonObject)
    private inner class Catalog : CatalogSource {
        val lists = mutableListOf<String>(); val changes = mutableListOf<Map<String, Any>>(); val requests = mutableListOf<String>()
        var update: Outcome<AdminCompany> = Outcome.Ok(msft); var refresh: Outcome<OperationAnswer> = Outcome.Ok(answerOf("succeeded"))
        var poll: Outcome<OperationAnswer> = Outcome.Ok(answerOf("succeeded")); var verify: Outcome<OperationAnswer> = Outcome.Ok(answerOf("ready")); var create: Outcome<OperationAnswer> = Outcome.Ok(answerOf("succeeded"))
        var list: Outcome<CatalogPage>? = null
        override suspend fun list(q: String, state: String, scope: String, coverage: String, page: Int): Outcome<CatalogPage> { lists += "$q/$state/$scope/$coverage/$page"; return list ?: Outcome.Ok(CatalogPage(1, results = listOf(msft))) }
        override suspend fun update(id: Long, change: Map<String, Any>): Outcome<AdminCompany> { changes += change; delay(50); return update }
        override suspend fun refresh(id: Long, requestId: String): Outcome<OperationAnswer> { requests += requestId; delay(50); return refresh }
        override suspend fun operation(id: String) = poll
        override suspend fun verify(requestId: String, ticker: String, sector: String): Outcome<OperationAnswer> { requests += requestId; delay(50); return verify }
        override suspend fun create(verificationId: String): Outcome<OperationAnswer> { requests += "create-$verificationId"; delay(50); return create }
    }
    private fun ids(): () -> String { var n = 0; return { "id-${++n}" } }

    @Test fun activationIsConfirmedSentOnceAndReflected() = exercise {
        val source = Catalog().apply { update = Outcome.Ok(msft.copy(is_active = false)) }; val vm = CatalogViewModel(source, SavedStateHandle(), ids()); vm.load(); advanceUntilIdle()
        vm.proposeToggle(msft); vm.confirmToggle(); vm.confirmToggle(); advanceUntilIdle()
        assertEquals(listOf(mapOf("is_active" to false)), source.changes); assertFalse(vm.catalog.value!!.results!!.single().is_active!!)
        source.update = forbidden; vm.proposeToggle(msft); vm.confirmToggle(); advanceUntilIdle(); assertNull(vm.catalog.value)
    }

    @Test fun pilotAndCoverageNeverMeanFivePillars() = exercise {
        assertEquals(CoverageLevel.MISSING, coverageLevel(null)); assertEquals(CoverageLevel.MISSING, coverageLevel(Coverage(has_data = false)))
        assertEquals(CoverageLevel.PARTIAL, coverageLevel(Coverage(5, 5, has_data = true, price_as_of = "2026-11-20")))
        assertEquals(CoverageLevel.COMPLETE, coverageLevel(Coverage(5, 5, fiscal_years = listOf(2024), price_as_of = "2026-11-20", has_data = true)))
        assertEquals(3, coverageGaps(Coverage(0, 5)).size)
        val source = Catalog(); val vm = CatalogViewModel(source, SavedStateHandle(), ids()); vm.filterScope("pilot"); advanceUntilIdle()
        assertEquals("/all/pilot/all/1", source.lists.last())
    }

    @Test fun editionValidatesBeforeSendingOnlyTheChangedFields() = exercise {
        val source = Catalog(); val vm = CatalogViewModel(source, SavedStateHandle(), ids()); vm.load(); advanceUntilIdle()
        vm.startEdit(msft); vm.writeWebsite("http://inseguro.com"); vm.saveEdit(); assertNotNull(vm.errors["website_url"])
        vm.writeWebsite("https://www.microsoft.com"); vm.writeDescription("x".repeat(3001)); vm.saveEdit(); assertNotNull(vm.errors["description"])
        vm.writeDescription("Texto"); vm.writeSector("inventado"); vm.saveEdit(); assertNotNull(vm.errors["sector"]); assertTrue(source.changes.isEmpty())
        vm.writeSector("technology"); vm.writeDescription("Nuevo"); source.update = Outcome.Ok(msft.copy(description = "Nuevo")); vm.saveEdit(); advanceUntilIdle()
        assertEquals(listOf(mapOf("description" to "Nuevo")), source.changes); assertNull(vm.editing)
        vm.startEdit(msft); vm.cancelEdit(); vm.saveEdit(); advanceUntilIdle(); assertEquals(1, source.changes.size)
    }

    @Test fun refreshRetriesTheSameRequestAfterATimeoutAndANewOneAfterAFinalError() = exercise {
        val source = Catalog(); val saved = SavedStateHandle(); val vm = CatalogViewModel(source, saved, ids()); vm.load(); advanceUntilIdle()
        source.refresh = offline; vm.refresh(msft); vm.refresh(msft); advanceUntilIdle()
        assertEquals("sin duplicados", listOf("id-1"), source.requests); assertNotNull(vm.refreshOf(5)!!.error); assertNotNull(vm.catalog.value)
        source.refresh = failure(429, "Cuota agotada"); vm.refresh(msft); advanceUntilIdle()
        assertEquals("el reintento incierto conserva el identificador", "id-1", source.requests.last()); assertEquals("Cuota agotada", vm.refreshOf(5)!!.error); assertEquals(100, vm.provider!!.calls)
        source.refresh = Outcome.Ok(answerOf("running")); vm.refresh(msft); advanceUntilIdle()
        assertEquals("id-2", source.requests.last()); assertEquals("succeeded", vm.refreshOf(5)!!.operation!!.status); assertNull(vm.refreshOf(5)!!.error)
        source.refresh = failure(503, "Proveedor no disponible"); vm.refresh(msft); advanceUntilIdle(); assertEquals("Proveedor no disponible", vm.refreshOf(5)!!.error)
        assertEquals("los datos siguen visibles", 1, vm.catalog.value!!.results!!.size)
    }

    @Test fun newCompanyIsVerifiedReviewedAndCreatedOnce() = exercise {
        val source = Catalog(); val vm = AddCompanyViewModel(source, SavedStateHandle(), ids())
        vm.writeTicker("1x"); vm.verify(); assertEquals(setOf("ticker", "sector"), vm.errors.keys); vm.confirm(); assertTrue(source.requests.isEmpty())
        vm.writeTicker("shop"); vm.writeSector("technology"); assertEquals("SHOP", vm.ticker)
        source.verify = failure(404, "El ticker no existe en la fuente."); vm.verify(); advanceUntilIdle(); assertNull(vm.verified); assertEquals("El ticker no existe en la fuente.", vm.errors["form"])
        source.verify = Outcome.Fail("El ticker ya está registrado. Busca la empresa en el catálogo.", 409); vm.verify(); advanceUntilIdle(); assertTrue(vm.errors["form"]!!.contains("ya está registrado"))
        source.verify = Outcome.Ok(answerOf("ready")); vm.verify(); advanceUntilIdle(); assertNotNull(vm.verified)
        source.create = offline; vm.confirm(); advanceUntilIdle(); assertNotNull("una respuesta perdida se puede confirmar de nuevo", vm.verified); assertNull(vm.created)
        source.create = Outcome.Ok(answerOf("succeeded")); vm.confirm(); vm.confirm(); advanceUntilIdle(); assertNotNull(vm.created); assertEquals(2, source.requests.count { it == "create-op" })
        vm.writeTicker("otro"); assertNull("cambiar el ticker descarta la verificación", vm.created)
    }

    // ---- Épica 12
    private fun notice(id: Long, read: String? = null) = Notice(id, "rating_change", "2026-11-23T10:00:00Z", read, "MSFT", "Microsoft")
    private inner class Notices : NotificationsSource {
        var user: String? = "1"; val lists = mutableListOf<Pair<Int, Long?>>(); var pages = listOf(listOf(notice(9), notice(8)), listOf(notice(8), notice(3)))
        var unread = 3; var list: Outcome<NoticePage>? = null; var read: Outcome<ReadAnswer>? = null; val reads = mutableListOf<String>()
        val saves = mutableListOf<PreferencesChange>(); var save: Outcome<Preferences>? = null
        var stored = Preferences("1", 2, true, Channels(web = true), Channels(web = true, email = true), mapOf("push" to "No disponible"))
        override fun owner() = user
        override suspend fun list(page: Int, through: Long?): Outcome<NoticePage> { lists += page to through; return list ?: Outcome.Ok(NoticePage(4, if (page < pages.size) "next" else null, pages[page - 1], user, unread, 9)) }
        override suspend fun markRead(id: Long): Outcome<ReadAnswer> { reads += "$id"; delay(50); return read ?: Outcome.Ok(ReadAnswer(user, 1, --unread)) }
        override suspend fun markAll(through: Long): Outcome<ReadAnswer> { reads += "all-$through"; delay(50); return read ?: Outcome.Ok(ReadAnswer(user, unread, 0)) }
        override suspend fun preferences(): Outcome<Preferences> = Outcome.Ok(stored)
        override suspend fun save(change: PreferencesChange): Outcome<Preferences> { saves += change; delay(50); return save ?: Outcome.Ok(stored.copy(version = change.version + 1, rating_changes = change.rating_changes, channels = change.channels).also { stored = it }) }
    }

    @Test fun noticesPageWithoutRepeatingAndKeepStateOnErrors() = exercise {
        val source = Notices(); val vm = NotificationsViewModel(source); vm.refresh(); advanceUntilIdle()
        assertEquals(3, vm.unread); assertTrue(vm.hasMore)
        vm.loadMore(); vm.loadMore(); advanceUntilIdle()
        assertEquals(listOf(9L, 8L, 3L), vm.items.map { it.id }); assertEquals(listOf(1 to null, 2 to 9L), source.lists); assertFalse(vm.hasMore)
        source.list = offline; vm.refresh(); advanceUntilIdle(); assertEquals(3, vm.items.size); assertNotNull(vm.error)
        source.user = "2"; assertTrue(vm.items.isEmpty()); assertEquals(0, vm.unread)
    }

    @Test fun emptyCentreIsNotAnError() = exercise {
        val source = Notices().apply { pages = listOf(emptyList()); unread = 0 }; val vm = NotificationsViewModel(source); vm.refresh(); advanceUntilIdle()
        assertTrue(vm.loaded && vm.items.isEmpty() && vm.error == null)
    }

    @Test fun readingUsesTheConfirmedCounterAndIgnoresDoubleTaps() = exercise {
        val source = Notices(); val vm = NotificationsViewModel(source); vm.refresh(); advanceUntilIdle()
        vm.markRead(9); vm.markRead(9); assertTrue("nada cambia antes de confirmar", vm.items.first().unread); advanceUntilIdle()
        assertEquals(listOf("9"), source.reads); assertEquals(2, vm.unread); assertFalse(vm.items.first().unread)
        vm.markRead(9); advanceUntilIdle(); assertEquals(1, source.reads.size)
        source.read = Outcome.Fail("Aviso no encontrado.", 404); vm.markRead(8); advanceUntilIdle(); assertEquals("Aviso no encontrado.", vm.message); assertTrue(vm.items[1].unread)
        source.read = offline; vm.markAll(); advanceUntilIdle(); assertEquals("un fallo no se muestra como éxito", 2, vm.unread)
        source.read = null; vm.markAll(); vm.markAll(); advanceUntilIdle(); assertEquals(0, vm.unread); assertTrue(vm.items.none { it.unread }); assertEquals(1, source.reads.count { it == "all-9" } - 1)
    }

    @Test fun preferencesSaveAutomaticallyAndNeverEnableAnUnavailableChannel() = exercise {
        val source = Notices(); val vm = PreferencesViewModel(source); vm.load(); advanceUntilIdle()
        vm.changeRating(false); assertEquals(SaveState.SAVING, vm.save); vm.changeChannels(vm.channels.copy(email = true, push = true)); advanceUntilIdle()
        assertEquals(SaveState.SAVED, vm.save); assertEquals(listOf(2, 3), source.saves.map { it.version })
        assertEquals(PreferencesChange(3, false, Channels(web = true, email = true, push = false)), source.saves.last())
        source.save = offline; vm.changeRating(true); advanceUntilIdle(); assertEquals(SaveState.FAILED, vm.save); assertTrue("conserva la selección", vm.rating)
        source.save = null; vm.retry(); advanceUntilIdle(); assertEquals(SaveState.SAVED, vm.save)
        val reopened = PreferencesViewModel(source); reopened.load(); advanceUntilIdle(); assertTrue(reopened.rating && reopened.channels.email)
        source.user = "2"; val count = source.saves.size; vm.changeRating(false); advanceUntilIdle(); assertEquals(count, source.saves.size); assertEquals(SaveState.FAILED, vm.save)
    }

    private class Memory : PushMemory { override val installationId = "phone"; override var linkedOwner: String? = null; override var wanted = true }
    private class Installations : PushInstallationsSource {
        var user: String? = "1"; var configured = true; var fail: Outcome.Fail? = null; val calls = mutableListOf<String>()
        override fun owner() = user
        override suspend fun register(id: String, token: String, granted: Boolean): Outcome<InstallationAnswer> { calls += "$user:$id:$token:$granted"; return fail ?: Outcome.Ok(InstallationAnswer(user, id, configured)) }
        override suspend fun unregister(id: String): Outcome<Unit> { calls += "$user:off:$id"; return Outcome.Ok(Unit) }
    }

    @Test fun pushLinksRenewsAndUnlinksByAccount() = exercise {
        val source = Installations(); val memory = Memory(); val link = PushLink(source, memory)
        assertTrue(link.sync("t1", true) is Outcome.Ok); assertTrue(link.linked())
        link.sync("t2", false); assertEquals("1:phone:t2:false", source.calls.last())
        source.user = "2"; assertFalse("otra cuenta no hereda el vínculo", link.linked())
        source.user = "1"; link.detach(); assertEquals("1:off:phone", source.calls.last()); assertNull(memory.linkedOwner)
        val count = source.calls.size; link.detach(); assertEquals(count, source.calls.size)
        source.configured = false; assertTrue("no se aparenta un envío disponible", link.sync("t3", true) is Outcome.Fail)
        memory.linkedOwner = null; source.fail = offline; link.sync("t4", true); assertFalse(link.linked())
        source.user = null; assertEquals(401, (link.sync("t5", true) as Outcome.Fail).code)
    }

    @Test fun pushOnlyAcceptsATickerAndItsOwner() {
        assertEquals(PushTarget("MSFT", "1", 9), pushTarget("MSFT", "1", "9")); assertEquals(PushTarget("BRK.B", "1", null), pushTarget("BRK.B", "1", "x"))
        assertNull(pushTarget("../admin", "1", "9")); assertNull(pushTarget("MSFT", null, "9")); assertNull(pushTarget("MSFT", "0", "9")); assertNull(pushTarget(null, "1", "9"))
    }

    // ---- Épica 15
    private class Monitoring : MonitoringSource {
        val calls = mutableListOf<String>(); var aiAnswer: Outcome<AiUsage>? = null; var saveAnswer: Outcome<Budget>? = null; var logs: Outcome<ErrorPage>? = null
        override suspend fun ai(start: String?, end: String?, provider: String?, model: String?): Outcome<AiUsage> { calls += "ai/$start/$end/$model"; return aiAnswer ?: Outcome.Ok(AiUsage("ai-usage-v1", start ?: "2026-11-01", end ?: "2026-11-23", calls = 0, breakdown = listOf(AiRow("local_ai", "qwen", 0)))) }
        override suspend fun plans(): Outcome<PlanDistribution> = Outcome.Ok(PlanDistribution("plan-distribution-v1", total = 0, results = emptyList()))
        override suspend fun budgets(): Outcome<Budgets> { calls += "budgets"; return Outcome.Ok(Budgets("provider-budgets-v1", listOf(Budget("local_ai", 100, 85, 4, "85.00", status = "warning", timezone = "UTC")))) }
        override suspend fun saveBudget(provider: String, change: BudgetChange): Outcome<Budget> { calls += "save/${change.version}/${change.daily_limit}"; delay(50); return saveAnswer ?: Outcome.Ok(Budget(provider, change.daily_limit, 85, change.version + 1, "42.50", status = "normal")) }
        override suspend fun monthly(month: String): Outcome<MonthlyValue> { calls += "month/$month"; return Outcome.Ok(MonthlyValue("v", month, simulated = true, results = emptyList())) }
        override suspend fun errors(level: String, component: String, page: Int): Outcome<ErrorPage> { calls += "errors/$level/$component/$page"; return logs ?: Outcome.Ok(ErrorPage(0, results = emptyList())) }
        override suspend fun error(id: Long): Outcome<ErrorLog> = Outcome.Ok(ErrorLog(id, "2026-11-23T10:00:00Z", "error"))
    }
    private val today = LocalDate.of(2026, 11, 23)

    @Test fun aiPeriodsAndRejectionDoNotShowOldFigures() = exercise {
        val source = Monitoring(); val vm = MonitoringViewModel(source, SavedStateHandle()) { today.plusDays(1) } // phone already in the next day
        vm.reload(); advanceUntilIdle(); assertEquals("ai/null/null/null", source.calls.last()); assertEquals(0, vm.ai.value!!.calls); assertEquals(listOf("qwen"), vm.models)
        vm.choosePeriod(AiPeriod.TODAY); assertNull("otro periodo no conserva cifras", vm.ai.value); advanceUntilIdle(); assertEquals("ai/2026-11-23/2026-11-23/null", source.calls.last())
        vm.chooseModel("qwen"); advanceUntilIdle(); assertTrue(source.calls.last().endsWith("/qwen"))
        source.aiAnswer = Outcome.Fail("Sin permisos", 403); vm.reload(); advanceUntilIdle(); assertNull(vm.ai.value); assertTrue(vm.ai.error!!.forbidden)
        assertEquals(today.minusDays(6) to today, aiRange(AiPeriod.WEEK, today))
    }

    @Test fun budgetStatesComeFromTheServer() {
        assertEquals(listOf("Normal", "Advertencia", "Agotado", "Sin estado"), listOf("normal", "warning", "exhausted", null).map(::budgetLabel))
        assertTrue(validBudget(Budget("local_ai", 100, 84, 1, "84.00", status = "normal"))); assertTrue(validBudget(Budget("local_ai", 100, 85, 1, "85.00", status = "warning"))); assertTrue(validBudget(Budget("local_ai", 100, 100, 1, "100.00", status = "exhausted")))
        assertFalse(validBudget(Budget("local_ai", 0, 1, 1, status = "normal"))); assertFalse(validBudget(Budget("local_ai", 10, 1, 1, status = "otro")))
    }

    @Test fun budgetLimitUsesItsVersionOnceAndReloadsOnConflict() = exercise {
        val source = Monitoring(); val vm = MonitoringViewModel(source, SavedStateHandle()) { today }; vm.open(2); advanceUntilIdle()
        val row = vm.budgets.value!!.results!!.single()
        vm.edit(row); vm.writeLimit("0"); vm.saveLimit(); assertNotNull(vm.budgetError)
        vm.writeLimit("200x"); vm.saveLimit(); vm.saveLimit(); advanceUntilIdle()
        assertEquals(1, source.calls.count { it.startsWith("save/") }); assertEquals("save/4/200", source.calls.last()); assertEquals("normal", vm.budgets.value!!.results!!.single().status)
        source.saveAnswer = Outcome.Fail("El límite cambió. Actualiza antes de guardar.", 409); vm.edit(row); vm.saveLimit(); advanceUntilIdle()
        assertEquals("El límite cambió. Actualiza antes de guardar.", vm.budgetError); assertEquals("budgets", source.calls.last())
    }

    @Test fun monthNeverGoesToTheFutureAndFiltersRestartPages() = exercise {
        val source = Monitoring(); val saved = SavedStateHandle(); val vm = MonitoringViewModel(source, saved) { today }
        vm.open(3); vm.moveMonth(1); advanceUntilIdle(); assertEquals("2026-11", vm.month); assertFalse(vm.canGoNext)
        vm.moveMonth(-1); advanceUntilIdle(); assertEquals("month/2026-10", source.calls.last()); assertTrue(vm.canGoNext)
        vm.open(4); vm.goLogs(3); vm.chooseLevel("error"); vm.chooseComponent("email"); advanceUntilIdle(); assertEquals("errors/error/email/1", source.calls.last())
        vm.openLog(3); advanceUntilIdle(); assertEquals(3L, vm.detail!!.value!!.id); vm.closeLog(); assertNull(vm.detail)
        val again = MonitoringViewModel(source, saved) { today }; assertEquals(listOf(4, "error", "email"), listOf(again.tab, again.level, again.component))
    }

    @Test fun datesAreNeverInvented() {
        assertEquals("Sin fecha", displayDateTime(null)); assertEquals("Sin fecha", displayDateTime("ayer")); assertEquals("Sin fecha", displayDay(""))
        assertTrue(displayDateTime("2026-11-23T10:00:00.123456-05:00", ZoneOffset.UTC).contains("15:00"))
    }
}
