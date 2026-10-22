package com.shockwave.pdfium

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfiumCoreTest {

    @Test
    fun passwordErrorsAreRecognised() {
        assertTrue(PdfiumCore.isPasswordError(Exception("Password required or incorrect password.")))
        assertTrue(PdfiumCore.isPasswordError(Exception("PASSWORD error")))
        assertFalse(PdfiumCore.isPasswordError(Exception("File not in PDF format or corrupted.")))
        assertFalse(PdfiumCore.isPasswordError(Exception()))
    }

    @Test
    fun passwordExceptionIsAnIOException() {
        // MainActivity relies on PdfPasswordException to ask for a password.
        assertTrue(java.io.IOException::class.java.isAssignableFrom(PdfPasswordException::class.java))
    }
}
