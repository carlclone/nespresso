package com.nes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Represents a NES Cartridge.
 * Handles loading of iNES format ROMs.
 */
public class Cartridge {

    private byte[] prgRom;
    private byte[] chrRom;
    private int mapperId;
    private int prgBanks;
    private int chrBanks;
    private int mirrorMode; // 0 = Horizontal, 1 = Vertical

    public Cartridge(String filePath) throws IOException {
        byte[] data = Files.readAllBytes(Paths.get(filePath));
        
        // Parse Header
        // 0-3: Constant $4E $45 $53 $1A ("NES" + EOF)
        if (data[0] != 'N' || data[1] != 'E' || data[2] != 'S' || data[3] != 0x1A) {
            throw new IOException("Invalid NES ROM header");
        }

        prgBanks = data[4] & 0xFF;
        chrBanks = data[5] & 0xFF;
        
        int flags6 = data[6] & 0xFF;
        int flags7 = data[7] & 0xFF;
        
        mapperId = ((flags7 & 0xF0) | (flags6 >> 4));
        mirrorMode = (flags6 & 0x01); // Bit 0: 0 = horizontal, 1 = vertical

        // Skip trainer if present (Bit 2 of flags6)
        int offset = 16;
        if ((flags6 & 0x04) != 0) {
            offset += 512;
        }

        // Read PRG ROM (16KB units)
        int prgSize = prgBanks * 16384;
        prgRom = Arrays.copyOfRange(data, offset, offset + prgSize);
        offset += prgSize;

        // Read CHR ROM (8KB units)
        int chrSize = chrBanks * 8192;
        if (chrBanks > 0) {
            chrRom = Arrays.copyOfRange(data, offset, offset + chrSize);
        } else {
            // If 0, it uses CHR RAM (not handled yet, but initialize empty)
            chrRom = new byte[8192]; 
        }
    }

    // For testing
    public Cartridge(byte[] prgRom, byte[] chrRom, int mapperId, int mirrorMode) {
        this.prgRom = prgRom;
        this.chrRom = chrRom;
        this.mapperId = mapperId;
        this.prgBanks = prgRom.length / 16384;
        this.chrBanks = chrRom.length / 8192;
        this.mirrorMode = mirrorMode;
    }

    // Legacy test constructor (defaults to Horizontal)
    public Cartridge(byte[] prgRom, byte[] chrRom, int mapperId) {
        this(prgRom, chrRom, mapperId, 0);
    }

    public int getPrgBanks() { return prgBanks; }
    public int getChrBanks() { return chrBanks; }
    public int getMapperId() { return mapperId; }
    public int getMirrorMode() { return mirrorMode; }

    /**
     * Read from PRG ROM.
     * Handles Mapper 0 (NROM) logic.
     * @param addr Address in CPU space (0x8000 - 0xFFFF)
     * @return Byte at address
     */
    public byte cpuRead(int addr) {
        // Mapper 0
        if (prgBanks == 1) {
            // 16KB PRG: Mirror 0x8000-0xBFFF to 0xC000-0xFFFF
            // Mask with 0x3FFF (16KB - 1)
            return prgRom[addr & 0x3FFF];
        } else {
            // 32KB PRG: Direct map
            // Mask with 0x7FFF (32KB - 1)
            return prgRom[addr & 0x7FFF];
        }
    }

    /**
     * Write to PRG ROM (Mapper 0 doesn't support writing to ROM, but some mappers do registers)
     */
    public void cpuWrite(int addr, byte data) {
        // Mapper 0: No write support
    }
    
    /**
     * PPU reads from CHR-ROM/RAM
     * @param addr Address in PPU space (0x0000 - 0x1FFF)
     * @return Byte at address
     */
    public byte ppuRead(int addr) {
        addr &= 0x1FFF;
        if (addr < chrRom.length) {
            return chrRom[addr];
        }
        return 0x00;
    }
    
    /**
     * PPU writes to CHR-RAM (if CHR-ROM, this does nothing)
     * @param addr Address in PPU space (0x0000 - 0x1FFF)
     * @param data Byte to write
     */
    public void ppuWrite(int addr, byte data) {
        addr &= 0x1FFF;
        // Only write if using CHR-RAM (chrBanks == 0)
        if (chrBanks == 0 && addr < chrRom.length) {
            chrRom[addr] = data;
        }
    }
}
