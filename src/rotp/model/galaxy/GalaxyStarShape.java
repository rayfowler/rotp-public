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
import java.awt.geom.AffineTransform; // modnar: TODO: perhaps add in some kind of rotation
import rotp.model.game.IGameOptions;

// modnar: custom map shape, Star
public class GalaxyStarShape extends GalaxyShape {
    private static final long serialVersionUID = 1L;
    private Path2D star;
	float adjust_density = 1.0f; // modnar: adjust stellar density
	
    public GalaxyStarShape(IGameOptions options) {
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
		
		star = new Path2D.Float();
		
		// create star shape, just with points
		star.moveTo(0.50*galaxyWidthLY(), 0.05*galaxyHeightLY());
		star.lineTo(0.38*galaxyWidthLY(), 0.39*galaxyHeightLY());
		star.lineTo(0.03*galaxyWidthLY(), 0.39*galaxyHeightLY());
		star.lineTo(0.30*galaxyWidthLY(), 0.59*galaxyHeightLY());
		star.lineTo(0.16*galaxyWidthLY(), 0.95*galaxyHeightLY());
		star.lineTo(0.50*galaxyWidthLY(), 0.73*galaxyHeightLY());
		star.lineTo(0.84*galaxyWidthLY(), 0.95*galaxyHeightLY());
		star.lineTo(0.70*galaxyWidthLY(), 0.59*galaxyHeightLY());
		star.lineTo(0.97*galaxyWidthLY(), 0.39*galaxyHeightLY());
		star.lineTo(0.62*galaxyWidthLY(), 0.39*galaxyHeightLY());
		star.lineTo(0.50*galaxyWidthLY(), 0.05*galaxyHeightLY());
		
		star.closePath();
		
		/*
		// modnar: TODO: rotate the star shape by using modulo (?)
		AffineTransform rot = new AffineTransform();
		rot.rotate(galaxyWidthLY() % 11); // need to modify to keep within galaxy bounds
		Path2D star = rot.createTransformedShape(star);
		*/
    }
    @Override
    public float maxScaleAdj()               { return 1.1f; }
    @Override
    protected int galaxyWidthLY() { 
        return (int) (adjust_density*1.3*Math.sqrt(opts.numberStarSystems()*adjustedSizeFactor()));
    }
    @Override
    protected int galaxyHeightLY() { 
        return (int) (adjust_density*1.3*Math.sqrt(opts.numberStarSystems()*adjustedSizeFactor()));
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
