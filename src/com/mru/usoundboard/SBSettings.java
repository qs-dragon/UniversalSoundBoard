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

import java.util.Properties;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import org.jnativehook.keyboard.NativeKeyEvent;

/**
 * Soundboard properties file with easy operators cause lazy n stuff
 * @author Matt
 */
public class SBSettings extends Properties{
    public static final String HOTKEY="Hotkey";
    public static final String HKMOD="HKMod";
    public static final String MIXER="Mixer";
    public static final String SOUNDBOARD="Soundboard";
    
    public SBSettings(){
        super();
        loadDefaults();
    }
    
    public void loadDefaults(){
        setProperty(HOTKEY, String.valueOf(NativeKeyEvent.VC_V));
        setProperty(HKMOD, String.valueOf(NativeKeyEvent.CTRL_L_MASK));
    }
    
    public void setHotkey(int v){
        setProperty(HOTKEY, String.valueOf(v));
    }
    
    public void setHotkeyMod(int v){
        setProperty(HKMOD, String.valueOf(v));
    }
    
    public int getHotkey(){
        String val = getProperty(HOTKEY);
        int v = Integer.valueOf(val);
        return v;
    }
    
    public int getHotkeyMod(){
        String val = getProperty(HKMOD);
        int v = Integer.valueOf(val);
        return v;
    }
    
    /**
     * Compares all currently available mixers to the mixer info in the saved file
     * if ALL parts of the mixer info match return the info, else returns null
     * @return The matching mixer info, else null
     */
    public Mixer.Info getMixer(){
        String val = getProperty(MIXER);
        String[] parts = val.split(",");
        for(Mixer.Info info : AudioSystem.getMixerInfo()){
            if(info.getName().equals(parts[0]) && info.getVendor().equals(parts[1])
                && info.getDescription().equals(parts[2]) && info.getVersion().equals(parts[3])){
                return info;
            }
        }
        return null;
    }
    
    public void setMixer(Mixer.Info mixer){
        String val = mixer.getName()+","+mixer.getVendor()+","+mixer.getDescription()
                +","+mixer.getVersion();
        setProperty(MIXER, val);
    }
    
    public String getSoundboard(){
        return getProperty(SOUNDBOARD);
    }
    
    public void setSoundboard(String path){
        setProperty(SOUNDBOARD, path);
    }
}
