# NES Emulator Implementation Plan - Phase 11: ROM Loading

## Goal Description
Implement the ability to load NES ROM files in the iNES format (.nes). This involves parsing the 16-byte header to extract information about the PRG-ROM (Program) and CHR-ROM (Character/Graphics) banks, as well as mapper and mirroring configuration.

## User Review Required
- **Mapper Support**: Initially, I will only support **Mapper 0 (NROM)**. This is the simplest mapper with 16KB or 32KB PRG-ROM mapped to 0x8000-0xFFFF.
- **File I/O**: I will use standard Java NIO for file reading.

## Proposed Changes

### ROM Handling
#### [NEW] [Cartridge.java](file:///c:/Users/lin/Desktop/my_nes2/src/main/java/com/nes/Cartridge.java)
- Class to represent a loaded game cartridge.
- Fields:
    - `prgRom`: byte array (Program data)
    - `chrRom`: byte array (Graphics data)
    - `mapperId`: int
    - `prgBanks`: int (Number of 16KB units)
    - `chrBanks`: int (Number of 8KB units)
- Method `load(String filePath)` or constructor:
    - Read header.
    - Validate signature ("NES" + 0x1A).
    - Read PRG ROM.
    - Read CHR ROM.

### Bus Integration
#### [MODIFY] [Bus.java](file:///c:/Users/lin/Desktop/my_nes2/src/main/java/com/nes/Bus.java)
- Add `insertCartridge(Cartridge cart)` method.
- Update `read/write` to route 0x8000-0xFFFF to the Cartridge.
    - For Mapper 0:
        - If PRG is 16KB (1 bank), mirror 0x8000-0xBFFF to 0xC000-0xFFFF.
        - If PRG is 32KB (2 banks), map directly.

## Verification Plan

### Automated Tests
- **CartridgeTest**:
    - Create a dummy .nes file (header + data).
    - Load it and verify header parsing (banks, mapper ID).
    - Verify PRG ROM data is correctly loaded.
- **BusTest**:
    - Insert cartridge.
    - Read from 0x8000 and verify it returns data from the cartridge.
- **Command**: `mvn test`
