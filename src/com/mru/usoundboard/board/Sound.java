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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Sounds are entries that have paths to the final sound file.
 * @author Matt
 */
public class Sound extends Entry{
    // needs support for multiple paths!
    private static final Random RANDOM = new Random();
    protected final List<String> paths = new ArrayList<>();

    public Sound(int keycode, String path) {
        super(keycode);
        paths.add(path);
    }
    
    public String getPath(){
        if(paths.size() > 1){
            int r = RANDOM.nextInt(paths.size());
            return paths.get(r);
        } else{
            return paths.get(0);
        }
    }
    
    public void addPath(String path){
        paths.add(path);
    }
}
