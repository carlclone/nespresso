package com.nes.cpu;

import com.nes.Bus;

/**
 * Emulates the MOS 6502 CPU.
 */
public class Cpu {

    // Registers
    public byte a = 0x00;      // Accumulator
    public byte x = 0x00;      // X Register
    public byte y = 0x00;      // Y Register
    public byte sp = (byte) 0xFD; // Stack Pointer
    public int pc = 0x0000;    // Program Counter
    public byte status = 0x00; // Status Register

    // Status Flags
    public static final byte C = (byte) (1 << 0); // Carry Bit
    public static final byte Z = (byte) (1 << 1); // Zero
    public static final byte I = (byte) (1 << 2); // Disable Interrupts
    public static final byte D = (byte) (1 << 3); // Decimal Mode (unused in NES)
    public static final byte B = (byte) (1 << 4); // Break
    public static final byte U = (byte) (1 << 5); // Unused
    public static final byte V = (byte) (1 << 6); // Overflow
    public static final byte N = (byte) (1 << 7); // Negative

    private Bus bus;

    public void connectBus(Bus bus) {
        this.bus = bus;
    }

    /**
     * Reset the CPU to its initial state.
     */
    public void reset() {
        a = 0;
        x = 0;
        y = 0;
        sp = (byte) 0xFD;
        status = (byte) (0x00 | U); // Unused bit is always 1

        // Read reset vector
        int lo = bus.read(0xFFFC) & 0xFF;
        int hi = bus.read(0xFFFD) & 0xFF;
        pc = (hi << 8) | lo;

        // Reset takes time
        // cycles = 8;
    }

    /**
     * Fetches the next byte from memory at the current PC and increments PC.
     * @return The fetched byte.
     */
    public byte fetch() {
        if (!(bus == null)) {
            return bus.read(pc++);
        }
        return 0x00;
    }

    // --- Addressing Modes ---

    // Implicit
    public int IMP() {
        return 0;
    }

    // Immediate
    public int IMM() {
        return pc++;
    }

    // Zero Page
    public int ZP0() {
        return fetch() & 0xFF;
    }

    // Zero Page, X
    public int ZPX() {
        return (fetch() + x) & 0xFF;
    }

    // Zero Page, Y
    public int ZPY() {
        return (fetch() + y) & 0xFF;
    }

    // Relative
    public int REL() {
        int rel = fetch();
        if ((rel & 0x80) != 0) {
            rel |= 0xFFFFFF00; // Sign extend
        }
        return rel;
    }

    // Absolute
    public int ABS() {
        int lo = fetch() & 0xFF;
        int hi = fetch() & 0xFF;
        return (hi << 8) | lo;
    }

    // Absolute, X
    public int ABX() {
        int lo = fetch() & 0xFF;
        int hi = fetch() & 0xFF;
        int addr = (hi << 8) | lo;
        addr += (x & 0xFF);
        return addr & 0xFFFF;
    }

    // Absolute, Y
    public int ABY() {
        int lo = fetch() & 0xFF;
        int hi = fetch() & 0xFF;
        int addr = (hi << 8) | lo;
        addr += (y & 0xFF);
        return addr & 0xFFFF;
    }

    // Indirect
    public int IND() {
        int ptrLo = fetch() & 0xFF;
        int ptrHi = fetch() & 0xFF;
        int ptr = (ptrHi << 8) | ptrLo;

        // Simulate page boundary bug
        if (ptrLo == 0xFF) {
            return (bus.read(ptr & 0xFF00) << 8) | (bus.read(ptr) & 0xFF);
        } else {
            return (bus.read(ptr + 1) << 8) | (bus.read(ptr) & 0xFF);
        }
    }

    // Indirect, X
    public int IZX() {
        int t = (fetch() + x) & 0xFF;
        int lo = bus.read(t & 0xFF) & 0xFF;
        int hi = bus.read((t + 1) & 0xFF) & 0xFF;
        return (hi << 8) | lo;
    }

    // Indirect, Y
    public int IZY() {
        int t = fetch() & 0xFF;
        int lo = bus.read(t & 0xFF) & 0xFF;
        int hi = bus.read((t + 1) & 0xFF) & 0xFF;
        int addr = (hi << 8) | lo;
        addr += (y & 0xFF);
        return addr & 0xFFFF;
    }

    // Helper to set flags based on result
    private void setFlag(byte flag, boolean v) {
        if (v) {
            status |= flag;
        } else {
            status &= ~flag;
        }
    }

    public byte getFlag(byte flag) {
        return (byte) ((status & flag) != 0 ? 1 : 0);
    }

    private void setZN(byte result) {
        setFlag(Z, result == 0);
        setFlag(N, (result & 0x80) != 0);
    }

    // --- Instructions ---

    // Load Accumulator
    public void LDA(int addr) {
        a = bus.read(addr);
        setZN(a);
    }

    // Load X Register
    public void LDX(int addr) {
        x = bus.read(addr);
        setZN(x);
    }

    // Load Y Register
    public void LDY(int addr) {
        y = bus.read(addr);
        setZN(y);
    }

    // Store Accumulator
    public void STA(int addr) {
        bus.write(addr, a);
    }

    // Store X Register
    public void STX(int addr) {
        bus.write(addr, x);
    }

    // Store Y Register
    public void STY(int addr) {
        bus.write(addr, y);
    }
}
