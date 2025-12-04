package com.nes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DisplaySyncTest {

    private Bus bus;
    private Ppu ppu;

    @BeforeEach
    public void setUp() {
        bus = new Bus();
        ppu = bus.getPpu();
        bus.reset();
    }

    @Test
    public void testFrameSynchronization() {
        // One frame = 262 scanlines * 341 cycles
        int frameCycles = 262 * 341;
        
        // Initial state
        assertEquals(0, ppu.getScanline(), "Start Scanline should be 0");
        assertEquals(0, ppu.getCycle(), "Start Cycle should be 0");
        assertEquals(0, ppu.getFrame(), "Start Frame should be 0");
        
        // Run one full frame
        for (int i = 0; i < frameCycles; i++) {
            bus.clock();
        }
        
        // Verify state after one frame
        // Cycle and Scanline should wrap around to 0
        assertEquals(0, ppu.getScanline(), "End Scanline should be 0");
        assertEquals(0, ppu.getCycle(), "End Cycle should be 0");
        assertEquals(1, ppu.getFrame(), "Frame count should increment to 1");
        
        // Run another frame
        for (int i = 0; i < frameCycles; i++) {
            bus.clock();
        }
        
        assertEquals(0, ppu.getScanline(), "End Scanline should be 0 (Frame 2)");
        assertEquals(0, ppu.getCycle(), "End Cycle should be 0 (Frame 2)");
        assertEquals(2, ppu.getFrame(), "Frame count should increment to 2");
    }
}
