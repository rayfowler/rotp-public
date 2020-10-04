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
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import rotp.model.game.IGameOptions;

// modnar: custom map shape, Bullseye
public class GalaxyBullseyeShape extends GalaxyShape {
    private static final long serialVersionUID = 1L;
    Shape circle, square, arc;
	Area totalArea, circleArea, squareArea, arcArea;
	
    public GalaxyBullseyeShape(IGameOptions options) {
        opts = options;
    }
    @Override
    public void init(int n) {
        super.init(n);
		
		float gE = (float) galaxyEdgeBuffer();
		float gW = (float) galaxyWidthLY();
		float gH = (float) galaxyHeightLY();
		
		// modnar: different bullseye/target configurations with setMapOption
		if (opts.setMapOption() == 1) { // standard dart board, exclusiveOr
			// number of arc sections in the dart board
			int nArcs = (int) Math.min(20, Math.ceil(Math.sqrt(opts.numberStarSystems())/4.0f));
			
			// double ring
			circle = new Ellipse2D.Float(gE,gE,gH,gH);
			circleArea = new Area(circle);
			totalArea = circleArea;
			circle = new Ellipse2D.Float(gE+0.0235f*gH, gE+0.0235f*gH, 0.953f*gH, 0.953f*gH);
			circleArea = new Area(circle);
			totalArea.subtract(circleArea);
			
			// treble ring
			circle = new Ellipse2D.Float(gE+0.1625f*gH, gE+0.1625f*gH, 0.675f*gH, 0.675f*gH);
			circleArea = new Area(circle);
			totalArea.add(circleArea);
			circle = new Ellipse2D.Float(gE+0.1855f*gH, gE+0.1855f*gH, 0.629f*gH, 0.629f*gH);
			circleArea = new Area(circle);
			totalArea.subtract(circleArea);
			
			// arc segments/sections
			for ( int i = 0; i < nArcs; i++ ){
				float arcStart = (float) (i*360.0f/nArcs + 90.0f/nArcs);
				float arcExtent = (float) (180.0f/nArcs);
				arc = new Arc2D.Float(gE, gE, gH, gH, arcStart, arcExtent, Arc2D.PIE);
				arcArea = new Area(arc);
				totalArea.exclusiveOr(arcArea);
			}
			
			// central bullseye
			circle = new Ellipse2D.Float(gE+0.45325f*gH, gE+0.45325f*gH, 0.0935f*gH, 0.0935f*gH);
			circleArea = new Area(circle);
			totalArea.add(circleArea);
			circle = new Ellipse2D.Float(gE+0.4813f*gH, gE+0.4813f*gH, 0.0374f*gH, 0.0374f*gH);
			circleArea = new Area(circle);
			totalArea.subtract(circleArea);
			
		}
		else if (opts.setMapOption() == 2) { // concentric ring target
			// number of rings/halos
			int nRings = (int) Math.min(200, Math.floor(Math.sqrt(opts.numberStarSystems())/2.5f));
			
			// width of each ring/halo, in ly
			float rWidth = 4.0f;
			
			// create each circular ring/halo
			circle = new Ellipse2D.Float(gE,gE,gH,gH);
			circleArea = new Area(circle);
			totalArea = circleArea;
			circle = new Ellipse2D.Float(gE+rWidth/2, gE+rWidth/2, gH-rWidth, gH-rWidth);
			circleArea = new Area(circle);
			totalArea.subtract(circleArea);
			
			for ( int i = 1; i < nRings; i++ ){
				float ringD = (float) (1.0f-1.0f*i/nRings);
				float ringPos = (float) 0.5f*(1.0f-ringD);
				
				circle = new Ellipse2D.Float(gE+ringPos*gH, gE+ringPos*gH, ringD*gH, ringD*gH);
				circleArea = new Area(circle);
				totalArea.add(circleArea);
				circle = new Ellipse2D.Float(gE+ringPos*gH+rWidth/2, gE+ringPos*gH+rWidth/2, ringD*gH-rWidth, ringD*gH-rWidth);
				circleArea = new Area(circle);
				totalArea.subtract(circleArea);
			}
			
			// central bullseye
			circle = new Ellipse2D.Float(gE+0.5f*gH*(1.0f-0.5f/nRings), gE+0.5f*gH*(1.0f-0.5f/nRings), 0.5f*gH/nRings, 0.5f*gH/nRings);
			circleArea = new Area(circle);
			totalArea.add(circleArea);
			
		}
		else if (opts.setMapOption() == 3) { // concentric square target
			// number of rings/halos
			int nRings = (int) Math.min(200, Math.floor(Math.sqrt(opts.numberStarSystems())/3));
			
			// width of each ring/halo, in ly
			float rWidth = 3.0f;
			
			// create each square ring/halo
			square = new Rectangle2D.Float(gE,gE,gH,gH);
			squareArea = new Area(square);
			totalArea = squareArea;
			square = new Rectangle2D.Float(gE+rWidth/2, gE+rWidth/2, gH-rWidth, gH-rWidth);
			squareArea = new Area(square);
			totalArea.subtract(squareArea);
			
			for ( int i = 1; i < nRings; i++ ){
				float ringD = (float) (1.0f-1.0f*i/nRings);
				float ringPos = (float) 0.5f*(1.0f-ringD);
				
				square = new Rectangle2D.Float(gE+ringPos*gH, gE+ringPos*gH, ringD*gH, ringD*gH);
				squareArea = new Area(square);
				totalArea.add(squareArea);
				square = new Rectangle2D.Float(gE+ringPos*gH+rWidth/2, gE+ringPos*gH+rWidth/2, ringD*gH-rWidth, ringD*gH-rWidth);
				squareArea = new Area(square);
				totalArea.subtract(squareArea);
			}
			
			// central bullseye
			square = new Rectangle2D.Float(gE+0.5f*gH*(1.0f-0.5f/nRings), gE+0.5f*gH*(1.0f-0.5f/nRings), 0.5f*gH/nRings, 0.5f*gH/nRings);
			squareArea = new Area(square);
			totalArea.add(squareArea);

		}
		
    }
    @Override
    public float maxScaleAdj()               { return 1.1f; }
    @Override
    protected int galaxyWidthLY() { 
        return (int) (Math.sqrt(opts.numberStarSystems()*adjustedSizeFactor()));
    }
    @Override
    protected int galaxyHeightLY() { 
        return (int) (Math.sqrt(opts.numberStarSystems()*adjustedSizeFactor()));
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
