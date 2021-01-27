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

public class GalaxyRectangularShape extends GalaxyShape {
    private static final long serialVersionUID = 1L;
	
	float adjust_density = 1.0f; // modnar: adjust stellar density
	
    public GalaxyRectangularShape(IGameOptions options) {
        opts = options;
    }
    @Override
    public float maxScaleAdj()               { return 0.95f; }
	
    @Override
    protected int galaxyWidthLY() { 
        return (int) (Math.sqrt(4.0/3.0*opts.numberStarSystems()*adjustedSizeFactor()));
    }
    @Override
    protected int galaxyHeightLY() { 
        return (int) (Math.sqrt(3.0/4.0*opts.numberStarSystems()*adjustedSizeFactor()));
    }
    @Override
    public void setRandom(Point.Float pt) {
        pt.x = randomLocation(width, galaxyEdgeBuffer());
        pt.y = randomLocation(height, galaxyEdgeBuffer());
    }
    @Override
    public boolean valid(float x, float y) {
        float buff = galaxyEdgeBuffer();
        if (x > (width-buff))
            return false;
        if (x < buff)
            return false;
        if (y > (height-buff))
            return false;
        if (y < buff)
            return false;
        return true;
    }
    float randomLocation(float max, float buff) {
        return buff + (random() * (max-buff-buff));
    }
    @Override
    protected float sizeFactor(String size) {
        float adj = 1.0f;
        switch (opts.selectedStarDensityOption()) {
            case IGameOptions.STAR_DENSITY_LOWEST:  adj = 1.3f; break;
            case IGameOptions.STAR_DENSITY_LOWER:   adj = 1.2f; break;
            case IGameOptions.STAR_DENSITY_LOW:     adj = 1.1f; break;
            case IGameOptions.STAR_DENSITY_HIGH:    adj = 0.9f; break;
            case IGameOptions.STAR_DENSITY_HIGHER:  adj = 0.8f; break;
            case IGameOptions.STAR_DENSITY_HIGHEST: adj = 0.7f; break;
        }
        switch (opts.selectedGalaxySize()) {
            case IGameOptions.SIZE_TINY:      return adj*10; 
            case IGameOptions.SIZE_SMALL:     return adj*15; 
            case IGameOptions.SIZE_SMALL2:    return adj*17;
            case IGameOptions.SIZE_MEDIUM:    return adj*19; 
            case IGameOptions.SIZE_MEDIUM2:   return adj*20; 
            case IGameOptions.SIZE_LARGE:     return adj*21; 
            case IGameOptions.SIZE_LARGE2:    return adj*22; 
            case IGameOptions.SIZE_HUGE:      return adj*23; 
            case IGameOptions.SIZE_HUGE2:     return adj*24; 
            case IGameOptions.SIZE_MASSIVE:   return adj*25; 
            case IGameOptions.SIZE_MASSIVE2:  return adj*26; 
            case IGameOptions.SIZE_MASSIVE3:  return adj*27; 
            case IGameOptions.SIZE_MASSIVE4:  return adj*28; 
            case IGameOptions.SIZE_MASSIVE5:  return adj*29; 
            case IGameOptions.SIZE_INSANE:    return adj*32; 
            case IGameOptions.SIZE_LUDICROUS: return adj*36; 
            default:             return adj*19; 
        }
    }

}
