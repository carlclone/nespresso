package com.nes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PpuRenderingTest {

    private Ppu ppu;
    private Bus bus;
    private Cartridge cartridge;

    @BeforeEach
    public void setUp() {
        bus = new Bus();
        ppu = bus.getPpu();
        // bus.connectPpu(ppu); // Removed as Bus creates its own PPU
        
        // Create a dummy cartridge
        // 16KB PRG, 8KB CHR
        byte[] prg = new byte[16384];
        byte[] chr = new byte[8192];
        cartridge = new Cartridge(prg, chr, 0);
        
        ppu.connectCartridge(cartridge);
        ppu.reset();
    }

    @Test
    public void testBackgroundRendering() {
        // 1. Setup Palette
        // Palette 0, Color 1 = 0x30 (White)
        ppu.cpuWrite(0x2006, (byte) 0x3F);
        ppu.cpuWrite(0x2006, (byte) 0x01);
        ppu.cpuWrite(0x2007, (byte) 0x30); // White
        
        // 2. Setup Nametable (Tile 1 at top-left)
        ppu.cpuWrite(0x2006, (byte) 0x20);
        ppu.cpuWrite(0x2006, (byte) 0x00);
        ppu.cpuWrite(0x2007, (byte) 0x01); // Tile ID 0x01
        
        // 3. Setup Pattern Table (Tile 0x01 is solid)
        // Pattern table 0 is at 0x0000
        // Tile 1 starts at 0x0010
        // We need to write to CHR-RAM (if cartridge allows) or mock it.
        // Since our Cartridge uses a byte array for CHR, we can write to it via PPU if it's treated as RAM or if we use a helper.
        // But wait, standard Mapper 0 has CHR-ROM usually. Our Cartridge class might need checking.
        // Let's assume for this test we can write to PPU address space 0x0000-0x1FFF if it's RAM, 
        // OR we can just modify the cartridge array directly if we had access.
        // Let's try writing via PPUADDR/DATA. If it fails (read-only), we'll know.
        
        // Write solid pattern for Tile 1
        ppu.cpuWrite(0x2006, (byte) 0x00);
        ppu.cpuWrite(0x2006, (byte) 0x10); // Address 0x0010
        for (int i = 0; i < 8; i++) ppu.cpuWrite(0x2007, (byte) 0xFF); // Low plane all 1s
        for (int i = 0; i < 8; i++) ppu.cpuWrite(0x2007, (byte) 0x00); // High plane all 0s
        // Result: Color index 1
        
        // 4. Enable Background Rendering
        // PPUCTRL: Background table 0 (0x0000)
        ppu.cpuWrite(0x2000, (byte) 0x00); 
        // PPUMASK: Show background (Bit 3), Show background in left 8 pixels (Bit 1)
        ppu.cpuWrite(0x2001, (byte) 0x0A); 
        
        // 5. Run PPU for one scanline
        // Pre-render scanline (-1 or 261) -> Scanline 0
        // We need to clock enough times to get past the pre-render and into visible scanline 0.
        
        // Advance to Scanline 0, Cycle 0
        while (ppu.getScanline() != 0) {
            ppu.clock();
        }
        
        // Run through Scanline 0
        // First 8 cycles will fetch the first tile.
        // Pixels should start appearing after that.
        for (int i = 0; i < 20; i++) {
            ppu.clock();
        }
        
        // 6. Check Frame Buffer
        // Pixel at (0,0) should be Color 1 (White -> 0x30 -> RGB value)
        int[] buffer = ppu.getFrameBuffer();
        int pixel = buffer[0];
        
        System.out.println("Pixel at 0,0: " + String.format("0x%06X", pixel));
        
        // NES Palette 0x30 is usually white/bright (approx 0xFFFFFF or similar)
        // It should NOT be black (0x000000) or the default grey.
        assertNotEquals(0, pixel, "Pixel should not be black");
        assertNotEquals(0xFF000000, pixel, "Pixel should not be transparent black");
    }
}
