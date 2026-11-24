package com.example.sp500auth.ui.ops

import androidx.compose.runtime.*
import androidx.lifecycle.*
import com.example.sp500auth.data.ops.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

enum class AiPeriod(val label: String) { TODAY("Hoy"), WEEK("7 días"), MONTH("Este mes"), QUARTER("90 días") }

fun aiRange(period: AiPeriod, today: LocalDate): Pair<LocalDate, LocalDate> = when (period) {
    AiPeriod.TODAY -> today to today
    AiPeriod.WEEK -> today.minusDays(6) to today
    AiPeriod.MONTH -> today.withDayOfMonth(1) to today
    AiPeriod.QUARTER -> today.minusDays(89) to today
}

/** HU15_03: names the state the server published; the thresholds are never evaluated on the phone. */
fun budgetLabel(status: String?) = when (status) { "normal" -> "Normal"; "warning" -> "Advertencia"; "exhausted" -> "Agotado"; else -> "Sin estado" }
fun providerLabel(provider: String?) = when (provider) {
    "local_ai" -> "Motor de IA local"; "yahoo_chart" -> "Yahoo Finance (precios)"; "api" -> "Servidor"; "email" -> "Correo"; "push" -> "Push"; else -> provider ?: "Sin proveedor"
}

/** Épica 15. */
@HiltViewModel
class MonitoringViewModel internal constructor(private val source: MonitoringSource, private val saved: SavedStateHandle, private val today: () -> LocalDate) : ViewModel() {
    @Inject constructor(repository: MonitoringRepository, saved: SavedStateHandle) : this(repository as MonitoringSource, saved, { LocalDate.now(ZoneId.systemDefault()) })

    var tab by mutableStateOf(saved.get<Int>("tab") ?: 0); private set
    var period by mutableStateOf(AiPeriod.entries[saved.get<Int>("period") ?: AiPeriod.MONTH.ordinal]); private set
    var model by mutableStateOf(saved.get<String>("model")); private set
    var month by mutableStateOf(saved.get<String>("month") ?: YearMonth.from(today()).toString()); private set
    var level by mutableStateOf(saved.get<String>("level") ?: "all"); private set
    var component by mutableStateOf(saved.get<String>("component") ?: "all"); private set
    var page by mutableStateOf(saved.get<Int>("page") ?: 1); private set

    var ai by mutableStateOf(Load<AiUsage>()); private set
    /** Models seen in an unfiltered answer, so a filter can be changed or removed. */
    var models by mutableStateOf<List<String>>(emptyList()); private set
    var plans by mutableStateOf(Load<PlanDistribution>()); private set
    var budgets by mutableStateOf(Load<Budgets>()); private set
    var monthly by mutableStateOf(Load<MonthlyValue>()); private set
    var logs by mutableStateOf(Load<ErrorPage>()); private set
    var detail by mutableStateOf<Load<ErrorLog>?>(null); private set

    var editing by mutableStateOf<Budget?>(null); private set
    var limitText by mutableStateOf(""); private set
    var saving by mutableStateOf(false); private set
    var budgetError by mutableStateOf<String?>(null); private set
    private val jobs = mutableMapOf<String, Job>()

    private fun <T> fetch(key: String, get: () -> Load<T>, set: (Load<T>) -> Unit, block: suspend () -> Outcome<T>, done: (T) -> Unit = {}) {
        jobs[key]?.cancel(); set(get().started())
        jobs[key] = viewModelScope.launch {
            val answer = block()
            ensureActive()
            set(get().with(answer)); (answer as? Outcome.Ok)?.let { done(it.value) }
        }
    }

    fun open(index: Int) { tab = index; saved["tab"] = index; reload() }
    fun reload() = when (tab) { 0 -> loadAi(); 1 -> loadPlans(); 2 -> loadBudgets(); 3 -> loadMonthly(); else -> loadLogs() }

    fun choosePeriod(value: AiPeriod) { period = value; saved["period"] = value.ordinal; loadAi(clear = true) }
    fun chooseModel(value: String?) { model = value; saved["model"] = value; loadAi(clear = true) }
    private fun loadAi(clear: Boolean = false) {
        // Figures of another period must not stay under the new title.
        if (clear) ai = Load()
        val chosen = period; val wanted = model
        fetch("ai", { ai }, { ai = it }, { aiFor(chosen, wanted) }) { data ->
            if (wanted == null) models = data.breakdown.orEmpty().mapNotNull { it.model }.distinct()
        }
    }
    /** The server's calendar decides what "today" is: the phone can be in another time zone. */
    private suspend fun aiFor(chosen: AiPeriod, wanted: String?): Outcome<AiUsage> {
        val month = source.ai(null, null, null, wanted.takeIf { chosen == AiPeriod.MONTH })
        if (chosen == AiPeriod.MONTH || month is Outcome.Fail) return month
        val serverDay = runCatching { LocalDate.parse((month as Outcome.Ok).value.end) }.getOrDefault(today())
        val (start, end) = aiRange(chosen, serverDay)
        return source.ai(start.toString(), end.toString(), null, wanted)
    }
    private fun loadPlans() = fetch("plans", { plans }, { plans = it }, source::plans)
    private fun loadBudgets() = fetch("budgets", { budgets }, { budgets = it }, source::budgets)

    val canGoNext get() = YearMonth.parse(month) < YearMonth.from(today())
    fun moveMonth(step: Long) {
        val next = YearMonth.parse(month).plusMonths(step)
        if (next > YearMonth.from(today())) return
        month = next.toString(); saved["month"] = month; monthly = Load(); loadMonthly()
    }
    private fun loadMonthly() { val wanted = month; fetch("monthly", { monthly }, { monthly = it }, { source.monthly(wanted) }) }

    fun chooseLevel(value: String) { level = value; saved["level"] = value; goLogs(1) }
    fun chooseComponent(value: String) { component = value; saved["component"] = value; goLogs(1) }
    fun goLogs(to: Int) { page = maxOf(1, to); saved["page"] = page; loadLogs() }
    private fun loadLogs() {
        val wanted = page
        fetch("logs", { logs }, { logs = it }, {
            val answer = source.errors(level, component, wanted)
            if (answer is Outcome.Fail && answer.code == 404 && answer.message != SERVICE_MISSING && wanted > 1) {
                page = 1; saved["page"] = 1; source.errors(level, component, 1)
            } else answer
        })
    }
    private var detailId = 0L
    fun retryLog() = openLog(detailId)
    fun openLog(id: Long) { detailId = id; detail = Load(loading = true); fetch("detail", { detail ?: Load() }, { if (detail != null) detail = it }, { source.error(id) }) }
    fun closeLog() { jobs["detail"]?.cancel(); detail = null }

    fun edit(budget: Budget) { if (!saving) { editing = budget; limitText = budget.limit?.toString().orEmpty(); budgetError = null } }
    fun cancelEdit() { if (!saving) { editing = null; budgetError = null } }
    fun writeLimit(text: String) { limitText = text.filter(Char::isDigit).take(7); budgetError = null }
    fun saveLimit() {
        val current = editing ?: return
        if (saving) return
        val limit = limitText.toIntOrNull()
        if (limit == null || limit !in 1..1_000_000) { budgetError = "El límite debe estar entre 1 y 1 000 000."; return }
        saving = true; budgetError = null
        viewModelScope.launch {
            when (val answer = source.saveBudget(current.provider!!, BudgetChange(current.version!!, limit, current.timezone ?: "UTC"))) {
                is Outcome.Ok -> {
                    budgets = budgets.copy(value = budgets.value?.let { data -> data.copy(results = data.results?.map { if (it.provider == current.provider) answer.value else it }) })
                    editing = null
                }
                is Outcome.Fail -> { budgetError = answer.fields.values.firstOrNull() ?: answer.message; if (answer.code == 409) loadBudgets() }
            }
            saving = false
        }
    }
}
