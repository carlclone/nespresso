# NES Emulator - PPU & Display Implementation Tasks

## Phase 1: PPU Foundation
- [x] PPU Registers Implementation
    - [x] PPUCTRL (0x2000) - Control register
    - [x] PPUMASK (0x2001) - Mask register
    - [x] PPUSTATUS (0x2002) - Status register
    - [x] OAMADDR (0x2003) - OAM address
    - [x] OAMDATA (0x2004) - OAM data
    - [x] PPUSCROLL (0x2005) - Scroll position
    - [x] PPUADDR (0x2006) - VRAM address
    - [x] PPUDATA (0x2007) - VRAM data
- [x] PPU Memory
    - [x] Pattern Tables (CHR-ROM access)
    - [x] Nametables (2KB VRAM)
    - [x] Palette RAM (32 bytes)
    - [x] OAM (256 bytes for sprites)
- [x] PPU Timing
    - [x] Scanline counter (262 scanlines)
    - [x] Cycle counter (341 cycles per scanline)
    - [x] Frame timing
    - [x] VBlank generation

## Phase 2: Background Rendering
- [ ] Tile Fetching
    - [ ] Nametable byte fetch
    - [ ] Attribute byte fetch
    - [ ] Pattern table tile fetch (low/high)
- [ ] Background Rendering Pipeline
    - [ ] Shift registers for tile data
    - [ ] Palette selection
    - [ ] Pixel rendering
- [ ] Scrolling
    - [ ] Horizontal scroll
    - [ ] Vertical scroll
    - [ ] Nametable mirroring

## Phase 3: Sprite Rendering
- [ ] Sprite Evaluation
    - [ ] Find sprites on current scanline
    - [ ] Sprite 0 hit detection
- [ ] Sprite Rendering
    - [ ] 8x8 sprite rendering
    - [ ] 8x16 sprite rendering (optional)
    - [ ] Sprite priority
    - [ ] Sprite flipping (H/V)

## Phase 4: Display Window
- [ ] GUI Framework Setup
    - [ ] Choose framework (Swing/JavaFX)
    - [ ] Create window (256x240)
    - [ ] Frame buffer implementation
- [ ] Rendering Loop
    - [ ] 60 FPS timing
    - [ ] Frame buffer to screen
    - [ ] Double buffering

## Phase 5: Controller Input
- [ ] Controller Registers
    - [ ] 0x4016 (Controller 1)
    - [ ] 0x4017 (Controller 2)
- [ ] Input Mapping
    - [ ] Keyboard to NES buttons
    - [ ] Button state tracking
- [ ] Input Polling
    - [ ] Strobe mechanism
    - [ ] Serial read

## Phase 6: Integration & Testing
- [ ] PPU-CPU Synchronization
    - [ ] Proper timing (3 PPU cycles per CPU cycle)
    - [ ] NMI on VBlank
- [ ] Complete Opcode Table
    - [ ] Add remaining official opcodes
    - [ ] Test with real games
- [ ] Game Testing
    - [ ] Test with simple games (e.g., Donkey Kong)
    - [ ] Debug rendering issues
    - [ ] Performance optimization

## Phase 7: Polish & Features
- [ ] APU (Audio) - Optional
    - [ ] Pulse channels
    - [ ] Triangle channel
    - [ ] Noise channel
- [ ] Additional Features
    - [ ] Save states
    - [ ] Debugging tools
    - [ ] Game Genie codes
    - [ ] Screenshot capability
