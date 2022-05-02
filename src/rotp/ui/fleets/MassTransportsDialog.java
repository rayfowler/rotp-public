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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import rotp.model.empires.Empire;
import rotp.model.galaxy.StarSystem;
import rotp.ui.BasePanel;
import rotp.ui.main.SystemPanel;
import rotp.ui.sprites.SystemTransportSprite;
import rotp.util.Palette;

public class MassTransportsDialog extends BasePanel {
    private static final long serialVersionUID = 1L;
    static int MAX_ROWS = 12;
    Palette palette;
    FleetUI topParent;
    TransportTargetListingUI listingUI;
    TransportTargetFooterUI footerUI;
    List<StarSystem> sourceSystems = new ArrayList<>();
    boolean synched = false;
    final Color backgroundC = new Color(76,57,41,192);

    public MassTransportsDialog(FleetUI p) {
        topParent = p;
        palette = Palette.named("Brown");
        init();
    }
    private void init() {
        setOpaque(true);
        initModel();
    }
    private int totalTransports() {
        int amt = 0;
        for (StarSystem sys : sourceSystems) {
            StarSystem dest = sys.transportSprite().starSystem();
            if ((dest == null) || (dest == topParent.targetSystem))
                amt += sys.transportSprite().amt();
        }
        return amt;
    }
    private int minTransportTime() {
        float time = Float.MAX_VALUE;
        for (StarSystem sys : sourceSystems) {
            StarSystem dest = sys.transportSprite().starSystem();
            if ((dest == null) || (dest == topParent.targetSystem)) {
                if (sys.transportSprite().amt() > 0)
                    time = min(time, sys.transportTimeTo(topParent.targetSystem));
            }
        }
        return (int) Math.ceil(time);
    }
    private int maxTransportTime() {
        float time = 0;
        for (StarSystem sys : sourceSystems) {
            StarSystem dest = sys.transportSprite().starSystem();
            if ((dest == null) || (dest == topParent.targetSystem)) {
                if (sys.transportSprite().amt() > 0)
                    time = max(time, sys.transportTimeTo(topParent.targetSystem));
            }
        }
        return (int) Math.ceil(time);
    }
    void cancelChanges() {
        for (StarSystem sys : sourceSystems)
            sys.transportSprite().cancel();
    }
    void sendTransports() {
        List<StarSystem> launchPoints = new ArrayList<>();
        for (StarSystem sys : sourceSystems) {
            SystemTransportSprite spr = sys.transportSprite();
            if ((spr.amt() > 0) && (spr.starSystem() == null)) {
                sys.transportSprite().clickedDest(topParent.targetSystem);
                launchPoints.add(sys);
            }
        }
        player().deployTransports(launchPoints, topParent.targetSystem, synched);
    }
    void clickSynch() {
        synched = !synched;
    }
    public void mouseWheelMoved(MouseWheelEvent e) {
        listingUI.mouseWheelMoved(e);
    }
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(backgroundC);
        g.fillRect(0, 0, getWidth(), getHeight());
    }
    public void initSystems() {
        StarSystem.TARGET_SYSTEM = topParent.targetSystem;
        Empire pl = player();
        sourceSystems.clear();
        sourceSystems.addAll(topParent.filteredSystems);
        sourceSystems.remove(topParent.targetSystem);
        Collections.sort(sourceSystems, StarSystem.TRANSPORT_TIME_TO_TARGET_SYSTEM);

        int listSize = max(6, min(MAX_ROWS,sourceSystems.size()));
        int uiH = s100+s30+(listSize*listingUI.rowHeight());
        setBoundsH(uiH);
    }
    @Override
    public void open() {
        initSystems();
        listingUI.open();
        setVisible(true);
    }
    public void close() {
        setVisible(false);
        listingUI.close();
        footerUI.close();
    }
    private void setBoundsH(int h) {
        setBounds(scaled(150),scaled(100),scaled(730),h);
    }
    private void initModel() {
        setBoundsH(scaled(600));
        setVisible(false);
        setOpaque(false);
        setLayout(new BorderLayout());
        listingUI = new TransportTargetListingUI(topParent);
        listingUI.setBorder(this.newEmptyBorder(0, 60,0,60));

        footerUI = new TransportTargetFooterUI();

        add(new TransportTargetHeaderUI(), BorderLayout.NORTH);
        add(listingUI, BorderLayout.CENTER);
        add(new TransportTargetSideUI(), BorderLayout.EAST);
        add(new TransportTargetSideUI(), BorderLayout.WEST);
        add(footerUI, BorderLayout.SOUTH);
    }
    class TransportTargetHeaderUI extends BasePanel {
        private static final long serialVersionUID = 1L;
        public TransportTargetHeaderUI() {
            initModel();
        }
        private void initModel() {
            setOpaque(false);
            setPreferredSize(new Dimension(getWidth(),s40));
        }
        @Override
        public void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;

            int w = getWidth();
            int h = getHeight();
            g.setFont(narrowFont(30));
            String title = text("FLEETS_TRANSPORTS_TITLE");
            int titleW = g.getFontMetrics().stringWidth(title);
            g.setColor(SystemPanel.orangeText);
            drawString(g,title, s20,h-s10);

            int warnW = w-s20-titleW-s20;
            // draw warning
            int maxAllowed = player().maxTransportsAllowed(topParent.targetSystem);
            if (totalTransports() > maxAllowed) {
                String warning;
                if (maxAllowed == 0)
                    warning = text("FLEETS_TRANSPORT_NO_ROOM");
                else
                    warning = text("FLEETS_TRANSPORT_SIZE_WARNING", str(maxAllowed));
                scaledFont(g, warning, warnW, 20, 14);
                int sw = g.getFontMetrics().stringWidth(warning);
                drawShadowedString(g, warning, 3, w-sw-s10, h-s10, SystemPanel.textShadowC, SystemPanel.redText);
            }
        }
    }
    class TransportTargetListingUI extends SystemListingUI {
        private static final long serialVersionUID = 1L;
        private DataView view;
        private StarSystem selectedSystem;
        private final int[] sysIds;
        private SystemSetTransportsColumn transportsCol;
        TransportTargetListingUI(BasePanel p) {
            super(p);
            sysIds = new int[MAX_ROWS];
        }
        @Override
        public String textureName()           { return TEXTURE_BROWN; }
        @Override
        protected DataView dataView()         { return view; }
        @Override
        protected List<StarSystem> systems()  { return sourceSystems;  }
        @Override
        protected StarSystem lastSelectedSystem() { return selectedSystem != null ? selectedSystem : systems().get(0); }
        @Override
        protected void selectedSystem(StarSystem sys, boolean updateFieldValues) { selectedSystem = sys; }
        @Override
        protected void shiftSelectedSystem(StarSystem sys, boolean updateFieldValues) { selectedSystem = sys; }
        @Override
        protected void controlSelectedSystem(StarSystem sys, boolean updateFieldValues) { selectedSystem = sys; }
        @Override
        protected boolean selectRows()  { return false; }
        @Override
        protected Color selectedC()  { return unselectedC; }
        @Override
        public void close()          {
            transportsCol.targetSystem(null);
            selectedSystem = null;
        }
        @Override
        public void open() {
            transportsCol.targetSystem(topParent.targetSystem);
        }
        @Override
        protected void postInit() {
            Column rowNumCol =  newRowNumColumn("PLANETS_LIST_NUM", 15, RIGHT);
            Column flagCol = newSystemFlagColumn("", "FLAG", 30, palette.black, StarSystem.VFLAG, LEFT);
            Column nameCol = newSystemDataColumn("PLANETS_LIST_NAME", "NAME", 140, palette.black, StarSystem.NAME, LEFT);
            Column populationCol = newSystemDataColumn("PLANETS_LIST_POP", "POPULATION", 60, palette.black, StarSystem.POPULATION, RIGHT);
            Column sizeCol = newSystemDataColumn("PLANETS_LIST_SIZE", "SIZE", 60, palette.black, StarSystem.CURRENT_SIZE, RIGHT);
            Column pTypeCol = newPlanetTypeColumn("PLANETS_LIST_TYPE", "PLANET_TYPE", 90, StarSystem.PLANET_TYPE);
            Column distCol = newSystemDataColumn(Column.YEARS_OR_TURNS, "TRANSPORT_TURNS", 60, palette.black, StarSystem.TRANSPORT_TIME_TO_TARGET_SYSTEM, RIGHT);
            transportsCol = newSystemSetTransportsColumn("PLANETS_LIST_TRANSPORTS", this, 999);

            view = new DataView();
            view.addColumn(rowNumCol);
            view.addColumn(flagCol);
            view.addColumn(nameCol);
            view.addColumn(populationCol);
            view.addColumn(sizeCol);
            view.addColumn(pTypeCol);
            view.addColumn(distCol);
            view.addColumn(transportsCol);

            selectedColumn(distCol);
        }
    }
    class TransportTargetSideUI extends BasePanel {
        private static final long serialVersionUID = 1L;
        public TransportTargetSideUI() {
            init();
        }
        private void init() {
            setPreferredSize(new Dimension(s10,getHeight()));
            setOpaque(false);
        }
    }
    class TransportTargetFooterUI extends BasePanel implements MouseListener, MouseMotionListener {
        private static final long serialVersionUID = 1L;
        private final Color okButtonBdrC = new Color(158,165,156);
        private final Color cancelButtonBdrC = new Color(148,131,112);
        private LinearGradientPaint synchBackC;
        private LinearGradientPaint sendBackC;
        private LinearGradientPaint cancelBackC;
        Rectangle synchButton = new Rectangle();
        Rectangle sendButton = new Rectangle();
        Rectangle cancelButton = new Rectangle();
        Rectangle hoverBox;
        Area textureArea;

        public TransportTargetFooterUI() {
            initModel();
        }
        public void close() {
            // when closing, null out gradients since their width is
            // based on text, which may language change after exiting
            synchBackC = null;
            sendBackC = null;
            cancelBackC = null;
        }
        @Override
        public String textureName()            { return TEXTURE_BROWN; }
        @Override
        public Area textureArea()              { return textureArea; }
        @Override
        public void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;

            int w = getWidth();
            int h = getHeight();

            int totalTransports = totalTransports();
            int minTime = totalTransports == 0 ? 0 : minTransportTime();
            int maxTime = totalTransports == 0 ? 0 : maxTransportTime();
            String transportDetail = "";
            if (totalTransports > 0) {
                if ((synched) || (minTime == maxTime))
                    transportDetail = text("FLEETS_TRANSPORT_TIME", str(maxTime));
                else
                    transportDetail = text("FLEETS_TRANSPORT_TIME_RANGE", str(minTime), str(maxTime));
            }

            g.setFont(narrowFont(20));
            int total = totalTransports();
            String totalStr = total == 0 ? "" : str(total);
            String acceptText = text("FLEETS_TRANSPORTS_SEND", totalStr);
            String cancelText = text("FLEETS_CANCEL");
            int sw1 = g.getFontMetrics().stringWidth(acceptText);
            int sw2 = g.getFontMetrics().stringWidth(cancelText);

            g.setFont(narrowFont(16));
            String synchText = text("FLEETS_TRANSPORTS_SYNCH");
            String unsynchText = text("FLEETS_TRANSPORTS_UNSYNCH");
            int sw3a = g.getFontMetrics().stringWidth(synchText);
            int sw3b = g.getFontMetrics().stringWidth(unsynchText);

            int buttonH = s32;
            int buttonY = h-buttonH-s10;

            int buttonW1 = max(sw1+s30, scaled(120));
            int buttonW2 = max(sw2+s30, scaled(120));
            int buttonW3 = max(sw3a+s15, sw3b+s15, s60);
            int x2 = w-buttonW2-s10;
            int x1 = x2-s10-buttonW1;
            int x3 = s10;

            if (sendBackC == null)
                initGradients(x1,x2,x3,buttonW1,buttonW2,buttonW3);

            int cnr = s2;
            if (totalTransports > 0) {
                // synch/unsynch button
                int buttonH3 = buttonH-s6;
                g.setFont(narrowFont(16));
                synchButton.setBounds(x3, buttonY+s3, buttonW3, buttonH3);
                g.setColor(SystemPanel.textShadowC);
                g.fillRoundRect(x3+s4, buttonY+s4+s3, buttonW3, buttonH3, cnr, cnr);
                g.setPaint(synchBackC);
                g.fillRoundRect(x3, buttonY+s3, buttonW3, buttonH3, cnr, cnr);
                Stroke prev = g.getStroke();
                g.setStroke(stroke1);
                if (hoverBox == synchButton)
                    g.setColor(Color.yellow);
                else
                    g.setColor(okButtonBdrC);
                g.drawRoundRect(x3, buttonY+s3, buttonW3, buttonH3, cnr, cnr);
                g.setStroke(prev);
                Color c0 = hoverBox == synchButton ? Color.yellow : SystemPanel.whiteText;
                String button3Text = synched ? unsynchText : synchText;
                int sw3 = synched ? sw3b : sw3a;
                int text3X = x3+((buttonW3 - sw3) / 2);
                drawShadowedString(g0, button3Text, 3, text3X, buttonY+buttonH-s12, SystemPanel.textShadowC, c0);

                // transport detail text
                int x3b = x3+buttonW3+s15;
                int detailW = x1-x3b-s20;
                g.setColor(SystemPanel.whiteText);
                scaledFont(g0, transportDetail, detailW, 18, 16);
                drawString(g,transportDetail, x3b, buttonY+buttonH-s10);
            }

            // transfer button
            g.setFont(narrowFont(20));
            sendButton.setBounds(x1, buttonY, buttonW1, buttonH);
            g.setColor(SystemPanel.textShadowC);
            g.fillRoundRect(x1+s4, buttonY+s4, buttonW1, buttonH, cnr, cnr);
            g.setPaint(sendBackC);
            g.fillRoundRect(x1, buttonY, buttonW1, buttonH, cnr, cnr);
            Stroke prev = g.getStroke();
            g.setStroke(stroke1);
            if (hoverBox == sendButton)
                g.setColor(Color.yellow);
            else
                g.setColor(okButtonBdrC);
            g.drawRoundRect(x1, buttonY, buttonW1, buttonH, cnr, cnr);
            g.setStroke(prev);
            Color c1 = hoverBox == sendButton ? Color.yellow : SystemPanel.whiteText;
            int text1X = x1+((buttonW1 - sw1) / 2);
            drawShadowedString(g0, acceptText, 3, text1X, buttonY+buttonH-s10, SystemPanel.textShadowC, c1);

            textureArea = new Area(new RoundRectangle2D.Float(x1, buttonY, buttonW1, buttonH, cnr, cnr));

            // cancel button
            cancelButton.setBounds(x2, buttonY, buttonW2, buttonH);
            g.setColor(SystemPanel.textShadowC);
            g.fillRoundRect(x2+s4, buttonY+s4, buttonW2, buttonH, cnr, cnr);
            g.setPaint(cancelBackC);
            g.fillRoundRect(x2, buttonY, buttonW2, buttonH, cnr, cnr);
            prev = g.getStroke();
            g.setStroke(stroke1);
            if (hoverBox == cancelButton)
                g.setColor(Color.yellow);
            else
                g.setColor(cancelButtonBdrC);
            g.drawRoundRect(x2, buttonY, buttonW2, buttonH, cnr, cnr);
            g.setStroke(prev);
            Color c2 = hoverBox == cancelButton ? Color.yellow : SystemPanel.whiteText;
            int text2X = x2+ ((buttonW2 - sw2) / 2);
            drawShadowedString(g0, cancelText, 3, text2X, buttonY+buttonH-s10, SystemPanel.textShadowC, c2);

            Area buttonArea = new Area(new RoundRectangle2D.Float(x2, buttonY, buttonW2, buttonH, cnr, cnr));
            textureArea.add(buttonArea);
        }
        private void initModel() {
            setPreferredSize(new Dimension(getWidth(),s60));
            setOpaque(false);
            addMouseListener(this);
            addMouseMotionListener(this);
        }
        private void initGradients(int x1, int x2, int x3, int buttonW1, int buttonW2, int buttonW3) {
            Point2D start1 = new Point2D.Float(x1, 0);
            Point2D end1 = new Point2D.Float(x1+buttonW1, 0);
            Point2D start2 = new Point2D.Float(x2, 0);
            Point2D end2 = new Point2D.Float(x2+buttonW2, 0);
            Point2D start3 = new Point2D.Float(x3, 0);
            Point2D end3 = new Point2D.Float(x3+buttonW3, 0);
            float[] dist = {0.0f, 0.1f, 0.5f, 0.9f, 1.0f};

            Color greenEdgeC = new Color(44,59,30);
            Color greenMidC = new Color(71,93,48);
            Color[] greenColors = {greenEdgeC, greenEdgeC, greenMidC, greenEdgeC, greenEdgeC };

            Color brownEdgeC = new Color(85,59,43);
            Color brownMidC = new Color(146,99,69);
            Color[] brownColors = {brownEdgeC, brownEdgeC, brownMidC, brownEdgeC, brownEdgeC };

            sendBackC = new LinearGradientPaint(start1, end1, dist, greenColors);
            cancelBackC = new LinearGradientPaint(start2, end2, dist, brownColors);
            synchBackC = new LinearGradientPaint(start3, end3, dist, brownColors);
        }
        private void setHoverSprite(int x, int y) {
            hoverBox = null;

            if (cancelButton.contains(x, y))
                hoverBox = cancelButton;
            else if (sendButton.contains(x, y))
                hoverBox = sendButton;
            else if (synchButton.contains(x, y))
                hoverBox = synchButton;
        }
        @Override
        public void mouseClicked(MouseEvent e) {    }
        @Override
        public void mousePressed(MouseEvent e) {    }
        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getButton() > 3)
                    return;
            if (hoverBox == cancelButton) {
                hoverBox = null;
                softClick();
                cancelChanges();
                topParent.closeTransportsDialog();
            }
            else if (hoverBox == sendButton) {
                hoverBox = null;
                softClick();
                sendTransports();
                topParent.clearMapSelections();
                topParent.showQueryPanel();
            }
            else if (hoverBox == synchButton) {
                clickSynch();
                softClick();
                repaint();
            }
        }
        @Override
        public void mouseEntered(MouseEvent e) {    }
        @Override
        public void mouseExited(MouseEvent e) {
            if (hoverBox != null) {
                hoverBox = null;
                repaint();
            }
        }
        @Override
        public void mouseDragged(MouseEvent e) {
            Shape prevHover = hoverBox;
            setHoverSprite(e.getX(),e.getY());

            if (prevHover != hoverBox)
                repaint();
        }
        @Override
        public void mouseMoved(MouseEvent e) {
            Shape prevHover = hoverBox;
            setHoverSprite(e.getX(),e.getY());

            if (prevHover != hoverBox)
                repaint();
        }
    }
}
