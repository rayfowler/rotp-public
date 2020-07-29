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

// modnar: custom map shape, Chaos Game
public class GalaxyChaosGameShape extends GalaxyShape {
    private static final long serialVersionUID = 1L;
    public GalaxyChaosGameShape(IGameOptions options) {
        opts = options;
    }
    @Override
    public float maxScaleAdj()               { return 0.95f; }
    @Override
    protected int galaxyWidthLY() { 
        return (int) (Math.sqrt(2.0*opts.numberStarSystems()*adjustedSizeFactor()));
    }
    @Override
    protected int galaxyHeightLY() { 
        return (int) (Math.sqrt(2.0*opts.numberStarSystems()*adjustedSizeFactor()));
    }
	
	// returns the midpoint of point1 and point2
	private static Point.Float midPoint(Point.Float point1, Point.Float point2) {
        return new Point.Float((point1.x + point2.x) / 2.0f, (point1.y + point2.y) / 2.0f);
    }
	
    @Override
    public void setRandom(Point.Float pt) {
		// set Chaos game boundary dimensions
		float boxWidth = (float) galaxyWidthLY() - 8*galaxyEdgeBuffer();
		float boxHeight = (float) galaxyHeightLY() - 8*galaxyEdgeBuffer();
		
		// box vertex points
        Point.Float p1 = new Point.Float(0.0f, 0.0f);
        Point.Float p2 = new Point.Float(boxWidth, 0.0f);
		Point.Float p3 = new Point.Float(boxWidth, boxHeight);
		Point.Float p4 = new Point.Float(0.0f, boxHeight);
		
		// initial start point for chaos game, take near middle point with some variation
        Point.Float pnew = new Point.Float(boxWidth/2.0f+(random()-0.5f)*1.0f, boxHeight/2.0f+(random()-0.5f)*1.0f);
		
		// scale number of iterations with stars
		int n = (int) Math.ceil(random() * 1.5 * maxStars);
		int i = 0;
		// selection verticies
		int newVertex = 0;
		int oldVertex = 0; int oldVertex2 = 1;
		
		// iterate through chaos game, with different rules
		// choose with setMapOption
		if (opts.setMapOption() == 1) {
			// currently chosen vertex cannot neighbor the previously chosen vertex if the two previously chosen vertices are the same
			while (i < n)
			{
				newVertex = ThreadLocalRandom.current().nextInt(4);
				if (newVertex == 0 && !(oldVertex==3 && oldVertex2==3) && !(oldVertex==1 && oldVertex2==1))
				{
				pnew = midPoint(pnew, p1);
				oldVertex2 = oldVertex;
				oldVertex = 0;
				}
				else if (newVertex == 1 && !(oldVertex==0 && oldVertex2==0) && !(oldVertex==2 && oldVertex2==2))
				{
				pnew = midPoint(pnew, p2);
				oldVertex2 = oldVertex;
				oldVertex = 1;
				}
				else if (newVertex == 2 && !(oldVertex==1 && oldVertex2==1) && !(oldVertex==3 && oldVertex2==3))
				{
				pnew = midPoint(pnew, p3);
				oldVertex2 = oldVertex;
				oldVertex = 2;
				}
				else if (newVertex == 3 && !(oldVertex==2 && oldVertex2==2) && !(oldVertex==0 && oldVertex2==0))
				{
				pnew = midPoint(pnew, p4);
				oldVertex2 = oldVertex;
				oldVertex = 3;
				}
				i++;
			}
        }
		else if (opts.setMapOption() == 2) {
			// current vertex cannot be the same as the previously chosen vertex
			while (i < n)
			{
				newVertex = ThreadLocalRandom.current().nextInt(4);
				if (newVertex == 0 && oldVertex != 0)
				{
				pnew = midPoint(pnew, p1);
				oldVertex = 0;
				}
				else if (newVertex == 1 && oldVertex != 1)
				{
				pnew = midPoint(pnew, p2);
				oldVertex = 1;
				}
				else if (newVertex == 2 && oldVertex != 2)
				{
				pnew = midPoint(pnew, p3);
				oldVertex = 2;
				}
				else if (newVertex == 3 && oldVertex != 3)
				{
				pnew = midPoint(pnew, p4);
				oldVertex = 3;
				}
				i++;
			}
        }
		else if (opts.setMapOption() == 3) {
			// current vertex cannot be one place away (anti-clockwise) from the previously chosen vertex
			while (i < n)
			{
				newVertex = ThreadLocalRandom.current().nextInt(4);
				if (newVertex == 0 && oldVertex != 1)
				{
				pnew = midPoint(pnew, p1);
				oldVertex = 0;
				}
				else if (newVertex == 1 && oldVertex != 2)
				{
				pnew = midPoint(pnew, p2);
				oldVertex = 1;
				}
				else if (newVertex == 2 && oldVertex != 3)
				{
				pnew = midPoint(pnew, p3);
				oldVertex = 2;
				}
				else if (newVertex == 3 && oldVertex != 0)
				{
				pnew = midPoint(pnew, p4);
				oldVertex = 3;
				}
				i++;
			}
        }
		
		pt.x = (float) pnew.x + 4.0f*galaxyEdgeBuffer() + (random()-0.5f)*1.0f;
		pt.y = (float) pnew.y + 4.0f*galaxyEdgeBuffer() + (random()-0.5f)*1.0f;
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
