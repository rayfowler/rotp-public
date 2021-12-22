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
package rotp.ui.tech;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import rotp.model.empires.Empire;
import rotp.model.tech.Tech;
import rotp.model.tech.TechCategory;
import rotp.ui.BasePanel;
import rotp.ui.main.SystemPanel;

public class SelectNewTechUI extends BasePanel implements MouseListener, MouseMotionListener, MouseWheelListener, ActionListener {
    private static final long serialVersionUID = 1L;
    private static final Color darkBrown = new Color(112,85,68);
    private static final Color darkBrownShade = new Color(112,85,68,128);
    private static final Color grayBrown = new Color(190,157,134);
    private static final Color scrollBarC = new Color(211,166,125);
    static Color grayShade = new Color(123,123,123,160);
    static Color dimWhite = new Color(225,225,255);
    static Color gray2 = new Color(123,123,123);
    static Color grayLight = new Color(148,148,148);
    static Color grayDark = new Color(64,64,64);

    private TechCategory category;
    private String hoverTech;

    HashMap<String, Rectangle> techBoxes = new HashMap<>();
    Rectangle techListBox = new Rectangle();
    List<String> availableTechs;

    Shape hoverShape;
    Rectangle selectTechBox = new Rectangle();

    int techsY, techsYMax;
    int dragY;
    Rectangle contactsListBox = new Rectangle();
    Rectangle contactsScroller = new Rectangle();

    int techIndex = 0;
    int talkTimeMs = 5000;
    long startTimeMs;

    int MIN_LIST_SIZE = 6;
    int MAX_LIST_SIZE = 10;
    boolean finished = false;

    public SelectNewTechUI() {
        init();
    }
    private void init() {
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
    }
    @Override
    public void paintComponent(Graphics g) {
        Image img = paintToImage();
        if (img != null)
            g.drawImage(img,0,0,null);
    }
    @Override
    public void animate() {
        if (!playAnimations())
            return;
        repaint();
    }
    @Override
    public String ambienceSoundKey() { return "ResearchAmbience"; }

    public void category(TechCategory c)  {
        player().race().resetScientist();
        category = c;
        techIndex = 0;
        availableTechs = category.techIdsAvailableForResearch();
        Collections.sort(availableTechs, Tech.LEVEL);
        hoverTech = availableTechs.get(0);
        startTimeMs = System.currentTimeMillis();
        dragY = 0;
        techsY = 0;
        finished = false;
        repaint();
    }
    public TechCategory category()        { return category; }

    private Image paintToImage() {
        if (finished)
            return null;
        techBoxes.clear();

        Empire pl = player();
        int bdr = s10;
        int techHeight = s25;
        int titleLineH = s26;
        String addlDesc = text("TECH_TOP_TIER_DETAIL");
        String obsDesc = text("TECH_OBSOLETE_DETAIL");
        String title = text(category.researchKey());

        BufferedImage backImg = player().race().laboratory();
        boolean talking = (System.currentTimeMillis() - startTimeMs) < talkTimeMs;
        Image raceImage = talking ? player().race().scientistTalking() : player().race().scientistQuiet();

        int w = getWidth();
        int h = getHeight();
        Image screenImg = createImage(w, h);

        //paint background
        Graphics2D g = (Graphics2D) screenImg.getGraphics();
        super.paintComponent(g);

        // draw background image
        g.drawImage(backImg, 0,0, w, h, 0, 0, backImg.getWidth(), backImg.getHeight(), null);

        // draw race
        g.drawImage(raceImage, 0,0, w, h, 0, 0, raceImage.getWidth(this), raceImage.getHeight(this), null);

        int techDisplaySize = availableTechs.size();
        int rightM = techDisplaySize > 9 ? s50 : s100;

        // get width of text box
        int boxLeftX = (getWidth()/2)+s100;
        int boxRightX = getWidth() - rightM;
        int boxWidth = boxRightX - boxLeftX;

        // break title into multiple lines based on width
        g.setFont(narrowFont(22));
        List<String> titleLines = wrappedLines(g, title, boxWidth-s60);
        int titleH = titleLines.size()*titleLineH;
        int footerH = 0;
        List<String> footerLines = null;
        if (availableTechs.size() > 1) {
            g.setFont(narrowFont(14));
            String changeDesc = text("TECH_CAN_CHANGE");
            footerLines = wrappedLines(g, changeDesc, boxWidth-s60);
            footerH = (footerLines.size()*s16);
        }

        // calculate vertical size of text box
        int boxHeight = min(h-s30, s20+titleH+s20+max(s100+s100, s40*min(15,techDisplaySize))+footerH);
        int boxTopY = min(scaled(200), (h-boxHeight)*2/3);
        int boxBottomY = boxHeight+boxTopY;
        
        // draw main box with shaded borders
        g.setColor(darkBrownShade);
        g.fillRect(boxLeftX-bdr, boxTopY-bdr, boxWidth+bdr+bdr, boxHeight+bdr+bdr);
        g.setColor(darkBrown);
        g.fillRect(boxLeftX, boxTopY, boxWidth, boxHeight);

        // draw title for main box
        int y0 = boxTopY;
        int x0 = boxLeftX+s20;
        g.setFont(narrowFont(22));
        for (String titleLine: titleLines) {
            y0 += titleLineH;
            drawShadowedString(g, titleLine, 4, x0, y0, SystemPanel.textShadowC, dimWhite);
        }
        
        if (footerH > 0) {
            g.setFont(narrowFont(14));
            int y1 = boxBottomY-footerH-s20;
            g.setColor(dimWhite);
            for (String line: footerLines) {
                y1 += s16;
                drawString(g,line, x0, y1);
            }
        }

        // draw dark background for list of techs
        int w0 = boxWidth-s40;
        int y0b = y0+s20;
        int listH = boxBottomY-y0-s40-footerH;
        g.setColor(AllocateTechUI.tierBackC);
        techListBox.setBounds(x0, y0b, w0, listH);
        g.fill(techListBox);
        g.setClip(techListBox);
        int y1 = y0b +s5 - techsY;
        int x1 = x0 + s10;
        int w1 = w0 - s10;
        int h1 = listH;
        int fullListH = s5;
        int techWidth = boxWidth-s45;

        g.setFont(narrowFont(20));

        // limit tech list size to 10, starting at tech index     
        for (int i=0;i<techDisplaySize;i++) {
            String id = availableTechs.get(i+techIndex);
            Tech t = tech(id);
            boolean topTier = t.quintile() == category.maxResearchableQuintile();
            boolean obsolete = t.isObsolete(pl);
            String detail = t.detail();
            if (topTier)
                detail = detail + " "+addlDesc;
            else if (obsolete)
                detail = detail + " "+obsDesc;
            g.setFont(narrowFont(14));
            List<String> lines = wrappedLines(g, detail, techWidth-s20);
             if (id.equals(hoverTech)) 
                g.setColor(AllocateTechUI.currentTechC);
            else
                g.setColor(darkBrown);
            
            int th = techHeight+((lines.size())*s15);
            g.fillRoundRect(x1-s5, y1, techWidth-s5, th, s10, s10);
            techBoxes.put(id, new Rectangle(x1, y1, techWidth, th));
            if (obsolete)
                g.setColor(grayBrown);
            else
                g.setColor(dimWhite);
            g.setFont(narrowFont(16));
            drawString(g,text(t.name()),x1+5, y1+s15);

            float techCost = category.costForTech(t);
            String cost = text("TECH_CHOOSE_RESEARCH_COST", shortFmt(techCost));
            int sw1 = g.getFontMetrics().stringWidth(cost);
            drawString(g,cost, boxRightX-s35-sw1, y1+s15);
            int y2 = y1 + s17;
            g.setColor(Color.black);
            g.setFont(narrowFont(14));
            for (String line: lines) {
                y2 += s15;
                drawString(g,line, x1+s5, y2);
            }
            y1 += th;
            y1 += s5;
            fullListH = fullListH + th + s5;
        }
        g.setClip(null);
        
        techsYMax = max(0, fullListH-listH);
        if (techsYMax == 0)
            contactsScroller.setBounds(0,0,0,0);
        else {
            int scrollW = s12;
            int scrollH = (int) ((float)listH*listH/(listH+techsYMax));
            int scrollX = x1+w1;
            int scrollY =(int) (y0b+s5+(float)listH*techsY/(techsYMax+listH));
            g.setColor(AllocateTechUI.tierBackC);
            
            g.fillRect(scrollX, y0b, scrollW+s3, listH);
            g.setClip(scrollX,y0b+s5,scrollW,h1-s8);
            g.setColor(scrollBarC);
            g.fillRect(scrollX, scrollY, scrollW, scrollH);
            contactsScroller.setBounds(scrollX, scrollY, scrollW, scrollH);
            if (hoverShape == contactsScroller) {
                Stroke prev = g.getStroke();
                g.setColor(Color.yellow);
                g.setStroke(stroke2);
                g.drawRect(scrollX, scrollY, scrollW, scrollH);
                g.setStroke(prev);
            }
        }
        g.setClip(null);      
        return screenImg;
    }
    private void scrollList(int i) {
        int maxIndex = max(availableTechs.size()-MAX_LIST_SIZE, 0);
        int hoverIndex = availableTechs.indexOf(hoverTech);

        int newTechIndex = techIndex+i;
        if (newTechIndex < 0)
            newTechIndex = 0;
        else if (newTechIndex > maxIndex)
            newTechIndex = maxIndex;

        int newHoverIndex = hoverIndex+i;
        if (newHoverIndex < 0)
            newHoverIndex = 0;
        else if (newHoverIndex > availableTechs.size()-1)
            newHoverIndex = availableTechs.size()-1;


        if ((newTechIndex != techIndex)
        || (newHoverIndex != hoverIndex)) {
            hoverTech = availableTechs.get(newHoverIndex);
            techIndex = newTechIndex;
            repaint();
        }
    }
    public float scaleFactor(int imgWidth, int imgHeight, int uiWidth, int uiHeight) {
        float widthRatio = (float)uiWidth/imgWidth;
        float heightRatio = (float)uiHeight/imgHeight;

        if (widthRatio > 1)
            return min(widthRatio, heightRatio);
        else
            return max(widthRatio, heightRatio);
    }
    private void selectTech(String id) {
        if (id.isEmpty())
            return;
        Tech t = tech(id);
        if (!category().currentTech(t))
            return;
        
        finished = true;
        log("Tech: ", t.name(), " selected for research");
        repaint();
        session().resumeNextTurnProcessing();
    }
    private void mouseAt(int x, int y) {
        Shape prevHover = hoverShape;
        hoverShape = null;
        hoverTech = "";

        if (techListBox.contains(x,y)) {
            for (String t: techBoxes.keySet()) {
                Rectangle r = techBoxes.get(t);
                if (r.contains(x, y)) {
                    hoverShape = r;
                    hoverTech = t;
                }
            }
        }
        if (contactsScroller.contains(x,y)) 
            hoverShape = contactsScroller;
 
        if (hoverShape != prevHover)
            repaint();        
    }
    @Override
    public void mouseClicked(MouseEvent e) { }
    @Override
    public void mouseEntered(MouseEvent e) { }
    @Override
    public void mouseExited(MouseEvent e) { }
    @Override
    public void mousePressed(MouseEvent e) {   
        dragY = e.getY();
    }
    @Override
    public void mouseReleased(MouseEvent e) {
        if ((e.getButton() > 3) || e.getClickCount() > 1)
            return;
        dragY = 0;
        if (hoverShape == null)
            return;
        if (!hoverTech.isEmpty()) {
            softClick();
            selectTech(hoverTech);
            return;
        }
    }
    @Override
    public void mouseDragged(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        int dY = y-dragY;
        dragY = y;
        if (contactsScroller == hoverShape) {
            if ((y >= contactsListBox.y) || (y <= (contactsListBox.y+techListBox.height))) { 
                int h = (int) techListBox.getHeight();
                int dListY = (int)((float)dY*(h+techsYMax)/h);
                if (dY < 0)
                    techsY = max(0,techsY+dListY);
                else 
                    techsY = min(techsYMax,techsY+dListY);
            }
            repaint(techListBox);
            return;
        }
    }
    @Override
    public void mouseMoved(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        
        if (finished)
            return;

        mouseAt(x,y);
    }
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        int count = e.getUnitsToScroll();
        int x = e.getX();
        int y = e.getY();
        int prevY = techsY;
        if (count < 0)
            techsY = max(0,techsY-s10);
        else 
            techsY = min(techsYMax,techsY+s10);
        if (techsY != prevY) {
            repaint(techListBox);
            mouseAt(x,y);
        }
    }
    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        int prevY = techsY;
        switch(k){
            case KeyEvent.VK_UP: scrollList(-1); return;
            case KeyEvent.VK_DOWN: scrollList(1); return;
            case KeyEvent.VK_PAGE_UP: techsY = techsY = max(0,techsY-s100);; break;
            case KeyEvent.VK_PAGE_DOWN: techsY = min(techsYMax,techsY+s100); break;
            case KeyEvent.VK_S:
            case KeyEvent.VK_ENTER:
                selectTech(hoverTech); return;
        }
        if (techsY != prevY) 
            repaint(techListBox);
    }
}
