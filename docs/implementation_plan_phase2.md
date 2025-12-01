# NES Emulator Implementation Plan - Phase 2: Addressing Modes & Basic Instructions

## Goal Description
Implement the 6502 CPU addressing modes and the basic Load/Store instructions (LDA, LDX, LDY, STA, STX, STY). This will allow the CPU to read and write data to memory using various addressing schemes.

## User Review Required
- None.

## Proposed Changes

### CPU Core
#### [MODIFY] [Cpu.java](file:///c:/Users/lin/Desktop/my_nes2/src/main/java/com/nes/cpu/Cpu.java)
- Implement `fetch()` method to get data from memory.
- Implement Addressing Mode methods:
    - `IMP()`: Implicit
    - `IMM()`: Immediate
    - `ZP0()`: Zero Page
    - `ZPX()`: Zero Page, X
    - `ZPY()`: Zero Page, Y
    - `REL()`: Relative
    - `ABS()`: Absolute
    - `ABX()`: Absolute, X
    - `ABY()`: Absolute, Y
    - `IND()`: Indirect
    - `IZX()`: Indirect, X
    - `IZY()`: Indirect, Y
- Implement Instruction methods:
    - `LDA()`: Load Accumulator
    - `LDX()`: Load X Register
    - `LDY()`: Load Y Register
    - `STA()`: Store Accumulator
    - `STX()`: Store X Register
    - `STY()`: Store Y Register
- Add a lookup table (array of instruction objects) to map opcodes to functions.

## Verification Plan

### Automated Tests
- **Addressing Mode Tests**: Verify that each addressing mode calculates the correct address.
- **Instruction Tests**: Verify that LDA, LDX, etc., load correct values and set flags (Z, N).
- **Command**: `mvn test`
