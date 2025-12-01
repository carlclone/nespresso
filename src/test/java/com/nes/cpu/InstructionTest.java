package com.nes.cpu;

import com.nes.Bus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class InstructionTest {

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
    public void testLDA() {
        // Load 0x55 into address 0x1000
        bus.write(0x1000, (byte) 0x55);
        
        // Execute LDA with address 0x1000
        cpu.LDA(0x1000);
        
        assertEquals(0x55, cpu.a);
        assertEquals(0, cpu.getFlag(Cpu.Z));
        assertEquals(0, cpu.getFlag(Cpu.N));
        
        // Test Zero Flag
        bus.write(0x1001, (byte) 0x00);
        cpu.LDA(0x1001);
        assertEquals(0x00, cpu.a);
        assertEquals(1, cpu.getFlag(Cpu.Z));
        
        // Test Negative Flag
        bus.write(0x1002, (byte) 0x80);
        cpu.LDA(0x1002);
        assertEquals((byte)0x80, cpu.a);
        assertEquals(1, cpu.getFlag(Cpu.N));
    }

    @Test
    public void testSTA() {
        cpu.a = 0x42;
        cpu.STA(0x2000);
        
        assertEquals(0x42, bus.read(0x2000));
    }
    
    @Test
    public void testLDX_LDY() {
        bus.write(0x100, (byte) 10);
        cpu.LDX(0x100);
        assertEquals(10, cpu.x);
        
        bus.write(0x101, (byte) 20);
        cpu.LDY(0x101);
        assertEquals(20, cpu.y);
    }
    
    @Test
    public void testSTX_STY() {
        cpu.x = 15;
        cpu.STX(0x3000);
        assertEquals(15, bus.read(0x3000));
        
        cpu.y = 25;
        cpu.STY(0x3001);
        assertEquals(25, bus.read(0x3001));
    }
}
