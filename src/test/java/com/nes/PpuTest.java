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
}
