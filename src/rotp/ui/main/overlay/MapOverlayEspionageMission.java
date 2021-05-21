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
package rotp.ui.main.overlay;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.List;
import rotp.Rotp;
import rotp.model.Sprite;
import rotp.model.empires.Empire;
import rotp.model.empires.EspionageMission;
import rotp.model.galaxy.Galaxy;
import rotp.model.tech.Tech;
import rotp.model.tech.TechCategory;
import rotp.ui.BasePanel;
import rotp.ui.RotPUI;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.main.MainUI;
import rotp.ui.main.SystemPanel;
import rotp.ui.sprites.TechCategorySprite;

public class MapOverlayEspionageMission extends MapOverlay {
    Color maskC  = new Color(40,40,40,160);
    Area mask;
    MainUI parent;
    BufferedImage labImg;
    private LinearGradientPaint grayBack1, grayBack2;
    private LinearGradientPaint greenBack1, greenBack2;
    private final Color grayEdgeC = new Color(59,59,59);
    private final Color grayMidC = new Color(93,93,93);
    private final Color greenEdgeC = new Color(44,59,30);
    private final Color greenMidC = new Color(70,93,48);

    private EspionageMission mission;
    private int empId;
    TechCategorySprite[] categorySprites = new TechCategorySprite[6];
    int techCategoryHoverButton = -1;
    boolean drawSprites = false;

    public MapOverlayEspionageMission(MainUI p) {
        parent = p;
        for (int i=0;i<categorySprites.length;i++)
            categorySprites[i] = new TechCategorySprite(this,i);
    }
    public boolean canSelect(int catNum) {
        return (mission.techChoice(TechCategory.id(catNum)) != null);
    }
    public void espionageCategorySelected() {
        if (drawSprites) {
            drawSprites = false;
            parent.clearOverlay();
            session().enableSpyReport();
            RotPUI.instance().selectStealTechPanel(mission, empId);
        }
    }
    public void init(EspionageMission esp, int id) {
        labImg = null;
        mission = esp;
        empId = id;
        techCategoryHoverButton = -1;
        drawSprites = true;
    }
    @Override
    public boolean drawSprites()   { return drawSprites; }
    @Override
    public boolean masksMouseOver(int x, int y)   { return true; }
    @Override
    public boolean hoveringOverSprite(Sprite o) {
        int prevHover = techCategoryHoverButton;
        techCategoryHoverButton = -1;
        if (o instanceof TechCategorySprite)
            techCategoryHoverButton = ((TechCategorySprite) o).categoryNum();
        if (prevHover != techCategoryHoverButton) {
            Graphics2D g = (Graphics2D) parent.map().getGraphics();
            parent.map().repaint();
            /*
            if (prevHover >= 0)
                paintTechCategoryButton(g, categorySprites[prevHover]);
            if (techCategoryHoverButton >= 0)
                paintTechCategoryButton(g, categorySprites[techCategoryHoverButton]);
            */
        }
        return true;
    }
    @Override
    public boolean handleKeyPress(KeyEvent e) {
        switch(e.getKeyCode()) {
            case KeyEvent.VK_1:
                if (this.canSelect(0))  
                    categorySprites[0].click(parent.map(), 1, false, true);
                break;
            case KeyEvent.VK_2:
                if (this.canSelect(1))
                    categorySprites[1].click(parent.map(), 1, false, true);
                break;
            case KeyEvent.VK_3:
                if (this.canSelect(2))
                    categorySprites[2].click(parent.map(), 1, false, true);
                break;
            case KeyEvent.VK_4:
                if (this.canSelect(3))
                    categorySprites[3].click(parent.map(), 1, false, true);
                break;
            case KeyEvent.VK_5:
                if (this.canSelect(4))
                    categorySprites[4].click(parent.map(), 1, false, true);
                break;
            case KeyEvent.VK_6:
                if (this.canSelect(5))
                    categorySprites[5].click(parent.map(), 1, false, true);
                break;
            default:
                misClick(); break;
        }
        return true;
    }
    @Override
    public void paintOverMap(MainUI parent, GalaxyMapPanel ui, Graphics2D g) {
        if (!drawSprites)
            return;
        
        int w = ui.getWidth()-scaled(150);
        int h = ui.getHeight()-BasePanel.s50;
        Galaxy gal = galaxy();
        Empire emp = gal.empire(empId);
        
        for (TechCategorySprite spr: categorySprites) {
            spr.espionage(mission);
            parent.addNextTurnControl(spr);
        }
        int s7 = BasePanel.s7;
        int s20 = BasePanel.s20;
        int s45 = BasePanel.s45;
        int s50 = BasePanel.s50;
        int s60 = BasePanel.s60;

        // draw map mask
        if (mask == null) {
            int r = s60;
            int centerX = w*2/5;
            int centerY = h*2/5;
            Ellipse2D window = new Ellipse2D.Float();
            window.setFrame(centerX-r, centerY-r, r+r, r+r);
            Area st1 = new Area(window);
            Rectangle blackout  = new Rectangle();
            blackout.setFrame(0,0,w,h);
            mask = new Area(blackout);
            mask.subtract(st1);
        }

        int catW = scaled(200);
        int catH = BasePanel.s46;
        int x0 = scaled(265);
        int y0 = scaled(205);
        int w0 = scaled(295)+(2*catW);
        int h0 = scaled(125)+(3*catH);
        g.setColor(MainUI.paneShadeC2);
        g.fillRect(x0, y0, w0, h0);

        int x1 = x0 + s7;
        int y1 = y0 + s7;
        int w1 = w0 - s7 - s7;
        int h1 = h0 - s7 - s7;
        g.setColor(MainUI.paneBackground);
        g.fillRect(x1, y1, w1, h1);

        // draw year/turn info
        String yearStr = displayYearOrTurn();
        g.setFont(narrowFont(40));
        int sw = g.getFontMetrics().stringWidth(yearStr);
        int leftW = scaled(250);
        int rightW = w1-leftW;
        int x1a = x1+((leftW-sw)/2);
        drawBorderedString(g, yearStr, 2, x1a, y1+s45, SystemPanel.textShadowC, SystemPanel.orangeText);

        // draw title
        String title = text("NOTICE_ESPIONAGE_TITLE");
        title = emp.replaceTokens(title, "alien");
        g.setFont(narrowFont(15));
        int x1b = x1+leftW;
        g.setColor(SystemPanel.blackText);
        drawString(g,title, x1b, y1+s20);

        // draw subtitle
        String subtitle = text("NOTICE_ESPIONAGE_SUBTITLE");
        g.setFont(narrowFont(22));
        drawShadowedString(g, subtitle, 3, x1b, y1+s50, SystemPanel.textShadowC, Color.white);

        // draw footer
        String footer = text("NOTICE_ESPIONAGE_FOOTER");
        g.setFont(narrowFont(15));
        int sw2 = g.getFontMetrics().stringWidth(footer);
        int x1c = x1+leftW+((rightW-sw2)/2);
        g.setColor(SystemPanel.blackText);
        drawString(g,footer, x1c, y1+h1-BasePanel.s10);

        Empire pl = player();
        if (labImg == null) {
            labImg = asBufferedImage(pl.race().laboratory());
            Graphics imgG = labImg.getGraphics();
            BufferedImage spyImg = pl.race().spyQuiet();
            int w0a = labImg.getWidth();
            int h0a = labImg.getHeight();
            imgG.drawImage(spyImg, w0a/2, 0, w0a, h0a, 0, 0, (spyImg.getWidth()/2), spyImg.getHeight(), null);
            imgG.dispose();
        }


        int y1b = y1+BasePanel.s60;
        int h1b = h1-BasePanel.s60;
        int imgH = h1b;
        int imgW = imgH*Rotp.IMG_W/Rotp.IMG_H;
        g.setClip(x1+BasePanel.s5,y1b,leftW-BasePanel.s10,h1b-BasePanel.s5);
        g.drawImage(labImg, x1+leftW-imgW, y1b, x1+leftW, y1b+h1b, 0, 0, labImg.getWidth(), labImg.getHeight(), null);
        g.setClip(null);
        
//        int imgW = leftW;
//        int imgH = imgW*Rotp.IMG_H/Rotp.IMG_W;
//        g.drawImage(labImg, x1, y1+h1-imgH, x1+leftW, y1+h1, 0, 0, labImg.getWidth(), labImg.getHeight(), null);

        // draw all 6 category boxes

        int catX1 = x1+scaled(260);
        int catX2 = catX1+catW+BasePanel.s5;
        int catY1 = y1+BasePanel.s65;
        int catY2 = catY1+catH+BasePanel.s7;
        int catY3 = catY2+catH+BasePanel.s7;

        if (grayBack1 == null) {
            float[] dist = {0.0f, 0.5f, 1.0f};
            Point2D start = new Point2D.Float(catX1, 0);
            Point2D end = new Point2D.Float(catX1+catW, 0);
            Color[] grays = { grayEdgeC, grayMidC, grayEdgeC };
            Color[] greens = { greenEdgeC, greenMidC, greenEdgeC };
            grayBack1 = new LinearGradientPaint(start, end, dist, grays);
            greenBack1 = new LinearGradientPaint(start, end, dist, greens);
            start = new Point2D.Float(catX2, 0);
            end = new Point2D.Float(catX2+catW, 0);
            grayBack2 = new LinearGradientPaint(start, end, dist, grays);
            greenBack2 = new LinearGradientPaint(start, end, dist, greens);
        }
        int[] catX = { catX1, catX2, catX1, catX2, catX1, catX2 };
        int[] catY = { catY1, catY1, catY2, catY2, catY3, catY3 };
        
        int hover = techCategoryHoverButton;
        
        // paint all of the non-hovering categories first
        if (hover != 0)
            paintTechCategoryButton(g, 0, catX1, catY1, catW, catH);
        if (hover != 1)
            paintTechCategoryButton(g, 1, catX2, catY1, catW, catH);

        if (hover != 2)
            paintTechCategoryButton(g, 2, catX1, catY2, catW, catH);
        if (hover != 3)
            paintTechCategoryButton(g, 3, catX2, catY2, catW, catH);

        if (hover != 4)
            paintTechCategoryButton(g, 4, catX1, catY3, catW, catH);
        if (hover != 5)
            paintTechCategoryButton(g, 5, catX2, catY3, catW, catH);
        
        // paint the hovering category last since it can overwrite the others
        if (hover >= 0)
            paintTechCategoryButton(g, hover, catX[hover], catY[hover], catW, catH);
    }
    @Override
    public void advanceMap() {
        parent.resumeTurn();
    }
    void paintTechCategoryButton(Graphics2D g, TechCategorySprite spr) {
        paintTechCategoryButton(g, spr.categoryNum(), spr.box().x, spr.box().y, spr.box().width, spr.box().height);
    }
    void paintTechCategoryButton(Graphics2D g, int catNum, int x, int y, int w, int h0) {
        // determine which green/gray gradients to use
        LinearGradientPaint grayBack, greenBack;
        if (catNum % 2 == 0) {
            grayBack = grayBack1;
            greenBack = greenBack1;
        }
        else {
            grayBack = grayBack2;
            greenBack = greenBack2;
        }

        // set mouse bounds for button
        categorySprites[catNum].setBounds(x,y,w,h0);
        
        int h = h0;
        int lineH = BasePanel.s20;
        List<String> techList = mission.possibleTechs(TechCategory.id(catNum));
        if ((techCategoryHoverButton == catNum) && (techList.size() > 1)) {
            h = h+(techList.size()*lineH);
        }
        
        
        // draw shadow around button
        int bdr = BasePanel.s3;
        g.setPaint(MainUI.darkShadowC);
        g.fillRect(x+bdr, y+bdr, w, h);

        String techDesc;
        if (techList.isEmpty()) 
            techDesc = text("NOTICE_ESPIONAGE_NO_TECH");
        else if (techList.size() > 1) 
            techDesc = text("NOTICE_ESPIONAGE_MANY_TECHS", str(techList.size()));
        else
            techDesc = tech(techList.get(0)).name();

        // draw button background gradient
        if (techList.isEmpty())
            g.setPaint(grayBack);
        else
            g.setPaint(greenBack);
        g.fillRect(x, y, w, h);

        // draw button border
        Stroke prev = g.getStroke();
        g.setStroke(BasePanel.stroke2);
        g.setColor(SystemPanel.whiteText);
        g.drawRect(x, y, w, h);
        g.setStroke(prev);

        // draw button text
        g.setFont(narrowFont(20));
        String s = text(TechCategory.id(catNum));
        if (canSelect(catNum))
            s = concat(str(catNum+1), " - ", s);
        int sw = g.getFontMetrics().stringWidth(s);
        int xc = x+(w-sw)/2;
        Color c0;
        
        if (techList.isEmpty())
            c0 = SystemPanel.grayText;
        else if (techCategoryHoverButton == catNum)
            c0 = Color.yellow;
        else
            c0 = Color.white;
        
        int y1 = y+lineH;
        drawShadowedString(g, s, 3, xc, y1, MainUI.darkShadowC, c0);
        
        scaledFont(g, techDesc, w-BasePanel.s10, 16, 12);
        int sw1 = g.getFontMetrics().stringWidth(techDesc);
        int x1 = x+(w-sw1)/2;      
        y1 += lineH;
        drawShadowedString(g, techDesc, 1, x1, y1, MainUI.darkShadowC, c0);
        
        if ((techCategoryHoverButton == catNum) && (techList.size() > 1)) {
            for (String tId: techList) {
                Tech t = tech(tId);
                y1 += lineH;
                drawShadowedString(g, t.name(), 1, x+BasePanel.s10, y1, MainUI.darkShadowC, c0);
            }
        }  
    }
}
