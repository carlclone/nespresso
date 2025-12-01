# NES Emulator Implementation Plan - PPU Phase 1: Foundation

## Goal
Implement the PPU (Picture Processing Unit) foundation including registers, memory structures, and basic timing to prepare for graphics rendering.

## Background
The NES PPU is responsible for all graphics rendering. It runs at 3x the CPU speed and generates video output at 60 FPS (NTSC). The PPU has:
- 8 memory-mapped registers accessible by the CPU
- Internal VRAM for background/sprite data
- Pattern tables (CHR-ROM) for tile graphics
- Timing synchronized with scanlines and frames

## User Review Required

> [!IMPORTANT]
> **Design Decisions:**
> - PPU will be cycle-accurate at the scanline level (not pixel-perfect initially)
> - We'll implement NTSC timing (60 FPS, 262 scanlines)
> - Nametable mirroring will support horizontal/vertical modes
> - Initial implementation focuses on functionality over optimization

> [!WARNING]
> **Breaking Changes:**
> - `Ppu.java` will change from a stub to a full implementation (~500+ lines)
> - `Bus.java` PPU register routing will need updates
> - CPU will need to handle NMI (Non-Maskable Interrupt) on VBlank

## Proposed Changes

### PPU Core

#### [MODIFY] [Ppu.java](file:///c:/Users/lin/Desktop/my_nes2/src/main/java/com/nes/Ppu.java)
**Current:** Stub with empty read/write methods  
**New:** Full PPU implementation with:

**Registers (CPU-accessible via 0x2000-0x2007):**
- `PPUCTRL (0x2000)`: Control flags (NMI enable, sprite size, etc.)
- `PPUMASK (0x2001)`: Rendering flags (show background, sprites, etc.)
- `PPUSTATUS (0x2002)`: Status flags (VBlank, sprite 0 hit, etc.)
- `OAMADDR (0x2003)`: OAM address pointer
- `OAMDATA (0x2004)`: OAM data read/write
- `PPUSCROLL (0x2005)`: Scroll position (write x2)
- `PPUADDR (0x2006)`: VRAM address (write x2)
- `PPUDATA (0x2007)`: VRAM data read/write

**Internal Memory:**
- `vram[2048]`: Nametable memory (2KB)
- `paletteRam[32]`: Palette memory
- `oam[256]`: Object Attribute Memory (sprites)
- `patternTables`: Reference to CHR-ROM from cartridge

**Timing:**
- `scanline`: Current scanline (0-261)
- `cycle`: Current cycle within scanline (0-340)
- `frame`: Frame counter
- `nmiOccurred`: VBlank NMI flag

**Methods:**
- `cpuRead(int addr)`: Handle CPU reads from PPU registers
- `cpuWrite(int addr, byte data)`: Handle CPU writes to PPU registers
- `clock()`: Advance PPU by one cycle
- `reset()`: Initialize PPU state

#### [MODIFY] [Cartridge.java](file:///c:/Users/lin/Desktop/my_nes2/src/main/java/com/nes/Cartridge.java)
**Add PPU memory access:**
- `ppuRead(int addr)`: Read from CHR-ROM/RAM
- `ppuWrite(int addr, byte data)`: Write to CHR-RAM (if present)
- `getMirrorMode()`: Return nametable mirroring mode

#### [MODIFY] [Bus.java](file:///c:/Users/lin/Desktop/my_nes2/src/main/java/com/nes/Bus.java)
**Update PPU register handling:**
- Fix address masking for PPU registers (currently `addr & 0x2007`, should handle mirroring properly)
- Add `nmi()` method to trigger CPU NMI

#### [MODIFY] [Cpu.java](file:///c:/Users/lin/Desktop/my_nes2/src/main/java/com/nes/cpu/Cpu.java)
**Add NMI support:**
- `nmi()`: Non-Maskable Interrupt handler
- Push PC and status to stack
- Jump to NMI vector (0xFFFA/B)

### Testing

#### [NEW] [PpuTest.java](file:///c:/Users/lin/Desktop/my_nes2/src/test/java/com/nes/PpuTest.java)
**Test coverage:**
- Register read/write behavior
- VRAM address increment
- Scroll register behavior
- VBlank flag timing
- Palette mirroring

## Verification Plan

### Automated Tests
- **PpuTest**: Verify register behavior, timing, memory access
- **Command**: `mvn test -Dtest=PpuTest`

### Manual Verification
- Run emulator and verify PPU timing (should reach VBlank)
- Check that NMI fires every frame
- Verify register reads/writes work correctly

## Implementation Notes

**PPU Register Quirks:**
- PPUSTATUS read clears VBlank flag and resets address latch
- PPUSCROLL and PPUADDR use a write toggle (first/second write)
- PPUDATA reads are buffered (except palette reads)
- Palette RAM has mirroring at 0x3F10, 0x3F14, 0x3F18, 0x3F1C

**Timing:**
- 341 PPU cycles per scanline
- 262 scanlines per frame (NTSC)
- VBlank starts at scanline 241
- VBlank ends at scanline 261

## Next Steps
After Phase 1 completion:
- **Phase 2**: Background rendering pipeline
- **Phase 3**: Sprite rendering
- **Phase 4**: Display window
