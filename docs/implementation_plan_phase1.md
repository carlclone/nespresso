# NES Emulator Implementation Plan - Phase 1: Setup & CPU Core

## Goal Description
Initialize the Java project with Maven, set up the directory structure, and implement the core 6502 CPU architecture (Registers, Bus Interface) along with basic memory access.

## User Review Required
- **Project Structure**: Standard Maven layout (`src/main/java`, `src/test/java`).
- **Testing Framework**: JUnit 5.
- **Build Tool**: Maven.

## Proposed Changes

### Project Configuration
#### [NEW] [pom.xml](file:///c:/Users/lin/Desktop/my_nes2/pom.xml)
- Define project coordinates (groupId: `com.nes`, artifactId: `nes-emulator`).
- Add dependencies:
    - `junit-jupiter` (Test scope)
- Set Java version to 17 or 21.

### Source Code Structure
#### [NEW] [Cpu.java](file:///c:/Users/lin/Desktop/my_nes2/src/main/java/com/nes/cpu/Cpu.java)
- Implement `Cpu` class.
- Define registers: `A` (Accumulator), `X`, `Y`, `PC` (Program Counter), `SP` (Stack Pointer), `P` (Status).
- Define Status Flags (Carry, Zero, Interrupt, Decimal, Break, Overflow, Negative).
- Implement `reset()` method.
- Implement `connectBus(Bus bus)` method.

#### [NEW] [Bus.java](file:///c:/Users/lin/Desktop/my_nes2/src/main/java/com/nes/Bus.java)
- Define `Bus` class (or interface) to handle read/write operations.
- `read(int addr)`
- `write(int addr, byte data)`
- Contains 64KB RAM (for now, acting as the whole system memory).

#### [NEW] [Main.java](file:///c:/Users/lin/Desktop/my_nes2/src/main/java/com/nes/Main.java)
- Simple entry point to verify setup.

## Verification Plan

### Automated Tests
- **Run all tests**: `mvn test`
- **Specific Test**: `mvn -Dtest=CpuTest test`

### Manual Verification
- **Build Project**: `mvn clean install`
- **Run Main**: `mvn exec:java -Dexec.mainClass="com.nes.Main"`
