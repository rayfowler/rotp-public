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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import rotp.model.game.IGameOptions;

public class GalaxyGridShape extends GalaxyShape {
    private static final long serialVersionUID = 1L;
    public GalaxyGridShape(IGameOptions options) {
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
        return (int) (Math.sqrt(1.5*4.0/3.0*opts.numberStarSystems()*adjustedSizeFactor()));
    }
    @Override
    protected int galaxyHeightLY() { 
        return (int) (Math.sqrt(1.5*3.0/4.0*opts.numberStarSystems()*adjustedSizeFactor()));
    }
    @Override
    public void setRandom(Point.Float pt) {
		
		// scale up the number of grid lines with size of map
		// scale up number of clusters with size of map
		float nGrid = (float) Math.floor(Math.sqrt(Math.sqrt(opts.numberStarSystems())) - 1);
		float nClusters = (float) min((nGrid+1)*(nGrid+1)-1, (float) Math.floor(Math.sqrt(opts.numberStarSystems())/1.5));
		
		// scale cluster radius with ~nGrid
		float clusterR = (float) (nGrid + 5.0f) / 2.0f;
		
		float gW = (float) galaxyWidthLY() - 2.0f*clusterR - 2.0f*galaxyEdgeBuffer();
		float gH = (float) galaxyHeightLY() - 2.0f*clusterR - 2.0f*galaxyEdgeBuffer();
		
		// scale the resolution of the grid with map dimensions and number of grids
		int numSteps = (int) (10*(gW+gH)*(nGrid+1));
		int horizontalSteps = (int) (10*gW*(nGrid+1));
		
		// randomly assign clusters at intersections
		// but use map size as seed to ensure same sequence for same map size
		// use shuffle list to ensure unique draws
		ArrayList<Integer> clusterList = new ArrayList<Integer>();
		for(int i = 0; i < (nGrid+1)*(nGrid+1); i++){
			clusterList.add(i);
		}
		Collections.shuffle(clusterList, new Random(opts.numberStarSystems()));
		
		
		// switch between populating the grid vs cluster
		switch (ThreadLocalRandom.current().nextInt(2)) {
            case 0:
				
				int stepSelect = (int) Math.floor(random()*numSteps);
				
				// horizontal grids
				if (stepSelect < horizontalSteps) { 
				
					int gridRow = (int) Math.floor(stepSelect/(10*gW));
					
					pt.x = (float) (clusterR + galaxyEdgeBuffer() + gW*(stepSelect-gridRow*(10*gW))/(10*gW));
					pt.y = (float) (clusterR + galaxyEdgeBuffer() + gH*(gridRow/nGrid));
					
					break;
				}
				
				// vertical grids
				else {
				
					int gridColumn = (int) Math.floor((stepSelect - horizontalSteps)/(10*gH));
					
					pt.x = (float) (clusterR + galaxyEdgeBuffer() + gW*(gridColumn/nGrid));
					pt.y = (float) (clusterR + galaxyEdgeBuffer() + gH*(stepSelect-horizontalSteps-gridColumn*(10*gH))/(10*gH));
					
					break;
				}
            case 1:
				
				int clusterSelect = (int) Math.floor(random()*nClusters);
				
				int clusterPos = clusterList.get(clusterSelect);
                int clusterX = (int) (clusterPos % (nGrid+1));
				int clusterY = (int) Math.floor(clusterPos / (nGrid+1));
				
				float xCluster = (float) ((clusterX/nGrid)*gW + clusterR + galaxyEdgeBuffer());
				float yCluster = (float) ((clusterY/nGrid)*gH + clusterR + galaxyEdgeBuffer());
				
				double phiCluster = random() * 2 * Math.PI;
				double radiusSelect = Math.sqrt(random()) * clusterR;
				
				pt.x = (float) (radiusSelect * Math.cos(phiCluster) + xCluster);
				pt.y = (float) (radiusSelect * Math.sin(phiCluster) + yCluster);
				
                break;
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
