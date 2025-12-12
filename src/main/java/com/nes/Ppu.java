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
    
    // Frame buffer (256x240 pixels, RGB format)
    private int[] frameBuffer = new int[256 * 240];
    
    // Background rendering state
    private int bgNextTileId = 0;
    private int bgNextTileAttrib = 0;
    private int bgNextTileLsb = 0;
    private int bgNextTileMsb = 0;
    private int bgShifterPatternLo = 0;
    private int bgShifterPatternHi = 0;
    private int bgShifterAttribLo = 0;
    private int bgShifterAttribHi = 0;
    
    // Sprite rendering state
    private byte[] secondaryOam = new byte[32]; // 8 sprites * 4 bytes
    private int spriteCount = 0; // Number of sprites found for next scanline
    
    // Sprite shift registers (8 sprites)
    private int[] spriteShifterPatternLo = new int[8];
    private int[] spriteShifterPatternHi = new int[8];
    
    // Sprite counters and latches
    private int[] spriteX = new int[8];      // X position counters
    private int[] spriteAttribute = new int[8]; // Attribute latches
    
    // Sprite 0 hit detection
    private boolean bSpriteZeroHitPossible = false;
    private boolean bSpriteZeroBeingRendered = false;
    
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
    
    // NES Color Palette (64 colors in RGB format)
    private static final int[] NES_PALETTE = {
        0x666666, 0x002A88, 0x1412A7, 0x3B00A4, 0x5C007E, 0x6E0040, 0x6C0600, 0x561D00,
        0x333500, 0x0B4800, 0x005200, 0x004F08, 0x00404D, 0x000000, 0x000000, 0x000000,
        0xADADAD, 0x155FD9, 0x4240FF, 0x7527FE, 0xA01ACC, 0xB71E7B, 0xB53120, 0x994E00,
        0x6B6D00, 0x388700, 0x0C9300, 0x008F32, 0x007C8D, 0x000000, 0x000000, 0x000000,
        0xFFFEFF, 0x64B0FF, 0x9290FF, 0xC676FF, 0xF36AFF, 0xFE6ECC, 0xFE8170, 0xEA9E22,
        0xBCBE00, 0x88D800, 0x5CE430, 0x45E082, 0x48CDDE, 0x4F4F4F, 0x000000, 0x000000,
        0xFFFEFF, 0xC0DFFF, 0xD3D2FF, 0xE8C8FF, 0xFBC2FF, 0xFEC4EA, 0xFECCC5, 0xF7D8A5,
        0xE4E594, 0xCFEF96, 0xBDF4AB, 0xB3F3CC, 0xB5EBF2, 0xB8B8B8, 0x000000, 0x000000
    };
    
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
        
        // Initialize palette RAM with default colors to avoid grey screen
        // Background palette 0
        paletteRam[0] = 0x0F;  // Black background
        paletteRam[1] = 0x00;  // Dark grey
        paletteRam[2] = 0x10;  // Light grey  
        paletteRam[3] = 0x30;  // White
        
        // Reset sprite state
        spriteCount = 0;
        bSpriteZeroHitPossible = false;
        bSpriteZeroBeingRendered = false;
        for (int i = 0; i < 8; i++) {
            spriteShifterPatternLo[i] = 0;
            spriteShifterPatternHi[i] = 0;
            spriteX[i] = 0;
            spriteAttribute[i] = 0;
        }
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
                
                // Debug Log for F1 Race
                // System.out.println("Write $2000: " + Integer.toHexString(data & 0xFF) + " at SL:" + scanline + " CYC:" + cycle);
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
                    // Debug Log for F1 Race
                    if (scanline > 0 && scanline < 240)
                         System.out.println("Write $2005 (X) Val=" + (data & 0xFF) + " FineX=" + fineX + " at SL:" + scanline + " CYC:" + cycle);
                } else {
                    // Second write: Y scroll
                    // t: CBA..HG FED..... = d: HGFEDCBA
                    tempVramAddr = (tempVramAddr & 0x8FFF) | ((data & 0x07) << 12);
                    tempVramAddr = (tempVramAddr & 0xFC1F) | ((data & 0xF8) << 2);
                    writeToggle = false;
                    // Debug Log for F1 Race
                    if (scanline > 0 && scanline < 240)
                         System.out.println("Write $2005 (Y) Val=" + (data & 0xFF) + " at SL:" + scanline + " CYC:" + cycle);
                }
                break;
                
            case 0x0006: // PPUADDR
                if (!writeToggle) {
                    // First write: High byte
                    // t: .FEDCBA ........ = d: ..FEDCBA
                    // t: X...... ........ = 0
                    tempVramAddr = (tempVramAddr & 0x80FF) | ((data & 0x3F) << 8);
                    writeToggle = true;
                     if (scanline > 0 && scanline < 240)
                         System.out.println("Write $2006 (Hi) Val=" + (data & 0xFF) + " at SL:" + scanline + " CYC:" + cycle);
                } else {
                    // Second write: Low byte
                    // t: ....... HGFEDCBA = d: HGFEDCBA
                    // v                   = t
                    tempVramAddr = (tempVramAddr & 0xFF00) | (data & 0xFF);
                    vramAddr = tempVramAddr;
                    writeToggle = false;
                     if (scanline > 0 && scanline < 240)
                         System.out.println("Write $2006 (Lo) Val=" + (data & 0xFF) + " vramAddr=" + Integer.toHexString(vramAddr) + " at SL:" + scanline + " CYC:" + cycle);
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
    /**
     * PPU reads from its own address space
     */
    byte ppuRead(int addr) {
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
                    // NT0 (0x000) & NT1 (0x400) -> VRAM A (0x000)
                    // NT2 (0x800) & NT3 (0xC00) -> VRAM B (0x400)
                    if ((addr & 0x0800) != 0) {
                        addr = 0x0400 | (addr & 0x03FF);
                    } else {
                        addr = addr & 0x03FF;
                    }
                } else if (mirrorMode == 1) { // Vertical
                    // NT0 (0x000) & NT2 (0x800) -> VRAM A (0x000)
                    // NT1 (0x400) & NT3 (0xC00) -> VRAM B (0x400)
                    addr = addr & 0x07FF;
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
    void ppuWrite(int addr, byte data) {
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
                    if ((addr & 0x0800) != 0) {
                        addr = 0x0400 | (addr & 0x03FF);
                    } else {
                        addr = addr & 0x03FF;
                    }
                } else if (mirrorMode == 1) { // Vertical
                    addr = addr & 0x07FF;
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
    
    private void fetchNametableByte() {
        int addr = 0x2000 | (vramAddr & 0x0FFF);
        bgNextTileId = ppuRead(addr) & 0xFF;
    }
    
    private void fetchAttributeByte() {
        int addr = 0x23C0 | (vramAddr & 0x0C00) | ((vramAddr >> 4) & 0x38) | ((vramAddr >> 2) & 0x07);
        bgNextTileAttrib = ppuRead(addr) & 0xFF;
        if ((vramAddr & 0x0040) != 0) bgNextTileAttrib >>= 4;
        if ((vramAddr & 0x0002) != 0) bgNextTileAttrib >>= 2;
        bgNextTileAttrib &= 0x03;
    }
    
    private void fetchPatternLow() {
        int fineY = (vramAddr >> 12) & 0x07;
        int table = (ppuCtrl & 0x10) != 0 ? 0x1000 : 0x0000;
        int addr = table + (bgNextTileId << 4) + fineY;
        bgNextTileLsb = ppuRead(addr) & 0xFF;
    }
    
    private void fetchPatternHigh() {
        int fineY = (vramAddr >> 12) & 0x07;
        int table = (ppuCtrl & 0x10) != 0 ? 0x1000 : 0x0000;
        int addr = table + (bgNextTileId << 4) + fineY + 8;
        bgNextTileMsb = ppuRead(addr) & 0xFF;
    }
    
    private void incrementScrollX() {
        if ((ppuMask & 0x18) == 0) return;
        if ((vramAddr & 0x001F) == 31) {
            vramAddr &= ~0x001F;
            vramAddr ^= 0x0400;
        } else {
            vramAddr++;
        }
    }
    
    private void incrementScrollY() {
        if ((ppuMask & 0x18) == 0) return;
        if ((vramAddr & 0x7000) != 0x7000) {
            vramAddr += 0x1000;
        } else {
            vramAddr &= ~0x7000;
            int y = (vramAddr & 0x03E0) >> 5;
            if (y == 29) {
                y = 0;
                vramAddr ^= 0x0800;
            } else if (y == 31) {
                y = 0;
            } else {
                y++;
            }
            vramAddr = (vramAddr & ~0x03E0) | (y << 5);
        }
    }
    
    /**
     * Evaluate sprites for the next scanline (Cycles 257-320)
     */
    private void evaluateSprites() {
        // Clear sprite count for next scanline
        spriteCount = 0;
        
        // Clear secondary OAM (conceptually)
        for (int i = 0; i < 32; i++) {
            secondaryOam[i] = (byte) 0xFF;
        }
        
        // Target scanline is the NEXT one
        int targetScanline = (scanline == 261) ? 0 : scanline + 1;
        
        // Sprite evaluation for next scanline
        int oamEntry = 0;
        while (oamEntry < 64 && spriteCount < 8) {
            int diff = targetScanline - (oam[oamEntry * 4] & 0xFF);
            
            // Check if sprite is visible on this scanline
            // Standard sprites are 8x8, so diff should be >= 0 and < 8
            // TODO: Add 8x16 support later
            int spriteHeight = (ppuCtrl & 0x20) != 0 ? 16 : 8;
            
            if (diff >= 0 && diff < spriteHeight) {
                // Sprite is visible, copy to secondary OAM
                if (spriteCount < 8) {
                    // Copy 4 bytes: Y, Tile ID, Attribute, X
                    secondaryOam[spriteCount * 4 + 0] = oam[oamEntry * 4 + 0];
                    secondaryOam[spriteCount * 4 + 1] = oam[oamEntry * 4 + 1];
                    secondaryOam[spriteCount * 4 + 2] = oam[oamEntry * 4 + 2];
                    secondaryOam[spriteCount * 4 + 3] = oam[oamEntry * 4 + 3];
                    spriteCount++;
                }
            }
            oamEntry++;
        }
        
        // Sprite overflow flag logic would go here (if > 8 sprites found)
        if (spriteCount == 8) {
            ppuStatus |= 0x20;
        }
    }
    
    /**
     * Fetch sprite patterns for the next scanline (Cycles 321-340 or end of scanline)
     */
    private void fetchSpritePatterns() {
        // For each visible sprite, fetch its pattern data
        for (int i = 0; i < spriteCount; i++) {
            int spriteY = (secondaryOam[i * 4 + 0] & 0xFF);
            int spriteTileId = (secondaryOam[i * 4 + 1] & 0xFF);
            int spriteAttrib = (secondaryOam[i * 4 + 2] & 0xFF);
            int spriteXPos = (secondaryOam[i * 4 + 3] & 0xFF);
            
            // Determine address of sprite pattern
            int addr = 0;
            int spriteHeight = (ppuCtrl & 0x20) != 0 ? 16 : 8;
            boolean flipV = (spriteAttrib & 0x80) != 0;
            
            int targetScanline = (scanline == 261) ? 0 : scanline + 1;
            int row = targetScanline - spriteY;
            if (flipV) {
                row = (spriteHeight - 1) - row;
            }
            
            // 8x8 Sprites
            if (spriteHeight == 8) {
                int table = (ppuCtrl & 0x08) != 0 ? 0x1000 : 0x0000;
                addr = table + (spriteTileId << 4) + row;
            }
            // 8x16 Sprites
            else {
                int table = (spriteTileId & 0x01) != 0 ? 0x1000 : 0x0000;
                spriteTileId &= 0xFE;
                if (row < 8) {
                    addr = table + (spriteTileId << 4) + row;
                } else {
                    addr = table + ((spriteTileId + 1) << 4) + (row - 8);
                }
            }
            
            // Fetch Low and High pattern bytes
            int patternLo = ppuRead(addr);
            int patternHi = ppuRead(addr + 8);
            
            // Flip Horizontally if needed
            if ((spriteAttrib & 0x40) != 0) {
                patternLo = flipByte(patternLo);
                patternHi = flipByte(patternHi);
            }
            
            // Load into shifters
            spriteShifterPatternLo[i] = patternLo;
            spriteShifterPatternHi[i] = patternHi;
            spriteX[i] = spriteXPos;
            spriteAttribute[i] = spriteAttrib;
        }
    }
    
    // Helper to flip a byte horizontally
    private int flipByte(int b) {
        b = (b & 0xF0) >> 4 | (b & 0x0F) << 4;
        b = (b & 0xCC) >> 2 | (b & 0x33) << 2;
        b = (b & 0xAA) >> 1 | (b & 0x55) << 1;
        return b & 0xFF;
    }
    
    /**
     * Render a single pixel at the current cycle
     */
    private void renderPixel() {
        int bgPixel = 0;
        int bgPalette = 0;
        
        // 1. Background Pixel
        if ((ppuMask & 0x08) != 0) {
            int bitMux = 0x8000 >> fineX;
            
            int p0 = (bgShifterPatternLo & bitMux) != 0 ? 1 : 0;
            int p1 = (bgShifterPatternHi & bitMux) != 0 ? 2 : 0;
            bgPixel = p1 | p0;
            
            int pal0 = (bgShifterAttribLo & bitMux) != 0 ? 1 : 0;
            int pal1 = (bgShifterAttribHi & bitMux) != 0 ? 2 : 0;
            bgPalette = pal1 | pal0;
        }
        
        // 2. Sprite Pixel
        int spritePixel = 0;
        int spritePalette = 0;
        boolean spritePriority = false; // True = behind background
        boolean spriteZeroBeingRendered = false;
        
        if ((ppuMask & 0x10) != 0) {
            // Iterate through all 8 possible sprites
            for (int i = 0; i < spriteCount; i++) {
                // Check if sprite is at current X position
                if (spriteX[i] == 0) {
                    // This logic was flawed in previous attempt.
                    // We need to check if the current cycle is within the sprite's X range.
                }
            }
            
            // Revised Sprite Logic:
            // Iterate all sprites. If (cycle - 1) is within [spriteX, spriteX + 7]
            // Then this sprite contributes to the pixel.
            // First non-transparent sprite wins.
            for (int i = 0; i < spriteCount; i++) {
                int offset = (cycle - 1) - spriteX[i];
                if (offset >= 0 && offset < 8) {
                    // Pixel is inside sprite
                    // Get bit from shifter (MSB first usually, but we flipped if needed)
                    // Since we flipped, we can just take bit (7 - offset)
                    int bit = 0x80 >> offset;
                    int p0 = (spriteShifterPatternLo[i] & bit) != 0 ? 1 : 0;
                    int p1 = (spriteShifterPatternHi[i] & bit) != 0 ? 2 : 0;
                    int pixel = p1 | p0;
                    
                    if (pixel != 0) {
                        // Found a non-transparent sprite pixel
                        if (spritePixel == 0) { // Only take the first one (highest priority)
                            spritePixel = pixel;
                            spritePalette = (spriteAttribute[i] & 0x03) + 4; // Sprites use palette 4-7
                            spritePriority = (spriteAttribute[i] & 0x20) != 0;
                            
                            if (i == 0) {
                                spriteZeroBeingRendered = true;
                            }
                        }
                        break; // Stop checking other sprites
                    }
                }
            }
        }
        
        // 3. Priority Multiplexer
        int finalPixel = 0;
        int finalPalette = 0;
        
        if (bgPixel == 0 && spritePixel == 0) {
            // Both transparent: Background color (Palette 0, Index 0)
            finalPixel = 0;
            finalPalette = 0;
        } else if (bgPixel == 0 && spritePixel > 0) {
            // Only sprite
            finalPixel = spritePixel;
            finalPalette = spritePalette;
        } else if (bgPixel > 0 && spritePixel == 0) {
            // Only background
            finalPixel = bgPixel;
            finalPalette = bgPalette;
        } else {
            // Both opaque
            if (spritePriority) {
                // Sprite is behind background
                finalPixel = bgPixel;
                finalPalette = bgPalette;
            } else {
                // Sprite is in front
                finalPixel = spritePixel;
                finalPalette = spritePalette;
            }
            
            // Sprite 0 Hit Detection
            if (spriteZeroBeingRendered && (ppuMask & 0x18) == 0x18) {
                // Both BG and Sprite enabled
                // Both pixels opaque
                // Cycle != 255 (right edge clipping, usually ignored for simplicity)
                // We are at the right cycle.
                if (scanline != 261) { // Not in pre-render
                     // Check left clipping
                     if (!((ppuMask & 0x06) != 0x06 && cycle <= 8)) {
                         if (bgPixel != 0) { // Check if background is opaque
                             ppuStatus |= 0x40; // Set Sprite 0 Hit
                         }
                     }
                }
            }
        }
        
        // 4. Color Output
        int colorIndex = ppuRead(0x3F00 + (finalPalette << 2) + finalPixel) & 0x3F;
        frameBuffer[scanline * 256 + (cycle - 1)] = NES_PALETTE[colorIndex];
    }
    
    /**
     * Advance PPU by one cycle
     */
    public void clock() {
        if (scanline < 240 || scanline == 261) {
            if ((cycle >= 1 && cycle <= 256) || (cycle >= 321 && cycle <= 336)) {
                updateShifters();
                switch ((cycle - 1) % 8) {
                    case 0: loadBackgroundShifters(); fetchNametableByte(); break;
                    case 2: fetchAttributeByte(); break;
                    case 4: fetchPatternLow(); break;
                    case 6: fetchPatternHigh(); break;
                    case 7: incrementScrollX(); break;
                }
            }
            if (cycle == 256) incrementScrollY();
            if (cycle == 257) {
                loadBackgroundShifters();
                if ((ppuMask & 0x18) != 0) {
                    vramAddr = (vramAddr & 0xFBE0) | (tempVramAddr & 0x041F);
                }
                
                // Evaluate sprites for NEXT scanline
                if (scanline < 240 || scanline == 261) {
                    evaluateSprites();
                } else {
                    spriteCount = 0;
                }
            }
            
            // Fetch sprite patterns at end of scanline
            if (cycle == 320) {
                if (scanline < 240 || scanline == 261) {
                    fetchSpritePatterns();
                }
            }
            
            if (scanline == 261 && cycle >= 280 && cycle <= 304 && (ppuMask & 0x18) != 0) {
                vramAddr = (vramAddr & 0x841F) | (tempVramAddr & 0x7BE0);
            }
            
            if (scanline < 240 && cycle >= 1 && cycle <= 256) {
                renderPixel();
            }
        }
        
        if (scanline == 241 && cycle == 1) {
            ppuStatus |= 0x80;
            if (nmiOutput && bus != null) bus.nmi();
        }
        
        if (scanline == 261 && cycle == 1) {
            ppuStatus &= ~0xE0; // Clear VBlank, Sprite 0, Overflow
            for (int i = 0; i < 8; i++) {
                spriteShifterPatternLo[i] = 0;
                spriteShifterPatternHi[i] = 0;
            }
        }
        
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
    
    // Debug Getters
    public byte getPpuCtrl() { return ppuCtrl; }
    public byte getPpuMask() { return ppuMask; }
    public byte getPpuStatus() { return ppuStatus; }
    public int getOamAddr() { return oamAddr & 0xFF; }
    
    // Getters for debugging
    public int getScanline() { return scanline; }
    public int getCycle() { return cycle; }
    public long getFrame() { return frame; }
    public int[] getFrameBuffer() { return frameBuffer; }
    
    /**
     * Get color from palette RAM
     */
    private int getColorFromPalette(int palette, int pixel) {
        int paletteIndex = (palette << 2) | pixel;
        int colorIndex = paletteRam[paletteIndex] & 0x3F;
        return NES_PALETTE[colorIndex];
    }
    

    
    /**
     * Load background shifters
     */
    private void loadBackgroundShifters() {
        bgShifterPatternLo = (bgShifterPatternLo & 0xFF00) | bgNextTileLsb;
        bgShifterPatternHi = (bgShifterPatternHi & 0xFF00) | bgNextTileMsb;
        
        bgShifterAttribLo = (bgShifterAttribLo & 0xFF00) | ((bgNextTileAttrib & 0x01) != 0 ? 0xFF : 0x00);
        bgShifterAttribHi = (bgShifterAttribHi & 0xFF00) | ((bgNextTileAttrib & 0x02) != 0 ? 0xFF : 0x00);
    }
    
    /**
     * Update background shifters
     */
    private void updateShifters() {
        if ((ppuMask & 0x08) != 0) { // Show background
            bgShifterPatternLo <<= 1;
            bgShifterPatternHi <<= 1;
            bgShifterAttribLo <<= 1;
            bgShifterAttribHi <<= 1;
        }
    }
}

