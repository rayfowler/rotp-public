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
import rotp.model.ships.ShipSpecialBlackHole;
import rotp.ui.BasePanel;
import rotp.ui.combat.ShipBattleUI;

import java.awt.*;

public final class TechBlackHole extends Tech {
    public int range = 1;

    public TechBlackHole(String typeId, int lv, int seq, boolean b, TechCategory c) {
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
        techType = Tech.BLACK_HOLE;
        switch(typeSeq) {
            case 0:
                cost = 275;
                size = 750;
                power = 750;
                break;
        }
    }
    @Override
    public float baseValue(Empire c) { return c.ai().scientist().baseValue(this); }
    @Override
    public float warModeFactor()        { return 1.5f; }
    @Override
    public boolean providesShipComponent()  { return true; }
    @Override
    public void provideBenefits(Empire c) {
        super.provideBenefits(c);
        ShipSpecialBlackHole sh = new ShipSpecialBlackHole(this);
        c.shipLab().addSpecial(sh);
    }

    @Override
    public void drawSpecialAttack(CombatStack source, CombatStack target, int wpnNum, float dmg) {
        ShipBattleUI ui = source.mgr.ui;
        if (ui == null)
            return;

        if (!source.mgr.showAnimations())
            return;

        int x = target.x;
        int y = target.y;

        source.mgr.performingStackTurn = true;
        ui.paintAllImmediately();

        Graphics2D g = (Graphics2D) ui.getGraphics();
        Stroke prev = g.getStroke();

        g.setStroke(BasePanel.baseStroke(4));

        Rectangle rect = ui.combatGrids[x][y];
        int n=15;
        int sourceX = ui.combatGrids[source.x][source.y].x;
        int startX = rect.x+(rect.width/2);
        int startY = rect.y+(rect.height/2);
        int startR = 1;
        int dX = -rect.width/(n*2);
        int dY = -rect.height/(n*2);
        int dR = min(rect.width, rect.height)/n;

        //
        int x0 = startX;
        int y0 = startY;
        int r0 = startR;
        for (int i=0; i<n; i++) {
            ui.paintCellImmediately(x,y);
            int alpha = 127 + (int) (128*((float)n/n));
            g.setColor(new Color(0,0,0,alpha));
            g.fillOval(x0, y0, r0, r0);
            x0 += dX;
            y0 += dY;
            r0 += dR;
        }
        sleep(100);
        for (int i=0; i<n; i++) {
            ui.paintCellImmediately(x,y);
            int alpha = 127 + (int) (128*((float)n/n));
            g.setColor(new Color(0,0,0,alpha));
            g.fillOval(x0, y0, r0, r0);
            x0 -= dX;
            y0 -= dY;
            r0 -= dR;
        }

        target.drawAttackResult(g, startX,startY,sourceX, dmg,text("SHIP_COMBAT_MISS"));   
        g.setStroke(prev);

        ui.paintAllImmediately();
        sleep(250);
        source.mgr.performingStackTurn = false;
    }
}
