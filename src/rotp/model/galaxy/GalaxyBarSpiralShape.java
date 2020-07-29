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
import java.awt.geom.Arc2D;
import java.awt.geom.AffineTransform;
import rotp.model.game.IGameOptions;

// modnar: custom map shape, Bar Spiral
public class GalaxyBarSpiralShape extends GalaxyShape {
    private static final long serialVersionUID = 1L;
	
    Shape barcircle, outArm, inArm;
	Area totalArea, barArea, barcircleArea, armsArea;
	float adjust_density = 1.0f;
	
    public GalaxyBarSpiralShape(IGameOptions options) {
        opts = options;
    }
    @Override
    public void init(int n) {
        super.init(n);
		float buff = galaxyEdgeBuffer();
		
		// choose different stellar densities with setMapOption
		if (opts.setMapOption() == 1) {
			adjust_density = 1.0f;
		}
		else if (opts.setMapOption() == 2) {
			adjust_density = 1.5f;
		}
		else if (opts.setMapOption() == 3) {
			adjust_density = 2.0f;
		}
		
		// create central bar/lob region
        barcircle = new Ellipse2D.Float(-0.325f*galaxyWidthLY(), -0.15f*galaxyHeightLY(), 0.65f*galaxyWidthLY(), 0.3f*galaxyHeightLY());
		
		// rotate and shift into center of map
		AffineTransform rotateShape = new AffineTransform();
		AffineTransform moveShape = new AffineTransform();
		rotateShape.rotate(0.85);
		moveShape.translate(buff+0.5f*galaxyWidthLY(), buff+0.5f*galaxyHeightLY());
		barcircle = rotateShape.createTransformedShape(barcircle);
		barcircle = moveShape.createTransformedShape(barcircle);
		
		barcircleArea = new Area(barcircle);
		totalArea = new Area(barcircle);
		
		// create left spiral arm
		outArm = new Arc2D.Float(buff+0.05f*galaxyWidthLY(), buff+0.075f*galaxyHeightLY(), 1.1f*galaxyWidthLY(), 0.9f*galaxyHeightLY(), 120, 200, Arc2D.CHORD);
		inArm = new Arc2D.Float(buff+0.17f*galaxyWidthLY(), buff+0.175f*galaxyHeightLY(), 1.0f*galaxyWidthLY(), 0.75f*galaxyHeightLY(), 120, 200, Arc2D.CHORD);
		
		armsArea = new Area(outArm);
		armsArea.subtract(new Area(inArm));
		totalArea.add(armsArea);
		
		// create right spiral arm
		outArm = new Arc2D.Float(buff-0.15f*galaxyWidthLY(), buff+0.025f*galaxyHeightLY(), 1.1f*galaxyWidthLY(), 0.9f*galaxyHeightLY(), 300, 200, Arc2D.CHORD);
		inArm = new Arc2D.Float(buff-0.17f*galaxyWidthLY(), buff+0.075f*galaxyHeightLY(), 1.0f*galaxyWidthLY(), 0.75f*galaxyHeightLY(), 300, 200, Arc2D.CHORD);
		
		armsArea = new Area(outArm);
		armsArea.subtract(new Area(inArm));
		totalArea.add(armsArea);
		
    }
    @Override
    public float maxScaleAdj()               { return 1.1f; }
	
    @Override
    protected int galaxyWidthLY() { 
        return (int) (Math.sqrt(adjust_density*1.5*4.0/3.0*maxStars*adjustedSizeFactor()));
    }
    @Override
    protected int galaxyHeightLY() { 
        return (int) (Math.sqrt(adjust_density*1.5*3.0/4.0*maxStars*adjustedSizeFactor()));
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
            case IGameOptions.SIZE_TINY:      return 12; 
            case IGameOptions.SIZE_SMALL:     return 12; 
            case IGameOptions.SIZE_SMALL2:    return 13;
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
            case IGameOptions.SIZE_LUDICROUS: return 40; 
            default:             return 19; 
        }
    }
}
