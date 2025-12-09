package com.nes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PPU registers and timing
 */
public class PpuTest {

    private Ppu ppu;
    private Bus bus;
    private Cartridge cart;

    @BeforeEach
    public void setUp() {
        // Create dummy cartridge
        byte[] prg = new byte[16384];
        byte[] chr = new byte[8192];
        cart = new Cartridge(prg, chr, 0);
        
        bus = new Bus();
        ppu = new Ppu();
        ppu.connectBus(bus);
        ppu.connectCartridge(cart);
        ppu.reset();
    }

    @Test
    public void testPPUSTATUSRead() {
        // Write to PPUSTATUS should have no effect (read-only)
        ppu.cpuWrite(0x2002, (byte) 0xFF);
        
        // Initially, VBlank should be clear
        byte status = ppu.cpuRead(0x2002);
        assertEquals(0, (status & 0x80) >> 7); // VBlank flag should be 0
    }

    @Test
    public void testPPUADDRWrite() {
        // Write high byte
        ppu.cpuWrite(0x2006, (byte) 0x20);
        // Write low byte
        ppu.cpuWrite(0x2006, (byte) 0x00);
        
        // Now write data to VRAM
        ppu.cpuWrite(0x2007, (byte) 0x42);
        
        // Reset address
        ppu.cpuWrite(0x2006, (byte) 0x20);
        ppu.cpuWrite(0x2006, (byte) 0x00);
        
        // Read back (first read is buffered)
        ppu.cpuRead(0x2007); // Dummy read
        byte data = ppu.cpuRead(0x2007);
        assertEquals(0x42, data);
    }

    @Test
    public void testVBlankTiming() {
        // Run PPU until VBlank
        // VBlank starts at scanline 241, cycle 1
        // Each scanline is 341 cycles
        
        // Run to scanline 241, cycle 1 (and complete it)
        int targetCycle = 241 * 341 + 2; // +2 to ensure cycle 1 completes
        for (int i = 0; i < targetCycle; i++) {
            ppu.clock();
        }
        
        // VBlank should be set
        byte status = ppu.cpuRead(0x2002);
        assertEquals(1, (status & 0x80) >> 7); // VBlank flag should be 1
        
        // Reading PPUSTATUS clears VBlank, so read again to verify it's cleared
        status = ppu.cpuRead(0x2002);
        assertEquals(0, (status & 0x80) >> 7); // Should be cleared after read
    }

    @Test
    public void testPaletteRAM() {
        // Write to palette RAM (0x3F00)
        ppu.cpuWrite(0x2006, (byte) 0x3F);
        ppu.cpuWrite(0x2006, (byte) 0x00);
        ppu.cpuWrite(0x2007, (byte) 0x30); // Write color
        
        // Read back
        ppu.cpuWrite(0x2006, (byte) 0x3F);
        ppu.cpuWrite(0x2006, (byte) 0x00);
        byte color = ppu.cpuRead(0x2007); // Palette reads are not buffered
        assertEquals(0x30, color);
    }

    @Test
    public void testOAMData() {
        // Set OAM address
        ppu.cpuWrite(0x2003, (byte) 0x00);
        
        // Write to OAM
        ppu.cpuWrite(0x2004, (byte) 0x10); // Y position
        ppu.cpuWrite(0x2004, (byte) 0x20); // Tile index
        ppu.cpuWrite(0x2004, (byte) 0x00); // Attributes
        ppu.cpuWrite(0x2004, (byte) 0x30); // X position
        
        // Reset OAM address
        ppu.cpuWrite(0x2003, (byte) 0x00);
        
        // Read back
        assertEquals(0x10, ppu.cpuRead(0x2004));
    }

    @Test
    public void testFrameCounter() {
        assertEquals(0, ppu.getFrame());
        
        // Run one complete frame (262 scanlines * 341 cycles)
        int frameCycles = 262 * 341;
        for (int i = 0; i < frameCycles; i++) {
            ppu.clock();
        }
        
        assertEquals(1, ppu.getFrame());
    }

    @Test
    public void testYScrollProgression() {
        // Enable background rendering
        ppu.cpuWrite(0x2001, (byte) 0x08); // Show background
        
        // Initial VRAM address should be 0 (or whatever was set)
        // Let's set it to 0
        ppu.cpuWrite(0x2006, (byte) 0x00);
        ppu.cpuWrite(0x2006, (byte) 0x00);
        
        // We need to access vramAddr. Since it's private, we might need to infer it 
        // or use reflection. But wait, we can check the behavior by reading PPUDATA?
        // No, reading PPUDATA increments vramAddr.
        
        // Instead, let's rely on the fact that if Y scroll works, 
        // we should be rendering different rows.
        // But we can't easily check "rendering" without looking at the framebuffer.
        
        // Let's use reflection to inspect vramAddr for this test, 
        // as it's the most direct way to verify the internal state.
        
        try {
            java.lang.reflect.Field vramAddrField = Ppu.class.getDeclaredField("vramAddr");
            vramAddrField.setAccessible(true);
            
            int previousTotalY = -1;
            
            // Run for 240 scanlines
            for (int scanline = 0; scanline < 240; scanline++) {
                // Run to cycle 256 (where Y increment happens)
                // Current cycle is 0.
                for (int c = 0; c < 256; c++) {
                    ppu.clock();
                }
                
                // Now at cycle 256. Y increment happens HERE.
                ppu.clock(); // Cycle 256 -> 257
                
                // Check vramAddr Y component
                int currentVramAddr = (int) vramAddrField.get(ppu);
                int fineY = (currentVramAddr & 0x7000) >> 12;
                int coarseY = (currentVramAddr & 0x03E0) >> 5;
                int totalY = coarseY * 8 + fineY;
                
                // Y should increment by 1
                if (previousTotalY != -1) {
                    // Handle wrapping at 240 (if applicable, though usually it wraps at 256 in pure math, but PPU logic wraps at 240)
                    // The incrementScrollY logic wraps Coarse Y at 29 (29*8 + 7 = 239).
                    // So 239 -> 0.
                    
                    if (previousTotalY == 239) {
                        assertEquals(0, totalY, "Y scroll should wrap to 0 at scanline " + scanline);
                    } else {
                        assertEquals(previousTotalY + 1, totalY, "Y scroll should increment by 1 at scanline " + scanline);
                    }
                }
                previousTotalY = totalY;
                
                // Finish the scanline (341 cycles total)
                // We did 257 cycles. Need 341 - 257 = 84 more.
                for (int c = 0; c < 84; c++) {
                    ppu.clock();
                }
            }
            
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }

    @Test
    public void testXScrollProgression() {
        // Enable background rendering
        ppu.cpuWrite(0x2001, (byte) 0x08); // Show background
        
        // Initial VRAM address 0
        ppu.cpuWrite(0x2006, (byte) 0x00);
        ppu.cpuWrite(0x2006, (byte) 0x00);
        
        try {
            java.lang.reflect.Field vramAddrField = Ppu.class.getDeclaredField("vramAddr");
            vramAddrField.setAccessible(true);
            
            // Run one scanline
            // X scroll increments at cycle 3, 11, 19... (every 8 cycles)? 
            // Actually, fetch happens every 8 cycles, and scroll increments at cycle 7, 15, 23...
            // Let's check the code: incrementScrollX() is called at cycle % 8 == 7.
            
            int previousCoarseX = -1;
            
            // We are at Scanline 0, Cycle 0.
            // Run through the visible part of the scanline (0-255)
            for (int c = 0; c <= 256; c++) {
                ppu.clock();
                
                // Check X increment
                // It happens at 7, 15, 23... 247, 255.
                if ((c % 8) == 7 && c < 256) {
                    int currentVramAddr = (int) vramAddrField.get(ppu);
                    int coarseX = currentVramAddr & 0x001F;
                    
                    if (previousCoarseX != -1) {
                        if (previousCoarseX < 31) {
                            assertEquals(previousCoarseX + 1, coarseX, "Coarse X should increment at cycle " + c);
                        } else {
                            assertEquals(0, coarseX, "Coarse X should wrap to 0 at cycle " + c);
                            // Check nametable flip if needed
                        }
                    }
                    previousCoarseX = coarseX;
                }
            }
            
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }
}
