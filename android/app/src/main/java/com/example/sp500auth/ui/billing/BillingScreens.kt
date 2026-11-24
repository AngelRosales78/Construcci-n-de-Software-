package com.example.sp500auth.ui.billing

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.sp500auth.data.repository.AccessInfo
import com.example.sp500auth.data.repository.InvoiceInfo
import com.example.sp500auth.data.repository.PlanInfo
import com.example.sp500auth.ui.sprint3.AccessObserver
import com.example.sp500auth.ui.sprint3.AccessViewModel
import com.example.sp500auth.ui.sprint3.RemoteContent
import com.example.sp500auth.util.Resource
import kotlin.random.Random

private val Popular = Color(0xFF36F5A0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BillingScaffold(nav: NavController, title: String, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(topBar = {
        TopAppBar(title = { Text(title, fontWeight = FontWeight.SemiBold) }, navigationIcon = {
            IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
        })
    }) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
    }
}

/** HU11_02, HU11_04 and HU11_05: plan comparison, current subscription and cancellation. */
@Composable
fun PlansScreen(nav: NavController, access: AccessViewModel = hiltViewModel(), billing: BillingViewModel = hiltViewModel()) {
    AccessObserver(access)
    LaunchedEffect(Unit) { billing.loadPlans() }
    var cancelling by rememberSaveable { mutableStateOf(false) }
    val cancellation = billing.cancellation
    LaunchedEffect(cancellation) { if (cancellation is Resource.Success) { cancelling = false; access.load() } }
    BillingScaffold(nav, "Mi plan") {
        RemoteContent(access.state, access::load) { info ->
            SubscriptionCard(info, onUpgrade = { nav.navigate("checkout") }, onCancel = { billing.cancellationShown(); cancelling = true })
        }
        if (cancellation is Resource.Success) Text("Cancelaste la renovación. Conservas Pro hasta ${displayDate(cancellation.successData.period_end)}.",
            color = MaterialTheme.colorScheme.primary)
        Text("Compara los planes", style = MaterialTheme.typography.titleMedium)
        RemoteContent(billing.plans, billing::loadPlans) { plans ->
            val current = (access.state as? Resource.Success)?.successData?.plan
            plans.plans.orEmpty().forEach { PlanCard(it, plans.currency ?: "USD", it.id == current) }
        }
        OutlinedButton(onClick = { nav.navigate("invoices") }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Receipt, null); Spacer(Modifier.width(8.dp)); Text("Ver comprobantes")
        }
        Text("Pagos simulados con fines académicos: no se realiza ningún cargo real.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    val info = (access.state as? Resource.Success)?.successData
    if (cancelling && info != null) CancelDialog(info, cancellation, onConfirm = billing::cancel, onDismiss = { cancelling = false; billing.cancellationShown() })
}

@Composable
private fun SubscriptionCard(info: AccessInfo, onUpgrade: () -> Unit, onCancel: () -> Unit) {
    val pro = info.plan == "pro" && info.active == true
    val colors = MaterialTheme.colorScheme
    Card(colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Suscripción actual", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            Text(if (pro) "Plan Pro" else "Plan Free", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            when {
                info.grant_origin == "local-development" -> Text("Acceso Pro académico local, sin compra ni vencimiento real.")
                pro && info.grant_origin == "administrator" -> Text("Pro asignado por un administrador, sin cobro ni vencimiento.")
                pro -> {
                    val progress = periodProgress(info.period_start, info.period_end)
                    if (progress != null) {
                        LinearProgressIndicator(progress = { progress.first }, modifier = Modifier.fillMaxWidth().height(8.dp)
                            .semantics { contentDescription = "Quedan ${progress.second} días del periodo" })
                        Text("Quedan ${progress.second} días del periodo actual.", style = MaterialTheme.typography.bodySmall)
                    }
                    if (info.auto_renew == true) {
                        Text("Se renueva el ${displayDate(info.period_end)} por ${info.price ?: "-"} ${info.currency.orEmpty()}.")
                        TextButton(onClick = onCancel, colors = ButtonDefaults.textButtonColors(contentColor = colors.error)) { Text("Cancelar suscripción") }
                    } else Text("Renovación cancelada. Conservas Pro hasta el ${displayDate(info.period_end)}; después volverás a Free.", color = colors.error)
                }
                else -> Button(onClick = onUpgrade, modifier = Modifier.fillMaxWidth()) { Text("Actualizar a Pro") }
            }
        }
    }
}

@Composable
private fun PlanCard(plan: PlanInfo, currency: String, current: Boolean) {
    val colors = MaterialTheme.colorScheme
    Card(colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
        border = if (plan.popular == true) androidx.compose.foundation.BorderStroke(2.dp, Popular) else null) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(plan.name.orEmpty(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (plan.popular == true) Surface(color = Popular, shape = RoundedCornerShape(50)) {
                    Text("Más popular", color = Color.Black, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
                if (current) AssistChip(onClick = {}, label = { Text("Tu plan") })
            }
            Text("${plan.price ?: "-"} $currency / mes", style = MaterialTheme.typography.titleMedium, color = colors.primary)
            plan.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
            plan.features.orEmpty().forEach { feature ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val included = feature.included == true
                    Icon(if (included) Icons.Default.Check else Icons.Default.Close, contentDescription = if (included) "Incluido" else "No incluido",
                        tint = if (included) Popular else colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Text(feature.label.orEmpty(), style = MaterialTheme.typography.bodyMedium,
                        color = if (included) colors.onSurface else colors.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun CancelDialog(info: AccessInfo, state: Resource<AccessInfo>, onConfirm: (String, String) -> Unit, onDismiss: () -> Unit) {
    var reason by rememberSaveable { mutableStateOf("") }
    var comment by rememberSaveable { mutableStateOf("") }
    val busy = state is Resource.Loading
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("¿Cancelar la suscripción Pro?") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("No se renovará. Conservarás Pro hasta el ${displayDate(info.period_end)} y después pasarás a Free.", fontWeight = FontWeight.SemiBold)
                Text("¿Por qué cancelas?", style = MaterialTheme.typography.labelLarge)
                cancelReasons.forEach { (id, label) ->
                    Row(Modifier.fillMaxWidth().selectable(reason == id, role = Role.RadioButton) { reason = id },
                        verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = reason == id, onClick = null); Spacer(Modifier.width(8.dp)); Text(label)
                    }
                }
                OutlinedTextField(comment, { comment = it.take(150) }, label = { Text("Comentario (opcional)") }, modifier = Modifier.fillMaxWidth())
                if (state is Resource.Error) Text(state.message.orEmpty(), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(reason, comment) }, enabled = reason.isNotEmpty() && !busy) {
                Text(if (busy) "Cancelando…" else "Cancelar suscripción", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Mantener Pro") } })
}

/** HU11_01: simulated checkout with Luhn validation before the server decides. */
@Composable
fun CheckoutScreen(nav: NavController, vm: BillingViewModel = hiltViewModel()) {
    // Saved across rotation in the screen's instance state; never written to disk storage or logs.
    var number by rememberSaveable { mutableStateOf("") }
    var holder by rememberSaveable { mutableStateOf("") }
    var expiryInput by rememberSaveable { mutableStateOf("") }
    var cvc by rememberSaveable { mutableStateOf("") }
    val expiry = expiryText(expiryInput)
    var touched by rememberSaveable { mutableStateOf(false) }
    val state = vm.payment
    LaunchedEffect(state) {
        if (state is Resource.Success) state.successData.invoice?.number?.let { invoice ->
            vm.paymentShown()
            nav.navigate("receipt/$invoice?celebrate=true") { popUpTo("plans") }
        }
    }
    val errors = cardErrors(number, holder, expiry, cvc)
    val digits = cardDigits(number)
    fun edit(change: () -> Unit) { change(); vm.cardEdited() }
    BillingScaffold(nav, "Actualizar a Pro") {
        Text("Plan Pro mensual · 9.99 USD", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(digits, { edit { number = cardDigits(it) } }, label = { Text("Número de tarjeta") },
            visualTransformation = remember { CardNumberTransformation() },
            supportingText = { Text(if (touched && errors.number != null) errors.number else if (digits.length >= 2) cardBrand(digits) else " ") },
            isError = touched && errors.number != null, singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        OutlinedTextField(holder, { edit { holder = it.take(80) } }, label = { Text("Titular") }, singleLine = true,
            isError = touched && errors.holder != null, supportingText = { if (touched && errors.holder != null) Text(errors.holder) },
            modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(expiryInput, { edit { expiryInput = expiryDigits(it) } }, label = { Text("Vence (MM/AA)") },
                visualTransformation = remember { ExpiryTransformation() }, placeholder = { Text("MM/AA") },
                isError = touched && errors.expiry != null, supportingText = { if (touched && errors.expiry != null) Text(errors.expiry) },
                singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(cvc, { edit { cvc = it.filter(Char::isDigit).take(4) } }, label = { Text("CVC") },
                isError = touched && errors.cvc != null, supportingText = { if (touched && errors.cvc != null) Text(errors.cvc) },
                singleLine = true, modifier = Modifier.weight(1f), visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
        }
        Button(onClick = { touched = true; if (errors.valid) vm.pay(digits, holder, expiry, cvc) },
            enabled = state !is Resource.Loading, modifier = Modifier.fillMaxWidth()) {
            if (state is Resource.Loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Pagar 9.99 USD (simulado)")
        }
        if (state is Resource.Error) Text(state.message.orEmpty(), color = MaterialTheme.colorScheme.error)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Pago simulado", fontWeight = FontWeight.SemiBold)
                Text("No se realiza ningún cargo. Usa tarjetas de prueba: 4242 4242 4242 4242 se aprueba y 4000 0000 0000 0002 se rechaza.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** HU11_03: receipt after the simulated payment, also reachable from the receipts list. */
@Composable
fun ReceiptScreen(nav: NavController, number: String, celebrate: Boolean, vm: BillingViewModel = hiltViewModel()) {
    LaunchedEffect(number) { vm.loadReceipt(number) }
    BillingScaffold(nav, "Comprobante") {
        RemoteContent(vm.receipt, { vm.loadReceipt(number) }) { invoice ->
            if (celebrate) SuccessBanner()
            ReceiptCard(invoice)
            if (celebrate) Button(onClick = { nav.navigate("compare") { popUpTo("plans") } }, modifier = Modifier.fillMaxWidth()) { Text("Probar el comparador Pro") }
        }
    }
}

@Composable
private fun SuccessBanner() {
    val scale = remember { Animatable(0.3f) }
    val fall = remember { Animatable(0f) }
    LaunchedEffect(Unit) { scale.animateTo(1f, tween(450)); fall.animateTo(1f, tween(1600)) }
    val pieces = remember { List(36) { Triple(Random.nextFloat(), Random.nextFloat() * .5f, listOf(Popular, Color(0xFFE0C34D), Color(0xFF6EA8FE), Color(0xFFFF8FB1)).random()) } }
    Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            pieces.forEach { (x, delay, color) ->
                val y = ((fall.value - delay) / (1f - delay)).coerceIn(0f, 1f)
                if (y > 0f) drawCircle(color.copy(alpha = 1f - y * .6f), 5.dp.toPx(), Offset(x * size.width, y * size.height))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.scale(scale.value)) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Popular, modifier = Modifier.size(64.dp))
            Text("¡Ya eres Pro!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ReceiptCard(invoice: InvoiceInfo) {
    val colors = MaterialTheme.colorScheme
    Card(colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (invoice.kind == "renewal") "Renovación Pro" else "Suscripción Pro", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${invoice.amount} ${invoice.currency.orEmpty()}", style = MaterialTheme.typography.headlineMedium, color = colors.primary)
            HorizontalDivider()
            listOf("Comprobante" to invoice.number, "Transacción" to invoice.transaction_id,
                "Fecha" to displayDate(invoice.created_at),
                "Medio de pago" to (invoice.card_last4?.takeIf { it.isNotBlank() }?.let { "${invoice.card_brand} •••• $it" } ?: "Renovación automática"),
                "Periodo" to "${displayDate(invoice.period_start)} – ${displayDate(invoice.period_end)}").forEach { (label, value) ->
                Row { Text(label, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f)); Text(value.orEmpty(), fontWeight = FontWeight.Medium) }
            }
            Text(if (invoice.emailed == true) "Enviamos una copia a tu correo." else "La copia por correo se envía al confirmarse el pago.",
                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            Text("Pago simulado: no se realizó ningún cargo.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicesScreen(nav: NavController, vm: BillingViewModel = hiltViewModel()) {
    LaunchedEffect(Unit) { vm.loadInvoices() }
    BillingScaffold(nav, "Comprobantes") {
        RemoteContent(vm.invoices, vm::loadInvoices) { list ->
            val rows = list.results.orEmpty()
            if (rows.isEmpty()) Text("Todavía no tienes comprobantes.")
            rows.forEach { invoice ->
                Card(onClick = { nav.navigate("receipt/${invoice.number}") }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(invoice.number.orEmpty(), fontWeight = FontWeight.SemiBold)
                            Text(displayDate(invoice.created_at), style = MaterialTheme.typography.bodySmall)
                        }
                        Text("${invoice.amount} ${invoice.currency.orEmpty()}")
                    }
                }
            }
        }
    }
}
