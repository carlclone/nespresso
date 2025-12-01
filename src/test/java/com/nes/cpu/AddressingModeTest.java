package com.nes.cpu;

import com.nes.Bus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AddressingModeTest {

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
    public void testImmediate() {
        // PC points to 0x8000
        // Write value at 0x8000
        bus.write(0x8000, (byte) 0x42);
        
        int addr = cpu.IMM();
        assertEquals(0x8000, addr);
        assertEquals(0x8001, cpu.pc);
    }

    @Test
    public void testZeroPage() {
        bus.write(0x8000, (byte) 0x10); // ZP address 0x10
        int addr = cpu.ZP0();
        assertEquals(0x0010, addr);
    }

    @Test
    public void testZeroPageX() {
        cpu.x = 0x05;
        bus.write(0x8000, (byte) 0x10); // ZP address 0x10
        int addr = cpu.ZPX();
        assertEquals(0x0015, addr);
    }

    @Test
    public void testAbsolute() {
        bus.write(0x8000, (byte) 0x00); // Lo
        bus.write(0x8001, (byte) 0x20); // Hi -> 0x2000
        int addr = cpu.ABS();
        assertEquals(0x2000, addr);
    }
    
    @Test
    public void testIndirectX() {
        cpu.x = 0x04;
        bus.write(0x8000, (byte) 0x20); // Base ZP address
        
        // Effective ZP address = 0x20 + 0x04 = 0x24
        // At 0x0024, store low byte of target
        bus.write(0x0024, (byte) 0x78);
        // At 0x0025, store high byte of target
        bus.write(0x0025, (byte) 0x56);
        
        int addr = cpu.IZX();
        assertEquals(0x5678, addr);
    }
}
