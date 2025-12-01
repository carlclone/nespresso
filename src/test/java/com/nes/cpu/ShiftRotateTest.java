package com.nes.cpu;

import com.nes.Bus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ShiftRotateTest {

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
    public void testASL_Acc() {
        cpu.a = 0x01;
        cpu.ASL_Acc();
        assertEquals(0x02, cpu.a);
        assertEquals(0, cpu.getFlag(Cpu.C));
        
        cpu.a = (byte) 0x80;
        cpu.ASL_Acc();
        assertEquals(0x00, cpu.a);
        assertEquals(1, cpu.getFlag(Cpu.C));
        assertEquals(1, cpu.getFlag(Cpu.Z));
    }

    @Test
    public void testLSR_Mem() {
        bus.write(0x1000, (byte) 0x02);
        cpu.LSR(0x1000);
        assertEquals(0x01, bus.read(0x1000));
        assertEquals(0, cpu.getFlag(Cpu.C));
        
        bus.write(0x1000, (byte) 0x01);
        cpu.LSR(0x1000);
        assertEquals(0x00, bus.read(0x1000));
        assertEquals(1, cpu.getFlag(Cpu.C));
        assertEquals(1, cpu.getFlag(Cpu.Z));
    }

    @Test
    public void testROL_Acc() {
        cpu.status |= Cpu.C; // Carry = 1
        cpu.a = 0x01;
        
        cpu.ROL_Acc();
        // (0x01 << 1) | 1 = 0x02 | 1 = 0x03
        assertEquals(0x03, cpu.a);
        assertEquals(0, cpu.getFlag(Cpu.C));
        
        cpu.a = (byte) 0x80;
        cpu.status &= ~Cpu.C; // Carry = 0
        cpu.ROL_Acc();
        // (0x80 << 1) | 0 = 0x00
        // Carry should be 1 (bit 7 was 1)
        assertEquals(0x00, cpu.a);
        assertEquals(1, cpu.getFlag(Cpu.C));
    }

    @Test
    public void testROR_Mem() {
        cpu.status |= Cpu.C; // Carry = 1
        bus.write(0x1000, (byte) 0x01);
        
        cpu.ROR(0x1000);
        // (0x01 >> 1) | (1 << 7) = 0x00 | 0x80 = 0x80
        // Carry = bit 0 of 0x01 = 1
        assertEquals((byte) 0x80, bus.read(0x1000));
        assertEquals(1, cpu.getFlag(Cpu.C));
        assertEquals(1, cpu.getFlag(Cpu.N));
    }
}
