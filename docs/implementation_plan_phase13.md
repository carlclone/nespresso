# NES Emulator Implementation Plan - Phase 13: Main Loop & Integration

## Goal Description
Implement the main system clock that synchronizes the CPU, PPU, and APU. Create a main entry point that loads a ROM and executes the emulation loop.

## User Review Required
- **System Timing**:
    - PPU runs at 3x CPU speed (NTSC).
    - For this phase, I will implement a simple `Bus.clock()` that ticks the PPU 3 times and the CPU 1 time.
- **Instruction Table**: The CPU instruction table is currently partial. I will not populate the full 256 entries in this phase as it is a massive data entry task. I will ensure the structure is ready for it.

## Proposed Changes

### Bus
#### [MODIFY] [Bus.java](file:///c:/Users/lin/Desktop/my_nes2/src/main/java/com/nes/Bus.java)
- Add `clock()` method:
    - `ppu.clock()` (x3) (Stub for now)
    - `apu.clock()` (Stub for now)
    - `cpu.clock()` (Once every 3 PPU clocks? Or just tick CPU and let it handle its own cycles?)
    - Standard design: Call `cpu.clock()` every time. `cpu.clock()` checks if it needs to run. If `cycles == 0`, it fetches next instruction.
    - Wait, `cpu.clock()` in my implementation decrements cycles. So it should be called every master clock?
    - Usually: Master Clock -> PPU (dot), CPU (every 3 dots).
    - Simplified: Call `cpu.clock()` every iteration. If `cycles > 0`, it just decrements. If `cycles == 0`, it executes.
    - I will add `systemClockCounter` to track ticks.

### Main
#### [MODIFY] [Main.java](file:///c:/Users/lin/Desktop/my_nes2/src/main/java/com/nes/Main.java)
- Update `main` method:
    - Load Cartridge from args or hardcoded path.
    - Insert into Bus.
    - Reset CPU.
    - Loop forever calling `bus.clock()`.
    - Add debug print of PC and Opcode to visualize execution.

## Verification Plan

### Manual Verification
- **Run Main**:
    - Load a test ROM (or dummy data).
    - Observe console output showing PC advancing.
- **Command**: `mvn exec:java -Dexec.mainClass="com.nes.Main"`
