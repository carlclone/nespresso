package com.nes.cpu;

import com.nes.Bus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BranchJumpTest {

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
    public void testBranchTaken() {
        // PC at 0x8000
        cpu.status = 0; // Clear flags (Z=0)
        
        // BNE +5 (0x05)
        // BNE is taken if Z=0
        cpu.BNE(0x05);
        
        assertEquals(0x8005, cpu.pc);
    }

    @Test
    public void testBranchNotTaken() {
        // PC at 0x8000
        cpu.status |= Cpu.Z; // Set Z=1
        
        // BNE +5
        // BNE is NOT taken if Z=1
        cpu.BNE(0x05);
        
        assertEquals(0x8000, cpu.pc);
    }

    @Test
    public void testBranchBackward() {
        // PC at 0x8010
        cpu.pc = 0x8010;
        cpu.status = 0;
        
        // BNE -5 (0xFB) -> Signed int -5
        cpu.BNE(-5);
        
        assertEquals(0x800B, cpu.pc);
    }

    @Test
    public void testJMP() {
        cpu.JMP(0x1234);
        assertEquals(0x1234, cpu.pc);
    }

    @Test
    public void testJSR_RTS() {
        // PC at 0x8000
        // Call JSR to 0x9000
        cpu.JSR(0x9000);
        
        assertEquals(0x9000, cpu.pc);
        // Stack should contain 0x8000 - 1 = 0x7FFF ? 
        // No, JSR pushes PC-1. PC was 0x8000 (assumed after fetch).
        // In real execution, PC would have advanced past the instruction bytes.
        // Here we manually set PC and call JSR.
        // If we assume manual call:
        // JSR pushes 0x7FFF.
        
        // Now RTS
        cpu.RTS(0); // Operand ignored
        
        assertEquals(0x8000, cpu.pc);
    }
}
