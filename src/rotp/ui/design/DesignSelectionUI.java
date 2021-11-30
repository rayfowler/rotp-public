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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import rotp.model.ships.ShipComponent;
import rotp.model.ships.ShipDesign;
import rotp.ui.BasePanel;
import rotp.ui.main.SystemPanel;

public abstract class DesignSelectionUI extends BasePanel implements MouseListener, MouseMotionListener, MouseWheelListener {
    private static final long serialVersionUID = 1L;
    static Color frameC = new Color(104,104,104);
    static Color textBackgroundC = new Color(20,20,20);
    static Color textHoverBackgroundC = new Color(97,0,0);

    private static final Color edgeC = new Color(112,85,68, 128);
    private static final Color brownC = new Color(112,85,68);
    private static final Color backC = new Color(140,101,76);
    private static final Color focusBackC = new Color(212,166,125);
    static Color titleC = new Color(65,220,26);
    static Color titleCHi = Color.yellow;
    static Color textC = Color.gray;
    static Color textCHi = Color.white;
    static Color backgroundHaze = new Color(0,0,0,160);

    Polygon upArrow = new Polygon();
    Polygon downArrow = new Polygon();
    int hoveringArrow = 0;
    Polygon hoverArrow = new Polygon();
    private final int upArrowX[] = new int[7];
    private final int upArrowY[] = new int[7];
    private final int downArrowX[] = new int[7];
    private final int downArrowY[] = new int[7];
    private final Color arrowColorHi = new Color(248,127,0);
    private final Color arrowColorLo = Color.lightGray;

    HashMap<Rectangle, ShipComponent> spriteMap = new HashMap<>();
    List<Rectangle> selectionBoxes = new ArrayList<>();
    List<Rectangle> sortingBoxes = new ArrayList<>();
    List<ShipComponent> components = new ArrayList<>();

    int startIndex = 0;
    int hoverComp = -1;
    int hoverHeader = -1;
    String title;
    List<String> headers = new ArrayList<>();
    List<Integer> columnWidths = new ArrayList<>();

    int MAX_LIST_SIZE = 30;
    int numToDisplay = 0;
    int sortColumn = -1;
    boolean reverseSort = false;
    public ShipDesign selectedDesign;

    abstract String title();
    abstract int numColumns();
    abstract List<? extends ShipComponent> baseComponents();
    abstract ShipComponent selectedComponent();
    abstract String header(int i);
    abstract void select(int compNum);

    int minimumWidth(int column)   { return 50; }
    int minimumSpacing()           { return 20; }
    int alignment(int column)      { return JLabel.LEFT; }
    int bank()                     { return 0; }

    @Override
    public void disableGlassPane() { 
        clearSettings();
        super.disableGlassPane();
    }

    public int numComponents()             { return components().size(); }
    public boolean isSelected(int compNum) { return components().get(compNum) == selectedComponent(); }
    public String value(int compNum, int fieldNum, int bank) {
            return components().get(compNum).fieldValue(fieldNum, selectedDesign, bank);
    }
    public DesignSelectionUI() {
        init();
    }
    private void init() {
        setOpaque(false);
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
    }
    public List<ShipComponent> components() {
        if (components.isEmpty())
            components.addAll(baseComponents());

        return components;
    }
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w = getWidth();
        int h = getHeight();

        Font headerFont = narrowFont(20);
        Font textFont = narrowFont(18);
        int itemH = s20;
        title = title();
        columnWidths.clear();
        headers.clear();
        selectionBoxes.clear();
        sortingBoxes.clear();
        for (int i=0;i<numColumns();i++)
                headers.add(header(i));

        // determine detail line width based on headers
        // min 30px per column, plus 20px between
        int itemW = 0;
        g.setFont(headerFont);
        for (int i=0;i<numColumns();i++) {
            String s = headers.get(i);
            int colW =  Math.max(scaled(minimumWidth(i)), g.getFontMetrics().stringWidth(s)+scaled(minimumSpacing()));
            columnWidths.add(colW);
            itemW += colW;
        }

        numToDisplay = min(MAX_LIST_SIZE, numComponents());
        int topLabelH = s40;
        int bottomLabelH = s40;
        int barWidthL = s10;
        int barWidthR = s10;
        if (numComponents() > MAX_LIST_SIZE)
            barWidthR += s15;   // make room for scroll arrows
        int listMargin = s10;
        int listH = (itemH*max(2, numToDisplay))+listMargin;
        int boxH = topLabelH+listH+bottomLabelH;
        int boxW = itemW+barWidthL+barWidthR;
        int boxX = (w-boxW)/2;
        int boxY = (h-boxH)/3;

        // draw background "haze"
        g.setColor(backgroundHaze);
        g.fillRect(0, 0, w, h);

        // draw border
        g.setColor(edgeC);
        g.fillRect(boxX-s10,boxY-s10,boxW+s20,boxH+s20);

        // draw outer background
        g.setColor(brownC);
        g.fillRect(boxX,boxY,boxW,boxH);


        // draw title 
        g.setFont(font(26));
        int sw = g.getFontMetrics().stringWidth(title);
        int x1 = (w-sw)/2;
        int y1 = boxY+s30;
        drawShadowedString(g,title,3,x1,y1,SystemPanel.textShadowC, SystemPanel.whiteText);

        // draw list background
        int x2 = boxX+barWidthL;
        int y2 = y1+s32;
        g.setColor(backC);
        g.fillRect(x2, y2, itemW, listH);

        // for each item, draw header and items, and save hover info
        g.setFont(headerFont);
        int x3 = x2+listMargin;
        int y3 = y1+s30;
        for (int i=0;i<headers.size();i++) {
            String s = headers.get(i);
            int align = alignment(i);
            int colW = columnWidths.get(i)-s20;
            int valW = g.getFontMetrics().stringWidth(s);
            if (i == hoverHeader)
                g.setColor(SystemPanel.blackText);
            else
                g.setColor(SystemPanel.blackText);
            Rectangle rect;
            switch (align) {
                case JLabel.CENTER:
                    rect = new Rectangle(x3+((colW-valW)/2),y3-itemH,valW,itemH);
                    drawString(g,s, x3+((colW-valW)/2), y3-s2);
                    break;
                case JLabel.RIGHT:
                    rect = new Rectangle(x3+colW-valW,y3-itemH,valW,itemH);
                    drawString(g,s, x3+colW-valW, y3-s2);
                    break;
                default: 
                    rect = new Rectangle(x3,y3-itemH,valW,itemH);
                    drawString(g,s, x3, y3-s2);
                    break;
            }
            sortingBoxes.add(rect);
            x3 += columnWidths.get(i);
        }

        y3 = y3+s5;

        g.setFont(textFont);
        for (int i=0;i<numToDisplay;i++) {
            int compNum = i+startIndex;
            x3 = x2+listMargin;
            if (isSelected(compNum)) {
                g.setColor(focusBackC);
                g.fillRect(x2, y3+s3, itemW, itemH);
            }
            selectionBoxes.add(new Rectangle(x2,y3+s3,itemW,itemH));
            y3 += itemH;
            g.setColor(SystemPanel.blackText);
            for (int fieldNum=0;fieldNum<numColumns();fieldNum++) {
                String val = value((compNum), fieldNum, bank());
                int align = alignment(fieldNum);
                int colW = columnWidths.get(fieldNum)-s20;
                int valW = g.getFontMetrics().stringWidth(val);
                switch (align) {
                    case JLabel.CENTER:
                        drawString(g,val, x3+((colW-valW)/2), y3-4);
                        break;
                    case JLabel.RIGHT:
                        drawString(g,val, x3+colW-valW, y3-4);
                        break;
                    default: 
                        drawString(g,val, x3, y3-4);
                        break;
                }
                x3 += columnWidths.get(fieldNum);
            }
        }

        if (numComponents() > MAX_LIST_SIZE)
            drawScrollArrows(g, boxX+boxW-s12, y2,boxY+boxH-bottomLabelH);
    }
    private void drawScrollArrows(Graphics g, int x, int y0, int y1) {
        // all start with arrow point, then work around the left
        upArrowX[0] = x;       upArrowY[0] = y0;
        upArrowX[1] = x-s10;   upArrowY[1] = y0+s20;
        upArrowX[2] = x-s5;    upArrowY[2] = y0+s20;
        upArrowX[3] = x-s5;    upArrowY[3] = y0+s40;
        upArrowX[4] = x+s5;    upArrowY[4] = y0+s40;
        upArrowX[5] = x+s5;    upArrowY[5] = y0+s20;
        upArrowX[6] = x+s10;   upArrowY[6] = y0+s20;
        upArrow = new Polygon(upArrowX, upArrowY, upArrowX.length);

        downArrowX[0] = x;     downArrowY[0] = y1;
        downArrowX[1] = x-s10; downArrowY[1] = y1-s20;
        downArrowX[2] = x-s5;  downArrowY[2] = y1-s20;
        downArrowX[3] = x-s5;  downArrowY[3] = y1-s40;
        downArrowX[4] = x+s5;  downArrowY[4] = y1-s40;
        downArrowX[5] = x+s5;  downArrowY[5] = y1-s20;
        downArrowX[6] = x+s10; downArrowY[6] = y1-s20;
        downArrow = new Polygon(downArrowX, downArrowY, downArrowX.length);

        if (hoveringArrow == 1)
            g.setColor(arrowColorHi);
        else
            g.setColor(arrowColorLo);
        g.fillPolygon(upArrow);


        if (hoveringArrow == 2)
            g.setColor(arrowColorHi);
        else
            g.setColor(arrowColorLo);
        g.fillPolygon(downArrow);

    }
    private void scrollDown() {
        int index = components().indexOf(selectedComponent());
        int bottomIndex = startIndex + MAX_LIST_SIZE;
        if (index < 0) 
            return;

        select(min(index+1, numComponents()-1));
        if (bottomIndex < numComponents()) 
            startIndex++;

        repaint();
    }
    private void scrollUp() {
        int index = components().indexOf(selectedComponent());
        if (index <= 0) 
            return;

        select(index-1);
        if (startIndex > 0) 
            startIndex--;

        repaint();
    }
    private void sortByColumn(int col) {
        reverseSort = !reverseSort;
        sortColumn = col;

        ShipComponent.FLD_DESIGN = selectedDesign;
        ShipComponent.FLD_NUM = col;
        Collections.sort(components(), ShipComponent.FIELD_VALUE);
        if (reverseSort)
            Collections.reverse(components());
        repaint();
    }

    @Override
    public void mouseClicked(MouseEvent arg0) {}
    @Override
    public void mouseEntered(MouseEvent arg0) {}
    @Override
    public void mouseExited(MouseEvent arg0) {}
    @Override
    public void mousePressed(MouseEvent arg0) {}
    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() > 3)
            return;
        boolean rightClick = SwingUtilities.isRightMouseButton(e);
        if (rightClick) {
            disableGlassPane();
            return;
        }
        if (hoveringArrow == 1) {
            scrollUp();
            return;
        }
        else if (hoveringArrow == 2) {
            scrollDown();
            return;
        }
        if (hoverComp != -1) {
            softClick();
            select(hoverComp);
            disableGlassPane();
            return;			
        }
        if (hoverHeader != -1) {
            softClick();
            sortByColumn(hoverHeader);
            return;
        }
    }
    private void clearSettings() {
        components.clear();
        sortColumn = -1;
        reverseSort = false;
        startIndex = 0;
    }
    @Override
    public void mouseDragged(MouseEvent e) {}
    @Override
    public void mouseMoved(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();

        boolean needRepaint = false;
        int prevHover = hoveringArrow;
        hoveringArrow = 0;
        if (upArrow.contains(x, y)) 
            hoveringArrow = 1;
        else if (downArrow.contains(x,y)) 
            hoveringArrow = 2;

        if (prevHover != hoveringArrow) 
            needRepaint = true;

        prevHover = hoverComp;
        hoverComp = -1;
        for (int compNum=0;compNum<numToDisplay;compNum++) {
            Rectangle box = selectionBoxes.get(compNum);
            if (box.contains(x, y)) {
                hoverHeader = -1;
                hoverComp = compNum+startIndex;
            }
        }
        if (prevHover != hoverComp)
            needRepaint = true;

        prevHover = hoverHeader;
        hoverHeader = -1;
        for (int i=0;i<sortingBoxes.size();i++) {
            Rectangle box = sortingBoxes.get(i);
            if (box.contains(x, y)) {
                hoverHeader = i;
                hoverComp = -1;
            }
        }
        if (prevHover != hoverHeader)
            needRepaint = true;

        if (needRepaint)
            repaint();
    }
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (e.getWheelRotation() < 0)
            scrollUp();
        else
            scrollDown();
    }
    @Override
    public void keyPressed(KeyEvent e) {
        switch(e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
            case KeyEvent.VK_ENTER:
                softClick();
                disableGlassPane();
                break;
            case KeyEvent.VK_UP:
                scrollUp();
                break;
            case KeyEvent.VK_DOWN:
                scrollDown();
                break;
            case KeyEvent.VK_HOME:
                while(startIndex > 0)
                    scrollUp();
                break;
            case KeyEvent.VK_END:
                while(startIndex < numComponents() - MAX_LIST_SIZE)
                    scrollDown();
                break;
        }
    }
}
