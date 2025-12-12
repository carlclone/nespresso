package com.nes;

import com.nes.cpu.Cpu;

public class Main {
    public static void main(String[] args) {
        System.out.println("NES Emulator Started");
        
        try {
            // Load Cartridge
            Cartridge cart;
            if (args.length > 0) {
                cart = new Cartridge(args[0]);
                System.out.println("Loaded ROM: " + args[0]);
            } else {
                System.out.println("No ROM provided. Creating dummy cartridge.");
                // Create a dummy cartridge
                byte[] prg = new byte[16384];
                byte[] chr = new byte[8192];
                
                // Fill CHR with a simple pattern for testing
                for (int i = 0; i < 8192; i++) {
                    chr[i] = (byte) ((i % 256) ^ 0xAA);
                }
                
                // Reset Vector
                prg[0x3FFC] = (byte) 0x00;
                prg[0x3FFD] = (byte) 0x80;
                
                cart = new Cartridge(prg, chr, 0);
            }
            
            Bus bus = new Bus();
            Cpu cpu = new Cpu();
            
            bus.connectCpu(cpu);
            bus.insertCartridge(cart);
            cpu.connectBus(bus);
            
            bus.reset();
            
            // Create and show GUI window
            EmulatorWindow window = new EmulatorWindow(bus);
            window.start();
            
            // Run emulation loop in separate thread
            Thread emulationThread = new Thread(() -> {
                System.out.println("Emulation thread started");
                
                final long TARGET_TIME = 1000000000 / 60; // 60 FPS in nanoseconds

                while (window.isRunning()) {
                    long startTime = System.nanoTime();
                    
                    // Run one frame worth of cycles
                    // One frame = 262 scanlines * 341 cycles * 3 (CPU runs at 1/3 PPU speed)
                    // But we call bus.clock() which handles the 3:1 ratio
                    int frameCycles = 262 * 341;
                    
                    for (int i = 0; i < frameCycles; i++) {
                        bus.clock();
                    }
                    
                    // Render the frame immediately after emulation
                    window.renderFrame();
                    
                    // Debug Alignment
                    if (bus.getPpu().getFrame() % 60 == 0) {
                        System.out.println("End of Frame Loop: Scanline=" + bus.getPpu().getScanline() + ", Cycle=" + bus.getPpu().getCycle());
                    }
                    
                    // Debug Output every 60 frames
                    if (bus.getPpu().getFrame() % 60 == 0) {
                        Ppu ppu = bus.getPpu();
                        System.out.println(String.format("Frame: %d, PPU CTRL: %02X, MASK: %02X, STATUS: %02X",
                            ppu.getFrame(), ppu.getPpuCtrl(), ppu.getPpuMask(), ppu.getPpuStatus()));
                    }

                    // Adaptive High-Precision Frame Timing
                    long endTime = System.nanoTime();
                    long duration = endTime - startTime;
                    long waitTime = TARGET_TIME - duration;
                    
                    if (waitTime > 0) {
                        try {
                            // Sleep for most of the time (up to 1ms before target), to save CPU
                            // Windows Thread.sleep is not precise, so we leave a buffer.
                            if (waitTime > 1000000) {
                                Thread.sleep((waitTime - 1000000) / 1000000);
                            }
                            
                            // Busy-wait for the remaining time for high precision
                            while (System.nanoTime() - startTime < TARGET_TIME) {
                                // Spin
                            }
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
                
                System.out.println("Emulation thread stopped");
            });
            
            emulationThread.start();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
