package proxyapp;

import java.awt.Image;
import java.awt.image.BufferedImage;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TrayIconManagerTest {

    @Test
    public void iconImageIsDrawnAndNotEmpty() {
        Image img = TrayIconManager.createImage();
        assertEquals(32, img.getWidth(null));
        assertEquals(32, img.getHeight(null));
        BufferedImage b = (BufferedImage) img;
        int visible = 0;
        for (int x = 0; x < 32; x++) {
            for (int y = 0; y < 32; y++) {
                if ((b.getRGB(x, y) >>> 24) != 0) {
                    visible++;
                }
            }
        }
        assertTrue(visible > 400);
        assertEquals(0, b.getRGB(0, 0) >>> 24);
    }

    @Test
    public void updatingAndRemovingBeforeInstallDoesNothing() {
        TrayIconManager tray = new TrayIconManager();
        tray.update(true, "x");
        tray.message("a", "b");
        tray.remove();
    }
}
