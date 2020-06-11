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
import java.awt.geom.Path2D;
import rotp.model.game.IGameOptions;

public class GalaxySwirlClustersShape extends GalaxyShape {
    private static final long serialVersionUID = 1L;
    Path2D swirl;
	Shape clusters;
	Area clusterSwirl, swirlArea, clustersArea;
    public GalaxySwirlClustersShape(IGameOptions options) {
        opts = options;
    }
    @Override
    public void init(int n) {
        super.init(n);
		
		float gW = (float) galaxyWidthLY();
		float gH = (float) galaxyHeightLY();
		
		// create swirl shape, with width independent of map size, and extends out towards the boundary
        swirl = new Path2D.Float();
		swirl.moveTo(0.5f*gW, 0.5f*gH);
		
		// scale up the swirl size with size of map
		// scale up number of clusters with size of map
		float numSwirls = (float) Math.sqrt(Math.sqrt(maxStars)) - 1;
		float numClusters = (float) Math.floor(Math.sqrt(maxStars)*Math.log(maxStars)/10 - 1);
		int numSteps = (int) Math.round(2*Math.sqrt(maxStars));
		// drop a cluster every clusterSteps, "skip" first numSwirls clusters  for being too close to center
		int clusterSteps = (int) Math.round(2*numSteps / (numSwirls+numClusters));
		int ncount = (int) -Math.floor(numSwirls*clusterSteps) + 2;
		// scale cluster radius with ~numSwirls
		float clusterR = (float) (numSwirls + 6.0f) / 2.0f;
		
		clusters = new Ellipse2D.Float(0.5f*gW-clusterR, 0.5f*gH-clusterR, 2*clusterR, 2*clusterR);
		clustersArea = new Area(clusters);
		clusterSwirl = new Area(clusters);
		
		// swirl out from center, slightly larger
		for (int i = 0; i < 2*numSteps; i++)
		{
			// offsetSwirl tries to maintain ~constant swirl width
			float offsetSwirl = 6.0f / (1 + 17.0f*i/(2.0f*numSteps));
			float x = (float) (0.5f*gW + (0.225f*gW+offsetSwirl)*i*Math.cos(numSwirls*i*Math.PI/numSteps)/numSteps);
			float y = (float) (0.5f*gH + (0.225f*gH+offsetSwirl)*i*Math.sin(numSwirls*i*Math.PI/numSteps)/numSteps);
			swirl.lineTo(x, y);
			
			// make new clusters at regular intervals
			// add area to total area
			if (ncount == clusterSteps){
				clusters = new Ellipse2D.Float(x-clusterR, y-clusterR, 2*clusterR, 2*clusterR);
				clustersArea = new Area(clusters);
				clusterSwirl.add(clustersArea);
				ncount = 0;
			}
			ncount++;
		}
		// swirl into center, slightly smaller
		for (int i = 2*numSteps; i > 1; i--)
		{
			// offsetSwirl tries to maintain ~constant swirl width
			float offsetSwirl = 6.0f / (1 + 17.0f*i/(2.0f*numSteps));
			float x = (float) (0.5f*gW + (0.225f*gW-offsetSwirl)*i*Math.cos(numSwirls*i*Math.PI/numSteps)/numSteps);
			float y = (float) (0.5f*gH + (0.225f*gH-offsetSwirl)*i*Math.sin(numSwirls*i*Math.PI/numSteps)/numSteps);
			swirl.lineTo(x, y);
		}
		swirl.closePath();
		
		// add swirl area to total area
		swirlArea = new Area(swirl);
		clusterSwirl.add(swirlArea);
    }
    @Override
    public float maxScaleAdj()               { return 1.1f; }
    @Override
    protected int galaxyWidthLY() { 
        return (int) (Math.sqrt(1.5*maxStars*adjustedSizeFactor()));
    }
    @Override
    protected int galaxyHeightLY() { 
        return (int) (Math.sqrt(1.5*maxStars*adjustedSizeFactor()));
    }
    @Override
    public void setRandom(Point.Float pt) {
        pt.x = randomLocation(width, galaxyEdgeBuffer());
        pt.y = randomLocation(height, galaxyEdgeBuffer());
    }
    @Override
    public boolean valid(Point.Float pt) {
        return clusterSwirl.contains(pt.x, pt.y);
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
