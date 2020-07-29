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
import rotp.model.game.IGameOptions;

// modnar: custom map shape, Lorenz 2
public class GalaxyLorenz2Shape extends GalaxyShape {
    private static final long serialVersionUID = 1L;
	private double dt=0.02; // integration time interval;
	private double a=1.5, b=0.5, c=5.0; // Lorenz-2 coefficients values
    public GalaxyLorenz2Shape(IGameOptions options) {
        opts = options;
    }
    @Override
    public float maxScaleAdj()               { return 0.95f; }
    @Override
    protected int galaxyWidthLY() { 
        return (int) (Math.sqrt(2.0*4.0/3.0*opts.numberStarSystems()*adjustedSizeFactor()));
    }
    @Override
    protected int galaxyHeightLY() { 
        return (int) (Math.sqrt(2.0*3.0/4.0*opts.numberStarSystems()*adjustedSizeFactor()));
    }
    @Override
    public void setRandom(Point.Float pt) {
		
		// iterate over the Lorenz attractor function a random
		// number of steps to get a random point on the function.
		double x = 0.5; double y = 1.0; double z = 0.5; //starting point for Lorenz-2
		int maxsteps = Math.max(2000, 1 * maxStars); // scale number of iterations with stars
		int n = (int) Math.ceil(random() * maxsteps);
		for (int i = 0; i < n; i++) {
			double x1 = x + dt * y; 
			double y1 = y + dt * (-a*x - b*y + y*z); 
			double z1 = z + dt * (-c*x*y - x*x + y*y); 
			x = x1;
			y = y1;
			z = z1;
		}
		
		float xf = (float) x;
		float yf = (float) y;
		float zf = (float) z;
		
		// choose Lorenz-2 view-point with setMapOption
		if (opts.setMapOption() == 1) {
			pt.x = galaxyEdgeBuffer() + ((xf+2.1f)/4.2f)*galaxyWidthLY() + (random()-0.5f)*2.0f;
			pt.y = galaxyEdgeBuffer() + ((yf+2.9f)/5.8f)*galaxyHeightLY() + (random()-0.5f)*2.0f;
        }
		else if (opts.setMapOption() == 2) {
			pt.x = galaxyEdgeBuffer() + ((xf-yf+2.5f)/5.0f)*galaxyWidthLY() + (random()-0.5f)*2.0f;
			pt.y = galaxyEdgeBuffer() + ((zf+5.1f)/7.5f)*galaxyHeightLY() + (random()-0.5f)*2.0f;
        }
		else if (opts.setMapOption() == 3) {
			pt.x = galaxyEdgeBuffer() + ((yf+xf+3.8f)/8.0f)*galaxyWidthLY() + (random()-0.5f)*2.0f;
			pt.y = galaxyEdgeBuffer() + ((zf+5.1f)/7.5f)*galaxyHeightLY() + (random()-0.5f)*2.0f;
        }
		
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
