/*
 * Copyright 2015-2020 Ray Fowler
 * 
 * Licensed under the GNU General Public License, Version 3 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.gnu.org/licenses/gpl-3.0.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rotp.util;

import java.awt.Color;
import java.util.HashMap;

public class Palette implements Base {
    private static final int MAX_BORDERS = 25;
    private static final HashMap<String, Palette> all = new HashMap<>();
    public static Palette named(String s) { return all.get(s); }
    public Color text;
    public Color lightBack, medBack, darkBack, darkBlue, darkGray;
    public Color bdrHiOut, bdrHiIn, bdrLoOut, bdrLoIn;
    public Color black, orange, maroon, blue, aqua, green, forest, yellow, white, red;

    private final ThickBevelBorder[] insetBorders = new ThickBevelBorder[MAX_BORDERS];
    private final ThickBevelBorder[] outsetBorders = new ThickBevelBorder[MAX_BORDERS];

    static {
        createDefaultPalettes();
    }
    public Palette() {
        orange = newColor(248,127,0);
        aqua   = newColor(73,220,178);
        green  = newColor(68,206,67);
        forest = newColor(34,103,34);
        yellow = newColor(255,241,78);
        white  = newColor(209,209,209);
        red    = newColor(192,18,20);
        darkBlue = newColor(57,77,100);
        darkGray = newColor(64,64,64);
        blue   = newColor(114,155,201);
        maroon = newColor(106,9,0);
        black  = newColor(19,19,19);
    }
    public ThickBevelBorder outsetBorder(int thick) {
        int w = thick-1;
        if (w >= MAX_BORDERS) 
            return new ThickBevelBorder(w, 1, bdrHiOut, bdrHiIn, bdrLoIn, bdrLoOut, bdrLoIn, bdrLoOut, bdrHiOut, bdrHiIn);
        if (outsetBorders[w] == null )
            outsetBorders[w] = new ThickBevelBorder(w, 1, bdrHiOut, bdrHiIn, bdrLoIn, bdrLoOut, bdrLoIn, bdrLoOut, bdrHiOut, bdrHiIn);
        return outsetBorders[w];
    }
    public ThickBevelBorder insetBorder(int thick) {
        int w = thick-1;
        if (w >= MAX_BORDERS) 
            return new ThickBevelBorder(w, 1, bdrLoIn, bdrLoOut, bdrHiOut, bdrHiIn, bdrHiOut, bdrHiIn, bdrLoIn, bdrLoOut);
        if (insetBorders[w] == null )
            insetBorders[w] = new ThickBevelBorder(w, 1, bdrLoIn, bdrLoOut, bdrHiOut, bdrHiIn, bdrHiOut, bdrHiIn, bdrLoIn, bdrLoOut);
        return insetBorders[w];
    }
    private static void createDefaultPalettes() {
        Palette brown    = new Palette();
        brown.text       = new Color(201,154,115);
        brown.lightBack  = new Color(132,98,77);
        brown.medBack    = new Color(112,84,68);
        brown.darkBack   = new Color(76,57,41);
        brown.bdrHiOut   = new Color(168,126,99);
        brown.bdrHiIn    = new Color(150,112,90);
        brown.bdrLoOut   = new Color(33,32,34);
        brown.bdrLoIn    = new Color(61,41,28);
        all.put("Brown", brown);
    }
}
