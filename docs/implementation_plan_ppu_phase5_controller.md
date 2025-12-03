# Phase 5: Controller Input Implementation Plan

## Goal Description
Implement NES controller support to allow user interaction with games. This involves emulating the standard NES controller (8 buttons), mapping keyboard inputs to these buttons, and handling the communication between the CPU and the controller via memory-mapped I/O (0x4016/0x4017).

## User Review Required
> [!NOTE]
> Default Key Mappings:
> - A: `X`
> - B: `Z`
> - Select: `A`
> - Start: `S`
> - Up: `Up Arrow`
> - Down: `Down Arrow`
> - Left: `Left Arrow`
> - Right: `Right Arrow`

## Proposed Changes

### Controller Logic
#### [NEW] [Controller.java](file:///c:/Users/lin/Desktop/my_nes2/src/main/java/com/nes/Controller.java)
- **State Management**: Track the state of 8 buttons (A, B, Select, Start, Up, Down, Left, Right).
- **Strobe Handling**: Implement the strobe mechanism. When CPU writes 1 then 0 to 0x4016, the controller latches the current button state.
- **Serial Read**: Implement the serial read logic. Each read from 0x4016 returns the next button state bit (1=Pressed, 0=Released).

### Memory Bus Integration
#### [MODIFY] [Bus.java](file:///c:/Users/lin/Desktop/my_nes2/src/main/java/com/nes/Bus.java)
- Add `Controller` instances for Player 1 (and optionally Player 2).
- Update `read()` to route 0x4016/0x4017 reads to the controller.
- Update `write()` to route 0x4016 writes to the controller (strobe).

### GUI Integration
#### [MODIFY] [EmulatorWindow.java](file:///c:/Users/lin/Desktop/my_nes2/src/main/java/com/nes/EmulatorWindow.java)
- Add `KeyListener` to the main window.
- Map key presses/releases to `Controller` methods (e.g., `controller.setButtonPressed(Controller.BUTTON_A, true)`).

## Verification Plan

### Automated Tests
- **`ControllerTest`**: Verify strobe behavior and serial reading logic.
    - Test latching state.
    - Test reading bits sequentially.
    - Test strobe reset.

### Manual Verification
- **Game Testing**: Load `f1.nes` or `90tank.nes`.
- **Input Check**: Verify that pressing keys moves the character or navigates menus correctly.
