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
import java.awt.LinearGradientPaint;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import rotp.model.tech.Tech;
import rotp.model.tech.TechCategory;
import rotp.ui.BasePanel;
import rotp.ui.main.SystemPanel;

public class SelectNewTechUI extends BasePanel implements MouseListener, MouseMotionListener, MouseWheelListener, ActionListener {
    private static final long serialVersionUID = 1L;
    static Color grayShade = new Color(123,123,123,160);
    static Color dimWhite = new Color(225,225,255);
    static Color gray2 = new Color(123,123,123);
    static Color grayLight = new Color(148,148,148);
    static Color grayDark = new Color(64,64,64);

    private TechCategory category;
    private String hoverTech;

    HashMap<String, Rectangle> techBoxes = new HashMap<>();
    List<String> availableTechs;

    Rectangle hoverBox;
    Rectangle selectTechBox = new Rectangle();
    private LinearGradientPaint greenBackC;

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
        finished = false;
        techIndex = 0;
        availableTechs = category.techIdsAvailableForResearch();
        Collections.sort(availableTechs, Tech.LEVEL);
        hoverTech = availableTechs.get(0);
        startTimeMs = System.currentTimeMillis();
        repaint();
    }
    public TechCategory category()        { return category; }

    private Image paintToImage() {
        techBoxes.clear();

        int bdr = s10;
        int techHeight = s20;
        int titleLineH = s26;
        int techListSize = max(MIN_LIST_SIZE, min(availableTechs.size(), MAX_LIST_SIZE));
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

        int sideMgn = scaled(130);
        // get width of text box
        int boxLeftX = (getWidth()/2)+sideMgn;
        int boxRightX = getWidth() - sideMgn;
        int boxWidth = boxRightX - boxLeftX;

        // break title into multiple lines based on width
        g.setFont(narrowFont(22));
        List<String> titleLines = wrappedLines(g, title, boxWidth-s60);
        int titleH = titleLines.size()*titleLineH;

        // calculate vertical size of text box
        int boxTopY = scaled(200);
        int boxBottomY = boxTopY+s20+titleH+s20+(techHeight*techListSize)+s20+s70;
        int boxHeight = boxBottomY - boxTopY;

        // draw main box with shaded borders
        g.setColor(grayShade);
        g.fillRect(boxLeftX-bdr, boxTopY-bdr, boxWidth+bdr+bdr, boxHeight+bdr+bdr);
        g.setColor(gray2);
        g.fillRect(boxLeftX, boxTopY, boxWidth, boxHeight);

        // draw title for main box
        int y0 = boxTopY;
        int x0 = boxLeftX+s20;
        g.setFont(narrowFont(22));
        for (String titleLine: titleLines) {
            y0 += titleLineH;
            drawShadowedString(g, titleLine, 4, x0, y0, SystemPanel.textShadowC, dimWhite);
        }

        // draw dark background for list of techs
        g.setColor(grayDark);
        g.fillRect(boxLeftX+s20, y0+s20, boxWidth-s40, (techHeight*techListSize)+s10);

        int y1 = y0 + s25;
        int x1 = x0 + s10;
        int techWidth = boxWidth-s50;

        g.setFont(narrowFont(20));

        int techDisplaySize = min(MAX_LIST_SIZE, availableTechs.size());
        // limit tech list size to 10, starting at tech index
        for (int i=0;i<techDisplaySize;i++) {
            String id = availableTechs.get(i+techIndex);
            Tech t = tech(id);
            if (id.equals(hoverTech)) {
                g.setColor(grayLight);
                g.fillRect(x1-s5, y1, techWidth-s5, techHeight);
            }
            techBoxes.put(id, new Rectangle(x1, y1, techWidth, techHeight));
            g.setColor(Color.black);
            g.setFont(narrowFont(17));
            g.drawString(text(t.name()),x1+5, y1+techHeight-s4);

            String cost = text("TECH_CHOOSE_RESEARCH_COST", (int)(t.researchCost()));
            int sw1 = g.getFontMetrics().stringWidth(cost);
            g.drawString(cost, boxRightX-s35-sw1, y1+techHeight-s4);
            y1 += techHeight;
        }

        // draw highlighted tech detail
        g.setFont(narrowFont(16));
        String detail = tech(hoverTech).detail();
        List<String> lines = wrappedLines(g, detail, techWidth);
        int y2 = boxBottomY-s100;
        g.setColor(Color.black);
        for (String line: lines) {
            y2 += s17;
            g.drawString(line, x1, y2);
        }

        int buttonW = scaled(150);
        int buttonX = boxRightX-s20-buttonW;
        int buttonH = s30;
        int buttonY = boxBottomY-buttonH-s10;
        // draw select button
        if (greenBackC == null) {
            Point2D start = new Point2D.Float(buttonX, 0);
            Point2D end = new Point2D.Float(buttonX+buttonW, 0);
            float[] dist = {0.0f, 0.5f, 1.0f};
            Color greenEdgeC = new Color(44,59,30);
            Color greenMidC = new Color(71,93,48);
            Color[] greenColors = {greenEdgeC, greenMidC, greenEdgeC };
            greenBackC = new LinearGradientPaint(start, end, dist, greenColors);
        }
        selectTechBox.setBounds(buttonX, buttonY, buttonW, buttonH);
        boolean hovering = hoverBox == selectTechBox;

        int cnr = s3;
        g.setColor(Color.darkGray);
        g.fillRoundRect(buttonX+s3, buttonY+s3, buttonW, buttonH, cnr, cnr);
        g.setPaint(greenBackC);
        g.fillRoundRect(buttonX, buttonY, buttonW, buttonH, cnr, cnr);

        if (hovering)
            g.setColor(Color.yellow);
        else
            g.setColor(Color.lightGray);
        Stroke prevStr = g.getStroke();
        g.setStroke(stroke2);
        g.drawRoundRect(buttonX, buttonY, buttonW, buttonH, cnr, cnr);
        g.setStroke(prevStr);

        if (hovering)
            g.setColor(Color.yellow);
        else
            g.setColor(Color.white);
        String buttonText = text("TECH_CONFIRM_CHOICE");
        g.setFont(narrowFont(20));
        int sw2 = g.getFontMetrics().stringWidth(buttonText);
        int x3 = buttonX+((buttonW-sw2)/2);
        g.drawString(buttonText, x3, buttonY+buttonH-s9);

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
    private String selectedTech(int x, int y) {
        for (String t: techBoxes.keySet()) {
            Rectangle r = techBoxes.get(t);
            if (r.contains(x, y))
                return t;
        }
        return null;
    }
    private void selectTech(String id) {
        Tech t = tech(id);
        category().currentTech(t);
        finished = true;
        log("Tech: ", t.name(), " selected for research");
        session().resumeNextTurnProcessing();
    }
    @Override
    public void mouseClicked(MouseEvent e) { }
    @Override
    public void mouseEntered(MouseEvent e) { }
    @Override
    public void mouseExited(MouseEvent e) { }
    @Override
    public void mousePressed(MouseEvent e) { }
    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() > 3)
            return;
        int x = e.getX();
        int y = e.getY();
        boolean repaint = false;
        int clickCount = e.getClickCount();

        if (selectTechBox.contains(x,y)) {
            selectTech(hoverTech);
            return;
        }
        String selectedTech = selectedTech(x,y);

        if (selectedTech != null) {
            softClick();
            if (clickCount >= 1) {
                if (!selectedTech.equals(hoverTech)) {
                    hoverTech = selectedTech;
                    repaint = true;
                }
            }
            if (clickCount == 2) {
                selectTech(hoverTech);
                return;
            }
        }

        if (repaint)
            repaint();
    }
    @Override
    public void mouseDragged(MouseEvent arg0) {}
    @Override
    public void mouseMoved(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();

        Rectangle prevHover = hoverBox;
        hoverBox = null;

        if (selectTechBox.contains(x,y))
            hoverBox = selectTechBox;

        if (hoverBox != prevHover)
            repaint();
    }
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        int count = e.getUnitsToScroll();
        if (count > 0)
            scrollList(1);
        else
            scrollList(-1);
    }
    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        switch(k){
            case KeyEvent.VK_UP: scrollList(-1); return;
            case KeyEvent.VK_DOWN: scrollList(1); return;
            case KeyEvent.VK_S:
            case KeyEvent.VK_ENTER:
                selectTech(hoverTech); return;
        }
    }
}
