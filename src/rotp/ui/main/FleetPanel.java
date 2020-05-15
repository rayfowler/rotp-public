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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LinearGradientPaint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import rotp.model.Sprite;
import rotp.model.empires.Empire;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.StarSystem;
import rotp.model.ships.ShipDesign;
import rotp.model.ships.ShipDesignLab;
import rotp.ui.BasePanel;
import rotp.ui.sprites.FlightPathSprite;

public class FleetPanel extends BasePanel implements MapSpriteViewer {
    private static final long serialVersionUID = 1L;
    private final SpriteDisplayPanel parent;
    protected BasePanel topPane;
    protected BasePanel detailPane;
    protected BasePanel bottomPane;
    private final int[] stackAdjustment = new int[ShipDesignLab.MAX_DESIGNS];
    //session vars
    private StarSystem selectedDest()         { return (StarSystem) sessionVar("FLEETDEPLOY_SELECTED_DEST"); }
    private void selectedDest(StarSystem s)   { sessionVar("FLEETDEPLOY_SELECTED_DEST", s); }
    private StarSystem tentativeDest()        { return (StarSystem) sessionVar("FLEETDEPLOY_TENTATIVE_DEST"); }
    private void tentativeDest(StarSystem s)  { sessionVar("FLEETDEPLOY_TENTATIVE_DEST", s); }
    private ShipFleet selectedFleet()         {
        Object obj = sessionVar("SELECTED_FLEET");
        if (obj instanceof ShipFleet)
            return (ShipFleet) obj;

        selectedFleet(null);
        return null;
    }
    @Override
    public void cancel()                            { removeSessionVar("ADJUSTED_FLEET"); clearStackAdjustments(); }
    private void selectedFleet(ShipFleet s)         { sessionVar("SELECTED_FLEET", s); }
    private ShipFleet adjustedFleet() {
        ShipFleet adjFleet = (ShipFleet) sessionVar("ADJUSTED_FLEET");
        if (adjFleet != null)
            return adjFleet;

        //log("creating adjusted fleet");
        adjFleet = newAdjustedFleet();
        adjustedFleet(adjFleet);
        if (adjFleet == null)
            return null;
        if (tentativeDest() != null)
            FlightPathSprite.workingPath(adjFleet.pathSpriteTo(tentativeDest()));
        return adjFleet;
    }
    private void  adjustedFleet(ShipFleet fl) {
        FlightPathSprite.workingPath(null);
        StarSystem sys = selectedDest();
        if ((fl != null) && (sys != null))
            FlightPathSprite.workingPath(fl.pathSpriteTo(sys));

        sessionVar("ADJUSTED_FLEET", fl);
    }
    private ShipFleet displayedFleet()  {
        ShipFleet fl = (ShipFleet) sessionVar("DISPLAYED_FLEET");
        fl = parent.shipFleetToDisplay();
        return (fl == null) ? adjustedFleet() : fl;
    }
    private void  displayedFleet(ShipFleet s)       { sessionVar("DISPLAYED_FLEET", s); }
    public FleetPanel(SpriteDisplayPanel p) {
        parent = p;
        selectNewFleet(null);
        initModel();
    }
    @Override
    public void handleNextTurn() {  clearStackAdjustments(); }
    public ShipFleet fleetToDisplay() {
        return parent.shipFleetToDisplay();
    }
    @Override
    public boolean hoverOverFleets()               { return (selectedFleet() == null) || (selectedFleet().empire() != player()); }
    @Override
    public boolean hoverOverFlightPaths()          { return selectedFleet() == null; }
    public StarSystem displayedDestination() {
        if (tentativeDest() != null)
            return tentativeDest();
        else if (selectedDest() != null)
            return selectedDest();
        else
            return null;
    }
    private void clearStackAdjustments() {
        adjustedFleet(null);
        for (int i=0;i<stackAdjustment.length;i++)
            stackAdjustment[i] = 0;
    }
    private boolean haveClickedOnCurrentFleet() {
        // is the current clicked sprite a Fleet?
        return parent.parent.isClicked(displayedFleet());
    }
    private boolean canConsume(Sprite s) {
        return (s == null) || (s instanceof ShipFleet) || (s instanceof StarSystem);
    }
    private boolean canSendFleet() {
        if (selectedDest() == null)
            return false;

        ShipFleet newFleet = adjustedFleet();
        if (newFleet.isEmpty())
            return false;

        return newFleet.canReach(selectedDest());
    }
    private ShipFleet newAdjustedFleet() {
        ShipFleet selectedFleet = selectedFleet();
        if (selectedFleet == null) {
            selectedFleet(null);
            return null;
        }
        if (!selectedFleet.isActive())
            return null;

        if (selectedFleet.deployed())
            return selectedFleet;

        ShipFleet newFleet = ShipFleet.copy(selectedFleet);
        for (int i=0;i<stackAdjustment.length;i++)
            newFleet.addShips(i, stackAdjustment[i]);
        newFleet.rallySysId(selectedFleet.rallySysId());
        newFleet.retreating(selectedFleet.retreating());
        return newFleet;
    }
    public void sendFleet() {
        // attempts to send fleet (OK button) if that selected
        // vars are valid
        if (!canSendFleet()) 
            return;

        ShipFleet newFleet = adjustedFleet();
        ShipFleet displayedFleet = displayedFleet();

        if (displayedFleet.isInTransit())
            galaxy().ships.redirectFleet(displayedFleet, selectedDest().id);
        else {
            boolean newFleetCreated = galaxy().ships.deploySubfleet(displayedFleet, newFleet.num, selectedDest().id);
            // newFleet isEmpty if it was the entire fleet selected
            if (newFleetCreated) {
                if (displayedFleet.isEmpty()) 
                    cancelFleet();
                else {
                    selectNewFleet(displayedFleet);
                    adjustStacksToMatchFleet(displayedFleet, newFleet);
                }
            }
            else {
                cancelFleet();
            }
        }
        FlightPathSprite.clearWorkingPaths();
        parent.parent.map().repaint();
    }
    public void undeployFleet() {
        galaxy().ships.undeployFleet(selectedFleet());
        selectNewFleet(null);
        parent.parent.reselectCurrentSystem();
        parent.parent.map().repaint();
    }
    public void cancelFleet() {
        selectNewFleet(null);
        parent.parent.reselectCurrentSystem();
    }
    private void selectNewFleet(ShipFleet fl) {
        clearStackAdjustments();
        adjustedFleet(null);
        tentativeDest(null);
        selectedDest(null);
        displayedFleet(fl);
        selectedFleet(fl);
        FlightPathSprite.clearWorkingPaths();
    }
    private void adjustStacksToMatchFleet(ShipFleet selected, ShipFleet deployed) {
        int selectedCount = 0;
        for (int i=0;i<stackAdjustment.length;i++) {
            if (selected.num(i) <= deployed.num(i)) 
                stackAdjustment[i] = 0;
        else
                    stackAdjustment[i] = deployed.num(i) - selected.num(i);
            selectedCount += (selected.num(i)+stackAdjustment[i]);
        }
        // if what remains is a selected fleet adjusted down to 0, then
        // clear the adjustments
        if (selectedCount == 0) 
            clearStackAdjustments();
    }
    @Override
    public boolean useHoveringSprite(Sprite o) {
        if (!canConsume(o))
            return false;

        // no selected fleet, so skip. This happens from when we have selected
        // a system, hover over a fleet (displaying this UI), and then
        // hover over a system (which can be used by this UI so it gets this far)
        if (selectedFleet() == null)
            return false;

        // SHOULD NEVER OCCUR as these sprites fail the prior check
        // any hovered fleets are consumed with no action
        if ((o instanceof ShipFleet)
        || (o instanceof FlightPathSprite)) {
            tentativeDest(null);
            FlightPathSprite.clearWorkingPaths();
            return haveClickedOnCurrentFleet();
        }

        if (o == null) {
            // if we aren't currently hovering over a target system,
            // then we aren't using this null to clear it out
            if (tentativeDest() == null)
                return false;
            if (haveClickedOnCurrentFleet()) {
                if (selectedDest() == null) 
                    FlightPathSprite.clearWorkingPaths();
                else
                    adjustedFleet().use(selectedDest(), parent.parent);
            }
            else {
                // we were hovering over fleet, so default back to selected fleet
                displayedFleet(null);
            }
            tentativeDest(null);
            adjustedFleet(null);
            parent.parent.repaint();
            return haveClickedOnCurrentFleet();
        }

        // if we are not a System, quit now
        if (!(o instanceof StarSystem))
            return false;
        
        if (adjustedFleet() == null)
            return false;

        if (adjustedFleet().empire() != player())
            return false;

        adjustedFleet().use(o, parent.parent);
        tentativeDest((StarSystem) o);
        return true;
    }
    @Override
    public boolean useNullClick(int cnt, boolean right) {
        if (right) {
            cancelFleet();
            return true;
        }
        return false;
    }
    @Override
    public boolean useClickedSprite(Sprite o, int count, boolean rightClick) {
        // we have clicked on a system view at this point
        if (rightClick) {
            cancelFleet();
            return true;
        }

        // use clicked Fleets that can be sent... just reset vars
        if (o instanceof ShipFleet) {
            ShipFleet clickedFleet = (ShipFleet) o;
            if (clickedFleet.empire() != player())
                return false;
            if (clickedFleet != selectedFleet())
                selectNewFleet(clickedFleet);
            return false;
        }

        if (o instanceof FlightPathSprite)  
            return true;    

        // clicking on anything but a systemview
        // will leave this screen
        if (!(o instanceof StarSystem)) 
            return false;

        // special case check:
        // on Cancel, then selected fleet is null and we get
        // here when the last selected system is reselected
        if (selectedFleet() == null) 
            return false;
        
        if (selectedFleet().empire() != player())
            return false;
        
        StarSystem sys = (StarSystem) o;

        if (selectedFleet().destSysId() == sys.id) 
            return false;

        tentativeDest(sys);
        // don't accept clicks for out of range systems
        // but consume the click (to stay on this view)
        ShipFleet adjustedFleet = adjustedFleet();
        if (adjustedFleet == null) 
            return false;
        if (!adjustedFleet.canReach(sys)) { 
            misClick();
            return true;
        }
        if (!adjustedFleet.canSendTo(id(sys))) {
            misClick();
            return true;
        }

        // if we are selecting a system that the selected fleet
        // is orbiting, then consume the event but do nothing
        if (adjustedFleet.system() == sys) 
            return true;

        softClick();
        selectedDest(sys);
        adjustedFleet.use(o, parent.parent);
        //if (count == 2)
        sendFleet();
        return true;
    }
    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        switch (k) {
            case KeyEvent.VK_ESCAPE:
                buttonClick();
                cancelFleet();
                return;
            case KeyEvent.VK_SPACE:
                buttonClick();
                sendFleet();
                return;
            case KeyEvent.VK_TAB:
                // tab-targeting for transports
                ShipFleet fl = adjustedFleet();
                StarSystem currSys;
                if (tentativeDest() != null)
                    currSys = tentativeDest();
                else {
                    int currSysId = fl.isInTransit() ? fl.destSysId() : fl.sysId();
                    currSys = galaxy().system(currSysId);
                }
                List<StarSystem> systems = player().orderedFleetTargetSystems(fl);
                if (systems.size() > 1)
                    softClick();
                else
                    misClick();
                // find next index (exploit that missing element returns -1, so set to 0)
                int index = 0;
                switch(e.getModifiersEx()) {
                    case 0:
                        index = systems.indexOf(currSys)+1;
                        if (index == systems.size())
                            index = 0;
                        break;
                    case 1:
                        index = systems.indexOf(currSys)-1;
                        if (index < 0)
                            index = systems.size()-1;
                        break;
                }
                useClickedSprite(systems.get(index), 1, false);
                //parent.parent.hoveringOverSprite(systems.get(index).sprite());
                parent.repaint();
        }
    }
    private void initModel() {
        setBackground(MainUI.paneBackground());

        topPane = topPane();
        detailPane = detailPane();
        bottomPane = bottomPane();

        setLayout(new BorderLayout());
        if (topPane != null) {
            topPane.setPreferredSize(new Dimension(getWidth(),scaled(145)));
            add(topPane, BorderLayout.NORTH);
        }
        add(detailPane, BorderLayout.CENTER);
        if (bottomPane != null) {
            bottomPane.setPreferredSize(new Dimension(getWidth(),s40));
            add(bottomPane, BorderLayout.SOUTH);
        }
    }
    protected BasePanel topPane() {
        if (topPane == null)
            topPane = new FleetGraphicPane(this);
        return topPane;
    }
    protected BasePanel detailPane() {
        if (detailPane == null)
            detailPane = new FleetDetailPane(this);
        return detailPane;
    }
    protected BasePanel bottomPane() {
        if (bottomPane == null)
            bottomPane = new FleetButtonPane(this);
        bottomPane.setBackground(MainUI.shadeBorderC());
        return bottomPane;
    }
    public class FleetGraphicPane extends BasePanel {
        private static final long serialVersionUID = 1L;
        private final FleetPanel parent;
        public FleetGraphicPane(FleetPanel p){
            parent = p;
            init();
        }
        private void init() {
            setBackground(Color.black);
        }
        @Override
        public void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            super.paintComponent(g);
            int w = getWidth();
            int h = getHeight();

            Empire pl = player();
            ShipFleet fl = parent.fleetToDisplay();

            // this can happen if the fleet is selected and then
            // all designs are scrapped before return to main ui
            if (fl.isEmpty()) {
                cancelFleet();
                return;
            }
            
            StarSystem sys = fl.isOrbiting() ? fl.system() : null;

            if (sys == null) {
                if (fl.hasDestination())
                    g.drawImage(pl.sv.starBackground(this), 0, 0, null);
            }
            else {
                g.drawImage(pl.sv.starBackground(this), 0, 0, null);
                drawStar(g, sys.starType(), s80, w/3, s70);
                sys.planet().draw(g, w, h, s20, s70, s80, 45);
            }
            // draw ship image
            Image shipImg = fl.empire().race().transport();
            int imgW = shipImg.getWidth(null);
            int imgH = shipImg.getHeight(null);
            float scale = (float) s80 / Math.max(imgW, imgH);
            int shipW = (int) (scale*imgW);
            int shipH = (int) (scale*imgH);
            int shipX = s70;
            int shipY = h-shipH-s10;
            g.drawImage(shipImg, shipX,shipY,shipX+shipW,shipY+shipH, 0,0,imgW,imgH, null);

            // draw title
            g.setFont(narrowFont(36));
            String str1 = text("MAIN_FLEET_TITLE", fl.empire().raceName());
            drawBorderedString(g, str1, 2, s15, s42, Color.black, SystemPanel.orangeText);

            // draw orbiting data, bottom up
            int y0 = h-s12;
            g.setColor(SystemPanel.whiteText);
            g.setFont(narrowFont(20));
            if (fl.launched()) {
                if (pl.knowETA(fl) && (fl.hasDestination())) {
                    String dest =  pl.sv.name(fl.destSysId());
                    String str2 = dest.isEmpty() ? text("MAIN_FLEET_DEST_UNSCOUTED") : text("MAIN_FLEET_DESTINATION", dest);
                    int sw2 = g.getFontMetrics().stringWidth(str2);
                    g.drawString(str2, w-sw2-s10, y0);
                    y0 -= s25;
                }
                String str3 = fl.retreating() ? text("MAIN_FLEET_RETREATING") : text("MAIN_FLEET_IN_TRANSIT");
                int sw3 = g.getFontMetrics().stringWidth(str3);
                g.drawString(str3, w-sw3-s10, y0);
                y0 -= s25;
                if (!fl.empire().isPlayer()) {
                    if (pl.alliedWith(fl.empId)) {
                        g.setColor(SystemPanel.greenText);
                        String str4 = text("MAIN_FLEET_ALLY");
                        int sw4 = g.getFontMetrics().stringWidth(str4);
                        g.drawString(str4, w-sw4-s10, y0);
                    } else if (pl.atWarWith(fl.empId)) {
                        g.setColor(SystemPanel.redText);
                        String str4 = text("MAIN_FLEET_ENEMY");
                        int sw4 = g.getFontMetrics().stringWidth(str4);
                        g.drawString(str4, w-sw4-s10, y0);
                    }
                }
            }
            else if (fl.deployed()) {
                String dest =  pl.sv.name(fl.destSysId());
                String str2 = dest.isEmpty() ? text("MAIN_FLEET_DEST_UNSCOUTED") : text("MAIN_FLEET_DESTINATION", dest);
                int sw2 = g.getFontMetrics().stringWidth(str2);
                g.drawString(str2, w-sw2-s10, y0);
                y0 -= s25;
                StarSystem sys1 = fl.system();
                String str3 = text("MAIN_FLEET_ORIGIN", pl.sv.name(sys1.id));
                int sw3 = g.getFontMetrics().stringWidth(str3);
                g.drawString(str3, w-sw3-s10, y0);
                y0 -= s25;
                String str4 = text("MAIN_FLEET_DEPLOYED");
                int sw4 = g.getFontMetrics().stringWidth(str4);
                g.drawString(str4, w-sw4-s10, y0);
            }
            else {
                StarSystem sys1 = fl.system();
                String str2 = sys1 == null ? "" : text("MAIN_FLEET_LOCATION", pl.sv.name(sys1.id));
                if (str2.isEmpty()) 
                    log("ERROR: No system assigned to fleet ");             
                int sw2 = g.getFontMetrics().stringWidth(str2);
                g.drawString(str2, w-sw2-s10, y0);
                y0 -= s25;
                String str3 = text("MAIN_FLEET_IN_ORBIT");
                int sw3 = g.getFontMetrics().stringWidth(str3);
                g.drawString(str3, w-sw3-s10, y0);
            }
            g.setColor(MainUI.shadeBorderC());
            g.fillRect(0, h-s5, w, s5);
        }
    }
    public class FleetDetailPane extends BasePanel implements MouseListener, MouseMotionListener, MouseWheelListener {
        private static final long serialVersionUID = 1L;
        private final Color fleetBackC = new Color(255,255,255,40);
        private BufferedImage starImg;
        private final FleetPanel parent;
        private final Color buttonBackC = new Color(30,30,30);
        private int hoverStackNum = -1;
        private Shape hoverBox, hoverBox2;
        private final Rectangle shipBox[] = new Rectangle[ShipDesignLab.MAX_DESIGNS];
        private final Polygon minBox[] = new Polygon[ShipDesignLab.MAX_DESIGNS];
        private final Polygon maxBox[] = new Polygon[ShipDesignLab.MAX_DESIGNS];
        private final Polygon downBox[] = new Polygon[ShipDesignLab.MAX_DESIGNS];
        private final Polygon upBox[] = new Polygon[ShipDesignLab.MAX_DESIGNS];
        protected Shape textureClip;

        public FleetDetailPane(FleetPanel p) {
            parent = p;
            init();
        }
        private void init() {
            for (int i=0;i<ShipDesignLab.MAX_DESIGNS;i++) {
                shipBox[i] = new Rectangle();
                minBox[i] = new Polygon();
                maxBox[i] = new Polygon();
                downBox[i] = new Polygon();
                upBox[i] = new Polygon();
            }
            addMouseListener(this);
            addMouseMotionListener(this);
            addMouseWheelListener(this);
        }
        @Override
        public String textureName()            { return TEXTURE_GRAY; }
        @Override
        public Shape textureClip()     { return textureClip; }
        @Override
        public void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            super.paintComponent(g0);
            int w = getWidth();
            int h = getHeight();
            int h1 = s90;

            Empire pl = player();
            ShipFleet origFleet = parent.fleetToDisplay();
            if (origFleet == null)
                return;
            ShipFleet displayFleet = origFleet;
            // do we want to display an adjustable fleet based on selected fleet?
            boolean canAdjust = (origFleet != null) && origFleet.canBeAdjustedBy(pl);
            if ((origFleet == null) || canAdjust)
                displayFleet = adjustedFleet();

            if (displayFleet == null)
                displayFleet = origFleet;

            clearButtons();
            drawInfo(g,displayFleet, 0,0,w,h1);
            drawFleet(g,origFleet,displayFleet, canAdjust,0,h1,w,h-h1);
        }
        private void clearButtons() {
            for (int i=0;i<shipBox.length;i++) {
                shipBox[i].setBounds(0,0,0,0);
                minBox[i].reset();
                maxBox[i].reset();
                upBox[i].reset();
                downBox[i].reset();
            }
        }
        private void drawInfo(Graphics2D g, ShipFleet displayFl, int x, int y, int w, int h) {
            textureClip = new Rectangle2D.Float(x,y,w,h);
            g.setColor(MainUI.paneBackground());
            g.fillRect(x, y, w, h);

            int x0 =s10;
            int y0 = s23;
            int lineH = s18;
            String title = displayFl.canBeSentBy(player()) ? text("MAIN_FLEET_DEPLOYMENT") : text("MAIN_FLEET_DISPLAY");
            g.setFont(narrowFont(26));
            drawShadowedString(g, title, 4, x0, y0, SystemPanel.textShadowC, Color.white);

            y0 += s5;
            y0 += lineH;

            StarSystem dest = parent.displayedDestination();
            String text = null;
            String text2 = null;
            g.setFont(narrowFont(16));
            if (displayFl.canBeSentBy(player())) {
                if (!displayFl.canSendTo(id(dest))) {
                    g.setColor(SystemPanel.blackText);
                    if (dest == null)
                        text = "";
                    else {
                        String name = player().sv.name(dest.id);
                        if (name.isEmpty())
                            text = text("MAIN_FLEET_INVALID_DESTINATION2");
                        else 
                            text = text("MAIN_FLEET_INVALID_DESTINATION", name);
                    }
                }
                else if (displayFl.isDeployed() || displayFl.isInTransit()) {
                    dest = dest == null ? displayFl.destination() : dest;
                    int dist = displayFl.travelTurns(dest);
                    String destName = player().sv.name(dest.id);
                    if (destName.isEmpty())
                        text = text("MAIN_FLEET_ETA_UNNAMED", dist);
                    else
                        text = text("MAIN_FLEET_ETA_NAMED", destName, dist);
                    if (displayFl.passesThroughNebula(dest))
                        text2 = text("MAIN_FLEET_THROUGH_NEBULA");
                }
                else if (displayFl.canSendTo(id(dest))) {
                    if (displayFl.canReach(dest)) {
                        g.setColor(SystemPanel.blackText);
                        int dist = displayFl.travelTurns(dest);
                        String destName = player().sv.name(dest.id);
                        if (destName.isEmpty())
                            text = text("MAIN_FLEET_ETA_UNNAMED", dist);
                        else
                            text = text("MAIN_FLEET_ETA_NAMED", destName, dist);
                    }
                    else {
                        int dist = player().rangeTo(dest);
                        g.setColor(SystemPanel.blackText);
                        text = text("MAIN_FLEET_OUT_OF_RANGE_DESC", dist);
                    }
                    if (displayFl.passesThroughNebula(dest))
                        text2 = text("MAIN_FLEET_THROUGH_NEBULA");
                }
                else if (displayFl.isOrbiting()) {
                    g.setColor(SystemPanel.blackText);
                    text = text("MAIN_FLEET_CHOOSE_DEST");
                }
            }
            else if (displayFl.isInTransit()) {
                if (player().knowETA(displayFl)) {
                    g.setColor(Color.black);
                    int dist = displayFl.travelTurnsRemaining();
                    if (displayFl.hasDestination()) {
                        String destName = player().sv.name(displayFl.destSysId());
                        if (destName.isEmpty())
                            text = text("MAIN_FLEET_ETA_UNNAMED", dist);
                        else
                            text = text("MAIN_FLEET_ETA_NAMED", destName, dist);
                    }
                }
                else {
                    g.setColor(SystemPanel.redText);
                    text = text("MAIN_FLEET_ETA_UNKNOWN");
                }
            }
            if (text != null) {
                List<String> lines = wrappedLines(g, text, w-s30);
                for (String line: lines) {
                    g.drawString(line, x0, y0);
                    y0 += lineH;
                }
            }
            if (text2 != null) {
                g.setColor(SystemPanel.redText);
                List<String> lines = wrappedLines(g, text2, w-s30);
                for (String line: lines) {
                    g.drawString(line, x0, y0);
                    y0 += lineH;
                }
            }
        }
        private void drawFleet(Graphics2D g, ShipFleet origFl, ShipFleet displayFl, boolean canAdjust, int x, int y, int w, int h) {
            // draw star background
            g.setColor(Color.black);
            g.fillRect(x, y, w, h);
            if (starImg == null) {
                starImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                drawBackgroundStars(starImg, null);
            }
            g.drawImage(starImg,x,y,null);

            int spacing = s15;
            // figure out size of ships
            int shipW = w/2;
            int shipH = (h/3)-spacing-spacing; // give room for text above/below ship
            if  ((shipH *3/2) <  shipW)
                shipW = shipH * 3/2;
            else
                shipH = shipW * 2/3;

            int xAdj = (w-(shipW*2))/3;
            int yAdj = (h-shipH*2)/3;
            int midX = x+(w/2);
            int midY = y+(h/2);
            int leftX = x+(shipW/2);
            int rightX = x+w-(shipW/2);

            int topY = y+(shipH/2)+spacing;
            int botY = y+h-(shipH/2)-spacing;

            // get count of all stacks based on design visibility
            int[] visible = origFl.visibleShips(player().id);
            // count how many of those visible designs have ships
            int num = 0;
            for (int cnt: visible) {
                if (cnt > 0)
                    num++;
            }
            
            boolean sameFleet = (origFl.empId() == displayFl.empId()) && (origFl.sysId() == displayFl.sysId()) && (origFl.destSysId() == displayFl.destSysId());
            boolean showAdjust = canAdjust && sameFleet;
            if (origFl != displayFl) {
                int i = 0;
            }
            switch(num) {
                case 0:
                    break;
                case 1:
                    drawShip(g, origFl, displayFl, showAdjust, 0, midX, midY, shipW, shipH);
                    break;
                case 2:
                    drawShip(g, origFl, displayFl, showAdjust, 0, leftX+xAdj, topY+yAdj, shipW, shipH);
                    drawShip(g, origFl, displayFl, showAdjust, 1, rightX-xAdj, botY-yAdj, shipW, shipH);
                    break;
                case 3:
                    drawShip(g, origFl, displayFl, showAdjust, 0, leftX, topY, shipW, shipH);
                    drawShip(g, origFl, displayFl, showAdjust, 1, midX, midY, shipW, shipH);
                    drawShip(g, origFl, displayFl, showAdjust, 2, rightX, botY, shipW, shipH);
                    break;
                case 4:
                    drawShip(g, origFl, displayFl, showAdjust, 0, leftX+xAdj, topY+yAdj, shipW, shipH);
                    drawShip(g, origFl, displayFl, showAdjust, 1, rightX-xAdj, topY+yAdj, shipW, shipH);
                    drawShip(g, origFl, displayFl, showAdjust, 2, leftX+xAdj, botY-yAdj, shipW, shipH);
                    drawShip(g, origFl, displayFl, showAdjust, 3, rightX-xAdj, botY-yAdj, shipW, shipH);
                    break;
                case 5:
                    drawShip(g, origFl, displayFl, showAdjust, 0, leftX, topY, shipW, shipH);
                    drawShip(g, origFl, displayFl, showAdjust, 1, rightX, topY, shipW, shipH);
                    drawShip(g, origFl, displayFl, showAdjust, 2, midX, midY, shipW, shipH);
                    drawShip(g, origFl, displayFl, showAdjust, 3, leftX, botY, shipW, shipH);
                    drawShip(g, origFl, displayFl, showAdjust, 4, rightX, botY, shipW, shipH);
                    break;
                case 6:
                default:
                    drawShip(g, origFl, displayFl, showAdjust, 0, leftX+xAdj, topY, shipW, shipH);
                    drawShip(g, origFl, displayFl, showAdjust, 1, rightX-xAdj, topY, shipW, shipH);
                    drawShip(g, origFl, displayFl, showAdjust, 2, leftX+xAdj, midY, shipW, shipH);
                    drawShip(g, origFl, displayFl, showAdjust, 3, rightX-xAdj, midY, shipW, shipH);
                    drawShip(g, origFl, displayFl, showAdjust, 4, leftX+xAdj, botY, shipW, shipH);
                    drawShip(g, origFl, displayFl, showAdjust, 5, rightX-xAdj, botY, shipW, shipH);
                    break;
            }
        }
        private void drawShip(Graphics2D g, ShipFleet origFl, ShipFleet displayFl, boolean canAdjust, int i, int x0, int y0, int w, int h) {
            int x = x0-w/2;
            int y = y0-h/2;
            g.setColor(fleetBackC);
            Stroke prev = g.getStroke();
            g.setStroke(stroke2);
            g.drawRoundRect(x,y-s10,w,h+s20,s20,s20);
            g.setStroke(prev);

            ShipDesign d = origFl.visibleDesign(player().id,i);
            Image img = d.image();
            int imgW = img.getWidth(null);
            int imgH = img.getHeight(null);
            float scale = min((float)w/imgW, (float)h/imgH);

            int w1 = (int)(scale*imgW);
            int h1 = (int)(scale*imgH);

            int x1 = x+((w-w1)/2);
            int y1 = y+((h-h1)/2);
            g.drawImage(img, x1, y1, x1+w1, y1+h1, 0, 0, imgW, imgH, parent);

            // draw ship name
            this.scaledFont(g, d.name(), w-s5, 18, 14);
            //g.setFont(narrowFont(18));
            int sw = g.getFontMetrics().stringWidth(d.name());
            int x2 = x+((w-sw)/2);
            g.setColor(SystemPanel.grayText);
            g.drawString(d.name(), x2, y+s5);

            int y3 = y+h+s7;

            Color c0 = shipBox[i] == hoverBox ? SystemPanel.yellowText : SystemPanel.grayText;
            Color c1;

            int a[] = new int[3];
            int b[] = new int[3];

            // draw adjustment arrows
            if (canAdjust) {
                g.setColor(buttonBackC);
                g.fillRoundRect(x, y3-s15, w, s18, s20, s20);
                g.setColor(c0);
                b[0]=y3-s6;  b[1]=y3-s12; b[2]=y3;
                // draw min box
                c1 = hoverBox2 == minBox[i] ? SystemPanel.yellowText : SystemPanel.grayText;
                g.setColor(c1);
                a[0]=x+s5; a[1]=x+s15; a[2]=x+s15;
                minBox[i].addPoint(a[0], b[0]);
                minBox[i].addPoint(a[1], b[1]);
                minBox[i].addPoint(a[2], b[2]);
                g.fill(minBox[i]); g.fillRect(a[0], b[1], s2, b[2]-b[1]);
                // draw left box
                c1 = hoverBox2 == downBox[i] ? SystemPanel.yellowText : SystemPanel.grayText;
                g.setColor(c1);
                a[0]=x+s17; a[1]=x+s27; a[2]=x+s27;
                downBox[i].addPoint(a[0], b[0]);
                downBox[i].addPoint(a[1], b[1]);
                downBox[i].addPoint(a[2], b[2]);
                g.fill(downBox[i]);
                // draw max box
                c1 = hoverBox2 == maxBox[i] ? SystemPanel.yellowText : SystemPanel.grayText;
                g.setColor(c1);
                a[0]=x+w-s5; a[1]=x+w-s15; a[2]=x+w-s15;
                maxBox[i].addPoint(a[0], b[0]);
                maxBox[i].addPoint(a[1], b[1]);
                maxBox[i].addPoint(a[2], b[2]);
                g.fill(maxBox[i]); g.fillRect(a[0]-s2, b[1], s2, b[2]-b[1]);
                // draw up box
                c1 = hoverBox2 == upBox[i] ? SystemPanel.yellowText : SystemPanel.grayText;
                g.setColor(c1);
                a[0]=x+w-s17; a[1]=x+w-s27; a[2]=x+w-s27;
                upBox[i].addPoint(a[0], b[0]);
                upBox[i].addPoint(a[1], b[1]);
                upBox[i].addPoint(a[2], b[2]);
                g.fill(upBox[i]);
            }

            // draw ship count
            g.setColor(c0);
            // format ship count
            int count2 = origFl.num(d.id());
            int count1 = canAdjust ? displayFl.num(d.id()) : count2;
            String s = count1 == count2 ? str(count1) : text("MAIN_FLEET_SHIP_COUNT", count1,count2);
            this.scaledFont(g, s, w-s60, 18, 12);
            int sw3 = g.getFontMetrics().stringWidth(s);
            int x3 = x+((w-sw3)/2);
            g.drawString(s, x3, y3);

            // if hovering, draw highlight frame
            if (hoverBox == shipBox[i]) {
                prev = g.getStroke();
                g.setStroke(stroke1);
                g.setColor(SystemPanel.yellowText);
                g.drawRoundRect(x,y-s10,w,h+s20,s20,s20);
                g.setStroke(prev);
            }
            shipBox[i].setBounds(x,y-s10,w,h+s20);
        }
        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            int count = e.getUnitsToScroll();
            if (count == 0)
                return;
            if (hoverStackNum < 0)
                return;

            ShipFleet fl = selectedFleet();
            if (fl == null)
                return;

            ShipDesign d = fl.visibleDesign(player().id, hoverStackNum);
            if (d == null)
                return;
            int index = d.id();
            int stackNum = fl.num(index);
            int currAdj = stackAdjustment[index];
            int n = stackNum+currAdj;
            int delta = n>30000 ? 10000 : (n>3000 ? 1000 : (n>300 ? 100 : (n>30 ? 10:1)));
            if (count > 0)
                stackAdjustment[index] = max(0-stackNum, currAdj-delta);
            else if (count < 0)
                stackAdjustment[index] = min(0, currAdj+delta);

            adjustedFleet(newAdjustedFleet());
            if (((stackNum + currAdj) == 0)
            || (stackNum + stackAdjustment[index]) == 0)
                repaint();
            else if (currAdj != stackAdjustment[index])
                repaint();
        }
        @Override
        public void mouseDragged(MouseEvent e) { }
        @Override
        public void mouseMoved(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();
            Shape prevHover = hoverBox;
            Shape prevHover2 = hoverBox2;
            hoverBox = null;
            hoverBox2 = null;
            hoverStackNum = -1;
            for (int i=0;i<shipBox.length;i++) {
                if (shipBox[i].contains(x,y)) {
                    hoverBox = shipBox[i];
                    hoverStackNum = i;
                }
                if (minBox[i].contains(x,y))
                    hoverBox2 = minBox[i];
                if (downBox[i].contains(x,y))
                    hoverBox2 = downBox[i];
                if (upBox[i].contains(x,y))
                    hoverBox2 = upBox[i];
                if (maxBox[i].contains(x,y))
                    hoverBox2 = maxBox[i];
            }
            if ((hoverBox != prevHover)
            || (hoverBox2 != prevHover2))
                repaint();
        }
        @Override
        public void mouseClicked(MouseEvent e) { }
        @Override
        public void mouseEntered(MouseEvent e) { }
        @Override
        public void mouseExited(MouseEvent e) {
            if ((hoverBox != null) || (hoverBox2 != null)){
                hoverBox = null;
                hoverBox2 = null;
                repaint();
            }
        }
        @Override
        public void mousePressed(MouseEvent e) { }
        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getButton() > 3)
                return;
            int x = e.getX();
            int y = e.getY();
            ShipFleet fl = selectedFleet();
            // selectedFleet can be null if hovering with mouse
            if ((fl == null) || (hoverStackNum < 0) )
                return;
            ShipDesign d = fl.visibleDesign(player().id, hoverStackNum);
            int index = d.id();
            int stackNum = fl.num(index);
            int currAdj = stackAdjustment[index];
            int newAdj = 1;
            for (int i=0;i<shipBox.length;i++) {
                if (minBox[i].contains(x,y))
                    newAdj = 0-stackNum;
                else if (downBox[i].contains(x,y))
                    newAdj = max(currAdj-1, 0-stackNum);
                else if (upBox[i].contains(x,y))
                    newAdj = min(currAdj+1, 0);
                else if (maxBox[i].contains(x,y))
                            newAdj = 0;
            }

            // nothing in click range
            if (newAdj > 0)
                return;

            if (newAdj == stackAdjustment[index])
                misClick();
            else {
                stackAdjustment[index] = newAdj;
                adjustedFleet(newAdjustedFleet());
                softClick();
                repaint();
            }
        }
    }
    public class FleetButtonPane extends BasePanel implements MouseListener, MouseMotionListener {
        private static final long serialVersionUID = 1L;
        private final FleetPanel parent;
        private final Color buttonShadowC = new Color(33,33,33);
        int leftM, midM1, midM2, rightM;
        private LinearGradientPaint fullGrayBackC;
        private LinearGradientPaint largeGreenBackC;
        private LinearGradientPaint largeRedBackC;
        private LinearGradientPaint smallGrayBackC;
        private boolean initted = false;

        private Shape hoverBox;
        private final Rectangle cancelBox = new Rectangle();
        private final Rectangle deployBox = new Rectangle();
        private final Rectangle undeployBox = new Rectangle();
        public FleetButtonPane(FleetPanel p) {
            parent = p;
            init();
        }
        @Override
        public String textureName()            { return TEXTURE_GRAY; }

        private void init() {
            addMouseListener(this);
            addMouseMotionListener(this);
        }
        private void initGradients() {
            initted = true;
            int w = getWidth();
            leftM = s2;
            midM1 = (w*3/5)-s2;
            midM2 = midM1+s4;
            rightM = w-s2;
            Point2D start = new Point2D.Float(leftM, 0);
            Point2D mid1 = new Point2D.Float(midM1, 0);
            Point2D mid2 = new Point2D.Float(midM2, 0);
            Point2D end = new Point2D.Float(rightM, 0);
            float[] dist = {0.0f, 0.5f, 1.0f};

            Color grayEdgeC = new Color(59,59,59);
            Color grayMidC = new Color(92,92,92);
            Color[] grayColors = {grayEdgeC, grayMidC, grayEdgeC };

            Color greenEdgeC = new Color(44,59,30);
            Color greenMidC = new Color(71,93,48);
            Color[] greenColors = {greenEdgeC, greenMidC, greenEdgeC };

            Color redEdgeC = new Color(92,20,20);
            Color redMidC = new Color(117,42,42);
            Color[] redColors = {redEdgeC, redMidC, redEdgeC };

            fullGrayBackC = new LinearGradientPaint(start, end, dist, grayColors);
            smallGrayBackC = new LinearGradientPaint(mid2, end, dist, grayColors);
            largeGreenBackC = new LinearGradientPaint(start, mid1, dist, greenColors);
            largeRedBackC = new LinearGradientPaint(start, mid1, dist, redColors);
        }
        @Override
        public void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            super.paintComponent(g);

            if (!initted)
                initGradients();

            ShipFleet fleet = parent.adjustedFleet();
            if (fleet == null)
                return;

            clearButtons();
            if (!fleet.empire().isPlayer()) {
                drawFullCancelButton(g);
                return;
            }

            StarSystem dest = parent.displayedDestination();
            if (dest != null) {
                if (!fleet.canReach(dest))  {
                    drawFullOutOfRangeButton(g);
                    return;
                }
                else if (!fleet.canSendTo(id(dest))) {
                    if (fleet.retreating()) 
                        drawFullInvalidRetreatButton(g);
                    else
                        drawFullCancelButton(g);
                    return;
                }
                else {
                    drawLargeDeployButton(g);
                    drawSmallCancelButton(g);
                    return;
                }
            }
            if (fleet.canUndeploy()) {
                drawLargeUndeployButton(g);
                drawSmallCancelButton(g);
                return;
            }
            drawFullCancelButton(g);
        }
        private void clearButtons() {
            cancelBox.setBounds(0,0,0,0);
            deployBox.setBounds(0,0,0,0);
            undeployBox.setBounds(0,0,0,0);
        }
        private void drawFullOutOfRangeButton(Graphics2D g) {
            drawButton(g,fullGrayBackC,text("MAIN_FLEET_OUT_OF_RANGE"), cancelBox, leftM, rightM);
        }
        private void drawFullInvalidRetreatButton(Graphics2D g) {
            drawButton(g,fullGrayBackC,text("MAIN_FLEET_INVALID_RETREAT"), cancelBox, leftM, rightM);
        }
        private void drawFullCancelButton(Graphics2D g) {
            drawButton(g,fullGrayBackC,text("MAIN_FLEET_CANCEL"), cancelBox, leftM, rightM);
        }
        private void drawLargeUndeployButton(Graphics2D g) {
            drawButton(g,largeRedBackC,text("MAIN_FLEET_UNDEPLOY_FLEET"), undeployBox, leftM, midM1);
        }
        private void drawLargeDeployButton(Graphics2D g) {
            drawButton(g,largeGreenBackC,text("MAIN_FLEET_DEPLOY_FLEET"), deployBox, leftM, midM1);
        }
        private void drawSmallCancelButton(Graphics2D g) {
            drawButton(g,smallGrayBackC,text("MAIN_FLEET_CANCEL"), cancelBox, midM2, rightM);
        }
        private void drawButton(Graphics2D g, LinearGradientPaint gradient, String label, Rectangle actionBox, int x1, int x2) {
            int y = s4;
            int h = getHeight()-s7;
            int w = x2 - x1;
            if (actionBox != null)
                actionBox.setBounds(x1,y,w,h);
            g.setColor(buttonShadowC);
            Stroke prev = g.getStroke();
            g.setStroke(stroke2);
            g.drawRoundRect(x1+s3,y+s2,w-s2,h,s10,s10);
            g.setStroke(prev);

            g.setPaint(gradient);
            g.fillRoundRect(x1,y,w,h,s10,s10);

            boolean hovering = (actionBox != null) && (actionBox == hoverBox);
            Color c0 = hovering ? SystemPanel.yellowText : SystemPanel.whiteText;

            g.setFont(narrowFont(22));
            int sw = g.getFontMetrics().stringWidth(label);
            int x0 = x1+((w-sw)/2);
            drawShadowedString(g, label, 3, x0, y+h-s11, SystemPanel.textShadowC, c0);

            g.setColor(c0);
            Stroke prev2 = g.getStroke();
            g.setStroke(stroke2);
            g.drawRoundRect(x1+s1,y,w-s2,h,s10,s10);
            g.setStroke(prev2);
        }
        @Override
        public void mouseDragged(MouseEvent e) { }
        @Override
        public void mouseMoved(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();

            Shape prevHover = hoverBox;
            hoverBox = null;
            if (cancelBox.contains(x,y))
                hoverBox = cancelBox;
            else if (deployBox.contains(x,y))
                hoverBox = deployBox;
            else if (undeployBox.contains(x,y))
                hoverBox = undeployBox;

            if (hoverBox != prevHover)
                repaint();
        }
        @Override
        public void mouseClicked(MouseEvent e) { }
        @Override
        public void mouseEntered(MouseEvent e) { }
        @Override
        public void mouseExited(MouseEvent e) {
            if (hoverBox != null) {
                hoverBox = null;
                repaint();
            }
        }
        @Override
        public void mousePressed(MouseEvent e) { }
        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getButton() > 3)
                return;
            int x = e.getX();
            int y = e.getY();

            if (cancelBox.contains(x,y)) {
                softClick();
                parent.cancelFleet();
            }
            else if (deployBox.contains(x,y)) {
                softClick();
                parent.sendFleet();
            }
            else if (undeployBox.contains(x,y)) {
                softClick();
                parent.undeployFleet();
            }
        }
    }
}
