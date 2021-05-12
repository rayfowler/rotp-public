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
import rotp.model.galaxy.StarSystem;
import rotp.model.galaxy.Transport;
import rotp.ui.main.GalaxyMapPanel;

public class SystemTransportSprite extends MapSprite {
    StarSystem clickedDest;
    StarSystem hoveringDest;

    public SystemTransportSprite(StarSystem s) {
        init(s);
    }
    private void init(StarSystem s) {
        source(s);
    }
    public int amt() {
        return homeSystem().transportAmt;
    }
    public void amt(int i) {
        homeSystem().transportAmt = i;
    }
    public FlightPathSprite pathSpriteTo(StarSystem sys) {
        // no system transports for uncolonized systems
        if (!player().sv.isColonized(homeSystem().id))
            return null;
        // cannot see transports than have not launched yet
        if (homeSystem() == sys)
            return null;

        return new FlightPathSprite(homeSystem().colony().transport(), sys);
    }
    public void cancel() {
        Transport tr = homeSystem().colony().transport();
        amt(tr.size());
        homeSystem().transportDestId = id(tr.destination());
        clickedDest = tr.destination();
        hoveringDest = null;
    }
    public void launch() {
        amt(0);
        homeSystem().transportDestId = StarSystem.NULL_ID;
        clickedDest = null;
        hoveringDest = null;
    }
    public void clear() {
        amt(0);
        homeSystem().transportDestId = StarSystem.NULL_ID;
        clickedDest = null;
        hoveringDest = null;
        homeSystem().colony().clearTransport();
    }
    public void accept() {
        homeSystem().transportDestId = id(clickedDest);
        homeSystem().transportTravelTime = 0;
        homeSystem().colony().scheduleTransportsToSystem(clickedDest, amt());
    }
    public void accept(float travelTime) {
        homeSystem().transportDestId = id(clickedDest);
        homeSystem().transportTravelTime = travelTime;
        homeSystem().colony().scheduleTransportsToSystem(clickedDest, amt(), travelTime);
    }
    public boolean canClear() {
        Transport tr = homeSystem().colony().transport();
        return tr.size() > 0;
    }
    public boolean canAccept() {
        Transport tr = homeSystem().colony().transport();
        return (clickedDest != null) && (amt() > 0) && ((clickedDest != tr.destination()) || (amt() != tr.size()));
    }
    public boolean increment(int n) {
        int maxSendingSize = player().sv.maxTransportsToSend(homeSystem().id);
        int prevAmt = amt();
        int newAmt = bounds(0, (prevAmt+n), maxSendingSize);
        if (prevAmt == newAmt)
            return false;

        homeSystem().transportAmt = newAmt;
        return true;
    }
    public boolean decrement(int n) {
        int prevAmt = amt();
        int newAmt = max(0, (prevAmt-n));
        if (prevAmt == newAmt)
            return false;

        homeSystem().transportAmt = newAmt;
        return true;
    }
    public void clickedDest(StarSystem sys)   { clickedDest = sys; }
    public void hoveringDest(StarSystem sys)  { hoveringDest = sys; }
    public StarSystem homeSystem()            { return (StarSystem) source(); }
    @Override
    public StarSystem starSystem()       {
        if (hoveringDest != null)
            return hoveringDest;
        else if (clickedDest != null)
            return clickedDest;
        else
            return null;
    }
    @Override
    public boolean isSelectableAt(GalaxyMapPanel map, int mapX, int mapY) {
        return false;
    }
    @Override
    public void draw(GalaxyMapPanel map, Graphics2D g2) {
        if (starSystem() == null)
            return;
        FlightPathSprite path = pathSpriteTo(starSystem());
        if (path != null) 
            path.draw(map, g2);
    }
}
