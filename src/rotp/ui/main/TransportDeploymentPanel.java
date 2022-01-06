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
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import rotp.model.Sprite;
import rotp.model.empires.Empire;
import rotp.model.galaxy.StarSystem;
import rotp.ui.BasePanel;
import rotp.ui.sprites.SystemTransportSprite;

public class TransportDeploymentPanel extends SystemPanel {
    private static final long serialVersionUID = 1L;
    public static boolean enableAbandon = false;
    protected BasePanel topPane;
    protected BasePanel detailPane;
    protected BasePanel bottomPane;
    static final Color sliderBoxEnabled = new Color(34,140,142);
    static final Color sliderBackEnabled = Color.black;
    //session vars
    public TransportDeploymentPanel(SpriteDisplayPanel p) {
        parentSpritePanel = p;
        init();
    }
    private void init() {
        initModel();
    }
    @Override
    public void handleNextTurn() {  cancel(); }
    @Override
    public void animate() {
        topPane.animate();
        detailPane.animate();
    }
    @Override
    public boolean canEscape()                      { return true; }
    private boolean canConsume(Sprite s) {
        if (s == null)
            return true;
        if  (s instanceof StarSystem)
            return true;

        return false;
    }
    @Override
    public boolean hoverOverFleets()               { return false; }
    @Override
    public boolean hoverOverFlightPaths()          { return false; }
    @Override
    public boolean useHoveringSprite(Sprite o) {
        if (!canConsume(o)) 
            return false;

        Sprite sprite =  parentSpritePanel.spriteToDisplay();
        if (!(sprite instanceof SystemTransportSprite))
            return false;

        SystemTransportSprite transportSprite = (SystemTransportSprite) sprite;
        if (o instanceof StarSystem)
            transportSprite.hoveringDest((StarSystem) o);
        else
            transportSprite.hoveringDest(null);

        return true;
    }
    @Override
    public boolean useNullClick(int cnt, boolean right) {
        if (right) {
            cancel();
            return true;
        }
        return false;
    }
    @Override
    public boolean useClickedSprite(Sprite o, int count, boolean rightClick) {
        // we have clicked on a system view at this point
        if (!canConsume(o))  {
            return true;
        }
        SystemTransportSprite sprite = (SystemTransportSprite) parentSpritePanel.spriteToDisplay();
        if (o instanceof StarSystem) {
            StarSystem sys = (StarSystem) o;
            if (enableAbandon) {
                if (player().canAbandonTo(sys))
                    sprite.clickedDest((StarSystem) o);
            }
            else if (player().canSendTransportsTo(sys))
                sprite.clickedDest((StarSystem) o);
            else {
                sprite.clickedDest(null);
                misClick();
            }
        }
        else
            misClick();
        return true;
    }
    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        int mods = e.getModifiersEx();

        switch (k) {
            case KeyEvent.VK_1:
                switch (mods) {
                    case 0: increment(true); break;
                    case 64: decrement(true); break;
                    default:  break;
                }
                return;
           case KeyEvent.VK_ESCAPE:
                cancel();
                return;
            case KeyEvent.VK_SPACE:
            case KeyEvent.VK_ENTER:
                if (transportSprite().canAccept())
                    acceptTransport();
                else
                    clearTransport();
                return;
            case KeyEvent.VK_TAB:
                // tab-targeting for transports
                StarSystem currSV = transportSprite().starSystem();
                List<StarSystem> systems = player().orderedTransportTargetSystems();
                // find next index (exploit that missing element returns -1, so set to 0)
                int index = 0;
                switch(e.getModifiersEx()) {
                    case 0:
                        index = systems.indexOf(currSV)+1;
                        if (index == systems.size())
                            index = 0;
                        break;
                    case 1:
                        index = systems.indexOf(currSV)-1;
                        if (index < 0)
                            index = systems.size()-1;
                        break;
                }
                useClickedSprite(systems.get(index), 1, false);
                parentSpritePanel.repaint();
                return;
        }
    }
    @Override
    public StarSystem systemViewToDisplay() {
        return system();
    }
    private StarSystem system() {
        return transportSprite() == null ? null : transportSprite().homeSystem();
    }
    private StarSystem destination() {
        return transportSprite() == null ? null : transportSprite().starSystem();
    }
    private SystemTransportSprite transportSprite() {
        if (!(parentSpritePanel.spriteToDisplay() instanceof SystemTransportSprite))
            return null;
        return (SystemTransportSprite) parentSpritePanel.spriteToDisplay();
    }
    private boolean canSendTransports() {
        return player().canSendTransportsTo(transportSprite().starSystem()) && transportSprite().amt() > 0;
    }
    @Override
    public void cancel() {
        if(transportSprite() != null)
        {
            transportSprite().cancel();
            parentSpritePanel.parent.clickedSprite(transportSprite().homeSystem());
            parentSpritePanel.parent.repaint();
        }
    }
    public void clearTransport() {
        transportSprite().clear();
        parentSpritePanel.parent.clickedSprite(transportSprite().homeSystem());
        parentSpritePanel.parent.repaint();
    }
    public void acceptTransport() {
        if (!canSendTransports())
            return;

        player().deployTransport(transportSprite().homeSystem());
        parentSpritePanel.parent.clickedSprite(transportSprite().homeSystem());
        parentSpritePanel.parent.repaint();
    }
    public void decrement(boolean click) {
        if (transportSprite().decrement(1)) {
            if (click)
                softClick();
            parentSpritePanel.repaint();
        }
        else if (click)
            misClick();
    }
    public void increment(boolean click) {
        if (transportSprite().increment(1)) {
            if (click)
                softClick();
            parentSpritePanel.repaint();
        }
        else if (click)
            misClick();
    }
    @Override
    protected BasePanel topPane() {
        if (topPane == null)
            topPane = new SystemViewInfoPane(this, true);
        return topPane;
    }
    @Override
    protected BasePanel detailPane() {
        if (detailPane == null)
            detailPane = new TransportDetailPane(parentSpritePanel);
        return detailPane;
    }
    @Override
    protected BasePanel bottomPane() {
        if (bottomPane == null)
            bottomPane = new TransportButtonPane(this);
        bottomPane.setBackground(MainUI.shadeBorderC());
        return bottomPane;
    }
    public class TransportDetailPane extends SystemPanel implements MouseListener, MouseMotionListener, MouseWheelListener {
        private static final long serialVersionUID = 1L;
        private Shape hoverBox, hoverBox2;
        public TransportDetailPane(SpriteDisplayPanel p) {
            parentSpritePanel = p;
            init();
        }
        private void init() {
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
        @Override
        public void mouseWheelMoved(MouseWheelEvent e) { }
        @Override
        public void mouseDragged(MouseEvent e) { }
        @Override
        public void mouseMoved(MouseEvent e) { }
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
        public void mouseReleased(MouseEvent e) { }
    }
    class FromSystemDetailPane extends BasePanel implements MouseListener, MouseMotionListener, MouseWheelListener {
        private static final long serialVersionUID = 1L;
        Shape arrow;
        int maxSendingSize = 0;
        // polygon coordinates for left & right increment buttons
        private final int leftButtonX[] = new int[3];
        private final int leftButtonY[] = new int[3];
        private final int rightButtonX[] = new int[3];
        private final int rightButtonY[] = new int[3];
        private final Polygon leftArrow = new Polygon();
        private final Polygon rightArrow = new Polygon();
        private final Rectangle sliderBox = new Rectangle();
        private Shape hoverBox;
        Shape textureClip;

        public FromSystemDetailPane() {
            init();
        }
        private void init() {
            setPreferredSize(new Dimension(getWidth(),scaled(175)));
            setBackground(Color.green);
            addMouseListener(this);
            addMouseMotionListener(this);
            addMouseWheelListener(this);
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
            int h = getHeight();

            g.setColor(MainUI.darkShadowC);
            g.fill(arrow(w, h));
            g.setColor(MainUI.paneBackground());
            g.fillRect(0, s8, w, s72);

            int leftM = s5;
            String title = enableAbandon ? text("MAIN_TRANSPORT_ABANDON_TITLE") : text("MAIN_TRANSPORT_TITLE");
            g.setFont(narrowFont(24));
            drawShadowedString(g, title, 3, leftM, s30, MainUI.shadeBorderC(), SystemPanel.whiteLabelText);

            int y0 = s35;

            Empire pl = player();
            StarSystem dest = destination();
            StarSystem from = system();
            if (from == null)
                return;
            String fromName = pl.sv.name(from.id);

            int maxAllowed = pl.maxTransportsAllowed(dest);
            Color promptColor = MainUI.darkShadowC;
            // draw main prompt, can be more than 2 lines - shrink font if necessary
            String prompt;
            if (!pl.canSendTransportsTo(dest))
                prompt = enableAbandon ? text("MAIN_TRANSPORT_ABANDON_PROMPT") : text("MAIN_TRANSPORT_PROMPT");
            else if (transportSprite().amt() > maxAllowed) {
                if (maxAllowed == 0)
                    prompt = text("MAIN_TRANSPORT_NO_ROOM");
                else
                    prompt = text("MAIN_TRANSPORT_SIZE_WARNING", str(maxAllowed));
                promptColor = SystemPanel.redText;
            }
            else if (dest.empire() == pl)
                prompt = text("MAIN_TRANSPORT_SELECT_POP", fromName);
            else
                prompt = text("MAIN_TRANSPORT_SELECT_TROOPS", fromName);

            int fontSize = 17;
            List<String> lines = new ArrayList<>();
            do {
                fontSize--;
                g.setFont(narrowFont(fontSize));
                lines = wrappedLines(g, prompt, w-leftM-s10);
            } while (lines.size() > 2);

            g.setColor(promptColor);
            for (String line: lines) {
                y0 += s18;
                drawString(g,line, leftM, y0);
            }


            // fall out if we can't send transports
            if (!pl.canSendTransportsTo(dest)) {
                textureClip = new Rectangle2D.Float(0,s8,w,s72);
                return;
            }

            textureClip = new Rectangle2D.Float(0,s8,w,scaled(108));
            //int maxDestSize = pl.sv.maxTransportsToReceive(dest.id);
            maxSendingSize = enableAbandon? pl.sv.population(from.id): pl.sv.maxTransportsToSend(from.id);
            int turns = (int) Math.ceil(from.transportTimeTo(dest));

            g.setColor(MainUI.paneBackground());
            g.fillRect(0, s79, w, s37);

            g.setColor(SystemPanel.blackText);
            String popLabel = enableAbandon ? text("MAIN_TRANSPORT_TOTAL_POP_LABEL", maxSendingSize): text("MAIN_TRANSPORT_POP_LABEL");
            int sw2 = g.getFontMetrics().stringWidth(popLabel);
            y0 += s21;
            g.setFont(narrowFont(16));
            drawString(g,popLabel, leftM, y0);

            // draw slider
            if (enableAbandon) 
                transportSprite().amt(maxSendingSize);
            else {
                if (transportSprite().amt() > maxSendingSize)
                    transportSprite().amt(maxSendingSize);
                float pct = (float) transportSprite().amt() / maxSendingSize;
                int button1X = s15+sw2;
                int button2X = w - s45;
                int buttonW = s10;
                int buttonH = s18;
                int buttonY = y0 - buttonH+s5;
                int buttonMidY = buttonY+(buttonH/2);

                leftButtonX[0] = button1X; leftButtonX[1] = button1X+buttonW; leftButtonX[2] = button1X+buttonW;
                leftButtonY[0] = buttonMidY; leftButtonY[1] = buttonY; leftButtonY[2] = buttonY+buttonH;
                leftArrow.reset();
                for (int i=0;i<leftButtonX.length;i++)
                    leftArrow.addPoint(leftButtonX[i], leftButtonY[i]);

                rightButtonX[0] = button2X; rightButtonX[1] = button2X-buttonW; rightButtonX[2] = button2X-buttonW;
                rightButtonY[0] = buttonMidY; rightButtonY[1] = buttonY; rightButtonY[2] = buttonY+buttonH;
                rightArrow.reset();
                for (int i=0;i<leftButtonX.length;i++)
                    rightArrow.addPoint(rightButtonX[i], rightButtonY[i]);

                Color c0 = hoverBox == leftArrow ? SystemPanel.yellowText : sliderBackEnabled;
                g.setColor(c0);
                g.fillPolygon(leftButtonX, leftButtonY, 3);

                Color c1 = hoverBox == rightArrow ? SystemPanel.yellowText : sliderBackEnabled;
                g.setColor(c1);
                g.fillPolygon(rightButtonX, rightButtonY, 3);

                int boxL = button1X+buttonW+s3;
                int boxW = (button2X-buttonW-s3) - boxL;
                int boxTopY = buttonY;
                int boxBottomY = buttonY+buttonH;
                int boxH = boxBottomY - boxTopY;
                int boxBorderW = s3;

                sliderBox.setBounds(boxL+boxBorderW, boxTopY-boxBorderW, boxW-(2*boxBorderW), boxH+(2*boxBorderW));

                g.setColor(sliderBackEnabled);
                g.fillRect(boxL, boxTopY, boxW, boxH);
                g.setColor(sliderBoxEnabled);
                g.fillRect(boxL, boxTopY+s2, (int) (pct*boxW), boxH-s3);

                if (hoverBox == sliderBox) {
                    g.setColor(SystemPanel.yellowText);
                    Stroke prev = g.getStroke();
                    g.setStroke(stroke2);
                    g.drawRect(boxL+boxBorderW-s2, boxTopY+boxBorderW-s2, boxW-(2*boxBorderW)+s4, boxH-(2*boxBorderW)+s4);
                    g.setStroke(prev);
                }
                // draw amt
                g.setColor(SystemPanel.blackText);
                g.setFont(narrowFont(18));
                drawString(g,str(transportSprite().amt()), button2X+buttonW+s5, y0);
            }

            // draw ETA line under slider
            String destName = pl.sv.name(dest.id);
            String etaLine = text("MAIN_TRANSPORT_ETA", destName, turns);
            y0 += s21;
            g.setColor(SystemPanel.blackText);
            g.setFont(narrowFont(16));
            drawString(g,etaLine, leftM, y0);
        }
        private Shape arrow(int w, int h) {
            if (arrow == null) {
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
            else if (sliderBox.contains(x,y)) {
                float pct = (float) (x -sliderBox.x) / sliderBox.width;
                if (pct >= 0) {
                    if (pct < .05)
                       pct = 0;
                    else if (pct > .95)
                       pct = 1;
                    transportSprite().amt(bounds(0, (int)(pct*maxSendingSize), maxSendingSize));
                    parentSpritePanel.repaint();
                }
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
        @Override
        public void mouseDragged(MouseEvent arg0) { }
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
    }
    class ToSystemDetailPane extends BasePanel implements MouseMotionListener, MouseListener, MouseWheelListener {
        private static final long serialVersionUID = 1L;
        Shape hoverBox;
        Rectangle flagBox = new Rectangle();
        public ToSystemDetailPane() {
            init();
        }
        private void init() {
            setPreferredSize(new Dimension(getWidth(),s80));
            setBackground(MainUI.paneBackground);
            addMouseWheelListener(this);
            addMouseMotionListener(this);
            addMouseListener(this);
        }
        @Override
        public String textureName()            { return TEXTURE_GRAY; }
        @Override
        public void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            super.paintComponent(g);
            int w = getWidth();
            int leftM = s5;
            int y0 = s32;

            g.setFont(narrowFont(24));
            StarSystem sys = destination();

            // no destination selected
            String abandonError = text("MAIN_TRANSPORT_ABANDON_ERROR", player().raceName());
            if (sys == null) {
                String name = text("MAIN_TRANSPORT_NO_SELECTION");
                drawShadowedString(g, name, 2, leftM, s22, MainUI.shadeBorderC(), SystemPanel.whiteLabelText);
                g.setFont(narrowFont(18));
                String prompt = enableAbandon ? abandonError : text("MAIN_TRANSPORT_ERROR");
                g.setColor(SystemPanel.redText);
                List<String> lines = wrappedLines(g, prompt, getWidth()-leftM-s20);
                for (String line: lines) {
                    y0 += s18;
                    drawString(g,line, leftM, y0);
                }
                return;
            }
            int id = sys.id;
            Empire pl = player();
            // uncolonized destination selected
            if (!pl.sv.isColonized(id) && !pl.sv.isAbandoned(id)) {
                String name = pl.sv.descriptiveName(id);
                drawShadowedString(g, name, 2, leftM, s22, MainUI.shadeBorderC(), SystemPanel.whiteLabelText);
                g.setFont(narrowFont(18));
                String prompt = enableAbandon ? abandonError : text("MAIN_TRANSPORT_ERROR");
                g.setColor(SystemPanel.redText);
                List<String> lines = wrappedLines(g, prompt, getWidth()-leftM-s20);
                for (String line: lines) {
                    y0 += s18;
                    drawString(g,line, leftM, y0);
                }
                return;
            }

            // draw panel for alien colonies
            Empire destEmpire = pl.sv.empire(id);
            // can be null for abandoned systems
            if (destEmpire != null) {
                GradientPaint back = new GradientPaint(0,0,destEmpire.color(),w, 0,MainUI.transC);
                g.setPaint(back);
                g.fillRect(0, 0, w, s29);
                g.setPaint(null);
            }
            String name = pl.sv.descriptiveName(id);
            drawShadowedString(g, name, 2, leftM, s22, MainUI.shadeBorderC(), SystemPanel.whiteLabelText);
            
            // draw system banner
            int sz = s60;
            if (hoverBox == flagBox) {
                Image hoverImage = parentSpritePanel.parent.flagHover(sys);
                g.drawImage(hoverImage, w-sz+s15, -s15, sz, sz, null);
            }
            Image flagImage = parentSpritePanel.parent.flagImage(sys);
            g.drawImage(flagImage, w-sz+s15, -s15, sz, sz, null);
            flagBox.setBounds(w-sz+s25,-s15,sz-s20,sz-s10);
                
            String error = null;
            if (!pl.sv.inShipRange(id))
                error = text("MAIN_TRANSPORT_OUT_OF_RANGE");
            else if (!pl.sv.isScouted(id))
                error = text("MAIN_TRANSPORT_UNSCOUTED");
            else if (!pl.canColonize(sys.planet().type()))
                error = text("MAIN_TRANSPORT_HOSTILE");
            if (error != null) {
                if (enableAbandon)
                    error = abandonError ;
                g.setFont(narrowFont(18));
                g.setColor(SystemPanel.redText);
                List<String> lines = wrappedLines(g, error, getWidth()-leftM-s20);
                for (String line: lines) {
                    y0 += s18;
                    drawString(g,line, leftM, y0);
                }
            }
            else if (destEmpire != null) {
                // colony data
                String unknown = text("RACES_UNKNOWN_DATA");
                String factLbl = text("MAIN_COLONY_FACTORIES");
                String baseLbl = text("MAIN_COLONY_BASES");
                String shieldLbl = text("MAIN_COLONY_SHIELD");
                String popLbl = text("MAIN_COLONY_POPULATION");
                boolean spied = pl.sv.isSpied(id);

                int x0 = s5;
                int x1 = w/2;
                y0 += s17;
                int y1 = y0+s25;

                g.setFont(narrowFont(16));
                g.setColor(SystemPanel.blackText);
                drawString(g,popLbl, x0, y0);
                drawString(g,factLbl, x1, y0);
                drawString(g,shieldLbl, x0, y1);
                drawString(g,baseLbl, x1, y1);

                String str1 = spied ? str(pl.sv.population(id)) : unknown;
                int sw1 = g.getFontMetrics().stringWidth(str1);
                drawString(g,str1, x1-sw1-s10, y0);
                String str2 = spied ? str(pl.sv.factories(id)) : unknown;
                int sw2 = g.getFontMetrics().stringWidth(str2);
                drawString(g,str2, w-s10-sw2, y0);
                String str3 = spied ? str(pl.sv.shieldLevel(id)) : unknown;
                int sw3 = g.getFontMetrics().stringWidth(str3);
                drawString(g,str3, x1-s10-sw3, y1);
                String str4 = spied ? str(pl.sv.bases(id)) : unknown;
                int sw4 = g.getFontMetrics().stringWidth(str4);
                drawString(g,str4, w-s10-sw4, y1);

                // draw borders around data
                g.setColor(dataBorders);
                Stroke prevStroke = g.getStroke();
                g.setStroke(stroke1);
                //g.drawLine(0, y0-s18, w, y0-s18);
                g.drawLine(0, y1-s18, w, y1-s18);
                g.drawLine(x1-s5, y0-s19, x1-s5, y0+s31);
                g.setStroke(prevStroke);
            }
        }
        public void toggleFlagColor(boolean rightClick) {
            StarSystem sys = destination();
            if (rightClick)
                player().sv.resetFlagColor(sys.id);
            else
                player().sv.toggleFlagColor(sys.id);
            parentSpritePanel.repaint();
        }
        @Override
        public void mouseDragged(MouseEvent e) { }
        @Override
        public void mouseMoved(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();
            Shape prevHover = hoverBox;
            hoverBox = null;
            if (flagBox.contains(x,y))
                hoverBox = flagBox;

            if (prevHover != hoverBox)
                repaint();
        }
        @Override
        public void mouseClicked(MouseEvent e) { }
        @Override
        public void mousePressed(MouseEvent e) { }
        @Override
        public void mouseReleased(MouseEvent e) {
            boolean rightClick = SwingUtilities.isRightMouseButton(e);
            if (hoverBox == flagBox) {
                toggleFlagColor(rightClick);
           }
        }
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
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (hoverBox == flagBox) {
                StarSystem sys = destination();
                if (e.getWheelRotation() < 0)
                    player().sv.toggleFlagColor(sys.id, true);
                else
                    player().sv.toggleFlagColor(sys.id, false);
                parentSpritePanel.repaint();
            }
        }
    }
    public class TransportButtonPane extends BasePanel implements MouseListener, MouseMotionListener {
        private static final long serialVersionUID = 1L;
        private final TransportDeploymentPanel parent;
        private final Color buttonShadowC = new Color(33,33,33);
        int leftM, midM1, midM2, rightM;
        private LinearGradientPaint fullGrayBackC;
        private LinearGradientPaint largeGreenBackC;
        private LinearGradientPaint largeRedBackC;
        private LinearGradientPaint smallGrayBackC;
        private boolean initted = false;

        private Shape hoverBox;
        private final Rectangle cancelBox = new Rectangle();
        private final Rectangle sendBox = new Rectangle();
        private final Rectangle clearBox = new Rectangle();
        Shape textureClip;
        public TransportButtonPane(TransportDeploymentPanel p) {
            parent = p;
            init();
        }
        private void init() {
            setPreferredSize(new Dimension(getWidth(),s40));
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
        public String textureName()        { return TEXTURE_GRAY; }
        @Override
        public Shape textureClip()         { return textureClip; }
        @Override
        public void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            super.paintComponent(g);

            if (!initted)
                initGradients();

            clearButtons();

            textureClip = new RoundRectangle2D.Float(leftM,s4,rightM-leftM,getHeight()-s7,s10,s10);
            if (!player().canSendTransportsTo(destination()))
                drawFullCancelButton(g);
            else if (system() == destination())
                drawFullCancelButton(g);
            else if (transportSprite().canAccept()) {
                if (enableAbandon)
                    drawLargeAbandonButton(g);
                else
                    drawLargeSendButton(g);
                drawSmallCancelButton(g);
            }
            else if (transportSprite().canClear()) {
                drawLargeClearButton(g);
                drawSmallCancelButton(g);
            }
            else
                drawFullCancelButton(g);
        }
        private void clearButtons() {
            cancelBox.setBounds(0,0,0,0);
            sendBox.setBounds(0,0,0,0);
            clearBox.setBounds(0,0,0,0);
        }
        private void drawLargeAbandonButton(Graphics2D g) {
            drawButton(g,largeRedBackC,text("MAIN_TRANSPORT_ABANDON"), sendBox, leftM, midM1);
        }
        private void drawLargeSendButton(Graphics2D g) {
            drawButton(g,largeGreenBackC,text("MAIN_TRANSPORT_SEND"), sendBox, leftM, midM1);
        }
        private void drawLargeClearButton(Graphics2D g) {
            drawButton(g,largeRedBackC,text("MAIN_TRANSPORT_CLEAR"), clearBox, leftM, midM1);
        }
        private void drawSmallCancelButton(Graphics2D g) {
            drawButton(g,smallGrayBackC,text("MAIN_TRANSPORT_CANCEL"), cancelBox, midM2, rightM);
        }
        private void drawFullCancelButton(Graphics2D g) {
            drawButton(g,fullGrayBackC,text("MAIN_TRANSPORT_CANCEL"), cancelBox, leftM, rightM);
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
            else if (sendBox.contains(x,y))
                hoverBox = sendBox;
            else if (clearBox.contains(x,y))
                hoverBox = clearBox;

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
            if (e.getClickCount() > 1)
                return;;
             int x = e.getX();
            int y = e.getY();

            if (cancelBox.contains(x,y)) 
                parent.cancel();
            else if (sendBox.contains(x,y)) 
                parent.acceptTransport();
            else if (clearBox.contains(x,y)) 
                parent.clearTransport();
        }
    }
}
