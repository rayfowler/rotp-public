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
package rotp.model.tech;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import rotp.model.combat.CombatStack;
import rotp.model.empires.Empire;
import rotp.model.ships.ShipSpecialStasisField;
import rotp.ui.BasePanel;
import rotp.ui.combat.ShipBattleUI;
import java.awt.image.BufferedImage;

public final class TechStasisField extends Tech {
    private static final int FRAME_MS = 30;
    public static final Color STASIS_COLOR = new Color(255,255,255,96);
    public float duration = 0;
    public int range = 1;

    public TechStasisField (String typeId, int lv, int seq, boolean b, TechCategory c) {
        id(typeId, seq);
        typeSeq = seq;
        level = lv;
        cat = c;
        free = b;
        init();
    }
    @Override
    public boolean canBeMiniaturized()      { return true; }
    @Override
    public void init() {
        super.init();
        techType = Tech.STASIS_FIELD;

        switch(typeSeq) {
            case 0:
                duration = 1;
                cost = 250;
                size = 200;
                power = 275;
                break;
        }
    }
    @Override
    public boolean providesShipComponent()  { return true; }
    @Override
    public void provideBenefits(Empire c) {
        super.provideBenefits(c);

        ShipSpecialStasisField sh = new ShipSpecialStasisField(this);
        c.shipLab().addSpecial(sh);
    }        
    @Override
    public void drawSpecialAttack(CombatStack source, CombatStack target, int wpnNum, float dmg) {
        ShipBattleUI ui = source.mgr.ui;
        if (ui == null)
            return;

        if (!source.mgr.showAnimations)
            return;

        int x = source.x;
        int y = source.y;

        source.mgr.performingStackTurn = true;
        ui.paintAllImmediately();

        Graphics2D g0 = (Graphics2D) ui.getGraphics();
        Stroke prev = g0.getStroke();

        g0.setStroke(BasePanel.baseStroke(1));

        Rectangle rect = ui.combatGrids[x][y];
        int n=15;
        int dX = BasePanel.s2;
        int dY = rect.height/(n*2);
        int dW = 0;
        int dH = 2*dY;

        // calculate proper image rotation for attack frames
        int rX = (int) (ui.boxW*3/2);
        int rY = (int) (ui.boxH*3/2);
        float radians = source.rotateRadians(target);

        // create attack frames
        BufferedImage[] frames = new BufferedImage[n];
        int w0 = BasePanel.s5;
        int h0 = rect.height/8;
        int x0 = ui.boxW*7/4;
        int y0 = (ui.boxH*3/2)-(h0/2);
        Color c0 = new Color(160,160,160);
        for (int i=0;i<n;i++) 
            frames[i] = newBufferedImage(ui.boxW*3, ui.boxH*3);
        for (int i=0;i<n;i++) {
            for (int j=i;j<n;j++) {
                Graphics2D g1 = (Graphics2D) frames[j].getGraphics();
                g1.setColor(c0);
                AffineTransform tx = g1.getTransform();
                g1.rotate(radians, rX, rY);
                g1.drawOval(x0, y0, w0, h0);
                g1.setTransform(tx);
                g1.dispose();
            }
            x0 = x0+w0+dX;
            y0 -= dY;
            w0 += dW;
            h0 += dH;
        }
        
        int repeat = 4;
        
        // create shade boxes for target as it gets increasingly in stasis
        int r = STASIS_COLOR.getRed();
        int g = STASIS_COLOR.getGreen();
        int b = STASIS_COLOR.getBlue();
        int alpha = 0;
        int dA = STASIS_COLOR.getAlpha()/(repeat+1);
        BufferedImage[] shading = new BufferedImage[repeat];
        for (int i=0;i<repeat;i++) {
            alpha += dA;
            Color shade = new Color(r,g,b,alpha);
            BufferedImage img = newBufferedImage(ui.boxW, ui.boxH);
            Graphics g1 = img.getGraphics();
            g1.setColor(shade);
            g1.fillRect(0,0,ui.boxW, ui.boxH);
            g1.dispose();
            shading[i] = img;
        }
        int startX = rect.x-ui.boxW;
        int startY = rect.y-ui.boxH;
        Rectangle tgtBox = ui.combatGrids[target.x][target.y];
        
        // draw attack
        for (int h=0; h<repeat; h++) {
            if (!source.mgr.showAnimations()) 
                break;
            ui.paintCellsImmediately(source.x, target.x, source.y, target.y);
            g0.drawImage(shading[h],tgtBox.x, tgtBox.y, null);
            for (int i=0; i<n; i++) {
                long t0 = System.currentTimeMillis();
                g0.drawImage(frames[i],startX,startY,null);
                 long t1 = System.currentTimeMillis()-t0;
                if (t1 < FRAME_MS)
                    sleep(FRAME_MS-t1);
            }
        }
        g0.setStroke(prev);
        source.mgr.performingStackTurn = false;
    }
}
