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
import rotp.model.Sprite;
import rotp.model.empires.Empire;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.StarSystem;
import rotp.ui.BasePanel;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.main.MainUI;
import rotp.ui.main.SystemPanel;
import rotp.ui.sprites.BombardNoSprite;
import rotp.ui.sprites.BombardYesSprite;
import rotp.ui.sprites.ClickToContinueSprite;

public class MapOverlayBombardPrompt extends MapOverlay {
    static final Color destroyedTextC = new Color(255,32,32,192);
    static final Color destroyedMaskC = new Color(0,0,0,160);
    Color maskC  = new Color(40,40,40,160);
    Area mask;
    BufferedImage planetImg;
    MainUI parent;
    boolean bombarded = false;
    int sysId;
    ShipFleet fleet;
    int pop, endPop, bases, endBases, fact, endFact, shield, transports;
    boolean drawSprites = false;
    ClickToContinueSprite clickSprite;
    BombardNoSprite noButton = new BombardNoSprite();
    BombardYesSprite yesButton = new BombardYesSprite();
    public MapOverlayBombardPrompt(MainUI p) {
        parent = p;
        clickSprite = new ClickToContinueSprite(parent);
    }
    public void init(int systemId, ShipFleet fl) {
        drawSprites = true;
        planetImg = null;
        Empire pl = player();
        StarSystem sys = galaxy().system(systemId);
        sysId = systemId;
        fleet = fl;
        bombarded = false;
        pl.sv.refreshFullScan(sysId);
        pop = endPop = pl.sv.population(sysId);
        bases = endBases = pl.sv.bases(sysId);
        fact = endFact = pl.sv.factories(sysId);
        shield = sys.colony().defense().shieldLevel();
        transports = player().transportsInTransit(sys);
        noButton.reset();
        yesButton.reset();
        parent.hideDisplayPanel();
        parent.map().setScale(20);
        parent.map().recenterMapOn(sys);
        parent.mapFocus(sys);
        parent.clickedSprite(sys);
        parent.repaint();
    }
    public void bombardYes() {
        drawSprites = false;
        mask = null;
        softClick();
        bombard();
        parent.map().repaint();
    }
    public void bombardCancel() {
        drawSprites = false;
        mask = null;
        //softClick();
        advanceMap();
    }
    private void bombard() {
        // avoid multiple bombings triggered by
        // repaints from animation
        if (!bombarded) {
            bombarded = true;
            fleet.bombard();
            Empire pl = player();
            endPop = pl.sv.population(sysId);
            endBases = pl.sv.bases(sysId);
            endFact = pl.sv.factories(sysId);
        }
    }
    @Override
    public boolean drawSprites()   { return drawSprites; }
    @Override
    public boolean masksMouseOver(int x, int y)   { return true; }
    @Override
    public boolean hoveringOverSprite(Sprite o) { return false; }
    @Override
    public void advanceMap() {
        parent.resumeTurn();
    }
    @Override
    public void paintOverMap(MainUI parent, GalaxyMapPanel ui, Graphics2D g) {
        StarSystem sys = galaxy().system(sysId);
        Empire pl = player();

        int s7 = BasePanel.s7;
        int s10 = BasePanel.s10;
        int s15 = BasePanel.s15;
        int s20 = BasePanel.s20;
        int s25 = BasePanel.s25;
        int s30 = BasePanel.s30;
        int s35 = BasePanel.s35;
        int s40 = BasePanel.s40;
        int s50 = BasePanel.s50;
        int s60 = BasePanel.s60;

        int w = ui.getWidth();
        int h = ui.getHeight();

        int transportH = transports > 0 ? s20 : 0;
        
        int bdrW = s7;
        int boxW = scaled(540);
        int boxH = scaled(245)+transportH;
        int boxH1 = BasePanel.s73+transportH;

        int boxX = -s40+(w/2);
        int boxY = s40+(h-boxH)/2;

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
        g.setColor(maskC);
        g.fill(mask);
        // draw border
        g.setColor(MainUI.paneShadeC);
        g.fillRect(boxX-bdrW, boxY-bdrW, boxW+bdrW+bdrW, boxH+bdrW+bdrW);

        // draw Box
        g.setColor(MainUI.paneBackground);
        g.fillRect(boxX, boxY, boxW, boxH1);

        String sysName = player().sv.name(sys.id);
        // draw planet image
        if (planetImg == null) {
            if (sys.planet().type().isAsteroids()) {
                planetImg = newBufferedImage(boxW, boxH-boxH1);
                Graphics imgG = planetImg.getGraphics();
                imgG.setColor(Color.black);
                imgG.fillRect(0, 0, boxW, boxH-boxH1);
                drawBackgroundStars(imgG, boxW, boxH-boxH1);
                parent.drawStar((Graphics2D) imgG, sys.starType(), s60, boxW*4/5, (boxH-boxH1)/3);
                imgG.dispose();
            }
            else {
                planetImg = sys.planet().type().panoramaImage();
                int planetW = planetImg.getWidth();
                int planetH = planetImg.getHeight();
                Graphics imgG = planetImg.getGraphics();
                Empire emp = pl.sv.empire(sysId);
                if (emp != null) {
                    BufferedImage fortImg = emp.race().fortress(sys.colony().fortressNum());
                    int fortW = scaled(fortImg.getWidth());
                    int fortH = scaled(fortImg.getHeight());
                    int fortScaleW = fortW*planetW/w;
                    int fortScaleH = fortH*planetW/w;
                    int fortX = planetImg.getWidth()-fortScaleW;
                    int fortY = planetImg.getHeight()-fortScaleH+(planetH/5);
                    imgG.drawImage(fortImg, fortX, fortY, fortX+fortScaleW, fortY+fortScaleH, 0, 0, fortImg.getWidth(), fortImg.getHeight(), null);
                    imgG.dispose();
                }
            }
        }
        g.drawImage(planetImg, boxX, boxY+boxH1, boxW, boxH-boxH1, null);

        // draw header info
        int leftW = boxW * 35/100;
        String yearStr = displayYearOrTurn();
        g.setFont(narrowFont(40));
        int sw = g.getFontMetrics().stringWidth(yearStr);
        int x0 = boxX+((leftW-sw)/2);
        int ya = boxY+boxH1-s20-(transportH/2);
        drawBorderedString(g, yearStr, 2, x0, ya, SystemPanel.textShadowC, SystemPanel.orangeText);

        if (bombarded) {
            String titleStr = text("MAIN_BOMBARD_COMPLETE");
            g.setFont(narrowFont(24));
            drawShadowedString(g, titleStr, 4, boxX+leftW, boxY+s30, SystemPanel.textShadowC, Color.white);
            String contStr = text("CLICK_CONTINUE");
            g.setColor(Color.black);
            g.setFont(narrowFont(14));
            g.drawString(contStr, boxX+leftW, boxY+s50);
            // click to continue sprite
            parent.addNextTurnControl(clickSprite);
        }
        else {
            String titleStr = text("MAIN_BOMBARD_TITLE", sysName, sys.empire().raceName());
            g.setColor(Color.black);
            int titleFontSize = scaledFont(g, titleStr, boxW-leftW, 20, 14);
            g.setFont(narrowFont(titleFontSize));
            g.drawString(titleStr, boxX+leftW, boxY+s25);

            if (transports > 0) {
                String subtitleStr = text("MAIN_BOMBARD_TROOPS", str(transports), player().raceName());
                g.setColor(Color.black);
                int subtitleFontSize = min(titleFontSize-2, scaledFont(g, subtitleStr, boxW-leftW, 20, 14));
                g.setFont(narrowFont(subtitleFontSize));
                //drawBorderedString(g, subtitleStr, 1, boxX+leftW, boxY+s20+transportH, Color.black, new Color(192,64,64));
                //drawShadowedString(g, subtitleStr, 1, boxX+leftW, boxY+s20+transportH, new Color(30,30,30,150), new Color(192,64,64));
                g.drawString(subtitleStr, boxX+leftW, boxY+s25+transportH);         
            }
            
            // calc width needed for yes/no buttons
            g.setFont(narrowFont(20));
            String yesStr = text("MAIN_BOMBARD_YES");
            String noStr = text("MAIN_BOMBARD_NO");
            int swYes = g.getFontMetrics().stringWidth(yesStr);
            int swNo = g.getFontMetrics().stringWidth(noStr);
            int buttonW = s20+Math.max(swYes, swNo);

            // print prompt string
            String promptStr = text("MAIN_BOMBARD_PROMPT");
            int promptFontSize = scaledFont(g, promptStr, boxW-leftW-buttonW-buttonW-s30, 24, 20);
            g.setFont(narrowFont(promptFontSize));
            int swPrompt = g.getFontMetrics().stringWidth(promptStr);
            int promptY = boxY+s35+transportH;
            drawShadowedString(g, promptStr, 4, boxX+leftW, promptY+s20, SystemPanel.textShadowC, Color.white);

            // draw yes/no buttons
            g.setFont(narrowFont(20));
            int buttonY = promptY;
            int buttonH = s30;
            int x2 = boxX+leftW+swPrompt+s10;
            int x3 = x2+buttonW+s10;
            // yes button
            parent.addNextTurnControl(yesButton);
            yesButton.parent(this);
            yesButton.setBounds(x2, buttonY, buttonW, buttonH);
            yesButton.draw(parent.map(), g);
            // no button
            parent.addNextTurnControl(noButton);
            noButton.parent(this);
            noButton.setBounds(x3, buttonY, buttonW, buttonH);
            noButton.draw(parent.map(), g);
        }

        // draw top data line
        int y0a = boxY+boxH1+s20;
        int x0a = boxX+s10;

        int pad = s30;
        int p1 = BasePanel.s5;
        String dmgStr = text("MAIN_BOMBARD_DMG", "-99");
        String popStr = text("MAIN_BOMBARD_POPULATION", endPop);
        String factStr = text("MAIN_BOMBARD_FACTORIES", endFact);
        String baseStr = text("MAIN_BOMBARD_BASES", endBases);
        String shieldStr = text("MAIN_BOMBARD_SHIELD", shield);

        String allText = concat(popStr,dmgStr,factStr,dmgStr,baseStr,dmgStr,shieldStr);
        int fontSize1 = scaledFont(g, allText, boxW-s10-s10-(3*pad)-(3*p1), 20, 13);
        g.setFont(narrowFont(fontSize1));
        int allsw = g.getFontMetrics().stringWidth(allText);
        pad = (boxW-allsw-(3*p1)-s10-s10)/3;
        int dmgW = g.getFontMetrics().stringWidth(dmgStr)+p1;

        drawBorderedString(g, popStr, 1, x0a, y0a, Color.black, Color.white);
        x0a += g.getFontMetrics().stringWidth(popStr);
        if (endPop < pop) {
            dmgStr = text("MAIN_BOMBARD_DMG", str(endPop-pop));
            drawBorderedString(g, dmgStr, 1, x0a+p1, y0a, Color.black, Color.red);
        }
        x0a += dmgW;
        x0a += pad;

        drawBorderedString(g, factStr, 1, x0a, y0a, Color.black, Color.white);
        x0a += g.getFontMetrics().stringWidth(factStr);
        if (endFact < fact) {
            dmgStr = text("MAIN_BOMBARD_DMG", str(endFact-fact));
            drawBorderedString(g, dmgStr, 1, x0a+p1, y0a, Color.black, Color.red);
        }
        x0a += dmgW;
        x0a += pad;

        drawBorderedString(g, baseStr, 1, x0a, y0a, Color.black, Color.white);
        x0a += g.getFontMetrics().stringWidth(baseStr);
        if (endBases < bases) {
            dmgStr = text("MAIN_BOMBARD_DMG", str(endBases-bases));
            drawBorderedString(g, dmgStr, 1, x0a+p1, y0a, Color.black, Color.red);
        }
        x0a += dmgW;
        x0a += pad;

        drawBorderedString(g, shieldStr, 1, x0a, y0a, Color.black, Color.white);

        // draw planet info, from bottom up
        int x1 = boxX+s15;
        int y1 = boxY+boxH-s10;
        int lineH = s20;
        int desiredFont = 18;

        if (pl.sv.isUltraPoor(sys.id)) {
            g.setColor(SystemPanel.redText);
            String s1 = text("MAIN_SCOUT_ULTRA_POOR_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 15);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }
        else if (pl.sv.isPoor(sys.id)) {
            g.setColor(SystemPanel.redText);
            String s1 = text("MAIN_SCOUT_POOR_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 15);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }
        else if (pl.sv.isRich(sys.id)) {
            g.setColor(SystemPanel.greenText);
            String s1 = text("MAIN_SCOUT_RICH_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 15);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }
        else if (pl.sv.isUltraRich(sys.id)) {
            g.setColor(SystemPanel.greenText);
            String s1 = text("MAIN_SCOUT_ULTRA_RICH_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 15);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }

        if (pl.sv.isOrionArtifact(sys.id)) {
            g.setColor(SystemPanel.greenText);
            String s1 = text("MAIN_SCOUT_ANCIENTS_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 15);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }
        else if (pl.sv.isArtifact(sys.id)) {
            g.setColor(SystemPanel.greenText);
            String s1 = text("MAIN_SCOUT_ARTIFACTS_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 15);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }

        if (pl.isEnvironmentHostile(sys)) {
            g.setColor(SystemPanel.redText);
            String s1 = text("MAIN_SCOUT_HOSTILE_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 15);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }
        else if (pl.isEnvironmentFertile(sys)) {
            g.setColor(SystemPanel.greenText);
            String s1 = text("MAIN_SCOUT_FERTILE_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 15);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }
        else if (pl.isEnvironmentGaia(sys)) {
            g.setColor(SystemPanel.greenText);
            String s1 = text("MAIN_SCOUT_GAIA_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 15);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }

        // classification line
        if (sys.planet().type().isAsteroids()) {
            String s1 = text("MAIN_SCOUT_NO_PLANET");
            g.setFont(narrowFont(desiredFont+3));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }
        else {
            String s1 = text("MAIN_SCOUT_TYPE", text(sys.planet().type().key()), (int)sys.planet().maxSize());
            g.setFont(narrowFont(desiredFont+3));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }

        // planet name
        y1 -= scaled(5);
        g.setFont(narrowFont(32));
        drawBorderedString(g, sysName, 1, x1, y1, Color.darkGray, SystemPanel.orangeText);

        if (sys.empire() == null) {
            g.setColor(destroyedMaskC);
            g.fillRect(boxX, boxY+boxH1, boxW, boxH-boxH1);
            String s = text("MAIN_BOMBARD_DESTROYED");
            int fontSize = scaledFont(g, s, boxW-s10, 50, 30);
            g.setFont(narrowFont(fontSize));
            sw = g.getFontMetrics().stringWidth(s);
            x0 = boxX+((boxW-sw)/2);
            int y0 = boxY+boxH1+scaled(fontSize+20);
            this.drawBorderedString(g, s, 2, x0, y0, Color.black, destroyedTextC);
        }
    }
    @Override
    public boolean handleKeyPress(KeyEvent e) {
        switch(e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
            case KeyEvent.VK_N:
                bombardCancel();
                break;
            case KeyEvent.VK_Y:
                bombardYes();
                break;
            default:
                misClick();
                break;
        }
        return true;
    }
}
