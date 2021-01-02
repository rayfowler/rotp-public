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
package rotp.ui.main;

import java.awt.CardLayout;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import rotp.model.Sprite;
import rotp.model.empires.Empire;
import rotp.model.galaxy.Ship;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.StarSystem;
import rotp.model.galaxy.Transport;
import rotp.ui.BasePanel;
import rotp.ui.SystemViewer;
import rotp.ui.map.IMapHandler;
import rotp.ui.sprites.FlightPathSprite;
import rotp.ui.sprites.ShipRelocationSprite;
import rotp.ui.sprites.SystemTransportSprite;

public class SpriteDisplayPanel extends BasePanel implements SystemViewer, MouseListener, MouseMotionListener, MouseWheelListener {
    private static final long serialVersionUID = 1L;
    private static final String PLAYER_SYSTEM = "PlayerSystem";
    private static final String ALIEN_SYSTEM = "AlienSystem";
    private static final String UNEXPLORED_ALIEN_SYSTEM = "UnexploredAlienSystem";
    private static final String EXPLORED_SYSTEM = "ExploredSystem";
    private static final String UNEXPLORED_SYSTEM = "UnexploredSystem";
    private static final String DISPLAY_FLEET = "DisplayFleet";
    private static final String DEPLOY_TRANSPORTS = "DeployTransports";
    private static final String DISPLAY_TRANSPORTS = "DisplayTransports";
    private static final String RELOCATE_SHIPS = "RelocateShips";

    private final CardLayout layout = new CardLayout();
    BasePanel currentPanel;

    EmpireSystemPanel playerSystemPane;
    AlienSystemPanel alienSystemPane;
    UnexploredAlienSystemPanel unexploredAlienPane;
    ExploredSystemPanel exploredSystemPane;
    UnexploredSystemPanel unexploredSystemPane;
    FleetPanel fleetPane;
    TransportDeploymentPanel transportDeployPane;
    TransportPanel transportDisplayPane;
    RallyPointPanel shipRelocationPane;

    IMapHandler parent;

    public SpriteDisplayPanel(IMapHandler p) {
        parent = p;
        initModel();
    }
    @Override
    public void cancel() {
        currentPanel.cancel();
    }
    @Override
    public boolean canEscape() {
        return currentPanel.canEscape();
    }
    public boolean hoverOverFleets() { 
        return currentPanel instanceof MapSpriteViewer ? ((MapSpriteViewer) currentPanel).hoverOverFleets() : true;
    }
    public boolean hoverOverSystems() { 
        return currentPanel instanceof MapSpriteViewer ? ((MapSpriteViewer) currentPanel).hoverOverSystems() : true;
    }
    public boolean hoverOverFlightPaths(){ 
        return currentPanel instanceof MapSpriteViewer ? ((MapSpriteViewer) currentPanel).hoverOverFlightPaths() : true;
    }
    @Override
    public void handleNextTurn()   { currentPanel.handleNextTurn(); }
    @Override
    public String subPanelTextureName()    { return TEXTURE_GRAY; }
    @Override
    public StarSystem systemViewToDisplay() {
        Sprite sprite = spriteToDisplay();
        if (sprite instanceof StarSystem)
            return (StarSystem) sprite;
        return spriteToDisplay().starSystem();
    }
    public ShipFleet shipFleetToDisplay()   {
        Sprite sprite = spriteToDisplay();
        if (sprite instanceof ShipFleet)
            return (ShipFleet) sprite;
        else if (sprite instanceof FlightPathSprite) {
            Ship ship = ((FlightPathSprite) sprite).ship();
            return ship instanceof ShipFleet ? (ShipFleet) ship : null;
        }
        else if (sprite == null)
            return null;
        else {
            // unexpected sprite type. Could occur if asynchronous mouse movement changes the
            // hover sprite before the clicked logic gets here
            return null;
        }
    }
    public Transport transportToDisplay()   {
        Sprite sprite = spriteToDisplay();
        if (sprite instanceof Transport)
            return (Transport) sprite;
        else if (sprite instanceof FlightPathSprite) {
            Ship ship = ((FlightPathSprite) sprite).ship();
            return ship instanceof Transport ? (Transport) ship : null;
        }
        else
            return null;
    }

    private Sprite hoveringSprite()  {
        Sprite s = parent.hoveringSprite();
        return canDisplay(s) ? s : null;
    }
    private Sprite clickedSprite() { return parent.clickedSprite(); }

    @Override
    public boolean useNullClick(int cnt, boolean right) {
        if (currentPanel == null)
            return super.useNullClick(cnt, right);
        else
            return currentPanel.useNullClick(cnt, right);
    }
    @Override
    public boolean useClickedSprite(Sprite o, int count, boolean rightClick) {
        boolean canUse = (currentPanel != null) && currentPanel.useClickedSprite(o, count, rightClick);

        if (!canUse) {
            // if the currentPanel cannot "use" the selected sprite, find out the
            // best panel to display the sprite. If different, switch to that panel
            // and return if that panel can use the sprite. This prevents errors when
            // the current panel doesn't switch properly before the click because there
            // was no mouse hovering event preceding this (i.e. on touch screens)
            BasePanel bestPanel = bestPanel(o);
            if (bestPanel != currentPanel) {
                selectBestPanel(o);
                return currentPanel.useClickedSprite(o, count, rightClick);
            }
        }
        return canUse;
    }
    @Override
    public boolean useHoveringSprite(Sprite o) {
        return (currentPanel != null) && currentPanel.useHoveringSprite(o);
    }
    private boolean canDisplay(Sprite s) {
        return (s instanceof StarSystem)
            || (s instanceof ShipFleet)
            || (s instanceof Transport)
            || (s instanceof FlightPathSprite);
    }
    public Sprite spriteToDisplay() {
        Sprite clicked = clickedSprite();
        Sprite hovering = hoveringSprite();

        if ((hovering == null) && (clicked == null))
            err("No clicked or hovering sprite!");

        return hovering == null ? clicked : hovering;
    }
    private void selectBestPanel(Sprite o) {
        if (o instanceof ShipRelocationSprite)
            selectShipRelocationPanel();
        else if (o instanceof SystemTransportSprite)
            selectTransportDeployPanel();
        else if (o instanceof StarSystem) {
            Empire pl = player();
            StarSystem sys = (StarSystem) o;
            if (!pl.sv.isScouted(sys.id)) {
                if (!pl.sv.isColonized(sys.id))
                    selectUnexploredSystemPanel();
                else
                    selectUnexploredAlienPanel();
            }
            else if (!pl.sv.isColonized(sys.id))
                selectExploredSystemPanel();
            else if (!(sys.empire() == pl))
                selectAlienSystemPanel();
            else
                selectPlayerSystemPanel();
        }
        else if (o instanceof ShipFleet)
            selectFleetPanel();
        else if (o instanceof Transport)
            selectTransportDisplayPanel();
        else if (o instanceof FlightPathSprite) {
            FlightPathSprite flspr = (FlightPathSprite) o;
            Ship sh = flspr.ship();
            if (sh instanceof ShipFleet)
                selectFleetPanel();
            else if (sh instanceof Transport)
                selectTransportDisplayPanel();
        }
    }
    private BasePanel bestPanel(Sprite o) {
        if (o instanceof ShipRelocationSprite)
            return shipRelocationPane;
        else if (o instanceof SystemTransportSprite)
            return transportDeployPane;
        else if (o instanceof StarSystem) {
            Empire pl = player();
            StarSystem sys = (StarSystem) o;
            if (!pl.sv.isScouted(sys.id)) {
                if (!player().sv.isColonized(sys.id))
                    return unexploredSystemPane;
                else
                    return unexploredAlienPane;
            }
            else if (!player().sv.isColonized(sys.id))
                return exploredSystemPane;
            else if (!(sys.empire() == pl))
                return alienSystemPane;
            else
                return playerSystemPane;
        }
        else if (o instanceof ShipFleet)
            return fleetPane;
        else if (o instanceof Transport)
            return transportDisplayPane;
        else if (o instanceof FlightPathSprite) {
            FlightPathSprite flspr = (FlightPathSprite) o;
            Ship sh = flspr.ship();
            if (sh instanceof ShipFleet)
                return fleetPane;
            else if (sh instanceof Transport)
                return transportDisplayPane;
        }
        return null;
    }

    public void selectPlayerSystemPanel()        { currentPanel = playerSystemPane;     layout.show(this, PLAYER_SYSTEM); }
    public void selectAlienSystemPanel()         { currentPanel = alienSystemPane;      layout.show(this, ALIEN_SYSTEM); }
    public void selectUnexploredAlienPanel()     { currentPanel = unexploredAlienPane;  layout.show(this, UNEXPLORED_ALIEN_SYSTEM); }
    public void selectExploredSystemPanel()      { currentPanel = exploredSystemPane;   layout.show(this, EXPLORED_SYSTEM); }
    public void selectUnexploredSystemPanel()    { currentPanel = unexploredSystemPane; layout.show(this, UNEXPLORED_SYSTEM); }
    public void selectFleetPanel()               { currentPanel = fleetPane;            layout.show(this, DISPLAY_FLEET); }
    public void selectTransportDeployPanel()     { currentPanel = transportDeployPane;  layout.show(this, DEPLOY_TRANSPORTS); }
    public void selectTransportDisplayPanel()    { currentPanel = transportDisplayPane; layout.show(this, DISPLAY_TRANSPORTS); }
    public void selectShipRelocationPanel()      { currentPanel = shipRelocationPane;   layout.show(this, RELOCATE_SHIPS); }

    @Override
    public void paint(Graphics g) {
        selectBestPanel(spriteToDisplay());

        try { super.paint(g); }
        catch(Exception e) {
            log("sprite panel display error: "+e.toString());
        }
    }
    @Override
    public void animate() {
        if (currentPanel != null)
            currentPanel.animate();
    }
    @Override
    public void keyPressed(KeyEvent e) {
        if (currentPanel != null)
            currentPanel.keyPressed(e);
    }
    private void initModel() {
        setBackground(MainUI.paneBackground());
        setLayout(layout);
        playerSystemPane = new EmpireSystemPanel(this);
        alienSystemPane = new AlienSystemPanel(this);
        unexploredAlienPane = new UnexploredAlienSystemPanel(this);
        exploredSystemPane = new ExploredSystemPanel(this);
        unexploredSystemPane = new UnexploredSystemPanel(this);
        fleetPane = new FleetPanel(this);
        transportDeployPane = new TransportDeploymentPanel(this);
        transportDisplayPane = new TransportPanel(this);
        shipRelocationPane = new RallyPointPanel(this);

        add(playerSystemPane, PLAYER_SYSTEM);
        add(alienSystemPane, ALIEN_SYSTEM);
        add(unexploredAlienPane, UNEXPLORED_ALIEN_SYSTEM);
        add(exploredSystemPane, EXPLORED_SYSTEM);
        add(unexploredSystemPane, UNEXPLORED_SYSTEM);
        add(fleetPane, DISPLAY_FLEET);
        add(transportDeployPane, DEPLOY_TRANSPORTS);
        add(transportDisplayPane, DISPLAY_TRANSPORTS);
        add(shipRelocationPane, RELOCATE_SHIPS);
        
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
    }
    @Override
    public void mouseClicked(MouseEvent e) { }
    @Override
    public void mousePressed(MouseEvent e) { }
    @Override
    public void mouseReleased(MouseEvent e) { }
    @Override
    public void mouseEntered(MouseEvent e) { }
    @Override
    public void mouseExited(MouseEvent e) { }
    @Override
    public void mouseDragged(MouseEvent e) { }
    @Override
    public void mouseMoved(MouseEvent e) { }
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) { }
}
