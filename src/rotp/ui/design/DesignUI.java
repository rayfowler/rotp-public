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
import rotp.model.ships.*;
import rotp.ui.*;
import rotp.ui.combat.ShipBattleUI;
import rotp.ui.main.SystemPanel;
import rotp.util.AnimationManager;
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
    int displayShipW = -1;
    int displayShipH = -1;

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
    }
    public void init() {
        shipCounts = galaxy().ships.shipDesignCounts(player().id);
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
        Border emptyBorder = newEmptyBorder(0, pad, pad, pad);
        setBorder(emptyBorder);

        // create center panel
        DesignTitlePanel titlePanel = new DesignTitlePanel("SHIP_DESIGN_TITLE");
        configPanel = new DesignConfigPanel();
        BasePanel mainPanel = new BasePanel();
        mainPanel.setOpaque(false);
        mainPanel.setBorder(newEmptyBorder(20,0,0,0));
        mainPanel.setLayout(new BorderLayout(0, s5));
        mainPanel.add(titlePanel, BorderLayout.NORTH);
        mainPanel.add(configPanel, BorderLayout.CENTER);

        // create design slot panel on right side of UI
        designSlotsPanel = new DesignSlotsPanel();

        DesignTitlePanel slotsTitlePanel = new DesignTitlePanel("SHIP_DESIGN_SLOTS");
        BasePanel rightPanel = new BasePanel();
        rightPanel.setPreferredSize(new Dimension(rightPaneW, getHeight()));
        rightPanel.setBorder(newEmptyBorder(20,0,0,0));
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
        ShipDesign design = configPanel.shipDesign();
        if (k == KeyEvent.VK_ESCAPE) {
            exit(false);
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
    class DesignTitlePanel extends BasePanel {
        private static final long serialVersionUID = 1L;
        String titleKey;
        public DesignTitlePanel(String s) {
            titleKey = s;
            init();
        }
        private void init() {
            setPreferredSize(new Dimension(getWidth(), s45));
            setOpaque(false);
        }
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            String title = text(titleKey);
            g.setFont(narrowFont(32));
            g.setColor(SystemPanel.orangeText);
            g.drawString(title, s10, s35);
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

            int boxH = getHeight()-s10;
            int boxW = boxH*6/5;
            drawShip(g);

            int leftM = boxW+s10;
            ShipDesign des = shipDesign();
            if (!des.active()) {
                g.setFont(narrowFont(18));
                drawShadowedString(g, text("SHIP_DESIGN_AVAILABLE"), 3, leftM, s20, SystemPanel.textShadowC, SystemPanel.whiteText);
                g.setFont(narrowFont(14));
                g.setColor(SystemPanel.blackText);
                String desc = designNum == selectedSlot ? text("SHIP_DESIGN_AVAILABLE_DESC2"): text("SHIP_DESIGN_AVAILABLE_DESC");
                List<String> lines = wrappedLines(g, desc, getWidth()-s20-leftM);
                int y0 = s40;
                for (String line: lines) {
                    g.drawString(line, leftM, y0);
                    y0 += s15;
                }
            }
            else {
                g.setFont(narrowFont(18));
                drawShadowedString(g, des.name(), 3, leftM, s20, SystemPanel.textShadowC, SystemPanel.whiteText);
                g.setFont(narrowFont(14));
                g.setColor(SystemPanel.blackText);
                String desc = text("SHIP_DESIGN_SLOT_DESC");
                int sw = g.getFontMetrics().stringWidth(desc);
                g.drawString(desc, leftM, s40);
                g.drawString(str(shipCounts[des.id()]), leftM+sw+s5, s40);
            }
            // draw copy button
            copyButtonArea[designNum].setBounds(0, 0, 0, 0);
            if (shipDesign().active() && !player().shipLab().design(selectedSlot).active()) {
                g.setFont(narrowFont(18));
                String str = text("SHIP_DESIGN_COPY_BUTTON");
                int sw = g.getFontMetrics().stringWidth(str);
                int buttonW = sw + s40;
                int buttonH = s25;
                int buttonX = leftM;
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
        private ShipDesign shipDesign()   { return player().shipLab().design(designNum); }
        private void drawShip(Graphics g) {
            int boxH = getHeight()-s10;
            int boxW = boxH*6/5;
            g.setColor(ShipBattleUI.spaceBlue);
            g.fillRect(s5,s5,boxW,boxH);

            ShipDesign des = shipDesign();
            if (!des.active())
                return;

            ShipImage shipImage = des.shipImage();
            Image img = icon(shipImage.nextIcon()).getImage();

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
                player().shipLab().design(selectedSlot).copyFrom(shipDesign());
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

            int boxY = s3+(selectedSlot*boxH);
            int boxX = s3;
            g.setStroke(stroke5);
            g.setColor(SystemPanel.yellowText);
            g.drawRect(boxX, boxY, boxW, boxH);
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
        private ShipDesign shipDesign()   { return player().shipLab().design(selectedSlot); }
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
                Point2D start = new Point2D.Float(0, getHeight() / 2);
                Point2D end = new Point2D.Float(0, getHeight());
                float[] dist = {0.0f, 1.0f};
                Color[] colors = {darkerBrown, brown};
                configGradient = new LinearGradientPaint(start, end, dist, colors);
            }
            g.setPaint(configGradient);
            g.fillRect(0, getHeight() / 2, getWidth(), getHeight() / 2);
            if (UserPreferences.textures()) 
                drawTexture(g0,0, getHeight() / 2, getWidth(), getHeight() / 2);

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
            
            if (UserPreferences.textures()) 
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
            String name = des.active() ? des.name() : text("SHIP_DESIGN_NEW");
            g.setFont(narrowFont(32));
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
            g.drawString(text("SHIP_DESIGN_SIZE_LABEL"), x1, y1);
            int y2 = y1+rowH+s10;
            g.drawString(text("SHIP_DESIGN_RANGE_LABEL"), x1, y2);
            int y3 = y2+rowH;
            g.drawString(text("SHIP_DESIGN_SPEED_LABEL"), x1, y3);
            int y4 = y3+rowH;
            g.drawString(text("SHIP_DESIGN_COST_LABEL"), x1, y4);
            int y5 = y4+rowH;
            g.drawString(text("SHIP_DESIGN_TOTAL_SPACE_LABEL"), x1, y5);
            int y6 = y5+rowH;
            if (des.availableSpace() < 0)
                g.setColor(errorRedC);
            g.drawString(text("SHIP_DESIGN_AVAIL_SPACE_LABEL"), x1, y6);

            g.setFont(narrowFont(22));
            drawShadowedString(g, text("SHIP_DESIGN_COMBAT_STATS_TITLE"),3,x2+s5,y1,SystemPanel.textShadowC, SystemPanel.whiteText);

            if (UserPreferences.textures()) 
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
                sizeFieldDecr.addPoint(boxX-s11, boxY+(boxH/2));
                sizeFieldDecr.addPoint(boxX-s3, boxY);
                sizeFieldDecr.addPoint(boxX-s3, boxY+boxH);
                sizeFieldIncr.reset();
                sizeFieldIncr.addPoint(boxX+boxW+s11, boxY+(boxH/2));
                sizeFieldIncr.addPoint(boxX+boxW+s3, boxY);
                sizeFieldIncr.addPoint(boxX+boxW+s3, boxY+boxH);
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
            g.drawString(sizeStr, x1a, y1);

            g.setColor(darkestBrown);
            g.setFont(narrowFont(17));
           
            String str = player().tech().topFuelRangeTech().unlimited ? text("SHIP_DESIGN_RANGE_UNLIMITED") : text("SHIP_DESIGN_RANGE_VALUE", (int)des.range());
            sw = g.getFontMetrics().stringWidth(str);
            g.drawString(str, x2-s20-sw, y2);
            str = text("SHIP_DESIGN_SPEED_VALUE", (int)des.warpSpeed());
            sw = g.getFontMetrics().stringWidth(str);
            g.drawString(str, x2-s20-sw, y3);
            des.recalculateCost();
            str = text("SHIP_DESIGN_COST_VALUE", (int)des.cost());
            sw = g.getFontMetrics().stringWidth(str);
            g.drawString(str, x2-s20-sw, y4);
            str = ""+ (int)des.spaceUsed();
            sw = g.getFontMetrics().stringWidth(str);
            g.drawString(str, x2-s20-sw, y5);
            str = "" + (int)Math.floor(des.availableSpace());
            sw = g.getFontMetrics().stringWidth(str);
            if (des.availableSpace() < 0)
                g.setColor(errorRedC);
            g.drawString(str, x2-s20-sw, y6);

            // right side
            g.setColor(Color.black);
            g.setFont(narrowFont(17));
            g.drawString(text("SHIP_DESIGN_HIT_POINTS_LABEL"), x2+s10, y2);
            g.drawString(text("SHIP_DESIGN_MISSILE_DEF_LABEL"), x2+s10, y3);
            g.drawString(text("SHIP_DESIGN_BEAM_DEF_LABEL"), x2+s10, y4);
            g.drawString(text("SHIP_DESIGN_ATTACK_LEVEL_LABEL"), x2+s10, y5);
            g.drawString(text("SHIP_DESIGN_COMBAT_SPEED_LABEL"), x2+s10, y6);

            // draw right side values
            int x3 = x+w-s20;
            g.setColor(darkestBrown);
            g.setFont(narrowFont(17));
            str = ""+(int)des.hits();
            sw = g.getFontMetrics().stringWidth(str);
            g.drawString(str, x3-sw, y2);
            str = ""+des.missileDefense();
            sw = g.getFontMetrics().stringWidth(str);
            g.drawString(str, x3-sw, y3);
            str = ""+des.beamDefense();
            sw = g.getFontMetrics().stringWidth(str);
            g.drawString(str, x3-sw, y4);
            str = ""+ (int)des.attackLevel();
            sw = g.getFontMetrics().stringWidth(str);
            g.drawString(str, x3-sw, y5);
            str = ""+ des.combatSpeed();
            sw = g.getFontMetrics().stringWidth(str);
            g.drawString(str, x3-sw, y6);

            if (des.active()) {
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

                // draw create button
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
            drawShadowedString(g, text("SHIP_DESIGN_ENGINES_TITLE"), 3, x + s10, y + s20, SystemPanel.textShadowC, SystemPanel.whiteText);
            g.setColor(Color.black);
            g.setFont(narrowFont(12));
            String desc = text("SHIP_DESIGN_ENGINES_DESC");
            List<String> lines = wrappedLines(g, desc, w - s30);
            int y0 = y + s28;
            int x0 = x + s20;
            for (String line : lines) {
                y0 += s12;
                g.drawString(line, x0, y0);
            }
            int scrunch = lines.size() > 1 ? ((lines.size() - 1) * s12) / 4 : 0;
            g.setColor(Color.black);
            g.setFont(narrowFont(16));
            int y1 = y0 + s26;
            g.drawString(text("SHIP_DESIGN_ENGINE_TYPE"), x0, y1);
            int y2 = y1 + s33 - scrunch;
            g.drawString(text("SHIP_DESIGN_ENGINE_SPEED"), x0, y2);
            int y3 = y2 + s17;
            g.drawString(text("SHIP_DESIGN_ENGINE_COST1"), x0, y3);
            int y4 = y3 + s17;
            g.drawString(text("SHIP_DESIGN_ENGINE_SIZE1"), x0, y4);
            int y5 = y4 + s17;
            g.drawString(text("SHIP_DESIGN_ENGINE_POWER1"), x0, y5);
            int y6 = y5 + s27 - scrunch;
            g.drawString(text("SHIP_DESIGN_POWER_REQUIREMENTS"), x0, y6);
            int y7 = y6 + s17;
            g.drawString(text("SHIP_DESIGN_ENGINES_REQUIRED"), x0, y7);
            int y8 = y7 + s27 - scrunch;
            g.drawString(text("SHIP_DESIGN_ENGINES_SIZE"), x0, y8);
            int y9 = y8 + s17;
            g.drawString(text("SHIP_DESIGN_ENGINES_COST"), x0, y9);

           if (UserPreferences.textures()) 
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
                engineFieldArea.setBounds(boxX, boxY, boxW, boxH);
                engineFieldDecr.reset();
                engineFieldDecr.addPoint(boxX - s11, boxY + (boxH / 2));
                engineFieldDecr.addPoint(boxX - s3, boxY);
                engineFieldDecr.addPoint(boxX - s3, boxY + boxH);
                engineFieldIncr.reset();
                engineFieldIncr.addPoint(boxX + boxW + s11, boxY + (boxH / 2));
                engineFieldIncr.addPoint(boxX + boxW + s3, boxY);
                engineFieldIncr.addPoint(boxX + boxW + s3, boxY + boxH);
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
            g.drawString(typeStr, x1a, y1);

            float engRequired = des.enginesRequired();
            float engSize = des.engine().size(des);
            float engPower = des.engine().powerOutput();
            float engCost =des.engine().cost(des);

            g.setColor(darkestBrown);
            g.setFont(narrowFont(16));
            String str = text("SHIP_DESIGN_SPEED_VALUE", (int)des.warpSpeed());
            sw = g.getFontMetrics().stringWidth(str);
            g.drawString(str, x3-sw, y2);
            str = ""+fmt(engCost,1);
            sw = g.getFontMetrics().stringWidth(str);
            g.drawString(str, x3-sw, y3);
            str = ""+fmt(engSize,1);
            sw = g.getFontMetrics().stringWidth(str);
            g.drawString(str, x3-sw, y4);
            str = ""+fmt(engPower,1);
            sw = g.getFontMetrics().stringWidth(str);
            g.drawString(str, x3-sw, y5);
            str = ""+fmt(engRequired*engPower,1);
            sw = g.getFontMetrics().stringWidth(str);
            g.drawString(str, x3-sw, y6);
            str = ""+ fmt(engRequired,1);
            sw = g.getFontMetrics().stringWidth(str);
            g.drawString(str, x3-sw, y7);
            str = ""+ (int) (engRequired*engSize);
            sw = g.getFontMetrics().stringWidth(str);
            g.drawString(str, x3-sw, y8);
            str = ""+ (int) (engRequired*engCost);
            sw = g.getFontMetrics().stringWidth(str);
            g.drawString(str, x3-sw, y9);
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
            //g.drawString(s2, x2+(w2-sw2)/2, y1);
            g.drawString(str, x2, y1);
            str = text("SHIP_DESIGN_TYPE_LABEL");
            int sw = g.getFontMetrics().stringWidth(str);
            g.drawString(str, x3+(w3-sw)/2, y1);
            str = text("SHIP_DESIGN_SIZE_LABEL");
            sw = g.getFontMetrics().stringWidth(str);
            g.drawString(str, x4+w4-sw, y1);
            str = text("SHIP_DESIGN_POWER_LABEL");
            sw = g.getFontMetrics().stringWidth(str);
            g.drawString(str, x5+w5-sw, y1);
            str = text("SHIP_DESIGN_COST_LABEL");
            sw = g.getFontMetrics().stringWidth(str);
            g.drawString(str, x6+w6-sw, y1);

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

            if (UserPreferences.textures()) 
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
                g.drawString(descLines.get(0), x2, y2);
            else if (descLines.size() > 1) {
                g.drawString(descLines.get(0), x2, y2-s5);
                g.drawString(descLines.get(1), x2, y2+s5);
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
                computerFieldArea.setBounds(boxX, boxY, boxW, boxH);
                computerFieldDecr.reset();
                computerFieldDecr.addPoint(boxX - s11, boxY + (boxH / 2));
                computerFieldDecr.addPoint(boxX - s3, boxY);
                computerFieldDecr.addPoint(boxX - s3, boxY + boxH);
                computerFieldIncr.reset();
                computerFieldIncr.addPoint(boxX + boxW + s11, boxY + (boxH / 2));
                computerFieldIncr.addPoint(boxX + boxW + s3, boxY);
                computerFieldIncr.addPoint(boxX + boxW + s3, boxY + boxH);
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
            g.drawString(compName, x3a, y2);

            g.setFont(narrowFont(17));
            g.setColor(darkestBrown);
            sw = g.getFontMetrics().stringWidth(compSize);
            g.drawString(compSize, x4+w4-sw, y2);

            sw = g.getFontMetrics().stringWidth(compPower);
            g.drawString(compPower, x5+w5-sw, y2);

            sw = g.getFontMetrics().stringWidth(compCost);
            g.drawString(compCost, x6+w6-sw, y2);

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
                g.drawString(descLines.get(0), x2, y3);
            }
            else if (descLines.size() > 1) {
                g.drawString(descLines.get(0), x2, y3-s5);
                g.drawString(descLines.get(1), x2, y3+s5);
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
                armorFieldArea.setBounds(boxX, boxY, boxW, boxH);
                armorFieldDecr.reset();
                armorFieldDecr.addPoint(boxX - s11, boxY + (boxH / 2));
                armorFieldDecr.addPoint(boxX - s3, boxY);
                armorFieldDecr.addPoint(boxX - s3, boxY + boxH);
                armorFieldIncr.reset();
                armorFieldIncr.addPoint(boxX + boxW + s11, boxY + (boxH / 2));
                armorFieldIncr.addPoint(boxX + boxW + s3, boxY);
                armorFieldIncr.addPoint(boxX + boxW + s3, boxY + boxH);
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
            g.drawString(armorName, x3a, y3);

            g.setFont(narrowFont(17));
            g.setColor(darkestBrown);
            sw = g.getFontMetrics().stringWidth(armorSize);
            g.drawString(armorSize, x4+w4-sw, y3);

            sw = g.getFontMetrics().stringWidth(armorPower);
            g.drawString(armorPower, x5+w5-sw, y3);

            sw = g.getFontMetrics().stringWidth(armorCost);
            g.drawString(armorCost, x6+w6-sw, y3);
         
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
                g.drawString(descLines.get(0), x2, y4);
            else if (descLines.size() > 1) {
                g.drawString(descLines.get(0), x2, y4-s5);
                g.drawString(descLines.get(1), x2, y4+s5);
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
                shieldsFieldArea.setBounds(boxX, boxY, boxW, boxH);
                shieldsFieldDecr.reset();
                shieldsFieldDecr.addPoint(boxX - s11, boxY + (boxH / 2));
                shieldsFieldDecr.addPoint(boxX - s3, boxY);
                shieldsFieldDecr.addPoint(boxX - s3, boxY + boxH);
                shieldsFieldIncr.reset();
                shieldsFieldIncr.addPoint(boxX + boxW + s11, boxY + (boxH / 2));
                shieldsFieldIncr.addPoint(boxX + boxW + s3, boxY);
                shieldsFieldIncr.addPoint(boxX + boxW + s3, boxY + boxH);
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
            g.drawString(shieldName, x3a, y4);

            g.setFont(narrowFont(17));
            g.setColor(darkestBrown);
            sw = g.getFontMetrics().stringWidth(shieldSize);
            g.drawString(shieldSize, x4+w4-sw, y4);

            sw = g.getFontMetrics().stringWidth(shieldPower);
            g.drawString(shieldPower, x5+w5-sw, y4);

            sw = g.getFontMetrics().stringWidth(shieldCost);
            g.drawString(shieldCost, x6+w6-sw, y4);
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
            //g.drawString(s2, x2+(w2-sw2)/2, y1);
            g.drawString(str, x2, y1);
            str = text("SHIP_DESIGN_TYPE_LABEL");
            int sw = g.getFontMetrics().stringWidth(str);
            g.drawString(str, x3+(w3-sw)/2, y1);
            str = text("SHIP_DESIGN_SIZE_LABEL");
            sw = g.getFontMetrics().stringWidth(str);
            g.drawString(str, x4+w4-sw, y1);
            str = text("SHIP_DESIGN_POWER_LABEL");
            sw = g.getFontMetrics().stringWidth(str);
            g.drawString(str, x5+w5-sw, y1);
            str = text("SHIP_DESIGN_COST_LABEL");
            sw = g.getFontMetrics().stringWidth(str);
            g.drawString(str, x6+w6-sw, y1);

            // draw ecm jammer row
            int rowH = (y+h-s10-y1)/3;
            int y2 = y1+rowH;
            String title1 = text("SHIP_DESIGN_ECM_TITLE");
            g.setFont(narrowFont(20));
            drawShadowedString(g, title1, 3, x1, y2, SystemPanel.textShadowC, SystemPanel.whiteText);

            // draw ship maneuver row
            int y3 = y2+rowH;
            String title2 = text("SHIP_DESIGN_MANEUVER_TITLE");
            g.setFont(narrowFont(20));
            drawShadowedString(g, title2, 3, x1, y3, SystemPanel.textShadowC, SystemPanel.whiteText);

            if (UserPreferences.textures()) 
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
                g.drawString(descLines.get(0), x2, y2);
            else if (descLines.size() > 1) {
                g.drawString(descLines.get(0), x2, y2-s5);
                g.drawString(descLines.get(1), x2, y2+s5);
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
                ecmFieldArea.setBounds(boxX, boxY, boxW, boxH);
                ecmFieldDecr.reset();
                ecmFieldDecr.addPoint(boxX - s11, boxY + (boxH / 2));
                ecmFieldDecr.addPoint(boxX - s3, boxY);
                ecmFieldDecr.addPoint(boxX - s3, boxY + boxH);
                ecmFieldIncr.reset();
                ecmFieldIncr.addPoint(boxX + boxW + s11, boxY + (boxH / 2));
                ecmFieldIncr.addPoint(boxX + boxW + s3, boxY);
                ecmFieldIncr.addPoint(boxX + boxW + s3, boxY + boxH);
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
            g.drawString(ecmName, x3a, y2);

            g.setFont(narrowFont(17));
            g.setColor(darkestBrown);
            sw = g.getFontMetrics().stringWidth(ecmSize);
            g.drawString(ecmSize, x4+w4-sw, y2);

            sw = g.getFontMetrics().stringWidth(ecmPower);
            g.drawString(ecmPower, x5+w5-sw, y2);

            sw = g.getFontMetrics().stringWidth(ecmCost);
            g.drawString(ecmCost, x6+w6-sw, y2);

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
                g.drawString(descLines.get(0), x2, y3);
            else if (descLines.size() > 1) {
                g.drawString(descLines.get(0), x2, y3-s5);
                g.drawString(descLines.get(1), x2, y3+s5);
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
                maneuverFieldArea.setBounds(boxX, boxY, boxW, boxH);
                maneuverFieldDecr.reset();
                maneuverFieldDecr.addPoint(boxX - s11, boxY + (boxH / 2));
                maneuverFieldDecr.addPoint(boxX - s3, boxY);
                maneuverFieldDecr.addPoint(boxX - s3, boxY + boxH);
                maneuverFieldIncr.reset();
                maneuverFieldIncr.addPoint(boxX + boxW + s11, boxY + (boxH / 2));
                maneuverFieldIncr.addPoint(boxX + boxW + s3, boxY);
                maneuverFieldIncr.addPoint(boxX + boxW + s3, boxY + boxH);
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
            g.drawString(manvName, x3a, y3);

            g.setFont(narrowFont(17));
            g.setColor(darkestBrown);
            sw = g.getFontMetrics().stringWidth(manvSize);
            g.drawString(manvSize, x4+w4-sw, y3);

            sw = g.getFontMetrics().stringWidth(manvPower);
            g.drawString(manvPower, x5+w5-sw, y3);

            sw = g.getFontMetrics().stringWidth(manvCost);
            g.drawString(manvCost, x6+w6-sw, y3);
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
            g.drawString(str, x2+(w2-sw)/2, y1);
            str = text("SHIP_DESIGN_COUNT_LABEL");
            sw = g.getFontMetrics().stringWidth(str);
            g.drawString(str, x3+(w3-sw)/2, y1);
            str = text("SHIP_DESIGN_DAMAGE_LABEL");
            sw = g.getFontMetrics().stringWidth(str);
            g.drawString(str, x4+(w4-sw)/2, y1);
            str = text("SHIP_DESIGN_RANGE_LABEL");
            sw = g.getFontMetrics().stringWidth(str);
            g.drawString(str, x5+(w5-sw)/2, y1);
            str = text("SHIP_DESIGN_SIZE_LABEL");
            sw = g.getFontMetrics().stringWidth(str);
            g.drawString(str, x6+w6-sw, y1);
            str = text("SHIP_DESIGN_POWER_LABEL");
            sw = g.getFontMetrics().stringWidth(str);
            g.drawString(str, x7+w7-sw, y1);
            str = text("SHIP_DESIGN_COST_LABEL");
            sw = g.getFontMetrics().stringWidth(str);
            g.drawString(str, x8+w8-sw, y1);
            str = text("SHIP_DESIGN_DESCRIPTION_LABEL");
            g.drawString(str, x9+s15, y1);

            // draw weapon row 1
            int rowH = (y+h-s10-y1)/4;
            int y2 = y1+rowH;
            String title1 = text("SHIP_DESIGN_WEAPON_TITLE");
            g.setFont(narrowFont(20));
            drawShadowedString(g, title1, 3, x1, y2, SystemPanel.textShadowC, SystemPanel.whiteText);

           if (UserPreferences.textures()) 
                drawTexture(g,x, y+s25, w0, h-s25);

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
                    weaponFieldDecr[i].addPoint(boxX - s11, boxY + (boxH / 2));
                    weaponFieldDecr[i].addPoint(boxX - s3, boxY);
                    weaponFieldDecr[i].addPoint(boxX - s3, boxY + boxH);
                    weaponFieldIncr[i].reset();
                    weaponFieldIncr[i].addPoint(boxX + boxW + s11, boxY + (boxH / 2));
                    weaponFieldIncr[i].addPoint(boxX + boxW + s3, boxY);
                    weaponFieldIncr[i].addPoint(boxX + boxW + s3, boxY + boxH);
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
                g.drawString(wpnName, x2a, y2);


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
                g.drawString(wpnCount, x3a, y2);

                g.setFont(narrowFont(17));
                g.setColor(darkestBrown);
                sw = g.getFontMetrics().stringWidth(wpnDmg);
                g.drawString(wpnDmg, x4+((w4-sw)/2), y2);
                sw = g.getFontMetrics().stringWidth(wpnRange);
                g.drawString(wpnRange, x5+((w5-sw)/2), y2);
                sw = g.getFontMetrics().stringWidth(wpnSize);
                g.drawString(wpnSize, x6+w6-sw, y2);
                sw = g.getFontMetrics().stringWidth(wpnPower);
                g.drawString(wpnPower, x7+w7-sw, y2);
                sw = g.getFontMetrics().stringWidth(wpnCost);
                g.drawString(wpnCost, x8+w8-sw, y2);
                g.drawString(wpnDesc, x9+s15, y2);
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
            g.drawString(str, x2+(w2-sw)/2, y1);
            str = text("SHIP_DESIGN_SIZE_LABEL");
            sw = g.getFontMetrics().stringWidth(str);
            g.drawString(str, x3+w3-sw, y1);
            str = text("SHIP_DESIGN_POWER_LABEL");
            sw = g.getFontMetrics().stringWidth(str);
            g.drawString(str, x4+w4-sw, y1);
            str = text("SHIP_DESIGN_COST_LABEL");
            sw = g.getFontMetrics().stringWidth(str);
            g.drawString(str, x5+w5-sw, y1);
            str = text("SHIP_DESIGN_DESCRIPTION_LABEL");
            g.drawString(str, x6+s15, y1);

            // draw special 1
            int rowH = (y+h-s10-y1)/3;
            int y2 = y1+rowH;
            String title1 = text("SHIP_DESIGN_SPECIAL_TITLE");
            g.setFont(narrowFont(20));
            drawShadowedString(g, title1, 3, x1, y2, SystemPanel.textShadowC, SystemPanel.whiteText);

           if (UserPreferences.textures()) 
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
                    specialsFieldArea[i].setBounds(boxX, boxY, boxW, boxH);
                    specialsFieldDecr[i].reset();
                    specialsFieldDecr[i].addPoint(boxX - s11, boxY + (boxH / 2));
                    specialsFieldDecr[i].addPoint(boxX - s3, boxY);
                    specialsFieldDecr[i].addPoint(boxX - s3, boxY + boxH);
                    specialsFieldIncr[i].reset();
                    specialsFieldIncr[i].addPoint(boxX + boxW + s11, boxY + (boxH / 2));
                    specialsFieldIncr[i].addPoint(boxX + boxW + s3, boxY);
                    specialsFieldIncr[i].addPoint(boxX + boxW + s3, boxY + boxH);
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
                g.drawString(wpnName, x2a, y2);

                g.setFont(narrowFont(17));
                g.setColor(darkestBrown);
                sw = g.getFontMetrics().stringWidth(wpnSize);
                g.drawString(wpnSize, x3 + w3 - sw, y2);
                sw = g.getFontMetrics().stringWidth(wpnPower);
                g.drawString(wpnPower, x4 + w4 - sw, y2);
                sw = g.getFontMetrics().stringWidth(wpnCost);
                g.drawString(wpnCost, x5 + w5 - sw, y2);
                g.drawString(wpnDesc, x6 + s15, y2);
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
            List<ShipEngine> engines = player().shipLab().engines();
            int index = engines.indexOf(des.engine());
            if (index > 0) {
                des.engine(engines.get(index - 1));
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
            List<ShipManeuver> maneuvers = player().shipLab().maneuvers();
            int index = maneuvers.indexOf(des.maneuver());
            if (index > 0) {
                des.maneuver(maneuvers.get(index - 1));
                repaint();
            }
        }
        private void shipManeuverIncrement() {
            ShipDesign des =  shipDesign();
            List<ShipManeuver> maneuvers = player().shipLab().maneuvers();
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
        private void shipWeaponCountDecrement(int i) {
            ShipDesign des =  shipDesign();
            if ((des.wpnCount(i) > 0) && !des.weapon(i).isNone()) {
                des.wpnCount(i, des.wpnCount(i) - 1);
                repaint();
            }
        }
        private void shipWeaponCountIncrement(int i) {
            ShipDesign des =  shipDesign();
            if (!des.weapon(i).isNone()) {
                des.wpnCount(i, des.wpnCount(i) + 1);
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
            
            if (shipDesign().active())
                return;
            
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
            if (prevHover != hoverTarget)
                repaint();
        }
        @Override
        public void mouseClicked(MouseEvent mouseEvent) { }
        @Override
        public void mousePressed(MouseEvent mouseEvent) { }
        @Override
        public void mouseReleased(MouseEvent mouseEvent) {
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
                    softClick(); shipWeaponCountDecrement(i); return;
                }
                if (hoverTarget == weaponCountIncr[i]) {
                    softClick(); shipWeaponCountIncrement(i); return;
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
                    if (count > 0)
                        shipWeaponCountDecrement(i);
                    else
                        shipWeaponCountIncrement(i);
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