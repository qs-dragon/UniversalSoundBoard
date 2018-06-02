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
package com.mru.usoundboard.board;

import com.mru.usoundboard.KeyCodeAdapter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A soundboard is a collection of Entrys associated with keycodes and file paths.
 * @author Matt
 */
public class SoundBoard {
    private static final Logger LOGGER = Logger.getLogger(SoundBoard.class.getName());
    protected final Category root = new Category(-1);
    protected String name;
    
    public SoundBoard(){
        
    }
    
    public SoundBoard(String name){
        this.name = name;
    }
    
    public void setName(String name){
        this.name = name;
    }
    
    public String getName(){
        return name;
    }
    
    public Category getRoot(){
        return root;
    }
    
    public static SoundBoard readFromFile(File f) throws FileNotFoundException,
            IOException{
        SoundBoard sb = new SoundBoard();
        
        BufferedReader reader = new BufferedReader(new FileReader(f));
        //we're gonna try to read each line of the file
        //the very first line should contain the name
        String line = reader.readLine();
        if(line != null){
            sb.setName(line);
        }
        //everything after this should be "=" seperated keycode-path pairs
        //keycodes can be alpha-numeric
        while((line = reader.readLine()) != null){
            String[] parts = line.split("=");
            String combo = parts[0];
            String path = parts[1];
            Category curCat = sb.getRoot();
            for(int x=0; x<combo.length(); x++){
                //determine if this char exists
                int code = KeyCodeAdapter.getCodeFromChar(combo.substring(x, x+1));
                //does this entry already exist?
                Entry prv = curCat.getEntry(code);
                if(prv == null){
                    //we're creating a new entry.
                    //is this a category or a sound?
                    if(x == combo.length()-1){
                        //does sound exist?
                        Entry cur = new Sound(code, path);
                        curCat.addEntry(cur);
                        LOGGER.log(Level.INFO, "Created new Sound, {0}={1}",
                                new Object[]{combo, path});
                    } else{
                        Category cur = new Category(code);
                        curCat.addEntry(cur);
                        curCat = cur;
                    }
                } else{
                    //iterate to next category or append sound
                    if(x == combo.length()-1 && prv instanceof Sound){
                        //TODO: append multiple sounds
                        Sound snd = (Sound)prv;
                        snd.addPath(path);
                    } else if(x != combo.length()-1 && prv instanceof Category){
                        curCat = (Category)prv;
                        LOGGER.log(Level.FINE, "Category already exists.");
                    } else{
                        LOGGER.log(Level.WARNING, "Cannot create entry,"
                                + " an entry already exists that is not compatable."
                                + " {0}={1}", new Object[]{combo, path});
                    }
                }
            }
        }
        return sb;
    }
    
    public void writeToFile(File f){
        
    }
}
