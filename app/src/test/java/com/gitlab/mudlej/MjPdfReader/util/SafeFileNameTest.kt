package com.gitlab.mudlej.MjPdfReader.util

import org.junit.Assert.assertEquals
import org.junit.Test

class SafeFileNameTest {
    @Test
    fun keepsNormalNames() = assertEquals("Report 1.pdf", safeFileName("Report 1.pdf"))

    @Test
    fun stripsDirectoriesAndIllegalCharacters() {
        assertEquals("evil.pdf", safeFileName("../../evil.pdf"))
        assertEquals("a_b.pdf", safeFileName("a:b.pdf"))
    }

    @Test
    fun neverEmpty() {
        assertEquals("document", safeFileName(null))
        assertEquals("document", safeFileName(""))
        assertEquals("document", safeFileName(".."))
    }
}
