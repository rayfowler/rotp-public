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
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import rotp.model.game.IGameOptions;

// modnar: custom map shape, Void
public class GalaxyVoidShape extends GalaxyShape {
    private static final long serialVersionUID = 1L;
	Shape block, circle;
	Area totalArea, blockArea, circleArea;
	
    public GalaxyVoidShape(IGameOptions options) {
        opts = options;
    }
    @Override
    public float maxScaleAdj()               { return 0.95f; }
	
	public void init(int n) {
        super.init(n);
		
		float gE = (float) galaxyEdgeBuffer();
		float gW = (float) galaxyWidthLY();
		float gH = (float) galaxyHeightLY();
		
		block = new Rectangle2D.Float(gE, gE, gW, gH);
		blockArea = new Area(block);
		totalArea = blockArea;
		
		// modnar: choose void configurations with setMapOption
		if (opts.setMapOption() == 1) {
			// single large central void
			circle = new Ellipse2D.Float(gE+0.5f*gW-0.44f*gH, gE+0.06f*gH, 0.88f*gH, 0.88f*gH);
			circleArea = new Area(circle);
			totalArea.subtract(circleArea);
		}
		else if (opts.setMapOption() == 2) {
			// two diagonal voids
			circle = new Ellipse2D.Float(gE+0.05f*gW, gE+0.05f*gH, 0.45f*gW, 0.45f*gW);
			circleArea = new Area(circle);
			totalArea.subtract(circleArea);
			
			circle = new Ellipse2D.Float(gE+0.5f*gW, gE+0.95f*gH-0.45f*gW, 0.45f*gW, 0.45f*gW);
			circleArea = new Area(circle);
			totalArea.subtract(circleArea);
		}
		else if (opts.setMapOption() == 3) {
			// five separated voids
			circle = new Ellipse2D.Float(gE+0.26f*gW, gE+0.5f*gH-0.24f*gW, 0.48f*gW, 0.48f*gW);
			circleArea = new Area(circle);
			totalArea.subtract(circleArea);
			
			circle = new Ellipse2D.Float(gE+0.05f*gW, gE+0.067f*gH, 0.3f*gH, 0.3f*gH);
			circleArea = new Area(circle);
			totalArea.subtract(circleArea);
			
			circle = new Ellipse2D.Float(gE+0.05f*gW, gE+0.633f*gH, 0.3f*gH, 0.3f*gH);
			circleArea = new Area(circle);
			totalArea.subtract(circleArea);
			
			circle = new Ellipse2D.Float(gE+0.95f*gW-0.3f*gH, gE+0.067f*gH, 0.3f*gH, 0.3f*gH);
			circleArea = new Area(circle);
			totalArea.subtract(circleArea);
			
			circle = new Ellipse2D.Float(gE+0.95f*gW-0.3f*gH, gE+0.633f*gH, 0.3f*gH, 0.3f*gH);
			circleArea = new Area(circle);
			totalArea.subtract(circleArea);
		}
    }
	
    @Override
    protected int galaxyWidthLY() { 
        return (int) (Math.sqrt(0.8*4.0/3.0*opts.numberStarSystems()*adjustedSizeFactor()));
    }
    @Override
    protected int galaxyHeightLY() { 
        return (int) (Math.sqrt(0.8*3.0/4.0*opts.numberStarSystems()*adjustedSizeFactor()));
    }
    @Override
    public void setRandom(Point.Float pt) {
        pt.x = randomLocation(width, galaxyEdgeBuffer());
        pt.y = randomLocation(height, galaxyEdgeBuffer());
    }
    @Override
    public boolean valid(Point.Float pt) {
		return totalArea.contains(pt.x, pt.y);
    }
    float randomLocation(float max, float buff) {
        return buff + (random() * (max-buff-buff));
    }
    @Override
    protected float sizeFactor(String size) {
        switch (opts.selectedGalaxySize()) {
            case IGameOptions.SIZE_TINY:      return 10; 
            case IGameOptions.SIZE_SMALL:     return 15; 
            case IGameOptions.SIZE_SMALL2:    return 17;
            case IGameOptions.SIZE_MEDIUM:    return 19; 
            case IGameOptions.SIZE_MEDIUM2:   return 20; 
            case IGameOptions.SIZE_LARGE:     return 21; 
            case IGameOptions.SIZE_LARGE2:    return 22; 
            case IGameOptions.SIZE_HUGE:      return 23; 
            case IGameOptions.SIZE_HUGE2:     return 24; 
            case IGameOptions.SIZE_MASSIVE:   return 25; 
            case IGameOptions.SIZE_MASSIVE2:  return 26; 
            case IGameOptions.SIZE_MASSIVE3:  return 27; 
            case IGameOptions.SIZE_MASSIVE4:  return 28; 
            case IGameOptions.SIZE_MASSIVE5:  return 29; 
            case IGameOptions.SIZE_INSANE:    return 32; 
            case IGameOptions.SIZE_LUDICROUS: return 36; 
            default:             return 19; 
        }
    }

}
