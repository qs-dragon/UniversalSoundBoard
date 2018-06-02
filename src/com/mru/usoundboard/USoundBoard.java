/* 
 * Copyright (C) 2018 Matthew Universe
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.mru.usoundboard;

import com.mru.usoundboard.board.Category;
import com.mru.usoundboard.board.Entry;
import com.mru.usoundboard.board.Sound;
import com.mru.usoundboard.board.SoundBoard;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Mixer.Info;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.NativeInputEvent;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

/**
 * Universal Sound Board
 * Inspired by Tribe's (and also Smite's) "VGS" system where you access audio bites
 * by typing a key-code (such as V(voice)G(General)S(Shazbot)). This app lets the
 * user write their own VGS system where they can define their own key commands
 * in easy to define text documents.
 * 
 * Each command can be given multiple audio file locations which can be looked up
 * during runtime. If a command has multiple definitions one is played randomly.
 * 
 * For extra fun install a "Virtual audio cable" and loop the output from your
 * personal soundboard to the microphone for in-game chat!
 * @author Matt
 */
public class USoundBoard implements NativeKeyListener{
    private static final Logger LOGGER = Logger.getLogger(USoundBoard.class.getName());
    private final Timer timer = new Timer();
    private final SBSettings settings;
    private TimerTask curTask;
    private JFrame frame;
    private JDialog shortcutDialog;
    private JFileChooser fileBrowser;
    private JLabel sbLabel;
    private Mixer.Info[] mixers;
    private Mixer.Info curMixer;
    private Clip clip;
    private SoundBoard curSoundBoard;
    private File soundBoardFile;
    private int hotKey = NativeKeyEvent.VC_V;
    private int tempKey = NativeKeyEvent.VC_V;
    private int hotkeyMod = NativeKeyEvent.CTRL_L_MASK;
    private boolean capture = false;
    private long timeoutDelay = 3000;
    private String combo;
    private Category curCategory;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        //load an app settings properties file
        SBSettings settings = new SBSettings();
        //attempt to load user settings
        USoundBoard board = new USoundBoard(settings);
        board.start();
    }
    
    private void loadSettings(){
        //attempt to load previous settings
        File settingsFile = getSettingsFile();
        if(settingsFile.exists()){
            try (FileReader reader = new FileReader(settingsFile)){
                settings.load(reader);
                LOGGER.log(Level.INFO, "User settings loaded successfully");
            } catch (IOException ex) {
                Logger.getLogger(USoundBoard.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else{
            LOGGER.log(Level.INFO, "User settings could not be found, using defaults");
        }
    }
    
    public USoundBoard(SBSettings settings){
        //TODO: load the last used sb instead of making a test one every time
        //create a default soundboard with my shitty test sounds ;)
        //load in default settings
        this.settings = settings;
        loadSettings();
        hotKey = settings.getHotkey();
        hotkeyMod = settings.getHotkeyMod();
        String sbPath = settings.getSoundboard();
        if(sbPath != null && sbPath.length() > 0){
            File f = new File(sbPath);
            if(f.exists()){
                SoundBoard sb;
                try {
                    sb = SoundBoard.readFromFile(f);
                    soundBoardFile = f;
                    curSoundBoard = sb;
                    curCategory = curSoundBoard.getRoot();
                } catch (IOException ex) {
                    Logger.getLogger(USoundBoard.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        if(curSoundBoard == null){
            SoundBoard sb = new SoundBoard("Test Soundboard");
            Category g = new Category(NativeKeyEvent.VC_G);
            sb.getRoot().addEntry(g);
            g.addEntry(new Sound(NativeKeyEvent.VC_G, "Blanc/ItsOnNow.wav"));
            curSoundBoard = sb;
        }
    }
    
    public void start(){
        //locate user data and get a list of soundboards
        
        try {
            //setup input
            GlobalScreen.setEventDispatcher(new VoidDispatchService());
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(this);
            Logger.getLogger(GlobalScreen.class.getPackage().getName()).setLevel(Level.OFF);
        } catch (NativeHookException ex) {
            //if this fails the whole program is fubar!zsd
            Logger.getLogger(USoundBoard.class.getName()).log(Level.SEVERE, null, ex);
        }
        //list audio mixers
        //only lists audio mixers capable of playing audio
        Mixer.Info[] allMixers = AudioSystem.getMixerInfo();
        List<Mixer.Info> ml = new ArrayList<>();
        for(Mixer.Info info : allMixers){
            try {
                Clip c = AudioSystem.getClip(info);
                if(c != null){
                    ml.add(info);
                }
            } catch (LineUnavailableException | IllegalArgumentException ex) {}
        }
        mixers = ml.toArray(new Mixer.Info[ml.size()]);
        
        buildUI();
        selectMixer(mixers[0]);
    }
    
    /**
     * 
     * @param info The system mixer info that is requested or null for default
     */
    private void selectMixer(Info info){
        LOGGER.log(Level.INFO, "Selecting {0} as output.", info);
        try {
            clip = AudioSystem.getClip(info);
            curMixer = info;
            settings.setMixer(curMixer);
        } catch (LineUnavailableException | IllegalArgumentException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }
    
    private void setSoundBoard(SoundBoard board){
        curSoundBoard = board;
        curCategory = board.getRoot();
        sbLabel.setText(curSoundBoard.getName());
    }
    
    public void buildUI(){
        frame = new JFrame("USoundBoard");
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
        
        fileBrowser = new JFileChooser();
        fileBrowser.addChoosableFileFilter(new FileNameExtensionFilter("usb","USB"));
        fileBrowser.addActionListener((ActionEvent e) -> {
            File f = fileBrowser.getSelectedFile();
            try {
                //attempt to load this file
                SoundBoard sb = SoundBoard.readFromFile(f);
                setSoundBoard(sb);
                soundBoardFile = f;
                settings.setSoundboard(soundBoardFile.getPath());
            } catch (IOException ex) {
                Logger.getLogger(USoundBoard.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        
        shortcutDialog = new JDialog(frame, "Choose a shortcut key", true);
        shortcutDialog.setLayout(new BoxLayout(shortcutDialog.getContentPane(),
                BoxLayout.Y_AXIS));
        JLabel scdDesc = new JLabel("Press any character...");
        scdDesc.setAlignmentX(Component.CENTER_ALIGNMENT);
        shortcutDialog.add(scdDesc);
        JLabel shortcutText = new JLabel(NativeKeyEvent.getKeyText(hotKey));
        shortcutDialog.add(shortcutText);
        JButton cancelSCD = new JButton("Cancel");
        JButton acceptSCD = new JButton("Accept");
        JPanel scdPanel = new JPanel(new FlowLayout());
        ActionListener closeSCD = (ActionEvent e) -> {
            shortcutDialog.setVisible(false);
        };
        //button
        JButton setShortcut = new JButton(NativeKeyEvent.getKeyText(hotKey));
        cancelSCD.addActionListener(closeSCD);
        acceptSCD.addActionListener(closeSCD);
        acceptSCD.addActionListener((ActionEvent e) -> {
            //the temp key becomes the regular key and change the setShortcut text
            hotKey = tempKey;
            setShortcut.setText(NativeKeyEvent.getKeyText(hotKey));
            settings.setHotkey(hotKey);
        });
        scdPanel.add(cancelSCD);
        scdPanel.add(acceptSCD);
        shortcutDialog.add(scdPanel);
        NativeKeyListener shortcutListener = new NativeKeyListener(){
            @Override
            public void nativeKeyTyped(NativeKeyEvent nativeEvent) {
            }

            @Override
            public void nativeKeyPressed(NativeKeyEvent nativeEvent) {
                tempKey = nativeEvent.getKeyCode();
                shortcutText.setText(NativeKeyEvent.getKeyText(tempKey));
            }

            @Override
            public void nativeKeyReleased(NativeKeyEvent nativeEvent) {
            }
        };
        shortcutDialog.addComponentListener(new ComponentListener(){
            @Override
            public void componentResized(ComponentEvent e) {            }

            @Override
            public void componentMoved(ComponentEvent e) {            }

            @Override
            public void componentShown(ComponentEvent e) {
                GlobalScreen.addNativeKeyListener(shortcutListener);
                LOGGER.log(Level.FINE, "Listening for user to type a shortcut");
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                GlobalScreen.removeNativeKeyListener(shortcutListener);
                LOGGER.log(Level.FINE, "Stopped listening for new shortcut");
            }
        });
        shortcutDialog.pack();
        
        //audio selection and options
        JLabel selout = new JLabel("Select Output");
        selout.setAlignmentX(Component.CENTER_ALIGNMENT);
        frame.add(selout);
        
        JComboBox mixerSelect = new JComboBox(mixers);
        mixerSelect.setAlignmentX(Component.CENTER_ALIGNMENT);
        frame.add(mixerSelect);
        Mixer.Info prvInfo = settings.getMixer();
        if(prvInfo != null){
            mixerSelect.setSelectedItem(prvInfo);
        }
        mixerSelect.addActionListener((ActionEvent e) -> {
            selectMixer((Info)mixerSelect.getSelectedItem());
        });
        
        //soundboard selection and options
        JLabel sbDesc = new JLabel("Current Soundboard");
        sbDesc.setAlignmentX(Component.CENTER_ALIGNMENT);
        frame.add(sbDesc);
        sbLabel = new JLabel(curSoundBoard.getName());
        sbLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        frame.add(sbLabel);
        JButton selectSoundboard = new JButton("Select Soundboard");
        selectSoundboard.setAlignmentX(Component.CENTER_ALIGNMENT);
        frame.add(selectSoundboard);
        selectSoundboard.addActionListener((ActionEvent e) -> {
            //open a file browser to choose a soundboard file
            fileBrowser.showOpenDialog(null);
        });
        
        //shortcut
        JLabel shortDesc = new JLabel("Modifier and Hot Key");
        shortDesc.setAlignmentX(Component.CENTER_ALIGNMENT);
        frame.add(shortDesc);
        Panel shortcut = new Panel(new FlowLayout());
        SelectableMod[] mods = new SelectableMod[]{
            new SelectableMod(NativeKeyEvent.CTRL_L_MASK, "Left CTRL"),
            new SelectableMod(NativeKeyEvent.SHIFT_L_MASK, "Left Shift"),
            new SelectableMod(NativeKeyEvent.ALT_L_MASK, "Left Alt"),
            new SelectableMod(NativeKeyEvent.CTRL_R_MASK, "Right CTRL"),
            new SelectableMod(NativeKeyEvent.SHIFT_R_MASK, "Right Shift"),
            new SelectableMod(NativeKeyEvent.ALT_R_MASK, "Right Alt"),
        };
        JComboBox modifierSelect = new JComboBox(mods);
        for(int x=0; x<mods.length; x++){
            if(mods[x].getKeyCode() == settings.getHotkeyMod()){
                modifierSelect.setSelectedIndex(x);
                break;
            }
        }
        modifierSelect.addItemListener((ItemEvent e) -> {
            if(e.getStateChange() == ItemEvent.SELECTED){
                SelectableMod mod = (SelectableMod)e.getItem();
                hotkeyMod = mod.getKeyCode();
                settings.setHotkeyMod(hotkeyMod);
                LOGGER.log(Level.INFO, "Modifier key set to {0}", mod);
            }
        });
        shortcut.add(modifierSelect);
        setShortcut.addActionListener((ActionEvent e) -> {
            //modal popup and input listener
            shortcutDialog.setVisible(true);
        });
        shortcut.add(setShortcut);
        frame.add(shortcut);
        
        JLabel credit = new JLabel("Created by Matthew Universe");
        credit.setAlignmentX(Component.CENTER_ALIGNMENT);
        frame.add(credit);
        String url = "https://mrugames.blogspot.com";
        JButton blog = new JButton(url);
        blog.setAlignmentX(Component.CENTER_ALIGNMENT);
        blog.addActionListener((ActionEvent e) -> {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (URISyntaxException | IOException ex) {
                Logger.getLogger(USoundBoard.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        frame.add(blog);
        //finalize frame
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.addWindowListener(new WindowAdapter(){
            @Override
            public void windowClosing(WindowEvent e) {
                stop();
            }
        });
        frame.setVisible(true);
    }
    
    private void playSound(Sound s){
        //we need to determine where the sound file actually is. We'll start looking
        //in the app dir, if that fails we'll move to the user.home, if that fails
        //we'll try all the way from root directory
        if(clip.isRunning()){
            LOGGER.log(Level.INFO, "Cannot play a new sound until the first has finished");
            return;
        }
        LOGGER.log(Level.INFO, "Playing sound {0} {1}", new Object[]{combo, s.getPath()});
        try {
            File f = resolvePath(s.getPath());
            try (AudioInputStream stream = AudioSystem.getAudioInputStream(f)) {
                clip.addLineListener((LineEvent event) -> {
                    if(event.getType() == LineEvent.Type.STOP){
                        clip.close();
                    }
                });
                clip.open(stream);
                clip.start();
            }
        } catch (FileNotFoundException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * TODO: Resolve from the soundboard definition directory first!
     * Attempt to resolve the path to the file, first user.dir, then user.home
     * and finally from root dir.
     * @param path
     * @return 
     */
    private File resolvePath(String path) throws FileNotFoundException{
        File f = new File(soundBoardFile.getParent()+File.separator+path);
        if(!f.exists()){
            f = new File(System.getProperty("user.dir")+File.separator+path);
            if(!f.exists()){
                f = new File(path);
                if(!f.exists()){
                    throw new FileNotFoundException("Cannot locate "+path);
                }
            }
        }
        return f;
    }
    
    private void enableCapture(){
        curTask = new Timeout();
        timer.schedule(curTask, timeoutDelay);
        capture = true;
        combo = "";
        LOGGER.log(Level.INFO, "Capture started");
    }
    
    private void disableCapture(){
        curTask.cancel();
        capture = false;
        curCategory = curSoundBoard.getRoot();
        LOGGER.log(Level.INFO, "Capture stopped");
    }
    
    private void activateEntry(Entry e){
        //if this entry is a category we want to enter it, if it is a sound we want
        //to play it!
        combo += NativeKeyEvent.getKeyText(e.getKeyCode());
        if(e instanceof Category){
            curCategory = (Category)e;
            //each keypress should reset the timer
            curTask.cancel();
            curTask = new Timeout();
            timer.schedule(curTask, timeoutDelay);
        } else if(e instanceof Sound){
            disableCapture();
            playSound((Sound)e);
        }
    }
    
    public void stop(){
        LOGGER.log(Level.INFO, "Application is closing.");
        //now is a good time to write our settings
        File settingsFile = getSettingsFile();
        if(!settingsFile.exists()){
            settingsFile.getParentFile().mkdirs();
            try {
                settingsFile.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(USoundBoard.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        try (FileWriter writer = new FileWriter(settingsFile)){
            settings.store(writer, "");
            LOGGER.log(Level.INFO, "Saved user preferences for next time");
        } catch (IOException ex) {
            Logger.getLogger(USoundBoard.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private File getSettingsFile(){
        String path = System.getProperty("user.home")+File.separator+
                ".usoundboard"+File.separator+"USoundBoard.properties";
        return new File(path);
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent nativeEvent) {
        
    }
    
    private void consumeEvent(NativeKeyEvent event){
        try {
            Field f = NativeInputEvent.class.getDeclaredField("reserved");
            f.setAccessible(true);
            f.setShort(event, (short)0x01);
        } catch (NoSuchFieldException | SecurityException 
                | IllegalArgumentException | IllegalAccessException ex) {
            Logger.getLogger(USoundBoard.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent nativeEvent) {
        int keyCode = nativeEvent.getKeyCode();
        
        //TODO: consume event!
        //need to enable "capture" mode if the hotkey and mod are pressed
        if(!capture){
            if(keyCode == hotKey && nativeEvent.getModifiers() == hotkeyMod){
                enableCapture();
                consumeEvent(nativeEvent);
            }
        } else{
            //can now capture a combo, can cancel early with hotkey&mod or timeout
            //use keycode to check next key in combo, if failed print fail and
            //disable capture, else continue until a sound is found
            //check current category for entries
            Entry e = curCategory.getEntry(keyCode);
            if(e != null){
                activateEntry(e);
            } else{
                disableCapture();
                LOGGER.log(Level.INFO, "No entry found for the key {0}{1}",
                        new Object[]{combo, NativeKeyEvent.getKeyText(keyCode)});
            }
            consumeEvent(nativeEvent);
        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent nativeEvent) {
        
    }
    
    private class Timeout extends TimerTask{
        @Override
        public void run(){
            disableCapture();
            LOGGER.log(Level.INFO, "Capture timed out.");
        }
    }
    
    private class SelectableMod{
        private final int keyCode;
        private final String description;
        
        protected SelectableMod(int keyCode, String description){
            this.keyCode = keyCode;
            this.description = description;
        }
        
        public int getKeyCode(){
            return keyCode;
        }
        
        @Override
        public String toString(){
            return description;
        }
    }
    
    private class VoidDispatchService extends AbstractExecutorService{
        private boolean running = false;
        
        public VoidDispatchService(){
            running = true;
        }
        
        @Override
        public void shutdown() {
            running = false;
        }

        @Override
        public List<Runnable> shutdownNow() {
            running = false;
            return new ArrayList<>(0);
        }

        @Override
        public boolean isShutdown() {
            return !running;
        }

        @Override
        public boolean isTerminated() {
            return !running;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return true;
        }

        @Override
        public void execute(Runnable command) {
            command.run();
        }
        
    }
}
