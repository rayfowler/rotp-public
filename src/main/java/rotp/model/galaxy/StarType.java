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
package rotp.model.galaxy;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import rotp.ui.sprites.RoundGradientPaint;
import rotp.util.Base;

public class StarType implements Base {
    public static final String RED = "RED";
    public static final String YELLOW = "YELLOW";
    public static final String ORANGE = "ORANGE";
    public static final String GREEN = "GREEN";
    public static final String WHITE = "WHITE";
    public static final String BLUE = "BLUE";
    public static final String PURPLE = "PURPLE";
	
    private static RoundGradientPaint rgp;
    private static final HashMap<String, StarType> typeMap = new HashMap<>();
    public static StarType keyed(String s)       { return typeMap.get(s); }
    private static void addStarType(String s)    { typeMap.put(s, new StarType(s)); }
    private RoundGradientPaint rgp() {
        if (rgp == null) 
            rgp = new RoundGradientPaint();
        return rgp;
    }

    static {
            addStarType(RED);
            addStarType(YELLOW);
            addStarType(ORANGE);
            addStarType(GREEN);
            addStarType(WHITE);
            addStarType(BLUE);
            addStarType(PURPLE);
    }

    private String key;
    private Color color;
    private String description;
    private transient HashMap<Integer, BufferedImage> images;

    public String key()               { return key; }
    public Color color()              { return color; }
    public String description()       { return description; }

    @Override
    public String toString()   { return concat("StarType: ", key); }

    private HashMap<Integer, BufferedImage> images() {
        if (images == null)
            images = new HashMap<>();
        return images;
    }
    public int maxRadius() {
        if (veryLowMemory())
            return 30;
        else if (lowMemory())
            return 45;
        else if (midMemory())
            return 60;
        else
            return 80;
    }
    public BufferedImage image(int r, int f) {
        int r0 = min(r,scaled(maxRadius()));
        int key = hashKey(r0,f);
        if (!images().containsKey(key))
            images().put(key, createStarImage(r0,f));
        return images().get(key);
    }
    private int hashKey(int r, int f)       { return (r*200)+f; }
    private BufferedImage createStarImage(int r, int f) {
        Color c = color();
        Color c0 = newColor(c.getRed(), c.getGreen(), c.getBlue(), 0);
        int r1 = 127+(c0.getRed()/2);
        int g1 = 127+(c0.getGreen()/2);
        int b1 = 127+(c0.getBlue()/2);
        Color c1 = unscaled(r) > 70 ? Color.white : newColor(r1,g1,b1);
        //Color c1 = Color.white;
        int w = (r+f+f)*2;
        int x = w/2;
        int y = w/2;

        // draw star
        RoundRectangle2D rect = new RoundRectangle2D.Float(x-(w/2), y-(w/2), w, w, 0, 0);
        rgp().set(x, y, c1, new Point2D.Float(0, r), c0, f);

        BufferedImage img = newBufferedImage(w,w);
        Graphics2D g = (Graphics2D) img.getGraphics();
        g.setPaint(rgp());
        g.fill(rect);
        g.dispose();
        return img;
    }

    private StarType(String s) {
        initType(s);
    }
    private void initType(String s) {
        key = s;
        switch(key) {
        case RED:
            description = "RED_STAR_DESCRIPTION";
            color = Color.red;
            break;
        case YELLOW:
            description = "YELLOW_STAR_DESCRIPTION";
            color = Color.yellow;
            break;
        case ORANGE:
        case GREEN:
            description = "ORANGE_STAR_DESCRIPTION";
            color = new Color(255,128,0);
            break;
        case WHITE:
            description = "WHITE_STAR_DESCRIPTION";
            color = Color.white;
            break;
        case BLUE:
            description = "BLUE_STAR_DESCRIPTION";
            color  = Color.blue;
            break;
        case PURPLE:
            description = "PURPLE_STAR_DESCRIPTION";
            color = Color.magenta;
            break;
        }
    }
}
