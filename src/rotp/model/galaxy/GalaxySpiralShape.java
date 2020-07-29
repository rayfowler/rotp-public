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
import rotp.model.game.IGameOptions;

public class GalaxySpiralShape extends GalaxyShape {
    private static final long serialVersionUID = 1L;
    static final float numArms = 5;
    static final float armOffsetMax = 0.7f;
    static final float rotationFactor = 3;
    static final float armSeparationDistance = 2 * (float)Math.PI / numArms;
    Shape circle;
	float adjust_density = 1.0f; // modnar: adjust stellar density
	
    float randomX = 0;
    float randomY = 0;
    public GalaxySpiralShape(IGameOptions options) {
        opts = options;
    }
    @Override
    public void init(int n) {
        super.init(n);
		
		// modnar: choose different stellar densities (map areas) with setMapOption
		if (opts.setMapOption() == 1) {
			adjust_density = 1.0f;
		}
		else if (opts.setMapOption() == 2) {
			adjust_density = 1.5f;
		}
		else if (opts.setMapOption() == 3) {
			adjust_density = 2.0f;
		}
		
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
