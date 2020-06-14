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
import java.util.concurrent.ThreadLocalRandom;
import rotp.model.game.IGameOptions;

public class GalaxySpiralArmsShape extends GalaxyShape {
    private static final long serialVersionUID = 1L;
    public GalaxySpiralArmsShape(IGameOptions options) {
        opts = options;
    }
    @Override
    public void init(int n) {
        super.init(n);
    }
    @Override
    public float maxScaleAdj()               { return 1.1f; }
    @Override
    protected int galaxyWidthLY() { 
        return (int) (Math.sqrt(1.8*opts.numberStarSystems()*adjustedSizeFactor()));
    }
    @Override
    protected int galaxyHeightLY() { 
        return (int) (Math.sqrt(1.8*opts.numberStarSystems()*adjustedSizeFactor()));
    }
    @Override
    public void setRandom(Point.Float pt) {
		
		float gW = (float) galaxyWidthLY();
		float gH = (float) galaxyHeightLY();
		
		// scale up the number of spirals with size of map
		// maybe (?) scale up the spiral swirl size with size of map (?)
		int numSpirals = (int) Math.floor(Math.sqrt(Math.sqrt(opts.numberStarSystems())));
		float numSwirls = (float) 2.0f; //Math.round(Math.sqrt(Math.sqrt(Math.sqrt(opts.numberStarSystems()))));
		int numSteps = (int) 50*numSpirals;
		
		// scale max spiral arm width (2*armRadius) with map size
		float armRadius = (float) max(1.5f, 0.03f*gW);
		
		int armSelect = ThreadLocalRandom.current().nextInt(numSpirals);
		int stepSelect = ThreadLocalRandom.current().nextInt(numSteps);
		
		float xArm = (float) (0.5f*gW + galaxyEdgeBuffer() + 0.45f*gW*stepSelect*Math.cos(numSwirls*stepSelect*Math.PI/numSteps + armSelect*2*Math.PI/numSpirals)/numSteps);
		float yArm = (float) (0.5f*gW + galaxyEdgeBuffer() + 0.45f*gW*stepSelect*Math.sin(numSwirls*stepSelect*Math.PI/numSteps + armSelect*2*Math.PI/numSpirals)/numSteps);
		
		double phi = random() * 2 * Math.PI;
		double radiusSelect = Math.sqrt(random()) * armRadius * (numSteps - stepSelect)/numSteps;
		
		pt.x = (float) (radiusSelect * Math.cos(phi) + xArm);
        pt.y = (float) (radiusSelect * Math.sin(phi) + yArm);
		
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
