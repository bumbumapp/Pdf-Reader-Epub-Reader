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

    /** Card #69: the reader's dark bar (#221f35) must get light icons, also after "Toggle PDF Theme". */
    @Test
    public void readerStatusBarIsPaintedOnEveryLoad() throws Exception {
        assertFalse(EdgeToEdgeInsets.isLight(0xFF221F35));
        String main = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(
                "src/main/java/com/gitlab/mudlej/MjPdfReader/ui/main/MainActivity.kt")), "UTF-8");
        assertTrue(main.contains("EdgeToEdgeInsets.paintStatusBar(this, Color.parseColor(\"#221f35\"))"));
    }
}
