# Phase 6: Integration & Testing Implementation Plan

## Goal Description
Ensure the emulator is robust and accurate enough to run standard NES games. This involves verifying the timing relationship between the CPU and PPU, completing the CPU instruction set, and testing with real game ROMs.

## User Review Required
> [!NOTE]
> This phase focuses on stability and compatibility. No new major features (like APU) will be added here.

## Proposed Changes

### PPU-CPU Synchronization
#### [MODIFY] [Bus.java](file:///c:/Users/lin/Desktop/my_nes2/src/main/java/com/nes/Bus.java)
- Verify `clock()` method ensures 3 PPU ticks per 1 CPU tick.
- Verify NMI generation triggers CPU interrupt correctly.

### CPU Completeness
#### [MODIFY] [Cpu.java](file:///c:/Users/lin/Desktop/my_nes2/src/main/java/com/nes/cpu/Cpu.java)
- Audit the `lookup` table for any missing official opcodes (0x00 - 0xFF).
- Implement any missing instructions (e.g., stack operations, flag manipulations that might have been missed).
- **Key Opcodes to Check**:
    - `RTI` (Return from Interrupt) - Crucial for NMI handling.
    - `BRK` (Break) - Software interrupt.
    - `BIT` (Bit Test) - Already added, but verify.
    - `ADC`/`SBC` (Decimal mode handling - NES doesn't use decimal mode, but ensure it's disabled/ignored).

### Game Testing
- Run `f1.nes` and `90tank.nes`.
- Observe for visual glitches, crashes, or input lag.
- Use debug logging (if needed) to trace execution flow.

## Verification Plan

### Automated Tests
- **`IntegrationTest`**: Expand to run for more frames and check PPU state.
- **`CpuTest`**: Add specific tests for NMI/IRQ handling.

### Manual Verification
- **Game Play**: Play `f1.nes` for at least 1 minute.
- **Visual Check**: Ensure scrolling is smooth and sprites don't flicker incorrectly.
