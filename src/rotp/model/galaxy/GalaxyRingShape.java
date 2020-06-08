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
import java.awt.geom.Ellipse2D;
import rotp.model.game.IGameOptions;

public class GalaxyRingShape extends GalaxyShape {
    private static final long serialVersionUID = 1L;
    Shape circle, hole;
    public GalaxyRingShape(IGameOptions options) {
        opts = options;
    }
    @Override
    public void init(int n) {
        super.init(n);
        int w = galaxyWidthLY();
        int h = galaxyHeightLY();
        // add galaxyEdgeBuffer() as upper left corner to prevent cutoff
        circle = new Ellipse2D.Float(galaxyEdgeBuffer(),galaxyEdgeBuffer(),w,h);
        hole =   new Ellipse2D.Float(w*3/10+galaxyEdgeBuffer(),h*3/10+galaxyEdgeBuffer(),w*4/10,h*4/10);
    }
    @Override
    public float maxScaleAdj()               { return 1.1f; }
    @Override
    protected int galaxyWidthLY() { 
        return (int) (Math.sqrt(maxStars*adjustedSizeFactor()));
    }
    @Override
    protected int galaxyHeightLY() { 
        return (int) (Math.sqrt(maxStars*adjustedSizeFactor()));
    }
    @Override
    public void setRandom(Point.Float pt) {
        pt.x = randomLocation(width, galaxyEdgeBuffer());
        pt.y = randomLocation(height, galaxyEdgeBuffer());
    }
    @Override
    public boolean valid(Point.Float pt) {
        return circle.contains(pt.x, pt.y) && !hole.contains(pt.x, pt.y);
    }
    float randomLocation(float max, float buff) {
        return buff + (random() * (max-buff-buff));
    }
    @Override
    protected float sizeFactor(String size) {
        switch (opts.selectedGalaxySize()) {
            case IGameOptions.SIZE_TINY:      return 8; 
            case IGameOptions.SIZE_SMALL:     return 10; 
            case IGameOptions.SIZE_SMALL2:    return 13;
            case IGameOptions.SIZE_MEDIUM:    return 16; 
            case IGameOptions.SIZE_MEDIUM2:   return 18; 
            case IGameOptions.SIZE_LARGE:     return 20; 
            case IGameOptions.SIZE_LARGE2:    return 22; 
            case IGameOptions.SIZE_HUGE:      return 24; 
            case IGameOptions.SIZE_HUGE2:     return 27; 
            case IGameOptions.SIZE_MASSIVE:   return 30; 
            case IGameOptions.SIZE_MASSIVE2:  return 31; 
            case IGameOptions.SIZE_MASSIVE3:  return 32; 
            case IGameOptions.SIZE_MASSIVE4:  return 33; 
            case IGameOptions.SIZE_MASSIVE5:  return 34; 
            case IGameOptions.SIZE_INSANE:    return 36; 
            case IGameOptions.SIZE_LUDICROUS: return 36; 
            default:             return 19; 
        }
    }
}
