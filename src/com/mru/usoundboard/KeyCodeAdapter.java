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

/**
 * 
 * @author Matt
 */
public class KeyCodeAdapter {
    
    /**
     * Parses single character text strings to find the "Native Keycode" for the
     * string. THIS IS NOT CROSS PLATFORM NOR WILL IT WORK ON NON US KEYBOARDS!
     * @param c
     * @return 
     */
    public static int getCodeFromChar(String c){
        int v = -1;
        switch(c.toLowerCase()){
            case "a":v=30; break;
            case "b":v=48; break;
            case "c":v=46; break;
            case "d":v=32; break;
            case "e":v=18; break;
            case "f":v=33; break;
            case "g":v=34; break;
            case "h":v=35; break;
            case "i":v=23; break;
            case "j":v=36; break;
            case "k":v=37; break;
            case "l":v=38; break;
            case "m":v=50; break;
            case "n":v=49; break;
            case "o":v=24; break;
            case "p":v=25; break;
            case "q":v=16; break;
            case "r":v=19; break;
            case "s":v=31; break;
            case "t":v=20; break;
            case "u":v=22; break;
            case "v":v=47; break;
            case "w":v=17; break;
            case "x":v=45; break;
            case "y":v=21; break;
            case "z":v=44; break;
        }
        return v;
    } 
}
