package com.nes;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

public class CartridgeTest {

    @Test
    public void testLoadHeader() throws IOException {
        // Create dummy iNES file
        // Header: NES<EOF> | PRG=1 | CHR=1 | Flags6=0 | Flags7=0
        byte[] header = new byte[] {
            'N', 'E', 'S', 0x1A,
            1, 1, 0, 0, 
            0, 0, 0, 0, 0, 0, 0, 0
        };
        
        byte[] prg = new byte[16384]; // 16KB
        prg[0] = (byte) 0xA9; // LDA
        prg[16383] = (byte) 0xFF;
        
        byte[] chr = new byte[8192]; // 8KB
        
        byte[] fileData = new byte[header.length + prg.length + chr.length];
        System.arraycopy(header, 0, fileData, 0, header.length);
        System.arraycopy(prg, 0, fileData, header.length, prg.length);
        System.arraycopy(chr, 0, fileData, header.length + prg.length, chr.length);
        
        Path tempFile = Files.createTempFile("test_rom", ".nes");
        Files.write(tempFile, fileData);
        
        Cartridge cart = new Cartridge(tempFile.toString());
        
        assertEquals(1, cart.getPrgBanks());
        assertEquals(1, cart.getChrBanks());
        assertEquals(0, cart.getMapperId());
        
        // Test Read (Mapper 0 Mirroring)
        // 0x8000 -> 0x0000 in PRG
        assertEquals((byte) 0xA9, cart.cpuRead(0x8000));
        
        // 0xC000 -> 0x0000 in PRG (Mirrored)
        assertEquals((byte) 0xA9, cart.cpuRead(0xC000));
        
        Files.delete(tempFile);
    }


    @Test
    public void testMapper3() {
        // Create Cartridge with Mapper 3 (CNROM)
        // 16KB PRG, 32KB CHR (4 banks)
        byte[] prg = new byte[16384];
        byte[] chr = new byte[32768];
        
        // Fill CHR banks with distinct data
        // Bank 0: 0x00
        // Bank 1: 0x11
        // Bank 2: 0x22
        // Bank 3: 0x33
        for (int i = 0; i < 8192; i++) chr[i] = 0x00;
        for (int i = 0; i < 8192; i++) chr[8192 + i] = 0x11;
        for (int i = 0; i < 8192; i++) chr[16384 + i] = 0x22;
        for (int i = 0; i < 8192; i++) chr[24576 + i] = 0x33;
        
        Cartridge cart = new Cartridge(prg, chr, 3, 0, 1, 4);
        
        // Default Bank 0
        assertEquals(0x00, cart.ppuRead(0x0000));
        
        // Switch to Bank 1
        // Write to 0x8000 (val = 1)
        cart.cpuWrite(0x8000, (byte) 0x01);
        assertEquals(0x11, cart.ppuRead(0x0000), "Should switch to CHR Bank 1");
        
        // Switch to Bank 2
        cart.cpuWrite(0x8000, (byte) 0x02);
        assertEquals(0x22, cart.ppuRead(0x0000), "Should switch to CHR Bank 2");
        
        // Switch to Bank 3
        cart.cpuWrite(0x8000, (byte) 0x03);
        assertEquals(0x33, cart.ppuRead(0x0000), "Should switch to CHR Bank 3");
    }
}
