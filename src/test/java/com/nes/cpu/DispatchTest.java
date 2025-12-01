package com.nes.cpu;

import com.nes.Bus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DispatchTest {

    private Cpu cpu;
    private Bus bus;

    @BeforeEach
    public void setUp() {
        cpu = new Cpu();
        bus = new Bus();
        cpu.connectBus(bus);
        
        // Initialize Reset Vector
        bus.write(0xFFFC, (byte) 0x00);
        bus.write(0xFFFD, (byte) 0x80);
        
        cpu.reset();
    }

    @Test
    public void testLDA_Immediate_Cycles() {
        // 0xA9: LDA Immediate (2 cycles)
        bus.write(0x8000, (byte) 0xA9);
        bus.write(0x8001, (byte) 0x55);
        
        // Reset sets PC to 0x8000
        // Initial state: cycles = 0 (after reset logic, usually reset takes cycles but here we manually reset)
        // My reset() doesn't set cycles, so it's 0.
        
        // Clock 1: Fetch opcode 0xA9. Cycles -> 2. Execute Addressing (IMM). Execute LDA. Cycles -> 1.
        cpu.clock();
        assertEquals(1, cpu.cycles);
        assertEquals(0x55, cpu.a); // Instruction executed immediately in my model (simplified)
        
        // Clock 2: Cycles -> 0.
        cpu.clock();
        assertEquals(0, cpu.cycles);
    }
    
    @Test
    public void testBRK_Cycles() {
        // 0x00: BRK (7 cycles)
        bus.write(0x8000, (byte) 0x00);
        bus.write(0xFFFE, (byte) 0x00);
        bus.write(0xFFFF, (byte) 0x90);
        
        cpu.clock();
        assertEquals(6, cpu.cycles); // 7 - 1
        assertEquals(0x9000, cpu.pc);
    }
}
