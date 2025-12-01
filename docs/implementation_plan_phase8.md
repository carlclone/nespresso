# NES Emulator Implementation Plan - Phase 8: Shifts & Rotates

## Goal Description
Implement bitwise shift and rotate instructions: `ASL` (Arithmetic Shift Left), `LSR` (Logical Shift Right), `ROL` (Rotate Left), `ROR` (Rotate Right). These instructions operate on the Accumulator or Memory.

## User Review Required
- **Accumulator vs Memory**: These instructions can target the Accumulator (Implied addressing) or a Memory address. I need to handle both cases. My current `Cpu` structure passes `addr` to instructions. For Accumulator mode, I might need a special address or a separate method signature, or handle it within the instruction if `addr` implies accumulator (usually handled by addressing mode returning a specific flag, but here I might need to separate `ASL_A` vs `ASL_Mem`).
- **Design Choice**: I will implement overloaded methods or separate methods for Accumulator vs Memory to keep it clean, or check the addressing mode. Since the instruction dispatch (switch case) isn't implemented yet, I will implement `ASL(int addr)` for memory and `ASL_Acc()` for accumulator for now.

## Proposed Changes

### CPU Core
#### [MODIFY] [Cpu.java](file:///c:/Users/lin/Desktop/my_nes2/src/main/java/com/nes/cpu/Cpu.java)
- Implement `ASL`: Shift Left. Bit 7 -> Carry. 0 -> Bit 0. Set Z, N.
    - `ASL(int addr)`
    - `ASL_Acc()`
- Implement `LSR`: Shift Right. Bit 0 -> Carry. 0 -> Bit 7. Set Z, N.
    - `LSR(int addr)`
    - `LSR_Acc()`
- Implement `ROL`: Rotate Left. Carry -> Bit 0. Bit 7 -> Carry. Set Z, N.
    - `ROL(int addr)`
    - `ROL_Acc()`
- Implement `ROR`: Rotate Right. Carry -> Bit 7. Bit 0 -> Carry. Set Z, N.
    - `ROR(int addr)`
    - `ROR_Acc()`

## Verification Plan

### Automated Tests
- **ShiftRotateTest**:
    - Test Accumulator and Memory variants.
    - Verify Carry flag updates.
    - Verify Zero and Negative flags.
- **Command**: `mvn test`
