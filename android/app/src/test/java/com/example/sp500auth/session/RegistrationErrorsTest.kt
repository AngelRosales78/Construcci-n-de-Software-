package com.example.sp500auth.session

import com.example.sp500auth.data.repository.registrationExceptionMessage
import com.example.sp500auth.data.repository.registrationHttpError
import com.example.sp500auth.ui.auth.registrationPasswordError
import org.junit.Assert.*
import org.junit.Test
import java.net.ConnectException
import java.net.SocketTimeoutException

class RegistrationErrorsTest {
    @Test fun shortPasswordExplainsRequirements() {
        assertTrue(registrationPasswordError("1234", "1234")!!.contains("8 caracteres"))
    }
    @Test fun passwordMustHaveEveryRequiredCharacterClass() {
        listOf("abcdefgh!", "ABCDEFG1!", "Abcdefgh1", "Abcdefgh_").forEach {
            assertNotNull(registrationPasswordError(it, it))
        }
        assertNull(registrationPasswordError("Ficticia8!", "Ficticia8!"))
    }
    @Test fun confirmationMustMatch() {
        assertTrue(registrationPasswordError("Ficticia8!", "Otra8!")!!.contains("coinciden"))
    }
    @Test fun fieldErrorsAreShown() {
        val message = registrationHttpError(400, """{"email":["Este correo ya existe."],"password":["Usa al menos 8 caracteres."]}""")
        assertTrue(message.contains("Correo: Este correo ya existe."))
        assertTrue(message.contains("Contraseña: Usa al menos 8 caracteres."))
    }
    @Test fun serverErrorNeverDisplaysDebugBody() {
        assertFalse(registrationHttpError(500, "<html>private traceback</html>").contains("private"))
    }
    @Test fun malformedErrorBodyHasSafeFallback() {
        assertTrue(registrationHttpError(400, "<html>invalid</html>").contains("HTTP 400"))
    }
    @Test fun connectionAndTimeoutAreExplained() {
        assertTrue(registrationExceptionMessage(ConnectException()).contains("conectar"))
        assertTrue(registrationExceptionMessage(SocketTimeoutException()).contains("tardó"))
    }
}
