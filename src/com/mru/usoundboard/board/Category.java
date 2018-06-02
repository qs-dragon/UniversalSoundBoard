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

import java.util.HashSet;
import java.util.Set;



/**
 * Categories are lists of other entries.
 * @author Matt
 */
public class Category extends Entry{
    protected final Set<Entry> entries = new HashSet<>();

    public Category(int keycode) {
        super(keycode);
    }
    
    public void addEntry(Entry entry){
        entries.add(entry);
    }
    
    public boolean removeEntry(Entry entry){
        return entries.remove(entry);
    }
    
    /**
     * Find an entry in this category that matches the given keycode
     * @param keyCode
     * @return the first entry or null if there is none
     */
    public Entry getEntry(int keyCode){
        for(Entry e : entries){
            if(e.getKeyCode() == keyCode){
                return e;
            }
        }
        return null;
    }
    
    public Set<Entry> getEntries(){
        return entries;
    }
}
