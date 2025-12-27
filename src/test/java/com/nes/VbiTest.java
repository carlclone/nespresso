package com.nes;

import com.nes.cpu.Cpu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class VbiTest {
    private Bus bus;
    private Ppu ppu;
    private Cpu cpu;
    private Cartridge cart;

    @BeforeEach
    public void setUp() {
        bus = new Bus();
        ppu = bus.getPpu();
        cpu = new Cpu();
        cpu.connectBus(bus);
        bus.connectCpu(cpu);
        
        // Dummy cartridge with NMI vector points to 0x1234
        byte[] prg = new byte[16384];
        prg[0x3FFA] = (byte) 0x34; // Lo
        prg[0x3FFB] = (byte) 0x12; // Hi
        byte[] chr = new byte[8192];
        cart = new Cartridge(prg, chr, 0);
        bus.insertCartridge(cart);
        
        ppu.reset();
        cpu.reset();
        
        // Setup NMI handler at 0x1234
        bus.write(0x1234, (byte) 0x4C); // JMP 0x1234 (infinite loop)
        bus.write(0x1235, (byte) 0x34);
        bus.write(0x1236, (byte) 0x12);
    }

    @Test
    public void testLateNmiEnable() {
        // 1. Run until in VBlank, but NMI is DISABLED
        ppu.cpuWrite(0x2000, (byte) 0x00); // NMI disabled
        
        // Run to scanline 241, cycle 10
        int cycles = 241 * 341 + 10;
        for (int i = 0; i < cycles; i++) {
            bus.clock();
        }
        
        // Verify we are in VBlank but CPU has NOT jumped to 0x1234
        assertTrue((ppu.getPpuStatus() & 0x80) != 0, "Should be in VBlank");
        assertNotEquals(0x1234, cpu.pc, "CPU should not have branched to NMI vector yet");
        
        // 2. Enable NMI LATE (while still in VBlank)
        ppu.cpuWrite(0x2000, (byte) 0x80); // NMI enabled
        
        // Run a few more cycles
        for (int i = 0; i < 50; i++) {
            bus.clock();
        }
        
        // CPU should have jumped to NMI vector now
        assertEquals(0x1234, cpu.pc, "CPU should have jumped to NMI vector after late enable");
    }

    @Test
    public void testPpuStatusReadAtVblankStart() {
        // If we read $2002 at the exact cycle VBlank is set, some docs say it might NOT set the flag 
        // or NOT trigger NMI. Let's see how our current impl handles it.
        
        // TODO: Implement more precise timing test if needed
    }

    @Test
    public void testNmiDoesNotInterruptInstruction() {
        // Setup a long instruction: e.g., ADC ABS,X (4+1 cycles)
        // Let's use something like 0x7D (ADC ABS,X)
        // PC = 0x4000
        int startPc = 0x4000;
        cpu.pc = startPc;
        bus.write(startPc, (byte) 0x7D); // ADC 0x0000, X
        bus.write(startPc + 1, (byte) 0x00);
        bus.write(startPc + 2, (byte) 0x00);
        
        // Enable NMI in PPU
        ppu.cpuWrite(0x2000, (byte) 0x80);
        
        // Run until just before scanline 241
        int cyclesToVblank = 241 * 341;
        for (int i = 0; i < cyclesToVblank - 10; i++) {
            bus.clock();
        }
        
        // Now run the instruction. It takes 4 CPU cycles (12 PPU cycles).
        // Trigger VBlank in the middle of this instruction.
        
        // Current state: cyclesToVblank - 10.
        // After 10 cycles, VBlank triggers.
        
        // Start instruction execution
        // We need to call bus.clock() in steps.
        // First PPU clock of the instruction
        bus.clock(); // PPU clock 1
        
        // Run until scanline 241, cycle 1 triggers (10 more clocks)
        for (int i = 0; i < 9; i++) {
            bus.clock();
        }
        
        // Now at scanline 241, cycle 1. PPU will trigger NMI.
        bus.clock(); 
        
        // Check if CPU PC has ALREADY changed to 0x1234.
        // If it did, it interrupted the ADC instruction.
        assertNotEquals(0x1234, cpu.pc, "CPU should NOT have jumped to NMI mid-instruction");
        
        // Run enough cycles to finish the instruction (ADC ABS,X is 4-5 cycles)
        for (int i = 0; i < 30; i++) {
            bus.clock();
        }
        
        assertEquals(0x1234, cpu.pc, "CPU should jump to NMI AFTER finishing instruction");
    }
}
