package com.example.sp500auth.ui.ops

import androidx.compose.runtime.*
import androidx.lifecycle.*
import com.example.sp500auth.data.ops.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import javax.inject.Inject

/** HU12_01, HU12_03 and HU12_05. Lives with the activity: rotation and navigation keep the list and the counter. */
@HiltViewModel
class NotificationsViewModel internal constructor(private val source: NotificationsSource) : ViewModel() {
    @Inject constructor(repository: NotificationsRepository) : this(repository as NotificationsSource)

    private var owner: String? = null
    private var rows by mutableStateOf<List<Notice>>(emptyList())
    private var counter by mutableStateOf(0)
    private var through: Long? = null
    private var lastPage = 0
    private var job: Job? = null
    private val mine get() = owner != null && owner == source.owner()

    val items: List<Notice> get() = if (mine) rows else emptyList()
    val unread: Int get() = if (mine) counter else 0
    var loaded by mutableStateOf(false); private set
    var loading by mutableStateOf(false); private set
    var loadingMore by mutableStateOf(false); private set
    var hasMore by mutableStateOf(false); private set
    var error by mutableStateOf<Outcome.Fail?>(null); private set
    var message by mutableStateOf<String?>(null); private set
    var marking by mutableStateOf<Set<Long>>(emptySet()); private set
    var markingAll by mutableStateOf(false); private set

    fun clear() {
        job?.cancel(); owner = null; rows = emptyList(); counter = 0; through = null; lastPage = 0
        loaded = false; loading = false; loadingMore = false; hasMore = false; error = null; message = null; marking = emptySet(); markingAll = false
    }

    private fun bind(): String? {
        val account = source.owner()
        if (account == null) { clear(); return null }
        if (account != owner) { clear(); owner = account }
        return account
    }

    /** First page again, from the newest notice. The previous list stays visible until the answer arrives. */
    fun refresh() {
        val account = bind() ?: return
        job?.cancel(); loading = true; loadingMore = false; error = null
        job = viewModelScope.launch {
            val answer = source.list(1, null)
            ensureActive()
            if (source.owner() != account) return@launch
            when (answer) {
                is Outcome.Ok -> {
                    rows = answer.value.results.orEmpty(); counter = answer.value.unread_count ?: 0
                    through = answer.value.read_through; lastPage = 1; hasMore = answer.value.next != null; loaded = true
                }
                is Outcome.Fail -> { error = answer; if (answer.code in setOf(401, 403)) { rows = emptyList(); counter = 0; loaded = false } }
            }
            loading = false
        }
    }

    /** Following pages keep the limit of the first one, so new notices never repeat or shift rows. */
    fun loadMore() {
        val account = owner ?: return
        if (!mine || loading || loadingMore || !hasMore) return
        loadingMore = true; error = null
        job = viewModelScope.launch {
            val answer = source.list(lastPage + 1, through)
            ensureActive()
            if (source.owner() != account) return@launch
            when (answer) {
                is Outcome.Ok -> {
                    rows = (rows + answer.value.results.orEmpty()).distinctBy { it.id }
                    counter = answer.value.unread_count ?: counter; lastPage++; hasMore = answer.value.next != null
                }
                is Outcome.Fail -> error = answer
            }
            loadingMore = false
        }
    }

    fun markRead(id: Long) {
        val account = owner ?: return
        if (!mine || id in marking || markingAll || rows.firstOrNull { it.id == id }?.unread == false) return
        marking = marking + id; message = null
        viewModelScope.launch {
            val answer = source.markRead(id)
            if (source.owner() == account) when (answer) {
                is Outcome.Ok -> { counter = answer.value.unread_count ?: counter; seen { it.id == id } }
                is Outcome.Fail -> message = answer.message
            }
            marking = marking - id
        }
    }

    fun markAll() {
        val account = owner ?: return
        val limit = through ?: return
        if (!mine || markingAll) return
        markingAll = true; message = null
        viewModelScope.launch {
            val answer = source.markAll(limit)
            if (source.owner() == account) when (answer) {
                // A notice that arrived after the list keeps counting; the server decides the number.
                is Outcome.Ok -> { counter = answer.value.unread_count ?: counter; seen { (it.id ?: Long.MAX_VALUE) <= limit } }
                is Outcome.Fail -> message = answer.message
            }
            markingAll = false
        }
    }

    private fun seen(which: (Notice) -> Boolean) { rows = rows.map { if (which(it) && it.unread) it.copy(read_at = "confirmed") else it } }
}

enum class SaveState { IDLE, SAVING, SAVED, FAILED }

/** HU12_04. Every switch is saved at once; a failure keeps the selection to retry it. */
@HiltViewModel
class PreferencesViewModel internal constructor(private val source: NotificationsSource) : ViewModel() {
    @Inject constructor(repository: NotificationsRepository) : this(repository as NotificationsSource)

    var preferences by mutableStateOf(Load<Preferences>()); private set
    var rating by mutableStateOf(true); private set
    var channels by mutableStateOf(Channels()); private set
    var save by mutableStateOf(SaveState.IDLE); private set
    var problem by mutableStateOf<String?>(null); private set
    private var owner: String? = null
    private var again = false

    fun load() {
        owner = source.owner(); val account = owner
        preferences = preferences.started(); save = SaveState.IDLE; problem = null
        viewModelScope.launch {
            val answer = source.preferences()
            if (source.owner() != account) return@launch
            preferences = preferences.with(answer)
            if (answer is Outcome.Ok) show(answer.value)
        }
    }

    private fun show(data: Preferences) { rating = data.rating_changes ?: true; channels = data.channels ?: Channels() }
    fun changeRating(value: Boolean) { rating = value; send() }
    fun changeChannels(value: Channels) { channels = value; send() }
    fun retry() = send()

    private fun send() {
        val known = preferences.value ?: return
        val account = owner
        if (account == null || account != source.owner()) { problem = "La cuenta cambió. Vuelve a abrir las preferencias."; save = SaveState.FAILED; return }
        if (save == SaveState.SAVING) { again = true; return }
        save = SaveState.SAVING; problem = null; again = false
        val available = known.available_channels ?: Channels()
        // A channel that cannot deliver is never sent as enabled.
        val wanted = Channels(channels.web && available.web, channels.email && available.email, channels.push && available.push)
        viewModelScope.launch {
            val answer = source.save(PreferencesChange(known.version ?: 0, rating, wanted))
            if (source.owner() != account) return@launch
            when (answer) {
                is Outcome.Ok -> {
                    val repeat = again
                    preferences = Load(answer.value); save = SaveState.SAVED
                    if (repeat) send() else show(answer.value)
                }
                is Outcome.Fail -> { save = SaveState.FAILED; again = false; problem = answer.fields.values.firstOrNull() ?: answer.message }
            }
        }
    }
}
