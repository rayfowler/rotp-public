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
package rotp.ui.design;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.border.Border;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import rotp.Rotp;
import rotp.model.galaxy.Ships;
import rotp.model.ships.*;
import rotp.ui.*;
import rotp.ui.combat.ShipBattleUI;
import rotp.ui.game.HelpUI;
import rotp.ui.main.SystemPanel;
import rotp.util.AnimationManager;
import rotp.util.Base;
import rotp.util.ImageColorizer;
import rotp.util.Palette;

public class DesignUI extends BasePanel {
    private static final long serialVersionUID = 1L;
    public static DesignUI instance;
    static Palette palette;

    private static final Color lightBrown = new Color(178,124,87);
    private static final Color brown = new Color(110,79,56);
    private static final Color darkBrown = new Color(112,85,68);
    private static final Color darkerBrown = new Color(75,55,39);
    private static final Color darkestBrown = new Color(50,36,26);

    private final Color grayEdgeC = new Color(59,59,59);
    private final Color grayMidC = new Color(93,93,93);
    private final Color greenEdgeC = new Color(44,59,30);
    private final Color greenMidC = new Color(70,93,48);
    private final Color redEdgeC = new Color(72,14,14);
    private final Color redMidC = new Color(126,28,28);
    private final Color brownEdgeC = new Color(59,44,30);
    private final Color brownMidC = new Color(93,70,48);

    private final Color grayShadeC = new Color(255,255,255,60);
    private final Color yellowShadeC = new Color(255, 255, 0, 108);
    private final Color errorRedC = new Color(224,0,0);

    private int selectedSlot = 0;
    private int helpFrame = 0;

    private DesignSlotsPanel designSlotsPanel;
    DesignSlotPanel[] designPanels = new DesignSlotPanel[ShipDesignLab.MAX_DESIGNS];
    DesignConfigPanel configPanel;

    private int pad = 10;
    private LinearGradientPaint backGradient;
    private LinearGradientPaint configGradient;
    private LinearGradientPaint clearBackground;
    private LinearGradientPaint renameBackground;
    private LinearGradientPaint scrapBackground;
    private LinearGradientPaint createBackground;
    private LinearGradientPaint copyBackground;
    List<BufferedImage> shipImages = new ArrayList<>();

    private Shape hoverTarget;
    private final Rectangle clearButtonArea = new Rectangle();
    private final Rectangle renameButtonArea = new Rectangle();
    private final Rectangle scrapButtonArea = new Rectangle();
    private final Rectangle createButtonArea = new Rectangle();
    private final Rectangle[] copyButtonArea = new Rectangle[ShipDesignLab.MAX_DESIGNS];
    private final Rectangle shipImageArea = new Rectangle();
    private final Polygon shipImageDecr0 = new Polygon();
    private final Polygon shipImageIncr0 = new Polygon();
    private final Rectangle shipImageDecr = new Rectangle();
    private final Rectangle shipImageIncr = new Rectangle();
    private final Rectangle sizeFieldArea = new Rectangle();
    private final Polygon sizeFieldDecr = new Polygon();
    private final Polygon sizeFieldIncr = new Polygon();
    private final Rectangle engineFieldArea = new Rectangle();
    private final Polygon engineFieldDecr = new Polygon();
    private final Polygon engineFieldIncr = new Polygon();
    private final Rectangle computerFieldArea = new Rectangle();
    private final Polygon computerFieldDecr = new Polygon();
    private final Polygon computerFieldIncr = new Polygon();
    private final Rectangle armorFieldArea = new Rectangle();
    private final Polygon armorFieldDecr = new Polygon();
    private final Polygon armorFieldIncr = new Polygon();
    private final Rectangle shieldsFieldArea = new Rectangle();
    private final Polygon shieldsFieldDecr = new Polygon();
    private final Polygon shieldsFieldIncr = new Polygon();
    private final Rectangle ecmFieldArea = new Rectangle();
    private final Polygon ecmFieldDecr = new Polygon();
    private final Polygon ecmFieldIncr = new Polygon();
    private final Rectangle maneuverFieldArea = new Rectangle();
    private final Polygon maneuverFieldDecr = new Polygon();
    private final Polygon maneuverFieldIncr = new Polygon();
    private final Rectangle[] weaponFieldArea = new Rectangle[ShipDesign.maxWeapons];
    private final Polygon[] weaponFieldDecr = new Polygon[ShipDesign.maxWeapons];
    private final Polygon[] weaponFieldIncr = new Polygon[ShipDesign.maxWeapons];
    private final Rectangle[] weaponCountArea = new Rectangle[ShipDesign.maxWeapons];
    private final Polygon[] weaponCountDecr = new Polygon[ShipDesign.maxWeapons];
    private final Polygon[] weaponCountIncr = new Polygon[ShipDesign.maxWeapons];
    private final Rectangle[] specialsFieldArea = new Rectangle[ShipDesign.maxSpecials];
    private final Polygon[] specialsFieldDecr = new Polygon[ShipDesign.maxSpecials];
    private final Polygon[] specialsFieldIncr = new Polygon[ShipDesign.maxSpecials];
    private final Rectangle[] shipColorArea = new Rectangle[12];

    private final DesignComputerSelectionUI computerSelectionUI;
    private final DesignShieldSelectionUI shieldSelectionUI;
    private final DesignEcmSelectionUI ecmSelectionUI;
    private final DesignArmorSelectionUI armorSelectionUI;
    private final DesignEngineSelectionUI engineSelectionUI;
    private final DesignManeuverSelectionUI maneuverSelectionUI;
    private final DesignWeaponSelectionUI weaponSelectionUI;
    private final DesignSpecialSelectionUI specialSelectionUI;
    private final ConfirmScrapUI confirmScrapUI;
    private final ConfirmCreateUI confirmCreateUI;
    
    BufferedImage shipPaneImg;

    int[] shipCounts;
    int[] orbitCounts;
    int[] inTransitCounts;
    int[] constructionCounts;
    int displayShipW = -1;
    int displayShipH = -1;
    int shipSlotW = -1;

    public DesignUI() {
        instance = this;
        pad = s10;
        palette = Palette.named("Brown");
        initModel();
        // must be created after palette is set
        computerSelectionUI = new DesignComputerSelectionUI();
        shieldSelectionUI     = new DesignShieldSelectionUI();
        ecmSelectionUI           = new DesignEcmSelectionUI();
        armorSelectionUI       = new DesignArmorSelectionUI();
        engineSelectionUI     = new DesignEngineSelectionUI();
        maneuverSelectionUI = new DesignManeuverSelectionUI();
        weaponSelectionUI     = new DesignWeaponSelectionUI();
        specialSelectionUI   = new DesignSpecialSelectionUI();
        confirmScrapUI   = new ConfirmScrapUI();
        confirmCreateUI   = new ConfirmCreateUI();
        for (int i=0;i<copyButtonArea.length;i++) 
            copyButtonArea[i] = new Rectangle();
        for (int i=0;i<weaponFieldArea.length;i++) {
            weaponFieldArea[i] = new Rectangle();
            weaponFieldDecr[i] = new Polygon();
            weaponFieldIncr[i] = new Polygon();
            weaponCountArea[i] = new Rectangle();
            weaponCountDecr[i] = new Polygon();
            weaponCountIncr[i] = new Polygon();
        }
        for (int i=0;i<specialsFieldArea.length;i++) {
            specialsFieldArea[i] = new Rectangle();
            specialsFieldDecr[i] = new Polygon();
            specialsFieldIncr[i] = new Polygon();
        }
        for (int i=0;i<shipColorArea.length;i++) 
            shipColorArea[i] = new Rectangle();
    }
    public void init() {
        int pid = player().id;
        Ships ships = galaxy().ships;
        constructionCounts = ships.shipDesignConstructionCounts(pid);
        shipCounts = ships.shipDesignCounts(pid);
        orbitCounts = shipCounts;
        inTransitCounts = ships.shipDesignInTransitCounts(pid);
        orbitCounts = new int[shipCounts.length];
        for (int i=0;i<shipCounts.length;i++) {
            orbitCounts[i] = shipCounts[i] - inTransitCounts[i];
        }
    }
    @Override
    public boolean drawMemory()            { return true; }
    @Override
    public void animate() {
        if (!AnimationManager.current().playAnimations())
            return;
        if (frame().getGlassPane().isVisible())
            return;
        if (animationCount() % 3 != 0)
            return;
        configPanel.animate();
    }
    private void initModel() {
        int w = scaled(Rotp.IMG_W);
        int h = scaled(Rotp.IMG_H);
        int rightPaneW = scaled(250);

        setBackground(Color.black);
        Border emptyBorder = newEmptyBorder(0, 8, pad, pad);
        setBorder(emptyBorder);

        // create center panel
        DesignTitlePanel titlePanel = new DesignTitlePanel(this, "SHIP_DESIGN_TITLE");
        configPanel = new DesignConfigPanel();
        BasePanel mainPanel = new BasePanel();
        mainPanel.setOpaque(false);
        mainPanel.setBorder(newEmptyBorder(10,0,0,0));
        mainPanel.setLayout(new BorderLayout(0, s5));
        mainPanel.add(titlePanel, BorderLayout.NORTH);
        mainPanel.add(configPanel, BorderLayout.CENTER);

        // create design slot panel on right side of UI
        designSlotsPanel = new DesignSlotsPanel();

        SlotTitlePanel slotsTitlePanel = new SlotTitlePanel("SHIP_DESIGN_SLOTS");
        BasePanel rightPanel = new BasePanel();
        rightPanel.setPreferredSize(new Dimension(rightPaneW, getHeight()));
        rightPanel.setBorder(newEmptyBorder(10,0,0,0));
        rightPanel.setOpaque(false);
        rightPanel.setLayout(new BorderLayout(0, s5));
        rightPanel.add(slotsTitlePanel, BorderLayout.NORTH);
        rightPanel.add(designSlotsPanel, BorderLayout.CENTER);
        rightPanel.add(new ExitDesignButton(getWidth(), s60, s10, s2), BorderLayout.SOUTH);

        BorderLayout layout0 = new BorderLayout();
        layout0.setHgap(pad);
        setLayout(layout0);
        add(mainPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);
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
        if (helpFrame > 4) 
            cancelHelp();
        loadHelpUI();
        repaint();
    }
    private void loadHelpUI() {
        HelpUI helpUI = RotPUI.helpUI();
        if (helpFrame == 0)
            return;
        
        ShipDesign des = configPanel.shipDesign();
        switch(helpFrame) {
            case 1: loadHelpFrame1(); break;
            case 2: loadHelpFrame2(); break;
            case 3: loadHelpFrame3(); break;
            case 4: if (des.active())
                        loadHelpFrame4A();
                    else
                        loadHelpFrame4B();
                    break;
        }

        helpUI.open(this);
    }
    private void loadHelpFrame1() {
        HelpUI helpUI = RotPUI.helpUI();

        helpUI.clear();
        int x1 = scaled(350);
        int w1 = scaled(430);
        int y1 = scaled(470);
        HelpUI.HelpSpec sp1 = helpUI.addBrownHelpText(x1, y1, w1, 8, text("SHIP_DESIGN_HELP_ALL"));

        int x2 = scaled(120);
        int w2 = scaled(280);
        int y2 = scaled(10);
        HelpUI.HelpSpec sp2 = helpUI.addBrownHelpText(x2, y2, w2, 2, text("SHIP_DESIGN_HELP_1A"));
        sp2.setLine(x2+w2, y2+sp2.height(), scaled(450), scaled(120));
        
        int x3 = scaled(50);
        int w3 = scaled(280);
        int y3 = scaled(70);
        HelpUI.HelpSpec sp3 = helpUI.addBrownHelpText(x3, y3, w3, 2, text("SHIP_DESIGN_HELP_1B"));
        sp3.setLine(x3+w3, y3+(sp3.height()/2), scaled(425), scaled(160));
        
        int x4 = scaled(20);
        int w4 = scaled(280);
        int y4 = scaled(130);
        HelpUI.HelpSpec sp4 = helpUI.addBrownHelpText(x4, y4, w4, 2, text("SHIP_DESIGN_HELP_1C"));
        sp4.setLine(x4+w4, scaled(173), scaled(440), scaled(173), scaled(460),scaled(178));
        
        int x5 = scaled(20);
        int w5 = scaled(280);
        int y5 = scaled(190);
        HelpUI.HelpSpec sp5 = helpUI.addBrownHelpText(x5, y5, w5, 2, text("SHIP_DESIGN_HELP_1D"));
        sp5.setLine(x5+w5, scaled(213), scaled(460), scaled(213), scaled(480), scaled(208));
        
        int x6 = scaled(50);
        int w6 = scaled(280);
        int y6 = scaled(250);
        HelpUI.HelpSpec sp6 = helpUI.addBrownHelpText(x6, y6, w6, 2, text("SHIP_DESIGN_HELP_1E"));
        sp6.setLine(x6+w6,scaled(265), scaled(440), scaled(265), scaled(490), scaled(230));
   
        int x7 = scaled(120);
        int w7 = scaled(280);
        int y7 = scaled(310);
        HelpUI.HelpSpec sp7 = helpUI.addBrownHelpText(x7,y7,w7, 2, text("SHIP_DESIGN_HELP_1F"));
        sp7.setLine(x7+w7, y7, scaled(490), scaled(250));
        
        int x8 = scaled(630);
        int w8 = scaled(280);
        int y8 = scaled(60);
        HelpUI.HelpSpec sp8 = helpUI.addBrownHelpText(x8,y8,w8, 2, text("SHIP_DESIGN_HELP_1G"));
        sp8.setLine(scaled(690), y8+sp8.height(), scaled(662), scaled(155));

        int x9 = scaled(730);
        int w9 = scaled(280);
        int y9 = scaled(120);
        HelpUI.HelpSpec sp9 = helpUI.addBrownHelpText(x9,y9,w9, 2, text("SHIP_DESIGN_HELP_1H"));
        sp9.setLine(x9, y9+(sp9.height()/2), scaled(665), scaled(181));

        int x10 = scaled(730);
        int w10 = scaled(280);
        int y10 = scaled(180);
        HelpUI.HelpSpec sp10 = helpUI.addBrownHelpText(x10,y10,w10, 2, text("SHIP_DESIGN_HELP_1I"));
        sp10.setLine(x10, y10+(sp10.height()/2), scaled(665), scaled(203));        

        int x11 = scaled(730);
        int w11 = scaled(280);
        int y11 = scaled(240);
        HelpUI.HelpSpec sp11 = helpUI.addBrownHelpText(x11,y11,w11, 2, text("SHIP_DESIGN_HELP_1J"));
        sp11.setLine(x11, y11+(sp11.height()/2), scaled(665), scaled(225));        

        int x12 = scaled(630);
        int w12 = scaled(280);
        int y12 = scaled(300);
        HelpUI.HelpSpec sp12 = helpUI.addBrownHelpText(x12,y12,w12, 2, text("SHIP_DESIGN_HELP_1K"));
        sp12.setLine(scaled(690), y12, scaled(665), scaled(250));        

        if (!configPanel.shipDesign().active()) {
            int x13 = scaled(430);
            int w13 = scaled(280);
            int y13 = scaled(360);
            HelpUI.HelpSpec sp13 = helpUI.addBrownHelpText(x13,y13,w13, 2, text("SHIP_DESIGN_HELP_1L"));
            sp13.setLine(scaled(560), y13, scaled(540), scaled(275));   
        }
    }
    private void loadHelpFrame2() {
        HelpUI helpUI = RotPUI.helpUI();

        helpUI.clear();
        int x1 = scaled(350);
        int w1 = scaled(430);
        int y1 = scaled(470);
        HelpUI.HelpSpec sp1 = helpUI.addBrownHelpText(x1, y1, w1, 8, text("SHIP_DESIGN_HELP_ALL"));

        int x2 = scaled(730);
        int w2 = scaled(280);
        int y2 = scaled(10);
        HelpUI.HelpSpec sp2 = helpUI.addBrownHelpText(x2, y2, w2, 2, text("SHIP_DESIGN_HELP_2A"));
        sp2.setLine(x2+(w2/2), y2+sp2.height(), x2+(w2/2), scaled(120));
        
        int x3 = scaled(930);
        int w3 = scaled(280);
        int y3 = scaled(70);
        HelpUI.HelpSpec sp3 = helpUI.addBrownHelpText(x3, y3, w3, 2, text("SHIP_DESIGN_HELP_2B"));
        sp3.setLine(x3, y3+sp3.height(), scaled(895), scaled(160));
        
        int x4 = scaled(930);
        int w4 = scaled(280);
        int y4 = scaled(130);
        HelpUI.HelpSpec sp4 = helpUI.addBrownHelpText(x4, y4, w4, 2, text("SHIP_DESIGN_HELP_2C"));
        sp4.setLine(x4, y4+(sp4.height()/2), scaled(897), scaled(180));
        
        int x5 = scaled(930);
        int w5 = scaled(280);
        int y5 = scaled(190);
        HelpUI.HelpSpec sp5 = helpUI.addBrownHelpText(x5, y5, w5, 2, text("SHIP_DESIGN_HELP_2D"));
        sp5.setLine(x5,  y5+(sp5.height()/2), scaled(897), scaled(200));
        
        int x6 = scaled(930);
        int w6 = scaled(280);
        int y6 = scaled(250);
        HelpUI.HelpSpec sp6 = helpUI.addBrownHelpText(x6, y6, w6, 2, text("SHIP_DESIGN_HELP_2E"));
        sp6.setLine(x6, y6, scaled(897), scaled(220));
   
        int x7 = scaled(400);
        int w7 = scaled(280);
        int y7 = scaled(180);
        HelpUI.HelpSpec sp7 = helpUI.addBrownHelpText(x7,y7,w7, 2, text("SHIP_DESIGN_HELP_2F"));
        sp7.setLine(x7+w7, scaled(228), scaled(855), scaled(228), scaled(875), scaled(235));
        
        int x8 = scaled(400);
        int w8 = scaled(280);
        int y8 = scaled(240);
        HelpUI.HelpSpec sp8 = helpUI.addBrownHelpText(x8,y8,w8, 2, text("SHIP_DESIGN_HELP_2G"));
        sp8.setLine(x8+w8, scaled(273), scaled(855), scaled(273), scaled(875), scaled(265));

        int x9 = scaled(400);
        int w9 = scaled(280);
        int y9 = scaled(300);
        HelpUI.HelpSpec sp9 = helpUI.addBrownHelpText(x9,y9,w9, 2, text("SHIP_DESIGN_HELP_2H"));
        sp9.setLine(x9+w9, y9+(sp9.height()/4), scaled(830), y9+(sp9.height()/4), scaled(875), scaled(295));

        int x10 = scaled(400);
        int w10 = scaled(280);
        int y10 = scaled(360);
        HelpUI.HelpSpec sp10 = helpUI.addBrownHelpText(x10,y10,w10, 2, text("SHIP_DESIGN_HELP_2I"));
        sp10.setLine(x10+w10, y10+(sp10.height()/2), scaled(830), y10+(sp10.height()/2), scaled(880), scaled(310));        

        int x11 = scaled(30);
        int w11 = scaled(280);
        int y11 = scaled(350);
        HelpUI.HelpSpec sp11 = helpUI.addBrownHelpText(x11,y11,w11, 6, text("SHIP_DESIGN_HELP_2J"));
        sp11.setLine(x11+(w11/2), y11, x11+(w11/2), scaled(300));        

        int x12 = scaled(270);
        int w12 = scaled(240);
        int y12 = scaled(10);
        HelpUI.HelpSpec sp12 = helpUI.addBrownHelpText(x12,y12,w12, 2, text("SHIP_DESIGN_HELP_2K"));
        sp12.setLine(scaled(490), y12+sp12.height(), scaled(490), scaled(80));        
    }
    private void loadHelpFrame3() {
        HelpUI helpUI = RotPUI.helpUI();

        helpUI.clear();
        int x1 = scaled(50);
        int w1 = scaled(430);
        int y1 = scaled(80);
        HelpUI.HelpSpec sp1 = helpUI.addBrownHelpText(x1, y1, w1, 8, text("SHIP_DESIGN_HELP_ALL"));

        int x2 = scaled(30);
        int w2 = scaled(280);
        int y2 = scaled(280);
        HelpUI.HelpSpec sp2 = helpUI.addBrownHelpText(x2, y2, w2, 2, text("SHIP_DESIGN_HELP_3A"));
        sp2.setLine(scaled(280), y2+sp2.height(), scaled(290), scaled(363));
        
        int x3 = scaled(380);
        int w3 = scaled(280);
        int y3 = scaled(280);
        HelpUI.HelpSpec sp3 = helpUI.addBrownHelpText(x3, y3, w3, 2, text("SHIP_DESIGN_HELP_3B"));
        sp3.setLine(scaled(430), y3+sp3.height(), scaled(345), scaled(400));
        
        int x4 = scaled(390);
        int w4 = scaled(280);
        int y4 = scaled(420);
        HelpUI.HelpSpec sp4 = helpUI.addBrownHelpText(x4, y4, w4, 2, text("SHIP_DESIGN_HELP_3C"));
        sp4.setLine(x4, y4+(sp4.height()/2), scaled(345), scaled(430));
        
        int x5 = scaled(730);
        int w5 = scaled(280);
        int y5 = scaled(280);
        HelpUI.HelpSpec sp5 = helpUI.addBrownHelpText(x5, y5, w5, 2, text("SHIP_DESIGN_HELP_3D"));
        sp5.setLine(scaled(800),  y5+sp5.height(), scaled(770), scaled(363));
        
        int x6 = scaled(720);
        int w6 = scaled(280);
        int y6 = scaled(420);
        HelpUI.HelpSpec sp6 = helpUI.addBrownHelpText(x6, y6, w6, 2, text("SHIP_DESIGN_HELP_3E"));
        sp6.setLine(scaled(800), y6, scaled(770), scaled(412));
   
        int x7 = scaled(440);
        int w7 = scaled(330);
        int y7 = scaled(495);
        HelpUI.HelpSpec sp7 = helpUI.addBrownHelpText(x7,y7,w7, 4, text("SHIP_DESIGN_HELP_3F"));
        sp7.setLine(x7, y7+(sp7.height()/2), scaled(400), y7+(sp7.height()/2));
        
        int x8 = scaled(440);
        int w8 = scaled(330);
        int y8 = scaled(640);
        HelpUI.HelpSpec sp8 = helpUI.addBrownHelpText(x8,y8,w8, 3, text("SHIP_DESIGN_HELP_3G"));
        sp8.setLine(x8, y8+(sp8.height()/2), scaled(400), y8+(sp8.height()/2));
    }
    private void loadHelpFrame4A() {
        int w = getWidth();
        HelpUI helpUI = RotPUI.helpUI();

        helpUI.clear();
        int x1 = scaled(50);
        int w1 = scaled(430);
        int y1 = scaled(80);
        HelpUI.HelpSpec sp1 = helpUI.addBrownHelpText(x1, y1, w1, 8, text("SHIP_DESIGN_HELP_ALL"));

        int x2 = scaled(30);
        int w2 = scaled(280);
        int y2 = scaled(350);
        HelpUI.HelpSpec sp2 = helpUI.addBrownHelpText(x2, y2, w2, 3, text("SHIP_DESIGN_HELP_4A"));
        sp2.setLine(x2+w2, y2, scaled(342), scaled(320));
        
        int x3 = scaled(380);
        int w3 = scaled(280);
        int y3 = scaled(350);
        HelpUI.HelpSpec sp3 = helpUI.addBrownHelpText(x3, y3, w3, 3, text("SHIP_DESIGN_HELP_4B"));
        sp3.setLine(x3+(w3*2/3), y3, scaled(580), scaled(320));
        
        int y[] = { 60, 168, 276, 384, 492, 600 };
        int firstActive = -1;
        int firstAvailable = -1;
        for (int i=0;i<ShipDesignLab.MAX_DESIGNS;i++) {
            ShipDesign des = player().shipLab().design(i);
            if ((firstActive < 0) && des.active())
               firstActive = i;     
            if ((firstAvailable < 0) && !des.active())
               firstAvailable = i;     
        }
        
        if (firstActive >= 0) {
            int topY = scaled(y[firstActive]);
            int x4 = w-scaled(560);
            int w4 = scaled(280);
            int y4 = topY;
            HelpUI.HelpSpec sp4 = helpUI.addBrownHelpText(x4, y4, w4, 2, text("SHIP_DESIGN_HELP_4C"));
            sp4.setLine(x4+w4, y4+(sp4.height()/2), w-scaled(230), y4+(sp4.height()/2));
        }
        
        if (firstAvailable >= 0) {
            int topY = scaled(y[firstAvailable]);
            int x5 = w-scaled(560);
            int w5 = scaled(280);
            int y5 = topY;
            HelpUI.HelpSpec sp5 = helpUI.addBrownHelpText(x5, y5, w5, 2, text("SHIP_DESIGN_HELP_4D"));
            sp5.setLine(x5+w5, y5+(sp5.height()/2), w-scaled(230), y5+(sp5.height()/2));
        }
    }
    private void loadHelpFrame4B() {
        HelpUI helpUI = RotPUI.helpUI();

        helpUI.clear();
        int x1 = scaled(50);
        int w1 = scaled(430);
        int y1 = scaled(80);
        HelpUI.HelpSpec sp1 = helpUI.addBrownHelpText(x1, y1, w1, 8, text("SHIP_DESIGN_HELP_ALL"));

        int x2 = scaled(30);
        int w2 = scaled(280);
        int y2 = scaled(350);
        HelpUI.HelpSpec sp2 = helpUI.addBrownHelpText(x2, y2, w2, 3, text("SHIP_DESIGN_HELP_4F"));
        sp2.setLine(x2+w2, y2, scaled(342), scaled(320));
        
        int x3 = scaled(380);
        int w3 = scaled(280);
        int y3 = scaled(350);
        HelpUI.HelpSpec sp3 = helpUI.addBrownHelpText(x3, y3, w3, 3, text("SHIP_DESIGN_HELP_4G"));
        sp3.setLine(x3+(w3*2/3), y3, scaled(580), scaled(320));
        
        int y[] = { 60, 168, 276, 384, 492, 600 };
        int firstActive = -1;
        int firstAvailable = -1;
        for (int i=0;i<ShipDesignLab.MAX_DESIGNS;i++) {
            ShipDesign des = player().shipLab().design(i);
            if ((firstActive < 0) && des.active())
               firstActive = i;     
            if ((firstAvailable < 0) && !des.active())
               firstAvailable = i;     
        }
        
        if (firstActive >= 0) {
            int topY = scaled(y[firstActive]);
            int x4 = scaled(670);
            int w4 = scaled(280);
            int y4 = topY;
            HelpUI.HelpSpec sp4 = helpUI.addBrownHelpText(x4, y4, w4, 2, text("SHIP_DESIGN_HELP_4C"));
            sp4.setLine(x4+w4, y4+(sp4.height()/2), scaled(1000), y4+(sp4.height()/2));
            int x6 = scaled(670);
            int w6 = scaled(280);
            int y6 = y4+(sp4.height()+s10);
            HelpUI.HelpSpec sp6 = helpUI.addBrownHelpText(x6, y6, w6, 2, text("SHIP_DESIGN_HELP_4E"));
            sp6.setLine(x6, y6+(sp6.height()/2), scaled(345), scaled(430));
            sp6.setLine(x6+w6, y6+(sp6.height()/2), scaled(1095), y4+scaled(77));
        }
        
        if (firstAvailable >= 0) {
            int topY = scaled(y[firstAvailable]);
            int x5 = scaled(670);
            int w5 = scaled(280);
            int y5 = topY;
            HelpUI.HelpSpec sp5 = helpUI.addBrownHelpText(x5, y5, w5, 2, text("SHIP_DESIGN_HELP_4D"));
            sp5.setLine(x5+w5, y5+(sp5.height()/2), scaled(1000), y5+(sp5.height()/2));
        }
        
    }
    @Override
    public boolean hasStarBackground()     { return true; }
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
        if (frame().getGlassPane().isVisible()) {
            BasePanel selectionPane = (BasePanel) frame().getGlassPane();
            selectionPane.keyPressed(e);
            return;
        }
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_ESCAPE) {
            exit(false);
            return;
        }
        else if ((k == KeyEvent.VK_F1)  || (e.getKeyChar() == '?')) {
            showHelp();
            return;
        }
        else if (k == KeyEvent.VK_DOWN) {
            if (selectedSlot < (designPanels.length - 1)) {
                selectedSlot++;
                configPanel.loadShipImages();
                repaint();
            }
        }
        else if (k == KeyEvent.VK_UP) {
            if (selectedSlot > 0) {
                selectedSlot--;
                configPanel.loadShipImages();
                repaint();
            }
        }

        ShipDesign design = configPanel.shipDesign();
        if (design.active()) {
            if (k == KeyEvent.VK_S) {
                if (player().shipLab().canScrapADesign())
                    configPanel.openScrapDialog();
                return;
            }
            else if (k == KeyEvent.VK_R) {
                    configPanel.openRenameDialog();
                return;
            }
        }
        else {
            if (k == KeyEvent.VK_D) {
                configPanel.openCreateDialog();
                return;
            }
            else if (k == KeyEvent.VK_C) {
                configPanel.clearDesign();
                return;
            }            
        }
    }
    private void exit(boolean pauseNextTurn) {
        configPanel.shipImageIndex = -1;
        shipImages.clear();
        buttonClick();
        RotPUI.instance().selectMainPanel(pauseNextTurn);
    }
    class DesignTitlePanel extends BasePanel implements MouseMotionListener, MouseListener {
        private static final long serialVersionUID = 1L;
        String titleKey;
        Rectangle hoverBox;
        Rectangle helpBox = new Rectangle();
        DesignUI parent;
        public DesignTitlePanel(DesignUI p, String s) {
            parent = p;
            titleKey = s;
            init();
        }
        private void init() {
            setPreferredSize(new Dimension(getWidth(), s45));
            setOpaque(false);
            addMouseListener(this);
            addMouseMotionListener(this);
        }
        @Override
        public void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            String title = text(titleKey);
            int helpW = s30;
            drawHelpButton(g);
            
            g.setFont(narrowFont(32));
            g.setColor(SystemPanel.orangeText);
            drawString(g,title, helpW+s10, s32);
        }
        public void drawHelpButton(Graphics2D g) {
            helpBox.setBounds(s10,s10,s20,s25);
            g.setColor(darkBrown);
            g.fillOval(s10, s10, s20, s25);
            g.setFont(narrowFont(25));
            if (helpBox == hoverBox)
                g.setColor(Color.yellow);
            else
                g.setColor(Color.white);

            drawString(g,"?", s16, s30);
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
                if (hoverBox == helpBox)
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
            if (helpBox.contains(x,y))
                hoverBox = helpBox;

            if (hoverBox != prevHover)
                repaint();
        }
    }
    class SlotTitlePanel extends BasePanel implements MouseListener, MouseMotionListener {
        private static final long serialVersionUID = 1L;
        String titleKey;
        Rectangle hoverBox;
        Rectangle prototypeBox = new Rectangle();
        Rectangle copyButton= new Rectangle();
        public SlotTitlePanel(String s) {
            titleKey = s;
            init();
        }
        private void init() {
            setPreferredSize(new Dimension(getWidth(), s100));
            setOpaque(false);
            addMouseListener(this);
            addMouseMotionListener(this);
        }
        @Override
        public String textureName()     { return TEXTURE_BROWN; }
        @Override
        public Rectangle textureClip()  { return prototypeBox; }
        @Override
        public void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            super.paintComponent(g);
            String title = text(titleKey);
            
            int w = getWidth();
            int h = getHeight();
            copyButton.setBounds(0,0,0,0);
            
            g.setFont(narrowFont(32));
            g.setColor(SystemPanel.orangeText);
            drawString(g,title, s10, s32);
            
            prototypeBox.setBounds(0,s50,w,h-s55);
            int shipW = shipSlotW < 0 ? s95 : shipSlotW;
            g.setColor(lightBrown);
            g.fillRect(0,s50,w,h-s55);
            g.setColor(darkBrown);
            g.fillRect(s5,s55,w-s10,h-s65);
            g.setColor(Color.black);
            g.fillRect(s10,s60,shipW,h-s75);
            int leftM = s10+shipW+s10;
            
            if ((selectedSlot >= 0) && !configPanel.shipDesign().active()) {
                g.setFont(narrowFont(16));
                String str = text("SHIP_DESIGN_COPY_BUTTON");
                int sw = g.getFontMetrics().stringWidth(str);
                int buttonW = sw + s40;
                int buttonH = s20;
                int buttonX = (leftM-buttonW)/2;
                int buttonY = h-buttonH-s18;
                copyButton.setBounds(buttonX, buttonY, buttonW, buttonH);

                if (copyBackground == null) {
                    float[] dist = {0.0f, 0.5f, 1.0f};
                    Point2D ptStart = new Point2D.Float(buttonX, 0);
                    Point2D ptEnd = new Point2D.Float(buttonX + buttonW, 0);
                    Color[] yesColors = {brownEdgeC, brownMidC, brownEdgeC};
                    copyBackground = new LinearGradientPaint(ptStart, ptEnd, dist, yesColors);
                }
                boolean hovering = hoverTarget == copyButton;
                g.setPaint(copyBackground);
                g.fillRoundRect(buttonX, buttonY, buttonW, buttonH, s3, s3);
                Color c0 = hovering ? SystemPanel.yellowText : SystemPanel.whiteText;
                g.setColor(c0);
                Stroke prevStr = g.getStroke();
                g.setStroke(BasePanel.stroke1);
                g.drawRoundRect(buttonX, buttonY, buttonW, buttonH, s3, s3);
                g.setStroke(prevStr);
                int x2a = buttonX + ((buttonW - sw) / 2);
                drawBorderedString(g, str, x2a, buttonY + buttonH - s6, SystemPanel.textShadowC, c0);
            }

            
            String s = text("SHIP_DESIGN_PROTOTYPE");
            g.setFont(narrowFont(18));
            drawShadowedString(g, s, 3, leftM, h-s20, SystemPanel.textShadowC, SystemPanel.whiteText);

            if (selectedSlot < 0) {
                int boxY = s52;
                int boxX = s2;
                g.setStroke(stroke5);
                g.setColor(SystemPanel.yellowText);
                g.drawRect(boxX, boxY, w-s4, h-s59);
            }            
        }
        @Override
        public void mouseClicked(MouseEvent e) { }
        @Override
        public void mousePressed(MouseEvent e) { }
        @Override
        public void mouseReleased(MouseEvent e) {
            if (hoverBox == copyButton) {
                softClick();
                configPanel.shipDesign().copyFrom(player().shipLab().prototypeDesign());
                configPanel.loadShipImages();
                instance.repaint();
                return;
            }
            else if (hoverBox == prototypeBox) {
                selectedSlot = -1;
                softClick();
                configPanel.loadShipImages();
                instance.repaint();
            }
        }
        @Override
        public void mouseEntered(MouseEvent e) {         }
        @Override
        public void mouseExited(MouseEvent e) { }
        @Override
        public void mouseDragged(MouseEvent e) { }
        @Override
        public void mouseMoved(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();
            Rectangle prevHover = hoverBox;
            hoverBox = null;
            if (copyButton.contains(x,y))
                hoverBox = copyButton;
            else if (prototypeBox.contains(x,y))
                hoverBox = prototypeBox;
            
            if (hoverBox != prevHover)
                instance.repaint();
        }
    }
    final class DesignSlotPanel extends BasePanel implements MouseListener, MouseMotionListener {
        private static final long serialVersionUID = 1L;
        int designNum = 0;
        public DesignSlotPanel(int i) {
            designNum = i;
            init();
        }
        private void init() {
            setBackground(darkBrown);
            addMouseListener(this);
            addMouseMotionListener(this);
        }
        @Override
        public void animate() {
            repaint();
        }
        @Override
        public String textureName()     { return TEXTURE_BROWN; }
        @Override
        public void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            super.paintComponent(g);
            g.setFont(narrowFont(32));

            int w = getWidth();
            int boxH = getHeight()-s10;
           
            if (shipSlotW < 0)
                shipSlotW = boxH*6/5;
            int boxW = shipSlotW;
            drawShip(g);

            int leftM = boxW+s10;
            ShipDesign des = slotDesign();
            if (!des.active()) {
                g.setFont(narrowFont(18));
                drawShadowedString(g, text("SHIP_DESIGN_AVAILABLE"), 3, leftM, s20, SystemPanel.textShadowC, SystemPanel.yellowText);
                g.setFont(narrowFont(14));
                g.setColor(SystemPanel.whiteText);
                String desc = designNum == selectedSlot ? text("SHIP_DESIGN_AVAILABLE_DESC2"): text("SHIP_DESIGN_AVAILABLE_DESC");
                List<String> lines = scaledNarrowWrappedLines(g, desc, getWidth()-s5-leftM, 4, 14, 12);
                int y0 = s40;
                for (String line: lines) {
                    drawString(g,line, leftM, y0);
                    y0 += s14;
                }
            }
            else {
                String name = des.name();
                g.setFont(narrowFont(18));
                scaledFont(g, name, w-leftM-s20, 18, 10);
                drawShadowedString(g, name, 3, leftM, s20, SystemPanel.textShadowC, SystemPanel.whiteText);
                g.setFont(narrowFont(14));
                g.setColor(SystemPanel.blackText);
                String desc = text("SHIP_DESIGN_SLOT_DESC");
                int sw1 = g.getFontMetrics().stringWidth(desc);
                
                String desc2 = text("SHIP_DESIGN_SLOT_DESC2");
                int sw2 = g.getFontMetrics().stringWidth(desc2);
                
                String desc3 = text("SHIP_DESIGN_SLOT_DESC3");
                int sw3 = g.getFontMetrics().stringWidth(desc3);
                
                String val1= str(shipCounts[des.id()]);
                int sw1a = g.getFontMetrics().stringWidth(val1);
                String val2= str(orbitCounts[des.id()]);
                int sw2a = g.getFontMetrics().stringWidth(val2);
                String val3= str(inTransitCounts[des.id()]);
                int sw3a = g.getFontMetrics().stringWidth(val3);
                
                int maxSw = max(sw1,sw2,sw3);
                int maxSwa = max(sw1a,sw2a,sw3a);
                drawString(g,desc, leftM,  s38);
                drawString(g,desc2, leftM, s54);
                drawString(g,desc3, leftM, s68);
                drawString(g,val1, leftM+maxSw+s5+maxSwa-sw1a, s40);
                drawString(g,val2, leftM+maxSw+s5+maxSwa-sw2a, s54);
                drawString(g,val3, leftM+maxSw+s5+maxSwa-sw3a, s68);

                if (constructionCounts[des.id()] > 0) {
                    String desc4 = text("SHIP_DESIGN_SLOT_DESC4");
                    int sw4 = g.getFontMetrics().stringWidth(desc4);                
                    String val4= str(constructionCounts[des.id()]);
                    drawString(g,desc4, leftM, s82);
                    drawString(g,val4, leftM+sw4+s5, s82);
                }
            }
            // draw copy button
            copyButtonArea[designNum].setBounds(0, 0, 0, 0);
            if (slotDesign().active() && !configPanel.shipDesign().active()) {
                g.setFont(narrowFont(18));
                String str = text("SHIP_DESIGN_COPY_BUTTON");
                int sw = g.getFontMetrics().stringWidth(str);
                int buttonW = sw + s40;
                int buttonH = s25;
                int buttonX = (leftM-buttonW)/2;
                int buttonY = boxH - s30;
                copyButtonArea[designNum].setBounds(buttonX, buttonY, buttonW, buttonH);

                if (copyBackground == null) {
                    float[] dist = {0.0f, 0.5f, 1.0f};
                    Point2D ptStart = new Point2D.Float(buttonX, 0);
                    Point2D ptEnd = new Point2D.Float(buttonX + buttonW, 0);
                    Color[] yesColors = {brownEdgeC, brownMidC, brownEdgeC};
                    copyBackground = new LinearGradientPaint(ptStart, ptEnd, dist, yesColors);
                }
                boolean hovering = hoverTarget == copyButtonArea[designNum];
                g.setPaint(copyBackground);
                g.fillRoundRect(buttonX, buttonY, buttonW, buttonH, s3, s3);
                Color c0 = hovering ? SystemPanel.yellowText : SystemPanel.whiteText;
                g.setColor(c0);
                Stroke prevStr = g.getStroke();
                g.setStroke(BasePanel.stroke1);
                g.drawRoundRect(buttonX, buttonY, buttonW, buttonH, s3, s3);
                g.setStroke(prevStr);
                int x2a = buttonX + ((buttonW - sw) / 2);
                drawBorderedString(g, str, x2a, buttonY + buttonH - s7, SystemPanel.textShadowC, c0);
            }

        }
        private ShipDesign slotDesign()   { return player().shipLab().design(designNum); }
        private void drawShip(Graphics g) {
            int boxH = getHeight()-s10;
            int boxW = boxH*6/5;
            g.setColor(ShipBattleUI.spaceBlue);
            g.fillRect(s5,s5,boxW,boxH);

            ShipDesign des = slotDesign();
            if (!des.active())
                return;

            Image img = des.image();

            int w0 = img.getWidth(null);
            int h0 = img.getHeight(null);
            float scale = min((float)boxW/w0, (float)boxH/h0);

            int w1 = (int)(scale*w0);
            int h1 = (int)(scale*h0);

            int x1 = (boxW - w1) / 2;
            int y1 = (boxH - h1) / 2;
            g.drawImage(img, x1+s5, y1+s5, x1+w1, y1+h1, 0, 0, w0, h0, this);
        }
        @Override
        public void mouseClicked(MouseEvent mouseEvent) {}
        @Override
        public void mousePressed(MouseEvent mouseEvent) {}
        @Override
        public void mouseReleased(MouseEvent mouseEvent) {
            if (hoverTarget == copyButtonArea[designNum]) {
                softClick();
                configPanel.shipDesign().copyFrom(slotDesign());
                configPanel.loadShipImages();
                instance.repaint();
                return;
            }
            if (selectedSlot != designNum) {
                softClick();
                selectedSlot = designNum;
                configPanel.loadShipImages();
                instance.repaint();
            }

        }
        @Override
        public void mouseEntered(MouseEvent mouseEvent) {}
        @Override
        public void mouseExited(MouseEvent mouseEvent) {
            if (hoverTarget != null) {
                hoverTarget = null;
                repaint();
            }
        }
        @Override
        public void mouseDragged(MouseEvent e) { }
        @Override
        public void mouseMoved(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();
            Shape prevHover = hoverTarget;
            hoverTarget = null;
            if (copyButtonArea[designNum].contains(x,y)) 
                hoverTarget = copyButtonArea[designNum];
                
            if (hoverTarget != prevHover)
                repaint();
        }
    }
    final class DesignSlotsPanel extends BasePanel {
        private static final long serialVersionUID = 1L;
        public DesignSlotsPanel() {
            setBackground(lightBrown);
            setBorder(newEmptyBorder(5,5,5,5));
            for (int i=0;i<designPanels.length;i++)
                designPanels[i] = new DesignSlotPanel(i);
            GridLayout designLayout = new GridLayout(designPanels.length,1);
            designLayout.setVgap(s5);
            setLayout(designLayout);
            for (DesignSlotPanel pnl: designPanels)
                add(pnl);
        }
        @Override
        public void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            super.paintComponent(g);
            int boxH = (getHeight() - s6)/designPanels.length;
            int boxW = (getWidth() - s6);

            if (selectedSlot >= 0) {
                int boxY = s3+(selectedSlot*boxH);
                int boxX = s3;
                g.setStroke(stroke5);
                g.setColor(SystemPanel.yellowText);
                g.drawRect(boxX, boxY, boxW, boxH);
            }
        }
    }
    class ExitDesignButton extends ExitButton {
        private static final long serialVersionUID = 1L;
        public ExitDesignButton(int w, int h, int vMargin, int hMargin) {
            super(w, h, vMargin, hMargin);
        }
        @Override
        protected void clickAction(int numClicks) {
            // force recalcuate map bounds when returning
            exit(true);
        }
    }
    final class DesignConfigPanel extends BasePanel implements MouseListener, MouseMotionListener, MouseWheelListener {
        private static final long serialVersionUID = 1L;
        int shipW = 0;
        int shipH = 0;
        int shipImageIndex = -1;
        public DesignConfigPanel() {
            init();
        }
        private void init() {
            setBackground(darkerBrown);
            setBorder(newEmptyBorder(5,5,5,5));
            addMouseMotionListener(this);
            addMouseListener(this);
            addMouseWheelListener(this);
        }
        private ShipDesign shipDesign()   { 
            ShipDesignLab lab = player().shipLab();
            return selectedSlot < 0 ? lab.prototypeDesign() : lab.design(selectedSlot); 
        }
        private void loadShipImages() {
            if (shipDesign() == null)
                return;
            shipImages.clear();
            shipImageIndex = 0;
            ShipImage images = shipDesign().shipImage();
            for (String key: images.icons()) {
                Image img = icon(key).getImage();
                int w0 = img.getWidth(null);
                int h0 = img.getHeight(null);
                
                float scale = min((float)(shipW-s20)/w0, (float)(shipH-s20)/h0);

                int w1 = (int)(scale*w0);
                int h1 = (int)(scale*h0);
                BufferedImage resizedImg = new BufferedImage(w1,h1, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = resizedImg.createGraphics();
                g.drawImage(img, 0, 0, w1, h1, null);
                g.dispose();
                shipImages.add(resizedImg);
            }
        }
        @Override
        public void animate() {
            repaintShip();
        }
        @Override
        public void drawTexture(Graphics g)     {  }
        @Override
        public String textureName()     { return TEXTURE_BROWN; }
        @Override
        public void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            // draw the gradient background for the header row
            if (configGradient == null) {
                Point2D start = new Point2D.Float(0, 0);
                Point2D end = new Point2D.Float(0, getHeight());
                float[] dist = {0.0f, 1.0f};
                Color[] colors = {darkerBrown, brown};
                configGradient = new LinearGradientPaint(start, end, dist, colors);
            }
            g.setPaint(configGradient);
            g.fillRect(0, 0, getWidth(), getHeight());
            if (UserPreferences.texturesInterface()) 
                drawTexture(g0,0, 0, getWidth(), getHeight());

            ShipDesign des = shipDesign();
            int sect1H = scaled(255);
            int sect2H = scaled(113);
            int sect3H = scaled(143);
            int sect4H = scaled(113);

            // top section: ship image, ship info, engine info
            shipW = scaled(315);
            shipH = sect1H;
            int infoW = scaled(350);
            int engineW = scaled(223);
            drawShipBorder(g, s10, s10, shipW, shipH);
            drawShip(g, s10, s10, shipW, shipH);
            drawSummaryInfo(g, des,s10+shipW, s10, infoW, sect1H);
            drawEngineInfo(g, des,s10+shipW+infoW+s10, s10, engineW, sect1H);

            // 2nd section, left: computers,armor,shields   right:ecm,maneuver
            int y1 = s10+sect1H+s10;
            int compW = (getWidth()-s30)/2;
            drawLeftComponentInfo(g, des,s10, y1, compW, sect2H);
            drawRightComponentInfo(g, des,s10+compW+s10, y1, compW, sect2H);

            //3rd section: weapons
            int y2 = y1+sect2H+s10;
            int boxW2 = getWidth()-s20;
            drawWeaponInfo(g, des,s10, y2, boxW2, sect3H);

            //4th section: specials
            int y3 = y2+sect3H+s10;
            drawSpecialInfo(g, des,s10, y3, boxW2, sect4H);
        }
        private void repaintShip() {
            Graphics g = getGraphics();
            drawShip(g, s10,s10,shipW,shipH);
        }
        private void drawShipBorder(Graphics g0, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g0;
            
            g2.setColor(darkBrown);
            Shape rect = new RoundRectangle2D.Float(x,y,w,h,w/8, h/8);
            g2.setClip(rect);
            g2.fill(rect);  
            
            if (UserPreferences.texturesInterface()) 
                drawTexture(g0, rect, x,y,w,h);       
            g2.setClip(null);
        }
        private void drawShip(Graphics g0, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g0;
            ShipDesign des = shipDesign();
            
            shipImageArea.setBounds(0,0,0,0);
            shipImageDecr.setBounds(0,0,0,0);
            shipImageIncr.setBounds(0,0,0,0);
            shipImageDecr0.reset();
            shipImageIncr0.reset();

            if (des == null)
                return;

            if (shipImageIndex < 0)
                loadShipImages();

            if (shipImages.isEmpty())
                return;

            int imgW = w-s12;
            int imgH = h-s12;
            int imgX = x+s6;
            int imgY = y+s6;
            if (shipPaneImg == null) {
                shipPaneImg = new BufferedImage(imgW,imgH,BufferedImage.TYPE_INT_RGB);
                Graphics2D g = shipPaneImg.createGraphics();
                g.setColor(Color.black);
                g.fillRect(0,0,imgW,imgH);
                drawBackgroundStars(shipPaneImg, this);
                g.dispose();
            }

            BufferedImage paneImg = new BufferedImage(imgW,imgH,BufferedImage.TYPE_INT_RGB);
            Graphics2D g = paneImg.createGraphics();
            g.drawImage(shipPaneImg, 0,0, this);
                
                
            shipImageIndex++;
            if (shipImageIndex >= shipImages.size())
                shipImageIndex = 0;

            BufferedImage img = shipImages.get(shipImageIndex);

            if (des.shipColor() > 0) 
                img = Base.colorizer.makeColor(des.shipColor(), img);
            
            int w1 = img.getWidth();
            int h1 = img.getHeight();

            g.drawImage(img, 0, 0, this);

            if (!shipDesign().active()) {
                int h2 = imgY + imgH - s40;
                shipImageArea.setBounds(imgX, imgY, imgW, imgH);
                shipImageDecr.setBounds(imgX, h2-s40, s80, imgH-h2+s40);
                shipImageIncr.setBounds(imgX + imgW - s80, h2-s40, s80, imgH-h2+s40);
                shipImageDecr0.addPoint(s10, h2);
                shipImageDecr0.addPoint(s30, h2 - s20);
                shipImageDecr0.addPoint(s30, h2 + s20);
                shipImageIncr0.addPoint(imgW - s10, h2);
                shipImageIncr0.addPoint(imgW - s30, h2 - s20);
                shipImageIncr0.addPoint(imgW - s30, h2 + s20);

                Color c0 = (hoverTarget == shipImageDecr) ? yellowShadeC : grayShadeC;
                g.setColor(c0);
                g.fill(shipImageDecr0);
                Color c1 = (hoverTarget == shipImageIncr) ? yellowShadeC : grayShadeC;
                g.setColor(c1);
                g.fill(shipImageIncr0);
            }
            g.dispose();
            Shape rect2 = new RoundRectangle2D.Float(imgX,imgY,imgW,imgH,imgW/8, imgH/8);
            g0.setClip(rect2);
            g0.drawImage(paneImg, imgX, imgY, this);
            g0.setClip(null);
        }
        private void drawSummaryInfo(Graphics2D g, ShipDesign des, int x, int y, int w, int h) {
            String name;
            if (selectedSlot < 0) 
                name = text("SHIP_DESIGN_PROTOTYPE_TITLE");
            else if (des.active())
                name = des.name();
            else
                name = text("SHIP_DESIGN_NEW");
            
            scaledFont(g, name, w-s10, 32, 24);
            int titleW = g.getFontMetrics().stringWidth(name);
            int x0 = x+((w-titleW)/2);
            int y0 = y+s35;
            drawShadowedString(g, name, 3, x0, y0, SystemPanel.textShadowC, SystemPanel.orangeText);
            g.setColor(darkBrown);
            g.fillRect(x, y0+s10, w, h-s85);

            int x1 = x+s10;
            int x2 = x+(w*55/100);
            int rowH=(h-s85-s25)/7;

            g.setFont(narrowFont(17));
            g.setColor(Color.black);
            int y1 = y0+rowH+s15;
            drawString(g,text("SHIP_DESIGN_SIZE_LABEL"), x1, y1);
            int y2 = y1+rowH+s10;
            drawString(g,text("SHIP_DESIGN_RANGE_LABEL"), x1, y2);
            int y3 = y2+rowH;
            drawString(g,text("SHIP_DESIGN_SPEED_LABEL"), x1, y3);
            int y4 = y3+rowH;
            drawString(g,text("SHIP_DESIGN_COST_LABEL"), x1, y4);
            int y5 = y4+rowH;
            drawString(g,text("SHIP_DESIGN_TOTAL_SPACE_LABEL"), x1, y5);
            int y6 = y5+rowH;
            if (des.availableSpace() < 0)
                g.setColor(errorRedC);
            drawString(g,text("SHIP_DESIGN_AVAIL_SPACE_LABEL"), x1, y6);
            
            if (!des.active())
                drawColorOptions(g, x1, y6, w-s20, rowH);
            
            g.setFont(narrowFont(22));
            String title = text("SHIP_DESIGN_COMBAT_STATS_TITLE");
            this.scaledFont(g, title, (w*45/100)-s10, 22, 16);
            drawShadowedString(g, title,3,x2+s5,y1,SystemPanel.textShadowC, SystemPanel.whiteText);

            if (UserPreferences.texturesInterface()) 
                drawTexture(g,x, y0+s10, w, h-s85);

            // draw left side values
            int boxW=s90;
            String sizeStr = des.sizeDesc();
            g.setColor(Color.black);
            int boxX = x2-s20-boxW;
            int boxY = y1-s15;
            int boxH = s20;
            if (shipDesign().active()) {
                g.fillRoundRect(boxX, boxY, boxW, boxH, s10, s10);
                g.setColor(SystemPanel.whiteText);
            }
            else {
                sizeFieldArea.setBounds(boxX, boxY, boxW,boxH);
                sizeFieldDecr.reset();
                if (des.size() != ShipDesign.SMALL) {
                    sizeFieldDecr.addPoint(boxX-s11, boxY+(boxH/2));
                    sizeFieldDecr.addPoint(boxX-s3, boxY);
                    sizeFieldDecr.addPoint(boxX-s3, boxY+boxH);
                }
                sizeFieldIncr.reset();
                if (des.size() != ShipDesign.HUGE) {
                    sizeFieldIncr.addPoint(boxX+boxW+s11, boxY+(boxH/2));
                    sizeFieldIncr.addPoint(boxX+boxW+s3, boxY);
                    sizeFieldIncr.addPoint(boxX+boxW+s3, boxY+boxH);
                }
                g.fill(sizeFieldArea);
                g.fill(sizeFieldDecr);
                g.fill(sizeFieldIncr);
                g.setColor(SystemPanel.yellowText);
                Stroke prevStr = g.getStroke();
                g.setStroke(BasePanel.stroke2);
                if (hoverTarget == sizeFieldArea)
                    g.draw(sizeFieldArea);
                else if (hoverTarget == sizeFieldDecr)
                    g.draw(sizeFieldDecr);
                else if (hoverTarget == sizeFieldIncr)
                    g.draw(sizeFieldIncr);
                g.setStroke(prevStr);
                g.setColor(SystemPanel.blueText);
            }
            g.setFont(narrowFont(15));
            int sw = g.getFontMetrics().stringWidth(sizeStr);
            int x1a = x2-s20-boxW+((boxW-sw)/2);
            drawString(g,sizeStr, x1a, y1);

            g.setColor(darkestBrown);
            g.setFont(narrowFont(17));
           
            String str = player().tech().topFuelRangeTech().unlimited ? text("SHIP_DESIGN_RANGE_UNLIMITED") : text("SHIP_DESIGN_RANGE_VALUE", (int)des.range());
            sw = g.getFontMetrics().stringWidth(str);
            drawString(g,str, x2-s20-sw, y2);
            str = text("SHIP_DESIGN_SPEED_VALUE", (int)des.warpSpeed());
            sw = g.getFontMetrics().stringWidth(str);
            drawString(g,str, x2-s20-sw, y3);
            des.recalculateCost();
            str = text("SHIP_DESIGN_COST_VALUE", (int)des.cost());
            sw = g.getFontMetrics().stringWidth(str);
            drawString(g,str, x2-s20-sw, y4);
            str = ""+ (int)des.spaceUsed();
            sw = g.getFontMetrics().stringWidth(str);
            drawString(g,str, x2-s20-sw, y5);
            str = "" + (int)Math.floor(des.availableSpace());
            sw = g.getFontMetrics().stringWidth(str);
            if (des.availableSpace() < 0)
                g.setColor(errorRedC);
            drawString(g,str, x2-s20-sw, y6);

            // right side
            g.setColor(Color.black);
            g.setFont(narrowFont(17));
            drawString(g,text("SHIP_DESIGN_HIT_POINTS_LABEL"), x2+s10, y2);
            drawString(g,text("SHIP_DESIGN_MISSILE_DEF_LABEL"), x2+s10, y3);
            drawString(g,text("SHIP_DESIGN_BEAM_DEF_LABEL"), x2+s10, y4);
            drawString(g,text("SHIP_DESIGN_ATTACK_LEVEL_LABEL"), x2+s10, y5);
            drawString(g,text("SHIP_DESIGN_COMBAT_SPEED_LABEL"), x2+s10, y6);

            // draw right side values
            int x3 = x+w-s20;
            g.setColor(darkestBrown);
            g.setFont(narrowFont(17));
            str = ""+(int)des.hits();
            sw = g.getFontMetrics().stringWidth(str);
            drawString(g,str, x3-sw, y2);
            str = ""+des.missileDefense();
            sw = g.getFontMetrics().stringWidth(str);
            drawString(g,str, x3-sw, y3);
            str = ""+des.beamDefense();
            sw = g.getFontMetrics().stringWidth(str);
            drawString(g,str, x3-sw, y4);
            str = ""+ (int)des.attackLevel();
            sw = g.getFontMetrics().stringWidth(str);
            drawString(g,str, x3-sw, y5);
            str = ""+ des.combatSpeed();
            sw = g.getFontMetrics().stringWidth(str);
            drawString(g,str, x3-sw, y6);

            if (selectedSlot < 0) {
                renameButtonArea.setBounds(0,0,0,0);
                scrapButtonArea.setBounds(0,0,0,0);
                // draw clear button
                g.setFont(narrowFont(18));
                str = text("SHIP_DESIGN_CLEAR_BUTTON");
                sw = g.getFontMetrics().stringWidth(str);
                int buttonW = sw + s40;
                int buttonH = s25;
                int buttonX = x + s10;
                int buttonY = y + h - s30;
                clearButtonArea.setBounds(buttonX, buttonY, buttonW, buttonH);

                if (clearBackground == null) {
                    float[] dist = {0.0f, 0.5f, 1.0f};
                    Point2D ptStart = new Point2D.Float(buttonX, 0);
                    Point2D ptEnd = new Point2D.Float(buttonX + buttonW, 0);
                    Color[] yesColors = {brownEdgeC, brownMidC, brownEdgeC};
                    clearBackground = new LinearGradientPaint(ptStart, ptEnd, dist, yesColors);
                }

                boolean hovering = hoverTarget == clearButtonArea;
                g.setPaint(clearBackground);
                g.fillRoundRect(buttonX, buttonY, buttonW, buttonH, s3, s3);
                Color c0 = hovering ? SystemPanel.yellowText : SystemPanel.whiteText;
                g.setColor(c0);
                Stroke prevStr = g.getStroke();
                g.setStroke(BasePanel.stroke1);
                g.drawRoundRect(buttonX, buttonY, buttonW, buttonH, s3, s3);
                g.setStroke(prevStr);
                int x2a = buttonX + ((buttonW - sw) / 2);
                drawBorderedString(g, str, x2a, buttonY + buttonH - s7, SystemPanel.textShadowC, c0);

                int firstAvailable = -1;
                for (int i=0;i<ShipDesignLab.MAX_DESIGNS;i++) {
                    ShipDesign des1 = player().shipLab().design(i);
                    if ((firstAvailable < 0) && !des1.active())
                       firstAvailable = i;     
                }
                
                if (firstAvailable < 0) {
                    createButtonArea.setBounds(0,0,0,0);
                    g.setFont(narrowFont(15));
                    g.setColor(SystemPanel.whiteText);
                    str = text("SHIP_DESIGN_PROTOTYPE_DESC");
                    int labelW = g.getFontMetrics().stringWidth(str);
                    int maxLabelW = min(labelW, scaled(200));
                    List<String> lines = wrappedLines(g, str, maxLabelW);
                    int lineX = x+w-maxLabelW;
                    int lineY = lines.size() == 1 ? y+h-s15 : y+h-s25;
                    for (String line: lines) {
                        drawString(g,line, lineX, lineY);
                        lineY += s16;
                    }
                }
                else {
                    // draw deploy button
                    g.setFont(narrowFont(18));
                    str = text("SHIP_DESIGN_DEPLOY_BUTTON");
                    sw = g.getFontMetrics().stringWidth(str);
                    buttonW = sw + s40;
                    buttonH = s25;
                    buttonX = x + w - buttonW;
                    buttonY = y + h - s30;
                    createButtonArea.setBounds(buttonX, buttonY, buttonW, buttonH);

                    // always create background for create button since config changes to change button color
                    float[] dist = {0.0f, 0.5f, 1.0f};
                    Point2D ptStart = new Point2D.Float(buttonX, 0);
                    Point2D ptEnd = new Point2D.Float(buttonX + buttonW, 0);
                    Color[] yesColors = {greenEdgeC, greenMidC, greenEdgeC};
                    Color[] grayColors = {grayEdgeC, grayMidC, grayEdgeC};
                    if (shipDesign().validConfiguration())
                        createBackground = new LinearGradientPaint(ptStart, ptEnd, dist, yesColors);
                    else
                        createBackground = new LinearGradientPaint(ptStart, ptEnd, dist, grayColors);

                    hovering = hoverTarget == createButtonArea;
                    g.setPaint(createBackground);
                    g.fillRoundRect(buttonX, buttonY, buttonW, buttonH, s3, s3);
                    c0 = des.validConfiguration() ? (hovering ? SystemPanel.yellowText : SystemPanel.whiteText) : SystemPanel.grayText;
                    g.setColor(c0);
                    prevStr = g.getStroke();
                    g.setStroke(BasePanel.stroke1);
                    g.drawRoundRect(buttonX, buttonY, buttonW, buttonH, s3, s3);
                    g.setStroke(prevStr);
                    x2a = buttonX + ((buttonW - sw) / 2);
                    drawBorderedString(g, str, x2a, buttonY + buttonH - s7, SystemPanel.textShadowC, c0);
                }
            }
            else if (des.active()) {
                clearButtonArea.setBounds(0,0,0,0);
                createButtonArea.setBounds(0,0,0,0);
                // draw rename button
                g.setFont(narrowFont(18));
                str = text("SHIP_DESIGN_RENAME_BUTTON");
                sw = g.getFontMetrics().stringWidth(str);
                int buttonW = sw + s40;
                int buttonH = s25;
                int buttonX = x + s10;
                int buttonY = y + h - s30;
                renameButtonArea.setBounds(buttonX, buttonY, buttonW, buttonH);

                if (renameBackground == null) {
                    float[] dist = {0.0f, 0.5f, 1.0f};
                    Point2D ptStart = new Point2D.Float(buttonX, 0);
                    Point2D ptEnd = new Point2D.Float(buttonX + buttonW, 0);
                    Color[] yesColors = {brownEdgeC, brownMidC, brownEdgeC};
                    renameBackground = new LinearGradientPaint(ptStart, ptEnd, dist, yesColors);
                }

                boolean hovering = hoverTarget == renameButtonArea;
                g.setPaint(renameBackground);
                g.fillRoundRect(buttonX, buttonY, buttonW, buttonH, s3, s3);
                Color c0 = hovering ? SystemPanel.yellowText : SystemPanel.whiteText;
                g.setColor(c0);
                Stroke prevStr = g.getStroke();
                g.setStroke(BasePanel.stroke1);
                g.drawRoundRect(buttonX, buttonY, buttonW, buttonH, s3, s3);
                g.setStroke(prevStr);
                int x2a = buttonX + ((buttonW - sw) / 2);
                drawBorderedString(g, str, x2a, buttonY + buttonH - s7, SystemPanel.textShadowC, c0);

                // draw scrap button
                scrapButtonArea.setBounds(0,0,0,0);
                if (player().shipLab().canScrapADesign()) {
                    g.setFont(narrowFont(18));
                    str = text("SHIP_DESIGN_SCRAP_BUTTON");
                    sw = g.getFontMetrics().stringWidth(str);
                    buttonW = sw + s40;
                    buttonH = s25;
                    buttonX = x + w - buttonW;
                    buttonY = y + h - s30;
                    scrapButtonArea.setBounds(buttonX, buttonY, buttonW, buttonH);

                    if (scrapBackground == null) {
                        float[] dist = {0.0f, 0.5f, 1.0f};
                        Point2D ptStart = new Point2D.Float(buttonX, 0);
                        Point2D ptEnd = new Point2D.Float(buttonX + buttonW, 0);
                        Color[] yesColors = {redEdgeC, redMidC, redEdgeC};
                        scrapBackground = new LinearGradientPaint(ptStart, ptEnd, dist, yesColors);
                    }

                    hovering = hoverTarget == scrapButtonArea;
                    g.setPaint(scrapBackground);
                    g.fillRoundRect(buttonX, buttonY, buttonW, buttonH, s3, s3);
                    c0 = hovering ? SystemPanel.yellowText : SystemPanel.whiteText;
                    g.setColor(c0);
                    prevStr = g.getStroke();
                    g.setStroke(BasePanel.stroke1);
                    g.drawRoundRect(buttonX, buttonY, buttonW, buttonH, s3, s3);
                    g.setStroke(prevStr);
                    x2a = buttonX + ((buttonW - sw) / 2);
                    drawBorderedString(g, str, x2a, buttonY + buttonH - s7, SystemPanel.textShadowC, c0);
                }
            }
            else {
                renameButtonArea.setBounds(0,0,0,0);
                scrapButtonArea.setBounds(0,0,0,0);
                // draw clear button
                g.setFont(narrowFont(18));
                str = text("SHIP_DESIGN_CLEAR_BUTTON");
                sw = g.getFontMetrics().stringWidth(str);
                int buttonW = sw + s40;
                int buttonH = s25;
                int buttonX = x + s10;
                int buttonY = y + h - s30;
                clearButtonArea.setBounds(buttonX, buttonY, buttonW, buttonH);

                if (clearBackground == null) {
                    float[] dist = {0.0f, 0.5f, 1.0f};
                    Point2D ptStart = new Point2D.Float(buttonX, 0);
                    Point2D ptEnd = new Point2D.Float(buttonX + buttonW, 0);
                    Color[] yesColors = {brownEdgeC, brownMidC, brownEdgeC};
                    clearBackground = new LinearGradientPaint(ptStart, ptEnd, dist, yesColors);
                }

                boolean hovering = hoverTarget == clearButtonArea;
                g.setPaint(clearBackground);
                g.fillRoundRect(buttonX, buttonY, buttonW, buttonH, s3, s3);
                Color c0 = hovering ? SystemPanel.yellowText : SystemPanel.whiteText;
                g.setColor(c0);
                Stroke prevStr = g.getStroke();
                g.setStroke(BasePanel.stroke1);
                g.drawRoundRect(buttonX, buttonY, buttonW, buttonH, s3, s3);
                g.setStroke(prevStr);
                int x2a = buttonX + ((buttonW - sw) / 2);
                drawBorderedString(g, str, x2a, buttonY + buttonH - s7, SystemPanel.textShadowC, c0);

                // draw deploy button
                g.setFont(narrowFont(18));
                str = text("SHIP_DESIGN_DEPLOY_BUTTON");
                sw = g.getFontMetrics().stringWidth(str);
                buttonW = sw + s40;
                buttonH = s25;
                buttonX = x + w - buttonW;
                buttonY = y + h - s30;
                createButtonArea.setBounds(buttonX, buttonY, buttonW, buttonH);

                // always create background for create button since config changes to change button color
                float[] dist = {0.0f, 0.5f, 1.0f};
                Point2D ptStart = new Point2D.Float(buttonX, 0);
                Point2D ptEnd = new Point2D.Float(buttonX + buttonW, 0);
                Color[] yesColors = {greenEdgeC, greenMidC, greenEdgeC};
                Color[] grayColors = {grayEdgeC, grayMidC, grayEdgeC};
                if (shipDesign().validConfiguration())
                    createBackground = new LinearGradientPaint(ptStart, ptEnd, dist, yesColors);
                else
                    createBackground = new LinearGradientPaint(ptStart, ptEnd, dist, grayColors);

                hovering = hoverTarget == createButtonArea;
                g.setPaint(createBackground);
                g.fillRoundRect(buttonX, buttonY, buttonW, buttonH, s3, s3);
                c0 = des.validConfiguration() ? (hovering ? SystemPanel.yellowText : SystemPanel.whiteText) : SystemPanel.grayText;
                g.setColor(c0);
                prevStr = g.getStroke();
                g.setStroke(BasePanel.stroke1);
                g.drawRoundRect(buttonX, buttonY, buttonW, buttonH, s3, s3);
                g.setStroke(prevStr);
                x2a = buttonX + ((buttonW - sw) / 2);
                drawBorderedString(g, str, x2a, buttonY + buttonH - s7, SystemPanel.textShadowC, c0);
            }
        }
        private void drawEngineInfo(Graphics2D g, ShipDesign des, int x, int y, int w, int h) {
            g.setColor(darkBrown);
            g.fillRect(x, y, w, h);

            g.setFont(narrowFont(22));
            drawShadowedString(g, text("SHIP_DESIGN_ENGINES_TITLE"), 3, x + s10, y + s25, SystemPanel.textShadowC, SystemPanel.whiteText);
            g.setColor(Color.black);
            g.setFont(narrowFont(12));
            String desc = text("SHIP_DESIGN_ENGINES_DESC");
            List<String> lines = wrappedLines(g, desc, w - s30);
            int y0 = y + s33;
            int x0 = x + s20;
            for (String line : lines) {
                y0 += s12;
                drawString(g,line, x0, y0);
            }
            int scrunch = lines.size() > 1 ? ((lines.size() - 1) * s12) / 4 : 0;
            g.setColor(Color.black);
            String typeLabel = text("SHIP_DESIGN_ENGINE_TYPE");
            scaledFont(g, typeLabel, w-scaled(140), 16, 12);
            int y1 = y0 + s26;
            drawString(g,typeLabel, x0, y1);
            int y2 = y1 + s33 - scrunch;
            g.setFont(narrowFont(16));
            drawString(g,text("SHIP_DESIGN_ENGINE_SPEED"), x0, y2);
            int y3 = y2 + s17;
            drawString(g,text("SHIP_DESIGN_ENGINE_COST1"), x0, y3);
            int y4 = y3 + s17;
            drawString(g,text("SHIP_DESIGN_ENGINE_SIZE1"), x0, y4);
            int y5 = y4 + s17;
            drawString(g,text("SHIP_DESIGN_ENGINE_POWER1"), x0, y5);
            int y6 = y5 + s27 - scrunch;
            drawString(g,text("SHIP_DESIGN_POWER_REQUIREMENTS"), x0, y6);
            int y7 = y6 + s17;
            drawString(g,text("SHIP_DESIGN_ENGINES_REQUIRED"), x0, y7);
            int y8 = y7 + s27 - scrunch;
            drawString(g,text("SHIP_DESIGN_ENGINES_SIZE"), x0, y8);
            int y9 = y8 + s17;
            drawString(g,text("SHIP_DESIGN_ENGINES_COST"), x0, y9);

           if (UserPreferences.texturesInterface()) 
                drawTexture(g,x, y,w,h);

            // draw right side values
            int x3 = x + w - s20;
            int boxW = s100;
            int boxX = x3 - boxW;
            int boxY = y1 - s15;
            int boxH = s20;
            if (shipDesign().active()) {
                g.fillRoundRect(boxX, boxY, boxW, boxH, s10, s10);
                g.setColor(SystemPanel.whiteText);
            }
            else {
                List<ShipEngine> comps = player().shipLab().engines();
                ShipEngine first = comps.get(0);
                ShipEngine last = comps.get(comps.size()-1);
                engineFieldArea.setBounds(boxX, boxY, boxW, boxH);
                engineFieldDecr.reset();
                if (des.engine() != first) {
                    engineFieldDecr.addPoint(boxX - s11, boxY + (boxH / 2));
                    engineFieldDecr.addPoint(boxX - s3, boxY);
                    engineFieldDecr.addPoint(boxX - s3, boxY + boxH);
                }
                engineFieldIncr.reset();
                if (des.engine() != last) {
                    engineFieldIncr.addPoint(boxX + boxW + s11, boxY + (boxH / 2));
                    engineFieldIncr.addPoint(boxX + boxW + s3, boxY);
                    engineFieldIncr.addPoint(boxX + boxW + s3, boxY + boxH);
                }
                g.fill(engineFieldArea);
                g.fill(engineFieldDecr);
                g.fill(engineFieldIncr);
                g.setColor(SystemPanel.yellowText);
                Stroke prevStr = g.getStroke();
                g.setStroke(BasePanel.stroke2);
                if (hoverTarget == engineFieldArea)
                    g.draw(engineFieldArea);
                else if (hoverTarget == engineFieldDecr)
                    g.draw(engineFieldDecr);
                else if (hoverTarget == engineFieldIncr)
                    g.draw(engineFieldIncr);
                g.setStroke(prevStr);
                g.setColor(SystemPanel.blueText);
            }

            String typeStr = des.engine().name();
            g.setFont(narrowFont(15));
            int sw = g.getFontMetrics().stringWidth(typeStr);
            int x1a = x3-boxW+((boxW-sw)/2);
            drawString(g,typeStr, x1a, y1);

            float engRequired = des.enginesRequired();
            float engSize = des.engine().size(des);
            float engPower = des.engine().powerOutput();
            float engCost =des.engine().cost(des);

            g.setColor(darkestBrown);
            g.setFont(narrowFont(16));
            String str = text("SHIP_DESIGN_SPEED_VALUE", (int)des.warpSpeed());
            sw = g.getFontMetrics().stringWidth(str);
            drawString(g,str, x3-sw, y2);
            str = ""+fmt(engCost,1);
            sw = g.getFontMetrics().stringWidth(str);
            drawString(g,str, x3-sw, y3);
            str = ""+fmt(engSize,1);
            sw = g.getFontMetrics().stringWidth(str);
            drawString(g,str, x3-sw, y4);
            str = ""+fmt(engPower,1);
            sw = g.getFontMetrics().stringWidth(str);
            drawString(g,str, x3-sw, y5);
            str = ""+fmt(engRequired*engPower,1);
            sw = g.getFontMetrics().stringWidth(str);
            drawString(g,str, x3-sw, y6);
            str = ""+ fmt(engRequired,1);
            sw = g.getFontMetrics().stringWidth(str);
            drawString(g,str, x3-sw, y7);
            str = ""+ (int) (engRequired*engSize);
            sw = g.getFontMetrics().stringWidth(str);
            drawString(g,str, x3-sw, y8);
            str = ""+ (int) (engRequired*engCost);
            sw = g.getFontMetrics().stringWidth(str);
            drawString(g,str, x3-sw, y9);
        }
        private void drawColorOptions(Graphics2D g, int x, int y, int w, int h) {
            ShipDesign des = shipDesign();
            g.setColor(Color.black);
            String s = text("SHIP_DESIGN_COLORS");
            int sw = g.getFontMetrics().stringWidth(s);
            drawString(g,s, x, y+h);
            int w0 = w-sw-s20;
            int boxW = w0/ShipDesign.shipColors.length;
            int boxH = h-s10;
            int boxY = y+s10;
            int boxX = x+sw+s20;
            g.setColor(Color.white);
            Stroke prev = g.getStroke();
            
            for (int i=0;i<ShipDesign.shipColors.length;i++) {
                g.setStroke(stroke1);
                int cIndex = ShipDesign.shipColors[i];
                shipColorArea[i].setBounds(boxX, boxY, boxW-s5, boxH);
                if (cIndex > 0) {
                    g.setColor(ImageColorizer.color(cIndex));             
                    g.fill(shipColorArea[i]);
                }
                if (hoverTarget == shipColorArea[i]) 
                    g.setColor(Color.yellow);
                else if (des.shipColor() == cIndex) {
                    g.setStroke(stroke2);
                    g.setColor(Color.white);
                }
                else
                   g.setColor(Color.black);
                g.draw(shipColorArea[i]);
                boxX += boxW;
            }
            g.setStroke(prev);
        }
        private void drawLeftComponentInfo(Graphics2D g, ShipDesign des, int x, int y, int w0, int h) {
            g.setColor(darkBrown);
            g.fillRect(x, y+s25, w0, h-s25);
 
            // set up column starts and widths
            int w = w0-s20;
            int y1 = y+s20;
            int x1 = x+s10; int w1 = w*30/100;
            int x2 = x1+w1; int w2 = w*20/100;
            int x3 = x2+w2; int w3 = w*24/100;
            int x4 = x3+w3; int w4 = w*8/100;
            int x5 = x4+w4; int w5 = w*10/100;
            int x6 = x5+w5; int w6 = w*8/100;

            // draw headers
            g.setColor(Color.black);
            g.setFont(narrowFont(16));
            String str = text("SHIP_DESIGN_DESCRIPTION_LABEL");
            //int sw2 = g.getFontMetrics().stringWidth(s2);
            //drawString(g,s2, x2+(w2-sw2)/2, y1);
            drawString(g,str, x2, y1);
            str = text("SHIP_DESIGN_TYPE_LABEL");
            int sw = g.getFontMetrics().stringWidth(str);
            drawString(g,str, x3+(w3-sw)/2, y1);
            str = text("SHIP_DESIGN_SIZE_LABEL");
            sw = g.getFontMetrics().stringWidth(str);
            drawString(g,str, x4+w4-sw, y1);
            str = text("SHIP_DESIGN_POWER_LABEL");
            sw = g.getFontMetrics().stringWidth(str);
            drawString(g,str, x5+w5-sw, y1);
            str = text("SHIP_DESIGN_COST_LABEL");
            scaledFont(g, str, w6-s5, 16, 12);
            sw = g.getFontMetrics().stringWidth(str);
            drawString(g,str, x6+w6-sw, y1);

            // draw ship computer row
            int rowH = (y+h-s10-y1)/3;
            int y2 = y1+rowH;
            String title1 = text("SHIP_DESIGN_COMPUTER_TITLE");
            g.setFont(narrowFont(20));
            drawShadowedString(g, title1, 3, x1, y2, SystemPanel.textShadowC, SystemPanel.whiteText);

           // draw ship armor row
            int y3 = y2+rowH;
            String title2 = text("SHIP_DESIGN_ARMOR_TITLE");
            g.setFont(narrowFont(20));
            drawShadowedString(g, title2, 3, x1, y3, SystemPanel.textShadowC, SystemPanel.whiteText);

            // draw ship shields row
            int y4 = y3+rowH;
            String title3 = text("SHIP_DESIGN_SHIELD_TITLE");
            g.setFont(narrowFont(20));
            drawShadowedString(g, title3, 3, x1, y4, SystemPanel.textShadowC, SystemPanel.whiteText);

            if (UserPreferences.texturesInterface()) 
                drawTexture(g,x, y+s25, w0, h-s25);

            // computer field
            ShipComputer comp = des.computer();
            String compDesc = comp.desc(des);
            String compName = comp.name().isEmpty() ? text("SHIP_DESIGN_COMPONENT_NONE") : comp.name();
            String compSize = fmt(comp.size(des), 1);
            String compPower = fmt(comp.power(des), 1);
            String compCost = fmt(comp.cost(des), 1);
            g.setFont(narrowFont(15));
            g.setColor(darkestBrown);
            List<String> descLines = wrappedLines(g, compDesc, w2);
            if (descLines.size() == 1) 
                drawString(g,descLines.get(0), x2, y2);
            else if (descLines.size() > 1) {
                drawString(g,descLines.get(0), x2, y2-s5);
                drawString(g,descLines.get(1), x2, y2+s5);
            }
            g.setColor(Color.black);
            int boxW = w3-s20;
            int boxX = x3+s10;
            int boxY = y2-s15;
            int boxH = s20;
            if (shipDesign().active()) {
                g.fillRoundRect(boxX, boxY, boxW, boxH, s10, s10);
                g.setColor(SystemPanel.whiteText);
            }
            else {
                List<ShipComputer> comps = player().shipLab().computers();
                ShipComputer first = comps.get(0);
                ShipComputer last = comps.get(comps.size()-1);
                computerFieldArea.setBounds(boxX, boxY, boxW, boxH);
                computerFieldDecr.reset();
                if (comp != first) {
                    computerFieldDecr.addPoint(boxX - s11, boxY + (boxH / 2));
                    computerFieldDecr.addPoint(boxX - s3, boxY);
                    computerFieldDecr.addPoint(boxX - s3, boxY + boxH);
                }
                computerFieldIncr.reset();
                if (comp != last) {
                    computerFieldIncr.addPoint(boxX + boxW + s11, boxY + (boxH / 2));
                    computerFieldIncr.addPoint(boxX + boxW + s3, boxY);
                    computerFieldIncr.addPoint(boxX + boxW + s3, boxY + boxH);
                }
                g.fill(computerFieldArea);
                g.fill(computerFieldDecr);
                g.fill(computerFieldIncr);
                g.setColor(SystemPanel.yellowText);
                Stroke prevStr = g.getStroke();
                g.setStroke(BasePanel.stroke2);
                if (hoverTarget == computerFieldArea)
                    g.draw(computerFieldArea);
                else if (hoverTarget == computerFieldDecr)
                    g.draw(computerFieldDecr);
                else if (hoverTarget == computerFieldIncr)
                    g.draw(computerFieldIncr);
                g.setStroke(prevStr);
                g.setColor(SystemPanel.blueText);
            }

            g.setFont(narrowFont(15));
            sw = g.getFontMetrics().stringWidth(compName);
            int x3a = x3+s10+((boxW-sw)/2);
            drawString(g,compName, x3a, y2);

            g.setFont(narrowFont(17));
            g.setColor(darkestBrown);
            sw = g.getFontMetrics().stringWidth(compSize);
            drawString(g,compSize, x4+w4-sw, y2);

            sw = g.getFontMetrics().stringWidth(compPower);
            drawString(g,compPower, x5+w5-sw, y2);

            sw = g.getFontMetrics().stringWidth(compCost);
            drawString(g,compCost, x6+w6-sw, y2);

            // armor field
            ShipArmor armor = des.armor();
            String armorDesc = armor.desc(des);
            String armorName = armor.name();
            String armorSize = fmt(armor.size(des), 1);
            String armorPower = fmt(armor.power(des), 1);
            String armorCost = fmt(armor.cost(des), 1);
            g.setFont(narrowFont(15));
            g.setColor(darkestBrown);
            descLines = wrappedLines(g, armorDesc, w2);
            if (descLines.size() == 1) {
                drawString(g,descLines.get(0), x2, y3);
            }
            else if (descLines.size() > 1) {
                drawString(g,descLines.get(0), x2, y3-s5);
                drawString(g,descLines.get(1), x2, y3+s5);
            }
            g.setColor(Color.black);
            boxW = w3-s20;
            boxX = x3+s10;
            boxY = y3-s15;
            boxH = s20;
            if (shipDesign().active()) {
                g.fillRoundRect(boxX, boxY, boxW, boxH, s10, s10);
                g.setColor(SystemPanel.whiteText);
            }
            else {
                List<ShipArmor> comps = player().shipLab().armors();
                ShipArmor first = comps.get(0);
                ShipArmor last = comps.get(comps.size()-1);
                armorFieldArea.setBounds(boxX, boxY, boxW, boxH);
                armorFieldDecr.reset();
                if (armor != first) {
                    armorFieldDecr.addPoint(boxX - s11, boxY + (boxH / 2));
                    armorFieldDecr.addPoint(boxX - s3, boxY);
                    armorFieldDecr.addPoint(boxX - s3, boxY + boxH);
                }
                armorFieldIncr.reset();
                if (armor != last) {
                    armorFieldIncr.addPoint(boxX + boxW + s11, boxY + (boxH / 2));
                    armorFieldIncr.addPoint(boxX + boxW + s3, boxY);
                    armorFieldIncr.addPoint(boxX + boxW + s3, boxY + boxH);
                }
                g.fill(armorFieldArea);
                g.fill(armorFieldDecr);
                g.fill(armorFieldIncr);
                g.setColor(SystemPanel.yellowText);
                Stroke prevStr = g.getStroke();
                g.setStroke(BasePanel.stroke2);
                if (hoverTarget == armorFieldArea)
                    g.draw(armorFieldArea);
                else if (hoverTarget == armorFieldDecr)
                    g.draw(armorFieldDecr);
                else if (hoverTarget == armorFieldIncr)
                    g.draw(armorFieldIncr);
                g.setStroke(prevStr);
                g.setColor(SystemPanel.blueText);
            }
            g.setFont(narrowFont(15));
            sw = g.getFontMetrics().stringWidth(armorName);
            x3a = x3+s10+((boxW-sw)/2);
            drawString(g,armorName, x3a, y3);

            g.setFont(narrowFont(17));
            g.setColor(darkestBrown);
            sw = g.getFontMetrics().stringWidth(armorSize);
            drawString(g,armorSize, x4+w4-sw, y3);

            sw = g.getFontMetrics().stringWidth(armorPower);
            drawString(g,armorPower, x5+w5-sw, y3);

            sw = g.getFontMetrics().stringWidth(armorCost);
            drawString(g,armorCost, x6+w6-sw, y3);
         
            // shield field
            ShipShield shield = des.shield();
            String shieldDesc = shield.desc(des);
            String shieldName = shield.name().isEmpty() ? text("SHIP_DESIGN_COMPONENT_NONE") : shield.name();
            String shieldSize = fmt(shield.size(des), 1);
            String shieldPower = fmt(shield.power(des), 1);
            String shieldCost = fmt(shield.cost(des), 1);
            g.setFont(narrowFont(15));
            g.setColor(darkestBrown);
            descLines = wrappedLines(g, shieldDesc, w2);

            if (descLines.size() == 1) 
                drawString(g,descLines.get(0), x2, y4);
            else if (descLines.size() > 1) {
                drawString(g,descLines.get(0), x2, y4-s5);
                drawString(g,descLines.get(1), x2, y4+s5);
            }
            g.setColor(Color.black);
            boxW = w3-s20;
            boxX = x3+s10;
            boxY = y4-s15;
            boxH = s20;
            if (shipDesign().active()) {
                g.fillRoundRect(boxX, boxY, boxW, boxH, s10, s10);
                g.setColor(SystemPanel.whiteText);
            }
            else {
                List<ShipShield> comps = player().shipLab().shields();
                ShipShield first = comps.get(0);
                ShipShield last = comps.get(comps.size()-1);
                shieldsFieldArea.setBounds(boxX, boxY, boxW, boxH);
                shieldsFieldDecr.reset();
                if (shield != first) {
                    shieldsFieldDecr.addPoint(boxX - s11, boxY + (boxH / 2));
                    shieldsFieldDecr.addPoint(boxX - s3, boxY);
                    shieldsFieldDecr.addPoint(boxX - s3, boxY + boxH);
                }
                shieldsFieldIncr.reset();
                if (shield != last) {
                    shieldsFieldIncr.addPoint(boxX + boxW + s11, boxY + (boxH / 2));
                    shieldsFieldIncr.addPoint(boxX + boxW + s3, boxY);
                    shieldsFieldIncr.addPoint(boxX + boxW + s3, boxY + boxH);
                }
                g.fill(shieldsFieldArea);
                g.fill(shieldsFieldDecr);
                g.fill(shieldsFieldIncr);
                g.setColor(SystemPanel.yellowText);
                Stroke prevStr = g.getStroke();
                g.setStroke(BasePanel.stroke2);
                if (hoverTarget == shieldsFieldArea)
                    g.draw(shieldsFieldArea);
                else if (hoverTarget == shieldsFieldDecr)
                    g.draw(shieldsFieldDecr);
                else if (hoverTarget == shieldsFieldIncr)
                    g.draw(shieldsFieldIncr);
                g.setStroke(prevStr);
                g.setColor(SystemPanel.blueText);
            }
            g.setFont(narrowFont(15));
            sw = g.getFontMetrics().stringWidth(shieldName);
            x3a = x3+s10+((boxW-sw)/2);
            drawString(g,shieldName, x3a, y4);

            g.setFont(narrowFont(17));
            g.setColor(darkestBrown);
            sw = g.getFontMetrics().stringWidth(shieldSize);
            drawString(g,shieldSize, x4+w4-sw, y4);

            sw = g.getFontMetrics().stringWidth(shieldPower);
            drawString(g,shieldPower, x5+w5-sw, y4);

            sw = g.getFontMetrics().stringWidth(shieldCost);
            drawString(g,shieldCost, x6+w6-sw, y4);
        }
        private void drawRightComponentInfo(Graphics2D g, ShipDesign des, int x, int y, int w0, int h) {
            g.setColor(darkBrown);
            g.fillRect(x, y+s25, w0, h-s25);

            // set up column starts and widths
            int w = w0-s20;
            int y1 = y+s20;
            int x1 = x+s10; int w1 = w*30/100;
            int x2 = x1+w1; int w2 = w*20/100;
            int x3 = x2+w2; int w3 = w*24/100;
            int x4 = x3+w3; int w4 = w*8/100;
            int x5 = x4+w4; int w5 = w*10/100;
            int x6 = x5+w5; int w6 = w*8/100;

            // draw headers
            g.setColor(Color.black);
            g.setFont(narrowFont(16));
            String str = text("SHIP_DESIGN_DESCRIPTION_LABEL");
            //int sw2 = g.getFontMetrics().stringWidth(s2);
            //drawString(g,s2, x2+(w2-sw2)/2, y1);
            drawString(g,str, x2, y1);
            str = text("SHIP_DESIGN_TYPE_LABEL");
            int sw = g.getFontMetrics().stringWidth(str);
            drawString(g,str, x3+(w3-sw)/2, y1);
            str = text("SHIP_DESIGN_SIZE_LABEL");
            sw = g.getFontMetrics().stringWidth(str);
            drawString(g,str, x4+w4-sw, y1);
            str = text("SHIP_DESIGN_POWER_LABEL");
            sw = g.getFontMetrics().stringWidth(str);
            drawString(g,str, x5+w5-sw, y1);
            str = text("SHIP_DESIGN_COST_LABEL");
            scaledFont(g, str, w6-s5, 16, 12);
            sw = g.getFontMetrics().stringWidth(str);
            drawString(g,str, x6+w6-sw, y1);

            // draw ecm jammer row
            int rowH = (y+h-s10-y1)/3;
            int y2 = y1+rowH;
            String title1 = text("SHIP_DESIGN_ECM_TITLE");
            g.setFont(narrowFont(20));
            scaledFont(g, title1, x2-x1-s5, 20, 17);
            drawShadowedString(g, title1, 3, x1, y2, SystemPanel.textShadowC, SystemPanel.whiteText);

            // draw ship maneuver row
            int y3 = y2+rowH;
            String title2 = text("SHIP_DESIGN_MANEUVER_TITLE");
            g.setFont(narrowFont(20));
            drawShadowedString(g, title2, 3, x1, y3, SystemPanel.textShadowC, SystemPanel.whiteText);

            if (UserPreferences.texturesInterface()) 
                drawTexture(g,x, y+s25, w0, h-s25);

            // ecm field
            ShipECM ecm = des.ecm();
            String ecmDesc = ecm.desc(des);
            String ecmName = ecm.name().isEmpty() ? text("SHIP_DESIGN_COMPONENT_NONE") : ecm.name();
            String ecmSize = fmt(ecm.size(des), 1);
            String ecmPower = fmt(ecm.power(des), 1);
            String ecmCost = fmt(ecm.cost(des), 1);
            g.setFont(narrowFont(15));
            g.setColor(darkestBrown);
            List<String> descLines = wrappedLines(g, ecmDesc, w2);
            if (descLines.size() == 1) 
                drawString(g,descLines.get(0), x2, y2);
            else if (descLines.size() > 1) {
                drawString(g,descLines.get(0), x2, y2-s5);
                drawString(g,descLines.get(1), x2, y2+s5);
            }
            g.setColor(Color.black);
            int boxW = w3-s20;
            int boxX = x3+s10;
            int boxY = y2-s15;
            int boxH = s20;
            if (shipDesign().active()) {
                g.fillRoundRect(boxX, boxY, boxW, boxH, s10, s10);
                g.setColor(SystemPanel.whiteText);
            }
            else {
                List<ShipECM> comps = player().shipLab().ecms();
                ShipECM first = comps.get(0);
                ShipECM last = comps.get(comps.size()-1);
                ecmFieldArea.setBounds(boxX, boxY, boxW, boxH);
                ecmFieldDecr.reset();
                if (ecm != first) {
                    ecmFieldDecr.addPoint(boxX - s11, boxY + (boxH / 2));
                    ecmFieldDecr.addPoint(boxX - s3, boxY);
                    ecmFieldDecr.addPoint(boxX - s3, boxY + boxH);
                }
                ecmFieldIncr.reset();
                if (ecm != last) {
                    ecmFieldIncr.addPoint(boxX + boxW + s11, boxY + (boxH / 2));
                    ecmFieldIncr.addPoint(boxX + boxW + s3, boxY);
                    ecmFieldIncr.addPoint(boxX + boxW + s3, boxY + boxH);
                }
                g.fill(ecmFieldArea);
                g.fill(ecmFieldDecr);
                g.fill(ecmFieldIncr);
                g.setColor(SystemPanel.yellowText);
                Stroke prevStr = g.getStroke();
                g.setStroke(BasePanel.stroke2);
                if (hoverTarget == ecmFieldArea)
                    g.draw(ecmFieldArea);
                else if (hoverTarget == ecmFieldDecr)
                    g.draw(ecmFieldDecr);
                else if (hoverTarget == ecmFieldIncr)
                    g.draw(ecmFieldIncr);
                g.setStroke(prevStr);
                g.setColor(SystemPanel.blueText);
            }
            g.setFont(narrowFont(15));
            sw = g.getFontMetrics().stringWidth(ecmName);
            int x3a = x3+s10+((boxW-sw)/2);
            drawString(g,ecmName, x3a, y2);

            g.setFont(narrowFont(17));
            g.setColor(darkestBrown);
            sw = g.getFontMetrics().stringWidth(ecmSize);
            drawString(g,ecmSize, x4+w4-sw, y2);

            sw = g.getFontMetrics().stringWidth(ecmPower);
            drawString(g,ecmPower, x5+w5-sw, y2);

            sw = g.getFontMetrics().stringWidth(ecmCost);
            drawString(g,ecmCost, x6+w6-sw, y2);

             // maneuver field
            ShipManeuver manv = des.maneuver();
            String manvDesc = manv.desc(des);
            String manvName = manv.name().isEmpty() ? text("SHIP_DESIGN_COMPONENT_NONE") : manv.name();
            String manvSize = fmt(manv.size(des), 1);
            String manvPower = fmt(manv.power(des), 1);
            String manvCost = fmt(manv.cost(des), 1);
            g.setFont(narrowFont(15));
            g.setColor(darkestBrown);
            descLines = wrappedLines(g, manvDesc, w2);
            if (descLines.size() == 1) 
                drawString(g,descLines.get(0), x2, y3);
            else if (descLines.size() > 1) {
                drawString(g,descLines.get(0), x2, y3-s5);
                drawString(g,descLines.get(1), x2, y3+s5);
            }
            g.setColor(Color.black);
            boxW = w3-s20;
            boxX = x3+s10;
            boxY = y3-s15;
            boxH = s20;
            if (shipDesign().active()) {
                g.fillRoundRect(boxX, boxY, boxW, boxH, s10, s10);
                g.setColor(SystemPanel.whiteText);
            }
            else {
                List<ShipManeuver> comps = player().shipLab().availableManeuversForDesign(shipDesign());
                ShipManeuver first = comps.get(0);
                ShipManeuver last = comps.get(comps.size()-1);
                maneuverFieldArea.setBounds(boxX, boxY, boxW, boxH);
                maneuverFieldDecr.reset();
                if (manv != first) {
                    maneuverFieldDecr.addPoint(boxX - s11, boxY + (boxH / 2));
                    maneuverFieldDecr.addPoint(boxX - s3, boxY);
                    maneuverFieldDecr.addPoint(boxX - s3, boxY + boxH);
                }
                maneuverFieldIncr.reset();
                if (manv != last) {
                    maneuverFieldIncr.addPoint(boxX + boxW + s11, boxY + (boxH / 2));
                    maneuverFieldIncr.addPoint(boxX + boxW + s3, boxY);
                    maneuverFieldIncr.addPoint(boxX + boxW + s3, boxY + boxH);
                }
                g.fill(maneuverFieldArea);
                g.fill(maneuverFieldDecr);
                g.fill(maneuverFieldIncr);
                g.setColor(SystemPanel.yellowText);
                Stroke prevStr = g.getStroke();
                g.setStroke(BasePanel.stroke2);
                if (hoverTarget == maneuverFieldArea)
                    g.draw(maneuverFieldArea);
                else if (hoverTarget == maneuverFieldDecr)
                    g.draw(maneuverFieldDecr);
                else if (hoverTarget == maneuverFieldIncr)
                    g.draw(maneuverFieldIncr);
                g.setStroke(prevStr);
                g.setColor(SystemPanel.blueText);
            }
            g.setFont(narrowFont(15));
            sw = g.getFontMetrics().stringWidth(manvName);
            x3a = x3+s10+((boxW-sw)/2);
            drawString(g,manvName, x3a, y3);

            g.setFont(narrowFont(17));
            g.setColor(darkestBrown);
            sw = g.getFontMetrics().stringWidth(manvSize);
            drawString(g,manvSize, x4+w4-sw, y3);

            sw = g.getFontMetrics().stringWidth(manvPower);
            drawString(g,manvPower, x5+w5-sw, y3);

            sw = g.getFontMetrics().stringWidth(manvCost);
            drawString(g,manvCost, x6+w6-sw, y3);
        }
        private void drawWeaponInfo(Graphics2D g, ShipDesign des, int x, int y, int w0, int h) {
            g.setColor(darkBrown);
            g.fillRect(x, y+s25, w0, h-s25);

            // set up column starts and widths
            int w = w0-s20;
            int y1 = y+s20;
            int x1 = x+s10; int w1 = w*14/100;
            int x2 = x1+w1; int w2 = w*20/100;
            int x3 = x2+w2; int w3 = w*10/100;
            int x4 = x3+w3; int w4 = w*7/100;
            int x5 = x4+w4; int w5 = w*5/100;
            int x6 = x5+w5; int w6 = w*5/100;
            int x7 = x6+w6; int w7 = w*5/100;
            int x8 = x7+w7; int w8 = w*5/100;
            int x9 = x8+w8; int w9 = w*29/100;

            // draw headers
            g.setColor(Color.black);
            g.setFont(narrowFont(16));
            String str = text("SHIP_DESIGN_TYPE_LABEL");
            int sw = g.getFontMetrics().stringWidth(str);
            drawString(g,str, x2+(w2-sw)/2, y1);
            str = text("SHIP_DESIGN_COUNT_LABEL");
            sw = g.getFontMetrics().stringWidth(str);
            drawString(g,str, x3+(w3-sw)/2, y1);
            str = text("SHIP_DESIGN_DAMAGE_LABEL");
            sw = g.getFontMetrics().stringWidth(str);
            drawString(g,str, x4+(w4-sw)/2, y1);
            str = text("SHIP_DESIGN_RANGE_LABEL");
            sw = g.getFontMetrics().stringWidth(str);
            drawString(g,str, x5+(w5-sw)/2, y1);
            str = text("SHIP_DESIGN_SIZE_LABEL");
            sw = g.getFontMetrics().stringWidth(str);
            drawString(g,str, x6+w6-sw, y1);
            str = text("SHIP_DESIGN_POWER_LABEL");
            sw = g.getFontMetrics().stringWidth(str);
            drawString(g,str, x7+w7-sw, y1);
            str = text("SHIP_DESIGN_COST_LABEL");
            sw = g.getFontMetrics().stringWidth(str);
            drawString(g,str, x8+w8-sw, y1);
            str = text("SHIP_DESIGN_DESCRIPTION_LABEL");
            drawString(g,str, x9+s15, y1);

            // draw weapon row 1
            int rowH = (y+h-s10-y1)/4;
            int y2 = y1+rowH;
            String title1 = text("SHIP_DESIGN_WEAPON_TITLE");
            g.setFont(narrowFont(20));
            drawShadowedString(g, title1, 3, x1, y2, SystemPanel.textShadowC, SystemPanel.whiteText);

            if (UserPreferences.texturesInterface()) 
                drawTexture(g,x, y+s25, w0, h-s25);

            List<ShipWeapon> comps = player().shipLab().weapons();
            ShipWeapon first = comps.get(0);
            ShipWeapon last = comps.get(comps.size()-1);

            for (int i=0;i<ShipDesign.maxWeapons;i++) {
                ShipWeapon wpn = des.weapon(i);
                String wpnDesc = wpn.desc(des);
                String wpnName = wpn.name().isEmpty() ? text("SHIP_DESIGN_COMPONENT_NONE") : wpn.name();
                int count = des.wpnCount(i);
                String wpnCount = ""+count;
                String wpnSize = fmt(count*wpn.size(des), 1);
                String wpnPower = fmt(count*wpn.power(des), 1);
                String wpnCost = fmt(count*wpn.cost(des), 1);
                String wpnRange = ""+wpn.range();
                int wpnDmgLo = wpn.minDamage();
                int wpnDmgHi = wpn.maxDamage();
                String wpnDmg = wpnDmgLo == wpnDmgHi ? ""+wpnDmgLo : ""+wpnDmgLo+"-"+wpnDmgHi;

                g.setColor(Color.black);
                int boxW = w2-s20;
                int boxX = x2+s10;
                int boxY = y2-s15;
                int boxH = s20;
                if (shipDesign().active()) {
                    g.fillRoundRect(boxX, boxY, boxW, boxH, s10, s10);
                   g.setColor(SystemPanel.whiteText);
                }
                 else {
                    weaponFieldArea[i].setBounds(boxX, boxY, boxW, boxH);
                    weaponFieldDecr[i].reset();
                    if (wpn != first) {
                        weaponFieldDecr[i].addPoint(boxX - s11, boxY + (boxH / 2));
                        weaponFieldDecr[i].addPoint(boxX - s3, boxY);
                        weaponFieldDecr[i].addPoint(boxX - s3, boxY + boxH);
                    }
                    weaponFieldIncr[i].reset();
                    if (wpn != last) {
                        weaponFieldIncr[i].addPoint(boxX + boxW + s11, boxY + (boxH / 2));
                        weaponFieldIncr[i].addPoint(boxX + boxW + s3, boxY);
                        weaponFieldIncr[i].addPoint(boxX + boxW + s3, boxY + boxH);
                    }
                    g.fill(weaponFieldArea[i]);
                    g.fill(weaponFieldDecr[i]);
                    g.fill(weaponFieldIncr[i]);
                    g.setColor(SystemPanel.yellowText);
                    Stroke prevStr = g.getStroke();
                    g.setStroke(BasePanel.stroke2);
                    if (hoverTarget == weaponFieldArea[i])
                        g.draw(weaponFieldArea[i]);
                    else if (hoverTarget == weaponFieldDecr[i])
                        g.draw(weaponFieldDecr[i]);
                    else if (hoverTarget == weaponFieldIncr[i])
                        g.draw(weaponFieldIncr[i]);
                    g.setStroke(prevStr);
                    g.setColor(SystemPanel.blueText);
                }
                g.setFont(narrowFont(15));
                sw = g.getFontMetrics().stringWidth(wpnName);
                int x2a = boxX+((boxW-sw)/2);
                drawString(g,wpnName, x2a, y2);


                g.setColor(Color.black);
                boxW = w3-s50;
                boxX = x3+s25;
                boxY = y2-s15;
                boxH = s20;
                if (shipDesign().active()) {
                    g.fillRoundRect(boxX, boxY, boxW, boxH, s10, s10);
                    g.setColor(SystemPanel.whiteText);
                }
                else {
                    weaponCountArea[i].setBounds(boxX, boxY, boxW, boxH);
                    weaponCountDecr[i].reset();
                    weaponCountDecr[i].addPoint(boxX - s11, boxY + (boxH / 2));
                    weaponCountDecr[i].addPoint(boxX - s3, boxY);
                    weaponCountDecr[i].addPoint(boxX - s3, boxY + boxH);
                    weaponCountIncr[i].reset();
                    weaponCountIncr[i].addPoint(boxX + boxW + s11, boxY + (boxH / 2));
                    weaponCountIncr[i].addPoint(boxX + boxW + s3, boxY);
                    weaponCountIncr[i].addPoint(boxX + boxW + s3, boxY + boxH);
                    g.fill(weaponCountArea[i]);
                    g.fill(weaponCountDecr[i]);
                    g.fill(weaponCountIncr[i]);
                    g.setColor(SystemPanel.yellowText);
                    Stroke prevStr = g.getStroke();
                    g.setStroke(BasePanel.stroke2);
                    if (hoverTarget == weaponCountArea[i])
                        g.draw(weaponCountArea[i]);
                    else if (hoverTarget == weaponCountDecr[i])
                        g.draw(weaponCountDecr[i]);
                    else if (hoverTarget == weaponCountIncr[i])
                        g.draw(weaponCountIncr[i]);
                    g.setStroke(prevStr);
                    g.setColor(SystemPanel.blueText);
                }
                g.setFont(narrowFont(15));
                sw = g.getFontMetrics().stringWidth(wpnCount);
                int x3a = boxX+((boxW-sw)/2);
                drawString(g,wpnCount, x3a, y2);

                g.setFont(narrowFont(17));
                g.setColor(darkestBrown);
                sw = g.getFontMetrics().stringWidth(wpnDmg);
                drawString(g,wpnDmg, x4+((w4-sw)/2), y2);
                sw = g.getFontMetrics().stringWidth(wpnRange);
                drawString(g,wpnRange, x5+((w5-sw)/2), y2);
                sw = g.getFontMetrics().stringWidth(wpnSize);
                drawString(g,wpnSize, x6+w6-sw, y2);
                sw = g.getFontMetrics().stringWidth(wpnPower);
                drawString(g,wpnPower, x7+w7-sw, y2);
                sw = g.getFontMetrics().stringWidth(wpnCost);
                drawString(g,wpnCost, x8+w8-sw, y2);
                drawString(g,wpnDesc, x9+s15, y2);
                y2 += rowH;
            }
        }
        private void drawSpecialInfo(Graphics2D g, ShipDesign des, int x, int y, int w0, int h) {
            g.setColor(darkBrown);
            g.fillRect(x, y+s25, w0, h-s25);

            // set up column starts and widths
            int w = w0-s20;
            int y1 = y+s20;
            int x1 = x+s10; int w1 = w*19/100;
            int x2 = x1+w1; int w2 = w*24/100;
            int x3 = x2+w2; int w3 = w*5/100;
            int x4 = x3+w3; int w4 = w*7/100;
            int x5 = x4+w4; int w5 = w*5/100;
            int x6 = x5+w5; int w6 = w*40/100;

            // draw headers
            g.setColor(Color.black);
            g.setFont(narrowFont(16));
            String str = text("SHIP_DESIGN_TYPE_LABEL");
            int sw = g.getFontMetrics().stringWidth(str);
            drawString(g,str, x2+(w2-sw)/2, y1);
            str = text("SHIP_DESIGN_SIZE_LABEL");
            sw = g.getFontMetrics().stringWidth(str);
            drawString(g,str, x3+w3-sw, y1);
            str = text("SHIP_DESIGN_POWER_LABEL");
            sw = g.getFontMetrics().stringWidth(str);
            drawString(g,str, x4+w4-sw, y1);
            str = text("SHIP_DESIGN_COST_LABEL");
            sw = g.getFontMetrics().stringWidth(str);
            drawString(g,str, x5+w5-sw, y1);
            str = text("SHIP_DESIGN_DESCRIPTION_LABEL");
            drawString(g,str, x6+s15, y1);

            // draw special 1
            int rowH = (y+h-s10-y1)/3;
            int y2 = y1+rowH;
            String title1 = text("SHIP_DESIGN_SPECIAL_TITLE");
            g.setFont(narrowFont(20));
            drawShadowedString(g, title1, 3, x1, y2, SystemPanel.textShadowC, SystemPanel.whiteText);

           if (UserPreferences.texturesInterface()) 
                drawTexture(g,x, y+s25, w0, h-s25);

            for (int i=0;i<ShipDesign.maxSpecials;i++) {
                ShipSpecial wpn = des.special(i);
                String wpnDesc = wpn.desc(des);
                String wpnName = wpn.name().isEmpty() ? text("SHIP_DESIGN_COMPONENT_NONE") : wpn.name();
                String wpnSize = fmt(wpn.size(des), 1);
                String wpnPower = fmt(wpn.power(des), 1);
                String wpnCost = fmt(wpn.cost(des), 1);

                g.setColor(Color.black);
                int boxW = w2-s20;
                int boxX = x2+s10;
                int boxY = y2-s15;
                int boxH = s20;
                if (shipDesign().active()) {
                    g.fillRoundRect(boxX, boxY, boxW, boxH, s10, s10);
                    g.setColor(SystemPanel.whiteText);
                }
                else {
                    List<ShipSpecial> specials = des.availableSpecialsForSlot(i);
                    ShipSpecial first = specials.get(0);
                    ShipSpecial last = specials.get(specials.size()-1);
                    specialsFieldArea[i].setBounds(boxX, boxY, boxW, boxH);
                    specialsFieldDecr[i].reset();
                    if (wpn != first) {
                        specialsFieldDecr[i].addPoint(boxX - s11, boxY + (boxH / 2));
                        specialsFieldDecr[i].addPoint(boxX - s3, boxY);
                        specialsFieldDecr[i].addPoint(boxX - s3, boxY + boxH);
                    }
                    specialsFieldIncr[i].reset();
                    if (wpn != last) {
                        specialsFieldIncr[i].addPoint(boxX + boxW + s11, boxY + (boxH / 2));
                        specialsFieldIncr[i].addPoint(boxX + boxW + s3, boxY);
                        specialsFieldIncr[i].addPoint(boxX + boxW + s3, boxY + boxH);
                    }
                    g.fill(specialsFieldArea[i]);
                    g.fill(specialsFieldDecr[i]);
                    g.fill(specialsFieldIncr[i]);
                    g.setColor(SystemPanel.yellowText);
                    Stroke prevStr = g.getStroke();
                    g.setStroke(BasePanel.stroke2);
                    if (hoverTarget == specialsFieldArea[i])
                        g.draw(specialsFieldArea[i]);
                    else if (hoverTarget == specialsFieldDecr[i])
                        g.draw(specialsFieldDecr[i]);
                    else if (hoverTarget == specialsFieldIncr[i])
                        g.draw(specialsFieldIncr[i]);
                    g.setStroke(prevStr);
                    g.setColor(SystemPanel.blueText);
                }
                g.setFont(narrowFont(15));
                sw = g.getFontMetrics().stringWidth(wpnName);
                int x2a = boxX+ ((boxW - sw) / 2);
                drawString(g,wpnName, x2a, y2);

                g.setFont(narrowFont(17));
                g.setColor(darkestBrown);
                sw = g.getFontMetrics().stringWidth(wpnSize);
                drawString(g,wpnSize, x3 + w3 - sw, y2);
                sw = g.getFontMetrics().stringWidth(wpnPower);
                drawString(g,wpnPower, x4 + w4 - sw, y2);
                sw = g.getFontMetrics().stringWidth(wpnCost);
                drawString(g,wpnCost, x5 + w5 - sw, y2);
                drawString(g,wpnDesc, x6 + s15, y2);
                y2 += rowH;
            }
        }
        private void openScrapDialog() {
            confirmScrapUI.targetDesign(shipDesign());
            enableGlassPane(confirmScrapUI);
            return;
        }
        private void openCreateDialog() {
            if (!shipDesign().validConfiguration())
                return;
            // if we are deploying from the prototype, switch to the first
            // empty slot and copy the prototype design to it
            if (selectedSlot < 0) {
                int firstAvailable = -1;
                for (int i=0;i<ShipDesignLab.MAX_DESIGNS;i++) {
                    ShipDesign des1 = player().shipLab().design(i);
                    if ((firstAvailable < 0) && !des1.active())
                       firstAvailable = i;     
                }
                selectedSlot = firstAvailable;
                configPanel.shipDesign().copyFrom(player().shipLab().prototypeDesign());
            }
            confirmCreateUI.targetDesign(shipDesign());
            confirmCreateUI.renamingOnly = false;
            enableGlassPane(confirmCreateUI);
            return;
        }
        private void openRenameDialog() {
            confirmCreateUI.targetDesign(shipDesign());
            confirmCreateUI.renamingOnly = true;
            enableGlassPane(confirmCreateUI);
            return;
        }
        private void clearDesign() {
            player().shipLab().clearDesign(shipDesign());
            repaint();
            return;
        }
        private void shipImageDecr() {
            shipDesign().prevImage();
            loadShipImages();
            repaint();
        }
        private void shipImageIncr() {
            shipDesign().nextImage();
            loadShipImages();
            repaint();
        }
        private void shipSizeDecrement() {
            ShipDesign des =  shipDesign();
            if (des.size() > ShipDesign.SMALL) {
                des.size(des.size() - 1);
                loadShipImages();
                repaint();
            }
        }
        private void shipSizeIncrement() {
            ShipDesign des =  shipDesign();
            if (des.size() < ShipDesign.HUGE) {
                des.size(des.size() + 1);
                loadShipImages();
                repaint();
            }
        }
        private void openShipEngineDialog() {
            engineSelectionUI.selectedDesign = shipDesign();
            enableGlassPane(engineSelectionUI);
            return;
        }
        private void shipEngineDecrement() {
            ShipDesign des =  shipDesign();
            ShipDesignLab lab = player().shipLab();
            List<ShipEngine> engines = lab.engines();
            int index = engines.indexOf(des.engine());
            if (index > 0) {
                des.engine(engines.get(index - 1));
                // if we decrement engine, our selected maneuver may no longer be valid
                List<ShipManeuver> manv = lab.availableManeuversForDesign(des);
                if (!manv.contains(des.maneuver()))
                    des.maneuver(manv.get(manv.size()-1));
                repaint();
            }
        }
        private void shipEngineIncrement() {
            ShipDesign des =  shipDesign();
            List<ShipEngine> engines = player().shipLab().engines();
            int index = engines.indexOf(des.engine());
            if (index < (engines.size()-1)) {
                des.engine(engines.get(index + 1));
                repaint();
            }
        }
        private void openShipComputerDialog() {
            computerSelectionUI.selectedDesign = shipDesign();
            enableGlassPane(computerSelectionUI);
            return;
        }
        private void shipComputerDecrement() {
            ShipDesign des =  shipDesign();
            List<ShipComputer> comps = player().shipLab().computers();
            int index = comps.indexOf(des.computer());
            if (index > 0) {
                des.computer(comps.get(index - 1));
                repaint();
            }
        }
        private void shipComputerIncrement() {
            ShipDesign des =  shipDesign();
            List<ShipComputer> comps = player().shipLab().computers();
            int index = comps.indexOf(des.computer());
            if (index < (comps.size()-1)) {
                des.computer(comps.get(index + 1));
                repaint();
            }
        }
        private void openShipArmorDialog() {
            armorSelectionUI.selectedDesign = shipDesign();
            enableGlassPane(armorSelectionUI);
            return;
        }
        private void shipArmorDecrement() {
            ShipDesign des =  shipDesign();
            List<ShipArmor> armors = player().shipLab().armors();
            int index = armors.indexOf(des.armor());
            if (index > 0) {
                des.armor(armors.get(index - 1));
                repaint();
            }
        }
        private void shipArmorIncrement() {
            ShipDesign des =  shipDesign();
            List<ShipArmor> armors = player().shipLab().armors();
            int index = armors.indexOf(des.armor());
            if (index < (armors.size()-1)) {
                des.armor(armors.get(index + 1));
                repaint();
            }
        }
        private void openShipShieldsDialog() {
            shieldSelectionUI.selectedDesign = shipDesign();
            enableGlassPane(shieldSelectionUI);
            return;
        }
        private void shipShieldsDecrement() {
            ShipDesign des =  shipDesign();
            List<ShipShield> shields = player().shipLab().shields();
            int index = shields.indexOf(des.shield());
            if (index > 0) {
                des.shield(shields.get(index - 1));
                repaint();
            }
        }
        private void shipShieldsIncrement() {
            ShipDesign des =  shipDesign();
            List<ShipShield> shields = player().shipLab().shields();
            int index = shields.indexOf(des.shield());
            if (index < (shields.size()-1)) {
                des.shield(shields.get(index + 1));
                repaint();
            }
        }
        private void openShipECMDialog() {
            ecmSelectionUI.selectedDesign = shipDesign();
            enableGlassPane(ecmSelectionUI);
            return;
        }
        private void shipECMDecrement() {
            ShipDesign des =  shipDesign();
            List<ShipECM> ecms = player().shipLab().ecms();
            int index = ecms.indexOf(des.ecm());
            if (index > 0) {
                des.ecm(ecms.get(index - 1));
                repaint();
            }
        }
        private void shipECMIncrement() {
            ShipDesign des =  shipDesign();
            List<ShipECM> ecms = player().shipLab().ecms();
            int index = ecms.indexOf(des.ecm());
            if (index < (ecms.size()-1)) {
                des.ecm(ecms.get(index + 1));
                repaint();
            }
        }
        private void openShipManeuverDialog() {
            maneuverSelectionUI.selectedDesign = shipDesign();
            enableGlassPane(maneuverSelectionUI);
            return;
        }
        private void shipManeuverDecrement() {
            ShipDesign des =  shipDesign();
            List<ShipManeuver> maneuvers = player().shipLab().availableManeuversForDesign(des);
            int index = maneuvers.indexOf(des.maneuver());
            if (index > 0) {
                des.maneuver(maneuvers.get(index - 1));
                repaint();
            }
        }
        private void shipManeuverIncrement() {
            ShipDesign des =  shipDesign();
            List<ShipManeuver> maneuvers = player().shipLab().availableManeuversForDesign(des);
            int index = maneuvers.indexOf(des.maneuver());
            if (index < (maneuvers.size()-1)) {
                des.maneuver(maneuvers.get(index + 1));
                repaint();
            }
        }
        private void openShipWeaponDialog(int i) {
            weaponSelectionUI.selectedDesign = shipDesign();
            weaponSelectionUI.bank(i);
            enableGlassPane(weaponSelectionUI);
            return;
        }
        private void shipWeaponDecrement(int i) {
            ShipDesign des =  shipDesign();
            List<ShipWeapon> weapons = player().shipLab().weapons();
            int index = weapons.indexOf(des.weapon(i));
            if (index > 0) {
                des.weapon(i, weapons.get(index - 1));
                if (des.weapon(i).isNone())
                    des.wpnCount(i,0);
                repaint();
            }
        }
        private void shipWeaponIncrement(int i) {
            ShipDesign des =  shipDesign();
            List<ShipWeapon> weapons = player().shipLab().weapons();
            int index = weapons.indexOf(des.weapon(i));
            if (index < (weapons.size()-1)) {
                des.weapon(i, weapons.get(index + 1));
                repaint();
            }
        }
        private void shipWeaponCountDecrement(int i, int amt) {
            ShipDesign des = shipDesign();
            if ((des.wpnCount(i) > 0) && !des.weapon(i).isNone()) {
                int newAmt = max(0, des.wpnCount(i)-amt);
                des.wpnCount(i, newAmt);
                repaint();
            }
        }
        private void shipWeaponCountIncrement(int i, int amt) {
            ShipDesign des =  shipDesign();
            if (!des.weapon(i).isNone()) {
                des.wpnCount(i, des.wpnCount(i) + amt);
                repaint();
            }
        }
        private void openShipSpecialsDialog(int i) {
            specialSelectionUI.selectedDesign = shipDesign();
            specialSelectionUI.bank(i);
            enableGlassPane(specialSelectionUI);
            return;
        }
        private void shipSpecialsDecrement(int i) {
            ShipDesign des =  shipDesign();
            List<ShipSpecial> specials = des.availableSpecialsForSlot(i);
            int index = specials.indexOf(des.special(i));
            if (index > 0) {
                des.special(i, specials.get(index - 1));
                repaint();
            }
        }
        private void shipSpecialsIncrement(int i) {
            ShipDesign des =  shipDesign();
            List<ShipSpecial> specials = des.availableSpecialsForSlot(i);
            int index = specials.indexOf(des.special(i));
            if (index < (specials.size()-1)) {
                des.special(i, specials.get(index + 1));
                repaint();
            }
        }
        private void setShipColor(int i) {
            ShipDesign des =  shipDesign();
            des.shipColor(ShipDesign.shipColors[i]);
        }
        @Override
        public void mouseDragged(MouseEvent e) { }
        @Override
        public void mouseMoved(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();

            Shape prevHover = hoverTarget;
            hoverTarget = null;

            if (scrapButtonArea.contains(x,y))
                hoverTarget = scrapButtonArea;
            else if (createButtonArea.contains(x,y))
                hoverTarget = createButtonArea;
            else if (renameButtonArea.contains(x,y))
                hoverTarget = renameButtonArea;
            else if (clearButtonArea.contains(x,y))
                hoverTarget = clearButtonArea;
            
            if (shipDesign().active()) {
                if (prevHover != hoverTarget)
                    repaint();
                return;
            }
            
            if (shipImageDecr.contains(x,y))
                hoverTarget = shipImageDecr;
            else if (shipImageIncr.contains(x,y))
                hoverTarget = shipImageIncr;
            else if (shipImageArea.contains(x,y))
                hoverTarget = shipImageArea;
            else if (sizeFieldArea.contains(x,y))
                hoverTarget = sizeFieldArea;
            else if (sizeFieldDecr.contains(x,y))
                hoverTarget = sizeFieldDecr;
            else if (sizeFieldIncr.contains(x,y))
                hoverTarget = sizeFieldIncr;
            else if (engineFieldArea.contains(x,y))
                hoverTarget = engineFieldArea;
            else if (engineFieldDecr.contains(x,y))
                hoverTarget = engineFieldDecr;
            else if (engineFieldIncr.contains(x,y))
                hoverTarget = engineFieldIncr;
            else if (computerFieldArea.contains(x,y))
                hoverTarget = computerFieldArea;
            else if (computerFieldDecr.contains(x,y))
                hoverTarget = computerFieldDecr;
            else if (computerFieldIncr.contains(x,y))
                hoverTarget = computerFieldIncr;
            else if (armorFieldArea.contains(x,y))
                hoverTarget = armorFieldArea;
            else if (armorFieldDecr.contains(x,y))
                hoverTarget = armorFieldDecr;
            else if (armorFieldIncr.contains(x,y))
                hoverTarget = armorFieldIncr;
            else if (shieldsFieldArea.contains(x,y))
                hoverTarget = shieldsFieldArea;
            else if (shieldsFieldDecr.contains(x,y))
                hoverTarget = shieldsFieldDecr;
            else if (shieldsFieldIncr.contains(x,y))
                hoverTarget = shieldsFieldIncr;
            else if (maneuverFieldArea.contains(x,y))
                hoverTarget = maneuverFieldArea;
            else if (maneuverFieldDecr.contains(x,y))
                hoverTarget = maneuverFieldDecr;
            else if (maneuverFieldIncr.contains(x,y))
                hoverTarget = maneuverFieldIncr;
            else if (ecmFieldArea.contains(x,y))
                hoverTarget = ecmFieldArea;
            else if (ecmFieldDecr.contains(x,y))
                hoverTarget = ecmFieldDecr;
            else if (ecmFieldIncr.contains(x,y))
                hoverTarget = ecmFieldIncr;

            if (hoverTarget == null) {
                for (int i = 0; i < weaponFieldArea.length; i++) {
                    if (weaponFieldArea[i].contains(x, y)) {
                        hoverTarget = weaponFieldArea[i];
                        break;
                    }
                    if (weaponFieldDecr[i].contains(x, y)) {
                        hoverTarget = weaponFieldDecr[i];
                        break;
                    }
                    if (weaponFieldIncr[i].contains(x, y)) {
                        hoverTarget = weaponFieldIncr[i];
                        break;
                    }
                    if (weaponCountArea[i].contains(x, y)) {
                        hoverTarget = weaponCountArea[i];
                        break;
                    }
                    if (weaponCountDecr[i].contains(x, y)) {
                        hoverTarget = weaponCountDecr[i];
                        break;
                    }
                    if (weaponCountIncr[i].contains(x, y)) {
                        hoverTarget = weaponCountIncr[i];
                        break;
                    }
                }
            }

            if (hoverTarget == null) {
                for (int i = 0; i < specialsFieldArea.length; i++) {
                    if (specialsFieldArea[i].contains(x, y)) {
                        hoverTarget = specialsFieldArea[i];
                        break;
                    }
                    if (specialsFieldDecr[i].contains(x, y)) {
                        hoverTarget = specialsFieldDecr[i];
                        break;
                    }
                    if (specialsFieldIncr[i].contains(x, y)) {
                        hoverTarget = specialsFieldIncr[i];
                        break;
                    }
                }
            }
            
            if (hoverTarget == null) {
                for (int i = 0; i < shipColorArea.length; i++) {
                    if (shipColorArea[i].contains(x, y)) {
                        hoverTarget = shipColorArea[i];
                        break;
                    }
                }
            }
            
            if (prevHover != hoverTarget)
                repaint();
        }
        @Override
        public void mouseClicked(MouseEvent mouseEvent) { }
        @Override
        public void mousePressed(MouseEvent mouseEvent) { }
        @Override
        public void mouseReleased(MouseEvent e) {
            boolean shiftPressed = (e.getModifiers() & InputEvent.SHIFT_MASK) != 0;
            boolean ctrlPressed = (e.getModifiers() & InputEvent.CTRL_MASK) != 0;
            
            if (hoverTarget == scrapButtonArea) {
                softClick(); openScrapDialog(); return;
            }
            else if (hoverTarget == createButtonArea) {
                softClick(); openCreateDialog(); return;
            }
            else if (hoverTarget == renameButtonArea) {
                softClick(); openRenameDialog(); return;
            }
            else if (hoverTarget == clearButtonArea) {
                softClick(); clearDesign(); return;
            }
            
            if (shipDesign().active())
                return;
            
            if (hoverTarget == shipImageDecr) {
                softClick(); shipImageDecr(); return;
            }
            else if (hoverTarget == shipImageIncr) {
                softClick(); shipImageIncr(); return;
            }
            else if (hoverTarget == sizeFieldDecr) {
                softClick(); shipSizeDecrement(); return;
            }
            else if (hoverTarget == sizeFieldIncr) {
                softClick(); shipSizeIncrement(); return;
            }
            else if (hoverTarget == engineFieldArea) {
                softClick(); openShipEngineDialog(); return;
            }
            else if (hoverTarget == engineFieldDecr) {
                softClick(); shipEngineDecrement(); return;
            }
            else if (hoverTarget == engineFieldIncr) {
                softClick(); shipEngineIncrement(); return;
            }
            else if (hoverTarget == computerFieldArea) {
                softClick(); openShipComputerDialog(); return;
            }
            else if (hoverTarget == computerFieldDecr) {
                softClick(); shipComputerDecrement(); return;
            }
            else if (hoverTarget == computerFieldIncr) {
                softClick(); shipComputerIncrement(); return;
            }
            else if (hoverTarget == armorFieldArea) {
                softClick(); openShipArmorDialog(); return;
            }
            else if (hoverTarget == armorFieldDecr) {
                softClick(); shipArmorDecrement(); return;
            }
            else if (hoverTarget == armorFieldIncr) {
                softClick(); shipArmorIncrement(); return;
            }
            else if (hoverTarget == shieldsFieldArea) {
                softClick(); openShipShieldsDialog(); return;
            }
            else if (hoverTarget == shieldsFieldDecr) {
                softClick(); shipShieldsDecrement(); return;
            }
            else if (hoverTarget == shieldsFieldIncr) {
                softClick(); shipShieldsIncrement(); return;
            }
            else if (hoverTarget == ecmFieldArea) {
                softClick(); openShipECMDialog(); return;
            }
            else if (hoverTarget == ecmFieldDecr) {
                softClick(); shipECMDecrement(); return;
            }
            else if (hoverTarget == ecmFieldIncr) {
                softClick(); shipECMIncrement(); return;
            }
            else if (hoverTarget == maneuverFieldArea) {
                softClick(); openShipManeuverDialog(); return;
            }
            else if (hoverTarget == maneuverFieldDecr) {
                softClick(); shipManeuverDecrement(); return;
            }
            else if (hoverTarget == maneuverFieldIncr) {
                softClick(); shipManeuverIncrement(); return;
            }
            for (int i=0;i<weaponFieldArea.length;i++) {
                if (hoverTarget == weaponFieldArea[i]) {
                    softClick(); openShipWeaponDialog(i); return;
                }
                if (hoverTarget == weaponFieldDecr[i]) {
                    softClick(); shipWeaponDecrement(i); return;
                }
                if (hoverTarget == weaponFieldIncr[i]) {
                    softClick(); shipWeaponIncrement(i); return;
                }
                if (hoverTarget == weaponCountDecr[i]) {
                    softClick();
                    if (shiftPressed) 
                        shipWeaponCountDecrement(i,5);
                    else if (ctrlPressed)
                        shipWeaponCountDecrement(i,20);
                    else
                        shipWeaponCountDecrement(i,1); 
                    return;
                }
                if (hoverTarget == weaponCountIncr[i]) {
                    softClick();
                    if (shiftPressed) 
                        shipWeaponCountIncrement(i,5); 
                    else if (ctrlPressed) 
                        shipWeaponCountIncrement(i,20); 
                    else
                        shipWeaponCountIncrement(i,1); 
                    return;
                }
            }
            for (int i=0;i<specialsFieldArea.length;i++) {
                if (hoverTarget == specialsFieldArea[i]) {
                    softClick(); openShipSpecialsDialog(i); return;
                }
                if (hoverTarget == specialsFieldDecr[i]) {
                    softClick(); shipSpecialsDecrement(i); return;
                }
                if (hoverTarget == specialsFieldIncr[i]) {
                    softClick(); shipSpecialsIncrement(i); return;
                }
            }
            for (int i=0;i<shipColorArea.length;i++) {
                if (hoverTarget == shipColorArea[i]) {
                    softClick(); setShipColor(i); return;
                }
            }
        }
        @Override
        public void mouseEntered(MouseEvent mouseEvent) {}
        @Override
        public void mouseExited(MouseEvent mouseEvent) {
            if (hoverTarget != null) {
                hoverTarget = null;
                repaint();
            }
        }
        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            int count = e.getUnitsToScroll();
            if (shipDesign().active())
                return;
            
            boolean shiftPressed = (e.getModifiers() & InputEvent.SHIFT_MASK) != 0;
            boolean ctrlPressed = (e.getModifiers() & InputEvent.CTRL_MASK) != 0;
            
            if (hoverTarget == shipImageArea) {
                if (count < 0)
                    shipImageDecr();
                else
                    shipImageIncr();
                return;
            }
            if (hoverTarget == sizeFieldArea) {
                if (count < 0)
                    shipSizeDecrement();
                else
                    shipSizeIncrement();
                return;
            }
            if (hoverTarget == engineFieldArea) {
                if (count < 0)
                    shipEngineDecrement();
                else
                    shipEngineIncrement();
                return;
            }
            if (hoverTarget == computerFieldArea) {
                if (count < 0)
                    shipComputerDecrement();
                else
                    shipComputerIncrement();
                return;
            }
            if (hoverTarget == armorFieldArea) {
                if (count < 0)
                    shipArmorDecrement();
                else
                    shipArmorIncrement();
                return;
            }
            if (hoverTarget == shieldsFieldArea) {
                if (count < 0)
                    shipShieldsDecrement();
                else
                    shipShieldsIncrement();
                return;
            }
            if (hoverTarget == ecmFieldArea) {
                if (count < 0)
                    shipECMDecrement();
                else
                    shipECMIncrement();
                return;
            }
            if (hoverTarget == maneuverFieldArea) {
                if (count < 0)
                    shipManeuverDecrement();
                else
                    shipManeuverIncrement();
                return;
            }
            for (int i=0;i<weaponFieldArea.length;i++) {
                if (hoverTarget == weaponFieldArea[i]) {
                    if (count < 0)
                        shipWeaponDecrement(i);
                    else
                        shipWeaponIncrement(i);
                    return;
                }
                if (hoverTarget == weaponCountArea[i]) {
                    if (count > 0) {
                        if (shiftPressed) 
                            shipWeaponCountDecrement(i,5);
                        else if (ctrlPressed) 
                            shipWeaponCountDecrement(i,20);
                        else 
                            shipWeaponCountDecrement(i,1);
                    }
                    else {
                        if (shiftPressed) 
                            shipWeaponCountIncrement(i,5);
                        else if (ctrlPressed) 
                            shipWeaponCountIncrement(i,20);
                        else 
                            shipWeaponCountIncrement(i,1);
                    }
                    return;
                }
            }
            for (int i=0;i<specialsFieldArea.length;i++) {
                if (hoverTarget == specialsFieldArea[i]) {
                    if (count < 0)
                        shipSpecialsDecrement(i);
                    else
                        shipSpecialsIncrement(i);
                    return;
                }
            }
        }
    }
}