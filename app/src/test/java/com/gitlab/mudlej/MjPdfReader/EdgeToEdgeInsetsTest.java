package com.gitlab.mudlej.MjPdfReader;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EdgeToEdgeInsetsTest {
    @Test
    public void lightBarColorsGetDarkIcons() {
        assertTrue(EdgeToEdgeInsets.isLight(0xFFFFFFFF));
        assertTrue(EdgeToEdgeInsets.isLight(0xFFF5F5F5));
    }

    @Test
    public void darkBarColorsGetLightIcons() {
        assertFalse(EdgeToEdgeInsets.isLight(0xFF000000));
        assertFalse(EdgeToEdgeInsets.isLight(0xFF303F9F));
    }
}
