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

import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
import rotp.model.game.IGameOptions;

public class GalaxySpiralShape extends GalaxyShape {
    private static final long serialVersionUID = 1L;
    public static final List<String> options1;
    public static final List<String> options2;
    static {
        options1 = new ArrayList<>();
        options1.add("SETUP_SPIRAL_2_ARMS");
        options1.add("SETUP_SPIRAL_3_ARMS");
        options1.add("SETUP_SPIRAL_4_ARMS");
        options1.add("SETUP_SPIRAL_5_ARMS");
        options1.add("SETUP_SPIRAL_6_ARMS");
        options1.add("SETUP_SPIRAL_7_ARMS");
        options1.add("SETUP_SPIRAL_8_ARMS");
        options2 = new ArrayList<>();
        options2.add("SETUP_SPIRAL_ROTATION_0");
        options2.add("SETUP_SPIRAL_ROTATION_1");
        options2.add("SETUP_SPIRAL_ROTATION_2");
        options2.add("SETUP_SPIRAL_ROTATION_3");
        options2.add("SETUP_SPIRAL_ROTATION_4");
        options2.add("SETUP_SPIRAL_ROTATION_5");
        options2.add("SETUP_SPIRAL_ROTATION_6");
    }
    public static int numOptions1 = 7;
    public static int numOptions2 = 7;
    float numArms = 8;
    float armOffsetMax = 0.7f;
    float rotationFactor = 0;
    float armSeparationDistance = 2 * (float)Math.PI / numArms;
    Shape circle;
    float adjust_density = 1.0f; // modnar: adjust stellar density
	
    float randomX = 0;
    float randomY = 0;
    public GalaxySpiralShape(IGameOptions options) {
        opts = options;
    }
    @Override
    public List<String> options1()  { return options1; }
    @Override
    public List<String> options2()  { return options2; }
    @Override
    public String defaultOption1()  { return options1.get(2); }
    @Override
    public String defaultOption2()  { return options2.get(3); }
    @Override
    public void init(int n) {
        super.init(n);
        int option1 = max(0, options1.indexOf(opts.selectedGalaxyShapeOption1()));
        int option2 = max(0, options2.indexOf(opts.selectedGalaxyShapeOption2()));

        numArms = option1+2;
        rotationFactor = option2;
        armSeparationDistance = 2 * (float)Math.PI / numArms;
        
        circle = new Ellipse2D.Float(0,0,galaxyWidthLY(), galaxyHeightLY());
    }
    @Override
    public float maxScaleAdj()               { return 1.1f; }
    @Override
    protected int galaxyWidthLY() { 
        return (int) (Math.sqrt(adjust_density*maxStars*adjustedSizeFactor()));
    }
    @Override
    protected int galaxyHeightLY() { 
        return (int) (Math.sqrt(adjust_density*maxStars*adjustedSizeFactor()));
    }
    @Override
    public void setRandom(Point.Float pt) {
        float buff = galaxyEdgeBuffer();
        float adjW = width-buff-buff;
        float adjH = height-buff-buff;
        
        float dist = random();
        dist = dist * dist;
        
        float angle = random()*2*(float)Math.PI;
        float armOffset = random() * armOffsetMax;
        armOffset = (armOffset - armOffsetMax/2)/dist;
        armOffset = armOffset > 0 ? armOffset*armOffset : -1*armOffset*armOffset;
        
        float rotation = dist * rotationFactor;
        angle = (int)(angle/armSeparationDistance)*armSeparationDistance+armOffset+rotation;
        
        float rX = (float)(Math.cos(angle)*dist);
        float rY = (float)(Math.sin(angle)*dist);
        pt.x = buff+(adjW*(1+rX)/2);
        pt.y = buff+(adjH*(1+rY)/2);
    }
    @Override
    public boolean valid(Point.Float pt) {
        return true;
    }
    float randomLocation(float max, float buff) {
        return buff + (random() * (max-buff-buff));
    }
    @Override
    protected float sizeFactor(String size) {
        switch (opts.selectedGalaxySize()) {
            case IGameOptions.SIZE_TINY:      return 24; 
            case IGameOptions.SIZE_SMALL:     return 24; 
            case IGameOptions.SIZE_SMALL2:    return 25;
            case IGameOptions.SIZE_MEDIUM:    return 26; 
            case IGameOptions.SIZE_MEDIUM2:   return 29; 
            case IGameOptions.SIZE_LARGE:     return 32; 
            case IGameOptions.SIZE_LARGE2:    return 36; 
            case IGameOptions.SIZE_HUGE:      return 40; 
            case IGameOptions.SIZE_HUGE2:     return 44; 
            case IGameOptions.SIZE_MASSIVE:   return 48; 
            case IGameOptions.SIZE_MASSIVE2:  return 50; 
            case IGameOptions.SIZE_MASSIVE3:  return 52; 
            case IGameOptions.SIZE_MASSIVE4:  return 54; 
            case IGameOptions.SIZE_MASSIVE5:  return 56; 
            case IGameOptions.SIZE_INSANE:    return 58; 
            case IGameOptions.SIZE_LUDICROUS: return 58; 
            default:             return 19; 
        }
    }
}
