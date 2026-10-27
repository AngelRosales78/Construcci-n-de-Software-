package com.example.sp500auth

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.*
import androidx.test.platform.app.InstrumentationRegistry
import com.example.sp500auth.data.local.TokenManager
import com.example.sp500auth.data.repository.*
import com.example.sp500auth.ui.analysis.*
import com.example.sp500auth.ui.pillars.*
import com.example.sp500auth.ui.scenarios.*
import com.example.sp500auth.util.Resource
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Recorre pantallas reales con API, PostgreSQL y modelo local de la demo. */
class Sprint2LiveUiTest {
    @get:Rule val compose=createComposeRule()
    private fun screenshot(name: String) {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val bitmap=instrumentation.uiAutomation.takeScreenshot()
        File(instrumentation.targetContext.filesDir,"sprint2_$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG,100,it)
        }
    }
    @Test fun pillarsScenariosAndBothAiLevelsWorkOnDevice() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val id=UUID.randomUUID().toString().replace("-","").take(12)
        val password="Aa9!"+UUID.randomUUID()
        val account=JSONObject().put("username","ui_$id").put("email","ui_$id@example.test")
            .put("password",password).put("password_confirm",password).put("user_type","retail")
        val plain=OkHttpClient.Builder().readTimeout(160,TimeUnit.SECONDS).build()
        val registration=plain.newCall(Request.Builder().url(BuildConfig.API_BASE_URL+"auth/register/")
            .post(account.toString().toRequestBody("application/json".toMediaType())).build()).execute()
        val session=registration.use { assertTrue("Registro aislado",it.isSuccessful);JSONObject(it.body!!.string()) }
        val access=session.getString("access")
        val tokens=TokenManager(context);tokens.saveTokens(access,session.getString("refresh"))
        val client=plain.newBuilder().addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("Authorization","Bearer $access").build())
        }.build()
        val retrofit=Retrofit.Builder().baseUrl(BuildConfig.API_BASE_URL).client(client)
            .addConverterFactory(GsonConverterFactory.create()).build()
        val pillars=PillarsViewModel(PillarsRepository(retrofit,tokens))
        val scenarios=ScenariosViewModel(SimulationRepository(retrofit))
        val analysis=AnalysisViewModel(AnalysisRepository(retrofit))
        compose.setContent {
            MaterialTheme {
                val nav=rememberNavController()
                NavHost(nav,startDestination="pillars") {
                    composable("pillars"){PillarScreen(nav,"all",pillars,"MSFT")}
                    composable("scenarios/MSFT"){ScenariosScreen(nav,"MSFT",scenarios)}
                    composable("analysis/MSFT/2024"){AnalysisScreen(nav,"MSFT",2024,analysis)}
                }
            }
        }
        compose.waitUntil(60000){pillars.state is PillarsState.Ready}
        compose.onNodeWithText("85,25 / 100").performScrollTo().assertIsDisplayed()
        screenshot("puntaje")
        for ((id,label) in pillarLabels) {
            compose.onNode(hasText("$label: ",substring=true) and hasClickAction()).performScrollTo().performClick()
            compose.onNodeWithText("Ver radar y puntaje").assertExists()
            screenshot(id)
            compose.onNodeWithText("Ver radar y puntaje").performScrollTo().performClick()
        }
        compose.onNodeWithText("Escenarios a doce meses").performScrollTo().performClick()
        compose.waitUntil(60000){scenarios.defaults is Resource.Success}
        compose.onNodeWithText("Recalcular").performScrollTo().performClick()
        compose.waitUntil(60000){scenarios.result is Resource.Success}
        val original=scenarios.result.data!!
        compose.onNodeWithContentDescription("Proyección P10, P50 y P90 de la misma ejecución, con escala común.")
            .performScrollTo().assertIsDisplayed()
        screenshot("escenarios")
        compose.onAllNodes(isToggleable())[2].performScrollTo().performClick()
        compose.runOnIdle{assertEquals(original.simulation_id,scenarios.result.data!!.simulation_id)}
        compose.onNodeWithText("Retorno anual supuesto (%)").performScrollTo().performTextReplacement("0")
        compose.onNodeWithText("Volatilidad anual (%)").performScrollTo().performTextReplacement("0")
        compose.onNodeWithText("Recalcular").performScrollTo().performClick()
        compose.waitUntil(60000){scenarios.result is Resource.Success}
        compose.runOnIdle{assertEquals(1,scenarios.result.data!!.points!!.map{it.p50}.toSet().size)}
        compose.onNodeWithText("Restablecer").performScrollTo().performClick()
        compose.waitUntil(60000){scenarios.result is Resource.Success}
        compose.runOnIdle{assertEquals(original.points,scenarios.result.data!!.points)}
        compose.onNodeWithText("Volver").performScrollTo().performClick()
        compose.onNodeWithText("Explicación de IA").performScrollTo().performClick()
        compose.waitUntil(180000){analysis.state is Resource.Success || analysis.state is Resource.Error}
        compose.runOnIdle{assertTrue("IA sencilla: ${analysis.state.message}",analysis.state is Resource.Success)}
        compose.onNodeWithText("Resumen").performScrollTo().assertIsDisplayed()
        screenshot("ia_sencilla")
        compose.onNodeWithText("Técnico").performScrollTo().performClick()
        compose.waitUntil(180000){analysis.state is Resource.Success || analysis.state is Resource.Error}
        compose.runOnIdle{
            assertTrue("IA técnica: ${analysis.state.message}",analysis.state is Resource.Success)
            assertEquals("technical",analysis.lastValid!!.level)
            assertEquals(5,analysis.lastValid!!.pillars!!.size)
        }
        compose.onNodeWithText("Resumen").performScrollTo().assertIsDisplayed()
        screenshot("ia_tecnica")
    }
}
