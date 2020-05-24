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
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.Border;
import rotp.model.empires.Empire;
import rotp.model.galaxy.StarSystem;
import rotp.model.planet.Planet;
import rotp.ui.BasePanel;
import rotp.ui.SystemViewer;
import rotp.ui.map.IMapHandler;
import rotp.util.ThickBevelBorder;

public abstract class SystemPanel extends BasePanel implements SystemViewer, MapSpriteViewer {
    private static final long serialVersionUID = 1L;
    public static final Color labelBorderLo = new Color(62,62,62);
    public static final Color labelBorderHi = new Color(175,175,175);
    public static final Color dataBorders = new Color(160,160,160);

    public static final Color yellowText = new Color(251,251,127);
    public static final Color orangeText = new Color(255,192,0);
    public static final Color darkOrangeText = new Color(160,96,0);
    public static final Color orangeClear = new Color(255,192,0,0);
    public static final Color blueText = new Color(34,140,142);
    public static final Color redText = new Color(153,0,11);
    public static final Color greenText = new Color(120, 147, 46);
    public static final Color limeText = new Color(176,242,90);
    public static final Color whiteText = new Color(210,210,210);
    public static final Color whiteLabelText = new Color(240,240,240);
    public static final Color grayText = new Color(160,160,160);
    public static final Color blackText = new Color(20,20,20);
    public static final Color starBackgroundC = new Color(0,0,30);
    public static final Color textShadowC = new Color(30,30,30,30);
    public static final Color buttonShadowC = new Color(30,30,30,90);

    public static final Color outOfRangeC = new Color(92,20,20);
    public static final Color scoutRangeC = new Color(173,125,72);
    public static final Color inRangeC = new Color(78,105,54);

    static Color buttonC1 = new Color(148,148,148);
    static Color buttonC2 = new Color(189,189,189);
    static Color buttonC3 = new Color(84,84,84);
    static Color buttonC4 = new Color(63,63,63);

    private static Border buttonBorder;
    private static Border topPaneBorderUncolonized;
    private static Border topPaneBorderHostile;
    private static Border topPaneBorderNeutral;
    private static Border topPaneBorderFriendly;
    private static Border topPaneBorderEmpire;
    private static Border thinBorder;

    public static Color rangeTooFar = redText;
    public static Color rangeExtendedOnly = orangeText;
    public static Color rangeGood = greenText;

    protected SpriteDisplayPanel parentSpritePanel;
    protected BasePanel overviewPane;
    protected BasePanel detailPane;
    protected BasePanel bottomPane;

    static final String EMPIRE_DETAIL = "EMPIRE_DETAIL";
    static final String STAR_DETAIL = "STAR_DETAIL";
    static final String PLANET_DETAIL = "PLANET_DETAIL";

    protected CardLayout detailLayout = new CardLayout();
    JPanel detailCardPane;

    protected SpriteDisplayPanel spritePanel()   { return parentSpritePanel; }

    protected void showDefaultDetail() { }
    protected void showStarDetail()    { }
    protected void showPlanetDetail()    { }

    @Override
    public StarSystem systemViewToDisplay() {
        return spritePanel().systemViewToDisplay();
    }
    public Border buttonBorder() {
        if (buttonBorder == null)
            buttonBorder = new ThickBevelBorder(4,1, buttonC1, buttonC2, buttonC1, buttonC1, buttonC3, buttonC4, buttonC3, buttonC4);
        return buttonBorder;
    }
    public Border topPaneBorderUncolonized() {
        if (topPaneBorderUncolonized == null)
            createPaneBorders();
        return topPaneBorderUncolonized;
    }
    public Border topPaneBorderNeutral() {
        if (topPaneBorderNeutral == null)
            createPaneBorders();
        return topPaneBorderNeutral;
    }
    public Border topPaneBorderHostile() {
        if (topPaneBorderHostile == null)
            createPaneBorders();
        return topPaneBorderHostile;
    }
    public Border topPaneBorderFriendly() {
        if (topPaneBorderFriendly == null)
            createPaneBorders();
        return topPaneBorderFriendly;
    }
    public Border topPaneBorderEmpire() {
        if (topPaneBorderEmpire == null)
            createPaneBorders();
        return topPaneBorderEmpire;
    }
    public Border thinBorder() {
        if (thinBorder == null)
            createPaneBorders();
        return thinBorder;
    }
    private void createPaneBorders() {
        Border line0 = newLineBorder(dataBorders,2);
        Border line1 = newLineBorder(newColor(32,32,32),3);
        Border lineYellow = newLineBorder(newColor(224,224,0),3);
        Border lineRed = newLineBorder(newColor(192,0,0),3);
        Border lineGreen = newLineBorder(newColor(0,192,0),3);
        Border lineBlue = newLineBorder(newColor(0,0,192),3);
        Border lineGray = newLineBorder(newColor(224,224,224),3);

        Border compound0 = BorderFactory.createCompoundBorder(line0, line1);
        topPaneBorderUncolonized = BorderFactory.createCompoundBorder(compound0, lineGray);
        topPaneBorderNeutral = BorderFactory.createCompoundBorder(compound0, lineYellow);
        topPaneBorderHostile = BorderFactory.createCompoundBorder(compound0, lineRed);
        topPaneBorderFriendly = BorderFactory.createCompoundBorder(compound0, lineGreen);
        topPaneBorderEmpire = BorderFactory.createCompoundBorder(compound0, lineBlue);

        thinBorder = newLineBorder(dataBorders,2);
    }
    protected Color backgroundColor()   { return MainUI.shadeBorderC(); }
    protected void initModel() {
        initModel(s5);
    }
    public void initModel(int gap) {
        setBackground(backgroundColor());

        overviewPane = topPane();
        detailPane = detailPane();
        bottomPane = bottomPane();

        setLayout(new BorderLayout(0,gap));
        if (overviewPane != null) 
            add(overviewPane, BorderLayout.NORTH);
        
        add(detailPane, BorderLayout.CENTER);
        if (bottomPane != null) 
            add(bottomPane, BorderLayout.SOUTH); 
    }
    protected abstract BasePanel topPane();
    protected abstract BasePanel detailPane();
    protected BasePanel bottomPane()  { return null; }

    public void recenterMap() {
        spritePanel().parent.map().recenterMapOn(systemViewToDisplay());
    }
    public void scrollToNextSystem(boolean forward) {
       StarSystem sys = systemViewToDisplay();
        if (sys == null)
            return;
        Empire emp = player().sv.empire(sys.id);
        if (emp == null)
            return;
        List<StarSystem> systems = emp.orderedColonies();
        
        int index = systems.indexOf(sys);
        if (forward) 
            index = (index == (systems.size()-1)) ? 0 : index + 1;
        else 
            index = (index == 0) ? systems.size()-1 : index -1;

        IMapHandler topPanel = spritePanel().parent;;
        topPanel.clickedSprite(systems.get(index));
        topPanel.map().recenterMapOn(systems.get(index));
        topPanel.repaint();
    }
    public void drawPlanetInfo(Graphics2D g, StarSystem sys, boolean showSpyData, boolean showPopulation, int startY, int w, int h) {
        int y = startY;

        int lines = showSpyData ? 5 : 4;
        int lineH = h/lines;

        // ensure we are always at least font 16, readjust lineH to compensate
        int estFontSize = min(20, max(16, unscaled(lineH)));
        lineH = scaled(estFontSize);

        Font f = narrowFont(estFontSize);
        y = h-lineH-s3;

        if (showSpyData) {
            drawSystemReportAge(g, sys, f,  y, w, lineH);
            y -= lineH;
        }

        if (showPopulation)
            drawSystemPopulation(g, sys, f, y, w, lineH);
        else
            drawSystemSize(g, sys, f, y, w, lineH);
        y -= lineH;

        if (drawResearchProject(g, sys, f, y, w, lineH))
            y -= lineH;

        if (drawSystemResources(g, sys, f, y, w, lineH))
            y -= lineH;

        if (drawSystemEnvironment(g, sys, f, y, w, lineH))
            y -= lineH;

        drawSystemPlanetType(g, sys, f, y, w, lineH);
        y -= lineH;

        if (player().sv.isColonized(sys.id))
            drawSystemTreatyStatus(g, sys, f, y, w, lineH);
    }
    public void drawSystemTreatyStatus(Graphics2D g, StarSystem sys, Font textF, int y, int w, int h) {
        int id = sys.id;
        Empire pl = player();
        int empId = pl.sv.empId(id);
        if (empId == pl.id)
            return;

        g.setFont(textF);

        if (pl.alliedWith(empId)) {
            g.setColor(SystemPanel.greenText);
            String str4 = text("MAIN_FLEET_ALLY");
            int sw4 = g.getFontMetrics().stringWidth(str4);
            g.drawString(str4, w-sw4-s10, y+h);
        } else if (pl.atWarWith(empId)) {
            g.setColor(SystemPanel.redText);
            String str4 = text("MAIN_FLEET_ENEMY");
            int sw4 = g.getFontMetrics().stringWidth(str4);
            g.drawString(str4, w-sw4-s10, y+h);
        }
    }
    public void drawSystemName(Graphics2D g2, StarSystem sys, int y, int w, int h) {
        g2.setFont(narrowFont(unscaled(h)));
        int h0 = g2.getFontMetrics().getHeight();
        drawBorderedString(g2, player().sv.name(sys.id), 2, s15, h0-s10, Color.black, Color.white);
    }
    public void drawSystemPlanetType(Graphics2D g2, StarSystem sys, Font textF, int y, int w, int h) {
        Planet planet = sys.planet();
        int rightMargin = s10;

        g2.setFont(textF);
        if (player().canColonize(planet))
            g2.setColor(greenText);
        else if (player().isLearningToColonize(planet))
            g2.setColor(yellowText);
        else if (player().canLearnToColonize(planet))
            g2.setColor(orangeText);
        else
            g2.setColor(redText);

        if (planet.type().isAsteroids())
            return;

        String typeStr = text(planet.type().key());
        int sw = g2.getFontMetrics().stringWidth(typeStr);

        int x0 = w-rightMargin-sw;
        g2.drawString(typeStr, x0, y+h);
    }
    public boolean drawSystemEnvironment(Graphics2D g2, StarSystem sys, Font textF, int y, int w, int h) {
        int rightMargin = s10;
        Empire pl = player();

        g2.setFont(textF);
        String envStr = "";
        if (pl.isEnvironmentHostile(sys)) {
            g2.setColor(redText);
            envStr = text("PLANET_HOSTILE");
        }
        else if (pl.isEnvironmentFertile(sys)) {
            g2.setColor(greenText);
            envStr = text("PLANET_FERTILE");
        }
        else if (pl.isEnvironmentGaia(sys)) {
            g2.setColor(greenText);
            envStr = text("PLANET_GAIA");
        }

        if (!envStr.isEmpty()) {
            int sw = g2.getFontMetrics().stringWidth(envStr);
            int x0 = w-rightMargin-sw;
            g2.drawString(envStr, x0, y+h);
            return true;
        }
        return false;
    }
    public boolean drawResearchProject(Graphics2D g2, StarSystem sys, Font textF, int y, int w, int h) {
        int rightMargin = s10;

        if (sys.hasEvent()) {
            g2.setFont(textF);
            String projStr = text(sys.eventKey());
            g2.setColor(redText);
            int sw = g2.getFontMetrics().stringWidth(projStr);
            int x0 = w-rightMargin-sw;
            g2.drawString(projStr, x0, y+h);
            return true;
        }
        return false;
    }
    public boolean drawSystemResources(Graphics2D g2, StarSystem sys, Font textF, int y, int w, int h) {
        int rightMargin = s10;
        Empire pl = player();

        g2.setFont(textF);
        String resourceStr = "";
        g2.setColor(blueText);
        if (pl.sv.isArtifact(sys.id)) {
            g2.setColor(orangeText);
            resourceStr = text("PLANET_ARTIFACTS");
        }
        else if (pl.sv.isUltraPoor(sys.id))
            resourceStr = text("PLANET_ULTRA_POOR");
        else if (pl.sv.isPoor(sys.id))
            resourceStr = text("PLANET_POOR");
        else if (pl.sv.isUltraRich(sys.id))
            resourceStr = text("PLANET_ULTRA_RICH");
        else if (pl.sv.isRich(sys.id))
            resourceStr = text("PLANET_RICH");

        if (!resourceStr.isEmpty()) {
            int sw = g2.getFontMetrics().stringWidth(resourceStr);
            int x0 = w-rightMargin-sw;
            g2.drawString(resourceStr, x0, y+h);
            return true;
        }
        return false;
    }
    public void drawSystemPopulation(Graphics2D g2, StarSystem sys, Font textF, int y, int w, int h) {
        int id = sys.id;
        Empire pl = player();

        Planet planet = sys.planet();
        int rightMargin = s10;

        if (planet.maxSize() > 0) {
            g2.setFont(textF);
            String popStr;
            boolean ignoreWaste = planet.isColonized() && planet.empire().race().ignoresPlanetEnvironment();
            int planetSize = (int) pl.sv.currentSize(sys.id);
            int population = (int) pl.sv.population(sys.id);
            g2.setColor(greenText);
            if (pl.sv.isColonized(id) && pl.sv.colony(id).inRebellion()) {
                popStr = text("MAIN_PLANET_REBELLION");
                g2.setColor(redText);
            }
            else if (planetSize == population)
                popStr = text("MAIN_PLANET_POP", population);
            else
                popStr = text("MAIN_PLANET_POP_SIZE", population, planetSize);

            int sw = g2.getFontMetrics().stringWidth(popStr);

            int x0 = w-rightMargin-sw;
            g2.drawString(popStr, x0, y+h);
        }
    }
    public void drawSystemSize(Graphics2D g2, StarSystem sys, Font textF, int y, int w, int h) {
        int id = sys.id;
        Empire pl = player();

        Planet planet = sys.planet();
        int rightMargin = s10;

        if (planet.maxSize() > 0) {
            g2.setFont(textF);
            String popStr;
            boolean ignoreWaste = planet.isColonized() && planet.empire().race().ignoresPlanetEnvironment();
            
            int planetSize = 0;
            if (planet.empire() != pl)
                planetSize = pl.sv.currentSize(id);
            else 
                planetSize = ignoreWaste ? (int) planet.currentSize() : (int) planet.sizeAfterWaste();
            if (ignoreWaste || (planet.waste() == 0))
                g2.setColor(greenText);
            else
                g2.setColor(redText);
            if (pl.sv.isColonized(id) && pl.sv.colony(id).inRebellion()) {
                popStr = text("MAIN_PLANET_REBELLION");
                g2.setColor(redText);
            }
            else if (planet.currentSize() == planet.maxSize())
                popStr = text("MAIN_PLANET_SIZE", planetSize);
            else if (pl.sv.isColonized(id) && pl.sv.empire(id).isAI())
                popStr = text("MAIN_PLANET_SIZE", planetSize);
            else
                popStr = text("MAIN_PLANET_SIZE+", planetSize);

            int sw = g2.getFontMetrics().stringWidth(popStr);

            int x0 = w-rightMargin-sw;
            g2.drawString(popStr, x0, y+h);
        }
    }
    public void drawSystemReportAge(Graphics2D g2, StarSystem sys, Font textF, int y, int w, int h) {
        int rightMargin = s10;

        int age = player().sv.spyReportAge(sys.id);

        if (age > 0) {
            g2.setFont(textF);
            g2.setColor(Color.gray);
            String title = text("RACES_REPORT_AGE", age);
            int sw = g2.getFontMetrics().stringWidth(title);
            int x0 = w-rightMargin-sw;
            g2.drawString(title, x0, y+h);
        }
    }
    protected class SystemRangePane extends BasePanel {
        private static final long serialVersionUID = 1L;
        SystemPanel parent;
        private Shape textureClip;
        public SystemRangePane(SystemPanel p) {
            parent = p;
            init();
        }
        private void init() {
            setOpaque(false);
            setPreferredSize(new Dimension(getWidth(),s40));
        }
        @Override
        public String textureName()            { return TEXTURE_GRAY; }
        @Override
        public Shape textureClip()    { return textureClip; }
        @Override
        public void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            super.paintComponent(g);

            Empire pl = player();

            int w = getWidth();
            int h = getHeight();
            StarSystem sys = parent.parentSpritePanel.systemViewToDisplay();
            if (sys == null)
                return;
            int range = (int) Math.ceil(pl.sv.distance(sys.id));
            
            String label = pl.alliedWith(id(sys.empire())) ?  text("MAIN_ALLIED_COLONY") : text("MAIN_SYSTEM_RANGE", range);
            String desc;
            Color c0;
            if (pl.sv.inShipRange(sys.id)) {
                c0 = inRangeC;
                desc = text("MAIN_IN_RANGE_DESC");
            }
            else if (pl.sv.inScoutRange(sys.id)) {
                c0 = scoutRangeC;
                desc = text("MAIN_SCOUT_RANGE_DESC");
            }
            else {
                c0 = outOfRangeC;
                desc = text("MAIN_OUT_OF_RANGE_DESC");
            }

            int y0 = s5;
            g.setColor(MainUI.shadeBorderC());
            g.fillRect(0, 0, w, y0);
            g.setColor(c0);
            g.fillRect(0, y0, w, h-y0);

            g.setFont(narrowFont(20));
            int sw = g.getFontMetrics().stringWidth(label);
            drawBorderedString(g, label, s10, h-s13, Color.black, whiteText);

            int x2 = s10+sw+s5;
            int w2 = w-s5-x2;
            g.setFont(narrowFont(15));
            List<String> descLines = wrappedLines(g, desc, w2);
            if (descLines.size() == 1) {
                int sw2 = g.getFontMetrics().stringWidth(desc);
                int x2a = x2+((w2-sw2)/2);
                drawBorderedString(g, desc, x2a, h-s13, Color.black, grayText);
            }
            else if (descLines.size() == 2) {
                String line = descLines.get(0);
                int sw2 = g.getFontMetrics().stringWidth(line);
                int x2a = x2+((w2-sw2)/2);
                drawBorderedString(g, line, x2a, h-s20, Color.black, grayText);
                line = descLines.get(1);
                sw2 = g.getFontMetrics().stringWidth(line);
                x2a = x2+((w2-sw2)/2);
                drawBorderedString(g, line, x2a, h-s6, Color.black, grayText);
            }

            g.setColor(Color.black);
            Stroke prev = g.getStroke();
            g.setStroke(stroke1);
            g.drawRect(0, y0, w-s1, h-y0-s1);
            g.setStroke(prev);
            textureClip = new Rectangle2D.Float(0, y0, w-s1, h-y0-s1);
        }
    }
}
