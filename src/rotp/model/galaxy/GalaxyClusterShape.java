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

public class GalaxyClusterShape extends GalaxyShape {
    private static final long serialVersionUID = 1L;
	private Point.Float cc1, cc2, cc3, cc4, cc5, cc6;
	
    public GalaxyClusterShape(IGameOptions options) {
        opts = options;
    }
    @Override
    public float maxScaleAdj()               { return 0.95f; }
    @Override
    protected int galaxyWidthLY() { 
        return (int) (Math.sqrt(1.5*4.0/3.0*opts.numberStarSystems()*adjustedSizeFactor()));
    }
    @Override
    protected int galaxyHeightLY() { 
        return (int) (Math.sqrt(1.5*3.0/4.0*opts.numberStarSystems()*adjustedSizeFactor()));
    }
	@Override
    public void init(int n) {
        super.init(n);
		cc1 = new Point.Float();
		cc2 = new Point.Float();
		cc3 = new Point.Float();
		cc4 = new Point.Float();
		cc5 = new Point.Float();
		cc6 = new Point.Float();
		
		//define cluster centers based on modulo galaxyWidthLY/galaxyHeightLY
		cc1.x = (float) (galaxyWidthLY() % 7)/7.0f*0.125f*galaxyWidthLY() + 0.2f*galaxyWidthLY() + galaxyEdgeBuffer();
        cc1.y = (float) (galaxyHeightLY() % 7)/7.0f*0.167f*galaxyHeightLY() + 0.25f*galaxyHeightLY() + galaxyEdgeBuffer();
		cc2.x = (float) (galaxyHeightLY() % 7)/7.0f*0.125f*galaxyHeightLY() + 0.4f*galaxyWidthLY() + galaxyEdgeBuffer();
        cc2.y = (float) (galaxyWidthLY() % 7)/7.0f*0.167f*galaxyWidthLY() + 0.6f*galaxyHeightLY() + galaxyEdgeBuffer();
		
		cc3.x = (float) (galaxyWidthLY() % 7)/7.0f*0.125f*galaxyWidthLY() + 0.5f*galaxyWidthLY() + galaxyEdgeBuffer();
        cc3.y = (float) (galaxyHeightLY() % 7)/7.0f*0.167f*galaxyHeightLY() + 0.2f*galaxyHeightLY() + galaxyEdgeBuffer();
		cc4.x = (float) (galaxyHeightLY() % 7)/7.0f*0.125f*galaxyHeightLY() + 0.7f*galaxyWidthLY() + galaxyEdgeBuffer();
        cc4.y = (float) (galaxyWidthLY() % 7)/7.0f*0.167f*galaxyWidthLY() + 0.55f*galaxyHeightLY() + galaxyEdgeBuffer();
		
		cc5.x = (float) (galaxyWidthLY() % 7)/7.0f*0.125f*galaxyWidthLY() + 0.7f*galaxyWidthLY() + galaxyEdgeBuffer();
        cc5.y = (float) (galaxyHeightLY() % 7)/7.0f*0.167f*galaxyHeightLY() + 0.35f*galaxyHeightLY() + galaxyEdgeBuffer();
		cc6.x = (float) (galaxyHeightLY() % 7)/7.0f*0.125f*galaxyHeightLY() + 0.2f*galaxyWidthLY() + galaxyEdgeBuffer();
        cc6.y = (float) (galaxyWidthLY() % 7)/7.0f*0.167f*galaxyWidthLY() + 0.45f*galaxyHeightLY() + galaxyEdgeBuffer();
    }
    @Override
	public void setRandom(Point.Float pt) {
        pt.x = randomLocation(width, galaxyEdgeBuffer());
        pt.y = randomLocation(height, galaxyEdgeBuffer());
    }
    @Override
    public boolean valid(Point.Float pt) {
		// calculate the distances from the random point to ClusterCenters
		float dcc1 = distance(pt.x,pt.y,cc1.x,cc1.y);
		float dcc2 = distance(pt.x,pt.y,cc2.x,cc2.y);
		float dcc3 = distance(pt.x,pt.y,cc3.x,cc3.y);
		float dcc4 = distance(pt.x,pt.y,cc4.x,cc4.y);
		float dcc5 = distance(pt.x,pt.y,cc5.x,cc5.y);
		float dcc6 = distance(pt.x,pt.y,cc6.x,cc6.y);
		
		// cRadius defines approx. cluster radius
		// using two for variety
		float cRadius1 = 0.22f*galaxyHeightLY();
		float cRadius2 = 0.15f*galaxyHeightLY();
		
		float min_dcc = min(dcc1/cRadius1, dcc2/cRadius1, dcc3/cRadius1, dcc4/cRadius1, dcc5/cRadius2, dcc6/cRadius2);
		
		// accept based on distance vs radius, more likely if closer to center, less likely further away		
        return (random() >= min_dcc);
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
