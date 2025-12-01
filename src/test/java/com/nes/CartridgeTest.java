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
}
