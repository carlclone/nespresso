package com.nes;

/**
 * Represents the NES Bus.
 * Connects the CPU, PPU, and other components to memory.
 */
public class Bus {
    // 64KB RAM
    private final byte[] ram = new byte[64 * 1024];

    // Connected CPU
    // private Cpu cpu; // Will be added later when Cpu is ready
    
    private Cartridge cartridge;

    public Bus() {
        // Initialize RAM to 0
        for (int i = 0; i < ram.length; i++) {
            ram[i] = 0;
        }
    }
    
    public void insertCartridge(Cartridge cartridge) {
        this.cartridge = cartridge;
    }

    /**
     * Read a byte from the bus.
     * @param addr The 16-bit address to read from.
     * @return The byte at the address.
     */
    public byte read(int addr) {
        // Cartridge Address Range
        if (addr >= 0x8000 && addr <= 0xFFFF) {
            if (cartridge != null) {
                return cartridge.cpuRead(addr);
            }
            // Fallback for testing: read from RAM if no cartridge
        }
        
        // RAM (0x0000 - 0x1FFF) - Mirrored every 2KB
        if (addr >= 0x0000 && addr <= 0x1FFF) {
            return ram[addr & 0x07FF];
        }

        // For now, just read from the 64KB RAM array for other areas (like stack 0x0100)
        // This is a temporary fallback until full memory map is implemented
        if (addr >= 0 && addr < ram.length) {
            return ram[addr];
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
        }
        
        // RAM (0x0000 - 0x1FFF) - Mirrored every 2KB
        if (addr >= 0x0000 && addr <= 0x1FFF) {
            ram[addr & 0x07FF] = data;
            return;
        }

        // Temporary fallback
        if (addr >= 0 && addr < ram.length) {
            ram[addr] = data;
        }
    }
}
