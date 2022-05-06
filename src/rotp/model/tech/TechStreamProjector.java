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
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import rotp.model.combat.CombatStack;
import rotp.model.empires.Empire;
import rotp.model.ships.ShipSpecialProjector;
import rotp.ui.BasePanel;
import rotp.ui.combat.ShipBattleUI;

public final class TechStreamProjector extends Tech {
    private static final int FRAME_MS = 30;
    public int range = 1;
    public float enemyArmorMod = 1;
    public float enemyArmorModMax = 1;
    public float extraArmorMod = 0;
    public float shipsPerExtraArmorMod = 1;
    private transient Color beamColor;

    public TechStreamProjector(String typeId, int lv, int seq, boolean b, TechCategory c) {
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
        techType = Tech.STREAM_PROJECTOR;

        switch(typeSeq) {
            case 0:
                range = 2;
                cost = 100;
                size = 250;
                power = 500;
                enemyArmorMod = .80f;
                enemyArmorModMax = .50f;
                extraArmorMod = .01f;
                shipsPerExtraArmorMod = 2;
                beamColor = new Color(255,0,0,64);
                break;
            case 1:
                range = 2;
                cost = 200;
                size = 500;
                power = 1250;
                enemyArmorMod = .60f;
                enemyArmorModMax = .25f;
                extraArmorMod = .01f;
                shipsPerExtraArmorMod = 1;
                beamColor = new Color(255,0,255,64);
                break;
        }
    }
    @Override
    public float warModeFactor()        { return 2; }
    @Override
    public boolean providesShipComponent()  { return true; }
    public float armorMod(int ships) {
        float mod = enemyArmorMod-(extraArmorMod*ships/shipsPerExtraArmorMod);
        return max(mod,enemyArmorModMax);
    }
    @Override
    public void provideBenefits(Empire c) {
        super.provideBenefits(c);
        ShipSpecialProjector sh = new ShipSpecialProjector(this);
        c.shipLab().addSpecial(sh);
    }
    @Override
    public float baseValue(Empire c) {
        return c.ai().scientist().baseValue(this);
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

        Rectangle rect = ui.combatGrids[x][y];
        int n=18;
        int dX = BasePanel.s2;
        int dY = rect.height/(n*2);
        int dW = BasePanel.s5;
        int dH = 2*dY;

        // calculate proper image rotation for attack frames
        int rX = (int) (ui.boxW*3/2);
        int rY = (int) (ui.boxH*3/2);
        float radians = source.rotateRadians(target);
        
        // create attack frames
        BufferedImage[] frames = new BufferedImage[n];
        int w0 = BasePanel.s10;
        int h0 = rect.height/8;
        int x0 = ui.boxW*7/4;
        int y0 = (ui.boxH*3/2)-(h0/2);
        for (int i=0;i<n;i++) 
            frames[i] = newBufferedImage(ui.boxW*3, ui.boxH*3);
        for (int i=0;i<n;i++) {
            for (int j=i;j<n;j++) {
                Graphics2D g1 = (Graphics2D) frames[j].getGraphics();
                g1.setStroke(BasePanel.baseStroke(3));
                g1.setColor(beamColor);
                AffineTransform tx = g1.getTransform();
                g1.rotate(radians, rX, rY);
                g1.drawArc(x0, y0, w0, h0,315,90);
                g1.setTransform(tx);
                g1.dispose();
            }
            x0 = x0+dX;
            y0 -= dY;
            w0 += dW;
            h0 += dH;
        }
        
        int repeat = 1;
        
        // create shade boxes for target as it gets increasingly in stasis

        int startX = rect.x-ui.boxW;
        int startY = rect.y-ui.boxH;
        
        // draw attack
        for (int h=0; h<repeat; h++) {
            if (!source.mgr.showAnimations()) 
                break;
            ui.paintCellsImmediately(source.x, target.x, source.y, target.y);
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
