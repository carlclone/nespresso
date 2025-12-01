package com.nes;

import com.nes.cpu.Cpu;

public class Main {
    public static void main(String[] args) {
        System.out.println("NES Emulator Started");
        
        try {
            // Load Cartridge
            // For now, use a dummy cartridge or a file if provided
            Cartridge cart;
            if (args.length > 0) {
                cart = new Cartridge(args[0]);
                System.out.println("Loaded ROM: " + args[0]);
            } else {
                System.out.println("No ROM provided. Creating dummy cartridge.");
                // Create a dummy cartridge with a simple program
                // 0x8000: A9 55 (LDA #$55)
                // 0x8002: 8D 00 02 (STA $0200)
                // 0x8005: 4C 00 80 (JMP $8000)
                byte[] prg = new byte[16384];
                prg[0] = (byte) 0xA9; prg[1] = (byte) 0x55;
                prg[2] = (byte) 0x8D; prg[3] = (byte) 0x00; prg[4] = (byte) 0x02;
                prg[5] = (byte) 0x4C; prg[6] = (byte) 0x00; prg[7] = (byte) 0x80;
                
                // Reset Vector at 0xFFFC
                prg[0x3FFC] = (byte) 0x00;
                prg[0x3FFD] = (byte) 0x80;
                
                byte[] chr = new byte[8192];
                cart = new Cartridge(prg, chr, 0);
            }
            
            Bus bus = new Bus();
            Cpu cpu = new Cpu();
            
            bus.connectCpu(cpu);
            bus.insertCartridge(cart);
            cpu.connectBus(bus);
            
            bus.reset();
            
            // Run for a few cycles to demonstrate
            System.out.println("Running...");
            for (int i = 0; i < 100; i++) {
                bus.clock();
                // Simple debug output
                if (cpu.cycles == 0) {
                    // System.out.println("PC: " + Integer.toHexString(cpu.pc) + " A: " + Integer.toHexString(cpu.a));
                }
            }
            System.out.println("Finished 100 cycles.");
            System.out.println("Final A: " + String.format("%02X", cpu.a));
            System.out.println("RAM[0x0200]: " + String.format("%02X", bus.read(0x0200)));
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
