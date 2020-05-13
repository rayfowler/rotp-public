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
package rotp.ui.planets;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.LinearGradientPaint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import rotp.model.colony.Colony;
import rotp.model.empires.Empire;
import rotp.model.galaxy.StarSystem;
import rotp.model.ships.Design;
import rotp.ui.BasePanel;
import rotp.ui.BaseTextField;
import rotp.ui.ExitButton;
import rotp.ui.RotPUI;
import rotp.ui.SystemViewer;
import rotp.ui.fleets.SystemListingUI;
import rotp.ui.fleets.SystemListingUI.Column;
import rotp.ui.fleets.SystemListingUI.DataView;
import static rotp.ui.fleets.SystemListingUI.LEFT;
import static rotp.ui.fleets.SystemListingUI.RIGHT;
import rotp.ui.main.EmpireColonyFoundedPane;
import rotp.ui.main.EmpireColonyInfoPane;
import rotp.ui.main.EmpireColonySpendingPane;
import rotp.ui.main.MainUI;
import rotp.ui.main.SystemPanel;
import rotp.util.Palette;
import rotp.util.ShadowBorder;

public class PlanetsUI extends BasePanel implements SystemViewer {
    private static final long serialVersionUID = 1L;
    private static final int ECOLOGY_MODE = 1;
    private static final int INDUSTRY_MODE = 2;
    private static final int MILITARY_MODE = 3;
    private static int selectedMode = ECOLOGY_MODE;

    private static final Color selectedC = new Color(178,124,87);
    private static final Color unselectedC = new Color(112,85,68);
    private static final Color darkBrown = new Color(45,14,5);
    private static final Color brown = new Color(64,24,13);
    private static final Color sliderBoxBlue = new Color(34,140,142);

    private static Palette palette;
    private static BaseTextField notesField;
    private static BaseTextField nameField;
    final static int UP_ACTION = 1;
    final static int DOWN_ACTION = 2;
    final static String CANCEL_ACTION = "cancel-input";

    private int pad = 10;
    private List<StarSystem> displayedSystems;
    private final HashMap<Integer, DataView> views = new HashMap<>();

    private final TransferReserveUI transferReservePane;
    private final PlanetDisplayPanel planetDisplayPane;
    private final PlanetViewSelectionPanel viewSelectionPane = new PlanetViewSelectionPanel();
    private EmpireColonySpendingPane spendingPane;

    private final PlanetsUI instance;
    private final PlanetListingUI planetListing;
    private PlanetDataListingUI listingUI;
    private LinearGradientPaint backGradient;

    public PlanetsUI() {
        palette = Palette.named("Brown");
        pad = s10;
        instance = this;

        initTextFields();

        transferReservePane = new TransferReserveUI();
        planetDisplayPane = new PlanetDisplayPanel(this);
        planetListing = new PlanetListingUI(this);

        initModel();
    }
    public void init() {
        displayedSystems = null;
        listingUI.open();
    }
    private void initModel() {
        BasePanel centerPanel = new BasePanel();
        centerPanel.setOpaque(false);
        centerPanel.setLayout(new BorderLayout());
        centerPanel.add(planetListing, BorderLayout.CENTER);
        centerPanel.add(new EmpireRevenueUI(), BorderLayout.SOUTH);

        BasePanel rightPanel = new BasePanel();
        rightPanel.setOpaque(false);
        rightPanel.setLayout(new BorderLayout(0,s22));
        rightPanel.add(planetDisplayPane, BorderLayout.CENTER);
        rightPanel.add(new ExitPlanetsButton(getWidth(), s60, s10, s2), BorderLayout.SOUTH);

        setBackground(Color.black);
        setBorder(BorderFactory.createEmptyBorder(s10,s10,s10,s10));
        BorderLayout layout = new BorderLayout();
        layout.setVgap(pad);
        layout.setHgap(pad);
        setLayout(layout);
        add(centerPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);

        initDataViews();
    }
    private void initTextFields() {
        notesField = new BaseTextField(this);
        notesField.setLimit(50);
        nameField = new BaseTextField(this);
        nameField.setLimit(24);
        nameField.setBackground(selectedC);
        nameField.setBorder(newEmptyBorder(10,5,0,0));
        nameField.setMargin(new Insets(0, s5, 0, 0));
        nameField.setFont(narrowFont(20));
        nameField.setForeground(palette.black);
        nameField.setCaretColor(palette.black);
        nameField.putClientProperty("caretWidth", s3);
        nameField.setFocusTraversalKeysEnabled(false);
        nameField.setVisible(false);
        nameField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) { setFieldValues(selectedSystem()); }
        });

        InputMap im0 = nameField.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am0 = nameField.getActionMap();
        im0.put(KeyStroke.getKeyStroke("ESCAPE"), CANCEL_ACTION);
        im0.put(KeyStroke.getKeyStroke("UP"), UP_ACTION);
        im0.put(KeyStroke.getKeyStroke("DOWN"), DOWN_ACTION);
        im0.put(KeyStroke.getKeyStroke("TAB"), DOWN_ACTION);
        im0.put(KeyStroke.getKeyStroke("ENTER"), DOWN_ACTION);
        am0.put(UP_ACTION, new UpAction());
        am0.put(DOWN_ACTION, new DownAction());
        am0.put(CANCEL_ACTION, new CancelAction());

        notesField.setBackground(selectedC);
        notesField.setBorder(newEmptyBorder(10,5,0,0));
        notesField.setMargin(new Insets(0, s5, 0, 0));
        notesField.setFont(narrowFont(20));
        notesField.setForeground(palette.black);
        notesField.setCaretColor(palette.black);
        notesField.putClientProperty("caretWidth", s3);
        notesField.setFocusTraversalKeysEnabled(false);
        notesField.setVisible(false);
        notesField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) { setFieldValues(selectedSystem()); }
        });

        InputMap im = notesField.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = notesField.getActionMap();
        im.put(KeyStroke.getKeyStroke("ESCAPE"), CANCEL_ACTION);
        im.put(KeyStroke.getKeyStroke("UP"), UP_ACTION);
        im.put(KeyStroke.getKeyStroke("DOWN"), DOWN_ACTION);
        im.put(KeyStroke.getKeyStroke("TAB"), DOWN_ACTION);
        im.put(KeyStroke.getKeyStroke("ENTER"), DOWN_ACTION);
        am.put(UP_ACTION, new UpAction());
        am.put(DOWN_ACTION, new DownAction());
        am.put(CANCEL_ACTION, new CancelAction());
    }
    @Override
    public String subPanelTextureName()    { return TEXTURE_BROWN; }
    @Override
    public boolean drawMemory()            { return true; }
    @Override
    public boolean hasStarBackground()     { return true; }
    @Override
    public void animate() {
        if (!playAnimations())
            return;
        planetListing.animate();
    }
    @Override
    public void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        // draw the gradient background for the header row
        if (backGradient == null) {
            Color c0 = new Color(71,53,39,0);
            Color c1 = new Color(71,53,39);
            Point2D start = new Point2D.Float(s10, getHeight()-scaled(200));
            Point2D end = new Point2D.Float(s10, getHeight()-s20);
            float[] dist = {0.0f, 1.0f};
            Color[] colors = {c0, c1 };
            backGradient = new LinearGradientPaint(start, end, dist, colors);
        }
        g.setPaint(backGradient);
        g.fillRect(s10,getHeight()-scaled(200),getWidth()-s20, scaled(190));
    }
    @Override
    public void keyPressed(KeyEvent e) {
        boolean repaint = false;
        int mods = e.getModifiersEx();
        switch(e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
                if (frame().getGlassPane().isVisible())
                    disableGlassPane();
                else
                    finish(false);
                return;
            case KeyEvent.VK_TAB:
                if (mods == 0)
                    viewSelectionPane.selectNextTab();
                else if (mods == 1)
                    viewSelectionPane.selectPreviousTab();
                return;
            case KeyEvent.VK_UP:     repaint = listingUI.scrollUp(); break;
            case KeyEvent.VK_DOWN:   repaint = listingUI.scrollDown(); break;
            case KeyEvent.VK_1:
            case KeyEvent.VK_2:
            case KeyEvent.VK_3:
            case KeyEvent.VK_4:
            case KeyEvent.VK_5:
                spendingPane.keyPressed(e); return;
        }
        if (repaint)
            repaint();
    }
    private List<StarSystem> systems()  {
        if (displayedSystems == null) {
            displayedSystems = player().allColonizedSystems();
            if (selectedSystem() == null)
                selectedSystem(displayedSystems.get(0), false);
            selectedSystem(selectedSystem(), false);
        }
        return displayedSystems;
    }
    void systems(List<StarSystem> s)   { displayedSystems = s; }
    @Override
    public StarSystem systemViewToDisplay()  { return selectedSystem(); }

    private synchronized void selectedSystem(StarSystem sys, boolean updateFieldValues) {
        notesField.setVisible(false);
        nameField.setVisible(false);
        RotPUI.instance().requestFocusInWindow();
        if (updateFieldValues)
            setFieldValues(selectedSystem());

        sessionVar("MAINUI_SELECTED_SYSTEM", sys);
        sessionVar("MAINUI_CLICKED_SPRITE", sys);

        notesField.setText(sys.notes());
        notesField.setCaretPosition(notesField.getText().length());
        nameField.setText(player().sv.name(sys.id));
        nameField.setCaretPosition(nameField.getText().length());
    }
    private void setFieldValues(StarSystem sys) {
        if (sys != null) {
            Empire pl = player();
            sys.notes(notesField.getText().trim());
            String name = nameField.getText().trim();
            if (!name.isEmpty())
                pl.sv.name(sys.id, name);
        }
    }
    private StarSystem selectedSystem() {
        StarSystem sys =(StarSystem) sessionVar("MAINUI_SELECTED_SYSTEM");
        Empire pl = player();
        int id = sys.id;
        if (pl.sv.empire(id) != pl)
            sys = pl.defaultSystem();
        return (sys == null) || !systems().contains(sys) ? null : sys;
    }
    private void initDataViews() {
        Column rowNumCol =  listingUI.newRowNumColumn("PLANETS_LIST_NUM", 15, RIGHT);
        Column nameCol = listingUI.newSystemNameColumn(nameField, "PLANETS_LIST_NAME", "NAME", 170, palette.black, StarSystem.NAME, LEFT);
        Column populationCol = listingUI.newSystemDeltaDataColumn("PLANETS_LIST_POPULATION", "POPULATION", 130, palette.black, StarSystem.POPULATION, RIGHT);
        Column sizeCol = listingUI.newSystemDataColumn("PLANETS_LIST_SIZE", "SIZE", 60, palette.black, StarSystem.CURRENT_SIZE, RIGHT);
        Column pTypeCol = listingUI.newPlanetTypeColumn("PLANETS_LIST_TYPE", "PLANET_TYPE", 90, StarSystem.PLANET_TYPE);
        Column wasteCol = listingUI.newSystemDataColumn("PLANETS_LIST_WASTE", "WASTE", 75, palette.black, StarSystem.WASTE, RIGHT);
        Column notesCol = listingUI.newSystemNotesColumn(notesField, "PLANETS_LIST_NOTES", "NOTES", 999, palette.black);
        Column factoriesCol = listingUI.newSystemDeltaDataColumn("PLANETS_LIST_FACTORIES", "FACTORIES", 110, palette.black, StarSystem.FACTORIES, RIGHT);
        Column productionCol = listingUI.newSystemDataColumn("PLANETS_LIST_PRODUCTION", "INCOME", 60, palette.black, StarSystem.INCOME, RIGHT);
        Column indRsvCol = listingUI.newSystemDataColumn("PLANETS_LIST_RESERVE", "RESERVE", 60, palette.black, StarSystem.INDUSTRY_RESERVE, RIGHT);
        Column basesCol = listingUI.newSystemDeltaDataColumn("PLANETS_LIST_BASES", "BASES", 70, palette.black, StarSystem.BASES, RIGHT);
        Column shieldCol = listingUI.newSystemDataColumn("PLANETS_LIST_SHIELD", "SHIELD", 70, palette.black, StarSystem.SHIELD, RIGHT);
        Column shipCol = listingUI.newSystemDataColumn("PLANETS_LIST_SHIPYARD", "SHIPYARD", 120, palette.black, StarSystem.SHIPYARD, LEFT);
        Column resourceCol = listingUI.newSystemDataColumn("PLANETS_LIST_RESOURCES", "RESOURCES", 90, palette.black, StarSystem.RESOURCES, LEFT);

        DataView ecoView = listingUI.newDataView();
        ecoView.addColumn(rowNumCol);
        ecoView.addColumn(nameCol);
        ecoView.addColumn(populationCol);
        ecoView.addColumn(pTypeCol);
        ecoView.addColumn(resourceCol);
        ecoView.addColumn(sizeCol);
        ecoView.addColumn(wasteCol);
        ecoView.addColumn(notesCol);
        views.put(ECOLOGY_MODE, ecoView);

        DataView indView = listingUI.newDataView();
        indView.addColumn(rowNumCol);
        indView.addColumn(nameCol);
        indView.addColumn(populationCol);
        indView.addColumn(resourceCol);
        indView.addColumn(factoriesCol);
        indView.addColumn(productionCol);
        indView.addColumn(indRsvCol);
        indView.addColumn(notesCol);
        views.put(INDUSTRY_MODE, indView);

        DataView milView = listingUI.newDataView();
        milView.addColumn(rowNumCol);
        milView.addColumn(nameCol);
        milView.addColumn(populationCol);
        milView.addColumn(resourceCol);
        milView.addColumn(productionCol);
        milView.addColumn(shieldCol);
        milView.addColumn(basesCol);
        milView.addColumn(shipCol);
        milView.addColumn(notesCol);
        views.put(MILITARY_MODE, milView);

        listingUI.selectedColumn(rowNumCol);
    }
    private void finish(boolean disableNextTurn) {
        displayedSystems = null;
        buttonClick();
        RotPUI.instance().selectMainPanel(disableNextTurn);
    }
    class PlanetListingUI extends BasePanel {
        private static final long serialVersionUID = 1L;
        public PlanetListingUI(PlanetsUI p) {
            init(p);
        }
        private void init(PlanetsUI p) {
            setOpaque(false);
            setLayout(new BorderLayout());

            listingUI = new PlanetDataListingUI(p);

            JPanel centerPanel = new JPanel();
            centerPanel.setOpaque(false);
            centerPanel.setLayout(new BorderLayout());
            centerPanel.add(viewSelectionPane, BorderLayout.NORTH);
            centerPanel.add(listingUI, BorderLayout.CENTER);

            add(centerPanel, BorderLayout.CENTER);
        }
        @Override
        public void animate() {
            planetDisplayPane.animate();
        }
    }
    class PlanetViewSelectionPanel extends BasePanel implements MouseMotionListener, MouseListener {
        private static final long serialVersionUID = 1L;
        Rectangle hoverBox;
        Rectangle ecologyBox = new Rectangle();
        Rectangle industryBox = new Rectangle();
        Rectangle militaryBox = new Rectangle();
        Area textureArea;
        public PlanetViewSelectionPanel() {
            initModel();
        }
        private void initModel() {
            setOpaque(false);
            setPreferredSize(new Dimension(getWidth(),s40));
            addMouseListener(this);
            addMouseMotionListener(this);
        }
        @Override
        public Area textureArea()       { return textureArea; }
        @Override
        public String textureName()     { return instance.subPanelTextureName(); }
        @Override
        public void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;

            int w = getWidth();
            int h = getHeight();
            int gap = s20;
            int tabW = (w-(6*gap))/4;
            String title = text("PLANETS_TITLE", player().raceName());
            String ecoLabel = text("PLANETS_VIEW_ECOLOGY");
            String indLabel =  text("PLANETS_VIEW_INDUSTRY");
            String milLabel =  text("PLANETS_VIEW_MILITARY");

            g.setColor(SystemPanel.orangeText);
            g.setFont(narrowFont(30));

            int x0 = gap;
            int y0 = h - s10;
            g.drawString(title, x0,y0);
            x0 += (tabW+gap);
            drawTab(g,x0,0,tabW,h,ecoLabel, ecologyBox, selectedMode == ECOLOGY_MODE);
            textureArea = new Area(new RoundRectangle2D.Float(x0,s10,tabW,h-s10,h/4,h/4));

            x0 += (tabW+gap);
            drawTab(g,x0,0,tabW,h,indLabel, industryBox, selectedMode == INDUSTRY_MODE);
            Area tab2Area = new Area(new RoundRectangle2D.Float(x0,s10,tabW,h-s10,h/4,h/4));
            textureArea.add(tab2Area);

            x0 += (tabW+gap);
            drawTab(g,x0,0,tabW,h,milLabel, militaryBox, selectedMode == MILITARY_MODE);
            Area tab3Area = new Area(new RoundRectangle2D.Float(x0,s10,tabW,h-s10,h/4,h/4));
            textureArea.add(tab3Area);
        }
        private void drawTab(Graphics2D g, int x, int y, int w, int h, String label, Rectangle box, boolean selected) {
            g.setFont(narrowFont(20));
            if (selected)
                g.setColor(selectedC);
            else
                g.setColor(unselectedC);

            box.setBounds(x, y+s10, w, h-s10);
            g.fillRoundRect(x, y+s10, w, h-s10, h/4, h/4);
            g.fillRect(x, h-s5, w, s5);

            if (box == hoverBox) {
                Stroke prev = g.getStroke();
                g.setStroke(stroke2);
                g.setColor(Color.yellow);
                g.setClip(x, y, w, h*2/3);
                g.drawRoundRect(x, y+s10, w, h-s10, h/4, h/4);
                g.setClip(x, y+h/2, w, h/2);
                g.drawRect(x, y+s10, w, h);
                g.setClip(null);
                g.setStroke(prev);
            }
            int sw = g.getFontMetrics().stringWidth(label);
            int x0 = x+((w-sw)/2);
            int y0 = y+h-s10;

            Color c0 = (box == hoverBox) ? Color.yellow : SystemPanel.whiteLabelText;
            drawShadowedString(g, label, 3, x0, y0, SystemPanel.textShadowC, c0);
        }
        public void selectNextTab() {
            switch(selectedMode) {
                case ECOLOGY_MODE: selectTab(INDUSTRY_MODE); break;
                case INDUSTRY_MODE: selectTab(MILITARY_MODE); break;
                case MILITARY_MODE: selectTab(ECOLOGY_MODE); break;
            }
        }
        public void selectPreviousTab() {
            switch(selectedMode) {
                case ECOLOGY_MODE: selectTab(MILITARY_MODE); break;
                case INDUSTRY_MODE: selectTab(ECOLOGY_MODE); break;
                case MILITARY_MODE: selectTab(INDUSTRY_MODE); break;
            }
        }
        private void selectTab(int mode) {
            if (mode != selectedMode) {
                softClick();
                selectedMode = mode;
                selectedSystem(selectedSystem(), true);
                instance.repaint();
            }
        }
        @Override
        public void mouseClicked(MouseEvent e) {}
        @Override
        public void mouseEntered(MouseEvent e) {}
        @Override
        public void mouseExited(MouseEvent e) {
            if (hoverBox != null) {
                hoverBox = null;
                repaint();
            }
        }
        @Override
        public void mousePressed(MouseEvent e) {}
        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getButton() > 3)
                return;
            int x = e.getX();
            int y = e.getY();
            if (hoverBox == null)
                misClick();
            else {
                if (hoverBox == ecologyBox)
                    selectTab(ECOLOGY_MODE);
                else if (hoverBox == industryBox)
                    selectTab(INDUSTRY_MODE);
                else if (hoverBox == militaryBox)
                    selectTab(MILITARY_MODE);
            }
        }
        @Override
        public void mouseDragged(MouseEvent e) {}
        @Override
        public void mouseMoved(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();
            Rectangle prevHover = hoverBox;
            if (ecologyBox.contains(x,y))
                hoverBox = ecologyBox;
            else if (industryBox.contains(x,y))
                hoverBox = industryBox;
            else if (militaryBox.contains(x,y))
                hoverBox = militaryBox;

            if (hoverBox != prevHover)
                repaint();
        }
    }
    class PlanetDisplayPanel extends SystemPanel {
        private static final long serialVersionUID = 1L;
        EmpireInfoGraphicPane graphicPane;
        PlanetsUI parent;
        public PlanetDisplayPanel(PlanetsUI p) {
            parent = p;
            init();
        }
        private void init() {
            setOpaque(true);
            setBackground(selectedC);
            setBorder(newEmptyBorder(6,6,6,6));
            setPreferredSize(new Dimension(scaled(250), getHeight()));

            graphicPane = new EmpireInfoGraphicPane(this);
            graphicPane.setPreferredSize(new Dimension(getWidth(),scaled(140)));

            BorderLayout layout = new BorderLayout();
            layout.setVgap(s6);
            setLayout(layout);
            add(topPane(), BorderLayout.NORTH);
            add(detailPane(), BorderLayout.CENTER);
        }
        @Override
        public String subPanelTextureName()    { return TEXTURE_BROWN; }
        @Override
        public StarSystem systemViewToDisplay() { return selectedSystem(); }
        @Override
        public void animate() { graphicPane.animate(); }
        @Override
        protected BasePanel topPane() {
            return graphicPane;
        }
        @Override
        protected BasePanel detailPane() {
            BasePanel empireDetailTopPane = new BasePanel();
            empireDetailTopPane.setOpaque(false);
            empireDetailTopPane.setBorder(new ShadowBorder(palette.bdrHiOut, palette.bdrLoIn));
            empireDetailTopPane.setLayout(new BorderLayout(0,s1));
            empireDetailTopPane.setPreferredSize(new Dimension(getWidth(),scaled(120)));
            empireDetailTopPane.add(new EmpireColonyFoundedPane(this, null, unselectedC), BorderLayout.NORTH);
            empireDetailTopPane.add(new EmpireColonyInfoPane(this, unselectedC, palette.bdrHiIn, palette.yellow, palette.bdrLoIn), BorderLayout.CENTER);

            BasePanel empireDetailBottomPane = new BasePanel();
            empireDetailBottomPane.setOpaque(false);
            empireDetailBottomPane.setBorder(new ShadowBorder(palette.bdrHiOut, palette.bdrLoIn));
            empireDetailBottomPane.setLayout(new BorderLayout(0,s3));
            empireDetailBottomPane.setPreferredSize(new Dimension(getWidth(),scaled(215)));
            empireDetailBottomPane.add(new ColonyShipPane(this), BorderLayout.NORTH);
            empireDetailBottomPane.add(new ColonyTransferFunds(this), BorderLayout.CENTER);
            empireDetailBottomPane.setBorder(newEmptyBorder(0,0,0,0));

            spendingPane = new EmpireColonySpendingPane(parent, unselectedC, palette.white, palette.bdrHiOut, palette.bdrLoIn);
            BasePanel empireDetailPane = new BasePanel();
            empireDetailPane.setOpaque(false);
            empireDetailPane.setLayout(new BorderLayout(0,s3));
            empireDetailPane.add(empireDetailTopPane, BorderLayout.NORTH);
            empireDetailPane.add(spendingPane, BorderLayout.CENTER);
            empireDetailPane.add(empireDetailBottomPane, BorderLayout.SOUTH);
            return empireDetailPane;
        }
    }
    class EmpireInfoGraphicPane extends BasePanel implements ActionListener {
        private static final long serialVersionUID = 1L;
        SystemPanel parent;
        Ellipse2D starCircle = new Ellipse2D.Float();
        Ellipse2D planetCircle = new Ellipse2D.Float();
        int currentHover = 0;
        EmpireInfoGraphicPane(SystemPanel p) {
            parent = p;
            init();
        }
        private void init() {
            setBackground(palette.black);
        }
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);

            StarSystem sys = parent.systemViewToDisplay();
            if (sys == null)
                return;

            Empire pl = player();
            int w = getWidth();
            int h = getHeight();

            Graphics2D g2 = (Graphics2D) g;
            g2.drawImage(pl.sv.starBackground(this), 0, 0, null);
            drawStar(g2, selectedSystem().starType(), s80, getWidth()/3, s70);
            starCircle.setFrame((getWidth()/3)-s20, s10, s40, s40);

            g.setFont(narrowFont(36));
            String str = player().sv.name(sys.id);
            int y0 = s42;
            int x0 = s25;
            drawBorderedString(g, str, 2, x0, y0, Color.black, SystemPanel.orangeText);

            int x1 = s20;
            int y1 = s70;
            int r = s40;
            selectedSystem().planet().draw(g, w, h, x1, y1, r+r, 45);
            planetCircle.setFrame(x1, y1, r+r, r+r);
            parent.drawPlanetInfo(g2, selectedSystem(), false, false, s40, getWidth(), getHeight()-s12);
        }
        @Override
        public void animate() {
            if (animationCount() % 3 == 0) {
                try {
                    selectedSystem().planet().rotate(1);
                    repaint();
                }
                catch (Exception e) { }
            }
        }
    }
    class ColonyShipPane extends BasePanel implements MouseListener, MouseMotionListener, MouseWheelListener {
        private static final long serialVersionUID = 1L;
        SystemPanel parent;
        // polygon coordinates for left & right increment buttons
        private final int leftButtonX[] = new int[3];
        private final int leftButtonY[] = new int[3];
        private final int rightButtonX[] = new int[3];
        private final int rightButtonY[] = new int[3];

        private final Rectangle shipDesignBox = new Rectangle();
        private final Rectangle shipNameBox = new Rectangle();
        private final Polygon prevDesign = new Polygon();
        private final Polygon nextDesign = new Polygon();
        private Shape hoverBox;

        private final Color textColor = newColor(204,204,204);
        ColonyShipPane(SystemPanel p) {
            parent = p;
            init();
        }
        @Override
        public String textureName()    { return parent.subPanelTextureName(); }
        private void init() {
            setBackground(unselectedC);
            setPreferredSize(new Dimension(getWidth(), scaled(110)));
            addMouseListener(this);
            addMouseMotionListener(this);
            addMouseWheelListener(this);
        }
        @Override
        public void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            int w = getWidth();
            int h = getHeight();

            int midMargin = scaled(105);
            drawTitle(g);
            drawShipIcon(g,s5,s30,midMargin-s15,s75);
            drawShipCompletion(g,midMargin,h-s75,w-s10-midMargin,s30);
            drawNameSelector(g,midMargin,h-s60,w-s10-midMargin,s30);
        }
        private void drawTitle(Graphics g) {
            g.setFont(narrowFont(20));
            g.setColor(Color.black);
            String str = text("MAIN_COLONY_SHIPYARD_CONSTRUCTION");
            drawShadowedString(g, str, 2, s5, s22, MainUI.shadeBorderC(), textColor);
        }
        private void drawShipIcon(Graphics2D g, int x, int y, int w, int h) {
            g.setColor(Color.black);
            g.fillRect(x, y, w, h);
            StarSystem sys = parent.systemViewToDisplay();
            Colony c = sys == null ? null : sys.colony();
            if (c == null)
                return;

            shipDesignBox.setBounds(x,y,w,h);

            Design d = c.shipyard().design();
            g.drawImage(initializedBackgroundImage(w, h), x,y, null);

            int y0a = h/3;
            int y0b = h*2/3;

            // horizontal bars
            g.setColor(darkBrown);
            g.fillRect(x, y+s4,     w, s4);
            g.fillRect(x, y+h-s8,   w, s4);
            g.fillRect(x, y+y0a,    w, s4);
            g.fillRect(x, y+y0b-s4, w, s4);

            g.setColor(brown);
            g.fillRect(x, y+s8,     w, s4);
            g.fillRect(x, y+h-s12,  w, s4);
            g.fillRect(x, y+y0a-s4, w, s4);
            g.fillRect(x, y+y0b,    w, s4);

            // vertical bars
            g.setColor(darkBrown);
            g.fillRect(x+w/8,   y+y0a+s4, s4, y0b-y0a-s8);
            g.fillRect(x+w*3/8, y+y0a+s4, s4, y0b-y0a-s8);
            g.fillRect(x+w*5/8, y+y0a+s4, s4, y0b-y0a-s8);
            g.fillRect(x+w*7/8, y+y0a+s4, s4, y0b-y0a-s8);

            Stroke prevStroke = g.getStroke();
            g.setStroke(stroke2);
            g.drawLine(x+s4,         y+s12,    x+(w/5)-s4,   y+y0a-s4);
            g.drawLine(x+(w/5)+s4,   y+y0a-s4, x+(w*2/5)-s4, y+s12);
            g.drawLine(x+(w*2/5)+s4, y+s12,    x+(w*3/5)-s4, y+y0a-s4);
            g.drawLine(x+(w*3/5)+s4, y+y0a-s4, x+(w*4/5)-s4, y+s12);
            g.drawLine(x+(w*4/5)+s4, y+s12,    x+w-s4,       y+y0a-s4);

            g.drawLine(x+s4,         y+h-s12,  x+(w/5)-s4,   y+y0b+s4);
            g.drawLine(x+(w/5)+s4,   y+y0b+s4, x+(w*2/5)-s4, y+h-s12);
            g.drawLine(x+(w*2/5)+s4, y+h-s12,  x+(w*3/5)-s4, y+y0b+s4);
            g.drawLine(x+(w*3/5)+s4, y+y0b+4,  x+(w*4/5)-4,  y+h-12);
            g.drawLine(x+(w*4/5)+s4, y+h-s12,  x+w-s4,       y+y0b+s4);

            g.setStroke(prevStroke);

            // draw design image
            Image img = d.image();
            int w0 = img.getWidth(null);
            int h0 = img.getHeight(null);
            float scale = min((float)w/w0, (float)h/h0);

            int w1 = (int)(scale*w0);
            int h1 = (int)(scale*h0);
            int x1 = x+(w - w1) / 2;
            int y1 = y+(h - h1) / 2;
            g.drawImage(img, x1, y1, x1+w1, y1+h1, 0, 0, w0, h0, this);

            if (hoverBox == shipDesignBox) {
                prevStroke = g.getStroke();
                g.setStroke(stroke2);
                g.setColor(SystemPanel.yellowText);
                g.drawRect(x, y, w, h);
                g.setStroke(prevStroke);
            }

            // draw expected build count
            String count;
            if (d.scrapped()) {
                count = text("MAIN_COLONY_SHIP_SCRAPPED");
                g.setColor(Color.red);
                g.setFont(narrowFont(20));
            }
            else {
                int i = c.shipyard().upcomingShipCount();
                if (i == 0)
                    return;
                count = Integer.toString(i);
                g.setColor(SystemPanel.yellowText);
                g.setFont(narrowFont(20));
            }

            int sw = g.getFontMetrics().stringWidth(count);
            g.drawString(count, x+w-s5-sw, y+h-s5);
        }
        private void drawShipCompletion(Graphics2D g, int x, int y, int w, int h) {
            StarSystem sys = parent.systemViewToDisplay();
            Colony c = sys == null ? null : sys.colony();
            if (c == null)
                return;

            String result = c.shipyard().shipCompletionResult();
            g.setColor(Color.black);
            g.setFont(narrowFont(16));
            g.drawString(result, x+s12, y+s10);
        }
        private void drawNameSelector(Graphics2D g, int x, int y, int w, int h) {
            StarSystem sys = parent.systemViewToDisplay();
            Colony c = sys == null ? null : sys.colony();
            if (c == null)
                return;

            int leftM = x;
            int rightM = x+w;
            int buttonW = s10;
            int buttonTopY = y+s5;
            int buttonMidY = y+s15;
            int buttonBotY = y+s25;
            leftButtonX[0] = leftM; leftButtonX[1] = leftM+buttonW; leftButtonX[2] = leftM+buttonW;
            leftButtonY[0] = buttonMidY; leftButtonY[1] = buttonTopY; leftButtonY[2] = buttonBotY;

            rightButtonX[0] = rightM; rightButtonX[1] = rightM-buttonW; rightButtonX[2] = rightM-buttonW;
            rightButtonY[0] = buttonMidY; rightButtonY[1] = buttonTopY; rightButtonY[2] = buttonBotY;
            prevDesign.reset();
            nextDesign.reset();
            for (int i=0;i<leftButtonX.length;i++) {
                prevDesign.addPoint(leftButtonX[i], leftButtonY[i]);
                nextDesign.addPoint(rightButtonX[i], rightButtonY[i]);
            }

            if (hoverBox == prevDesign)
                g.setColor(SystemPanel.yellowText);
            else
                g.setColor(Color.black);
            g.fillPolygon(leftButtonX, leftButtonY, 3);


            if (hoverBox == nextDesign)
                g.setColor(SystemPanel.yellowText);
            else
                g.setColor(Color.black);
            g.fillPolygon(rightButtonX, rightButtonY, 3);

            int barX = x+s12;
            int barW = w-s24;
            int barY = y+s5;
            int barH = h-s10;
            g.setColor(Color.black);
            g.fillRect(barX, barY, barW, barH);
            shipNameBox.setBounds(barX, barY, barW, barH);

            g.setColor(sliderBoxBlue);
            g.setFont(narrowFont(18));
            String name = c.shipyard().design().name();
            int sw = g.getFontMetrics().stringWidth(name);
            int x0 = barX+((barW-sw)/2);
            g.drawString(name, x0, barY+s16);

            if (hoverBox == shipNameBox) {
                Stroke prev = g.getStroke();
                g.setColor(SystemPanel.yellowText);
                g.setStroke(stroke2);
                g.draw(shipNameBox);
                g.setStroke(prev);
            }
        }
        private Image initializedBackgroundImage(int w, int h) {
            if ((starBackground == null)
            || (starBackground.getWidth() != w)
            || (starBackground.getHeight() != h)) {
                starBackground = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                Graphics g = starBackground.getGraphics();
                drawBackgroundStars(g,w,h);
                g.dispose();
            }
            return starBackground;
        }
        public void nextShipDesign(boolean click) {
            StarSystem sys = parent.systemViewToDisplay();
            Colony c = sys == null ? null : sys.colony();
            if (c == null)
                return;
            if (!c.shipyard().canCycleDesign()) {
                if (click)
                    misClick();
            }
            else {
                if (click)
                    softClick();
                c.shipyard().goToNextDesign();
                parent.repaint();
            }
        }
        public void prevShipDesign(boolean click) {
            StarSystem sys = parent.systemViewToDisplay();
            Colony c = sys == null ? null : sys.colony();
            if (c  == null)
                return;
            if (!c.shipyard().canCycleDesign()) {
                if (click)
                    misClick();
            }
            else {
                if (click)
                    softClick();
                c.shipyard().goToPrevDesign();
                parent.repaint();
            }
        }
        @Override
        public void mouseClicked(MouseEvent arg0) { }
        @Override
        public void mouseEntered(MouseEvent arg0) { }
        @Override
        public void mouseExited(MouseEvent arg0) {
            if (hoverBox != null) {
                hoverBox = null;
                repaint();
            }
        }
        @Override
        public void mousePressed(MouseEvent arg0) { }
        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getButton() > 3)
                return;
            int x = e.getX();
            int y = e.getY();

            if (shipDesignBox.contains(x,y)){
                nextShipDesign(true);
                parent.repaint();
            }
            else if (shipNameBox.contains(x,y)){
                nextShipDesign(true);
                parent.repaint();
            }
            else if (nextDesign.contains(x,y)){
                nextShipDesign(true);
                parent.repaint();
            }
            else if (prevDesign.contains(x,y)){
                prevShipDesign(true);
                parent.repaint();
            }
        }
        @Override
        public void mouseDragged(MouseEvent arg0) { }
        @Override
        public void mouseMoved(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();
            Shape prevHover = hoverBox;

            hoverBox = null;

            if (shipDesignBox.contains(x,y))
                hoverBox = shipDesignBox;
            else if (shipNameBox.contains(x,y))
                hoverBox = shipNameBox;
            else if (nextDesign.contains(x,y))
                hoverBox = nextDesign;
            else if (prevDesign.contains(x,y))
                hoverBox = prevDesign;

            if (prevHover != hoverBox)
                repaint();
        }
        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (e.getWheelRotation() < 0)
                nextShipDesign(false);
            else
                prevShipDesign(false);

            parent.repaint();
        }
    }
    class ColonyTransferFunds extends BasePanel implements MouseListener, MouseMotionListener {
        private static final long serialVersionUID = 1L;
        SystemPanel parent;
        private final Rectangle transferBox = new Rectangle();
        private Shape hoverBox;

        private final Color textColor = newColor(204,204,204);
        ColonyTransferFunds(SystemPanel p) {
            parent = p;
            init();
        }
        private void init() {
            setBackground(unselectedC);
            addMouseListener(this);
            addMouseMotionListener(this);
        }
        @Override
        public String textureName()    { return parent.subPanelTextureName(); }
        @Override
        public void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            int w = getWidth();
            int h = getHeight();

            int y0 = s22;
            int x0 = s5;

            // draw title
            g.setFont(narrowFont(20));
            String str = text("PLANETS_COLONY_TRANSFER_TITLE");
            drawShadowedString(g, str, 2, x0, y0, MainUI.shadeBorderC(), textColor);

            y0 += s5;
            // draw detail
            g.setFont(narrowFont(16));
            g.setColor(palette.black);
            List<String> lines = scaledNarrowWrappedLines(g, text("PLANETS_COLONY_TRANSFER_DETAIL"), w-s15, 2, 16, 12);
            for (String line: lines) {
                y0 += s16;
                g.drawString(line, x0, y0);
            }
            int buttonH = s30;
            drawTransferButton(g, s5, h-buttonH-s5, w-s10, buttonH);
        }
        private void drawTransferButton(Graphics2D g, int x, int y, int w, int h) {
            transferBox.setBounds(x,y,w,h);
            g.setColor(Color.blue);

            Stroke prev = g.getStroke();
            g.setStroke(stroke3);
            g.setColor(Color.black);
            g.drawRect(x+s2,y+s2,w,h);
            g.setStroke(prev);

            g.setColor(unselectedC);
            g.fillRect(x,y,w,h);

            Color c0 = palette.white;
            if ((hoverBox == transferBox)
            && isButtonEnabled())
                c0 = Color.yellow;


            String lbl = text("PLANETS_COLONY_TRANSFER_BUTTON");
            int fontSize = scaledFont(g, lbl, w-s10, 20, 14);
            g.setFont(narrowFont(fontSize));
            int sw = g.getFontMetrics().stringWidth(lbl);
            int x0 = x+((w-sw)/2);
            int y0 = y+h-s8;
            drawShadowedString(g, lbl, 2, x0, y0, MainUI.shadeBorderC(), c0);

            prev = g.getStroke();
            g.setStroke(stroke2);
            g.setColor(c0);
            g.drawRect(x,y,w,h);
            g.setStroke(prev);
        }
        public boolean isButtonEnabled() { return (int)player().totalReserve() > 0; }
        public void buttonClicked() {
            if (isButtonEnabled()) {
                softClick();
                transferReservePane.targetSystem(selectedSystem());
                enableGlassPane(transferReservePane);
            }
            else
                misClick();
        }
        @Override
        public void mouseClicked(MouseEvent arg0) { }
        @Override
        public void mouseEntered(MouseEvent arg0) { }
        @Override
        public void mouseExited(MouseEvent arg0) {
            if (hoverBox != null) {
                hoverBox = null;
                repaint();
            }
        }
        @Override
        public void mousePressed(MouseEvent arg0) { }
        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getButton() > 3)
                return;
            int x = e.getX();
            int y = e.getY();

            if (transferBox.contains(x,y)){
                buttonClicked();
                parent.repaint();
            }
        }
        @Override
        public void mouseDragged(MouseEvent arg0) { }
        @Override
        public void mouseMoved(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();
            Shape prevHover = hoverBox;

            hoverBox = null;
            if (transferBox.contains(x,y))
                hoverBox = transferBox;

            if (prevHover != hoverBox)
                repaint();
        }
    }
    final class PlanetDataListingUI extends SystemListingUI {
        private static final long serialVersionUID = 1L;
        PlanetDataListingUI(BasePanel p) {
            super(p);
            setBorder(BorderFactory.createMatteBorder(scaled(3), 0,0,0, selectedC));
        }
        @Override
        public String textureName()    { return instance.subPanelTextureName(); }
        @Override
        protected DataView dataView() { return views.get(selectedMode); }
        @Override
        protected List<StarSystem> systems() { return instance.systems();  }
        @Override
        protected StarSystem selectedSystem() { return instance.selectedSystem(); }
        @Override
        protected void selectedSystem(StarSystem sys, boolean updateFieldValues) {
            instance.selectedSystem(sys, updateFieldValues);
        }
        @Override
        protected void postInit() {
            nameField.addMouseListener(this);
            notesField.addMouseListener(this);
            add(nameField);
            add(notesField);
        }
    }
    class EmpireRevenueUI extends BasePanel {
        private static final long serialVersionUID = 1L;
        public EmpireRevenueUI() {
            initModel();
        }
        private void initModel() {
            setPreferredSize(new Dimension(getWidth(),scaled(160)));
            setOpaque(false);
            setLayout(new BorderLayout());
            add(new ReserveUI(), BorderLayout.EAST);
            add(new SpendingCostsUI(), BorderLayout.CENTER);
            add(new TotalIncomeUI(), BorderLayout.WEST);
        }
    }
    class SpendingCostsUI extends BasePanel {
        private static final long serialVersionUID = 1L;
        Shape textureClip;
        public SpendingCostsUI() {
            init();
        }
        private void init() {
            setOpaque(false);
        }
        @Override
        public String textureName()   { return instance.subPanelTextureName(); }
        @Override
        public Shape textureClip()    { return textureClip; }
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);

            int w = getWidth();
            int h = getHeight();
            g.setFont(narrowFont(24));
            String title = text("PLANETS_SPENDING_COSTS");
            int sw = g.getFontMetrics().stringWidth(title);
            int x0 = (w-sw)/2;
            drawShadowedString(g, title, 2, x0, s35, palette.black, SystemPanel.orangeText);

            int margin = s10;
            int border = s10;
            int x1 = border; int w1 = w-x1-border;
            int y1 = s45; int h1 = h-y1-s10;
            g.setColor(palette.medBack);
            g.fillRect(x1, y1, w1, h1);

            textureClip = new Rectangle(x1,y1,w1,h1);

            // DESC
            g.setFont(narrowFont(15));
            String desc = text("PLANETS_COSTS_DESC");
            List<String> descLines = wrappedLines(g, desc, w-s40);
            g.setColor(palette.black);
            y1 = s63;
            for (String line: descLines) {
                g.drawString(line, x1+s20, y1);
                y1 += s16;
            }

            g.setFont(narrowFont(18));

            // LEFT COLUMN
            w1 = (w*3/5)-x1-s5;
            y1 = h-s60;
            int midX = x1+w1-s50;
            String lbl = text("PLANETS_COSTS_SHIPS");
            sw = g.getFontMetrics().stringWidth(lbl);
            drawShadowedString(g, lbl, 2, midX-sw, y1, SystemPanel.textShadowC, SystemPanel.whiteText);

            String val = text("PLANETS_AMT_PCT", fmt(100*player().shipMaintCostPerBC(),1));
            sw = g.getFontMetrics().stringWidth(val);
            g.setColor(palette.black);
            g.drawString(val, midX+s50-sw, y1);

            y1 = h-s40;
            lbl = text("PLANETS_COSTS_BASES");
            sw = g.getFontMetrics().stringWidth(lbl);
            drawShadowedString(g, lbl, 2, midX-sw, y1, SystemPanel.textShadowC, SystemPanel.whiteText);
            val = text("PLANETS_AMT_PCT", fmt(100*player().totalMissileBaseCostPct(),1));
            sw = g.getFontMetrics().stringWidth(val);
            g.setColor(palette.black);
            g.drawString(val, midX+s50-sw, y1);


            y1 = h-s20;
            lbl = text("PLANETS_COSTS_STARGATES");
            sw = g.getFontMetrics().stringWidth(lbl);
            drawShadowedString(g, lbl, 2, midX-sw, y1, SystemPanel.textShadowC, SystemPanel.whiteText);
            val = text("PLANETS_AMT_PCT", fmt(100*player().totalStargateCostPct(),1));
            sw = g.getFontMetrics().stringWidth(val);
            g.setColor(palette.black);
            g.drawString(val, midX+s50-sw, y1);

            // RIGHT COLUMN
            x1 = (w*2/3)+s20; w1 = w-x1-border-margin;
            y1 = h-s60;
            midX = x1+w1-s50;
            lbl = text("PLANETS_COSTS_SPYING");
            sw = g.getFontMetrics().stringWidth(lbl);
            drawShadowedString(g, lbl, 2, midX-sw, y1, SystemPanel.textShadowC, SystemPanel.whiteText);

            val = text("PLANETS_AMT_PCT", fmt(100*player().totalSpyCostPct(),1));
            sw = g.getFontMetrics().stringWidth(val);
            g.setColor(palette.black);
            g.drawString(val, midX+s50-sw, y1);

            y1 = h-s40;
            lbl = text("PLANETS_COSTS_SECURITY");
            sw = g.getFontMetrics().stringWidth(lbl);
            drawShadowedString(g, lbl, 2, midX-sw, y1, SystemPanel.textShadowC, SystemPanel.whiteText);
            val = text("PLANETS_AMT_PCT", fmt(100*player().internalSecurityCostPct(),1));
            sw = g.getFontMetrics().stringWidth(val);
            g.setColor(palette.black);
            g.drawString(val, midX+s50-sw, y1);
        }
    }
    class TotalIncomeUI extends BasePanel {
        private static final long serialVersionUID = 1L;
        Shape textureClip;
        public TotalIncomeUI() {
            initModel();
        }
        private void initModel() {
            setPreferredSize(new Dimension(scaled(300),getHeight()));
            setOpaque(false);
        }
        @Override
        public String textureName()   { return instance.subPanelTextureName(); }
        @Override
        public Shape textureClip()    { return textureClip; }
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);

            int w = getWidth();
            int h = getHeight();
            g.setFont(narrowFont(24));
            String title = text("PLANETS_TOTAL_INCOME");
            int sw = g.getFontMetrics().stringWidth(title);
            int x0 = (w-sw)/2;
            drawShadowedString(g, title, 2, x0, s35, palette.black, SystemPanel.orangeText);

            int margin = s10;
            int border = s10;
            int x1 = border; int w1 = w-x1-border;
            int y1 = s45; int h1 = h-y1-s10;
            g.setColor(palette.medBack);
            g.fillRect(x1, y1, w1, h1);

            textureClip = new Rectangle(x1,y1,w1,h1);

            int midP = x1 + (w1*3/5);
            int amtP = midP+s90;
            x1 += margin;
            w1 = w1 - (2*margin);
            y1 += s25;
            g.setFont(narrowFont(20));
            String label = text("PLANETS_INCOME_TRADE");
            sw = g.getFontMetrics().stringWidth(label);
            drawShadowedString(g, label, 2, midP-sw, y1, SystemPanel.textShadowC, SystemPanel.whiteText);

            String val = text("PLANETS_AMT_BC", df1.format(player().netTradeIncome()));
            sw = g.getFontMetrics().stringWidth(val);
            g.setColor(palette.black);
            g.drawString(val, amtP-sw, y1);

            y1 += s25;
            label = text("PLANETS_INCOME_PLANETS");
            sw = g.getFontMetrics().stringWidth(label);
            drawShadowedString(g, label, 2, midP-sw, y1, SystemPanel.textShadowC, SystemPanel.whiteText);
            val = text("PLANETS_AMT_BC", df1.format(player().totalPlanetaryIncome()));
            sw = g.getFontMetrics().stringWidth(val);
            g.setColor(palette.black);
            g.drawString(val, amtP-sw, y1);

            // draw divider line
            y1 += s15;
            g.setColor(Color.black);
            g.fillRect(x1,y1,w1,s3/2);

            y1 += s25;
            label = text("PLANETS_INCOME_TOTAL");
            sw = g.getFontMetrics().stringWidth(label);
            drawShadowedString(g, label, 2, midP-sw, y1, SystemPanel.textShadowC, SystemPanel.whiteText);
            val = text("PLANETS_AMT_BC", df1.format(player().totalIncome()));
            sw = g.getFontMetrics().stringWidth(val);
            g.setColor(palette.black);
            g.drawString(val, amtP-sw, y1);
        }
    }
    class ReserveUI extends BasePanel implements MouseListener, MouseMotionListener, MouseWheelListener {
        private static final long serialVersionUID = 1L;
        final Color sliderHighlightColor = new Color(255,255,255);
        final Color sliderBoxEnabled = new Color(34,140,142);
        final Color sliderBackEnabled = Color.black;
        private final Polygon leftArrow = new Polygon();
        private final Polygon rightArrow = new Polygon();
        private final Rectangle sliderBox = new Rectangle();
        private Shape hoverBox;
        // polygon coordinates for left & right increment buttons
        private final int leftButtonX[] = new int[3];
        private final int leftButtonY[] = new int[3];
        private final int rightButtonX[] = new int[3];
        private final int rightButtonY[] = new int[3];
        Shape textureClip;
        public ReserveUI() {
            initModel();
        }
        private void initModel() {
            setOpaque(false);
            setPreferredSize(new Dimension(scaled(300),getHeight()));
            addMouseListener(this);
            addMouseMotionListener(this);
            addMouseWheelListener(this);
        }
        @Override
        public String textureName()   { return instance.subPanelTextureName(); }
        @Override
        public Shape textureClip()    { return textureClip; }
        @Override
        public void paintComponent(Graphics g0) {
            super.paintComponent(g0);

            Graphics2D g = (Graphics2D) g0;
            int w = getWidth();
            int h = getHeight();
            g.setFont(narrowFont(24));
            String title = text("PLANETS_TREASURY");
            int sw = g.getFontMetrics().stringWidth(title);
            int x0 = (w-sw)/2;
            drawShadowedString(g, title, 2, x0, s35, palette.black, SystemPanel.orangeText);

            int border = s10;
            int x1 = border; int w1 = w-x1-border;
            int y1 = s45; int h1 = h-y1-s10;
            g.setColor(palette.medBack);
            g.fillRect(x1, y1, w1, h1);

            textureClip = new Rectangle(x1,y1,w1,h1);

            int midP = x1 + (w1*3/5);
            y1 += s20;
            g.setFont(narrowFont(20));
            String label = text("PLANETS_TREASURY_FUNDS");
            sw = g.getFontMetrics().stringWidth(label);
            drawShadowedString(g, label, 2, midP-sw, y1, SystemPanel.textShadowC, SystemPanel.whiteText);

            g.setColor(palette.black);
            String text = text("PLANETS_AMT_BC", (int)player().totalReserve());
            g.drawString(text, midP+s10, y1);

            y1 += s10;

            List<String> lines = scaledNarrowWrappedLines(g, text("PLANETS_TAX_DESC"), w-s40, 1, 15, 12);
            for (String line: lines) {
                y1 += s15;
                g.drawString(line, s20, y1);
            }

            y1 += s30;
            x1 = s20;
            g.setFont(narrowFont(16));
            String lbl = text("PLANETS_RESERVE_TAX");
            sw = g.getFontMetrics().stringWidth(lbl);
            g.drawString(lbl, x1, y1);

            x1 = x1+sw+s5;
            int boxW=s100;
            drawSliderBox(g, x1, y1-s15, boxW, s18);

            String result;
            if (player().empireTaxLevel() > 0)
                result = text("PLANETS_RESERVE_INCREASE", fmt(player().empireTaxRevenue(),1));
            else
                result = text("PLANETS_RESERVE_NO_TAX");
            g.setFont(narrowFont(16));
            g.setColor(palette.black);
            x1 = x1+boxW+s5;
            g.drawString(result, x1, y1);
        }
        private void drawSliderBox(Graphics2D g, int x, int y, int w, int h) {
            int leftMargin = x;
            int rightMargin = x+w;
            int buttonW = s10;
            int buttonBottomY = y+h;
            int buttonTopY = y;
            int buttonMidY = (buttonTopY+buttonBottomY)/2;
            int boxBorderW = s3;

            int boxL = x+s10;
            int boxTopY = y;
            int boxW = w-s20;
            int boxH =h;
            // slider
            leftButtonX[0] = leftMargin; leftButtonX[1] = leftMargin+buttonW; leftButtonX[2] = leftMargin+buttonW;
            leftButtonY[0] = buttonMidY; leftButtonY[1] = buttonTopY; leftButtonY[2] = buttonBottomY;

            rightButtonX[0] = rightMargin; rightButtonX[1] = rightMargin-buttonW; rightButtonX[2] = rightMargin-buttonW;
            rightButtonY[0] = buttonMidY; rightButtonY[1] = buttonTopY; rightButtonY[2] = buttonBottomY;

            Color c1 = sliderBoxEnabled;
            Color c2 = sliderBackEnabled;

            Color c3 = hoverBox == leftArrow ? SystemPanel.yellowText : sliderBackEnabled;
            g.setColor(c3);
            g.fillPolygon(leftButtonX, leftButtonY, 3);

            c3 = hoverBox == rightArrow ? SystemPanel.yellowText : sliderBackEnabled;
            g.setColor(c3);
            g.fillPolygon(rightButtonX, rightButtonY, 3);

            leftArrow.reset();
            rightArrow.reset();
            for (int i=0;i<leftButtonX.length;i++) {
                leftArrow.addPoint(leftButtonX[i], leftButtonY[i]);
                rightArrow.addPoint(rightButtonX[i], rightButtonY[i]);
            }

            sliderBox.x = boxL;
            sliderBox.y = boxTopY;
            sliderBox.width = boxW;
            sliderBox.height = boxH;

            g.setFont(narrowFont(18));
            String pctAmt = text("PLANETS_AMT_PCT", player().empireTaxLevel());
            int sw = g.getFontMetrics().stringWidth(pctAmt);
            int y0 = boxTopY+boxH-s3;
            int x0 = sliderBox.x+((sliderBox.width-sw)/2);

            g.setColor(c2);
            int coloredW = boxW-(2*boxBorderW);
            g.setClip(boxL+boxBorderW, boxTopY, coloredW, boxH);
            g.fillRect(boxL+boxBorderW, boxTopY, coloredW, boxH);


            if (player().empireTaxLevel() > 0) {
                if (player().empireTaxLevel() < player().maxEmpireTaxLevel()) {
                    coloredW = coloredW*player().empireTaxLevel() / player().maxEmpireTaxLevel();
                    g.setClip(boxL+boxBorderW, boxTopY, coloredW, boxH);
                }
                g.setColor(c1);
                g.fillRect(boxL+boxBorderW, boxTopY+s2, boxW-(2*boxBorderW), boxH-s3);
            }

            g.setClip(null);

            g.setColor(palette.white);
            g.drawString(pctAmt, x0, y0);

            if (hoverBox == sliderBox) {
                g.setColor(SystemPanel.yellowText);
                Stroke prev = g.getStroke();
                g.setStroke(stroke2);
                g.drawRect(boxL+s3, boxTopY+s1, boxW-s6, boxH-s2);
                g.setStroke(prev);
            }
        }
        public void decrement(boolean click) {
            if (player().decrementEmpireTaxLevel()) {
                if (click)
                    softClick();
                repaint();
                planetDisplayPane.repaint();

            }
            else if (click)
                misClick();
        }
        public void increment(boolean click) {
            if (player().incrementEmpireTaxLevel()) {
                if (click)
                    softClick();
                repaint();
                planetDisplayPane.repaint();
            }
            else if (click)
                misClick();
        }
        public float pctBoxSelected(int x, int y) {
            int bw = s3; // border width
            int minX = sliderBox.x+bw;
            int maxX = sliderBox.x+sliderBox.width-bw;

            if ((x < minX)
            || (x > maxX)
            || (y < (sliderBox.y+bw))
            || (y > (sliderBox.y+sliderBox.height-bw)))
                return -1;

            float num = x - minX;
            float den = maxX-minX;
            return num/den;
        }
        @Override
        public void mouseClicked(MouseEvent e) { }
        @Override
        public void mousePressed(MouseEvent e) { }
        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getButton() > 3)
                return;
            int x = e.getX();
            int y = e.getY();
            if (leftArrow.contains(x,y))
                decrement(true);
            else if (rightArrow.contains(x,y))
                increment(true);
            else {
                float pct = pctBoxSelected(x,y);
                if (pct >= 0) {
                    int newLevel = (int) Math.ceil(pct*player().maxEmpireTaxLevel());
                    softClick();
                    player().empireTaxLevel(newLevel);
                    repaint();
                    planetDisplayPane.repaint();
                }
            }
        }
        @Override
        public void mouseEntered(MouseEvent e) {  }
        @Override
        public void mouseExited(MouseEvent e) {
            if (hoverBox != null) {
                hoverBox = null;
                repaint();
            }
        }
        @Override
        public void mouseDragged(MouseEvent e) { }
        @Override
        public void mouseMoved(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();

            Shape newHover = null;
            if (sliderBox.contains(x,y))
                newHover = sliderBox;
            else if (leftArrow.contains(x,y))
                newHover = leftArrow;
            else if (rightArrow.contains(x,y))
                newHover = rightArrow;

            if (newHover != hoverBox) {
                hoverBox = newHover;
                repaint();
            }
        }
        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            int rot = e.getWheelRotation();
            if (hoverBox == sliderBox) {
                if (rot > 0)
                    decrement(false);
                else if (rot  < 0)
                    increment(false);
            }
        }
    }
    class ExitPlanetsButton extends ExitButton {
        private static final long serialVersionUID = 1L;
        public ExitPlanetsButton(int w, int h, int vMargin, int hMargin) {
            super(w, h, vMargin, hMargin);
        }
        @Override
        protected void clickAction(int numClicks) {
            displayedSystems = null;
            finish(true);
        }
    }
    class UpAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent ev) {
            if (listingUI.scrollUp())
                instance.repaint();
        }
    }
    class DownAction extends AbstractAction {
        private static final long serialVersionUID = 1L;
        @Override
        public void actionPerformed(ActionEvent ev) {
            if (listingUI.scrollDown())
                instance.repaint();
        }
    }
    class CancelAction extends AbstractAction {
        private static final long serialVersionUID = 1L;
        @Override
        public void actionPerformed(ActionEvent ev) {
            displayedSystems = null;
            setFieldValues(selectedSystem());
            finish(false);
        }
    }
}
