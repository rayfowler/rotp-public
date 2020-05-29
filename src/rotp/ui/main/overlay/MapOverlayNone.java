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
package rotp.ui.main.overlay;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.List;
import rotp.model.Sprite;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.StarSystem;
import rotp.ui.BasePanel;
import rotp.ui.RotPUI;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.main.MainUI;
import rotp.ui.sprites.ShipRelocationSprite;
import rotp.ui.sprites.SystemTransportSprite;

public class MapOverlayNone extends MapOverlay {
    MainUI parent;
    public MapOverlayNone(MainUI p) {
        parent = p;
    }
    @Override
    public boolean hideNextTurnNotice()           { return false; }
    @Override
    public boolean canChangeMapScale()            { return true; }
    @Override
    public boolean consumesClicks(Sprite spr)  { return false; }
    @Override
    public boolean hoveringOverSprite(Sprite o) { return false; }
    @Override
    public void advanceMap() {
        parent.resumeTurn();
    }
    @Override
    public void paintOverMap(MainUI parent, GalaxyMapPanel ui, Graphics2D g2) {
    }
    @Override
    public boolean handleKeyPress(KeyEvent e) {
        if (session().performingTurn()) {
                misClick();
                return false;
        }
        int s40 = BasePanel.s40;
        List<StarSystem> systems;
        List<ShipFleet> fleets;
        StarSystem currSys;
        ShipFleet currFleet;
        int index;

        switch(e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
                parent.displayPanel().keyPressed(e);
                break;
            case KeyEvent.VK_EQUALS:
                if (e.isShiftDown())  
                    parent.map().adjustZoom(-1);
                break;
            case KeyEvent.VK_MINUS:
                parent.map().adjustZoom(1);
                break;
            case KeyEvent.VK_UP:
                parent.map().dragMap(0, s40);
                break;
            case KeyEvent.VK_DOWN:
                parent.map().dragMap(0, -s40);
                break;
            case KeyEvent.VK_LEFT:
                parent.map().dragMap(s40, 0);
                break;
            case KeyEvent.VK_RIGHT:
                parent.map().dragMap(-s40, 0);
                break;
            case KeyEvent.VK_G:
                buttonClick();
                RotPUI.instance().selectGamePanel();
                break;
            case KeyEvent.VK_D:
                buttonClick();
                RotPUI.instance().selectDesignPanel();
                break;
            case KeyEvent.VK_F:
                buttonClick();
                RotPUI.instance().selectFleetPanel();
                break;
            case KeyEvent.VK_S:
                if (e.isShiftDown())
                    RotPUI.instance().showSpiesCaptured();
                else {
                    buttonClick();
                    RotPUI.instance().selectSystemsPanel();
                }
                break;
            case KeyEvent.VK_R:
                if  (e.getModifiersEx() == 0) {
                    buttonClick();
                    RotPUI.instance().selectRacesPanel();
                    break;
                }
                else if (e.isAltDown()) {
                    if (parent.clickedSprite() instanceof StarSystem) {
                        StarSystem sys = (StarSystem) parent.clickedSprite();
                        if (player().canRallyFleetsTo(id(sys))) {
                            softClick();
                            player().changeAllExistingRallies(sys);
                            parent.displayPanel().repaint();
                            break;
                        }
                        }
                }
                else if (e.isControlDown()) {
                    if (parent.clickedSprite() instanceof StarSystem) {
                        StarSystem sys = (StarSystem) parent.clickedSprite();
                        if (player().canRallyFleetsFrom(id(sys))) {
                            softClick();
                            parent.clickedSprite(sys.rallySprite());
                            parent.displayPanel().repaint();
                            break;
                        }
                    }
                }
                misClick();
                break;
            case KeyEvent.VK_C:
                buttonClick();
                RotPUI.instance().selectPlanetsPanel();
                break;
            case KeyEvent.VK_T:
                if  (e.getModifiersEx() == 0) {
                    buttonClick();
                    RotPUI.instance().selectTechPanel();
                    break;
                }
                else if (e.getModifiersEx() == 2) {
                    if (parent.clickedSprite() instanceof StarSystem) {
                        StarSystem sys = (StarSystem) parent.clickedSprite();
                        if (player().canSendTransportsFrom(sys)) {
                            softClick();
                            parent.clickedSprite(sys.transportSprite());
                            parent.displayPanel().repaint();
                            break;
                        }
                    }
                }
                misClick();
                break;
            case KeyEvent.VK_L:
                if (parent.clickedSprite() instanceof StarSystem) {
                    StarSystem sys = (StarSystem) parent.clickedSprite();
                    if (player().canRallyFleetsFrom(id(sys))) {
                        softClick();
                        parent.clickedSprite(sys.rallySprite());
                        parent.displayPanel().repaint();
                        break;
                    }
                }
                misClick();
                break;
            case KeyEvent.VK_N:
                //softClick();
                parent.handleNextTurn();
                session().nextTurn();
                break;
            case KeyEvent.VK_F2:
                //softClick();
                systems = player().orderedColonies();
                currSys = null;
                // are we transporting?
                if (parent.clickedSprite() instanceof StarSystem) 
                    currSys = (StarSystem) parent.clickedSprite();
                // find next index (exploit that missing element returns -1, so set to 0)
                index = systems.indexOf(currSys)+1;
                if (index == systems.size())
                    index = 0;
                parent.clickedSprite(systems.get(index));
                parent.map().recenterMapOn(systems.get(index));
                parent.repaint();
                break;
            case KeyEvent.VK_F3:
                //softClick();
                systems = player().orderedColonies();
                currSys = null;
                if (parent.clickedSprite() instanceof StarSystem) 
                    currSys = (StarSystem) parent.clickedSprite();
                index = systems.indexOf(currSys)-1;
                if (index < 0)
                    index = systems.size()-1;
                parent.clickedSprite(systems.get(index));
                parent.map().recenterMapOn(systems.get(index));
                parent.repaint();
                break;
            case KeyEvent.VK_F5:
                systems = player().orderedShipConstructingColonies();
                currSys = null;
                if (systems.isEmpty())
                    misClick();
                else {
                    //softClick();
                    // are we transporting?
                    if (parent.clickedSprite() instanceof SystemTransportSprite)
                        currSys = ((SystemTransportSprite) parent.clickedSprite()).homeSystem();
                    // are we ship relocating?
                    else if (parent.clickedSprite() instanceof ShipRelocationSprite)
                        currSys = ((ShipRelocationSprite) parent.clickedSprite()).homeSystemView();
                    else if (parent.clickedSprite() instanceof StarSystem)
                        currSys = (StarSystem) parent.clickedSprite();
                    // find next index (exploit that missing element returns -1, so set to 0)
                    index = systems.indexOf(currSys)+1;
                    if (index == systems.size())
                        index = 0;
                    parent.clickedSprite(systems.get(index));
                    parent.map().recenterMapOn(systems.get(index));
                    parent.repaint();
                }
                break;
            case KeyEvent.VK_F6:
                systems = player().orderedShipConstructingColonies();
                currSys = null;
                if (systems.isEmpty())
                    misClick();
                else {
                    //softClick();
                    // are we transporting?
                    if (parent.clickedSprite() instanceof SystemTransportSprite)
                        currSys = ((SystemTransportSprite) parent.clickedSprite()).homeSystem();
                    // are we ship relocating?
                    else if (parent.clickedSprite() instanceof ShipRelocationSprite)
                        currSys = ((ShipRelocationSprite) parent.clickedSprite()).homeSystemView();
                    else if (parent.clickedSprite() instanceof StarSystem)
                        currSys = (StarSystem) parent.clickedSprite();
                    index = systems.indexOf(currSys)-1;
                    if (index < 0)
                        index = systems.size()-1;
                    parent.clickedSprite(systems.get(index));
                    parent.map().recenterMapOn(systems.get(index));
                    parent.repaint();
                }
                break;
            case KeyEvent.VK_F7:
                systems = player().orderedUnderAttackSystems(parent.map().showUnarmedShips(), !parent.map().showFleetsOnly());
                currSys = null;
                if (systems.isEmpty())
                    misClick();
                else {
                    //softClick();
                    // are we transporting?
                    if (parent.clickedSprite() instanceof SystemTransportSprite)
                        currSys = ((SystemTransportSprite) parent.clickedSprite()).homeSystem();
                        // are we ship relocating?
                    else if (parent.clickedSprite() instanceof ShipRelocationSprite)
                        currSys = ((ShipRelocationSprite) parent.clickedSprite()).homeSystemView();
                    else if (parent.clickedSprite() instanceof StarSystem)
                        currSys = (StarSystem) parent.clickedSprite();
                    // find next index (exploit that missing element returns -1, so set to 0)
                    index = systems.indexOf(currSys)+1;
                    if (index == systems.size())
                        index = 0;
                    parent.clickedSprite(systems.get(index));
                    parent.map().recenterMapOn(systems.get(index));
                    parent.repaint();
                }
                break;
            case KeyEvent.VK_F8:
                systems = player().orderedUnderAttackSystems(parent.map().showUnarmedShips(), !parent.map().showFleetsOnly());
                currSys = null;
                if (systems.isEmpty())
                    misClick();
                else {
                    //softClick();
                    // are we transporting?
                    if (parent.clickedSprite() instanceof SystemTransportSprite)
                        currSys = ((SystemTransportSprite) parent.clickedSprite()).homeSystem();
                    // are we ship relocating?
                    else if (parent.clickedSprite() instanceof ShipRelocationSprite)
                        currSys = ((ShipRelocationSprite) parent.clickedSprite()).homeSystemView();
                    else if (parent.clickedSprite() instanceof StarSystem)
                        currSys = (StarSystem) parent.clickedSprite();
                    index = systems.indexOf(currSys)-1;
                    if (index < 0)
                        index = systems.size()-1;
                    parent.clickedSprite(systems.get(index));
                    parent.map().recenterMapOn(systems.get(index));
                    parent.repaint();
                }
                break;
            case KeyEvent.VK_F9:
                fleets = player().orderedFleets();
                currFleet = null;
                if (fleets.isEmpty())
                    misClick();
                else {
                    //softClick();
                    if (parent.clickedSprite() instanceof ShipFleet)
                        currFleet = (ShipFleet)parent.clickedSprite();
                    index = fleets.indexOf(currFleet)+1;
                    if (index == fleets.size())
                        index = 0;
                    parent.clickingOnSprite(fleets.get(index), 1, false, true);
                    parent.map().recenterMapOn(fleets.get(index));
                    parent.repaint();
                }
                break;
            case KeyEvent.VK_F10:
                fleets = player().orderedFleets();
                currFleet = null;
                if (fleets.isEmpty())
                    misClick();
                else {
                    //softClick();
                    if (parent.clickedSprite() instanceof ShipFleet)
                        currFleet = (ShipFleet)parent.clickedSprite();
                    index = fleets.indexOf(currFleet)-1;
                    if (index < 0)
                        index = fleets.size()-1;
                    parent.clickingOnSprite(fleets.get(index), 1, false, true);
                    parent.map().recenterMapOn(fleets.get(index));
                    parent.repaint();
                }
                break;
            case KeyEvent.VK_F11:
                fleets = player().orderedEnemyFleets();
                currFleet = null;
                if (fleets.isEmpty())
                    misClick();
                else {
                    //softClick();
                    if (parent.clickedSprite() instanceof ShipFleet)
                        currFleet = (ShipFleet)parent.clickedSprite();
                    index = fleets.indexOf(currFleet)+1;
                    if (index == fleets.size())
                        index = 0;
                    parent.clickingOnSprite(fleets.get(index), 1, false, true);
                    parent.map().recenterMapOn(fleets.get(index));
                    parent.repaint();
                }
                break;
            case KeyEvent.VK_F12:
                fleets = player().orderedEnemyFleets();
                currFleet = null;
                if (fleets.isEmpty())
                    misClick();
                else {
                    //softClick();
                    if (parent.clickedSprite() instanceof ShipFleet)
                        currFleet = (ShipFleet)parent.clickedSprite();
                    index = fleets.indexOf(currFleet)-1;
                    if (index < 0)
                        index = fleets.size()-1;
                    parent.clickingOnSprite(fleets.get(index), 1, false, true);
                    parent.map().recenterMapOn(fleets.get(index));
                    parent.repaint();
                }
                break;
            default:
                parent.displayPanel().keyPressed(e);
                break;
        }
        return true;
    }
}
