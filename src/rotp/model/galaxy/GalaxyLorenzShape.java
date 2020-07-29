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

// modnar: custom map shape, Lorenz
public class GalaxyLorenzShape extends GalaxyShape {
    private static final long serialVersionUID = 1L;
	private double dt=0.005; // integration time interval;
	private double sigma=10.0, rho=28.0, beta=8.0/3.0; // Lorenz coefficients values
    public GalaxyLorenzShape(IGameOptions options) {
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
		double x = 10.0; double y = 10.0; double z = 10.0; //starting point for Lorenz
		int maxsteps = (int) Math.max(1000, Math.ceil(1.5 * maxStars)); // scale number of iterations with stars
		int n = (int) Math.ceil(random() * maxsteps);
		for (int i = 0; i < n; i++) {
			double x1 = x + dt * sigma*(y-x); 
			double y1 = y + dt * (rho*x - y - x*z); 
			double z1 = z + dt * (x*y - beta*z); 
			x = x1;
			y = y1;
			z = z1;
		}
		
		float xf = (float) x;
		float yf = (float) y;
		float zf = (float) z;
		
		// choose lorenz view-point with setMapOption
		if (opts.setMapOption() == 1) {
			pt.x = galaxyEdgeBuffer() + ((xf+22.0f)/44.0f)*galaxyWidthLY() + (random()-0.5f)*1.0f;
			pt.y = galaxyEdgeBuffer() + ((yf+30.0f)/60.0f)*galaxyHeightLY() + (random()-0.5f)*1.0f;
        }
		else if (opts.setMapOption() == 2) {
			pt.x = galaxyEdgeBuffer() + ((xf+22.0f)/44.0f)*galaxyWidthLY() + (random()-0.5f)*1.0f;
			pt.y = galaxyEdgeBuffer() + ((zf+0.0f)/55.0f)*galaxyHeightLY() + (random()-0.5f)*1.0f;
        }
		else if (opts.setMapOption() == 3) {
			pt.x = galaxyEdgeBuffer() + ((yf+30.0f)/60.0f)*galaxyWidthLY() + (random()-0.5f)*1.0f;
			pt.y = galaxyEdgeBuffer() + ((zf+0.0f)/55.0f)*galaxyHeightLY() + (random()-0.5f)*1.0f;
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
