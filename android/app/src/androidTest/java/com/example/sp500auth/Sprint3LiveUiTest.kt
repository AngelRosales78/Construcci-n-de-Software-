package com.example.sp500auth

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.lifecycle.SavedStateHandle
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.platform.app.InstrumentationRegistry
import com.example.sp500auth.data.repository.*
import com.example.sp500auth.ui.favorites.*
import com.example.sp500auth.ui.reports.ReportProgress
import com.example.sp500auth.ui.reports.ReportsViewModel
import com.example.sp500auth.ui.reports.PdfFileActions
import com.google.gson.Gson
import com.example.sp500auth.ui.theme.SP500AuthTheme
import com.example.sp500auth.util.Resource
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.Before
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File

/** Sesión efímera suministrada fuera de Git. Pruebas de dispositivo contra la API real. */
class Sprint3LiveUiTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    @Before fun waitForEmulatorSystemUi() {
        // El arranque en frío del AVD puede mostrar un aviso del sistema ajeno a la app.
        instrumentation.uiAutomation.rootInActiveWindow?.findAccessibilityNodeInfosByText("Wait")
            ?.firstOrNull()?.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
    }
    private fun remote(): ScopedRemote {
        val file = File(context.filesDir,"sprint3_qa_session.json")
        assumeTrue("Se necesita la sesión de prueba local",file.exists())
        val session = JSONObject(file.readText())
        val login = OkHttpClient().newCall(Request.Builder().url(BuildConfig.API_BASE_URL + "auth/token/")
            .post(session.getJSONObject("qa_login").toString().toRequestBody("application/json".toMediaType())).build()).execute()
        val renewed = login.use { assertTrue("Renovar sesión aislada",it.isSuccessful);JSONObject(it.body!!.string()) }
        val token = renewed.getString("access")
        session.put("access",token).put("refresh",renewed.getString("refresh"));file.writeText(session.toString())
        val api = Retrofit.Builder().baseUrl(BuildConfig.API_BASE_URL).client(OkHttpClient())
            .addConverterFactory(GsonConverterFactory.create()).build()
        return ScopedRemote(api) { token }
    }
    private fun proof(name: String, value: JSONObject) {
        File(context.filesDir,"sprint3_$name.json").writeText(value.toString())
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(context.filesDir,"sprint3_$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
    }
    @Test fun favoritesSynchronizeWithTheBrowserInBothDirections() {
        val remote = remote()
        val session = JSONObject(File(context.filesDir,"sprint3_qa_session.json").readText())
        val source = FavoritesRepository(remote.retrofit) { session.getString("access") }
        val vm = FavoritesViewModel(source)
        compose.setContent { SP500AuthTheme { Column {
            FavoriteAction("MSFT", vm)
            FavoritesScreen(rememberNavController(), vm)
        } } }
        compose.waitUntil(15000) { !vm.busy && vm.visibleState is Resource.Success }
        // El navegador espera este marcador antes de medir la recepción.
        proof("sync_ready",JSONObject().put("ready",true))
        Thread.sleep(8000)
        compose.waitUntil(10000) { !vm.busy && vm.visibleState is Resource.Success }
        compose.waitForIdle()
        val started = System.currentTimeMillis()
        compose.onNodeWithText("☆ Guardar en favoritos").performClick()
        compose.waitUntil(5000) { vm.item("MSFT") != null }
        compose.onNodeWithText("1 empresa favorita").assertExists()
        proof("sync_added",JSONObject().put("added_at_ms",started))
        // El usuario de prueba elimina desde la web; el polling Android debe retirarlo.
        compose.waitUntil(120000) { vm.item("MSFT") == null }
        compose.onNodeWithText("0 empresas favoritas").assertExists()
        proof("sync_removed",JSONObject().put("removed_observed_at_ms",System.currentTimeMillis()))
    }
    @Test fun realComparativePdfOpensWithAndroidPdfRenderer() = runBlocking {
        val remote = remote()
        val source = ReportRepository(remote,AccessRepository(remote))
        val request = ReportRequest(kind="comparison",a="GOOGL",b="META",fiscal_year=2024,include_charts=true)
        val created = source.submit(request)
        assertTrue(created.message,created is Resource.Success)
        var job = (created as Resource.Success).successData
        withTimeout(30000) { while(job.status in setOf("queued","running")) {
            delay(100)
            job = (source.status(job.id!!,request) as Resource.Success).successData
        } }
        assertEquals("ready",job.status)
        val result = source.download(job)
        assertTrue(result.message,result is Resource.Success)
        val file = File(context.cacheDir,"sprint3-check.pdf")
        try {
            file.writeBytes((result as Resource.Success).successData.bytes)
            PdfRenderer(ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY)).use { pdf ->
                assertTrue(pdf.pageCount in 1..2)
                for(index in 0 until pdf.pageCount) pdf.openPage(index).use { page ->
                    assertTrue(page.width > page.height)
                    val bitmap=Bitmap.createBitmap(page.width,page.height,Bitmap.Config.ARGB_8888)
                    page.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    File(context.filesDir,"sprint3_pdf_page_$index.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
                }
            }
        } finally { file.delete() }
        val saved = SavedStateHandle(mapOf("report_owner" to remote.owner(),"report_request" to Gson().toJson(request),"report_id" to job.id))
        val vm = ReportsViewModel(source,context,saved) { _, _ -> throw java.io.IOException("ENOSPC: no space left on device (simulado)") }
        compose.setContent { SP500AuthTheme { Column {
            ReportProgress(job,null,null,null,true,{})
            PdfFileActions(vm.payload != null,vm.savedUri != null,vm.saving,
                {vm.save(null,vm.payload?.sha256)}, {}, {})
            vm.message?.let { Text(it) }
        } } }
        compose.onNodeWithText("PDF disponible").assertExists()
        compose.runOnIdle {vm.resume()}
        compose.waitUntil(15000) {(vm.state as? Resource.Success)?.successData?.status == "ready"}
        compose.runOnIdle {vm.download()}
        compose.waitUntil(15000) {vm.payload != null}
        compose.onNodeWithText("Guardar PDF").performClick()
        compose.onNodeWithText("Guardado cancelado.").assertExists()
        // El escritor de prueba devuelve ENOSPC sin llenar almacenamiento real.
        val full = android.net.Uri.parse("content://sprint3.qa/full.pdf")
        compose.runOnIdle {vm.save(full,vm.payload?.sha256)}
        compose.waitUntil(10000) {!vm.saving && vm.message == "No se pudo completar el guardado."}
        compose.onNodeWithText("No se pudo completar el guardado.").assertExists()
        assertNull(vm.savedUri)
        compose.runOnIdle {vm.pause()}
        proof("pdf_renderer",JSONObject().put("pages",job.page_count).put("size_bytes",job.size_bytes).put("sha256",job.content_sha256))
    }
}
