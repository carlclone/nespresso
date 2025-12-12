package com.nes;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Display window for the NES emulator.
 * Shows the PPU frame buffer output.
 */
public class EmulatorWindow extends JFrame {
    
    private static final int NES_WIDTH = 256;
    private static final int NES_HEIGHT = 240;
    private static final int SCALE = 3;
    private static final int WINDOW_WIDTH = NES_WIDTH * SCALE;
    private static final int WINDOW_HEIGHT = NES_HEIGHT * SCALE;
    
    private Bus bus;
    private BufferedImage frameImage;
    private Canvas displayCanvas;
    private boolean running = false;
    
    public EmulatorWindow(Bus bus) {
        this.bus = bus;
        
        setTitle("NES Emulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        
        // Create buffered image for frame buffer
        frameImage = new BufferedImage(NES_WIDTH, NES_HEIGHT, BufferedImage.TYPE_INT_RGB);
        
        // Create display canvas
        displayCanvas = new Canvas();
        displayCanvas.setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        displayCanvas.setFocusable(false); // Window handles focus
        add(displayCanvas);
        
        pack();
        setLocationRelativeTo(null); // Center on screen
        
        // Timer removed - rendering is now driven by the main emulation loop
        
        // Add Key Listener for Controller Input
        addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                handleInput(e.getKeyCode(), true);
            }

            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                handleInput(e.getKeyCode(), false);
            }
        });
        
        setFocusable(true);
        requestFocusInWindow();
    }
    
    private void handleInput(int keyCode, boolean pressed) {
        Controller controller = bus.getController(0); // Player 1
        
        switch (keyCode) {
            case java.awt.event.KeyEvent.VK_X:
                controller.setButtonPressed(Controller.BUTTON_A, pressed);
                break;
            case java.awt.event.KeyEvent.VK_Z:
                controller.setButtonPressed(Controller.BUTTON_B, pressed);
                break;
            case java.awt.event.KeyEvent.VK_A:
                controller.setButtonPressed(Controller.BUTTON_SELECT, pressed);
                break;
            case java.awt.event.KeyEvent.VK_S:
                controller.setButtonPressed(Controller.BUTTON_START, pressed);
                break;
            case java.awt.event.KeyEvent.VK_UP:
                controller.setButtonPressed(Controller.BUTTON_UP, pressed);
                break;
            case java.awt.event.KeyEvent.VK_DOWN:
                controller.setButtonPressed(Controller.BUTTON_DOWN, pressed);
                break;
            case java.awt.event.KeyEvent.VK_LEFT:
                controller.setButtonPressed(Controller.BUTTON_LEFT, pressed);
                break;
            case java.awt.event.KeyEvent.VK_RIGHT:
                controller.setButtonPressed(Controller.BUTTON_RIGHT, pressed);
                break;
        }
    }
    
    /**
     * Start the emulator display
     */
    public void start() {
        running = true;
        setVisible(true);
        // Create triple buffer strategy for better VSync
        displayCanvas.createBufferStrategy(3);
    }
    
    /**
     * Stop the emulator display
     */
    public void stop() {
        running = false;
    }
    
    /**
     * Update frame buffer from PPU and repaint.
     * Called synchronously from the emulation loop.
     */
    public void renderFrame() {
        if (!running) return;
        
        // Get frame buffer from PPU
        int[] frameBuffer = bus.getFrameBuffer();
        
        // Synchronize image update
        synchronized (frameImage) {
            frameImage.setRGB(0, 0, NES_WIDTH, NES_HEIGHT, frameBuffer, 0, NES_WIDTH);
        }
        
        // Render using BufferStrategy
        java.awt.image.BufferStrategy bs = displayCanvas.getBufferStrategy();
        if (bs == null) return;
        
        Graphics g = bs.getDrawGraphics();
        Graphics2D g2d = (Graphics2D) g;
        
        // Settings
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
                            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                            
        // Clear background (optional since we draw over everything)
        // g.setColor(Color.BLACK);
        // g.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
        
        // Draw Image
        synchronized (frameImage) {
            g2d.drawImage(frameImage, 0, 0, WINDOW_WIDTH, WINDOW_HEIGHT, null);
        }
        
        g.dispose();
        bs.show();
        
        // Toolkit.sync() is not needed with BufferStrategy and can cause stutter
        // Toolkit.getDefaultToolkit().sync();
    }
    
    /**
     * Custom panel for rendering the NES display
     */
    // DisplayPanel class removed
    
    public boolean isRunning() {
        return running;
    }
}
