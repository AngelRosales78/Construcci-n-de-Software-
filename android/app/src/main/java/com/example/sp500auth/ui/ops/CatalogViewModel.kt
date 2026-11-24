package com.example.sp500auth.ui.ops

import androidx.compose.runtime.*
import androidx.lifecycle.*
import com.example.sp500auth.data.ops.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import java.net.URI
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

/** Progress of one market consultation. `requestId` survives uncertain answers so a retry never starts a second one. */
data class RefreshState(val requestId: String, val running: Boolean = false, val operation: MarketOperation? = null, val error: String? = null)

/** HU14_04: same limits the server applies. */
fun companyProblems(description: String, sector: String, website: String): Map<String, String> = buildMap {
    if (description.length > 3000) put("description", "La descripción admite hasta 3000 caracteres (${description.length}).")
    if (sector !in catalogSectors) put("sector", "Elige un sector del catálogo.")
    val site = website.trim()
    if (site.length > 500) put("website_url", "La dirección admite hasta 500 caracteres.")
    else if (site.isNotEmpty() && runCatching { URI(site).run { scheme.equals("https", true) && host.orEmpty().contains('.') } }.getOrDefault(false).not())
        put("website_url", "Utiliza una dirección HTTPS válida, por ejemplo https://www.empresa.com")
}

/** HU14_01 to HU14_04. */
@HiltViewModel
class CatalogViewModel internal constructor(
    private val source: CatalogSource, private val saved: SavedStateHandle, private val newId: () -> String
) : ViewModel() {
    @Inject constructor(repository: CatalogRepository, saved: SavedStateHandle) : this(repository as CatalogSource, saved, { UUID.randomUUID().toString() })

    var query by mutableStateOf(saved.get<String>("q") ?: ""); private set
    var state by mutableStateOf(saved.get<String>("state") ?: "all"); private set
    var scope by mutableStateOf(saved.get<String>("scope") ?: "all"); private set
    var coverage by mutableStateOf(saved.get<String>("coverage") ?: "all"); private set
    var page by mutableStateOf(saved.get<Int>("page") ?: 1); private set
    var catalog by mutableStateOf(Load<CatalogPage>()); private set
    var provider by mutableStateOf<ProviderUsage?>(null); private set

    var toggling by mutableStateOf<AdminCompany?>(null); private set
    var editing by mutableStateOf<AdminCompany?>(null); private set
    var description by mutableStateOf(""); private set
    var sector by mutableStateOf(""); private set
    var website by mutableStateOf(""); private set
    var saving by mutableStateOf(false); private set
    var errors by mutableStateOf<Map<String, String>>(emptyMap()); private set
    var notice by mutableStateOf<String?>(null); private set
    private val refreshes = mutableStateMapOf<Long, RefreshState>()
    private var job: Job? = null

    fun refreshOf(id: Long?): RefreshState? = id?.let { refreshes[it] }
    fun search(text: String) { query = text.take(120); saved["q"] = query; go(1, 350) }
    fun filterState(value: String) { state = value; saved["state"] = value; go(1) }
    fun filterScope(value: String) { scope = value; saved["scope"] = value; go(1) }
    fun filterCoverage(value: String) { coverage = value; saved["coverage"] = value; go(1) }
    fun go(to: Int, wait: Long = 0) { page = maxOf(1, to); saved["page"] = page; load(wait) }

    fun load(wait: Long = 0) {
        job?.cancel()
        catalog = catalog.started()
        job = viewModelScope.launch {
            if (wait > 0) delay(wait)
            val answer = source.list(query, state, scope, coverage, page)
            ensureActive()
            if (answer is Outcome.Fail && answer.code == 404 && answer.message != SERVICE_MISSING && page > 1) { go(1); return@launch }
            catalog = catalog.with(answer)
            (answer as? Outcome.Ok)?.value?.market_provider?.let { provider = it }
        }
    }

    private fun replace(company: AdminCompany) {
        catalog = catalog.copy(value = catalog.value?.let { data -> data.copy(results = data.results?.map { if (it.id == company.id) company else it }) })
    }

    fun proposeToggle(company: AdminCompany) { if (!saving) { toggling = company; errors = emptyMap(); notice = null } }
    fun dismissToggle() { if (!saving) toggling = null }
    fun confirmToggle() {
        val company = toggling ?: return
        patch(company, mapOf("is_active" to (company.is_active != true))) { confirmed ->
            toggling = null
            notice = if (confirmed.is_active == true) "${confirmed.ticker} vuelve a aparecer en las búsquedas." else "${confirmed.ticker} ya no aparece en las búsquedas. Su ficha y sus datos se conservan."
        }
    }

    fun startEdit(company: AdminCompany) {
        if (saving) return
        editing = company; errors = emptyMap(); notice = null
        description = company.description.orEmpty(); sector = company.sector.orEmpty(); website = company.website_url.orEmpty()
    }
    fun cancelEdit() { if (!saving) { editing = null; errors = emptyMap() } }
    fun writeDescription(text: String) { description = text; errors = errors - "description" }
    fun writeSector(value: String) { sector = value; errors = errors - "sector" }
    fun writeWebsite(text: String) { website = text; errors = errors - "website_url" }

    fun saveEdit() {
        val company = editing ?: return
        errors = companyProblems(description, sector, website)
        if (errors.isNotEmpty() || saving) return
        // Partial update: financial fields are never part of it.
        val change = buildMap<String, Any> {
            if (description.trim() != company.description.orEmpty()) put("description", description.trim())
            if (sector != company.sector) put("sector", sector)
            if (website.trim() != company.website_url.orEmpty()) put("website_url", website.trim())
        }
        if (change.isEmpty()) { editing = null; notice = "No había cambios por guardar."; return }
        patch(company, change) { editing = null; notice = "Se guardaron los datos descriptivos de ${it.ticker}." }
    }

    private fun patch(company: AdminCompany, change: Map<String, Any>, done: (AdminCompany) -> Unit) {
        val id = company.id ?: return
        if (saving) return
        saving = true; errors = emptyMap()
        viewModelScope.launch {
            when (val answer = source.update(id, change)) {
                is Outcome.Ok -> { replace(answer.value); done(answer.value) }
                is Outcome.Fail -> {
                    errors = answer.fields.ifEmpty { mapOf("form" to answer.message) }
                    if (answer.code in setOf(401, 403)) catalog = catalog.with(answer)
                }
            }
            saving = false
        }
    }

    /** HU14_03. Retrying after a lost answer repeats the same identifier; after a final answer a new one is used. */
    fun refresh(company: AdminCompany) {
        val id = company.id ?: return
        val previous = refreshes[id]
        if (previous?.running == true) return
        val key = "refresh_$id"
        val requestId = saved.get<String>(key) ?: newId().also { saved[key] = it }
        refreshes[id] = RefreshState(requestId, running = true, operation = previous?.operation)
        viewModelScope.launch {
            var answer = source.refresh(id, requestId)
            var polls = 0
            while (answer is Outcome.Ok && answer.value.operation?.status == "running" && polls++ < 20) {
                refreshes[id] = RefreshState(requestId, true, answer.value.operation)
                delay(1500)
                answer = source.operation(answer.value.operation!!.id!!)
            }
            finishRefresh(id, key, requestId, answer)
        }
    }

    private suspend fun finishRefresh(id: Long, key: String, requestId: String, answer: Outcome<OperationAnswer>) {
        val failed = (answer as? Outcome.Fail)?.let(::failedOperation)
        val result = (answer as? Outcome.Ok)?.value ?: failed
        result?.provider?.let { provider = it }
        val operation = result?.operation
        val still = operation?.status == "running"
        val uncertain = answer is Outcome.Fail && answer.uncertain
        if (!uncertain && !still) saved.remove<String>(key)
        refreshes[id] = RefreshState(requestId, false, operation, when {
            operation?.status == "failed" -> operation.message ?: result.detail ?: "La consulta no se completó."
            still -> "La actualización sigue en proceso. Consulta el resultado en un momento."
            answer is Outcome.Fail -> answer.message
            else -> null
        })
        if (operation?.status == "succeeded") {
            val current = source.list(query, state, scope, coverage, page)
            if (current is Outcome.Ok) catalog = catalog.with(current)
        }
    }
}

/** HU14_05: verify first, review, then confirm. Nothing is created from unverified data. */
@HiltViewModel
class AddCompanyViewModel internal constructor(
    private val source: CatalogSource, private val saved: SavedStateHandle, private val newId: () -> String
) : ViewModel() {
    @Inject constructor(repository: CatalogRepository, saved: SavedStateHandle) : this(repository as CatalogSource, saved, { UUID.randomUUID().toString() })

    var ticker by mutableStateOf(saved.get<String>("ticker") ?: ""); private set
    var sector by mutableStateOf(saved.get<String>("sector") ?: ""); private set
    var busy by mutableStateOf(false); private set
    var verified by mutableStateOf<MarketOperation?>(null); private set
    var created by mutableStateOf<MarketOperation?>(null); private set
    var errors by mutableStateOf<Map<String, String>>(emptyMap()); private set

    fun writeTicker(text: String) { if (!busy) { ticker = text.trim().uppercase(Locale.ROOT).take(10); saved["ticker"] = ticker; reset() } }
    fun writeSector(value: String) { if (!busy) { sector = value; saved["sector"] = value; reset() } }
    private fun reset() { verified = null; created = null; errors = emptyMap(); saved.remove<String>("verify") }

    fun verify() {
        if (busy) return
        errors = buildMap {
            if (!ADMIN_TICKER.matches(ticker)) put("ticker", "Escribe un ticker de 1 a 10 caracteres, por ejemplo AAPL.")
            if (sector !in catalogSectors) put("sector", "Elige el sector de la empresa.")
        }
        if (errors.isNotEmpty()) return
        val requestId = saved.get<String>("verify") ?: newId().also { saved["verify"] = it }
        busy = true
        viewModelScope.launch {
            var answer = source.verify(requestId, ticker, sector)
            var polls = 0
            while (answer is Outcome.Ok && answer.value.operation?.status == "running" && polls++ < 20) {
                delay(1500); answer = source.operation(answer.value.operation!!.id!!)
            }
            settle(answer)
            busy = false
        }
    }

    private fun settle(answer: Outcome<OperationAnswer>) {
        val failed = (answer as? Outcome.Fail)?.let(::failedOperation)
        val operation = (answer as? Outcome.Ok)?.value?.operation ?: failed?.operation
        if (!(answer is Outcome.Fail && answer.uncertain)) saved.remove<String>("verify")
        when {
            operation?.status == "ready" -> verified = operation
            operation?.status == "succeeded" -> created = operation
            operation?.status == "failed" -> errors = mapOf("form" to (operation.message ?: "No se pudo verificar el ticker."))
            answer is Outcome.Fail -> errors = answer.fields.ifEmpty { mapOf("form" to answer.message) }
            else -> errors = mapOf("form" to "La verificación sigue en proceso. Vuelve a intentarlo en un momento.")
        }
    }

    fun confirm() {
        val operation = verified ?: return
        if (busy) return
        busy = true; errors = emptyMap()
        viewModelScope.launch {
            when (val answer = source.create(operation.id!!)) {
                is Outcome.Ok -> if (answer.value.operation?.status == "succeeded") { created = answer.value.operation; verified = null }
                    else errors = mapOf("form" to (answer.value.operation?.message ?: "El alta no se completó."))
                is Outcome.Fail -> {
                    errors = mapOf("form" to answer.message)
                    // An expired verification must be repeated; a lost answer can be confirmed again safely.
                    if (answer.code == 409 || answer.code == 404) verified = null
                }
            }
            busy = false
        }
    }
}
