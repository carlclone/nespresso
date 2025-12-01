package com.nes;

/**
 * NES Picture Processing Unit (PPU) - 2C02
 * Handles all graphics rendering for the NES.
 */
public class Ppu {
    
    // PPU Registers (CPU-accessible via 0x2000-0x2007)
    private byte ppuCtrl = 0x00;      // 0x2000 - Control register
    private byte ppuMask = 0x00;      // 0x2001 - Mask register
    private byte ppuStatus = 0x00;    // 0x2002 - Status register
    private byte oamAddr = 0x00;      // 0x2003 - OAM address
    
    // Internal registers
    private int vramAddr = 0x0000;    // Current VRAM address (15 bits)
    private int tempVramAddr = 0x0000; // Temporary VRAM address
    private byte fineX = 0x00;        // Fine X scroll (3 bits)
    private boolean writeToggle = false; // First/second write toggle for PPUSCROLL and PPUADDR
    
    // Data buffer for PPUDATA reads
    private byte dataBuffer = 0x00;
    
    // Memory
    private byte[] vram = new byte[2048];      // 2KB nametable memory
    private byte[] paletteRam = new byte[32];  // 32 bytes palette memory
    private byte[] oam = new byte[256];        // 256 bytes OAM (sprite memory)
    
    // Timing
    private int scanline = 0;   // Current scanline (0-261)
    private int cycle = 0;      // Current cycle (0-340)
    private long frame = 0;     // Frame counter
    
    // Flags
    private boolean nmiOccurred = false;
    private boolean nmiOutput = false;
    
    // Reference to cartridge for CHR-ROM access
    private Cartridge cartridge;
    
    // Reference to bus for NMI triggering
    private Bus bus;
    
    public void connectCartridge(Cartridge cartridge) {
        this.cartridge = cartridge;
    }
    
    public void connectBus(Bus bus) {
        this.bus = bus;
    }
    
    /**
     * Reset PPU to initial state
     */
    public void reset() {
        ppuCtrl = 0x00;
        ppuMask = 0x00;
        ppuStatus = 0x00;
        oamAddr = 0x00;
        
        vramAddr = 0x0000;
        tempVramAddr = 0x0000;
        fineX = 0x00;
        writeToggle = false;
        dataBuffer = 0x00;
        
        scanline = 0;
        cycle = 0;
        frame = 0;
        
        nmiOccurred = false;
        nmiOutput = false;
    }
    
    /**
     * CPU reads from PPU registers (0x2000-0x2007, mirrored)
     */
    public byte cpuRead(int addr) {
        byte data = 0x00;
        
        switch (addr & 0x0007) {
            case 0x0000: // PPUCTRL - Write only
                break;
                
            case 0x0001: // PPUMASK - Write only
                break;
                
            case 0x0002: // PPUSTATUS
                // Read status register
                data = (byte) ((ppuStatus & 0xE0) | (dataBuffer & 0x1F));
                
                // Clear VBlank flag
                ppuStatus &= ~0x80;
                
                // Reset write toggle
                writeToggle = false;
                break;
                
            case 0x0003: // OAMADDR - Write only
                break;
                
            case 0x0004: // OAMDATA
                data = oam[oamAddr & 0xFF];
                break;
                
            case 0x0005: // PPUSCROLL - Write only
                break;
                
            case 0x0006: // PPUADDR - Write only
                break;
                
            case 0x0007: // PPUDATA
                // Read from VRAM
                data = dataBuffer;
                dataBuffer = ppuRead(vramAddr);
                
                // Palette reads are not buffered
                if ((vramAddr & 0x3FFF) >= 0x3F00) {
                    data = dataBuffer;
                }
                
                // Increment VRAM address
                vramAddr += ((ppuCtrl & 0x04) != 0) ? 32 : 1;
                vramAddr &= 0x3FFF;
                break;
        }
        
        return data;
    }
    
    /**
     * CPU writes to PPU registers (0x2000-0x2007, mirrored)
     */
    public void cpuWrite(int addr, byte data) {
        switch (addr & 0x0007) {
            case 0x0000: // PPUCTRL
                ppuCtrl = data;
                
                // Update NMI output
                nmiOutput = (ppuCtrl & 0x80) != 0;
                
                // t: ...BA.. ........ = d: ......BA
                tempVramAddr = (tempVramAddr & 0xF3FF) | ((data & 0x03) << 10);
                break;
                
            case 0x0001: // PPUMASK
                ppuMask = data;
                break;
                
            case 0x0002: // PPUSTATUS - Read only
                break;
                
            case 0x0003: // OAMADDR
                oamAddr = data;
                break;
                
            case 0x0004: // OAMDATA
                oam[oamAddr & 0xFF] = data;
                oamAddr++;
                break;
                
            case 0x0005: // PPUSCROLL
                if (!writeToggle) {
                    // First write: X scroll
                    // t: ....... ...HGFED = d: HGFED...
                    // x:              CBA = d: .....CBA
                    tempVramAddr = (tempVramAddr & 0xFFE0) | ((data & 0xFF) >> 3);
                    fineX = (byte) (data & 0x07);
                    writeToggle = true;
                } else {
                    // Second write: Y scroll
                    // t: CBA..HG FED..... = d: HGFEDCBA
                    tempVramAddr = (tempVramAddr & 0x8FFF) | ((data & 0x07) << 12);
                    tempVramAddr = (tempVramAddr & 0xFC1F) | ((data & 0xF8) << 2);
                    writeToggle = false;
                }
                break;
                
            case 0x0006: // PPUADDR
                if (!writeToggle) {
                    // First write: High byte
                    // t: .FEDCBA ........ = d: ..FEDCBA
                    // t: X...... ........ = 0
                    tempVramAddr = (tempVramAddr & 0x80FF) | ((data & 0x3F) << 8);
                    writeToggle = true;
                } else {
                    // Second write: Low byte
                    // t: ....... HGFEDCBA = d: HGFEDCBA
                    // v                   = t
                    tempVramAddr = (tempVramAddr & 0xFF00) | (data & 0xFF);
                    vramAddr = tempVramAddr;
                    writeToggle = false;
                }
                break;
                
            case 0x0007: // PPUDATA
                // Write to VRAM
                ppuWrite(vramAddr, data);
                
                // Increment VRAM address
                vramAddr += ((ppuCtrl & 0x04) != 0) ? 32 : 1;
                vramAddr &= 0x3FFF;
                break;
        }
    }
    
    /**
     * PPU reads from its own address space
     */
    private byte ppuRead(int addr) {
        addr &= 0x3FFF;
        
        // Pattern tables (0x0000-0x1FFF) - CHR-ROM/RAM
        if (addr < 0x2000) {
            if (cartridge != null) {
                return cartridge.ppuRead(addr);
            }
            return 0x00;
        }
        // Nametables (0x2000-0x3EFF)
        else if (addr < 0x3F00) {
            addr &= 0x0FFF;
            
            // Apply mirroring
            if (cartridge != null) {
                int mirrorMode = cartridge.getMirrorMode();
                if (mirrorMode == 0) { // Horizontal
                    if (addr >= 0x0400 && addr < 0x0800) addr -= 0x0400;
                    if (addr >= 0x0C00) addr -= 0x0400;
                } else if (mirrorMode == 1) { // Vertical
                    if (addr >= 0x0800) addr -= 0x0800;
                }
            }
            
            return vram[addr & 0x07FF];
        }
        // Palette RAM (0x3F00-0x3FFF)
        else {
            addr &= 0x001F;
            
            // Mirroring: 0x3F10/14/18/1C mirror 0x3F00/04/08/0C
            if (addr == 0x0010) addr = 0x0000;
            if (addr == 0x0014) addr = 0x0004;
            if (addr == 0x0018) addr = 0x0008;
            if (addr == 0x001C) addr = 0x000C;
            
            return paletteRam[addr];
        }
    }
    
    /**
     * PPU writes to its own address space
     */
    private void ppuWrite(int addr, byte data) {
        addr &= 0x3FFF;
        
        // Pattern tables (0x0000-0x1FFF) - CHR-ROM/RAM
        if (addr < 0x2000) {
            if (cartridge != null) {
                cartridge.ppuWrite(addr, data);
            }
        }
        // Nametables (0x2000-0x3EFF)
        else if (addr < 0x3F00) {
            addr &= 0x0FFF;
            
            // Apply mirroring
            if (cartridge != null) {
                int mirrorMode = cartridge.getMirrorMode();
                if (mirrorMode == 0) { // Horizontal
                    if (addr >= 0x0400 && addr < 0x0800) addr -= 0x0400;
                    if (addr >= 0x0C00) addr -= 0x0400;
                } else if (mirrorMode == 1) { // Vertical
                    if (addr >= 0x0800) addr -= 0x0800;
                }
            }
            
            vram[addr & 0x07FF] = data;
        }
        // Palette RAM (0x3F00-0x3FFF)
        else {
            addr &= 0x001F;
            
            // Mirroring
            if (addr == 0x0010) addr = 0x0000;
            if (addr == 0x0014) addr = 0x0004;
            if (addr == 0x0018) addr = 0x0008;
            if (addr == 0x001C) addr = 0x000C;
            
            paletteRam[addr] = data;
        }
    }
    
    /**
     * Advance PPU by one cycle
     */
    public void clock() {
        // Visible scanlines (0-239) and pre-render scanline (261)
        if (scanline < 240 || scanline == 261) {
            // TODO: Rendering logic will go here
        }
        
        // Post-render scanline (240) - idle
        
        // VBlank scanlines (241-260)
        if (scanline == 241 && cycle == 1) {
            // Set VBlank flag
            ppuStatus |= 0x80;
            nmiOccurred = true;
            
            // Trigger NMI if enabled
            if (nmiOutput && bus != null) {
                bus.nmi();
            }
        }
        
        // End of VBlank
        if (scanline == 261 && cycle == 1) {
            // Clear VBlank flag
            ppuStatus &= ~0x80;
            nmiOccurred = false;
            
            // Clear sprite 0 hit and sprite overflow
            ppuStatus &= ~0x40;
            ppuStatus &= ~0x20;
        }
        
        // Advance cycle and scanline
        cycle++;
        if (cycle >= 341) {
            cycle = 0;
            scanline++;
            if (scanline >= 262) {
                scanline = 0;
                frame++;
            }
        }
    }
    
    // Getters for debugging
    public int getScanline() { return scanline; }
    public int getCycle() { return cycle; }
    public long getFrame() { return frame; }
}
