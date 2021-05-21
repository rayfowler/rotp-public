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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import rotp.model.Sprite;
import rotp.model.galaxy.StarSystem;
import rotp.ui.BasePanel;
import rotp.ui.sprites.ShipRelocationSprite;

public class RallyPointPanel extends SystemPanel {
    private static final long serialVersionUID = 1L;
    BasePanel topPane;
    public RallyPointPanel(SpriteDisplayPanel p) {
        parentSpritePanel = p;
        init();
    }
    private void init() {
        initModel();
    }
    @Override
    public void animate() {
        topPane.animate();
        detailPane.animate();
    }
    @Override
    public boolean canEscape()                      { return true; }
    @Override
    public boolean useHoveringSprite(Sprite o) {
        if (!(parentSpritePanel.spriteToDisplay() instanceof ShipRelocationSprite))
            return false;

        ShipRelocationSprite sprite = (ShipRelocationSprite) parentSpritePanel.spriteToDisplay();
        if (o instanceof StarSystem)
            sprite.hoveringDest((StarSystem) o);
        else
            sprite.hoveringDest(null);

        return true;
    }
    @Override
    public boolean useNullClick(int cnt, boolean right) {
        if (right) {
            cancelRelocation();
            return true;
        }
        return false;
    }
    @Override
    public boolean useClickedSprite(Sprite o, int count, boolean rightClick) {
        ShipRelocationSprite sprite = (ShipRelocationSprite) parentSpritePanel.spriteToDisplay();
        if (o instanceof StarSystem)
            sprite.clickedDest((StarSystem) o);

        if (canRelocateShips())
            createRelocationPath();

        return true;
    }
    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        switch (k) {
            case KeyEvent.VK_ESCAPE:
                cancelRelocation();
                return;
            case KeyEvent.VK_SPACE:
            case KeyEvent.VK_ENTER:
                if (isCancellingRally())
                    cancelRelocationPath();
                else if (canRelocateShips())
                    createRelocationPath();
                return;
            case KeyEvent.VK_TAB:
                // tab-targeting for ship relocation
                StarSystem currSys = relocationSprite().starSystem();
                List<StarSystem> systems = player().orderedColonies();
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
                parentSpritePanel.repaint();
        }
    }
    @Override
    public StarSystem systemViewToDisplay() {
        return system();
    }
    private ShipRelocationSprite relocationSprite() {
        Sprite sprite = parentSpritePanel.spriteToDisplay();
        if (sprite instanceof ShipRelocationSprite)
            return (ShipRelocationSprite) parentSpritePanel.spriteToDisplay();
        return null;
    }
    private StarSystem system() {
        return relocationSprite() == null ? null : relocationSprite().homeSystemView();
    }
    private StarSystem destination() {
        return relocationSprite() == null ? null : relocationSprite().starSystem();
    }
    private boolean canRelocateShips() {
        return (destination() != null) && player().canRallyFleetsTo(id(destination()));
    }
    private boolean isStartingRally() {
        StarSystem sys = system();
        return (destination() != null) && (destination() != player().sv.rallySystem(sys.id));
    }
    private boolean isCancellingRally() {
        StarSystem sys = system();
        return (destination() != null)
            && ((sys == destination())
            || (destination() == player().sv.rallySystem(sys.id)));
    }
    public void cancelRelocation() {
        StarSystem sys = relocationSprite().from();
        relocationSprite().clear();
        parentSpritePanel.parent.clickedSprite(sys);
        parentSpritePanel.parent.repaint();
    }
    public void createRelocationPath() {
        if (!canRelocateShips())
            return;

        ShipRelocationSprite spr = relocationSprite();
        StarSystem sys = spr.homeSystemView();
        player().sv.rallySystem(sys.id, spr.starSystem());
        parentSpritePanel.parent.clickedSprite(sys);
        parentSpritePanel.parent.repaint();
    }
    public void cancelRelocationPath() {
        StarSystem sys = relocationSprite().homeSystemView();
        player().sv.stopRally(sys.id);
        parentSpritePanel.parent.clickedSprite(sys);
        parentSpritePanel.parent.repaint();
    }
    @Override
    protected BasePanel topPane() {
        if (topPane == null)
            topPane = new SystemViewInfoPane(this);
        return topPane;
    }
    @Override
    protected BasePanel detailPane() {
        if (detailPane == null)
            detailPane = new RallyPointDetailPane(parentSpritePanel);
        return detailPane;
    }
    @Override
    protected BasePanel bottomPane() {
        if (bottomPane == null)
            bottomPane = new RallyPointButtonPane(this);
        bottomPane.setBackground(MainUI.shadeBorderC());
        return bottomPane;
    }
    class RallyPointDetailPane extends SystemPanel {
        private static final long serialVersionUID = 1L;
        public RallyPointDetailPane(SpriteDisplayPanel p) {
            parentSpritePanel = p;
            initModel();
        }
        @Override
        public void animate() {
            detailPane.animate();
        }
        @Override
        public StarSystem systemViewToDisplay() {
            return destination();
        }
        @Override
        protected BasePanel topPane() { return new FromSystemDetailPane(); }
        @Override
        protected BasePanel detailPane() { return new SystemViewInfoPane(this); }
        @Override
        protected BasePanel bottomPane() { return new ToSystemDetailPane(); }
    }
    class FromSystemDetailPane extends BasePanel  implements MouseListener, MouseMotionListener {
        private static final long serialVersionUID = 1L;
        private Shape hoverBox;
        private final Rectangle retreatBox = new Rectangle();
        Shape arrow;
        Shape textureClip;
        public FromSystemDetailPane() {
            initModel();
        }
        private void initModel() {
            setPreferredSize(new Dimension(getWidth(),scaled(175)));
            setBackground(Color.green);
            addMouseListener(this);
            addMouseMotionListener(this);
        }
        @Override
        public String textureName()            { return TEXTURE_GRAY; }
        @Override
        public Shape textureClip()          { return textureClip; }
        @Override
        public boolean hasStarBackground()  { return true; }
        @Override
        public Color starBackgroundC()      { return MainUI.shadeBorderC(); }
        @Override
        public void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            super.paintComponent(g);
            int w = getWidth();
            g.setColor(MainUI.darkShadowC);
            g.fill(arrow());
            g.setColor(MainUI.paneBackground());
            g.fillRect(0, s5, w, s100+s5);

            textureClip = new Rectangle2D.Float(0,s5,w,s100+s5);

            int leftM = s5;
            String title = text("MAIN_RALLY_TITLE");
            g.setFont(narrowFont(24));
            drawShadowedString(g, title, 3, leftM, s30, MainUI.shadeBorderC(), SystemPanel.whiteLabelText);

            String prompt = text("MAIN_RALLY_PROMPT");
            g.setFont(narrowFont(18));

            int y0 = s40;
            g.setColor(MainUI.darkShadowC);
            List<String> lines = wrappedLines(g, prompt, getWidth()-leftM-s20);
            for (String line: lines) {
                y0 += s18;
                drawString(g,line, leftM, y0);
            }
            
            int checkW = s12;
            int checkX = leftM;
            y0 = y0+s25;
            retreatBox.setBounds(checkX, y0-checkW, checkW, checkW);
            Stroke prev = g.getStroke();
            g.setStroke(stroke2);
            g.setColor(MainUI.shadeBorderC());
            g.fill(retreatBox);
            if (hoverBox == retreatBox) {
                g.setColor(Color.yellow);
                g.draw(retreatBox);
            }
            if (relocationSprite().forwardRallies()) {
                g.setColor(SystemPanel.whiteText);
                g.drawLine(checkX-s1, y0-s6, checkX+s3, y0-s3);
                g.drawLine(checkX+s3, y0-s3, checkX+checkW, y0-s12);
            }
            g.setStroke(prev);
            String forward = text("MAIN_RALLY_FORWARD");
            g.setFont(narrowFont(16));
            g.setColor(MainUI.darkShadowC);
            drawString(g,forward, leftM+s15, y0-s1);
        }
        private Shape arrow() {
            if (arrow == null) {
                int w  = getWidth();
                int h = getHeight();
                int[] x = new int[9];
                int[] y = new int[9];
                int x0=(w/2)-s35; int x1=x0+s25; int x2=x1+s20; int x3=x2+s25;
                int y0=0; int y1=h-s45; int y2=h-s65; int y3=h-s40; int y4=h-s5;
                x[0]=x1;x[1]=x1;x[2]=x0;x[3]=x0;x[4]=w/2;x[5]=x3;x[6]=x3;x[7]=x2;x[8]=x2;
                y[0]=y0;y[1]=y1;y[2]=y2;y[3]=y3;y[4]=y4;y[5]=y3;y[6]=y2;y[7]=y1;y[8]=y0;
                arrow = new Polygon(x,y,x.length);
            }
            return arrow;
        }
        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getButton() > 3)
                return;
            int x = e.getX();
            int y = e.getY();
             if (retreatBox.contains(x,y)) {
                relocationSprite().toggleForwardRallies();
                softClick();
                repaint();
                return;
            }
        }
        @Override
        public void mousePressed(MouseEvent e) { }
        @Override
        public void mouseClicked(MouseEvent e) { }
        @Override
        public void mouseEntered(MouseEvent e) { }
        @Override
        public void mouseExited(MouseEvent e) {
            if (hoverBox != null){
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
            Shape prevHover = hoverBox;
            hoverBox = null;
            
            if (retreatBox.contains(x,y)) 
                hoverBox = retreatBox;

            if (hoverBox != prevHover)
                repaint();
        }
    }
    class ToSystemDetailPane extends BasePanel {
        private static final long serialVersionUID = 1L;
        public ToSystemDetailPane() {
            initModel();
        }
        private void initModel() {
            setPreferredSize(new Dimension(getWidth(),s80));
            setBackground(MainUI.paneBackground);
        }
        @Override
        public String textureName()            { return TEXTURE_GRAY; }
        @Override
        public void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            super.paintComponent(g);
            int w = getWidth();
            int leftM = s5;

            g.setFont(narrowFont(24));
            if (destination() == null) {
                String name = text("MAIN_RALLY_NO_SELECTION");
                drawShadowedString(g, name, 2, leftM, s22, MainUI.shadeBorderC(), SystemPanel.whiteLabelText);
            }
            else {
                int destId = destination().id;
                if (player().sv.isColonized(destId)) {
                    GradientPaint back = new GradientPaint(0,0,player().sv.empire(destId).color(),w, 0,MainUI.transC);
                    g.setPaint(back);
                    g.fillRect(0, 0, w, s29);
                    g.setPaint(null);
                }
                String name = player().sv.descriptiveName(destId);
                drawShadowedString(g, name, 2, leftM, s22, MainUI.shadeBorderC(), SystemPanel.whiteLabelText);
            }
            g.setFont(narrowFont(18));
            int y0 = s32;
            if (!canRelocateShips()) {
                String prompt = text("MAIN_RALLY_ERROR");
                g.setColor(SystemPanel.redText);
                List<String> lines = wrappedLines(g, prompt, getWidth()-leftM-s20);
                for (String line: lines) {
                    y0 += s18;
                    drawString(g,line, leftM, y0);
                }
                return;
            }

            if (system() == destination()) {
                String prompt = text("MAIN_RALLY_WILL_CANCEL");
                g.setColor(MainUI.darkShadowC);
                List<String> lines = wrappedLines(g, prompt, getWidth()-leftM-s20);
                for (String line: lines) {
                    y0 += s18;
                    drawString(g,line, leftM, y0);
                }
                return;
            }

            if (!system().colony().isBuildingShip())
                return;

            int turns = (int) Math.ceil(system().rallyTimeTo(destination()));
            String destName = player().sv.name(destination().id);

            String prompt = turns == 0 ? text("MAIN_RALLY_INSTANT_TIME", destName) : text("MAIN_RALLY_TRAVEL_TIME", destName, turns);
            g.setColor(MainUI.darkShadowC);
            List<String> lines = wrappedLines(g, prompt, getWidth()-leftM-s20);
            for (String line: lines) {
                y0 += s18;
                drawString(g,line, leftM, y0);
            }
        }
    }
    class RallyPointButtonPane extends BasePanel implements MouseListener, MouseMotionListener {
        private static final long serialVersionUID = 1L;
        private final RallyPointPanel parent;
        private final Color buttonShadowC = new Color(33,33,33);
        int leftM, midM1, midM2, rightM;
        private LinearGradientPaint fullGrayBackC;
        private LinearGradientPaint largeGreenBackC;
        private LinearGradientPaint largeRedBackC;
        private LinearGradientPaint smallGrayBackC;
        private boolean initted = false;

        private Shape hoverBox;
        private final Rectangle cancelBox = new Rectangle();
        private final Rectangle startBox = new Rectangle();
        private final Rectangle stopBox = new Rectangle();
        Shape textureClip;
        public RallyPointButtonPane (RallyPointPanel p) {
            parent = p;
            initModel();
        }
        private void initModel() {
            setPreferredSize(new Dimension(getWidth(),s40));
            addMouseListener(this);
            addMouseMotionListener(this);
        }
        @Override
        public String textureName()            { return TEXTURE_GRAY; }
        @Override
        public Shape textureClip()         { return textureClip; }
        @Override
        public void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            super.paintComponent(g);

            if (!initted)
                init();
            clearButtons();

            textureClip = new RoundRectangle2D.Float(leftM,s4,rightM-leftM,getHeight()-s7,s10,s10);
            if (parent.isCancellingRally()) {
                drawLargeUndeployButton(g);
                drawSmallCancelButton(g);
            }
            else if (parent.isStartingRally()) {
                drawLargeDeployButton(g);
                drawSmallCancelButton(g);
            }
            else 
                drawFullCancelButton(g);
        }
        private void clearButtons() {
            cancelBox.setBounds(0,0,0,0);
            startBox.setBounds(0,0,0,0);
            stopBox.setBounds(0,0,0,0);
        }
        private void drawFullCancelButton(Graphics2D g) {
            drawButton(g,fullGrayBackC,text("MAIN_RALLY_CANCEL"), cancelBox, leftM, rightM);
        }
        private void drawLargeUndeployButton(Graphics2D g) {
            drawButton(g,largeRedBackC,text("MAIN_RALLY_STOP"), stopBox, leftM, midM1);
        }
        private void drawLargeDeployButton(Graphics2D g) {
            drawButton(g,largeGreenBackC,text("MAIN_RALLY_START"), startBox, leftM, midM1);
        }
        private void drawSmallCancelButton(Graphics2D g) {
            drawButton(g,smallGrayBackC,text("MAIN_RALLY_CANCEL"), cancelBox, midM2, rightM);
        }
        private void drawButton(Graphics2D g, LinearGradientPaint gradient, String label, Rectangle actionBox, int x1, int x2) {
            int y = s4;
            int h = getHeight()-s7;
            int w = x2 - x1;
            if (actionBox != null)
                actionBox.setBounds(x1,y,w,h);
            g.setColor(buttonShadowC);
            Stroke prev = g.getStroke();
            g.setStroke(stroke1);
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
            g.setStroke(stroke1);
            g.drawRoundRect(x1+s1,y,w-s2,h,s10,s10);
            g.setStroke(prev2);
        }
        private void init() {
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
        public void mouseDragged(MouseEvent arg0) { }
        @Override
        public void mouseMoved(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();

            Shape prevHover = hoverBox;
            hoverBox = null;
            if (cancelBox.contains(x,y))
                hoverBox = cancelBox;
            else if (startBox.contains(x,y))
                hoverBox = startBox;
            else if (stopBox.contains(x,y))
                hoverBox = stopBox;

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

            if (cancelBox.contains(x,y)) 
                parent.cancelRelocation();
            else if (startBox.contains(x,y)) 
                parent.createRelocationPath();
            else if (stopBox.contains(x,y)) 
                parent.cancelRelocationPath();
        }
    }
}
