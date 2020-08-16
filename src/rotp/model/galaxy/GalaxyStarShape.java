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
import java.awt.geom.Path2D;
import java.awt.geom.AffineTransform;
import rotp.model.game.IGameOptions;

// modnar: custom map shape, Star
public class GalaxyStarShape extends GalaxyShape {
    private static final long serialVersionUID = 1L;
    private Path2D star;
	int numPoints = 5;
	
    public GalaxyStarShape(IGameOptions options) {
        opts = options;
    }
    @Override
    public void init(int n) {
        super.init(n);
		
		float gE = (float) galaxyEdgeBuffer();
		float gW = (float) galaxyWidthLY();
		float gH = (float) galaxyHeightLY();
		
		float innerRadius = 0.2f*gH;
		
		// modnar: choose different number of star points with setMapOption
		if (opts.setMapOption() == 1) {
			numPoints = 5;
			innerRadius = 0.2f*gH;
		}
		else if (opts.setMapOption() == 2) {
			numPoints = 3;
			innerRadius = 0.12f*gH;
		}
		else if (opts.setMapOption() == 3) {
			numPoints = 8;
			innerRadius = 0.18f*gH;
		}
		
		star = new Path2D.Float();
		
		// create star shape, just with points/path
		float deltaAngle = (float) Math.PI/numPoints;
		for (int i = 0; i < 2*numPoints; i++)
        {
			// offsetAngle, spins star with number of opponents
			float offsetAngle = (float) (opts.selectedNumberOpponents()*Math.PI/15);
            float pathX = (float) Math.cos(offsetAngle + i*deltaAngle);
            float pathY = (float) Math.sin(offsetAngle + i*deltaAngle);
			
            if ((i % 2) == 0)
            {
                pathX *= 0.5f*gH; // outerRadius
                pathY *= 0.5f*gH; // outerRadius
            }
            else
            {
                pathX *= innerRadius;
                pathY *= innerRadius;
            }
            if (i == 0)
            {
                star.moveTo(gE + 0.5f*gW + pathX, gE + 0.5f*gH + pathY); // initial start
            }
            else
            {
                star.lineTo(gE + 0.5f*gW + pathX, gE + 0.5f*gH + pathY);
            }
        }
        star.closePath();
    }
    @Override
    public float maxScaleAdj()               { return 1.1f; }
    @Override
    protected int galaxyWidthLY() { 
        return (int) (1.3*Math.sqrt(opts.numberStarSystems()*adjustedSizeFactor()));
    }
    @Override
    protected int galaxyHeightLY() { 
        return (int) (1.3*Math.sqrt(opts.numberStarSystems()*adjustedSizeFactor()));
    }
    @Override
    public void setRandom(Point.Float pt) {
        pt.x = randomLocation(width, galaxyEdgeBuffer());
        pt.y = randomLocation(height, galaxyEdgeBuffer());
    }
    @Override
    public boolean valid(Point.Float pt) {
        return star.contains(pt.x, pt.y);
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
