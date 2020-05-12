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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.swing.border.Border;
import rotp.model.empires.SystemView;
import rotp.model.galaxy.StarSystem;
import rotp.model.planet.Planet;
import rotp.ui.BasePanel;
import rotp.ui.BaseTextField;
import rotp.ui.main.SystemPanel;
import rotp.ui.sprites.SystemTransportSprite;
import rotp.util.Base;
import rotp.util.Palette;

public abstract class SystemListingUI extends BasePanel implements MouseListener, MouseMotionListener, MouseWheelListener {
    private static final long serialVersionUID = 1L;
    private static LinearGradientPaint headerBack;
    private static Border cellBorder;
    private static final Color headerCenterC = new Color(162,110,77);
    private static final Color headerEdgeC = new Color(100,70,50);
    private static final Color cellBorderC = new Color(150,129,108);
    protected static final Color selectedC = new Color(178,124,87);
    protected static final Color unselectedC = new Color(112,85,68);
    protected static final Color selectedRedC = new Color(192,0,0);
    protected static final Color unselectedRedC = new Color(128,0,0);
    public static final Color scrollBarC = new Color(211,166,125);

    public static final int LEFT = -1;
    public static final int CENTER = 0;
    public static final int RIGHT = 1;

    int TRANSPORT_SLIDER = 1;
    int TRANSPORT_DECREMENT = 2;
    int TRANSPORT_INCREMENT = 3;
    int TRANSPORT_STOP = 4;

    private final BasePanel topParent;
    protected Palette palette;
    private final List<Sprite> sprites = new ArrayList<>();
    private final List<List<SystemButton>> rowButtons = new ArrayList<>();
    SystemButton hoveringButton;

    private final Rectangle listBox = new Rectangle();
    Rectangle hoverBox;
    private boolean dragging = false;
    private final int dragStartX = 0;
    private final int dragStartY = 0;
    private int dragY = 0;
    private int lastMouseY;
    private int yOffset = 0;
    private Sprite hoveringSprite;
    private Column hoveringHeader;
    private Column selectedColumn;
    private boolean redrawHeaders = false;
    final Rectangle listScroller = new Rectangle();
    int startY = 0;
    int maxY = 0;
    int minDisplayY, maxDisplayY;
    boolean scrolling = false;

    public SystemListingUI(BasePanel p) {
        topParent = p;
        cellBorder = newLineBorder(cellBorderC, 1);
        initModel();
    }
    protected void initPalette() { palette = Palette.named("Brown"); }
    protected void postInit() { }
    protected abstract List<StarSystem> systems();
    protected abstract StarSystem selectedSystem();
    protected abstract void selectedSystem(StarSystem sv, boolean updateFieldValues);
    protected abstract DataView dataView();
    protected boolean selectRows()  { return true; }
    public int rowHeight()          { return s30; }
    protected Color selectedC()     { return selectedC; }
    protected Color unselectedC()   { return unselectedC; }
    protected Color selectedRedC()  { return selectedRedC; }
    protected Color unselectedRedC(){ return unselectedRedC; }
    protected int dataFontSize()    { return 20; }
    public void open() { 
        int rowH = rowHeight();
        int listH = getHeight()-rowH;
        float displayedRows = (float) listH/rowH;
        
        int selectedIndex = systems().indexOf(selectedSystem());
        int rowIndex = selectedIndex+1;
        if (rowIndex <= displayedRows)
            startY = 0;
        else
            startY = (int) (rowH * (rowIndex - displayedRows));
    }
    public void close() { }
    private void initModel() {
        initPalette();
        setOpaque(true);
        setBackground(palette.darkBack);
        setLayout(null);
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        postInit();
    }
    public void selectedColumn(Column col)  { selectedColumn = col; }
    public DataView newDataView() { return new DataView(); }
    public RowNumColumn newRowNumColumn(String s, int i, int align) {
        return new RowNumColumn(s, i, align);
    }
    public SystemDataColumn newSystemDataColumn(String s1, String s2, int i, Color clr, Comparator<StarSystem> c, int align) {
        return new SystemDataColumn(s1, s2, i, clr, c, align);
    }
    public SystemDeltaDataColumn newSystemDeltaDataColumn(String s1, String s2, int i, Color clr, Comparator<StarSystem> c, int align) {
        return new SystemDeltaDataColumn(s1, s2, i, clr, c, align);
    }
    public SystemNameColumn newSystemNameColumn(BaseTextField field, String s1, String s2, int i, Color clr, Comparator<StarSystem> c, int align) {
        return new SystemNameColumn(field, s1, s2, i, clr, c, align);
    }
    public SystemNotesColumn newSystemNotesColumn(BaseTextField field, String s1, String s2, int i, Color clr) {
        return new SystemNotesColumn(field, s1, s2, i, clr);
    }
    public PlanetTypeColumn newPlanetTypeColumn(String s1, String s2, int i, Comparator<StarSystem> c) {
        return new PlanetTypeColumn(s1, s2, i, c);
    }
    public SystemSetTransportsColumn newSystemSetTransportsColumn(String s1, SystemListingUI ui, int i) {
        return new SystemSetTransportsColumn(s1, ui, i);
    }
    @Override
    public void paintComponent(Graphics g0) {
        sprites.clear();
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;

        int w = getWidth();
        int h = getHeight();

        int leftM = 0;
        int rightM = 0;
        int topM = 0;
        int cellSpacing = 0;
        int scrollBoxW = s20;

        // draw the gradient background for the header row
        if (headerBack == null) {
            Point2D start = new Point2D.Float(leftM, 0);
            Point2D end = new Point2D.Float(w-rightM, 0);
            float[] dist = {0.0f, 0.25f, 0.5f, 0.75f, 1.0f};
            Color[] colors = {headerEdgeC, headerEdgeC, headerCenterC, headerEdgeC, headerEdgeC };
            headerBack = new LinearGradientPaint(start, end, dist, colors);
        }
        g.setPaint(headerBack);
        g.fillRect(leftM,topM,w-leftM-rightM,rowHeight());


        int rowH = rowHeight()+cellSpacing;
        int listH = h-topM-rowH; // don't count header row in height
        int numRows = systems().size();
        maxY = max(0, (numRows*rowH)-listH);
        scrolling = numRows*rowH > listH;
        if (scrolling)
            rightM = scrollBoxW;
        
        g.setFont(narrowFont(dataFontSize()));

        DataView dv = dataView();
        int x0 = leftM;
        int y0 = topM+rowHeight();
        for (Column col: dv.columns) {
            int colWidth = min(col.width(), w-rightM-x0);
            col.drawHeader(g, x0, y0, colWidth);
            x0 += (col.width() + cellSpacing);
            sprites.add(new HeaderSprite(col));
        }
        int row1Y = y0;
        //y0 += (rowHeight()+cellSpacing);
        int rowNum = 0;
        int y1 = rowH; // already drew the header row
        g.setClip(leftM, y1, w-rightM-leftM, listH);
        minDisplayY = y1+rowH;
        maxDisplayY = minDisplayY+listH;
        y0 = minDisplayY-startY;
        int minSelectableIndex = numRows;
        int maxSelectableIndex = 0;
        for (int i=0;i<systems().size();i++) {
            if (rowButtons.size() <= rowNum)
                rowButtons.add(new ArrayList<>());
            StarSystem sys = systems().get(i);
            RowSprite row = new RowSprite(sys, rowButtons.get(rowNum), leftM, y0, 0, rowHeight());
            x0 = leftM;
            for (Column col: dv.columns) {
                int colWidth = min(col.width(), w-rightM-x0);
                col.draw(g, row, sys, x0, y0, colWidth);
                x0 += (col.width() + cellSpacing);
            }
            if ((y0 >= minDisplayY) && ((y0+rowH) <= maxDisplayY)) {
                minSelectableIndex = min(minSelectableIndex, i);
                maxSelectableIndex = max(maxSelectableIndex, i);
            }
            row.w = x0-leftM;
            sprites.add(row);
            y0 += rowH;
            rowNum++;
        }
       
        int selectedIndex = selectedIndex();
        if (selectedIndex < minSelectableIndex)
            selectedSystem(systems().get(minSelectableIndex),true);
        else if (selectedIndex > maxSelectableIndex) 
            selectedSystem(systems().get(maxSelectableIndex),true);
        g.setClip(null);
        listBox.setBounds(0,row1Y,w,h-row1Y);
        
        if (maxY == 0)
            listScroller.setBounds(0,0,0,0);
        else {
            g.setColor(scrollBarC);
            int scrollW = scrollBoxW-s5;
            int scrollH = (int) ((float)listH*listH/(listH+maxY));
            int scrollX = w-rightM+s3;
            int scrollY =(int) (minDisplayY-rowH+ (float)listH*startY/(maxY+listH));
            g.fillRoundRect(scrollX, scrollY, scrollW, scrollH, s4, s4);
            listScroller.setBounds(scrollX, scrollY, scrollW, scrollH);
            if (hoverBox == listScroller) {
                Stroke prev = g.getStroke();
                g.setColor(Color.yellow);
                g.setStroke(stroke2);
                g.drawRoundRect(scrollX, scrollY, scrollW, scrollH, s4, s4);
                g.setStroke(prev);
            }
        }
    }
    public boolean scrollUp()    { 
        int prevY = startY;
        startY = max(0, startY-s10);
        boolean changed = startY != prevY;
        if ((startY == 0) && (startY == prevY)) {
            int index = selectedIndex();
            if (index > 0) {
                index--;
                selectedSystem(systems().get(index), true);
                changed = true;
            }
        }
        return changed;
    }
    public boolean scrollDown()  { 
        int prevY = startY;
        startY = min(maxY, startY+s10);
        boolean changed = startY != prevY;
        if ((startY == maxY) && (startY == prevY)) {
            int index = selectedIndex();
            if (index < systems().size()-1) {
                index++;
                selectedSystem(systems().get(index), true);
                changed = true;
            }
        }
        return changed;
    }
    private int selectedIndex()         { return systems().indexOf(selectedSystem()); }
    private void scrollY(int deltaY) {
        yOffset += deltaY;
        if ((yOffset > rowHeight())) {
            yOffset -= rowHeight();
            if (scrollUp())
                repaint();
        }
        else if (yOffset < -rowHeight()) {
            yOffset += rowHeight();
            if (scrollDown())
                repaint();
        }
    }
    private Sprite matchingSprite(int x, int y) {
        for (Sprite s: sprites)
            if (s.isSelectableAt(x, y))
                return s;

        return null;
    }
    private SystemButton matchingButton(int x, int y) {
        for (List<SystemButton> buttons : rowButtons) {
            for (SystemButton button: buttons) {
                if (button.contains(x, y))
                    return button;
            }
        }
        return null;
    }
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        int x = e.getX();
        int y = e.getY();
        SystemButton button = matchingButton(x,y);
        if ((button != null) && button.wantsMouseWheel()) {
            Sprite sprite = matchingSprite(x,y);
            button.mouseWheelMoved(sprite.system(), e);
            return;
        }
        if (!scrolling)
            return;
        boolean scrolled = (e.getWheelRotation() < 0) ? scrollUp() : scrollDown();
        if (scrolled)
            topParent.repaint();
    }
    @Override
    public void mouseDragged(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        int dY = y-dragY;
        dragY = y;
        if (listScroller == hoverBox) {
            if ((y >= listBox.y) || (y <= (listBox.y+listBox.height))) { 
                int h = (int) listBox.getHeight();
                int dListY = (int)((float)dY*(h+maxY)/h);
                if (dY < 0)
                    startY = max(0,startY+dListY);
                else 
                    startY = min(maxY,startY+dListY);
            }
            repaint(listBox);
            return;
        }
        else if (listBox == hoverBox) {
            if (listBox.contains(x,y)) { 
                int h = (int) listBox.getHeight();
                int dListY = (int)(-(float)dY*(h+maxY)/h);
                if (dListY < 0)
                    startY = max(0,startY+dListY);
                else 
                    startY = min(maxY,startY+dListY);
            }
            repaint(listBox);
            return;
        }
    }
    @Override
    public void mouseMoved(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();

        Rectangle prevHover = hoverBox;
        hoverBox = null;
        if (listScroller.contains(x,y)) 
            hoverBox = listScroller;
        else if (listBox.contains(x,y)) 
            hoverBox = listBox;
        
        if (prevHover != hoverBox) {
            repaint();
            return;
        }
        int deltaY = y - lastMouseY;
        lastMouseY = y;

        if (dragging && listBox.contains(x,y))
            scrollY(deltaY);

        // check for hovering buttons within rows first
        SystemButton prevButton = hoveringButton;
        hoveringButton = matchingButton(x,y);
        if (prevButton != hoveringButton) {
            repaint();
            return;
        }

        // check for row highlighting
        Sprite sprite = matchingSprite(x,y);
        boolean sameSprite = (sprite == hoveringSprite)
                         || ((sprite != null) && sprite.equalsSprite(hoveringSprite));

        if (!sameSprite) {
            if (hoveringSprite != null)
                hoveringSprite.exit();
            hoveringSprite = sprite;
            if (hoveringSprite != null)
                hoveringSprite.enter();
            if (redrawHeaders) {
                repaint();
                redrawHeaders = false;
            }
        }
    }
    @Override
    public void mouseClicked(MouseEvent arg0) { }
    @Override
    public void mouseEntered(MouseEvent arg0) { }
    @Override
    public void mouseExited(MouseEvent arg0) {
        hoveringSprite = null;
        hoveringHeader = null;
        hoveringButton = null;
        repaint();
    }
    @Override
    public void mousePressed(MouseEvent e) {
        dragY = e.getY();
    }
    @Override
    public void mouseReleased(MouseEvent e) {
        dragY = 0;
        if (e.getButton() > 3)
            return;
        int x = e.getX();
        int y = e.getY();
        
        if (hoverBox == listScroller)
            return;

        if (dragging) {
            dragging = false;
            int dragDist = unscaled((int)distance(x, y, dragStartX, dragStartY));
            // if >20px, then this was a real drag event and not a sloppy click
            if (dragDist >= 20)
                return;
        }

        if (e.getSource() instanceof BaseTextField)
            return;

        // check for hovering buttons within rows first
        SystemButton button = matchingButton(x,y);
        if ((button != null) && button.wantsMouseRelease()) {
            Sprite sprite = matchingSprite(x,y);
            button.mouseReleased(sprite.system(), e);
            repaint();
            return;
        }

        Sprite sprite = matchingSprite(x,y);
        if (sprite instanceof RowSprite && !selectRows())
            return;

        if (sprite != null) {
            sprite.click();
            topParent.repaint();
        }
    }
    public class DataView {
        public final List<Column> columns = new ArrayList<>();
        public void addColumn(Column c)   { columns.add(c); }
    }
    public abstract class Column implements Base {
        String headerKey;
        int width;
        int x, y;
        int align = LEFT;
        boolean reversed = false;
        public int width()             { return width; }
        public void click()            { selectedColumn = this; }
        protected void drawCell(Graphics g, Color c, int x, int y, int w) {
            g.setColor(c);
            g.fillRect(x, y-rowHeight(), w, rowHeight());
            cellBorder.paintBorder(topParent, g, x, y-rowHeight(), w, rowHeight());
        }
        public void drawBlank(Graphics g, int x, int y, int w) {
            drawCell(g, palette.medBack, x, y, w);
        }
        public void draw(Graphics g, RowSprite row, StarSystem sys, int x, int y, int w) {
            g.setFont(narrowFont(dataFontSize()));
            SystemView sv = player().sv.view(sys.id);
            boolean alert = sv.isAlert();
            Color selectedC = alert ? selectedRedC() : selectedC();
            Color unselectedC = alert ? unselectedRedC() : unselectedC();
            Color backC = (sys == selectedSystem()) || (selectedColumn == this) ? selectedC : unselectedC;
            drawCell(g, backC, x,y,w);
        }
        public void drawHeader(Graphics g, int x0, int y0, int w) {
            x = x0;
            y = y0;

            int displayW = w;

            String title = text(headerKey);
            int sw = Integer.MAX_VALUE;
            int fontSize = dataFontSize();

            int minFont = 16;
            while ((sw > displayW) && (fontSize > minFont)) {
                g.setFont(narrowFont(fontSize));
                sw = g.getFontMetrics().stringWidth(title);
                fontSize--;
            }

            if (sw > displayW) {
                width = sw;
                displayW = sw;
            }

            Color c0 = hoveringHeader == this ? palette.yellow : palette.white;

            // always draw header centered
            drawShadowedString(g, title, 3, x+((displayW-sw)/2), y-s10, SystemPanel.textShadowC, c0);
        }
    }
    public class RowNumColumn extends Column {
        public RowNumColumn(String s, int i, int a) {
            headerKey = s;
            width = scaled(i);
            align = a;
        }
        @Override
        public int width() {
            int n = player().numColonies();
            if (n < 100)
                return width * 2;
            else if (n < 1000)
                return width * 3;
            else
                return width * 4;
        }
        @Override
        public void draw(Graphics g, RowSprite row, StarSystem sys, int x, int y, int w) {
            super.draw(g, row, sys, x, y, w);
            String val = str(systems().indexOf(sys)+1);
            int sw = g.getFontMetrics().stringWidth(val);
            if (sys.hasEvent())
                g.setColor(palette.black);
            else
                g.setColor(palette.maroon);
            switch(align) {
                case LEFT:    g.drawString(val, x+s5, y-s5); break;
                case RIGHT:   g.drawString(val, x+w-s10-sw, y-s5); break;
                case CENTER:  g.drawString(val, x+((w-sw)/2), y-s5); break;
            }
        }
    }
    public class SystemSetTransportsColumn extends Column {
        StarSystem targetSystem;
        SystemListingUI parentUI;
        final int MAX_TICKS = 50;
        private final Color sliderBoxBlue = new Color(34,140,142);
        private final Color sliderButtonColor = Color.black;

        int boxAreaL, boxAreaW;
        // polygon coordinates for left & right increment buttons
        private final int leftButtonX[] = new int[3];
        private final int leftButtonY[] = new int[3];
        private final int rightButtonX[] = new int[3];
        private final int rightButtonY[] = new int[3];
        SystemSetTransportsColumn(String s1, SystemListingUI ui, int i) {
            headerKey = s1;
            parentUI = ui;
            width = scaled(i);
        }
        public void targetSystem(StarSystem s)  { targetSystem = s; }
        @Override
        public void draw(Graphics g, RowSprite row, StarSystem sys, int x, int y, int w) {
            super.draw(g, row, sys, x, y, w);

            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(Color.black);
            if (targetSystem == null)
                return;

            if (!player().canSendTransportsFrom(sys)) {
                if (sys.colony().inRebellion())
                    drawErrorString(g2, text("MAIN_PLANET_REBELLION"), x+s10, y-s6);
                else if (sys.colony().quarantined())
                    drawErrorString(g2, text("MAIN_PLANET_QUARANTINE"), x+s10, y-s6);
                return;
            }

            StarSystem dest = sys.colony().transportDestination();
            if ((dest == null) || (dest == targetSystem)) {
                drawSliderBox(g2, row, sys, x, y - s23, w);
            } else {
                drawExistingTransports(g2, row, sys, x, y - s26, w);
            }
        }
        private void drawErrorString(Graphics2D g, String err, int x0, int y1) {
            g.setFont(narrowFont(18));
            g.setColor(SystemPanel.blackText);
            g.drawString(err,x0,y1);
        }
        private void drawSliderBox(Graphics2D g, RowSprite row, StarSystem sys, int x0, int y1, int w) {
            int amt = sys.transportSprite().amt();
            int maxAmt = player().sv.maxTransportsToSend(sys.id);
            if (maxAmt == 0) {
                drawUnableToSendTransports(g, row, sys, x0, y1-s3, w);
                return;
            }

            TransportSliderButton sliderBox = (TransportSliderButton) row.getButton(TRANSPORT_SLIDER);
            if (sliderBox == null) {
                sliderBox = new TransportSliderButton();
                row.addButton(sliderBox);
            }
            TransportDecrementButton leftArrow = (TransportDecrementButton) row.getButton(TRANSPORT_DECREMENT);
            if (leftArrow == null) {
                leftArrow = new TransportDecrementButton();
                row.addButton(leftArrow);
            }
            TransportIncrementButton rightArrow = (TransportIncrementButton) row.getButton(TRANSPORT_INCREMENT);
            if (rightArrow == null) {
                rightArrow = new TransportIncrementButton();
                row.addButton(rightArrow);
            }
            // slider arrow buttons
            int arrowLeftM = x0+s15;
            int arrowRightM = x0+w-s60;
            int arrowW = s8;
            int arrowTopY = y1;
            int arrowH = s16;
            leftButtonX[0] = arrowLeftM; leftButtonX[1] = arrowLeftM+arrowW; leftButtonX[2] = arrowLeftM+arrowW;
            leftButtonY[0] = arrowTopY+(arrowH/2); leftButtonY[1] = arrowTopY; leftButtonY[2] = arrowTopY+arrowH;
            rightButtonX[0] = arrowRightM; rightButtonX[1] = arrowRightM-arrowW; rightButtonX[2] = arrowRightM-arrowW;
            rightButtonY[0] = arrowTopY+(arrowH/2); rightButtonY[1] = arrowTopY; rightButtonY[2] = arrowTopY+arrowH;
            leftArrow.reset();
            rightArrow.reset();
            for (int i=0;i<3;i++) {
                leftArrow.addPoint(leftButtonX[i], leftButtonY[i]);
                rightArrow.addPoint(rightButtonX[i], rightButtonY[i]);
            }

            if (parentUI.hoveringButton == leftArrow)
                g.setColor(Color.yellow);
            else
                g.setColor(sliderButtonColor);
            g.fillPolygon(leftButtonX, leftButtonY, 3);
            if (parentUI.hoveringButton == rightArrow)
                g.setColor(Color.yellow);
            else
                g.setColor(sliderButtonColor);
            g.fillPolygon(rightButtonX, rightButtonY, 3);

            // slider box
            int boxL = arrowLeftM+arrowW+s4;
            int boxR = arrowRightM-arrowW-s4;
            int boxW = boxR - boxL;
            int boxTopY = arrowTopY;
            int boxH = arrowH;
            int boxBorderW = s2;

            g.setColor(Color.black);
            g.fillRect(boxL, boxTopY, boxW, boxH);
            g.setColor(sliderBoxBlue);
            g.fillRect(boxL, boxTopY+s1, boxW*amt/maxAmt, boxH-s2);

            if (parentUI.hoveringButton == sliderBox) {
                g.setColor(Color.yellow);
                Stroke prev = g.getStroke();
                g.setStroke(stroke2);
                g.drawRect(boxL, boxTopY, boxW, boxH);
                g.setStroke(prev);
            }

            boxAreaL = boxL+boxBorderW;
            boxAreaW = boxW-boxBorderW-boxBorderW;
            sliderBox.setBounds(boxAreaL, boxTopY, boxAreaW, boxH);

            if (amt > 0) {
                g.setColor(Color.black);
                g.setFont(narrowFont(dataFontSize()));
                String amtStr = str(amt);
                int amtW = g.getFontMetrics().stringWidth(amtStr);
                g.drawString(amtStr, arrowRightM+s35-amtW, boxTopY+boxH-s1);
            }
        }
        private void drawUnableToSendTransports(Graphics2D g, RowSprite row, StarSystem sys, int x0, int y1, int w) {
            g.setColor(Color.black);
            String sysName = player().sv.name(sys.id);
            String detail = text("FLEETS_CANNOT_SEND_TRANSPORTS", sysName);
            scaledFont(g, detail, w-s20, 20, 12);
            g.drawString(detail, x0+s10, y1+s18);
        }
        private void drawExistingTransports(Graphics2D g, RowSprite row, StarSystem sys, int x0, int y1, int w) {
            TransportStopButton stopButton = (TransportStopButton) row.getButton(TRANSPORT_STOP);
            if (stopButton == null) {
                stopButton = new TransportStopButton();
                row.addButton(stopButton);
            }
            g.setFont(narrowFont(16));
            String lbl = text("FLEETS_EXISTING_STOP");
            int lblW = g.getFontMetrics().stringWidth(lbl);

            // button
            int boxR = x0+w-s10;
            int boxL = boxR-lblW-s10;
            int boxW = boxR - boxL;
            int boxTopY = y1;
            int boxH = s20;

            g.setColor(Color.black);
            g.fillRect(boxL+s2, boxTopY+s2, boxW, boxH);
            g.setColor(palette.medBack);
            g.fillRect(boxL, boxTopY, boxW, boxH);

            Stroke prev = g.getStroke();
            Color c0 = parentUI.hoveringButton == stopButton ? Color.yellow : SystemPanel.whiteText;
            g.setColor(c0);
            g.setStroke(stroke1);
            g.drawRect(boxL, boxTopY, boxW, boxH);
            g.setStroke(prev);

            drawShadowedString(g, lbl, 2, boxL+s5, boxTopY+boxH-s5, SystemPanel.textShadowC, c0);

            stopButton.setBounds(boxL, boxTopY, boxW, boxH);

            g.setColor(Color.black);
            SystemTransportSprite spr = sys.transportSprite();
            String sysName = player().sv.name(spr.starSystem().id);
            String detail = text("FLEETS_EXISTING_TRANSPORT", str(spr.amt()), sysName);
            this.scaledFont(g, detail, boxL-s5-x0, 20, 12);
            g.drawString(detail, x0+s10, boxTopY+boxH-s2);
        }
    }
    public class SystemDataColumn extends Column {
        String attributeKey;
        Color color;
        Comparator<StarSystem> comp;
        SystemDataColumn(String s1, String s2, int i, Color clr, Comparator<StarSystem> c, int a) {
            headerKey = s1;
            attributeKey = s2;
            width = scaled(i);
            color = clr;
            comp = c;
            align = a;
        }
        protected Color color(StarSystem sys)   { return color; }
        public boolean enabled()  { return comp != null; }
        @Override
        public void click() {
            super.click();
            if (enabled()) {
                reversed = !reversed;
                Collections.sort(systems(), comp);
                if (reversed)
                    Collections.reverse(systems());
                selectedSystem(selectedSystem(), true);
            }
        }
        @Override
        public void draw(Graphics g, RowSprite row, StarSystem sys, int x, int y, int w) {
            super.draw(g, row, sys, x, y, w);
            String val = sys.getAttribute(attributeKey);
            int sw = g.getFontMetrics().stringWidth(val);
            g.setColor(color(sys));
            switch(align) {
                case LEFT:    g.drawString(val, x+s5, y-s5); break;
                case RIGHT:   g.drawString(val, x+w-s10-sw, y-s5); break;
                case CENTER:  g.drawString(val, x+((w-sw)/2), y-s5); break;
            }
        }
    }
    public class SystemNameColumn extends SystemDataColumn {
        private final BaseTextField nameField;
        SystemNameColumn(BaseTextField fld, String s1, String s2, int i, Color clr, Comparator<StarSystem> c, int a) {
            super(s1,s2,i,clr,c,a);
            nameField = fld;
        }
        @Override
        public void draw(Graphics g, RowSprite row, StarSystem sys, int x, int y, int w) {
            if (sys != selectedSystem()) {
                super.draw(g, row, sys, x, y, w);
                return;
            }
            if (nameField.isVisible()) {
                if (nameField.getY() != (y-s30)) {
                    nameField.setBounds(x, y-s30, w, s30);
                    nameField.repaint();
                }
            }
            else {
                SystemView sv = player().sv.view(sys.id);
                if (sv.isAlert())
                    nameField.setBackground(selectedRedC());
                else
                    nameField.setBackground(selectedC());
                nameField.setBounds(x, y-s30, w, s30);
                nameField.setVisible(true);
                nameField.repaint();
            }
        }
        public boolean showField(int y) {
            return (y >= minDisplayY) && (y <= maxDisplayY);
        }
    }
    public class SystemNotesColumn extends SystemDataColumn {
        private final BaseTextField notesField;
        SystemNotesColumn(BaseTextField fld, String s1, String s2, int i, Color clr) {
            super(s1,s2,i,clr,StarSystem.NOTES, LEFT);
            notesField = fld;
        }
        @Override
        public void draw(Graphics g, RowSprite row, StarSystem sys, int x, int y, int w) {
            if (sys != selectedSystem()) {
                super.draw(g, row, sys, x, y, w);
                return;
            }
            if (notesField.isVisible()) {
                if (notesField.getY() != (y-s30)) {
                    notesField.setBounds(x, y-s30, w, s30);
                    notesField.repaint();
                }
            }
            else {
                SystemView sv = player().sv.view(sys.id);
                if (sv.isAlert())
                    notesField.setBackground(selectedRedC());
                else
                    notesField.setBackground(selectedC());
                notesField.setBounds(x, y-s30, w, s30);
                notesField.setVisible(true);
                notesField.repaint();
            }
        }
    }
    public class PlanetTypeColumn extends Column implements Base {
        String attributeKey;
        Comparator<StarSystem> comp;
        PlanetTypeColumn(String s1, String s2, int i, Comparator<StarSystem> c) {
            headerKey = s1;
            attributeKey = s2;
            width = scaled(i);
            comp = c;
            align = CENTER;
        }
        public boolean enabled()  { return comp != null; }
        @Override
        public void click() {
            super.click();
            if (enabled()) {
                reversed = !reversed;
                Collections.sort(systems(), comp);
                if (reversed)
                    Collections.reverse(systems());
                selectedSystem(selectedSystem(), true);
            }
        }
        @Override
        public void draw(Graphics g, RowSprite row, StarSystem sys, int x, int y, int w) {
            super.draw(g, row, sys, x, y, w);
            String val = sys.getAttribute(attributeKey);
            int sw = g.getFontMetrics().stringWidth(val);

            Planet p = sys.planet();
            if (player().isEnvironmentFertile(sys) || player().isEnvironmentGaia(sys))
                g.setColor(palette.green);
            else if (player().isEnvironmentHostile(sys))
                g.setColor(palette.maroon);
            else
                g.setColor(palette.black);
            switch(align) {
                case LEFT:    g.drawString(val, x+s5, y-s5); break;
                case RIGHT:   g.drawString(val, x+w-s10-sw, y-s5); break;
                case CENTER:  g.drawString(val, x+((w-sw)/2), y-s5); break;
            }
        }
    }
    public class SystemDeltaDataColumn extends Column implements Base {
        String attributeKey;
        String deltaKey;
        Color color;
        Comparator<StarSystem> comp;
        int px[] = new int[3];
        int py[] = new int[3];
        SystemDeltaDataColumn(String s1, String s2, int i, Color clr, Comparator<StarSystem> c, int a) {
            headerKey = s1;
            attributeKey = s2;
            width = scaled(i);
            color = clr;
            comp = c;
            align = a;
            deltaKey = concat("DELTA_", attributeKey);
        }
        public boolean enabled()  { return comp != null; }
        @Override
        public void click() {
            super.click();
            if (enabled()) {
                reversed = !reversed;
                Collections.sort(systems(), comp);
                if (reversed)
                    Collections.reverse(systems());
                selectedSystem(selectedSystem(), true);
            }
        }
        @Override
        public void draw(Graphics g, RowSprite row, StarSystem sys, int x, int y, int w) {
            super.draw(g, row, sys, x, y, w);
            String val1 = sys.getAttribute(attributeKey);
            String val2 = sys.getAttribute(deltaKey);
            // remove leading - from negative deltas
            int delta = 0; try { delta = Integer.parseInt(val2); } catch (NumberFormatException e) {}
            val2 = delta < 0 ? str(Math.abs(delta)) : val2;

            int sw1 = g.getFontMetrics().stringWidth(val1);
            int sw2 = g.getFontMetrics().stringWidth(val2);
            g.setColor(color);
            int w1 = w*2/3;
            int w2 = w - w1;
            int x0 = x+w1;
            int yb = y-s5;
            switch(align) {
                case LEFT:    g.drawString(val1, x+s5, yb); break;
                case RIGHT:   g.drawString(val1, x+w1-s10-sw1, yb); break;
                case CENTER:  g.drawString(val1, x+((w1-sw1)/2), yb); break;
            }
            if (delta != 0) {
                int x1 = 0;
                int x2 = 0;
                switch(align) {
                    case LEFT:
                        x2 = x0+s5;
                        x1 = x2+s11;
                        break;
                    case RIGHT:
                        x1 = x0+w2-s5-sw2;
                        x2 = x1-s11;
                        break;
                    case CENTER:
                        x1 = x0+((w2-sw2)/2);
                        x2 = x1-s11;
                        break;
                }
                Color c;
                if (sys == selectedSystem())
                    c = delta < 0 ? palette.red : palette.forest;
                else
                    c = delta < 0 ? palette.red : palette.green;

                g.setColor(c);
                g.drawString(val2, x1, yb);

                // arrow (up or down)
                int xHalfHead=s10/2;
                int xHalfTail=s10/5;
                int xMid=x2+xHalfHead;
                int sh=s14-xHalfHead;
                px[0]=xMid-xHalfHead; px[1]=xMid+xHalfHead; px[2]=xMid;
                if (delta < 0) {
                    py[0]=yb-xHalfHead; py[1]=yb-xHalfHead; py[2]=yb;
                    g.fillRect(xMid-xHalfTail, yb-sh-xHalfHead, xHalfTail*2, sh);
                }
                else {
                    py[0]=yb-sh+1; py[1]=yb-sh+1; py[2]=yb-sh-xHalfHead;
                    g.fillRect(xMid-xHalfTail, yb-sh, xHalfTail*2, sh);
                }
                g.fillPolygon(px, py, 3);
            }
        }
    }
    public abstract class Sprite {
        public void exit()   { hoveringSprite = null; }
        public void enter()  { hoveringSprite = this; }
        public void click()  { }
        public abstract boolean isSelectableAt(int x, int y);
        public boolean equalsSprite(Sprite s)  { return this == s; }
        public StarSystem system()             { return null; }
    }
    public class HeaderSprite extends Sprite {
        Column column;
        public HeaderSprite(Column col) {
            column = col;
        }
        @Override
        public boolean equalsSprite(Sprite s)  { return (s instanceof HeaderSprite) && (((HeaderSprite) s).column == column); }
        @Override
        public boolean isSelectableAt(int x, int y) {
            return (x >= column.x)
                && (x <= (column.x+column.width))
                && (y <= column.y)
                && (y >= column.y-rowHeight());
        }
        @Override
        public void enter() {
            super.enter();
            if (hoveringHeader != column) {
                hoveringHeader = column;
                redrawHeaders = true;
            }
        }
        @Override
        public void exit()  {
            super.exit();
            hoveringHeader = null;
            redrawHeaders = true;
        }
        @Override
        public void click() { column.click(); }
    }
    public class RowSprite extends Sprite {
        StarSystem system;
        List<SystemButton> buttons;
        int x, y, w, h;
        RowSprite(StarSystem sys, List<SystemButton> btns, int x0, int y0, int w0, int h0) {
            system = sys;
            buttons = btns;
            x = x0;
            y = y0;
            w = w0;
            h = h0;
        }
        @Override
        public StarSystem system()             { return system; }
        void addButton(SystemButton b) {
            if (!buttons.contains(b))
                buttons.add(b);
        }
        SystemButton getButton(int id) {
            for (SystemButton b: buttons) {
                if (b.id() == id)
                    return b;
            }
            return null;
        }
        @Override
        public boolean isSelectableAt(int x0, int y0) {
            return (x0 >= x)
                && (x0 <= x+w)
                && (y0 <= y)
                && (y0 >= y-h);
        }
        @Override
        public void enter() {  }
        @Override
        public void exit()  {  }
        @Override
        public void click() { selectedSystem(system, true); }
    }
    interface SystemButton {
        void reset();
        int id();
        boolean contains(int x, int y);
        default boolean wantsMouseRelease()    { return true; }
        default boolean wantsMouseWheel()      { return false; }
        default void mouseReleased(StarSystem s, MouseEvent e) { }
        default void mouseWheelMoved(StarSystem s, MouseWheelEvent e) { }
    }
    class TransportSliderButton extends Rectangle implements SystemButton {
        private static final long serialVersionUID = 1L;
        @Override
        public void mouseReleased(StarSystem sys, MouseEvent e) {
            if (e.getButton() > 3)
                return;
            int maxSendingSize = player().sv.maxTransportsToSend(sys.id);
            float pct = (float) (e.getX() -x) / width;
            int newAmt = max(0, (int) Math.ceil(pct*(maxSendingSize+1))-1);
            int oldAmt = sys.transportSprite().amt();
            if (oldAmt != newAmt) {
                softClick();
                sys.transportSprite().amt(newAmt);
                repaint();
            }
        }
        @Override
        public void reset() { setBounds(0,0,0,0); }
        @Override
        public int id()     { return TRANSPORT_SLIDER; }
        @Override
        public boolean contains(int x, int y)  { return super.contains(x,y); }
        @Override
        public boolean wantsMouseWheel()       { return true; }
        @Override
        public void mouseWheelMoved(StarSystem sys, MouseWheelEvent e) {
            int rot = e.getWheelRotation();
            if (rot > 0)
                sys.transportSprite().decrement(rot);
            else if (rot  < 0)
                sys.transportSprite().increment(-rot);
        }
    }
    class TransportDecrementButton extends Polygon implements SystemButton {
        private static final long serialVersionUID = 1L;
        @Override
        public void mouseReleased(StarSystem sys, MouseEvent e) {
            if (e.getButton() > 3)
                return;
            if (sys.transportSprite().decrement(1)) {
                softClick();
                repaint();
            }
            else
                misClick();
        }
        @Override
        public void reset() { super.reset(); }
        @Override
        public int id()     { return TRANSPORT_DECREMENT; }
        @Override
        public boolean contains(int x, int y)  { return super.contains(x,y); }
    }
    class TransportIncrementButton extends Polygon implements SystemButton {
        private static final long serialVersionUID = 1L;
        @Override
        public void mouseReleased(StarSystem sys, MouseEvent e) {
            if (e.getButton() > 3)
                return;
            if (sys.transportSprite().increment(1)) {
                softClick();
                repaint();
            }
            else
                misClick();
        }
        @Override
        public void reset() { super.reset(); }
        @Override
        public int id()     { return TRANSPORT_INCREMENT; }
        @Override
        public boolean contains(int x, int y)  { return super.contains(x,y); }
    }
    class TransportStopButton extends Rectangle implements SystemButton {
        private static final long serialVersionUID = 1L;
        @Override
        public void mouseReleased(StarSystem sys, MouseEvent e) {
            if (e.getButton() > 3)
                return;
            sys.transportSprite().clear();
            softClick();
            repaint();
        }
        @Override
        public void reset() { setBounds(0,0,0,0); }
        @Override
        public int id()     { return TRANSPORT_STOP; }
        @Override
        public boolean contains(int x, int y)  { return super.contains(x,y); }
    }
}