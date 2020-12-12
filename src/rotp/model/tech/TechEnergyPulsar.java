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

import rotp.model.combat.CombatStack;
import rotp.model.empires.Empire;
import rotp.model.ships.ShipSpecialPulsar;
import rotp.ui.BasePanel;
import rotp.ui.combat.ShipBattleUI;

import java.awt.*;
import java.awt.image.BufferedImage;

public final class TechEnergyPulsar extends Tech {
    private static final int FRAME_MS = 20;
    public int type;
    public int firstShipDamage;
    public int extraShipDamage;
	public int shipsPerExtraDamage; // modnar: correct Pulsar damage
    public int range = 1;
    private transient BufferedImage[] frames;

    public TechEnergyPulsar() {
        
    }
    public TechEnergyPulsar(String typeId, int lv, int seq, boolean b, TechCategory c) {
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
        techType = Tech.ENERGY_PULSAR;

        switch(typeSeq) {
            case 0:
                cost = 75;
                size = 150;
                power = 250;
                firstShipDamage = 5;
                extraShipDamage = 1;
				shipsPerExtraDamage = 2; // modnar: correct Pulsar damage
                break;
            case 1:
                cost = 150;
                size = 400;
                power = 750;
                firstShipDamage = 10;
                extraShipDamage = 1;
				shipsPerExtraDamage = 1; // modnar: correct Pulsar damage
                break;
        }
    }
    @Override
    public float warModeFactor()        { return 1.5f; }
    @Override
    public boolean providesShipComponent()  { return true; }
    @Override
    public boolean isObsolete(Empire c) {
        return (c.tech().topEnergyPulsarTech() != null) && (level < c.tech().topEnergyPulsarTech().level);
    }
    @Override
    public float baseValue(Empire c) {
        return c.tech().topEnergyPulsarTech() == null ? level : level - c.tech().topEnergyPulsarTech().level;
    }
    @Override
    public void provideBenefits(Empire c) {
        super.provideBenefits(c);
        if (!isObsolete(c))
            c.tech().topEnergyPulsarTech(this);

        ShipSpecialPulsar sh = new ShipSpecialPulsar(this);
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

        Graphics2D g = (Graphics2D) ui.getGraphics();
        Stroke prev = g.getStroke();

        g.setStroke(BasePanel.baseStroke(4));

        Rectangle rect = ui.combatGrids[x][y];
        int n=15;
        int dX = -rect.width/n;
        int dY = -rect.height/n;
        int dW = 2*rect.width/n;
        int dH = 2*rect.height/n;

        if (frames == null) {
            frames = new BufferedImage[n];
            int x0 = ui.boxW;
            int y0 = ui.boxH;
            int w0 = rect.width;
            int h0 = rect.height;
            for (int i=0;i<n;i++) 
                frames[i] = newBufferedImage(ui.boxW*3, ui.boxH*3);
            for (int i=0;i<n;i++) {
                int alpha = (int) (255*((float)(n-i)/n));
                Color c0 = new Color(160,160,255,alpha);
                int startJ = i;
                int endJ = min(n, i+5);
                for (int j=startJ;j<endJ;j++) {
                    Graphics2D g0 = (Graphics2D) frames[j].getGraphics();
                    g0.setStroke(BasePanel.baseStroke(5));
                    g0.setColor(c0);
                    g0.drawOval(x0, y0, w0, h0);
                    g0.dispose();
                }
                x0 += dX;
                y0 += dY;
                w0 += dW;
                h0 += dH;
            }
        }
        int startX = rect.x-ui.boxW;
        int startY = rect.y-ui.boxH;
        for (int i=0; i<n; i++) {
            long t0 = System.currentTimeMillis();
            ui.paintCellsImmediately(x-1, x+1, y-1,y+1);
            g.drawImage(frames[i],startX,startY,null);
            long t1 = System.currentTimeMillis()-t0;
            if (t1 < FRAME_MS)
                sleep(FRAME_MS-t1);
        }
        g.setStroke(prev);

        ui.paintAllImmediately();
        sleep(250);
        source.mgr.performingStackTurn = false;
    }
}
