# NES Emulator Implementation Plan - Phase 5: Branch & Jump Instructions

## Goal Description
Implement flow control instructions. Branch instructions change the Program Counter (PC) based on Status Flags using a relative offset. Jump instructions change the PC to a specific address. Subroutine instructions (`JSR`, `RTS`) manage the stack for function calls.

## User Review Required
- **Branch Logic**: Branches use signed 8-bit relative offsets.
- **Page Boundary**: Branching to a different page costs an extra CPU cycle (will be implemented in cycle counting later, but logic should be aware).

## Proposed Changes

### CPU Core
#### [MODIFY] [Cpu.java](file:///c:/Users/lin/Desktop/my_nes2/src/main/java/com/nes/cpu/Cpu.java)
- Implement `branch()` helper:
    - Adds relative offset to PC.
- Implement Branch Instructions:
    - `BCC` (Branch if Carry Clear)
    - `BCS` (Branch if Carry Set)
    - `BEQ` (Branch if Equal / Zero Set)
    - `BNE` (Branch if Not Equal / Zero Clear)
    - `BMI` (Branch if Minus / Negative Set)
    - `BPL` (Branch if Positive / Negative Clear)
    - `BVC` (Branch if Overflow Clear)
    - `BVS` (Branch if Overflow Set)
- Implement Jump Instructions:
    - `JMP` (Jump Absolute / Indirect) - Already partially handled by addressing modes, need instruction logic to set PC.
    - `JSR` (Jump to Subroutine) - Push PC to stack, set PC.
    - `RTS` (Return from Subroutine) - Pop PC from stack, increment PC.
- Implement Stack Helpers (if not already present):
    - `push(byte data)`
    - `pop()`
    - `pushWord(int data)`
    - `popWord()`

## Verification Plan

### Automated Tests
- **BranchTest**:
    - Test each branch condition (taken vs not taken).
    - Test forward and backward branching (signed offset).
- **JumpTest**:
    - Test `JMP` (Absolute and Indirect).
    - Test `JSR` and `RTS` sequence (verify Stack Pointer and PC).
- **Command**: `mvn test`
