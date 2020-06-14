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

public class GalaxySwirlClustersShape extends GalaxyShape {
    private static final long serialVersionUID = 1L;
    public GalaxySwirlClustersShape(IGameOptions options) {
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
        return (int) (Math.sqrt(1.5*opts.numberStarSystems()*adjustedSizeFactor()));
    }
    @Override
    protected int galaxyHeightLY() { 
        return (int) (Math.sqrt(1.5*opts.numberStarSystems()*adjustedSizeFactor()));
    }
    @Override
    public void setRandom(Point.Float pt) {
		
		float gW = (float) galaxyWidthLY();
		float gH = (float) galaxyHeightLY();
		
		// scale up the swirl size with size of map
		// scale up number of clusters with size of map
		float numSwirls = (float) Math.sqrt(Math.sqrt(opts.numberStarSystems())) - 1;
		int numClusters = (int) Math.floor(Math.sqrt(opts.numberStarSystems())*Math.log(opts.numberStarSystems())/10);
		
		int numSteps = (int) (200*numSwirls*numSwirls);
		// drop a cluster "every" clusterSteps
		// not quite since distance along swirl is not uniform with steps
		int clusterSteps = (int) Math.floor(2*numSteps / (numClusters-1));
		
		// scale cluster radius with ~numSwirls
		float clusterR = (float) (numSwirls + 4.0f) / 1.5f;
		float swirlWidth = 0.05f;
		
		int stepSelect = ThreadLocalRandom.current().nextInt(2*numSteps)+1;
		// select cluster position non-uniformally
		int clusterRandom = ThreadLocalRandom.current().nextInt(numClusters);
		int clusterSelect = (int) Math.floor(Math.sqrt(clusterRandom)*Math.sqrt(numClusters-1)*clusterSteps);
		
		// switch between populating the swirl vs cluster
		switch (ThreadLocalRandom.current().nextInt(2)) {
            case 0:
                float xSwirl = (float) (0.5f*gW + galaxyEdgeBuffer() + 0.225f*gW*stepSelect*Math.cos(numSwirls*stepSelect*Math.PI/numSteps)/numSteps);
				float ySwirl = (float) (0.5f*gW + galaxyEdgeBuffer() + 0.225f*gW*stepSelect*Math.sin(numSwirls*stepSelect*Math.PI/numSteps)/numSteps);
				
				double phiSwirl = random() * 2 * Math.PI;
				double radiusSwirl = Math.sqrt(random()) * swirlWidth;
				
				pt.x = (float) (radiusSwirl * Math.cos(phiSwirl) + xSwirl);
				pt.y = (float) (radiusSwirl * Math.sin(phiSwirl) + ySwirl);
		
                break;
            case 1:
                float xCluster = (float) (0.5f*gW + galaxyEdgeBuffer() + 0.225f*gW*clusterSelect*Math.cos(numSwirls*clusterSelect*Math.PI/numSteps)/numSteps);
				float yCluster = (float) (0.5f*gW + galaxyEdgeBuffer() + 0.225f*gW*clusterSelect*Math.sin(numSwirls*clusterSelect*Math.PI/numSteps)/numSteps);
				
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
