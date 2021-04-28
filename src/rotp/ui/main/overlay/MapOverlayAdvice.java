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
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.List;
import rotp.model.Sprite;
import rotp.model.empires.Empire;
import rotp.model.empires.Race;
import rotp.ui.BasePanel;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.main.MainUI;
import rotp.ui.main.SystemPanel;
import rotp.ui.sprites.AdvisorOKSprite;

public class MapOverlayAdvice extends MapOverlay {
    static Color backDarkC = new Color(34,53,102);
    static Color centerC = new Color(79,102,156);
    RadialGradientPaint backPaint;
    LinearGradientPaint buttonPaint;
    Rectangle bounds = new Rectangle();
    MainUI parent;
    BufferedImage advisorImg;
    AdvisorOKSprite okButton = new AdvisorOKSprite();
    String textKey;
    Empire emp1;
    String var1, var2, var3;
    public MapOverlayAdvice(MainUI p) {
        parent = p;
    }
    public void init(String key, Empire e1, String s1, String s2, String s3) {
        textKey = key;
        emp1 = e1;
        var1 = s1;
        var2 = s2;
        var3 = s3;
        
        Race r = player().race();
        BufferedImage img;
        r.resetScientist();
        r.resetSpy();
        r.resetDiplomat();
        r.resetSoldier();
        switch(key) {
            case "MAIN_ADVISOR_SCOUT":            img = r.advisorScout(); break;
            case "MAIN_ADVISOR_TRANSPORT":        img = r.advisorTransport(); break;
            case "MAIN_ADVISOR_DIPLOMACY":        img = r.advisorDiplomacy(); break;
            case "MAIN_ADVISOR_SHIP_ENGINE":      img = r.advisorShip(); break;
            case "MAIN_ADVISOR_RALLY_POINTS":     img = r.advisorRally(); break;
            case "MAIN_ADVISOR_MISSILE_BASES":    img = r.advisorMissile(); break;
            case "MAIN_ADVISOR_SHIP_WEAPON":      img = r.advisorWeapon(); break;
            case "MAIN_ADVISOR_COUNCIL":          img = r.advisorCouncil(); break;
            case "MAIN_ADVISOR_REBELLION":        img = r.advisorRebellion(); break;
            case "MAIN_ADVISOR_COUNCIL_RESISTED": img = r.advisorCouncilResisted(); break;
            case "MAIN_ADVISOR_RESIST_COUNCIL":   img = r.advisorResistCouncil(); break;
            default: img = r.advisorRebellion();
        }
        
        advisorImg = newBufferedImage(img);      
        okButton.draw(true);
        parent.repaint();
    }
    @Override
    public boolean hideNextTurnNotice()           { return false; }
    @Override
    public boolean canChangeMapScale()            { return true; }
    @Override
    public boolean consumesClicks(Sprite spr)  { return false; }
    @Override
    public boolean masksMouseOver(int x, int y)   { return bounds.contains(x,y); }
    @Override
    public boolean hoveringOverSprite(Sprite o) { return false; }
    @Override
    public void advanceMap() { parent.clearAdvice(); okButton.draw(false);}
    @Override
    public void paintOverMap(MainUI parent, GalaxyMapPanel ui, Graphics2D g) {
        if (advisorImg == null)
            return;
        
        int h = ui.getHeight();

        int mgn = BasePanel.s3;
        int buttonBarH = BasePanel.s68;
        int boxW = scaled(380);
        int boxH = scaled(200);

        if (backPaint == null) {
            float[] dist = {0.0f, 1.0f};
            Color[] colors = {centerC, backDarkC};
            Point2D center = new Point2D.Float(mgn+(boxW/2), h-(boxH/2)-buttonBarH);
            float radius = (float) (Math.sqrt((boxW*boxW)+(boxH*boxH))/2);
            backPaint = new RadialGradientPaint(center, radius, dist, colors);
        }
        g.setPaint(backPaint);
        int boxBottom = h-buttonBarH;
        int boxTop = boxBottom-boxH;
        int boxLeft = mgn;
        int boxRight = boxLeft+boxW;
        g.fillRect(mgn, boxTop, boxW, boxH);
        bounds.setBounds(mgn, boxTop, boxW, boxH);

        if (okButton.hovering()) {
            Stroke prev = g.getStroke();
            g.setStroke(BasePanel.stroke2);
            g.setColor(BasePanel.hoverC);
            g.drawRect(mgn, boxTop, boxW, boxH);
            g.setStroke(prev);
        }

        // draw text title
        int textMgn = boxLeft+scaled(155);
        g.setFont(narrowFont(28));
        String title = text("MAIN_ADVISOR_TITLE", player().raceName(), player().leader().name());
        title = player().replaceTokens(title, "player");
        drawShadowedString(g, title, 4, textMgn, boxTop+scaled(28), SystemPanel.textShadowC, SystemPanel.whiteText);

        int imgW = advisorImg.getWidth();
        int imgH = advisorImg.getHeight();
        int dispW = imgW*scaled(160)/440;
        int dispH = imgH*dispW/imgW;
        int x1 = boxLeft;
        int y1 = boxBottom-dispH;
        g.drawImage(advisorImg, x1, y1, x1+dispW, y1+dispH, 0, 0, imgW, imgH, null);

        // draw text
        g.setFont(narrowFont(18));
        String desc;
        if (var1 == null)
            desc = text(textKey);
        else if (var2 == null)
            desc = text(textKey, var1);
        else if (var3 == null)
            desc = text(textKey, var1, var2);
        else
            desc = text(textKey, var1, var2, var3);
        
        desc = player().replaceTokens(desc, "player");
        
        if (emp1 != null)
            desc = emp1.replaceTokens(desc, "alien");
        
        List<String> lines = wrappedLines(g, desc, boxW+mgn-textMgn-scaled(10));
        int lineH = scaled(19);
        int y0 = boxTop+scaled(55);
        for (String line: lines) {
            drawString(g,line, textMgn, y0);
            y0 += lineH;
        }
        String okStr = text("MAIN_ADVISOR_BUTTON_OK");
        g.setFont(narrowFont(20));
        int okW = g.getFontMetrics().stringWidth(okStr);
        // draw button
        int buttonW = Math.max(scaled(75), okW+scaled(30));
        int buttonX = boxRight-buttonW-scaled(10);
        int buttonY = boxBottom-scaled(40);
        int buttonH = scaled(30);
        parent.addNextTurnControl(okButton);
        okButton.parent(this);
        okButton.setBounds(buttonX, buttonY, buttonW, buttonH);
        okButton.setSelectionBounds(bounds.x, bounds.y, bounds.width, bounds.height);
        okButton.draw(parent.map(), g);
    }
    @Override
    public boolean handleKeyPress(KeyEvent e) {
        switch(e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
                advanceMap();
                return true;
            default:
                return false;
        }
    }
}
