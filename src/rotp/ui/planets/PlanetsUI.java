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
import java.awt.CardLayout;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import rotp.model.colony.Colony;
import rotp.model.empires.Empire;
import rotp.model.galaxy.StarSystem;
import rotp.model.ships.Design;
import rotp.ui.BasePanel;
import rotp.ui.BaseTextField;
import rotp.ui.ExitButton;
import rotp.ui.RotPUI;
import rotp.ui.SystemViewer;
import rotp.ui.fleets.FleetUI;
import rotp.ui.fleets.SystemListingUI;
import rotp.ui.fleets.SystemListingUI.Column;
import rotp.ui.fleets.SystemListingUI.DataView;
import static rotp.ui.fleets.SystemListingUI.LEFT;
import static rotp.ui.fleets.SystemListingUI.RIGHT;
import rotp.ui.game.HelpUI;
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
    private static final String SINGLE_PLANET_PANEL = "SinglePlanet";
    private static final String MULTI_PLANET_PANEL = "MultiPlanet";

    private static final Color selectedC = new Color(178,124,87);
    private static final Color unselectedC = new Color(112,85,68);
    private static final Color sliderBoxBlue = new Color(34,140,142);
    static final Color enabledArrowColor = Color.black;
    static final Color disabledArrowColor = new Color(65,65,65);

    private static Palette palette;
    private static BaseTextField notesField;
    private static BaseTextField nameField;
    final static int UP_ACTION = 1;
    final static int DOWN_ACTION = 2;
    final static String CANCEL_ACTION = "cancel-input";

    private int pad = 10;
    private int helpFrame = 0;
    private List<StarSystem> displayedSystems;
    private final HashMap<Integer, DataView> views = new HashMap<>();

    Rectangle ecologyBox = new Rectangle();
    Rectangle industryBox = new Rectangle();
    Rectangle militaryBox = new Rectangle();

    private final TransferReserveUI transferReservePane;
    private final PlanetDisplayPanel planetDisplayPane;
    private final MultiPlanetDisplayPanel multiPlanetDisplayPane;
    private final PlanetViewSelectionPanel viewSelectionPane;
    private EmpireColonySpendingPane spendingPane;
    private EmpireColonyFoundedPane colonyFoundedPane;
    private MultiColonySpendingPane multiSpendingPane;

    BasePanel rightPlanetPanel;
    private final CardLayout planetCardLayout = new CardLayout();

    private final PlanetsUI instance;
    private final PlanetListingUI planetListing;
    private PlanetDataListingUI listingUI;
    private LinearGradientPaint backGradient;

    public PlanetsUI() {
        palette = Palette.named("Brown");
        pad = s10;
        instance = this;

        initTextFields();

        viewSelectionPane = new PlanetViewSelectionPanel(this);
        transferReservePane = new TransferReserveUI();
        planetDisplayPane = new PlanetDisplayPanel(this);
        multiPlanetDisplayPane = new MultiPlanetDisplayPanel(this);
        planetListing = new PlanetListingUI(this);

        initModel();
    }
    public void init() {
        displayedSystems = null;
        nameField.setFont(narrowFont(20));
        notesField.setFont(narrowFont(20));
        listingUI.open();
    }
    private void initModel() {
        BasePanel centerPanel = new BasePanel();
        centerPanel.setOpaque(false);
        centerPanel.setLayout(new BorderLayout());
        centerPanel.add(planetListing, BorderLayout.CENTER);
        centerPanel.add(new EmpireRevenueUI(), BorderLayout.SOUTH);

        rightPlanetPanel = new BasePanel();
        rightPlanetPanel.setOpaque(false);
        rightPlanetPanel.setLayout(planetCardLayout);
        rightPlanetPanel.add(planetDisplayPane, SINGLE_PLANET_PANEL);
        rightPlanetPanel.add(multiPlanetDisplayPane, MULTI_PLANET_PANEL);

        BasePanel rightPanel = new BasePanel();
        rightPanel.setOpaque(false);
        rightPanel.setLayout(new BorderLayout(0,s22));
        rightPanel.add(rightPlanetPanel, BorderLayout.NORTH);
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
    private void showSinglePlanetPanel() { planetCardLayout.show(rightPlanetPanel, SINGLE_PLANET_PANEL); }
    private void showMultiPlanetPanel() { planetCardLayout.show(rightPlanetPanel, MULTI_PLANET_PANEL); }
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
            public void keyReleased(KeyEvent e) { setFieldValues(lastSelectedSystem()); }
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
            public void keyReleased(KeyEvent e) { setFieldValues(lastSelectedSystem()); }
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
    public void cancelHelp() {
        helpFrame = 0;
        RotPUI.helpUI().close();
    }
    @Override
    public void showHelp() {
        helpFrame = 1;
        loadHelpUI();
        repaint();   
    }
    @Override 
    public void advanceHelp() {
        if (helpFrame == 0)
            return;
        helpFrame++;
        if (helpFrame > 2) 
            cancelHelp();
        loadHelpUI();
        repaint();
    }
    private void loadHelpUI() {
        HelpUI helpUI = RotPUI.helpUI();
        helpUI.clear();

        if (helpFrame == 0)
            return;

        switch(helpFrame) {
            case 1: loadHelpUI1(); break;
            case 2:
                switch(selectedMode) {
                    case ECOLOGY_MODE: loadHelpUI2a(); break;
                    case INDUSTRY_MODE: loadHelpUI2b(); break;
                    case MILITARY_MODE: loadHelpUI2c(); break;
                }
                break;
        }
        helpUI.open(this);
    }
    private void loadHelpUI1() {
        int w = getWidth();
        HelpUI helpUI = RotPUI.helpUI();

        int x1 = scaled(150);
        int w1 = scaled(400);
        int y1 = scaled(270);
        HelpUI.HelpSpec sp1 = helpUI.addBrownHelpText(x1, y1, w1, 4, text("PLANETS_HELP_ALL"));

        int x2 = ecologyBox.x;
        int w2 = scaled(200);
        int y2 = scaled(80);
        HelpUI.HelpSpec sp2 = helpUI.addBrownHelpText(x2, y2, w2, 3, text("PLANETS_HELP_1A"));
        sp2.setLine(x2+(w2/2), y2, x2+(w2/2), scaled(52));
        
        int x3 = industryBox.x;
        int w3 = scaled(200);
        int y3 = scaled(80);
        HelpUI.HelpSpec sp3 = helpUI.addBrownHelpText(x3, y3, w3, 3, text("PLANETS_HELP_1B"));
        sp3.setLine(x3+(w3/2), y3, x3+(w3/2), scaled(52));
        
        int x4 = militaryBox.x;
        int w4 = scaled(200);
        int y4 = scaled(80);
        HelpUI.HelpSpec sp4 = helpUI.addBrownHelpText(x4, y4, w4, 3, text("PLANETS_HELP_1C"));
        sp4.setLine(x4+(w4/2), y4, x4+(w4/2), scaled(52));
        
        int x5 = w-scaled(490);
        int w5 = scaled(210);
        int y5 = scaled(330);
        HelpUI.HelpSpec sp5 = helpUI.addBrownHelpText(x5, y5, w5, 3, text("PLANETS_HELP_1D"));
        sp5.setLine(x5+w5, y5+(sp5.height()/2), w-scaled(245), y5+(sp5.height()/2));
        
        int y6 = scaled(500);
        HelpUI.HelpSpec sp6 = helpUI.addBrownHelpText(x5, y6, w5, 4, text("PLANETS_HELP_1E"));
        sp6.setLine(x5+w5, y6+sp6.height(), w-scaled(230), scaled(645));
        
        int x7 = scaled(30);
        int w7 = scaled(210);
        int y7 = scaled(500);
        HelpUI.HelpSpec sp7 = helpUI.addBrownHelpText(x7,y7,w7, 3, text("PLANETS_HELP_1F"));
        sp7.setLine(scaled(80), y7+sp7.height(), scaled(80), scaled(660));
        
        int x9 = w-scaled(740);
        int w9 = scaled(230);
        int y9 = scaled(470);
        HelpUI.HelpSpec sp9 = helpUI.addBrownHelpText(x9,y9,w9, 6, text("PLANETS_HELP_1H"));
        sp9.setLine(x9+(w9/2), y9+sp9.height(), w-scaled(480), scaled(710));
       
        int w8 = scaled(210);
        int x8 = x7+w7+((x9-x7-w7-w8)/2);  // center this box between the x7 & x9 boxes
        int y8 = scaled(500);
        HelpUI.HelpSpec sp8 = helpUI.addBrownHelpText(x8,y8,w8, 3, text("PLANETS_HELP_1G"));
        sp8.setLine(scaled(400), y8+sp8.height(), scaled(400), scaled(660));
        
    }
    private void loadHelpUI2a() {
        HelpUI helpUI = RotPUI.helpUI();

        int x1 = scaled(200);
        int w1 = scaled(400);
        int y1 = scaled(350);
        HelpUI.HelpSpec sp1 = helpUI.addBrownHelpText(x1, y1, w1, 4, text("PLANETS_HELP_ALL"));

        int x1a = scaled(10);
        int w1a = scaled(130);
        int y1a = scaled(190);
        HelpUI.HelpSpec sp1a = helpUI.addBrownHelpText(x1a, y1a, w1a, 3, text("PLANETS_HELP_4I"));
        sp1a.setLine(scaled(50), y1a, scaled(50), s100);
        
        int x2 = scaled(60);
        int w2 = scaled(160);
        int y2 = scaled(100);
        HelpUI.HelpSpec sp2 = helpUI.addBrownHelpText(x2, y2, w2, 3, text("PLANETS_HELP_2A"));
        sp2.setLine(scaled(150), y2, scaled(150), s77);
       
        int x3 = scaled(150);
        int w3 = scaled(190);
        int y3 = scaled(190);
        HelpUI.HelpSpec sp3 = helpUI.addBrownHelpText(x3, y3, w3, 3, text("PLANETS_HELP_2B"));
        sp3.setLine(scaled(240), y3, scaled(240), s77);
        
        int x4 = scaled(255);
        int w4 = scaled(190);
        int y4 = scaled(100);
        HelpUI.HelpSpec sp4 = helpUI.addBrownHelpText(x4, y4, w4, 3, text("PLANETS_HELP_2C"));
        sp4.setLine(scaled(385), y4, scaled(385), s77);
        
        int x5 = scaled(370);
        int w5 = scaled(190);
        int y5 = scaled(190);
        HelpUI.HelpSpec sp5 = helpUI.addBrownHelpText(x5, y5, w5, 3, text("PLANETS_HELP_2E"));
        sp5.setLine(scaled(460), y5, scaled(460), s77);
        
        int x6 = scaled(475);
        int w6 = scaled(140);
        int y6 = scaled(100);
        HelpUI.HelpSpec sp6 = helpUI.addBrownHelpText(x6, y6, w6, 3, text("PLANETS_HELP_2D"));
        sp6.setLine(scaled(550), y6, scaled(550), s77);
        
        int x7 = scaled(590);
        int w7 = scaled(210);
        int y7 = scaled(190);
        HelpUI.HelpSpec sp7 = helpUI.addBrownHelpText(x7,y7,w7, 3, text("PLANETS_HELP_2F"));
        sp7.setLine(scaled(630), y7, scaled(630), s77);
        
        int x8 = scaled(700);
        int w8 = scaled(210);
        int y8 = scaled(100);
        HelpUI.HelpSpec sp8 = helpUI.addBrownHelpText(x8,y8,w8, 3, text("PLANETS_HELP_2G"));
        sp8.setLine(scaled(805), y8, scaled(805), s77);
    }
    private void loadHelpUI2b() {
        HelpUI helpUI = RotPUI.helpUI();

        int x1 = scaled(200);
        int w1 = scaled(400);
        int y1 = scaled(350);
        HelpUI.HelpSpec sp1 = helpUI.addBrownHelpText(x1, y1, w1, 4, text("PLANETS_HELP_ALL"));

        int x1a = scaled(10);
        int w1a = scaled(130);
        int y1a = scaled(190);
        HelpUI.HelpSpec sp1a = helpUI.addBrownHelpText(x1a, y1a, w1a, 3, text("PLANETS_HELP_4I"));
        sp1a.setLine(scaled(50), y1a, scaled(50), s100);
        
        int x2 = scaled(60);
        int w2 = scaled(160);
        int y2 = scaled(100);
        HelpUI.HelpSpec sp2 = helpUI.addBrownHelpText(x2, y2, w2, 3, text("PLANETS_HELP_3A"));
        sp2.setLine(scaled(150), y2, scaled(150), s77);
        
        int x3 = scaled(150);
        int w3 = scaled(190);
        int y3 = scaled(190);
        HelpUI.HelpSpec sp3 = helpUI.addBrownHelpText(x3, y3, w3, 3, text("PLANETS_HELP_3B"));
        sp3.setLine(scaled(240), y3, scaled(240), s77);
        
        int x4 = scaled(255);
        int w4 = scaled(190);
        int y4 = scaled(100);
        HelpUI.HelpSpec sp4 = helpUI.addBrownHelpText(x4, y4, w4, 3, text("PLANETS_HELP_3C"));
        sp4.setLine(scaled(385), y4, scaled(385), s77);
        
        int x5 = scaled(370);
        int w5 = scaled(190);
        int y5 = scaled(190);
        HelpUI.HelpSpec sp5 = helpUI.addBrownHelpText(x5, y5, w5, 3, text("PLANETS_HELP_3D"));
        sp5.setLine(scaled(460), y5, scaled(460), s77);
        
        int x6 = scaled(475);
        int w6 = scaled(140);
        int y6 = scaled(100);
        HelpUI.HelpSpec sp6 = helpUI.addBrownHelpText(x6, y6, w6, 3, text("PLANETS_HELP_3E"));
        sp6.setLine(scaled(565), y6, scaled(565), s77);
        
        int x9 = scaled(590);
        int w9 = scaled(210);
        int y9 = scaled(190);
        HelpUI.HelpSpec sp9 = helpUI.addBrownHelpText(x9,y9,w9, 3, text("PLANETS_HELP_3H"));
        sp9.setLine(scaled(630), y9, scaled(630), s77);
        
        int x7 = scaled(640);
        int w7 = scaled(180);
        int y7 = scaled(100);
        HelpUI.HelpSpec sp7 = helpUI.addBrownHelpText(x7,y7,w7, 3, text("PLANETS_HELP_3F"));
        sp7.setLine(scaled(690), y7, scaled(690), s77);
        
        int x8 = scaled(810);
        int w8 = scaled(210);
        int y8 = scaled(190);
        HelpUI.HelpSpec sp8 = helpUI.addBrownHelpText(x8,y8,w8, 3, text("PLANETS_HELP_3G"));
        sp8.setLine(scaled(835), y8, scaled(835), s77);
    }
    private void loadHelpUI2c() {
        HelpUI helpUI = RotPUI.helpUI();

        int x1 = scaled(200);
        int w1 = scaled(400);
        int y1 = scaled(350);
        HelpUI.HelpSpec sp1 = helpUI.addBrownHelpText(x1, y1, w1, 4, text("PLANETS_HELP_ALL"));

        int x1a = scaled(10);
        int w1a = scaled(130);
        int y1a = scaled(190);
        HelpUI.HelpSpec sp1a = helpUI.addBrownHelpText(x1a, y1a, w1a, 3, text("PLANETS_HELP_4I"));
        sp1a.setLine(scaled(50), y1a, scaled(50), s100);
        
        int x2 = scaled(60);
        int w2 = scaled(160);
        int y2 = scaled(100);
        HelpUI.HelpSpec sp2 = helpUI.addBrownHelpText(x2, y2, w2, 3, text("PLANETS_HELP_4A"));
        sp2.setLine(scaled(150), y2, scaled(150), s77);
        
        int x3 = scaled(150);
        int w3 = scaled(190);
        int y3 = scaled(190);
        HelpUI.HelpSpec sp3 = helpUI.addBrownHelpText(x3, y3, w3, 3, text("PLANETS_HELP_4B"));
        sp3.setLine(scaled(240), y3, scaled(240), s77);
        
        int x4 = scaled(255);
        int w4 = scaled(190);
        int y4 = scaled(100);
        HelpUI.HelpSpec sp4 = helpUI.addBrownHelpText(x4, y4, w4, 3, text("PLANETS_HELP_4C"));
        sp4.setLine(scaled(385), y4, scaled(385), s77);
        
        int x5 = scaled(350);
        int w5 = scaled(190);
        int y5 = scaled(190);
        HelpUI.HelpSpec sp5 = helpUI.addBrownHelpText(x5, y5, w5, 3, text("PLANETS_HELP_4D"));
        sp5.setLine(scaled(460), y5, scaled(460), s77);
        
        int x6 = scaled(475);
        int w6 = scaled(140);
        int y6 = scaled(100);
        HelpUI.HelpSpec sp6 = helpUI.addBrownHelpText(x6, y6, w6, 3, text("PLANETS_HELP_4E"));
        sp6.setLine(scaled(530), y6, scaled(530), s77);
        
        int x7 = scaled(560);
        int w7 = scaled(200);
        int y7 = scaled(190);
        HelpUI.HelpSpec sp7 = helpUI.addBrownHelpText(x7,y7,w7, 3, text("PLANETS_HELP_4F"));
        sp7.setLine(scaled(630), y7, scaled(630), s92, scaled(600), s77);
        
        int x8 = scaled(650);
        int w8 = scaled(200);
        int y8 = scaled(100);
        HelpUI.HelpSpec sp8 = helpUI.addBrownHelpText(x8,y8,w8, 3, text("PLANETS_HELP_4G"));
        sp8.setLine(scaled(670), y8, scaled(670), s77);
        
        int x9 = scaled(795);
        int w9 = scaled(210);
        int y9 = scaled(190);
        HelpUI.HelpSpec sp9 = helpUI.addBrownHelpText(x9,y9,w9, 3, text("PLANETS_HELP_4H"));
        sp9.setLine(scaled(865), y9, scaled(865), s77);
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
        boolean shift = e.isShiftDown();
        boolean control = e.isControlDown();
        switch(e.getKeyCode()) {
            case KeyEvent.VK_F1:
                showHelp();
                return;
            case KeyEvent.VK_ESCAPE:
                if (frame().getGlassPane().isVisible())
                    disableGlassPane();
                else
                    finish(false);
                return;
            case KeyEvent.VK_TAB:
                if (!shift)
                    viewSelectionPane.selectNextTab();
                else
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
            case KeyEvent.VK_F:
                colonyFoundedPane.toggleFlagColor(shift); 
                instance.repaint();
                return;
            case KeyEvent.VK_A:
                if (control) {
                    selectAllSystems();
                }
                return;
            case KeyEvent.VK_S:
                multiSpendingPane.selectCat(0);
                return;
            case KeyEvent.VK_D:
                multiSpendingPane.selectCat(1);
                return;
            case KeyEvent.VK_I:
                multiSpendingPane.selectCat(2);
                return;
            case KeyEvent.VK_E:
                multiSpendingPane.selectCat(3);
                return;
            case KeyEvent.VK_T:
                multiSpendingPane.selectCat(4);
                return;
        }
        if (repaint)
            repaint();
    }
    private List<StarSystem> allSystems()  {
        if (displayedSystems == null) {
            displayedSystems = player().allColonizedSystems();
            if (lastSelectedSystem() == null)
                selectedSystem(displayedSystems.get(0), false);
            selectedSystem(lastSelectedSystem(), false);
        }
        return displayedSystems;
    }
    private StarSystem anchorSystem() {
        StarSystem sys = (StarSystem) sessionVar("COLONYUI_ANCHOR_SYSTEM");

        if (sys == null) {
            sys = (StarSystem) sessionVar("MAINUI_SELECTED_SYSTEM");
            sessionVar("COLONYUI_ANCHOR_SYSTEM", sys);  
        }
        return sys;
    }
    private void setAnchorSystem(StarSystem sys, boolean updateFieldValues) {
        notesField.setVisible(false);
        nameField.setVisible(false);
        RotPUI.instance().requestFocusInWindow();
        if (updateFieldValues)
            setFieldValues(lastSelectedSystem());
        sessionVar("COLONYUI_ANCHOR_SYSTEM", sys);
        notesField.setText(sys.notes());
        notesField.setCaretPosition(notesField.getText().length());
        nameField.setText(player().sv.name(sys.id));
        nameField.setCaretPosition(nameField.getText().length());
    }
    @Override
    public StarSystem systemViewToDisplay()  { return lastSelectedSystem(); }

    private List<StarSystem> selectedSystems(){
        List<StarSystem> systems = (List<StarSystem>)sessionVar("COLONYUI_SELECTED_SYSTEMS");
        if (systems == null) {
            systems = new ArrayList<>();
            sessionVar("COLONYUI_SELECTED_SYSTEMS", systems);
        }
        return systems;
    }
    private void selectAllSystems() {
        List<StarSystem> selectedSystems = selectedSystems();
        selectedSystems.clear();
        selectedSystems.addAll(allSystems());
        
        if (selectedSystems.size() > 1)
            showMultiPlanetPanel();
        repaint();
        setAnchorSystem(selectedSystems().get(0), true);
    }
    private void shiftSelectedSystem(StarSystem sys, boolean updateFieldValues) {
        StarSystem anchor = anchorSystem();
        List<StarSystem> allSystems = allSystems();

        int prevAnchorIndex = allSystems.indexOf(anchor);
        int newAnchorIndex = allSystems.indexOf(sys);
        
        List<StarSystem> selectedSystems = selectedSystems();
        selectedSystems.clear();
        
        if (prevAnchorIndex == newAnchorIndex) {
            selectedSystems.add(sys);
            showSinglePlanetPanel();
            return;
        }
        
        int start = min(prevAnchorIndex, newAnchorIndex);
        int end = max(prevAnchorIndex, newAnchorIndex);
        
        for (int i=start;i<=end;i++) {
            StarSystem sys1 = allSystems.get(i);
            selectedSystems.add(sys1);
        }
        showMultiPlanetPanel();
        
        setAnchorSystem(sys, updateFieldValues);
    }
    private boolean controlSelectedSystem(StarSystem sys, boolean updateFieldValues) {
        List<StarSystem> systems = selectedSystems();

        boolean toggled = true;
        if (systems.contains(sys)) {
            if (systems.size() == 1)
                toggled = false;
            else
                systems.remove(sys);
        }
        else
            systems.add(sys);
        
        if (toggled) 
            setAnchorSystem(sys, updateFieldValues);
        
        if (systems.size() == 1)
            showSinglePlanetPanel();
        else
            showMultiPlanetPanel();
        
        return toggled;
    }
    private synchronized void selectedSystem(StarSystem sys, boolean updateFieldValues) {
        sessionVar("MAINUI_SELECTED_SYSTEM", sys);
        sessionVar("MAINUI_CLICKED_SPRITE", sys);
        
        List<StarSystem> systems = selectedSystems();
        systems.clear();
        systems.add(sys);
        
        setAnchorSystem(sys, updateFieldValues);
        showSinglePlanetPanel();
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
    private StarSystem lastSelectedSystem() {
        StarSystem sys = anchorSystem();
        Empire pl = player();
        int id = sys.id;
        if (pl.sv.empire(id) != pl)
            sys = pl.defaultSystem();
        return (sys == null) || !allSystems().contains(sys) ? null : sys;
    }
    private void initDataViews() {
        Column rowNumCol =  listingUI.newRowNumColumn("PLANETS_LIST_NUM", 15, RIGHT);
        Column flagCol = listingUI.newSystemFlagColumn("", "FLAG", 30, palette.black, StarSystem.VFLAG, LEFT);
        Column nameCol = listingUI.newSystemNameColumn(nameField, "PLANETS_LIST_NAME", "NAME", 170, palette.black, StarSystem.NAME, LEFT);
        Column populationCol = listingUI.newSystemDeltaDataColumn("PLANETS_LIST_POPULATION", "POPULATION", 90, palette.black, StarSystem.POPULATION, RIGHT);
        Column sizeCol = listingUI.newSystemDataColumn("PLANETS_LIST_SIZE", "SIZE", 60, palette.black, StarSystem.CURRENT_SIZE, RIGHT);
        Column pTypeCol = listingUI.newPlanetTypeColumn("PLANETS_LIST_TYPE", "PLANET_TYPE", 90, StarSystem.PLANET_TYPE);
        Column wasteCol = listingUI.newSystemDataColumn("PLANETS_LIST_WASTE", "WASTE", 75, palette.black, StarSystem.WASTE, RIGHT);
        Column notesCol = listingUI.newSystemNotesColumn(notesField, "PLANETS_LIST_NOTES", "NOTES", 999, palette.black);
        Column factoriesCol = listingUI.newSystemDeltaDataColumn("PLANETS_LIST_FACTORIES", "FACTORIES", 90, palette.black, StarSystem.FACTORIES, RIGHT);
        Column productionCol = listingUI.newSystemDataColumn("PLANETS_LIST_PRODUCTION", "INCOME", 60, palette.black, StarSystem.INCOME, RIGHT);
        Column capacityCol = listingUI.newSystemDataColumn("PLANETS_LIST_CAPACITY", "CAPACITY", 60, palette.black, StarSystem.CAPACITY, RIGHT);
        Column indRsvCol = listingUI.newSystemDataColumn("PLANETS_LIST_RESERVE", "RESERVE", 60, palette.black, StarSystem.INDUSTRY_RESERVE, RIGHT);
        Column basesCol = listingUI.newSystemDeltaDataColumn("PLANETS_LIST_BASES", "BASES", 60, palette.black, StarSystem.BASES, RIGHT);
        Column shieldCol = listingUI.newSystemDataColumn("PLANETS_LIST_SHIELD", "SHIELD", 60, palette.black, StarSystem.SHIELD, RIGHT);
        Column shipCol = listingUI.newSystemDataColumn("PLANETS_LIST_SHIPYARD", "SHIPYARD", 140, palette.black, StarSystem.SHIPYARD, LEFT);
        Column resourceCol = listingUI.newSystemDataColumn("PLANETS_LIST_RESOURCES", "RESOURCES", 90, palette.black, StarSystem.RESOURCES, LEFT);

        DataView ecoView = listingUI.newDataView();
        ecoView.addColumn(rowNumCol);
        ecoView.addColumn(flagCol);
        ecoView.addColumn(nameCol);
        ecoView.addColumn(populationCol);
        ecoView.addColumn(pTypeCol);
        ecoView.addColumn(sizeCol);
        ecoView.addColumn(resourceCol);
        ecoView.addColumn(wasteCol);
        ecoView.addColumn(notesCol);
        views.put(ECOLOGY_MODE, ecoView);

        DataView indView = listingUI.newDataView();
        indView.addColumn(rowNumCol);
        indView.addColumn(flagCol);
        indView.addColumn(nameCol);
        indView.addColumn(populationCol);
        indView.addColumn(resourceCol);
        indView.addColumn(factoriesCol);
        indView.addColumn(productionCol);
        indView.addColumn(capacityCol);
        indView.addColumn(indRsvCol);
        indView.addColumn(notesCol);
        views.put(INDUSTRY_MODE, indView);

        DataView milView = listingUI.newDataView();
        milView.addColumn(rowNumCol);
        milView.addColumn(flagCol);
        milView.addColumn(nameCol);
        milView.addColumn(populationCol);
        milView.addColumn(resourceCol);
        milView.addColumn(productionCol);
        milView.addColumn(capacityCol);
        milView.addColumn(shieldCol);
        milView.addColumn(basesCol);
        milView.addColumn(shipCol);
        milView.addColumn(notesCol);
        views.put(MILITARY_MODE, milView);

        listingUI.selectedColumn(rowNumCol);
    }
    private void finish(boolean disableNextTurn) {
        displayedSystems = null;
        sessionVar("COLONYUI_ANCHOR_SYSTEM", null); 
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
        PlanetsUI parent;
        Rectangle hoverBox;
        Rectangle helpBox = new Rectangle();
        Area textureArea;
        public PlanetViewSelectionPanel(PlanetsUI p) {
            parent = p;
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
            int helpW = s30;
            int x0 = gap+helpW;
            int tabW = (w-helpW-(6*gap))/4;
            String title = text("PLANETS_TITLE", player().raceName());
            title = player().replaceTokens(title, "player");
            String ecoLabel = text("PLANETS_VIEW_ECOLOGY");
            String indLabel =  text("PLANETS_VIEW_INDUSTRY");
            String milLabel =  text("PLANETS_VIEW_MILITARY");

            drawHelpButton(g);

            g.setColor(SystemPanel.orangeText);
            g.setFont(narrowFont(30));

            int y0 = h - s10;
            drawString(g,title, x0,y0);
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
        private void drawHelpButton(Graphics2D g) {
            helpBox.setBounds(s10,s10,s20,s25);
            g.setColor(unselectedC);
            g.fillOval(s10, s10, s20, s25);
            g.setFont(narrowFont(25));
            if (helpBox == hoverBox)
                g.setColor(Color.yellow);
            else
                g.setColor(Color.white);

            drawString(g,"?", s16, s30);
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
                else if (hoverBox == helpBox)
                    parent.showHelp();
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
           else if (helpBox.contains(x,y))
                hoverBox = helpBox;

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
            setPreferredSize(new Dimension(scaled(250), scaled(670)));

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
        public StarSystem systemViewToDisplay() { return lastSelectedSystem(); }
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
            colonyFoundedPane = new EmpireColonyFoundedPane(this, null, unselectedC);
            colonyFoundedPane.repainter = parent;
            empireDetailTopPane.add(colonyFoundedPane, BorderLayout.NORTH);
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
    class MultiPlanetDisplayPanel extends SystemPanel {
        private static final long serialVersionUID = 1L;
        EmpireInfoGraphicPane graphicPane;
        PlanetsUI parent;
        public MultiPlanetDisplayPanel(PlanetsUI p) {
            parent = p;
            init();
        }
        private void init() {
            setOpaque(true);
            setBackground(selectedC);
            setBorder(newEmptyBorder(6,6,6,6));
            setPreferredSize(new Dimension(scaled(250), scaled(670)));

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
        public List<StarSystem> systemsToDisplay() { return instance.selectedSystems(); }
        @Override
        public StarSystem systemViewToDisplay() { return lastSelectedSystem(); }
        @Override
        public void repaintAll()                { instance.repaint(); }
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
            colonyFoundedPane = new EmpireColonyFoundedPane(this, null, unselectedC);
            empireDetailTopPane.add(colonyFoundedPane, BorderLayout.NORTH);
            empireDetailTopPane.add(new EmpireColonyInfoPane(this, unselectedC, palette.bdrHiIn, palette.yellow, palette.bdrLoIn), BorderLayout.CENTER);

            BasePanel empireDetailBottomPane = new BasePanel();
            empireDetailBottomPane.setOpaque(false);
            empireDetailBottomPane.setBorder(new ShadowBorder(palette.bdrHiOut, palette.bdrLoIn));
            empireDetailBottomPane.setLayout(new BorderLayout(0,s3));
            empireDetailBottomPane.setPreferredSize(new Dimension(getWidth(),scaled(110)));
            empireDetailBottomPane.add(new ColonyShipPane(this), BorderLayout.CENTER);
            //empireDetailBottomPane.add(new ColonyTransferFunds(this), BorderLayout.CENTER);
            empireDetailBottomPane.setBorder(newEmptyBorder(0,0,0,0));

            multiSpendingPane = new MultiColonySpendingPane(this, unselectedC, palette.white, palette.bdrHiOut, palette.bdrLoIn);
            BasePanel empireDetailPane = new BasePanel();
            empireDetailPane.setOpaque(false);
            empireDetailPane.setLayout(new BorderLayout(0,s3));
            empireDetailPane.add(empireDetailTopPane, BorderLayout.NORTH);
            empireDetailPane.add(multiSpendingPane, BorderLayout.CENTER);
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

            StarSystem sys = null;
            List<StarSystem> systems = parent.systemsToDisplay();
            if (systems == null) {
                systems = new ArrayList<>();
                sys = parent.systemViewToDisplay();
                if (sys != null)
                    systems.add(sys);
            }
            
            if (systems.isEmpty())
                return;
            
            if (systems.size() == 1)
                sys = systems.get(0);

            StarSystem anchorSys = lastSelectedSystem();
            
            Empire pl = player();
            int w = getWidth();
            int h = getHeight();

            Graphics2D g2 = (Graphics2D) g;
            g2.drawImage(pl.sv.starBackground(this), 0, 0, null);
            drawStar(g2, anchorSys.starType(), s80, getWidth()/3, s70);
            starCircle.setFrame((getWidth()/3)-s20, s10, s40, s40);

            g.setFont(narrowFont(36));
            String str = sys == null ? text("PLANETS_MULTI_SYSTEMS", systems.size()) : player().sv.name(sys.id);
            scaledFont(g, str, w-s30, 36, 24);
            int y0 = s42;
            int x0 = s25;
            drawBorderedString(g, str, 2, x0, y0, Color.black, SystemPanel.orangeText);

            int x1 = s20;
            int y1 = s70;
            int r = s40;
            anchorSys.planet().draw(g, w, h, x1, y1, r+r, 45);
            planetCircle.setFrame(x1, y1, r+r, r+r);
            
            if (sys != null)
                parent.drawPlanetInfo(g2, sys, false, false, s40, getWidth(), getHeight()-s12);
        }
        @Override
        public void animate() {
            if (animationCount() % 3 == 0) {
                try {
                    lastSelectedSystem().planet().rotate(1);
                    repaint();
                }
                catch (Exception e) { }
            }
        }
    }
    class ColonyShipPane extends BasePanel implements MouseListener, MouseMotionListener, MouseWheelListener {
        private static final long serialVersionUID = 1L;
        SystemPanel parent;
        public Design currDesign;
        public int currBuildLimit;
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
        private final Polygon upArrow = new Polygon();
        private final Polygon downArrow = new Polygon();
        private final int upButtonX[] = new int[3];
        private final int upButtonY[] = new int[3];
        private final int downButtonX[] = new int[3];
        private final int downButtonY[] = new int[3];
        protected Rectangle limitBox = new Rectangle();


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
            scaledFont(g, str, getWidth()-s10, 20, 16);
            drawShadowedString(g, str, 2, s5, s22, MainUI.shadeBorderC(), textColor);
        }
        private List<Colony> colonies() {
            List<StarSystem> systems = parent.systemsToDisplay();
            if (systems == null) {
                systems = new ArrayList<>();
                StarSystem sys = parent.systemViewToDisplay();
                if (sys != null)
                    systems.add(sys);
            }
            
            List<Colony> colonies = new ArrayList<>();
            if (systems.isEmpty())
                return colonies;           
            
            for (StarSystem sys1: systems) {
                Colony c = sys1.colony();
                if (c != null)
                    colonies.add(c);
            }
            return colonies;
        }
        private void drawShipIcon(Graphics2D g, int x, int y, int w, int h) {
            g.setColor(Color.black);
            g.fillRect(x, y, w, h);
            
            List<Colony> colonies = colonies();
            if (colonies.isEmpty())
                return;

            shipDesignBox.setBounds(x,y,w,h);

            currDesign = colonies.get(0).shipyard().design();
            for (Colony c: colonies) {
                if (currDesign != c.shipyard().design()) {
                    currDesign = null;
                    break;
                }
            }
            if ((currDesign != null) && currDesign.scrapped())
                currDesign = null;
            g.drawImage(initializedBackgroundImage(w, h), x,y, null);

            // draw design image
            if (currDesign != null) {
                Image img = currDesign.image();
                int w0 = img.getWidth(null);
                int h0 = img.getHeight(null);
                float scale = min((float)w/w0, (float)h/h0);

                int w1 = (int)(scale*w0);
                int h1 = (int)(scale*h0);
                int x1 = x+(w - w1) / 2;
                int y1 = y+(h - h1) / 2;
                g.drawImage(img, x1, y1, x1+w1, y1+h1, 0, 0, w0, h0, this);
            }

            if (hoverBox == shipDesignBox) {
                Stroke prevStroke = g.getStroke();
                g.setStroke(stroke2);
                g.setColor(SystemPanel.yellowText);
                g.drawRect(x, y, w, h);
                g.setStroke(prevStroke);
            }

            // draw expected build count
            if (colonies.size() == 1) {
                int i = colonies.get(0).shipyard().upcomingShipCount();
                if (i == 0)
                    return;
                String count = Integer.toString(i);
                g.setColor(SystemPanel.yellowText);
                g.setFont(narrowFont(20));
                int sw = g.getFontMetrics().stringWidth(count);
                drawString(g,count, x+w-s5-sw, y+h-s5);
            }
        }
        private void drawShipCompletion(Graphics2D g, int x, int y, int w, int h) {
            List<Colony> colonies = colonies();
            if (colonies.isEmpty())
                return;

            currBuildLimit = colonies.get(0).shipyard().buildLimit();
            for (Colony c: colonies) 
                currBuildLimit = min(currBuildLimit,c.shipyard().buildLimit());
            
            String limitStr = currBuildLimit == 0 ? text("MAIN_COLONY_SHIPYARD_LIMIT_NONE") : str(currBuildLimit); 

            g.setFont(narrowFont(16));
            g.setColor(Color.black);
            String label = text("MAIN_COLONY_SHIPYARD_LIMIT");
            int sw1 = g.getFontMetrics().stringWidth(label);
            String none = text("MAIN_COLONY_SHIPYARD_LIMIT_NONE");
            int sw2 = g.getFontMetrics().stringWidth(none);           
            String amt = limitStr;
            int sw3 = g.getFontMetrics().stringWidth(amt);
            
            int x1 = x+s12;
            int y1 = y+s8;
            int x2 = x1+sw1+s5;
            int x3 = x1+sw1+s5+max(sw2,sw3)+s5;
            int y3 = y1+s2;
            drawString(g,label, x1, y1);
            drawString(g,amt, x2, y1);  
            
            limitBox.setBounds(x2-s3,y1-s15,x3-x2,s18);
            if (hoverBox == limitBox) {
                Stroke prevStroke = g.getStroke();
                g.setStroke(stroke2);
                g.setColor(SystemPanel.yellowText);
                g.draw(limitBox);
                g.setStroke(prevStroke);
            }

            upButtonX[0] = x3+s6; upButtonX[1] = x3; upButtonX[2] = x3+s12;
            upButtonY[0] = y3-s17; upButtonY[1] = y3-s9; upButtonY[2] = y3-s9;

            downButtonX[0] = x3+s6; downButtonX[1] = x3; downButtonX[2] = x3+s12;
            downButtonY[0] = y3; downButtonY[1] = y3-s8; downButtonY[2] = y3-s8;

            g.setColor(enabledArrowColor);
            g.fillPolygon(upButtonX, upButtonY, 3);

            if (currBuildLimit == 0)
                g.setColor(disabledArrowColor);
            else
                g.setColor(enabledArrowColor);
            g.fillPolygon(downButtonX, downButtonY, 3);

            upArrow.reset();
            downArrow.reset();
            for (int i=0;i<upButtonX.length;i++) {
                upArrow.addPoint(upButtonX[i], upButtonY[i]);
                downArrow.addPoint(downButtonX[i], downButtonY[i]);
            }
            Stroke prevStroke = g.getStroke();
            g.setStroke(stroke2);
            if (hoverBox == upArrow) {
                g.setColor(SystemPanel.yellowText);
                g.drawPolygon(upArrow);
            }
            else if ((hoverBox == downArrow)
                && (currBuildLimit > 0)) {
                g.setColor(SystemPanel.yellowText);
                g.drawPolygon(downArrow);
            }
            g.setStroke(prevStroke);
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
            String name = currDesign == null ? text("FLEETS_MULTIPLE_DESIGNS") : currDesign.name();
            scaledFont(g, name, barW-s5, 18, 8);
            int sw = g.getFontMetrics().stringWidth(name);
            int x0 = barX+((barW-sw)/2);
            drawString(g,name, x0, barY+s16);

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
        private boolean anyHaveStargate() {
            List<Colony> colonies = colonies();
            for (Colony c: colonies) {
                if (c.shipyard().hasStargate())
                    return true;
            }
            return false;
        }
        public void nextShipDesign(boolean click) {
            boolean hasStargate = anyHaveStargate();
            Design prev = currDesign;
            currDesign = player().shipLab().nextDesignFrom(currDesign, hasStargate);
            if (currDesign != prev) {
                if (click)
                    softClick();
                setAllShipDesigns();
                instance.repaint();
            }
        }
        public void prevShipDesign(boolean click) {
            boolean hasStargate = anyHaveStargate();
            Design prev = currDesign;
            currDesign = player().shipLab().prevDesignFrom(currDesign, hasStargate);
            if (currDesign != prev) {
                if (click)
                    softClick();
                setAllShipDesigns();
                instance.repaint();
            }
        }
        private void setAllShipDesigns() {
            List<Colony> colonies = colonies();
            for (Colony c: colonies) 
                c.shipyard().switchToDesign(currDesign);
        }
        private void incrementBuildLimit() {
            currBuildLimit++;
            List<Colony> colonies = colonies();
            softClick();
            for (Colony c: colonies)
                c.shipyard().buildLimit(currBuildLimit);
            
            parent.repaint();
        }
        private void decrementBuildLimit() {
            if (currBuildLimit == 0)
                return;
            currBuildLimit--;
            List<Colony> colonies = colonies();
            softClick();
            for (Colony c: colonies)
                c.shipyard().buildLimit(currBuildLimit);
            
            parent.repaint();
        }
        private void resetBuildLimit() {
            StarSystem sys = parent.systemViewToDisplay();
            Colony col = sys == null ? null : sys.colony();
            if (col == null)
                return;
            boolean updated = col.shipyard().resetBuildLimit();
            if (updated) {
                softClick();
                repaint();
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
            boolean rightClick = SwingUtilities.isRightMouseButton(e);

            if (upArrow.contains(x,y))
                incrementBuildLimit();
            else if (downArrow.contains(x,y)) 
                decrementBuildLimit();
            else if (limitBox.contains(x,y))
                resetBuildLimit();
            else if (shipDesignBox.contains(x,y)){
                if (rightClick)
                    prevShipDesign(true);
                else
                    nextShipDesign(true);
                instance.repaint();
            }
            else if (shipNameBox.contains(x,y)){
                if (rightClick)
                    prevShipDesign(true);
                else
                    nextShipDesign(true);
                instance.repaint();
            }
            else if (nextDesign.contains(x,y)){
                nextShipDesign(true);
                instance.repaint();
            }
            else if (prevDesign.contains(x,y)){
                prevShipDesign(true);
                instance.repaint();
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

            if (upArrow.contains(x,y))
                hoverBox = upArrow;
            else if (downArrow.contains(x,y))
                hoverBox = downArrow;
            else if (limitBox.contains(x,y))
                hoverBox = limitBox;
            else if (shipDesignBox.contains(x,y))
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
            int x = e.getX();
            int y = e.getY();
            
            if (limitBox.contains(x,y)) {
                if (e.getWheelRotation() < 0)
                    incrementBuildLimit();
                else
                    decrementBuildLimit();
                return;
            }
            if (shipDesignBox.contains(x,y) 
            || shipNameBox.contains(x,y)) {
                if (e.getWheelRotation() < 0)
                    nextShipDesign(false);
                else
                    prevShipDesign(false);
                return;
            }
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
                drawString(g,line, x0, y0);
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
                transferReservePane.targetSystem(lastSelectedSystem());
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
        protected List<StarSystem> systems() { return instance.allSystems();  }
        @Override
        protected StarSystem lastSelectedSystem() { return instance.lastSelectedSystem(); }
        @Override
        protected void selectedSystem(StarSystem sys, boolean updateFieldValues) {
            instance.selectedSystem(sys, updateFieldValues);
        }
        @Override
        protected void shiftSelectedSystem(StarSystem sys, boolean updateFieldValues) {
            instance.shiftSelectedSystem(sys, updateFieldValues);
        }
        @Override
        protected void controlSelectedSystem(StarSystem sys, boolean updateFieldValues) {
            instance.controlSelectedSystem(sys, updateFieldValues);
        }
        @Override
        protected boolean isSelected(StarSystem sys) { 
            return instance.selectedSystems().contains(sys);
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
                drawString(g,line, x1+s20, y1);
                y1 += s16;
            }

            g.setFont(narrowFont(18));

            // calculate column widthss
            String lbl1 = text("PLANETS_COSTS_SHIPS");
            int sw1 = g.getFontMetrics().stringWidth(lbl1);
            String lbl2 = text("PLANETS_COSTS_BASES");
            int sw2 = g.getFontMetrics().stringWidth(lbl2);
            String lbl3 = text("PLANETS_COSTS_STARGATES");
            int sw3 = g.getFontMetrics().stringWidth(lbl3);
            int col1W = max(sw1,sw2,sw3);

            String lbl4 = text("PLANETS_COSTS_SPYING");
            int sw4 = g.getFontMetrics().stringWidth(lbl4);
            String lbl5 = text("PLANETS_COSTS_SECURITY");
            int sw5 = g.getFontMetrics().stringWidth(lbl5);
            int col2W = max(sw4,sw5);

            // s50 is width for data columns.. s20 is spacing
            int totalW = col1W+col2W+s50+s50;
            int spacing = (w-totalW)/3;
            
            // LEFT COLUMN
            x1 = spacing;
            w1 = col1W+s50;
            y1 = h-s60;
            int midX = x1+col1W;
            drawShadowedString(g, lbl1, 2, midX-sw1, y1, SystemPanel.textShadowC, SystemPanel.whiteText);

            String val = text("PLANETS_AMT_PCT", fmt(100*player().shipMaintCostPerBC(),1));
            sw = g.getFontMetrics().stringWidth(val);
            g.setColor(palette.black);
            drawString(g,val, x1+w1-sw, y1);

            y1 = h-s40;
            drawShadowedString(g, lbl2, 2, midX-sw2, y1, SystemPanel.textShadowC, SystemPanel.whiteText);
            val = text("PLANETS_AMT_PCT", fmt(100*player().missileBaseCostPerBC(),1));
            sw = g.getFontMetrics().stringWidth(val);
            g.setColor(palette.black);
            drawString(g,val, x1+w1-sw, y1);


            y1 = h-s20;
            drawShadowedString(g, lbl3, 2, midX-sw3, y1, SystemPanel.textShadowC, SystemPanel.whiteText);
            val = text("PLANETS_AMT_PCT", fmt(100*player().stargateCostPerBC(),1));
            sw = g.getFontMetrics().stringWidth(val);
            g.setColor(palette.black);
            drawString(g,val, x1+w1-sw, y1);

            // RIGHT COLUMN
            int x2 = x1+w1+spacing;
            int w2 = col2W+s50;
            y1 = h-s60;
            midX = x2+col2W;
            drawShadowedString(g, lbl4, 2, midX-sw4, y1, SystemPanel.textShadowC, SystemPanel.whiteText);
            val = text("PLANETS_AMT_PCT", fmt(100*player().totalSpyCostPct(),1));
            sw = g.getFontMetrics().stringWidth(val);
            g.setColor(palette.black);
            drawString(g,val, x2+w2-sw, y1);

            y1 = h-s40;
            drawShadowedString(g, lbl5, 2, midX-sw5, y1, SystemPanel.textShadowC, SystemPanel.whiteText);
            val = text("PLANETS_AMT_PCT", fmt(100*player().internalSecurityCostPct(),1));
            sw = g.getFontMetrics().stringWidth(val);
            g.setColor(palette.black);
            drawString(g,val, x2+w2-sw, y1);
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
            drawString(g,val, amtP-sw, y1);

            y1 += s25;
            label = text("PLANETS_INCOME_PLANETS");
            sw = g.getFontMetrics().stringWidth(label);
            drawShadowedString(g, label, 2, midP-sw, y1, SystemPanel.textShadowC, SystemPanel.whiteText);
            val = text("PLANETS_AMT_BC", df1.format(player().totalPlanetaryIncome()));
            sw = g.getFontMetrics().stringWidth(val);
            g.setColor(palette.black);
            drawString(g,val, amtP-sw, y1);

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
            drawString(g,val, amtP-sw, y1);
        }
    }
    class ReserveUI extends BasePanel implements MouseListener, MouseMotionListener, MouseWheelListener {
        private static final long serialVersionUID = 1L;
        final Color sliderHighlightColor = new Color(255,255,255);
        final Color sliderBoxEnabled = new Color(34,140,142);
        final Color sliderBackEnabled = Color.black;
        private final Polygon leftArrow = new Polygon();
        private final Polygon rightArrow = new Polygon();
        private final Rectangle reserveBox = new Rectangle();
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
            String text = text("PLANETS_AMT_BC", shortFmt(player().totalReserve()));
            drawString(g,text, midP+s10, y1);

            y1 += s10;
            int lineH = s15;
            List<String> lines = scaledNarrowWrappedLines(g, text("PLANETS_TAX_DESC"), w-s40, 1, 15, 12);
            if (lines.size() > 1) {
                lineH = s12;
                y1 -= s5;
            }
            for (String line: lines) {
                y1 += lineH;
                drawString(g,line, s20, y1);
            }

            int boxW=s100;
            y1 += s27;
            x1 = s20;
            String lbl = text("PLANETS_RESERVE_TAX");
            scaledFont(g, lbl, ((w-boxW)/2)-s15, 16, 13);
            sw = g.getFontMetrics().stringWidth(lbl);
            drawString(g,lbl, x1, y1);

            x1 = x1+sw+s5;
            drawSliderBox(g, x1, y1-s15, boxW, s18);

            String result;
            if (player().empireTaxLevel() > 0) {
                float revenue = player().empireTaxRevenue();
                String revStr;
                if (revenue < 100)
                    revStr = fmt(player().empireTaxRevenue(),1);
                else
                    revStr = shortFmt(revenue);
                result = text("PLANETS_RESERVE_INCREASE", revStr);
            }
            else
                result = text("PLANETS_RESERVE_NO_TAX");
            
            x1 = x1+boxW+s5;
            scaledFont(g, result, w-x1-border-s5, 16, 13);
            g.setColor(palette.black);
            drawString(g,result, x1, y1);
            

             // draw check box
            g.setFont(narrowFont(14));
            String opt = text("PLANETS_RESERVE_ONLY_DEVELOPED"); 
            int optSW = g.getFontMetrics().stringWidth(opt);
            int checkW = s12;
            int totalW = checkW+s6+optSW;
            int checkX=(w-totalW)/2;
            
            y1 += s25;
            reserveBox.setBounds(checkX, y1-checkW, checkW, checkW);
            int labelX = checkX+checkW+s6;
            Stroke prev = g.getStroke();
            g.setStroke(stroke2);
            g.setColor(FleetUI.backHiC);
            g.fill(reserveBox);
            if (hoverBox == reserveBox) {
                g.setColor(Color.yellow);
                g.draw(reserveBox);
            }
            if (player().empireTaxOnlyDeveloped()) {
                g.setColor(SystemPanel.whiteText);
                g.drawLine(checkX-s1, y1-s6, checkX+s3, y1-s3);
                g.drawLine(checkX+s3, y1-s3, checkX+checkW, y1-s12);
            }
            g.setStroke(prev);
            g.setColor(palette.black);
            drawString(g,opt,labelX,y1);
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
            drawString(g,pctAmt, x0, y0);

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
            if (reserveBox.contains(x,y)) {
                player().toggleEmpireTaxOnlyDeveloped();
                repaint();
                planetDisplayPane.repaint();
            }
            else if (leftArrow.contains(x,y))
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
            else if (reserveBox.contains(x,y))
                newHover = reserveBox;
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
        private static final long serialVersionUID = 1L;
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
            setFieldValues(lastSelectedSystem());
            finish(false);
        }
    }
}
