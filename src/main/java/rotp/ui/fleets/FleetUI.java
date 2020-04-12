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
package rotp.ui.fleets;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.LinearGradientPaint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLayeredPane;
import javax.swing.border.Border;
import rotp.Rotp;
import rotp.model.Sprite;
import rotp.model.empires.Empire;
import rotp.model.empires.SystemView;
import rotp.model.galaxy.Location;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.StarSystem;
import rotp.model.ships.Design;
import rotp.model.ships.ShipDesign;
import rotp.model.ships.ShipDesignLab;
import rotp.ui.BasePanel;
import rotp.ui.ExitButton;
import rotp.ui.RotPUI;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.main.MainUI;
import rotp.ui.main.SystemPanel;
import rotp.ui.map.IMapHandler;
import rotp.ui.sprites.FlightPathSprite;
import rotp.ui.sprites.MapControlSprite;
import rotp.util.Base;

public final class FleetUI extends BasePanel implements IMapHandler, ActionListener, MouseWheelListener {
    private static final long serialVersionUID = 1L;
    public static final Color backHiC = new Color(178,124,87);
    public static final Color backLoC = new Color(112,85,68);
    public static final Color rallyBackHiC = new Color(192,139,105);
    public static final Color rallyBackLoC = new Color(77,55,34);
    public static final Color rallyBorderC = new Color(208,172,148);
    public static final Color darkShadingC = new Color(50,50,50);
    public static final Color darkBrown = new Color(45,14,5);
    public static final Color brown = new Color(64,24,13);
    public static final Color sliderBoxBlue = new Color(34,140,142);
    public static final Color mapMask = new Color(0,0,0,192);

    private final String QUERY_PANEL = "Query";
    private final String RALLY_PANEL = "Rally";
    private final String DEPLOY_PANEL = "Deploy";
    private final String TRANSPORT_PANEL = "Transport";

    public static FleetUI instance;
    private MainTitlePanel titlePanel;
    private final CardLayout cardLayout = new CardLayout();
    private String currentPane = QUERY_PANEL;

    public HashMap<ShipDesign, BufferedImage> designImageCache = new HashMap<>();
    private int pad = 10;
    private GalaxyMapPanel map;
    private LinearGradientPaint backGradient;

    private final List<Sprite> controls = new ArrayList<>();

    // resources shared by SystemQueryAction panels
    public List<StarSystem> selectedSystems = new ArrayList<>();
    public List<StarSystem> filteredSystems = new ArrayList<>();
    public SystemResourcesFilter resourceFilter = new SystemResourcesFilter();
    public SystemRallyPointFilter rallyPointFilter = new SystemRallyPointFilter();
    public SystemTransportsFilter transportsFilter = new SystemTransportsFilter();
    public SystemStargateFilter stargateFilter = new SystemStargateFilter();
    public ShipDesignFilter[] systemDesignFilters = new ShipDesignFilter[ShipDesignLab.MAX_DESIGNS];
    public Design currDesign;

    // resources shared by FleetQueryAction panels
    public List<ShipFleet> selectedFleets = new ArrayList<>();
    public List<ShipFleet> filteredFleets = new ArrayList<>();
    public FleetHasOrdersFilter inTransitFilter = new FleetHasOrdersFilter();
    public ShipDesignFilter[] fleetDesignFilters = new ShipDesignFilter[ShipDesignLab.MAX_DESIGNS];

    // public for all
    public StarSystem hoverSystem;
    public StarSystem targetSystem;

    private BasePanel dataPanel;
    private BasePanel queryPanel;
    private BasePanel rallyPanel;
    private BasePanel deployPanel;
    private BasePanel transportPanel;
    private FleetMassDeployPanel massDeployPanel;
    private SystemMassRallyPanel massRallyPanel;
    private SystemMassTransportPanel massTransportPanel;
    private MassTransportsDialog massTransportDialog;
    JLayeredPane layers = new JLayeredPane();

    public int SIDE_PANE_W;
    private LinearGradientPaint grayBackC;
    private LinearGradientPaint redBackC;
    private LinearGradientPaint greenBackC;
    private LinearGradientPaint brownBackC;

    public FleetUI() {
        instance = this;
        pad = s10;
        for (int i=0;i<systemDesignFilters.length;i++)
            systemDesignFilters[i] = new ShipDesignFilter(i);
        for (int i=0;i<fleetDesignFilters.length;i++)
            fleetDesignFilters[i] = new ShipDesignFilter(i);
        initModel();
    }
    public void init() {
        if (grayBackC == null)
            initGradients();

        // reset map everytime we open
        sessionVar("FLEETUI_MAP_INITIALIZED", false);
        targetSystem = null;
        map.clearRangeMap();

        selectedSystems.clear();
        selectedSystems.addAll(player().allColonizedSystems());
        for (StarSystem sys: selectedSystems)
            sys.transportSprite().cancel();
        resetAllSystemFilters();
        filterSelectedSystems();

        selectedFleets.clear();
        filteredFleets.clear();
        resetAllFleetFilters();
        showQueryPanel();
    }
    public boolean showingQueryPanel()     { return currentPane.equals(QUERY_PANEL); }
    public boolean showingRallyPanel()     { return currentPane.equals(RALLY_PANEL); }
    public boolean showingTransportPanel() { return currentPane.equals(TRANSPORT_PANEL); }
    public void showQueryPanel()           { showPanel(QUERY_PANEL, queryPanel); }
    public void showDeployPanel()          { showPanel(DEPLOY_PANEL, deployPanel); }
    public void showRallyPanel()           { showPanel(RALLY_PANEL, rallyPanel); }
    public void showTransportPanel()       { showPanel(TRANSPORT_PANEL, transportPanel); }
    @Override
    public boolean allowsDragSelect()      { return true; }

    private boolean mapIsMasked() {
        return massTransportDialog.isVisible();
    }
    private void resetAllSystemFilters() {
        resourceFilter.checked = false;
        rallyPointFilter.checked = false;
        transportsFilter.checked = false;
        stargateFilter.checked = false;
        for (QueryFilter f: systemDesignFilters)
            f.checked = false;
    }
    private void resetAllFleetFilters() {
        resetNonDesignFleetFilters();
        for (QueryFilter f: fleetDesignFilters)
            f.checked = false;
    }
    private void resetNonDesignFleetFilters() {
        inTransitFilter.checked = false;
    }
    public boolean isSelectedSystem(StarSystem sv) {
        return filteredSystems.contains(sv);
    }
    public boolean isSelectedFleet(ShipFleet fl) {
        return filteredFleets.contains(fl);
    }
    public void systemFiltersChanged() {
        filterSelectedSystems();
        resetSystemRallyPaths();
        if (massTransportDialog.isVisible())
            massTransportDialog.initSystems();
    }
    public void fleetFiltersChanged() {
        filterSelectedFleets();
        resetFleetDeploymentPaths();
    }
    public void clickSystem(StarSystem v, int count) {
        int id = v.id;
        Empire emp = player();
        switch (currentPane) {
            case QUERY_PANEL:
                if (emp.sv.isColonized(id) && (emp.sv.empire(id) == emp)) {
                    if (selectedSystems.contains(v))
                        selectedSystems.remove(v);
                    else
                        selectedSystems.add(v);
                    filterSelectedSystems();
                    repaint();
                }
                break;
            case DEPLOY_PANEL:
                targetSystem = v;
                if (count == 2)
                    massDeployPanel.deploySelectedFleets();
                repaint();
                break;
            case RALLY_PANEL:
                targetSystem = v;
                if (count == 2)
                    massRallyPanel.startRallies();
                repaint();
                break;
            case TRANSPORT_PANEL:
                targetSystem = v;
                massTransportPanel.deployTransports();
                repaint();
                break;
        }
    }

    public void clickFleet(ShipFleet fl) {
        switch (currentPane) {
            case QUERY_PANEL:
            case DEPLOY_PANEL:
                if (!player().assignableFleets().contains(fl))
                    return;
                if (fl.empire().isPlayer()) {
                    if (selectedFleets.contains(fl))
                        selectedFleets.remove(fl);
                    else
                        selectedFleets.add(fl);
                    filterSelectedFleets();
                    repaint();
                }
                break;
        }
    }
    public void filterSelectedSystems() {
        filteredSystems.clear();
        for (StarSystem sv: selectedSystems) {
            if (!resourceFilter.matchesSystem(sv))
                continue;
            if (!stargateFilter.matchesSystem(sv))
                continue;
            if (!rallyPointFilter.matchesSystem(sv))
                continue;
            if (!transportsFilter.matchesSystem(sv))
                continue;
            boolean matchingDesign = false;
            int numChecked = 0;
            for (QueryFilter filt: systemDesignFilters) {
                if (filt.checked)
                    numChecked++;
                matchingDesign = matchingDesign || filt.matchesSystem(sv);
            }
            if ((numChecked == 0) || matchingDesign)
                filteredSystems.add(sv);
        }

        currDesign = filteredSystems.isEmpty() ? null : filteredSystems.get(0).colony().shipyard().design();
        for (StarSystem v: filteredSystems) {
            if (currDesign != v.colony().shipyard().design()) {
                currDesign = null;
                break;
            }
        }
        if ((currDesign != null) && currDesign.scrapped())
            currDesign = null;
        repaint();
    }
    @Override
    public boolean dragSelect(int x0, int y0, int x1, int y1) {
        Rectangle box = new Rectangle(x0,y0,x1-x0,y1-y0);
        boolean added = false;
        List<StarSystem> allSystems = new ArrayList<>(player().allColonizedSystems());
        allSystems.removeAll(selectedSystems);
        for (StarSystem s: allSystems) {
            int mapX = s.mapX(map);    
            int mapY = s.mapY(map);
            if (box.contains(mapX,mapY)) {
                selectedSystems.add(s);
                filteredSystems.add(s);
                added = true;
            }
        }            

        List<ShipFleet> allFleets = player().assignableFleets();
        allFleets.removeAll(selectedFleets);
        for (ShipFleet fl: allFleets) {
            int mapX = fl.mapX(map);   
            int mapY = fl.mapY(map);
            if (box.contains(mapX,mapY)) {
                selectedFleets.add(fl);
                filteredFleets.add(fl);
                added = true;
            }
        }
         return added; 
    }
    public void removeDisbandedFleets() {
        selectedFleets.removeIf(fl->fl.isEmpty());
        filteredFleets.removeIf(fl->fl.isEmpty());
    }
    public void filterSelectedFleets() {
        filteredFleets.clear();
        for (ShipFleet fl: selectedFleets) {
            if (!inTransitFilter.matchesFleet(fl))
                continue;
            boolean matchingDesign = false;
            int numChecked = 0;
            for (ShipDesignFilter filt: fleetDesignFilters) {
                if (filt.checked)
                    numChecked++;
                matchingDesign = matchingDesign || filt.matchesFleet(fl);
            }
            if ((numChecked == 0) || matchingDesign)
                filteredFleets.add(fl);
        }
        log("Filtered Fleets: "+filteredFleets.size());
        repaint();
    }
    public boolean allowedDesign(int n) {
        if (fleetDesignFilters[n].checked) 
            return true;
        
        for (ShipDesignFilter f: fleetDesignFilters) {
            if (f.checked)
                return false;
        }
        return true;
    }
    public List<ShipDesign> selectedSystemDesigns() {
        List<ShipDesign> designs = new ArrayList<>();
        for (ShipDesignFilter f: systemDesignFilters) {
            if (f.checked)
                designs.add(f.design());
        }
        return designs;
    }
    public List<ShipDesign> selectedFleetDesigns() {
        List<ShipDesign> designs = new ArrayList<>();
        for (ShipDesignFilter f: fleetDesignFilters) {
            if (f.checked)
                designs.add(f.design());
        }
        return designs;
    }
    public boolean selectAllSystems() {
        List<StarSystem> allSystems = player().allColonizedSystems();
        if (selectedFleets.size() == allSystems.size())
            return false;

        selectedSystems.clear();
        selectedSystems.addAll(allSystems);
        resetAllSystemFilters();
        return true;
    }
    public boolean deselectAllSystems() {
        if (selectedSystems.isEmpty())
            return false;

        selectedSystems.clear();
        resetAllSystemFilters();
        return true;
    }
    public boolean selectAllFleets() {
        List<ShipFleet> allFleets = player().assignableFleets();
        if (selectedFleets.size() == allFleets.size())
            return false;

        selectedFleets.clear();
        selectedFleets.addAll(allFleets);
        resetAllFleetFilters();
        return true;
    }
    public boolean deselectAllFleets() {
        if (selectedFleets.isEmpty())
            return false;

        selectedFleets.clear();
        resetAllFleetFilters();
        return true;
    }
    public static void drawFilter(Graphics2D g, int x, int y, int w, QueryFilter filt) {
        int boxW = s12;
        int boxX = x+s5;
        int labelX = boxX+boxW+s6;
        filt.setCheckBounds(boxX,y-boxW,boxW,boxW);
        filt.setTextBounds(labelX,y-boxW,w-labelX,boxW);
        Stroke prev = g.getStroke();
        g.setStroke(stroke2);
        g.setColor(FleetUI.backHiC);
        g.fillRect(boxX, y-boxW, boxW, boxW);
        if (filt.hoveringCheck()) {
            g.setColor(Color.yellow);
            g.drawRect(boxX, y-boxW, boxW, boxW);
        }
        if (filt.isChecked()) {
            g.setColor(SystemPanel.whiteText);
            g.drawLine(boxX-s1, y-s6, boxX+s3, y-s3);
            g.drawLine(boxX+s3, y-s3, boxX+boxW, y-s12);
        }
        g.setStroke(prev);
        if (filt.hoveringText())
            g.setColor(Color.yellow);
        else
            g.setColor(SystemPanel.blackText);
        g.drawString(filt.text(), labelX, y-s2);
    }
    public void drawBrownButton(Graphics2D g, String label, Rectangle actionBox, Shape hoverBox, int y) {
        drawButton(g, brownBackC, label, actionBox, hoverBox, y);
    }
    public void drawGrayButton(Graphics2D g, String label, Rectangle actionBox, Shape hoverBox, int y) {
        drawButton(g, grayBackC, label, null, hoverBox, y);
    }
    public void drawGreenButton(Graphics2D g, String label, Rectangle actionBox, Shape hoverBox, int y) {
        drawButton(g, greenBackC, label, actionBox, hoverBox, y);
    }
    public void drawRedButton(Graphics2D g, String label, Rectangle actionBox, Shape hoverBox, int y) {
        drawButton(g, redBackC, label, actionBox, hoverBox, y);
    }
    public void drawButton(Graphics2D g, LinearGradientPaint gradient, String label, Rectangle actionBox, Shape hoverBox, int y) {
        int buttonH = s27;
        int x1 = s3;
        int w1 = SIDE_PANE_W-s18;
        if (actionBox != null)
            actionBox.setBounds(x1,y,w1,buttonH);
        g.setColor(SystemPanel.buttonShadowC);
        g.fillRoundRect(x1+s1,y+s3,w1,buttonH,s8,s8);
        g.fillRoundRect(x1+s2,y+s4,w1,buttonH,s8,s8);

        g.setPaint(gradient);
        g.fillRoundRect(x1,y,w1,buttonH,s5,s5);

        boolean hovering = (actionBox != null) && (actionBox == hoverBox);
        Color c0 = (actionBox == null) ? SystemPanel.grayText : hovering ? SystemPanel.yellowText : SystemPanel.whiteText;

        g.setFont(narrowFont(18));
        int sw = g.getFontMetrics().stringWidth(label);
        int x0 = x1+((w1-sw)/2);
        drawShadowedString(g, label, 3, x0, y+buttonH-s7, SystemPanel.textShadowC, c0);

        g.setColor(c0);
        Stroke prev2 = g.getStroke();
        g.setStroke(stroke1);
        g.drawRoundRect(x1+s1,y,w1-s2,buttonH,s5,s5);
        g.setStroke(prev2);
    }
    private void initGradients() {
        SIDE_PANE_W = scaled(250);
        int w = getWidth();
        int leftM = s2;
        int rightM = w-s2;
        Point2D start = new Point2D.Float(leftM, 0);
        Point2D end = new Point2D.Float(rightM, 0);
        float[] dist = {0.0f, 0.5f, 1.0f};

        Color brownEdgeC = new Color(100,70,50);
        Color brownMidC = new Color(161,110,76);
        Color[] brownColors = {brownEdgeC, brownMidC, brownEdgeC };

        Color grayEdgeC = new Color(59,59,59);
        Color grayMidC = new Color(92,92,92);
        Color[] grayColors = {grayEdgeC, grayMidC, grayEdgeC };

        Color greenEdgeC = new Color(44,59,30);
        Color greenMidC = new Color(71,93,48);
        Color[] greenColors = {greenEdgeC, greenMidC, greenEdgeC };

        Color redEdgeC = new Color(92,20,20);
        Color redMidC = new Color(117,42,42);
        Color[] redColors = {redEdgeC, redMidC, redEdgeC };

        brownBackC = new LinearGradientPaint(start, end, dist, brownColors);
        grayBackC = new LinearGradientPaint(start, end, dist, grayColors);
        greenBackC = new LinearGradientPaint(start, end, dist, greenColors);
        redBackC = new LinearGradientPaint(start, end, dist, redColors);
    }
    @Override
    public void paint(Graphics g) {
        checkMapInitialized();
        super.paint(g);
    }
    @Override
    public void paintOverMap(GalaxyMapPanel ui, Graphics2D g) {
        int w = ui.getWidth();
        int h = ui.getHeight();
                           
        if (backGradient == null) {
            Color c0 = Color.black;
            Color c1 = new Color(71,53,39);
            Point2D start = new Point2D.Float(s10, h-scaled(200));
            Point2D end = new Point2D.Float(s10, h-s20);
            float[] dist = {0.0f, 0.7f, 1.0f};
            Color[] colors = {c0, c0, c1 };
            backGradient = new LinearGradientPaint(start, end, dist, colors);
        }
        g.setPaint(backGradient);
        Area a = new Area(new Rectangle(0,0,w,h));
        a.subtract(new Area(new Rectangle(s10, s40,w-scaled(295), h-s70)));
        g.fill(a);
        if (mapIsMasked()) {
            g.setColor(mapMask);
            g.fillRect(0,0,w,h);
        }
    }
    @Override
    public Color shadeC()                          { return rallyBackLoC; }
    @Override
    public Color backC()                           { return rallyBackHiC; }
    @Override
    public Color lightC()                          { return rallyBorderC; }
    @Override
    public boolean drawMemory()            { return true; }
    @Override
    public GalaxyMapPanel map()         { return map; }
    private void initModel() {
        int w = scaled(Rotp.IMG_W);
        int h = scaled(Rotp.IMG_H);
        int rightPaneW = scaled(250);

        setBackground(Color.black);
        Border emptyBorder = newEmptyBorder(0,pad,pad,pad);
        setBorder(emptyBorder);

        map = new GalaxyMapPanel(this);
        map.setBorder(null);
        map.setBounds(0,0,w,h);

        titlePanel = new MainTitlePanel("FLEETS_TITLE");
        titlePanel.setBounds(0,0,w-rightPaneW-s25, s45);
        
        massTransportDialog = new MassTransportsDialog(this);

        massDeployPanel = new FleetMassDeployPanel(this);
        massRallyPanel = new SystemMassRallyPanel(this);
        massTransportPanel = new SystemMassTransportPanel(this);
        queryPanel = dataPanelWith(new SystemMassQueryPanel(this), new FleetMassQueryPanel(this), s10);
        rallyPanel = dataPanelWith(new SystemMassQueryPanel(this), massRallyPanel, 0);
        deployPanel = dataPanelWith(new FleetMassQueryPanel(this), massDeployPanel, s10);
        transportPanel = dataPanelWith(new SystemMassQueryPanel(this), massTransportPanel, 0);

        dataPanel = new BasePanel();
        dataPanel.setLayout(cardLayout);
        dataPanel.add(queryPanel, QUERY_PANEL);
        dataPanel.add(rallyPanel, RALLY_PANEL);
        dataPanel.add(deployPanel, DEPLOY_PANEL);
        dataPanel.add(transportPanel, TRANSPORT_PANEL);

        BasePanel rightPanel = new BasePanel();
        rightPanel.setBounds(w-rightPaneW-s25,0,rightPaneW,h-s20);
        rightPanel.setOpaque(false);
        rightPanel.setLayout(new BorderLayout(0, pad));
        rightPanel.add(dataPanel, BorderLayout.CENTER);
        rightPanel.add(new ExitFleetsButton(w, s60, s10, s2), BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(layers, BorderLayout.CENTER);

        layers.add(titlePanel, new Integer(3));
        layers.add(rightPanel,new Integer(2));
        layers.add(massTransportDialog, new Integer(1));
        layers.add(map, new Integer(0));
        Border line1 = newLineBorder(newColor(60,60,60),2);
        Border line2 = newLineBorder(newColor(0,0,0),8);
        Border compound1 = BorderFactory.createCompoundBorder(line2, line1);
        setBorder(compound1);
        setOpaque(false);

        addMouseWheelListener(this);
    }
    private void showPanel(String panelName, BasePanel panel)   {
        currentPane = panelName;
        cardLayout.show(dataPanel, panelName);
    }
    public void openTransportsDialog() {
        massTransportDialog.open();
        repaint();
    }
    public void closeTransportsDialog() {
        massTransportDialog.close();
        repaint();
    }
    private BasePanel dataPanelWith(BasePanel top, BasePanel bottom, int vGap) {
        BasePanel pan = new BasePanel();
        pan.setOpaque(true);
        pan.setBorder(newEmptyBorder(6,6,6,6));
        pan.setBackground(FleetUI.backHiC);
        GridLayout lay = new GridLayout(2,1);
        lay.setVgap(vGap);
        pan.setLayout(lay);
        pan.add(top);
        pan.add(bottom);
        return pan;
    }
    public void clearMapSelections() {
        targetSystem = null;
        hoverSystem = null;
        closeTransportsDialog();
        List<FlightPathSprite> paths = FlightPathSprite.workingPaths();
        paths.clear();
    }
    public void resetFleetDeploymentPaths() {
        StarSystem target = null;
        if (hoverSystem != null)
            target = hoverSystem;
        else if (targetSystem != null)
            target = targetSystem;

        List<FlightPathSprite> sprites = FlightPathSprite.workingPaths();
        sprites.clear();
        if (target != null) {
            for (ShipFleet fl: filteredFleets)
                if (fl.canSend())
                    sprites.add(new FlightPathSprite(fl, target));
        }
    }
    private void resetSystemRallyPaths() {
        StarSystem target = null;
        if (hoverSystem != null)
            target = hoverSystem;
        else if (targetSystem != null)
            target = targetSystem;

        List<FlightPathSprite> sprites = FlightPathSprite.workingPaths();
        sprites.clear();
        if (target != null) {
            for (StarSystem sys: filteredSystems)
                sprites.add(new FlightPathSprite(sys, target));
        }
    }
    private void resetTransportPaths() {
        StarSystem target = null;
        if (hoverSystem != null)
            target = hoverSystem;
        else if (targetSystem != null)
            target = targetSystem;

        List<FlightPathSprite> sprites = FlightPathSprite.workingPaths();
        sprites.clear();
        if (target != null) {
            for (StarSystem sys: filteredSystems)
                sprites.add(new FlightPathSprite(sys, target));
        }
    }
    @Override
    public void checkMapInitialized() {
        Boolean inited = (Boolean) sessionVar("FLEETUI_MAP_INITIALIZED");
        if ((inited == null) || (inited == false)) {
            // init appropriate scale and bounds
            //map.toggleFlightPathDisplay(false);
            sessionVar("FLEETUI_MAP_INITIALIZED", true);
        }
    }
    @Override
    public Color alertColor(SystemView sv)            { 
        if (sv.isAlert())
            return MainUI.redAlertC;
        return null; 
    }
    @Override
    public boolean forwardMouseEvents()             { return mapIsMasked(); }
    @Override
    public boolean canChangeMapScales()                 { return true; }
    @Override
    public void clickingOnSprite(Sprite o, int count, boolean rightClick, boolean sound) {
        if (mapIsMasked())
            return;
        if (o instanceof StarSystem) {
            clickSystem((StarSystem) o, count);
            repaint();
            return;
        }
        else if (o instanceof ShipFleet) {
            clickFleet((ShipFleet) o);
            repaint();
            return;
        }
        o.click(map, count, rightClick, sound);
        repaint();
    }
    public Sprite lastHoveringSprite()       { return (Sprite) sessionVar("MAINUI_LAST_HOVERING_SPRITE"); }
    public void lastHoveringSprite(Sprite s) { sessionVar("MAINUI_LAST_HOVERING_SPRITE", s); }
    @Override
    public void hoveringOverSprite(Sprite o) {
        if (o == lastHoveringSprite())
            return;

        if (lastHoveringSprite() != null)
            lastHoveringSprite().mouseExit(map);
        lastHoveringSprite(o);
        
        if (mapIsMasked())
            return;
        if (o == null) {
            if (hoverSystem != null) {
                hoverSystem = null;
                switch(currentPane) {
                    case DEPLOY_PANEL:
                        resetFleetDeploymentPaths();
                        break;
                    case RALLY_PANEL:
                        resetSystemRallyPaths();
                        break;
                    case TRANSPORT_PANEL:
                        resetTransportPaths();
                        break;
                }
                repaint();
            }
            return;
        }
        if (o instanceof MapControlSprite) {
            o.mouseEnter(map);
            repaint();
            return;
        }
        if (o instanceof StarSystem) {
            StarSystem sys = (StarSystem) o;
            if (sys != hoverSystem) {
                hoverSystem = sys;
                switch(currentPane) {
                   case DEPLOY_PANEL:
                        if (massDeployPanel.canDeployFleets())
                            resetFleetDeploymentPaths();
                        break;
                   case RALLY_PANEL:
                        if (massRallyPanel.canStartRallies())
                            resetSystemRallyPaths();
                        break;
                    case TRANSPORT_PANEL:
                        if (massTransportPanel.canDeployTransports())
                            resetTransportPaths();
                        break;
                }
                repaint();
            }
            return;
        }
    }
    @Override
    public boolean shouldDrawSprite(Sprite s) {
        if (s == null)
            return false;
        if (s instanceof FlightPathSprite) {
            FlightPathSprite fp = (FlightPathSprite) s;
            Sprite fpShip = (Sprite) fp.ship();
            if (isClicked(fpShip) || isHovering(fpShip))
                return true;
            if (isClicked((Sprite) fp.destination()))
                return true;
            if (FlightPathSprite.workingPaths().contains(fp))
                return true;
            if (map.showAllFlightPaths())
                return true;
            if (map.showImportantFlightPaths())
                return fp.isPlayer() || fp.aggressiveToPlayer();
            return false;
        }      
        return true;
    }
    @Override
    public Location mapFocus() {
        Location loc = (Location) sessionVar("MAINUI_MAP_FOCUS");
        if (loc == null) {
            loc = new Location();
            sessionVar("MAINUI_MAP_FOCUS", loc);
        }
        return loc;
    }
    @Override
    public boolean isClicked(Sprite s) {
        if (s == null)
            return false;
        else if (s instanceof StarSystem)
            return isSelectedSystem((StarSystem) s);
        else if (s instanceof ShipFleet)
            return isSelectedFleet((ShipFleet) s);
        return false;
    }
    @Override
    public Sprite clickedSprite()      { return (Sprite) sessionVar("FLEETUI_CLICKED_SPRITE"); }
    @Override
    public void clickedSprite(Sprite s) {
        sessionVar("FLEETUI_CLICKED_SPRITE", s);
    }
    @Override
    public List<Sprite> controlSprites()      { return controls; }
    @Override
    public Border mapBorder()       { return null; }
    @Override
    public float startingScalePct() {
        return (player().maxY()-player().minY()) / map().sizeY();
    }
    @Override
    public Sprite hoveringSprite() { return null; }
    @Override
    public void animate() {
        map.animate();
        queryPanel.animate();
    }
    @Override
    public void keyPressed(KeyEvent e) {
        switch(e.getKeyCode()) {
                case KeyEvent.VK_ESCAPE:
                    if (massTransportDialog.isVisible())
                        closeTransportsDialog();
                    else {
                        clearMapSelections();
                        buttonClick();
                        RotPUI.instance().selectMainPanel(false);
                        return;
                    }
                case KeyEvent.VK_EQUALS:
                    if (e.isShiftDown())  {
                        softClick();
                        map().adjustZoom(-1);
                    }
                    return;
                case KeyEvent.VK_MINUS:
                    softClick();
                    map().adjustZoom(1);
                    return;
                case KeyEvent.VK_UP:
                    softClick();
                    map().dragMap(0, s40);
                    return;
                case KeyEvent.VK_DOWN:
                    softClick();
                    map().dragMap(0, -s40);
                    return;
                case KeyEvent.VK_LEFT:
                    softClick();
                    map().dragMap(s40, 0);
                    return;
                case KeyEvent.VK_RIGHT:
                    softClick();
                    map().dragMap(-s40, 0);
                    return;
        }
    }
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (massTransportDialog.isVisible())
            massTransportDialog.mouseWheelMoved(e);
    }
    class MainTitlePanel extends BasePanel {
        private static final long serialVersionUID = 1L;
        String titleKey;
        public MainTitlePanel(String s) {
            titleKey = s;
            initModel();
        }

        private void initModel() {
            setOpaque(false);
            setPreferredSize(new Dimension(getWidth(),s45));
        }
        @Override
        public void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;

            int w = getWidth();
            int h = getHeight();
            int gap = s10;
            int x0 = gap;
            int y0 = h - s10;
            String title = text(titleKey);
            g.setColor(SystemPanel.orangeText);
            g.setFont(narrowFont(32));
            g.drawString(title, x0,y0);

            g.setColor(backHiC);
            g.fillRect(s10, h-s5, w-s20, s5);
        }

    }
    class ExitFleetsButton extends ExitButton {
        private static final long serialVersionUID = 1L;
        public ExitFleetsButton(int w, int h, int vMargin, int hMargin) {
            super(w, h, vMargin, hMargin);
        }
        @Override
        protected void clickAction(int numClicks) {
            // force recalcuate map bounds when returning
            instance.closeTransportsDialog();
            designImageCache.clear();
            clickedSprite(null);
            clearMapSelections();
            buttonClick();
            RotPUI.instance().selectMainPanel(true);
        }
    }
    public abstract class QueryFilter implements Base {
        Rectangle checkBox = new Rectangle();
        Rectangle textBox = new Rectangle();
        boolean hoverCheck = false;
        boolean hoverText = false;
        boolean checked = false;
        abstract String text();
        protected boolean hasAlternateText()          { return false; }
        public boolean matchesSystem(StarSystem sv) { return true; }
        public boolean matchesFleet(ShipFleet fl)   { return true; }
        public boolean isVisible()                  { return true; }
        public boolean isChecked()                  { return checked; }
        public boolean mouseMoved(int x, int y) {
            boolean oldCheck = hoverCheck;
            boolean oldText = hoverText;
            hoverCheck = checkBox.contains(x,y);
            hoverText = hasAlternateText() && textBox.contains(x,y);
            return (oldCheck != hoverCheck) || (oldText != hoverText);
        }
        public boolean mouseExited() {
            boolean oldCheck = hoverCheck;
            boolean oldText = hoverText;
            hoverCheck = false;
            hoverText = false;
            return oldCheck || oldText;
        }
        public boolean clickText()       { return false; }
        public boolean hoveringText()    { return hoverText; }
        public boolean hoveringCheck()   { return hoverCheck; }
        public void setCheckBounds(int x, int y, int w, int h) {
            checkBox.setBounds(x,y,w,h);
        }
        public void setTextBounds(int x, int y, int w, int h) {
            textBox.setBounds(x,y,w,h);
        }
        public boolean hoveringCheck(int x, int y) { return checkBox.contains(x,y); }
        public boolean hoveringText(int x, int y)  { return hasAlternateText() && textBox.contains(x,y); }
        public boolean click(int x, int y) {
            if (hoveringCheck(x,y)) {
                checked = !checked;
                return true;
            }
            if (QueryFilter.this.hoveringText(x,y)) 
                return clickText();        
            return false;
        }
    }
    public class ShipDesignFilter extends QueryFilter {
        int num;
        public ShipDesignFilter(int i) {
            num = i;
        }
        @Override
        public String text() {
            ShipDesign d = design();
            return d.active() ? text("FLEETS_BUILDING_DESIGN", d.name()) : "";
        }
        @Override
        public boolean matchesSystem(StarSystem sys) {
            return checked && player().sv.isColonized(sys.id) && (sys.colony().shipyard().design() == design());
        }
        @Override
        public boolean matchesFleet(ShipFleet fl) {
            return checked && fl.hasShip(design());
        }
        @Override
        public boolean isVisible()  { return design().active(); }
        private ShipDesign design() { return player().shipLab().design(num); }
    }
    public class SystemStargateFilter extends QueryFilter {
        boolean negated = false;
        @Override
        protected boolean hasAlternateText()          { return true; }
        @Override
        public String text() {
            return negated ? text("FLEETS_HAS_NO_STARGATE") : text("FLEETS_HAS_STARGATE");
        }
        @Override
        public boolean clickText() {
            negated = !negated;
            return true;
        }
        @Override
        public boolean matchesSystem(StarSystem sys) {
            Empire pl = player();
            if (!checked)
                return true;
            if (!pl.sv.isColonized(sys.id) || !(pl.sv.empire(sys.id) == pl))
                return false;
            if (negated)
                return !pl.sv.hasStargate(sys.id);
            else
                return pl.sv.hasStargate(sys.id);
        }
    }
    public class SystemRallyPointFilter extends QueryFilter {
        boolean negated = false;
        @Override
        protected boolean hasAlternateText()          { return true; }
        @Override
        public String text() {
            return negated ? text("FLEETS_HAS_NO_RALLY_POINT") : text("FLEETS_HAS_RALLY_POINT");
        }
        @Override
        public boolean clickText() {
            negated = !negated;
            return true;
        }
        @Override
        public boolean matchesSystem(StarSystem sys) {
            Empire pl = player();
            if (!checked)
                return true;
            if (!pl.sv.isColonized(sys.id) || !(pl.sv.empire(sys.id) == pl))
                return false;
            if (negated)
                return !pl.sv.hasRallyPoint(sys.id);
            else
                return pl.sv.hasRallyPoint(sys.id);
        }
    }
    public class SystemTransportsFilter extends QueryFilter {
        int queryType = 0;
        @Override
        protected boolean hasAlternateText()          { return true; }
        @Override
        public String text() {
            switch(queryType) {
                case 0: return text("FLEETS_SENDING_TRANSPORTS");
                case 1:
                default: return text("FLEETS_NOT_SENDING_TRANSPORTS");
            }
        }
        @Override
        public boolean clickText() {
            queryType++;
            if (queryType > 1)
                queryType = 0;
            return true;
        }
        @Override
        public boolean matchesSystem(StarSystem sys) {
            Empire emp = player();
            if (!checked)
                return true;
            if (!emp.sv.isColonized(sys.id) || !(emp.sv.empire(sys.id) == emp))
                return false;
            switch(queryType) {
                case 0: return emp.sv.colony(sys.id).transporting();
                case 1:
                default: return !emp.sv.colony(sys.id).transporting();
            }
        }
    }
    public class SystemResourcesFilter extends QueryFilter {
        int queryType = 0;
        @Override
        protected boolean hasAlternateText()          { return true; }
        @Override
        public String text() {
            switch(queryType) {
                case 0: return text("FLEETS_IS_ULTRA_RICH");
                case 1: return text("FLEETS_IS_RICH");
                case 2: return text("FLEETS_IS_POOR");
                case 3:
                default: return text("FLEETS_IS_ULTRA_POOR");
            }
        }
        @Override
        public boolean clickText() {
            queryType++;
            if (queryType > 3)
                queryType = 0;
            return true;
        }
        @Override
        public boolean matchesSystem(StarSystem sys) {
            Empire pl = player();
            if (!checked)
                return true;
            if (!pl.sv.isColonized(sys.id) || !(pl.sv.empire(sys.id) == pl))
                return false;
            switch(queryType) {
                case 0: return pl.sv.isUltraRich(sys.id);
                case 1: return pl.sv.isRich(sys.id) || pl.sv.isUltraRich(sys.id);
                case 2: return pl.sv.isPoor(sys.id) || pl.sv.isUltraPoor(sys.id);
                case 3:
                default: return pl.sv.isUltraPoor(sys.id);
            }
        }
    }
    public class FleetHasOrdersFilter extends QueryFilter {
        boolean negated = false;
        @Override
        protected boolean hasAlternateText()          { return true; }
        @Override
        public String text() {
            return negated ? text("FLEETS_HAS_NO_ORDERS") : text("FLEETS_HAS_ORDERS");
        }
        @Override
        public boolean clickText() {
            negated = !negated;
            return true;
        }
        @Override
        public boolean matchesFleet(ShipFleet fl) {
            if (!checked)
                return true;
            if (negated)
                return !fl.inTransit();
            else
                return fl.inTransit();
        }
    }
}
