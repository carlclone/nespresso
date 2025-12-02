# NES Emulator

A Nintendo Entertainment System (NES) emulator written in Java, featuring a fully functional MOS 6502 CPU core with cycle-accurate instruction execution and **working graphics rendering**!

## Features

### ✅ Implemented
- **Complete 6502 CPU Core**
  - All 12 addressing modes
  - 56+ instructions across all categories
  - Cycle-accurate timing
  - Proper status flag handling
  
- **PPU (Picture Processing Unit)**
  -  All 8 PPU registers (PPUCTRL, PPUMASK, PPUSTATUS, etc.)
  -  Background rendering with tile fetching
  -  **Sprite rendering (8x8, flipping, priority)**
  -  **Sprite 0 Hit detection**
  -  NES 64-color palette
  -  Scrolling support
  -  VBlank and NMI generation
  -  Frame buffer (256x240 pixels)
  
- **Display Window**
  -  Java Swing GUI (768x720, 3x scale)
  -  60 FPS rendering
  -  Real-time graphics display
  
- **Memory System**
  - 2KB RAM with mirroring
  - Full memory map (CPU, PPU, APU address spaces)
  - Cartridge ROM support
  
- **ROM Loading**
  - iNES format support
  - Mapper 0 (NROM) implementation
  - 16KB/32KB PRG-ROM support
  
- **Comprehensive Testing**
  - 60+ unit tests
  - Integration tests
  - Real program execution validation

### 🚧 Future Enhancements
- APU (Audio Processing Unit) - Sound synthesis
- Additional mappers (MMC1, MMC3, etc.)
- Controller input
- Save states

## Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.6+

### Building
```bash
mvn clean compile
```

### Running Tests
```bash
# Run all tests
mvn test

# Run specific test suite
mvn test -Dtest=PpuTest
```

### Running the Emulator
```bash
# Run with dummy ROM (test pattern)
mvn exec:java -Dexec.mainClass="com.nes.Main"

# Run with your own ROM file
mvn exec:java -Dexec.mainClass="com.nes.Main" -Dexec.args="path/to/game.nes"
```

**Example:**
```bash
mvn exec:java -Dexec.mainClass="com.nes.Main" -Dexec.args="C:\Users\lin\Downloads\90tank.nes"
```

## What You'll See

When you run the emulator:
- A 768x720 window will open
- **Background and Sprite graphics will render!** 🎮
- NES games will display their backgrounds and characters with correct colors
- The emulator runs at 60 FPS

## Project Structure
```
my_nes2/
├── src/main/java/com/nes/
│   ├── cpu/Cpu.java           # 6502 CPU implementation (~750 lines)
│   ├── Ppu.java               # PPU with rendering (~500 lines)
│   ├── Bus.java               # Memory bus and routing
│   ├── Cartridge.java         # ROM loader
│   ├── EmulatorWindow.java    # GUI display
│   ├── Apu.java               # APU stub
│   └── Main.java              # Entry point
├── src/test/java/com/nes/
│   ├── cpu/                   # CPU unit tests (10 suites)
│   ├── PpuTest.java          # PPU tests
│   ├── IntegrationTest.java   # Integration tests
│   ├── CartridgeTest.java
│   └── MemoryMapTest.java
├── docs/                      # Implementation plans
└── pom.xml
```

## CPU Instructions Implemented

### Load/Store
`LDA`, `LDX`, `LDY`, `STA`, `STX`, `STY`

### Arithmetic
`ADC`, `SBC`

### Logical
`AND`, `ORA`, `EOR`, `BIT`

### Branches
`BCC`, `BCS`, `BEQ`, `BNE`, `BMI`, `BPL`, `BVC`, `BVS`

### Jumps
`JMP`, `JSR`, `RTS`

### Stack
`PHA`, `PHP`, `PLA`, `PLP`, `TSX`, `TXS`

### Transfers
`TAX`, `TAY`, `TXA`, `TYA`

### Inc/Dec
`INC`, `DEC`, `INX`, `DEX`, `INY`, `DEY`

### Shifts/Rotates
`ASL`, `LSR`, `ROL`, `ROR`

### System
`BRK`, `NOP`, `RTI`, `CLC`, `SEC`, `CLI`, `SEI`, `CLV`, `CLD`, `SED`

### Compares
`CMP`, `CPX`, `CPY`

## Memory Map

| Address Range | Description |
|--------------|-------------|
| `0x0000-0x1FFF` | 2KB RAM (mirrored 4x) |
| `0x2000-0x3FFF` | PPU Registers (mirrored every 8 bytes) |
| `0x4000-0x4017` | APU Registers |
| `0x8000-0xFFFF` | Cartridge ROM |

## Current Capabilities

✅ **Fully Working:**
- CPU executes 6502 machine code
- PPU renders background tiles and **sprites** from ROMs
- Display window shows graphics at 60 FPS
- ROM loading (iNES format, Mapper 0)
- Correct NES color palette
- Memory mapping with mirroring

⏳ **Not Yet Implemented:**
- Sound/Audio (APU)
- Controller input
- Additional mappers

## Development Approach
This project was developed using **Test-Driven Development (TDD)**:
- Each feature implemented with corresponding tests
- Comprehensive test coverage (60+ tests)
- Git commits after each implementation phase
- Detailed implementation plans in `docs/` folder

## Performance
- Runs at 60 FPS
- Cycle-accurate CPU timing
- Real-time rendering

## License
This is an educational project.

## Acknowledgments
- MOS 6502 CPU architecture documentation
- NES development community
- NES PPU documentation
- iNES ROM format specification

