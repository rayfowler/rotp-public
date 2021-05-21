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
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import rotp.model.Sprite;
import rotp.model.ships.ShipDesign;
import rotp.model.ships.ShipDesignLab;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.main.MainUI;
import rotp.ui.main.SystemPanel;
import rotp.ui.sprites.ClickToContinueSprite;

public class MapOverlayShipsConstructed extends MapOverlay {
    Color maskC  = new Color(40,40,40,160);
    Area mask;
    BufferedImage planetImg;
    MainUI parent;
    List<ShipDesign> designs = new ArrayList<>();
    ClickToContinueSprite clickSprite;
    public MapOverlayShipsConstructed(MainUI p) {
        parent = p;
        clickSprite = new ClickToContinueSprite(parent);
    }
    public void init() {
        parent.hideDisplayPanel();
        designs.clear();
        for (ShipDesign d: player().shipLab().designs()) {
            if (d != null) {
                if (session().shipsConstructed().containsKey(d))
                    designs.add(d);
            }
        }
        if (designs.isEmpty())
            advanceMap();
    }
    @Override
    public boolean masksMouseOver(int x, int y)   { return true; }
    @Override
    public boolean hoveringOverSprite(Sprite o) { return false; }
    @Override
    public void advanceMap() {
        designs.clear();
        parent.resumeTurn();
    }
    @Override
    public void paintOverMap(MainUI parent, GalaxyMapPanel ui, Graphics2D g) {
        int w = ui.getWidth();
        int h = ui.getHeight();

        int s7 = scaled(7);
        int s20 = scaled(20);
        int s30 = scaled(30);
        int s40 = scaled(40);
        int s68 = scaled(68);

        int bdrW = s7;
        int boxW = scaled(540);
        int boxH = scaled(240);
        int boxH1 = s68;

        int boxX = (w-boxW)/2;
        int boxY = (h-boxH)/2;

        // draw map mask
        if (mask == null) {
            Rectangle blackout  = new Rectangle();
            blackout.setFrame(0,0,w,h);
            mask = new Area(blackout);
        }
        g.setColor(maskC);
        g.fill(mask);
        // draw border
        g.setColor(MainUI.paneShadeC);
        g.fillRect(boxX-bdrW, boxY-bdrW, boxW+bdrW+bdrW, boxH+bdrW+bdrW);

        // draw Box
        g.setColor(MainUI.paneBackground);
        g.fillRect(boxX, boxY, boxW, boxH1);

        // draw planet image
        if (planetImg == null) {
            planetImg = newBufferedImage(boxW, boxH-boxH1);
            Graphics imgG = planetImg.getGraphics();
            imgG.setColor(Color.black);
            imgG.fillRect(0, 0, boxW, boxH-boxH1);
            drawBackgroundStars(imgG, boxW, boxH-boxH1);
            imgG.dispose();
        }
        g.drawImage(planetImg, boxX, boxY+boxH1, boxW, boxH-boxH1, null);

        // draw header info
        int leftW = boxW * 2/5;
        String yearStr = displayYearOrTurn();
        g.setFont(narrowFont(40));
        int sw = g.getFontMetrics().stringWidth(yearStr);
        int x0 = boxX+((leftW-sw)/2);
        drawBorderedString(g, yearStr, 2, x0, boxY+boxH1-s20, SystemPanel.textShadowC, SystemPanel.orangeText);

        String scoutStr = text("MAIN_FLEET_PRODUCTION_TITLE");
        g.setFont(narrowFont(24));
        drawShadowedString(g, scoutStr, 4, boxX+leftW, boxY+boxH1-s40, SystemPanel.textShadowC, Color.white);

        String skipStr = text("CLICK_CONTINUE");
        g.setColor(Color.darkGray);
        g.setFont(narrowFont(16));
        drawString(g,skipStr, boxX+leftW+s30, boxY+boxH1-s20);

        int numShips = designs.size();
        int shipW = boxW/ShipDesignLab.MAX_DESIGNS;
        int shipH = shipW*2/3;
        int hSpacing = (boxW-(numShips*shipW))/(numShips+1);
        int vSpacing= (boxH-boxH1-shipH)/2;
        int shipX = hSpacing;
        for (ShipDesign d: designs) {
            drawShip(parent, parent.map(), g, d, boxX+shipX, boxY+boxH1+vSpacing, shipW, shipH);
            shipX = shipX+shipW+hSpacing;
        }
        parent.addNextTurnControl(clickSprite);
    }
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
    private void drawShip(MainUI parent, GalaxyMapPanel ui, Graphics2D g, ShipDesign d, int x, int y, int w, int h) {
        g.setFont(narrowFont(18));
        int sw = g.getFontMetrics().stringWidth(d.name());
        int x0 = x+((w-sw)/2);
        g.setColor(SystemPanel.yellowText);
        drawString(g,d.name(), x0, y);

        Image img = d.image();
        
        if (img != null) {
            int imgW = img.getWidth(null);
            int imgH = img.getHeight(null);
            float scale = min((float)w/imgW, (float)h/imgH);

            int w1 = (int)(scale*imgW);
            int h1 = (int)(scale*imgH);

            int x1 = x+((w-w1)/2);
            int y1 = y+((h-h1)/2);
            g.drawImage(img, x1, y1, x1+w1, y1+h1, 0, 0, imgW, imgH, parent);
        }

        int count = session().shipsConstructed().containsKey(d) ? session().shipsConstructed().get(d) : 0;
        String s = str(count);
        int sw2 = g.getFontMetrics().stringWidth(s);
        int x2 = x+((w-sw2)/2);
        g.setColor(SystemPanel.yellowText);
        drawString(g,s, x2, y+h+scaled(15));
    }
}
