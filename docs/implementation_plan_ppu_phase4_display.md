# NES Emulator Implementation Plan - Phase 4: Display Window

## Goal
Create a GUI window to display the PPU's frame buffer output, enabling visual feedback from the emulator.

## Background
The PPU generates a 256x240 pixel frame buffer in RGB format. We need a window to:
- Display the frame buffer at 60 FPS
- Scale the display (NES resolution is small on modern screens)
- Handle window events (close, resize)
- Eventually handle keyboard input for controllers

## User Review Required

> [!IMPORTANT]
> **Technology Choice:**
> - **Java Swing** - Built-in, simple, good for desktop apps
> - Pros: No dependencies, easy to use, cross-platform
> - Cons: Older UI framework
> - Alternative: JavaFX (more modern but requires separate dependency)

> [!NOTE]
> **Design Decisions:**
> - Window size: 768x720 (3x scale of 256x240)
> - Frame rate: Target 60 FPS (16.67ms per frame)
> - Rendering: Use BufferedImage for frame buffer conversion
> - Input: Will be added in Phase 5

## Proposed Changes

### Display Window

#### [NEW] [EmulatorWindow.java](file:///c:/Users/lin/Desktop/my_nes2/src/main/java/com/nes/EmulatorWindow.java)
**Create GUI window class:**

**Components:**
- `JFrame` - Main window
- `JPanel` - Custom panel for rendering
- `BufferedImage` - Convert int[] frame buffer to displayable image
- `Timer` - 60 FPS refresh timer

**Methods:**
- `EmulatorWindow(Bus bus)` - Constructor
- `updateFrame()` - Convert frame buffer to BufferedImage
- `paintComponent()` - Draw to screen
- `start()` - Start rendering loop
- `stop()` - Stop rendering

#### [MODIFY] [Main.java](file:///c:/Users/lin/Desktop/my_nes2/src/main/java/com/nes/Main.java)
**Update to use GUI:**
- Create `EmulatorWindow`
- Remove console-only output
- Run emulation loop in separate thread
- Update frame buffer continuously

### Testing

#### [NEW] [EmulatorWindowTest.java](file:///c:/Users/lin/Desktop/my_nes2/src/test/java/com/nes/EmulatorWindowTest.java)
**Basic GUI tests:**
- Window creation
- Frame buffer update
- (Manual testing for visual verification)

## Verification Plan

### Manual Verification
- **Run emulator**: `mvn exec:java -Dexec.mainClass="com.nes.Main"`
- Verify window opens (768x720)
- Verify window title shows "NES Emulator"
- Window should show black screen initially (no rendering yet)
- Window should close properly

### Future Testing
- Load a ROM with graphics
- Verify frame buffer displays correctly
- Check 60 FPS performance

## Implementation Notes

**Swing Rendering:**
```java
BufferedImage image = new BufferedImage(256, 240, BufferedImage.TYPE_INT_RGB);
image.setRGB(0, 0, 256, 240, frameBuffer, 0, 256);
g.drawImage(image, 0, 0, 768, 720, null); // 3x scale
```

**60 FPS Timer:**
```java
Timer timer = new Timer(16, e -> repaint()); // ~60 FPS
```

**Thread Safety:**
- Frame buffer updates happen on emulation thread
- Rendering happens on Swing EDT
- May need synchronization

## Next Steps
After Phase 4:
- **Phase 3**: Complete background rendering (integrate into clock)
- **Phase 5**: Controller input
- **Phase 6**: Sprite rendering
