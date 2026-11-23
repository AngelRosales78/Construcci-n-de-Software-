package com.example.sp500auth

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.sp500auth.ui.MainActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

/** Contra la API local activa; cuentas y contraseñas provienen de un archivo excluido de Git. */
class Sprint4LiveUiTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private val context get()=instrumentation.targetContext
    private val http=OkHttpClient.Builder().callTimeout(30,TimeUnit.SECONDS).build()
    private lateinit var credentials:JSONObject
    private var token=""

    private fun request(path:String,method:String="GET",body:JSONObject?=null):JSONObject {
        val builder=Request.Builder().url(BuildConfig.API_BASE_URL+path)
        if(token.isNotBlank())builder.header("Authorization","Bearer $token")
        if(method!="GET")builder.method(method,(body ?: JSONObject()).toString().toRequestBody("application/json".toMediaType()))
        return http.newCall(builder.build()).execute().use{assertTrue("El servicio debe confirmar $path",it.isSuccessful);val text=it.body?.string().orEmpty();if(text.isBlank())JSONObject() else JSONObject(text)}
    }

    @Before fun authenticateAdmin() {
        org.junit.Assume.assumeTrue("Se requiere el archivo local de cuentas excluido de Git.",instrumentation.context.assets.list("").orEmpty().contains("sprint4-live.json"))
        credentials=JSONObject(instrumentation.context.assets.open("sprint4-live.json").bufferedReader().use{it.readText()})
        val login=request("auth/token/","POST",credentials.getJSONObject("admin"))
        token=login.getString("access")
        instrumentation.uiAutomation.rootInActiveWindow?.findAccessibilityNodeInfosByText("Wait")?.firstOrNull()?.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
        compose.waitUntil(60_000){
            compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size>=2 ||
                compose.onAllNodesWithText("Explorar").fetchSemanticsNodes().isNotEmpty()
        }
        if(compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size>=2) {
            compose.onAllNodes(hasSetTextAction())[0].performTextReplacement(credentials.getJSONObject("admin").getString("username"))
            compose.onAllNodes(hasSetTextAction())[1].performTextReplacement(credentials.getJSONObject("admin").getString("password"))
            compose.onNodeWithText("Iniciar sesión",substring=false).performScrollTo().performClick()
        }
        waitText("Explorar")
    }

    private fun waitText(text:String,substring:Boolean=false) {
        compose.waitUntil(60_000){compose.onAllNodesWithText(text,substring=substring,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
    }
    private fun scrollClick(text:String) {
        compose.onNode(SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsActions.ScrollToIndex)).performScrollToNode(hasText(text))
        compose.onNodeWithText(text).performClick()
    }
    private fun back() {
        compose.onNode(SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsActions.ScrollToIndex)).performScrollToIndex(0)
        compose.onNodeWithText("Volver").performClick()
    }
    private fun account() {
        waitText("Explorar")
        compose.onNodeWithContentDescription("Mi perfil").performClick()
        waitText("Gestionar cuenta, avisos y suscripción")
        compose.onNodeWithText("Gestionar cuenta, avisos y suscripción").performClick()
        waitText("Tu espacio")
    }

    @Test fun administrationUsesRealTotalsAndSharedCatalog() {
        val plans=request("admin/monitoring/plans/")
        account();scrollClick("Uso y operación")
        waitText("Llamadas observadas:",true)
        compose.onNodeWithText("Planes",substring=false).performClick()
        waitText("Cuentas: "+plans.getInt("total"))
        compose.onNodeWithText("Presupuestos",substring=false).performClick()
        waitText("Motor de análisis local")
        compose.onNodeWithText("Registros",substring=false).performClick()
        compose.waitForIdle()
        back()
        scrollClick("Empresas y datos")
        waitText("Buscar empresa")
        compose.onNodeWithText("Buscar empresa").performTextInput("MSFT")
        compose.onNodeWithText("Buscar",substring=false).performClick()
        waitText("MSFT ·",true)
        back()
        scrollClick("Usuarios y acceso")
        waitText("Buscar usuario")
        compose.onNodeWithText("Buscar usuario").performTextInput(credentials.getJSONObject("admin").getString("username"))
        compose.onNodeWithText("Buscar",substring=false).performClick()
        waitText("Rol: Administrador",true)
    }

    @Test fun preferencesPersistThroughMobileAndWebContract() {
        val previous=request("user/notification-preferences/")
        account();scrollClick("Preferencias de avisos")
        waitText("Cambios de perfil financiero")
        compose.waitUntil(60_000){compose.onAllNodes(isToggleable() and isEnabled()).fetchSemanticsNodes().isNotEmpty()}
        compose.onAllNodes(isToggleable()).onFirst().performClick()
        scrollClick("Guardar preferencias")
        waitText("Cambios guardados.")
        val saved=request("user/notification-preferences/")
        assertEquals(!previous.getBoolean("rating_changes"),saved.getBoolean("rating_changes"))
        request("user/notification-preferences/","PUT",JSONObject().put("version",saved.getInt("version"))
            .put("rating_changes",previous.getBoolean("rating_changes")).put("channels",previous.getJSONObject("channels")))
        back()
        scrollClick("Mis avisos")
        waitText("Mis avisos")
        compose.onNodeWithText("Actualizar",substring=false).performClick()
        waitText("Sin leer:",true)
    }

    @Test fun subscriptionUsesServerDurationAndReceiptHistory() {
        account();scrollClick("Mi suscripción")
        waitText("Historial de suscripciones")
        val state=request("user/subscription/history/").getJSONObject("subscription")
        waitText(if(state.getString("effective_plan")=="pro")"Plan Pro" else "Plan Free")
    }
}
