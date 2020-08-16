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
import java.awt.geom.AffineTransform;
import rotp.model.game.IGameOptions;

public class GalaxyEllipticalShape extends GalaxyShape {
    private static final long serialVersionUID = 1L;
    Shape ellipse;
	Area totalArea, ellipseArea;
	
    public GalaxyEllipticalShape(IGameOptions options) {
        opts = options;
    }
    @Override
    public float maxScaleAdj()               { return 0.8f; }
    @Override
    public void init(int n) {
        super.init(n);
		
		float gE = (float) galaxyEdgeBuffer();
		float gW = (float) galaxyWidthLY();
		float gH = (float) galaxyHeightLY();
		
		// modnar: different ellipse configurations with setMapOption
		if (opts.setMapOption() == 1) {
			// single ellipse
			// modnar: add galaxyEdgeBuffer() as upper left corner to prevent cutoff
			ellipse = new Ellipse2D.Float(gE,gE,gW,gH);
			ellipseArea = new Area(ellipse);
			totalArea = ellipseArea;
		}
		else if (opts.setMapOption() == 2) {
			// 2 ellipses (x-shape)
			ellipse = new Ellipse2D.Float(-1.1f*gW/2,-0.4f*gH/2,1.1f*gW,0.4f*gH);
			// rotate and shift ellipse
			AffineTransform rotateShape = new AffineTransform();
			AffineTransform moveShape = new AffineTransform();
			rotateShape.rotate(0.45);
			moveShape.translate(gE+gW/2,gE+gH/2);
			ellipse = rotateShape.createTransformedShape(ellipse);
			ellipse = moveShape.createTransformedShape(ellipse);
			ellipseArea = new Area(ellipse);
			totalArea = ellipseArea;
			
			ellipse = new Ellipse2D.Float(-1.1f*gW/2,-0.4f*gH/2,1.1f*gW,0.4f*gH);
			// rotate and shift ellipse
			rotateShape.rotate(-0.9);
			ellipse = rotateShape.createTransformedShape(ellipse);
			ellipse = moveShape.createTransformedShape(ellipse);
			ellipseArea = new Area(ellipse);
			totalArea.add(ellipseArea);
		}
		else if (opts.setMapOption() == 3) {
			// 4 ellipses (upright, side-by-side)
			ellipse = new Ellipse2D.Float(gE,gE,0.25f*gW,gH);
			ellipseArea = new Area(ellipse);
			totalArea = ellipseArea;
			
			ellipse = new Ellipse2D.Float(gE+0.25f*gW,gE,0.25f*gW,gH);
			ellipseArea = new Area(ellipse);
			totalArea.add(ellipseArea);
			
			ellipse = new Ellipse2D.Float(gE+0.5f*gW,gE,0.25f*gW,gH);
			ellipseArea = new Area(ellipse);
			totalArea.add(ellipseArea);
			
			ellipse = new Ellipse2D.Float(gE+0.75f*gW,gE,0.25f*gW,gH);
			ellipseArea = new Area(ellipse);
			totalArea.add(ellipseArea);
		}
		
    }
    @Override
    protected int galaxyWidthLY() { 
        return (int) (Math.sqrt(2*maxStars*adjustedSizeFactor()));
    }
    @Override
    protected int galaxyHeightLY() { 
        return (int) (Math.sqrt(0.5*maxStars*adjustedSizeFactor()));
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
            case IGameOptions.SIZE_TINY:      return 8; 
            case IGameOptions.SIZE_SMALL:     return 10; 
            case IGameOptions.SIZE_SMALL2:    return 12;
            case IGameOptions.SIZE_MEDIUM:    return 13; 
            case IGameOptions.SIZE_MEDIUM2:   return 14; 
            case IGameOptions.SIZE_LARGE:     return 16; 
            case IGameOptions.SIZE_LARGE2:    return 18; 
            case IGameOptions.SIZE_HUGE:      return 20; 
            case IGameOptions.SIZE_HUGE2:     return 22; 
            case IGameOptions.SIZE_MASSIVE:   return 24; 
            case IGameOptions.SIZE_MASSIVE2:  return 26; 
            case IGameOptions.SIZE_MASSIVE3:  return 28; 
            case IGameOptions.SIZE_MASSIVE4:  return 30; 
            case IGameOptions.SIZE_MASSIVE5:  return 32; 
            case IGameOptions.SIZE_INSANE:    return 36; 
            case IGameOptions.SIZE_LUDICROUS: return 49; 
            default:             return 19; 
        }
    }
}
