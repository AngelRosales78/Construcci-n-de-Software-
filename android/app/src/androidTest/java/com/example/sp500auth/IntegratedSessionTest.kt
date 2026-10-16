package com.example.sp500auth

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sp500auth.data.local.TokenManager
import com.example.sp500auth.ui.MainActivity
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Runs against the isolated backend documented in docs/Integracion_sprint1.md. */
@RunWith(AndroidJUnit4::class)
class IntegratedSessionTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun waitText(text: String) {
        compose.waitUntil(20000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun registrationReopenAndLogoutUseTheRealBackend() {
        waitText("Crear Cuenta")
        // A biometric action without enrollment must not open a dashboard.
        compose.onNodeWithText("Acceso con Huella Dactilar").performClick()
        waitText("Inicia sesión y activa la huella desde tu perfil.")
        compose.onAllNodesWithText("Explorar · ejemplo").assertCountEquals(0)
        compose.onNodeWithText("Crear Cuenta").performClick()
        waitText("Registro de Usuario")
        val username = "android_${System.currentTimeMillis()}"
        compose.onNodeWithText("Usuario").performTextInput(username)
        compose.onNodeWithText("Email").performTextInput("$username@example.test")
        compose.onNodeWithText("Nombre").performTextInput("Prueba")
        compose.onNodeWithText("Apellido").performTextInput("Android")
        compose.onNodeWithText("Contraseña").performScrollTo().performTextInput("Sprint!Android2026")
        compose.onNodeWithText("Confirmar Contraseña").performScrollTo().performTextInput("Sprint!Android2026")
        compose.onNodeWithText("Registrarse").performScrollTo().performClick()
        waitText("Explorar · ejemplo")
        assertTrue(TokenManager(compose.activity).isUserLoggedIn())
        compose.activityRule.scenario.recreate()
        waitText("Explorar · ejemplo")
        compose.onNodeWithContentDescription("Perfil").performClick()
        waitText(username)
        compose.onNodeWithText("Cerrar sesión").performClick()
        waitText("Crear Cuenta")
        assertFalse(TokenManager(compose.activity).isUserLoggedIn())
        compose.activityRule.scenario.recreate()
        waitText("Crear Cuenta")
        compose.onAllNodesWithText("Explorar · ejemplo").assertCountEquals(0)
    }

    @Test fun invalidPasswordDoesNotOpenDashboard() {
        waitText("Crear Cuenta")
        val fields = compose.onAllNodes(hasSetTextAction())
        fields[0].performTextInput("not-a-valid-user")
        fields[1].performTextInput("invalid-password")
        compose.onNodeWithText("Iniciar sesión").performClick()
        waitText("Usuario, correo o contraseña incorrectos.")
        assertFalse(TokenManager(compose.activity).isUserLoggedIn())
        compose.onAllNodesWithText("Explorar · ejemplo").assertCountEquals(0)
    }
}
