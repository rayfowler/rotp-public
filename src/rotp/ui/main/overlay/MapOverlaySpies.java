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
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import rotp.Rotp;
import rotp.model.Sprite;
import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
import rotp.ui.BasePanel;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.main.MainUI;
import rotp.ui.main.SystemPanel;
import rotp.ui.sprites.ClickToContinueSprite;

public class MapOverlaySpies extends MapOverlay {
    Color maskC  = new Color(40,40,40,160);
    Area mask;
    MainUI parent;
    BufferedImage labImg;

    private final List<Empire> empires = new ArrayList<>();
    boolean drawSprites = false;
    ClickToContinueSprite clickSprite;

    public MapOverlaySpies(MainUI p) {
        parent = p;
        clickSprite = new ClickToContinueSprite(parent);
    }
    public boolean shouldDisplay() {
        return !empires.isEmpty();
    }
    public void init() {
        labImg = null;
        drawSprites = true;
        List<Empire> allEmpires = player().contactedEmpires();
        empires.clear();
        for (Empire emp: allEmpires) {
            EmpireView v = player().viewForEmpire(emp.id);
            if ((v.spies().spiesLost() > 0)
            || (v.otherView().spies().spiesLost() > 0)) 
                empires.add(emp);
        }
    }
    @Override
    public boolean drawSprites()   { return drawSprites; }
    @Override
    public boolean masksMouseOver(int x, int y)   { return true; }
    @Override
    public boolean hoveringOverSprite(Sprite o) { return false; }
    @Override
    public boolean handleKeyPress(KeyEvent e) {
        switch(e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
                advanceMap();
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
        int h = ui.getHeight()-BasePanel.s30;
        Empire pl = player();
        
        int s7 = BasePanel.s7;
        int s10 = BasePanel.s10;
        int s20 = BasePanel.s20;
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

        int x0 = scaled(335);
        int y0 = scaled(235);
        int w0 = scaled(555);
        int h0 = scaled(225);
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
        drawBorderedString(g, yearStr, 2, x1a, y1+BasePanel.s45, SystemPanel.textShadowC, SystemPanel.orangeText);

        // draw title
        int x1b = x1+leftW+s10;
        String subtitle = text("NOTICE_SPIES_TITLE");
        g.setFont(narrowFont(26));
        drawShadowedString(g, subtitle, 3, x1b, y1+BasePanel.s25, SystemPanel.textShadowC, Color.white);


        String skipStr = text("CLICK_CONTINUE");
        g.setColor(SystemPanel.blackText);
        g.setFont(narrowFont(14));
        g.drawString(skipStr, x1b+BasePanel.s20, y1+BasePanel.s42);

        
        
        // draw header
        if (empires.isEmpty()) {
            String noSpies = text("NOTICE_SPIES_CAUGHT_NONE");
            g.setFont(narrowFont(18));
            g.setColor(SystemPanel.blackText);
            List<String> lines = wrappedLines(g, noSpies, rightW-s20);
            int y2 = y1+scaled(110);
            for (String line: lines) {
                g.drawString(line, x1b, y2);
                y2 += s20;
            }
        }
        else {
            int y2 = y1+scaled(70);
            int lineH = BasePanel.s16;
            g.setFont(narrowFont(18));
            g.setColor(SystemPanel.whiteText);
            String capt = text("NOTICE_SPIES_CAUGHT");
            String ours = text("NOTICE_SPIES_CAUGHT_OURS");
            String theirs = text("NOTICE_SPIES_CAUGHT_THEIRS");
            int swa = g.getFontMetrics().stringWidth(ours);
            int swb = g.getFontMetrics().stringWidth(theirs);
            int lineb = x1+w1-s50;
            int linea = lineb-swb-s10;
            g.drawString(capt, x1b, y2);
            g.drawString(ours, linea-swa, y2);
            g.drawString(theirs, lineb-swb, y2);
            g.setFont(narrowFont(15));
            g.setColor(SystemPanel.blackText);
            
            for (Empire emp: empires) {
                y2 += lineH;
                EmpireView v = pl.viewForEmpire(emp.id);
                String spiesA = str(v.spies().spiesLost());
                String spiesB = str(v.otherView().spies().spiesLost());
                swa = g.getFontMetrics().stringWidth(spiesA);
                swb = g.getFontMetrics().stringWidth(spiesB);
                g.drawString(v.empire().raceName(), x1b, y2);
                g.drawString(spiesA, linea-swa, y2);
                g.drawString(spiesB, lineb-swb, y2);
            }
        }
        

        int xOff = scaled(pl.race().espionageX);
        int yOff = scaled(pl.race().espionageY);
        if (labImg == null) {
            labImg = asBufferedImage(pl.race().laboratory());
            Graphics imgG = labImg.getGraphics();
            BufferedImage spyImg = pl.race().spyQuiet();
            imgG.drawImage(spyImg, 0, 0, labImg.getWidth(), labImg.getHeight(), xOff, yOff, (spyImg.getWidth()/2)+xOff, (spyImg.getHeight()/2)+yOff, null);
            imgG.dispose();
        }

        int imgW = leftW;
        int imgH = imgW*Rotp.IMG_H/Rotp.IMG_W;
        g.drawImage(labImg, x1, y1+h1-imgH, x1+leftW, y1+h1, 0, 0, labImg.getWidth(), labImg.getHeight(), null);

        parent.addNextTurnControl(clickSprite);
    }
    @Override
    public void advanceMap() {
        parent.resumeTurn();
    }
}
