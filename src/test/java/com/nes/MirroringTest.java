package com.nes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MirroringTest {

    private Ppu ppu;
    private Cartridge cartridge;

    @BeforeEach
    public void setUp() {
        ppu = new Ppu();
        // Create a dummy cartridge with 16KB PRG, 8KB CHR, Mapper 0
        // We will override mirror mode via reflection or a mock if needed, 
        // but Cartridge is final. We can create a cartridge with specific header.
    }

    // Helper to create a cartridge with specific mirror mode
    private Cartridge createCartridge(int mirrorMode) {
        // Header: NES<EOF> | PRG | CHR | Flags6 | Flags7
        // Flags6: Bit 0 = Mirroring (0=H, 1=V)
        byte[] header = new byte[16];
        header[0] = 'N';
        header[1] = 'E';
        header[2] = 'S';
        header[3] = 0x1A;
        header[4] = 1; // 1x16KB PRG
        header[5] = 1; // 1x8KB CHR
        header[6] = (byte) (mirrorMode & 0x01); // Mirroring

        byte[] prg = new byte[16384];
        byte[] chr = new byte[8192];

        // Combine
        byte[] data = new byte[16 + 16384 + 8192];
        System.arraycopy(header, 0, data, 0, 16);
        System.arraycopy(prg, 0, data, 16, 16384);
        System.arraycopy(chr, 0, data, 16 + 16384, 8192);

        try {
            // We need a way to load from byte array or mock it.
            // Cartridge constructor takes a file path.
            // Let's use the testing constructor I saw earlier!
            // public Cartridge(byte[] prgRom, byte[] chrRom, int mapperId)
            // But that one sets mirrorMode = 0 by default.
            // I might need to modify Cartridge to allow setting mirror mode in test constructor.
            return new Cartridge(prg, chr, 0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testHorizontalMirroring() {
        // Mode 0 = Horizontal
        cartridge = new Cartridge(new byte[16384], new byte[8192], 0, 0);
        ppu.connectCartridge(cartridge);

        // NT0 ($2000) and NT1 ($2400) should map to VRAM A
        ppu.cpuWrite(0x2000, (byte) 0xAA);
        assertEquals((byte) 0xAA, ppu.cpuRead(0x2000), "Read back from NT0");
        assertEquals((byte) 0xAA, ppu.cpuRead(0x2400), "NT1 should mirror NT0");

        // NT2 ($2800) and NT3 ($2C00) should map to VRAM B
        ppu.cpuWrite(0x2800, (byte) 0xBB);
        assertEquals((byte) 0xBB, ppu.cpuRead(0x2800), "Read back from NT2");
        assertEquals((byte) 0xBB, ppu.cpuRead(0x2C00), "NT3 should mirror NT2");

        // Verify they are distinct
        assertEquals((byte) 0xAA, ppu.cpuRead(0x2000), "NT0 should be unchanged");
        assertNotEquals(ppu.cpuRead(0x2000), ppu.cpuRead(0x2800), "NT0 and NT2 should be distinct");
    }

    @Test
    public void testVerticalMirroring() {
        // Mode 1 = Vertical
        cartridge = new Cartridge(new byte[16384], new byte[8192], 0, 1);
        ppu.connectCartridge(cartridge);

        // NT0 ($2000) and NT2 ($2800) should map to VRAM A
        ppu.cpuWrite(0x2000, (byte) 0xCC);
        assertEquals((byte) 0xCC, ppu.cpuRead(0x2000), "Read back from NT0");
        assertEquals((byte) 0xCC, ppu.cpuRead(0x2800), "NT2 should mirror NT0");

        // NT1 ($2400) and NT3 ($2C00) should map to VRAM B
        ppu.cpuWrite(0x2400, (byte) 0xDD);
        assertEquals((byte) 0xDD, ppu.cpuRead(0x2400), "Read back from NT1");
        assertEquals((byte) 0xDD, ppu.cpuRead(0x2C00), "NT3 should mirror NT1");

        // Verify they are distinct
        assertEquals((byte) 0xCC, ppu.cpuRead(0x2000), "NT0 should be unchanged");
        assertNotEquals(ppu.cpuRead(0x2000), ppu.cpuRead(0x2400), "NT0 and NT1 should be distinct");
    }
}