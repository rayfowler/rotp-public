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
import rotp.model.ships.ShipWeaponBiological;
import rotp.ui.BasePanel;
import rotp.ui.combat.ShipBattleUI;
import java.awt.*;

public final class TechBiologicalWeapon extends Tech {
    public int minDamage;
    public int maxDamage;

    public TechBiologicalWeapon (String typeId, int lv, int seq, boolean b, TechCategory c) {
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
        techType = Tech.BIOLOGICAL_WEAPON;

        switch(typeSeq) {
            case 0:
                minDamage = 1;
                maxDamage = 1;
                cost = 12;
                size = 100;
                power = 10;
                break;
            case 1:
                minDamage = 1;
                maxDamage = 2;
                cost = 16;
                size = 200;
                power = 10;
                break;
            case 2:
                minDamage = 1;
                maxDamage = 3;
                cost = 20;
                size = 300;
                power = 10;
                break;
        }
    }
    @Override
    public float warModeFactor()           { return 2; }
    @Override
    public boolean providesShipComponent()  { return true; }
    @Override
    public boolean isObsolete(Empire c) {
        return maxDamage < c.tech().biologicalAttackLevel();
    }
    @Override
    public float baseValue(Empire c) { return c.ai().scientist().baseValue(this); }
    @Override
    public void provideBenefits(Empire c) {
        super.provideBenefits(c);
        if (! isObsolete(c))
            c.tech().topBiologicalWeaponTech(this);

        ShipWeaponBiological sh = new ShipWeaponBiological(this);
        c.shipLab().addWeapon(sh);
    }
    public static float avgDamage(int dmg, int antidoteLevel) {
        // p.139 of OSG
        switch(dmg) {
            case 1: return Math.max(0, 1 - antidoteLevel);
            case 2: return Math.max(0, 1.5f - antidoteLevel);
            case 3:
                switch(antidoteLevel) {
                    case 0: return 2.0f;
                    case 1: return 1.0f;
                    case 2: return 1/3.0f;
                    default: return 0;
                }
        }
        return 0;
    }
    @Override
    public void drawIneffectiveAttack(CombatStack source, CombatStack target, int wpnNum) {
        ShipBattleUI ui = source.mgr.ui;
        if (ui == null)
            return;

        int stW = ui.stackW();
        int stH = ui.stackH();
        int st0X = ui.stackX(source);
        int st0Y = ui.stackY(source);
        int st1X = ui.stackX(target);
        int st1Y = ui.stackY(target);

        int x0 = st0X > st1X ? st0X+(stW/3) :st0X+(stW*2/3);
        int y0 = st0Y+stH/2;
        int x1 = st1X+stW/2;
        int y1 = st1Y+stH/2;
        drawAttack(source, target, x0, y0, x1, y1, wpnNum, -1);
    }
    @Override
    public void drawUnsuccessfulAttack(CombatStack source, CombatStack target, int wpnNum) {
         if (!source.mgr.showAnimations())
            return;
        ShipBattleUI ui = source.mgr.ui;

        int stW = ui.stackW();
        int stH = ui.stackH();
        int st0X = ui.stackX(source);
        int st0Y = ui.stackY(source);
        int st1X = ui.stackX(target);
        int st1Y = ui.stackY(target);

        int xRoll = roll(0,2);
        int xMiss = stW/4 + (xRoll *stW/4);
        int yMiss = xRoll == 1 ? stH/4+roll(0,1) *stH/2 : stH/4+(roll(0,2) *stH/4);

        int x0 = st0X > st1X ? st0X+(stW/3) :st0X+(stW*2/3);
        int y0 = st0Y+stH/2;
        int x1 = st1X+xMiss;
        int y1 = st1Y+yMiss;
        drawAttack(source, target, x0, y0, x1, y1, wpnNum, 0);
    }
    @Override
    public void drawSuccessfulAttack(CombatStack source, CombatStack target, int wpnNum, float dmg) {
         if (!source.mgr.showAnimations())
            return;
        ShipBattleUI ui = source.mgr.ui;

        int stW = ui.stackW();
        int stH = ui.stackH();
        int st0X = ui.stackX(source);
        int st0Y = ui.stackY(source);
        int st1X = ui.stackX(target);
        int st1Y = ui.stackY(target);

        int x0 = st0X > st1X ? st0X+(stW/3) :st0X+(stW*2/3);
        int y0 = st0Y+stH/2;
        int x1 = st1X+stW/2;
        int y1 = st1Y+stH/2;
        drawAttack(source, target, x0, y0, x1, y1, wpnNum, dmg);
    }
    public void drawAttack(CombatStack source, CombatStack target, int x0, int y0, int x1, int y1, int wpnNum, float dmg) {
         if (!source.mgr.showAnimations())
            return;
        ShipBattleUI ui = source.mgr.ui;
        
        Graphics2D g = (Graphics2D) ui.getGraphics();
        Stroke prev = g.getStroke();

        int lineSpacing = BasePanel.s3;
        int ySpacing = source.x != target.x ? lineSpacing : 0;
        int xSpacing = source.y != target.y ? lineSpacing : 0;
        if ((source.x < target.x) && (source.y < target.y))
            ySpacing = -lineSpacing;
        else if ((source.x > target.x) && (source.y > target.y))
            ySpacing = -lineSpacing;

        float bombSpacing = (float) BasePanel.s10;
        float bombSize = (float) BasePanel.s2;
        float noBombSize = (float) BasePanel.s20;
        int bombLinePhase = BasePanel.s2 + BasePanel.s20;
        // "bombing" is 5 parallel and randomly staggered dash lines
        // animation is doing this 4 times
        for (int n=0;n<10;n++) {
            ui.paintAllImmediately();
            for (int i = -2; i < 3; i++) {
                int xAdj = i * xSpacing;
                int yAdj = i * ySpacing;
                int phase = roll(0,bombLinePhase-1);
                Color c0 = i % 2 == 1 ? Color.yellow : Color.green;
                g.setColor(c0);
                Stroke bombLine = new BasicStroke(BasePanel.s2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, bombSpacing, new float[]{bombSize, noBombSize}, phase);
                g.setStroke(bombLine);
                g.drawLine(x0 + xAdj, y0 + yAdj, x1 + (xAdj*2), y1 + (yAdj*2));
            }
            sleep(200);
        }
        g.setStroke(prev);
        String missLabel = dmg < 0 ? text("SHIP_COMBAT_DEFLECTED") : text("SHIP_COMBAT_MISS");
        target.drawAttackResult(g, x1,y1,x0, dmg,missLabel);   
        ui.paintAllImmediately();
        sleep(250);

    }
}
