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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import rotp.model.game.IGameOptions;

public class GalaxyGridShape extends GalaxyShape {
    private static final long serialVersionUID = 1L;
	Path2D grid;
	Shape clusters;
	Area totalArea, gridArea, clustersArea;
    public GalaxyGridShape(IGameOptions options) {
        opts = options;
    }
	    @Override
    public void init(int n) {
        super.init(n);
		
		float gCorner = 10.0f;
		float gW = (float) galaxyWidthLY() - gCorner;
		float gH = (float) galaxyHeightLY() - gCorner;
		
		// scale up the number of grid lines with size of map
		// scale up number of clusters with size of map
		float nGrid = (float) Math.floor(Math.sqrt(Math.sqrt(maxStars)) - 1);
		float nClusters = (float) Math.floor(Math.sqrt(maxStars)*Math.log(maxStars)/12);
		
		// scale cluster radius with ~nGrid
		float clusterR = (float) (nGrid + 9.0f) / 2.0f;
		
		clusters = new Ellipse2D.Float(gCorner, gCorner, 1.0f, 1.0f); // dummy cluster
		//clusters = new Ellipse2D.Float(gCorner - clusterR, gCorner - clusterR, 2.0f*clusterR, 2.0f*clusterR);
		clustersArea = new Area(clusters);
		totalArea = new Area(clusters);
		
		// grid out both horizontal and vertical
		for (int i = 0; i <= nGrid; i++)
		{
			// vertical
			grid = new Path2D.Float();
			grid.moveTo((i/nGrid)*gW + gCorner-0.75f, gCorner);
			grid.lineTo((i/nGrid)*gW + gCorner+0.75f, gCorner);
			grid.lineTo((i/nGrid)*gW + gCorner+0.75f, gH + gCorner);
			grid.lineTo((i/nGrid)*gW + gCorner-0.75f, gH + gCorner);
			grid.lineTo((i/nGrid)*gW + gCorner-0.75f, gCorner);
			grid.closePath();
			
			// add grid area to total area
			gridArea = new Area(grid);
			totalArea.add(gridArea);
			
			// horizontal
			grid = new Path2D.Float();
			grid.moveTo(gCorner, (i/nGrid)*gH-0.75f + gCorner);
			grid.lineTo(gCorner, (i/nGrid)*gH+0.75f + gCorner);
			grid.lineTo(gW + gCorner, (i/nGrid)*gH+0.75f + gCorner);
			grid.lineTo(gW + gCorner, (i/nGrid)*gH-0.75f + gCorner);
			grid.lineTo(gCorner, (i/nGrid)*gH-0.75f + gCorner);
			grid.closePath();
			
			// add grid area to total area
			gridArea = new Area(grid);
			totalArea.add(gridArea);
		}
		
		
		// randomly place clusters at intersections
		// but use map size as seed to ensure same sequence for same map size
		// use shuffle list to ensure unique draws
		ArrayList<Integer> clusterList = new ArrayList<Integer>();
		for(int i = 0; i < (nGrid+1)*(nGrid+1); i++){
			clusterList.add(i);
		}
		Collections.shuffle(clusterList, new Random(maxStars));
		
		for (int i = 0; i < nClusters; i++)
		{
			int clusterPos = clusterList.get(i);
			int clusterX = (int) (clusterPos % (nGrid+1));
			int clusterY = (int) Math.floor(clusterPos / (nGrid+1));
			
			clusters = new Ellipse2D.Float((clusterX/nGrid)*gW + gCorner - clusterR, (clusterY/nGrid)*gH + gCorner - clusterR, 2.0f*clusterR, 2.0f*clusterR);
			clustersArea = new Area(clusters);
			totalArea.add(clustersArea);
		}
		
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
    public void setRandom(Point.Float pt) {
        pt.x = randomLocation(width, galaxyEdgeBuffer());
        pt.y = randomLocation(height, galaxyEdgeBuffer());
    }
    @Override
    public boolean valid(Point.Float pt) {
        return totalArea.contains(pt.x, pt.y);
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
