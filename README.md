# NES Emulator

A Nintendo Entertainment System (NES) emulator written in Java, featuring a fully functional MOS 6502 CPU core with cycle-accurate instruction execution.

## Features

### ✅ Implemented
- **Complete 6502 CPU Core**
  - All 12 addressing modes
  - 56+ instructions across all categories
  - Cycle-accurate timing
  - Proper status flag handling
  
- **Memory System**
  - 2KB RAM with mirroring
  - Full memory map (CPU, PPU, APU address spaces)
  - Cartridge ROM support
  
- **ROM Loading**
  - iNES format support
  - Mapper 0 (NROM) implementation
  - 16KB/32KB PRG-ROM support
  
- **Comprehensive Testing**
  - 50+ unit tests
  - Integration tests
  - Real program execution validation

### 🚧 Future Enhancements
- PPU (Picture Processing Unit) - Graphics rendering
- APU (Audio Processing Unit) - Sound synthesis
- Additional mappers (MMC1, MMC3, etc.)
- Controller input
- GUI display

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
mvn test -Dtest=IntegrationTest
```

### Running the Emulator
```bash
# Run with dummy ROM (demo program)
mvn exec:java -Dexec.mainClass="com.nes.Main"

# Run with your own ROM file
mvn exec:java -Dexec.mainClass="com.nes.Main" -Dexec.args="path/to/game.nes"
```

## Project Structure
```
my_nes2/
├── src/main/java/com/nes/
│   ├── cpu/Cpu.java           # 6502 CPU implementation
│   ├── Bus.java               # Memory bus and routing
│   ├── Cartridge.java         # ROM loader
│   ├── Ppu.java               # PPU stub
│   ├── Apu.java               # APU stub
│   └── Main.java              # Entry point
├── src/test/java/com/nes/
│   ├── cpu/                   # CPU unit tests
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

## Memory Map

| Address Range | Description |
|--------------|-------------|
| `0x0000-0x1FFF` | 2KB RAM (mirrored 4x) |
| `0x2000-0x3FFF` | PPU Registers (mirrored every 8 bytes) |
| `0x4000-0x4017` | APU Registers |
| `0x8000-0xFFFF` | Cartridge ROM |

## Example Output
```
NES Emulator Started
No ROM provided. Creating dummy cartridge.
Running...
Finished 100 cycles.
Final A: 55
RAM[0x0200]: 55
```

## Development Approach
This project was developed using **Test-Driven Development (TDD)**:
- Each feature implemented with corresponding tests
- Comprehensive test coverage (50+ tests)
- Git commits after each implementation phase
- Detailed implementation plans in `docs/` folder

## License
This is an educational project.

## Acknowledgments
- MOS 6502 CPU architecture documentation
- NES development community
- iNES ROM format specification
