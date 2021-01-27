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
package rotp.ui.sprites;

import java.awt.Graphics2D;
import rotp.model.galaxy.Ship;
import rotp.model.galaxy.StarSystem;
import rotp.ui.main.GalaxyMapPanel;

public class ShipRelocationSprite extends MapSprite {
    StarSystem from;
    StarSystem clickedDest;
    StarSystem hoveringDest;
    FlightPathSprite pathSprite;
    public StarSystem from()                  { return from; }
    public boolean isActive()                 { return rallySystem() != null; }
    public ShipRelocationSprite(StarSystem tr) {
        init(tr);
    }
    private void init(StarSystem tr) {
        source(tr);
        from = tr;
    }
    public void clear() {
        pathSprite = null;
        clickedDest = null;
        hoveringDest = null;
    }
    public void stop() {
        pathSprite = null;
        clickedDest = null;
        hoveringDest = null;
    }
    public boolean hasSelectedDestination()   { return clickedDest != null; }
    public void clickedDest(StarSystem sv)    { clickedDest = sv; }
    public void hoveringDest(StarSystem sv)   { hoveringDest = sv; }
    public StarSystem rallySystem()           { return player().sv.rallySystem(from.id); }
    public StarSystem homeSystemView()        { return (StarSystem) source(); }
    public boolean forwardRallies()           { return player().sv.forwardRallies(from.id); }
    public void toggleForwardRallies()        { player().sv.toggleForwardRallies(from.id); }

    private FlightPathSprite pathSprite() {
        if (pathSprite == null)
            pathSprite =  pathSpriteTo(rallySystem());
        return pathSprite;
    }
    public Ship ship()              { return homeSystemView().colony().transport(); }
    @Override
    public StarSystem starSystem()       {
        if (hoveringDest != null)
            return hoveringDest;
        else if (clickedDest != null)
            return clickedDest;
        else if (rallySystem() != null)
            return rallySystem();
        else
            return null;
    }
    @Override
    public boolean isSelectableAt(GalaxyMapPanel map, int mapX, int mapY) {
        return false;
    }
    public FlightPathSprite pathSpriteTo(StarSystem sys) {
        return new FlightPathSprite(from, sys);
    }
    @Override
    public void draw(GalaxyMapPanel map, Graphics2D g2) {
        if (map.scaleX() >  GalaxyMapPanel.MAX_RALLY_SCALE) 
            return;

        StarSystem displayDest = starSystem();
        if (displayDest != null) 
            pathSprite().drawPlanetPath(map, g2, displayDest);
    }
}
