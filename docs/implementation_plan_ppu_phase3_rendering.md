# NES Emulator Implementation Plan - Phase 3: Background Rendering Integration

## Goal
Integrate the background rendering pipeline into the PPU clock cycle to actually display graphics from ROMs.

## Background
Currently:
- ✅ Frame buffer exists
- ✅ Rendering methods exist
- ✅ Display window works
- ❌ Rendering not called during clock cycle

We need to fetch tiles during specific cycles and render pixels.

## User Review Required

> [!IMPORTANT]
> **Rendering Approach:**
> - Simplified scanline rendering (not cycle-accurate pixel rendering)
> - Fetch tiles every 8 cycles during visible scanlines
> - Render pixels using shift registers and fine X scroll
> - This is sufficient for most games

> [!NOTE]
> **What Will Work:**
> - Background rendering
- Scrolling (basic)
> - Palette colors
> 
> **What Won't Work Yet:**
> - Sprites (Phase 5)
> - Advanced scrolling effects
> - Mid-frame register changes

## Proposed Changes

### PPU Rendering Integration

#### [MODIFY] [Ppu.java](file:///c:/Users/lin/Desktop/my_nes2/src/main/java/com/nes/Ppu.java)
**Update `clock()` method:**

**Add tile fetching during visible scanlines:**
- Cycles 1-256: Fetch and render background
- Every 8 cycles: Fetch nametable, attribute, pattern bytes
- Every cycle: Shift registers and render pixel

**Implement tile fetching methods:**
- `fetchNametableByte()` - Get tile index from nametable
- `fetchAttributeByte()` - Get palette selection
- `fetchPatternLow()` - Get pattern table low byte
- `fetchPatternHigh()` - Get pattern table high byte

**Cycle-by-cycle logic:**
```
Cycle 0: Idle
Cycles 1-256: Render + fetch next tile
  - Shift registers
  - Render pixel
  - Fetch tile data (every 8 cycles)
Cycles 257-320: Fetch sprites (skip for now)
Cycles 321-336: Fetch first tiles of next scanline
```

## Verification Plan

### Manual Verification
- Run emulator with a real ROM
- **Expected**: See background graphics appear!
- Check that colors look correct
- Verify scrolling works (if ROM uses it)

### Test ROMs
- Try simple test ROMs (e.g., color bars, test patterns)
- Try actual games (e.g., Super Mario Bros, Donkey Kong)

## Implementation Notes

**Tile Fetching Sequence:**
```
Cycle % 8:
  1: Fetch nametable byte
  3: Fetch attribute byte  
  5: Fetch pattern low byte
  7: Fetch pattern high byte
  0: Load shifters
```

**VRAM Address Calculation:**
- Nametable: 0x2000 + (v & 0x0FFF)
- Attribute: 0x23C0 + (v >> 4 & 0x38) + (v >> 2 & 0x07)
- Pattern: (PPUCTRL.B << 12) + (tile_id << 4) + fine_y

## Next Steps
After Phase 3:
- **Phase 5**: Sprite rendering
- **Phase 6**: Controller input
- **Polish**: Performance optimization, debugging tools
