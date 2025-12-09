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
        ppu = bus.getPpu();
        // ppu.connectBus(bus); // Bus already connected to PPU internally? 
        // Bus.java:38: ppu.connectBus(this); is called in connectCpu.
        // But we don't call connectCpu in test.
        // Let's check Bus constructor. It doesn't connect bus to PPU.
        // We need to ensure PPU has reference to Bus.
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

    @Test
    public void testVRAMIncrement() {
        ppu.reset();
        
        // Mode 0: Increment by 1
        ppu.cpuWrite(0x2000, (byte) 0x00); // PPUCTRL = 0
        ppu.cpuWrite(0x2006, (byte) 0x20); // Address 0x2000
        ppu.cpuWrite(0x2006, (byte) 0x00);
        
        ppu.cpuWrite(0x2007, (byte) 0xFF); // Write data at 0x2000
        ppu.cpuWrite(0x2007, (byte) 0xAA); // Write data at 0x2001
        
        // Read back
        ppu.cpuWrite(0x2006, (byte) 0x20); // Address 0x2000
        ppu.cpuWrite(0x2006, (byte) 0x00);
        ppu.cpuRead(0x2007); // Buffer load (0xFF)
        assertEquals((byte)0xFF, ppu.cpuRead(0x2007)); // Read 0xFF, Buffer load (0xAA)
        assertEquals((byte)0xAA, ppu.cpuRead(0x2007)); // Read 0xAA
        
        // Mode 1: Increment by 32
        ppu.cpuWrite(0x2000, (byte) 0x04); // PPUCTRL bit 2 set
        ppu.cpuWrite(0x2006, (byte) 0x20); // Address 0x2000
        ppu.cpuWrite(0x2006, (byte) 0x00);
        
        ppu.cpuWrite(0x2007, (byte) 0x11); // Write at 0x2000, addr becomes 0x2020
        
        // Verify we are at 0x2020 by writing something else there
        ppu.cpuWrite(0x2007, (byte) 0x22); // Write at 0x2020
        
        // Read back 0x2020
        ppu.cpuWrite(0x2006, (byte) 0x20);
        ppu.cpuWrite(0x2006, (byte) 0x20);
        ppu.cpuRead(0x2007); // Buffer load
        assertEquals((byte)0x22, ppu.cpuRead(0x2007));
    }

    @Test
    public void testNametableMirroring() {
        // 1. Horizontal Mirroring (Default in setUp)
        // NT0 (0x2000) == NT1 (0x2400)
        // NT2 (0x2800) == NT3 (0x2C00)
        
        ppu.cpuWrite(0x2000, (byte) 0x00);
        
        // Write to 0x2000
        ppu.cpuWrite(0x2006, (byte) 0x20);
        ppu.cpuWrite(0x2006, (byte) 0x00);
        ppu.cpuWrite(0x2007, (byte) 0xAA);
        
        // Read from 0x2400 (Should match 0x2000)
        ppu.cpuWrite(0x2006, (byte) 0x24);
        ppu.cpuWrite(0x2006, (byte) 0x00);
        ppu.cpuRead(0x2007); // Buffer
        assertEquals((byte)0xAA, ppu.cpuRead(0x2007));
        
        // Write to 0x2800
        ppu.cpuWrite(0x2006, (byte) 0x28);
        ppu.cpuWrite(0x2006, (byte) 0x00);
        ppu.cpuWrite(0x2007, (byte) 0xBB);
        
        // Read from 0x2C00 (Should match 0x2800)
        ppu.cpuWrite(0x2006, (byte) 0x2C);
        ppu.cpuWrite(0x2006, (byte) 0x00);
        ppu.cpuRead(0x2007); // Buffer
        assertEquals((byte)0xBB, ppu.cpuRead(0x2007));
        
        // 2. Vertical Mirroring
        // Create new cartridge with Vertical Mirroring (Mode 1)
        byte[] prg = new byte[16384];
        byte[] chr = new byte[8192];
        Cartridge vCart = new Cartridge(prg, chr, 0, 1); // Mode 1 = Vertical
        ppu.connectCartridge(vCart);
        ppu.reset();
        
        // NT0 (0x2000) == NT2 (0x2800)
        // NT1 (0x2400) == NT3 (0x2C00)
        
        // Write to 0x2000
        ppu.cpuWrite(0x2006, (byte) 0x20);
        ppu.cpuWrite(0x2006, (byte) 0x00);
        ppu.cpuWrite(0x2007, (byte) 0xCC);
        
        // Read from 0x2800 (Should match 0x2000)
        ppu.cpuWrite(0x2006, (byte) 0x28);
        ppu.cpuWrite(0x2006, (byte) 0x00);
        ppu.cpuRead(0x2007); // Buffer
        assertEquals((byte)0xCC, ppu.cpuRead(0x2007));
        
        // Write to 0x2400
        ppu.cpuWrite(0x2006, (byte) 0x24);
        ppu.cpuWrite(0x2006, (byte) 0x00);
        ppu.cpuWrite(0x2007, (byte) 0xDD);
        
        // Read from 0x2C00 (Should match 0x2400)
        ppu.cpuWrite(0x2006, (byte) 0x2C);
        ppu.cpuWrite(0x2006, (byte) 0x00);
        ppu.cpuRead(0x2007); // Buffer
        assertEquals((byte)0xDD, ppu.cpuRead(0x2007));
    }

    @Test
    public void testSpriteZeroHit() {
        // Create Cartridge with CHR-RAM (chrBanks = 0)
        byte[] prg = new byte[16384];
        byte[] chr = new byte[8192]; // 8KB RAM
        // prgBanks=1, chrBanks=0 (RAM)
        Cartridge ramCart = new Cartridge(prg, chr, 0, 0, 1, 0);
        
        ppu.connectCartridge(ramCart);
        ppu.reset();
        
        // 1. Setup OAM for Sprite 0
        // Y = 10, Tile = 1, Attr = 0, X = 10
        ppu.cpuWrite(0x2003, (byte) 0x00);
        ppu.cpuWrite(0x2004, (byte) 10);
        ppu.cpuWrite(0x2004, (byte) 1);
        ppu.cpuWrite(0x2004, (byte) 0);
        ppu.cpuWrite(0x2004, (byte) 10);
        
        // 2. Setup Pattern Table (CHR-RAM)
        // We need Sprite 0 (Tile 1) to be opaque at the hit point.
        // And Background (Tile 0 by default) to be opaque.
        
        // Set PPUADDR to 0x0010 (Tile 1, Plane 0)
        ppu.cpuWrite(0x2006, (byte) 0x00);
        ppu.cpuWrite(0x2006, (byte) 0x10);
        // Write 0xFF (Solid line)
        for (int i = 0; i < 8; i++) ppu.cpuWrite(0x2007, (byte) 0xFF);
        
        // Set PPUADDR to 0x1000 (Background Tile 0, Plane 0) - Assuming BG uses table 1 (0x1000)
        // Let's configure PPUCTRL to use Table 1 for BG and Table 0 for Sprites.
        // PPUCTRL: 0x10 (Bit 4 = BG Table 1)
        ppu.cpuWrite(0x2000, (byte) 0x10);
        
        ppu.cpuWrite(0x2006, (byte) 0x10);
        ppu.cpuWrite(0x2006, (byte) 0x00);
        for (int i = 0; i < 8; i++) ppu.cpuWrite(0x2007, (byte) 0xFF);
        
        // 3. Setup Nametable
        // We need Tile 0 at (0,0) in Nametable (0x2000).
        // It's 0 by default, but let's be sure.
        ppu.cpuWrite(0x2006, (byte) 0x20);
        ppu.cpuWrite(0x2006, (byte) 0x00);
        ppu.cpuWrite(0x2007, (byte) 0x00);
        
        // 4. Enable Rendering
        // PPUMASK: 0x1E (Show BG, Show Sprites, No clipping)
        ppu.cpuWrite(0x2001, (byte) 0x1E);
        
        // 5. Run until Scanline 10
        // Each scanline is 341 cycles.
        // We expect hit at Scanline 10, Cycle ~10.
        
        // Run 9 full scanlines first
        for (int i = 0; i < 9 * 341; i++) ppu.clock();
        
        // Now in Scanline 9 (0-indexed? No, scanline 0 is first).
        // So we just finished scanline 8.
        // Let's run scanline 9.
        for (int i = 0; i < 341; i++) ppu.clock();
        
        // Now at Scanline 10.
        // Check Status - Should be 0 initially
        assertEquals(0, (ppu.cpuRead(0x2002) & 0x40));
        
        // Run to Cycle 10 (approx)
        for (int i = 0; i < 15; i++) ppu.clock();
        
        // Check Status - Should be 1 (Hit!)
        // Note: Exact cycle depends on pipeline latency, usually +2 or +3 pixels.
        // Sprite X=10 means pixel 10.
        assertEquals(0x40, (ppu.cpuRead(0x2002) & 0x40), "Sprite 0 Hit should be set");
    }

    @Test
    public void testScrollReset() {
        ppu.reset();
        
        // Set Scroll: X=0, Y=0
        // Write to PPUCTRL to set Base Nametable = 0
        ppu.cpuWrite(0x2000, (byte) 0x00);
        
        // Write to PPUSCROLL: X=0, Y=0
        ppu.cpuWrite(0x2005, (byte) 0x00);
        ppu.cpuWrite(0x2005, (byte) 0x00);
        
        // Enable Rendering
        ppu.cpuWrite(0x2001, (byte) 0x1E);
        
        // Run one full frame
        for (int i = 0; i < 262 * 341; i++) ppu.clock();
        
        // Verify VRAM Address is reset to 0 (or close to it) at start of next frame
        // Actually, at Scanline 0, Cycle 0, vramAddr should be equal to tempVramAddr (0).
        
        try {
            java.lang.reflect.Field vramAddrField = Ppu.class.getDeclaredField("vramAddr");
            vramAddrField.setAccessible(true);
            int vramAddr = (int) vramAddrField.get(ppu);
            
            // It should be 0x0002 (Coarse X = 2) due to prefetch cycles (321-336) on pre-render line
            assertEquals(0x0002, vramAddr & 0x7FFF);
            
            // Now set a scroll and verify it resets correctly
            // Set Scroll: X=0, Y=120 (Middle of screen)
            // Y=120 -> Coarse Y = 15 (0x0F), Fine Y = 0
            // PPUSCROLL writes:
            // 1. X=0
            // 2. Y=120
            
            // We need to write to PPUADDR/SCROLL during VBlank or Pre-render to set t.
            // Let's reset and set t manually via registers.
            ppu.reset();
            ppu.cpuWrite(0x2000, (byte) 0x00);
            ppu.cpuWrite(0x2005, (byte) 0x00); // X=0
            ppu.cpuWrite(0x2005, (byte) 120);  // Y=120
            
            // Enable Rendering
            ppu.cpuWrite(0x2001, (byte) 0x1E);
            
            // Run until Pre-render line (261), Cycle 304 (End of vertical copy)
            // 261 * 341 + 304
            int cyclesToCopy = 261 * 341 + 304;
            for (int i = 0; i < cyclesToCopy; i++) ppu.clock();
            
            // Check vramAddr
            // Y=120 -> Coarse Y=15 (001111), Fine Y=0
            // vramAddr should have these bits set.
            // Coarse Y is bits 5-9. 15 << 5 = 480 (0x1E0)
            vramAddr = (int) vramAddrField.get(ppu);
            
            assertEquals(0x01E0, vramAddr & 0x7FFF, "VRAM Address should have Y scroll applied at start of frame");
            
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
        }


    @Test
    public void testCoarseXReset() {
        ppu.reset();
        
        // Setup a scroll position
        // T: ...NN.. ...XXXXX
        // Let's set Coarse X = 10 (0x0A) and Nametable = 1 (0x01)
        // T = 0000 0100 0000 1010 = 0x040A
        
        // We can set T via PPUCTRL (Nametable) and PPUSCROLL (X/Y)
        // PPUCTRL: Set NT=1 (Bit 0)
        ppu.cpuWrite(0x2000, (byte) 0x01);
        
        // PPUSCROLL: X=80 (Coarse X=10, Fine X=0)
        // First write to 2005 sets Coarse X and Fine X in T
        ppu.cpuWrite(0x2005, (byte) 80);
        ppu.cpuWrite(0x2005, (byte) 0); // Y=0
        
        // Enable Rendering
        ppu.cpuWrite(0x2001, (byte) 0x1E);
        
        // Run to Cycle 256 of Scanline 0
        // At this point, VRAM Addr (v) might have incremented Coarse X during rendering.
        // But at Cycle 257, it MUST copy Coarse X and NT X from T.
        
        for (int i = 0; i <= 256; i++) ppu.clock();
        
        // Now at Cycle 257. The copy happens here.
        ppu.clock();
        
        try {
            java.lang.reflect.Field vramAddrField = Ppu.class.getDeclaredField("vramAddr");
            vramAddrField.setAccessible(true);
            int vramAddr = (int) vramAddrField.get(ppu);
            
            // Check Coarse X (Bits 0-4) and NT X (Bit 10)
            // Should match T (0x040A)
            // Note: Other bits (Y scroll) should also match T if we are at Scanline 0?
            // Actually, vramAddr tracks Y scroll, so Y bits might be different if we were further down.
            // But at Scanline 0, Y is 0.
            
            assertEquals(0x0A, vramAddr & 0x001F, "Coarse X should be reset to 10");
            assertEquals(0x0400, vramAddr & 0x0400, "Nametable X should be reset to 1");
            
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
        }


    @Test
    public void testOAMDMA() {
        ppu.reset();
        
        // 1. Fill CPU RAM with known data
        // DMA usually copies from 0xXX00. Let's use page 0x02 (0x0200-0x02FF)
        // This is commonly used for OAM in games.
        for (int i = 0; i < 256; i++) {
            bus.write(0x0200 + i, (byte) (0xFF - i));
        }
        
        // 2. Trigger DMA
        // Write 0x02 to 0x4014
        bus.write(0x4014, (byte) 0x02);
        
        // 3. Verify OAM data
        for (int i = 0; i < 256; i++) {
            // Set OAMADDR to i
            ppu.cpuWrite(0x2003, (byte) i);
            byte data = ppu.cpuRead(0x2004);
            assertEquals((byte) (0xFF - i), data, "OAM byte " + i + " should match RAM");
        }
    }

    @Test
    public void testNMITiming() {
        // Setup CPU
        com.nes.cpu.Cpu cpu = new com.nes.cpu.Cpu();
        cpu.connectBus(bus);
        bus.connectCpu(cpu);
        
        ppu.reset();
        cpu.reset();
        
        // Set NMI Vector (0xFFFA/B) to 0x1234
        // We need to write to RAM/ROM. Since we have a cartridge, writes to 0xFFFA might be ignored 
        // if it's ROM. But our Cartridge class doesn't support writing to PRG ROM.
        // However, Bus.read() falls back to RAM if no cartridge, OR if address is in RAM.
        // But 0xFFFA is in ROM space.
        // We need a way to set the NMI vector.
        // Let's use a custom Cartridge or just write to the bus and hope it falls back?
        // Bus.write(0xFFFA) -> Cartridge.cpuWrite().
        // Cartridge.cpuWrite() does nothing for Mapper 0.
        
        // Workaround: Use reflection to set RAM in Bus or mock Cartridge?
        // Easier: Create a Cartridge with the vector already set.
        byte[] prg = new byte[16384];
        // Set NMI vector at end of PRG (offset 0x3FFA for 16KB PRG mapped at 0xC000)
        // 16KB PRG is mapped to 0x8000-0xBFFF and mirrored to 0xC000-0xFFFF.
        // So 0xFFFA is at offset 0x3FFA in the 16KB array.
        prg[0x3FFA] = (byte) 0x34; // Lo
        prg[0x3FFB] = (byte) 0x12; // Hi
        
        byte[] chr = new byte[8192];
        Cartridge nmiCart = new Cartridge(prg, chr, 0);
        bus.insertCartridge(nmiCart);
        ppu.reset();
        cpu.reset(); // Reset to load start vector (which is also in PRG, but we don't care about start vector here)
        
        // Enable NMI in PPU
        ppu.cpuWrite(0x2000, (byte) 0x88); // Bit 7 = 1 (NMI Enable), Bit 3 = 1 (Bg addr)
        
        // Write infinite loop at 0x1234 so CPU stays there
        // 0x1234 is in RAM (0x0000-0x1FFF)
        bus.write(0x1234, (byte) 0x4C); // JMP
        bus.write(0x1235, (byte) 0x34); // Lo
        bus.write(0x1236, (byte) 0x12); // Hi
        
        // Run PPU to VBlank
        // VBlank at Scanline 241, Cycle 1.
        // Total cycles to run: 241 * 341 + 1
        int cyclesToRun = 241 * 341 + 1;
        
        for (int i = 0; i < cyclesToRun; i++) {
            bus.clock(); // Clock the whole system
        }
        
        // Check if CPU PC is 0x1234
        // CPU clock runs every 3 PPU clocks.
        // NMI takes 7 CPU cycles to process.
        // We need to run enough cycles for CPU to pick it up.
        // After PPU triggers NMI, CPU needs to finish current instruction and then process interrupt.
        // Since we are just idling (executing 0x00 BRK or whatever is in PRG), it might take a few cycles.
        
        // Let's run a bit more to ensure CPU processes it.
        for (int i = 0; i < 100; i++) {
            bus.clock();
        }
        
        // Debug checks
        // 1. Check if VBlank flag is set
        assertEquals(1, (ppu.cpuRead(0x2002) & 0x80) >> 7, "VBlank flag should be set");
        
        // 2. Check if NMI vector is readable
        assertEquals((byte)0x34, bus.read(0xFFFA), "Bus should read NMI vector Lo");
        assertEquals((byte)0x12, bus.read(0xFFFB), "Bus should read NMI vector Hi");
        
        assertEquals(0x1234, cpu.pc, "CPU should jump to NMI vector 0x1234");
    }
}
