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

    // Add with Carry
    public void ADC(int addr) {
        byte fetched = bus.read(addr);
        int val = fetched & 0xFF;
        int aVal = a & 0xFF;
        int cVal = getFlag(C);
        
        int sum = aVal + val + cVal;
        
        setFlag(C, sum > 255);
        setFlag(Z, (sum & 0xFF) == 0);
        setFlag(N, (sum & 0x80) != 0);
        
        // Overflow: ~(A ^ M) & (A ^ R) & 0x80
        boolean overflow = (~(aVal ^ val) & (aVal ^ sum) & 0x80) != 0;
        setFlag(V, overflow);
        
        a = (byte) sum;
    }

    // Subtract with Carry
    public void SBC(int addr) {
        byte fetched = bus.read(addr);
        int val = (fetched ^ 0xFF) & 0xFF; // Invert bits
        int aVal = a & 0xFF;
        int cVal = getFlag(C);
        
        int sum = aVal + val + cVal;
        
        setFlag(C, sum > 255);
        setFlag(Z, (sum & 0xFF) == 0);
        setFlag(N, (sum & 0x80) != 0);
        
        // Overflow logic is same as ADC but with inverted M
        boolean overflow = (~(aVal ^ val) & (aVal ^ sum) & 0x80) != 0;
        setFlag(V, overflow);
        
        a = (byte) sum;
    }

    // --- Logical Instructions ---

    // Bitwise AND
    public void AND(int addr) {
        byte fetched = bus.read(addr);
        a &= fetched;
        setZN(a);
    }

    // Bitwise OR
    public void ORA(int addr) {
        byte fetched = bus.read(addr);
        a |= fetched;
        setZN(a);
    }

    // Exclusive OR
    public void EOR(int addr) {
        byte fetched = bus.read(addr);
        a ^= fetched;
        setZN(a);
    }

    // Bit Test
    public void BIT(int addr) {
        byte fetched = bus.read(addr);
        int val = fetched & 0xFF;
        int aVal = a & 0xFF;
        
        // Z flag set if (A & M) == 0
        setFlag(Z, (aVal & val) == 0);
        
        // N flag = bit 7 of M
        setFlag(N, (val & 0x80) != 0);
        
        // V flag = bit 6 of M
        setFlag(V, (val & 0x40) != 0);
    }

    // --- Stack Operations ---

    public void push(byte data) {
        bus.write(0x0100 + (sp & 0xFF), data);
        sp--;
    }

    public byte pop() {
        sp++;
        return bus.read(0x0100 + (sp & 0xFF));
    }

    public void pushWord(int data) {
        push((byte) ((data >> 8) & 0xFF));
        push((byte) (data & 0xFF));
    }

    public int popWord() {
        int lo = pop() & 0xFF;
        int hi = pop() & 0xFF;
        return (hi << 8) | lo;
    }

    // --- Branch Instructions ---

    private void branch(int addr) {
        // addr is the relative offset (signed byte)
        // In this emulator structure, the addressing mode (REL) already returns the offset
        // But wait, REL returns the *offset value*, not the target address?
        // Let's check REL implementation.
        // REL reads a byte. If 0x80 is set, it sign extends. It returns the signed int offset.
        // So 'addr' here is actually the offset.
        
        int offset = addr;
        pc += offset;
    }

    public void BCC(int offset) {
        if (getFlag(C) == 0) branch(offset);
    }

    public void BCS(int offset) {
        if (getFlag(C) == 1) branch(offset);
    }

    public void BEQ(int offset) {
        if (getFlag(Z) == 1) branch(offset);
    }

    public void BNE(int offset) {
        if (getFlag(Z) == 0) branch(offset);
    }

    public void BMI(int offset) {
        if (getFlag(N) == 1) branch(offset);
    }

    public void BPL(int offset) {
        if (getFlag(N) == 0) branch(offset);
    }

    public void BVC(int offset) {
        if (getFlag(V) == 0) branch(offset);
    }

    public void BVS(int offset) {
        if (getFlag(V) == 1) branch(offset);
    }

    // --- Jump Instructions ---

    public void JMP(int addr) {
        pc = addr;
    }

    public void JSR(int addr) {
        // Push PC - 1 to stack
        // PC currently points to next instruction (after JSR opcode and address)
        // But JSR pushes the address of the last byte of the JSR instruction (PC + 2 - 1 = PC + 1)
        // Wait, my fetch() increments PC.
        // JSR is 3 bytes: Opcode, Lo, Hi.
        // When JSR is executed, Opcode is fetched (PC+1), Lo fetched (PC+2), Hi fetched (PC+3).
        // PC is now at next instruction.
        // We need to push PC - 1.
        
        pushWord(pc - 1);
        pc = addr;
    }

    public void RTS(int addr) { // addr is unused for RTS (Implied)
        pc = popWord();
        pc++;
    }

    // --- Stack Instructions ---

    // Push Accumulator
    public void PHA(int addr) {
        push(a);
    }

    // Push Processor Status
    public void PHP(int addr) {
        // Push status with Break (B) and Unused (U) bits set
        push((byte) (status | B | U));
    }

    // Pull Accumulator
    public void PLA(int addr) {
        a = pop();
        setZN(a);
    }

    // Pull Processor Status
    public void PLP(int addr) {
        // Pull status, ignore B and U bits (they don't exist in the register)
        // Actually, U is always 1 in the register effectively, but B is not.
        // When pulling, we overwrite flags. B flag in register is not affected by PLP?
        // According to docs: "The B flag is not set or cleared by PLP."
        // Also U bit is always 1.
        
        byte fetched = pop();
        status = (byte) ((fetched & ~B) | U); // Ensure U is set, B is ignored (or cleared?)
        // Wait, B flag doesn't exist in the status register physically. It only exists on stack.
        // So we just load the byte into status, but mask out B bit (keep it 0 or whatever it was?)
        // Actually, the B bit in the status register is unused/meaningless.
        // But usually we mask it out to avoid confusion.
        // Let's stick to: status = (fetched & ~B) | U;
    }

    // Transfer Stack Pointer to X
    public void TSX(int addr) {
        x = sp;
        setZN(x);
    }

    // Transfer X to Stack Pointer
    public void TXS(int addr) {
        sp = x;
    }

    // --- Register Transfers ---

    public void TAX(int addr) {
        x = a;
        setZN(x);
    }

    public void TAY(int addr) {
        y = a;
        setZN(y);
    }

    public void TXA(int addr) {
        a = x;
        setZN(a);
    }

    public void TYA(int addr) {
        a = y;
        setZN(a);
    }

    // --- Increment / Decrement ---

    public void INC(int addr) {
        byte val = bus.read(addr);
        val++;
        bus.write(addr, val);
        setZN(val);
    }

    public void DEC(int addr) {
        byte val = bus.read(addr);
        val--;
        bus.write(addr, val);
        setZN(val);
    }

    public void INX(int addr) {
        x++;
        setZN(x);
    }

    public void DEX(int addr) {
        x--;
        setZN(x);
    }

    public void INY(int addr) {
        y++;
        setZN(y);
    }

    public void DEY(int addr) {
        y--;
        setZN(y);
    }

    // --- Shifts & Rotates ---

    // Arithmetic Shift Left
    public void ASL(int addr) {
        byte val = bus.read(addr);
        setFlag(C, (val & 0x80) != 0);
        val <<= 1;
        bus.write(addr, val);
        setZN(val);
    }

    public void ASL_Acc() {
        setFlag(C, (a & 0x80) != 0);
        a <<= 1;
        setZN(a);
    }

    // Logical Shift Right
    public void LSR(int addr) {
        byte val = bus.read(addr);
        setFlag(C, (val & 0x01) != 0);
        val = (byte) ((val & 0xFF) >>> 1);
        bus.write(addr, val);
        setZN(val);
    }

    public void LSR_Acc() {
        setFlag(C, (a & 0x01) != 0);
        a = (byte) ((a & 0xFF) >>> 1);
        setZN(a);
    }

    // Rotate Left
    public void ROL(int addr) {
        byte val = bus.read(addr);
        int c = getFlag(C);
        setFlag(C, (val & 0x80) != 0);
        val = (byte) ((val << 1) | c);
        bus.write(addr, val);
        setZN(val);
    }

    public void ROL_Acc() {
        int c = getFlag(C);
        setFlag(C, (a & 0x80) != 0);
        a = (byte) ((a << 1) | c);
        setZN(a);
    }

    // Rotate Right
    public void ROR(int addr) {
        byte val = bus.read(addr);
        int c = getFlag(C);
        setFlag(C, (val & 0x01) != 0);
        val = (byte) (((val & 0xFF) >>> 1) | (c << 7));
        bus.write(addr, val);
        setZN(val);
    }

    public void ROR_Acc() {
        int c = getFlag(C);
        setFlag(C, (a & 0x01) != 0);
        a = (byte) (((a & 0xFF) >>> 1) | (c << 7));
        setZN(a);
    }
}
