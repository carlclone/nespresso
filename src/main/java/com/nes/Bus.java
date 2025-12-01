package com.nes;

import com.nes.cpu.Cpu;

/**
 * Represents the NES Bus.
 * Connects the CPU, PPU, and other components to memory.
 */
public class Bus {
    // Connected CPU
    private Cpu cpu;

    // 64KB RAM
    private final byte[] ram = new byte[64 * 1024];
    
    private Cartridge cartridge;
    private Ppu ppu = new Ppu();
    private Apu apu = new Apu();
    
    private long systemClockCounter = 0;

    public Bus() {
        // Initialize RAM to 0
        for (int i = 0; i < ram.length; i++) {
            ram[i] = 0;
        }
    }
    
    public void connectCpu(Cpu cpu) {
        this.cpu = cpu;
    }
    
    public void insertCartridge(Cartridge cartridge) {
        this.cartridge = cartridge;
    }
    
    public void clock() {
        // PPU runs 3 times faster than CPU
        // ppu.clock(); 
        
        // CPU runs once every 3 system ticks
        if (systemClockCounter % 3 == 0) {
            if (cpu != null) {
                cpu.clock();
            }
        }
        
        // apu.clock();
        
        systemClockCounter++;
    }

    /**
     * Read a byte from the bus.
     * @param addr The 16-bit address to read from.
     * @return The byte at the address.
     */
    public byte read(int addr) {
        // Cartridge Address Range (0x4020 - 0xFFFF)
        if (addr >= 0x8000 && addr <= 0xFFFF) {
            if (cartridge != null) {
                return cartridge.cpuRead(addr);
            }
            // Fallback for testing: read from RAM if no cartridge
            return ram[addr];
        }
        
        // RAM (0x0000 - 0x1FFF) - Mirrored every 2KB
        if (addr >= 0x0000 && addr <= 0x1FFF) {
            return ram[addr & 0x07FF];
        }

        // PPU Registers (0x2000 - 0x3FFF) - Mirrored every 8 bytes
        if (addr >= 0x2000 && addr <= 0x3FFF) {
            return ppu.cpuRead(addr & 0x2007);
        }
        
        // APU Registers (0x4000 - 0x4017)
        if (addr >= 0x4000 && addr <= 0x4017) {
            return apu.cpuRead(addr);
        }

        return 0x00;
    }

    /**
     * Write a byte to the bus.
     * @param addr The 16-bit address to write to.
     * @param data The byte to write.
     */
    public void write(int addr, byte data) {
        // Cartridge Address Range
        if (addr >= 0x8000 && addr <= 0xFFFF) {
            if (cartridge != null) {
                cartridge.cpuWrite(addr, data);
                return;
            }
            // Fallback for testing: write to RAM if no cartridge
            ram[addr] = data;
            return;
        }
        
        // RAM (0x0000 - 0x1FFF) - Mirrored every 2KB
        if (addr >= 0x0000 && addr <= 0x1FFF) {
            ram[addr & 0x07FF] = data;
            return;
        }
        
        // PPU Registers (0x2000 - 0x3FFF) - Mirrored every 8 bytes
        if (addr >= 0x2000 && addr <= 0x3FFF) {
            ppu.cpuWrite(addr & 0x2007, data);
            return;
        }
        
        // APU Registers (0x4000 - 0x4017)
        if (addr >= 0x4000 && addr <= 0x4017) {
            apu.cpuWrite(addr, data);
            return;
        }
    }
}
