# NES Emulator Implementation Plan - Phase 10: Cycle Counting & Dispatch

## Goal Description
Implement the main execution loop for the CPU. This involves creating an Opcode Lookup Table that maps byte values to specific Instructions and Addressing Modes, along with their cycle counts. The `clock()` method will drive the CPU, fetching opcodes and executing them while tracking remaining cycles.

## User Review Required
- **Opcode Table**: I will implement a lookup table for all 256 opcodes. Unimplemented opcodes will be treated as `XXX` (Illegal/Unofficial) and effectively act as NOPs for now.
- **Cycle Accuracy**: The emulator will be cycle-accurate at the instruction level (not sub-instruction/bus level yet, which is acceptable for a basic NES emulator).

## Proposed Changes

### CPU Core
#### [MODIFY] [Cpu.java](file:///c:/Users/lin/Desktop/my_nes2/src/main/java/com/nes/cpu/Cpu.java)
- Add `cycles` (int) to track remaining cycles for the current instruction.
- Define functional interfaces for Instruction and Addressing Mode logic.
- Create an inner class `InstructionEntry` to hold:
    - Opcode name (String)
    - Operation (Functional Interface)
    - Addressing Mode (Functional Interface)
    - Base Cycles (int)
- Create `lookup` array of 256 `InstructionEntry` objects.
- Implement `clock()` method:
    - If `cycles == 0`:
        - Read opcode from PC.
        - Get `InstructionEntry` from lookup.
        - Set `cycles` = base cycles.
        - Execute Addressing Mode (may add extra cycles, e.g., page crossing).
        - Execute Operation (may add extra cycles, e.g., branch taken).
    - `cycles--`.
- Populate the `lookup` table with all implemented instructions (LDA, STA, ADC, etc.) and their modes.

## Verification Plan

### Automated Tests
- **CycleTest**:
    - Verify that instructions consume the correct number of cycles.
    - Verify that page crossing adds cycles (e.g., `LDA ABS,X`).
    - Verify branch taken/not-taken cycles.
- **Command**: `mvn test`
