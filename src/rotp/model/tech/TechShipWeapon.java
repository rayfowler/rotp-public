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

import java.awt.*;
import rotp.model.combat.CombatStack;
import rotp.model.empires.Empire;
import rotp.model.ships.ShipComponent;
import rotp.model.ships.ShipWeaponBeam;
import rotp.ui.BasePanel;
import rotp.ui.combat.ShipBattleUI;

public final class TechShipWeapon extends Tech {
    private int damageLow = 0;
    private int damageHigh = 0;
    public int range = 1;

    public boolean heavyAllowed = false;
    private int heavyDamageLow = 0;
    private int heavyDamageHigh = 0;
    public int heavyRange = 2;

    public int attacksPerRound = 1;
    public int computer = 0;
    public float enemyShieldMod = 1;
    public boolean streaming = false;

    // graphic effects
    public int weaponWidth = 1;
    public int weaponSpread = 1;
    public boolean pellets = false;
    public boolean waves = false;

    private int beamStroke, heavyStroke, dashStroke;
    private transient Color beamColor;
    private Stroke weaponStroke;

    public int damageLow()       { return (int) (session().damageBonus() * damageLow); }
    public int damageHigh()      { return (int) (session().damageBonus() * damageHigh); }
    public int heavyDamageLow()  { return (int) (session().damageBonus() * heavyDamageLow); }
    public int heavyDamageHigh() { return (int) (session().damageBonus() * heavyDamageHigh); }
    
    protected String soundEffect() { return "ShipLaser"; }

    public TechShipWeapon(String typeId, int lv, int seq, boolean b, TechCategory c) {
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
        techType = Tech.SHIP_WEAPON;
        dashStroke = 0;

        switch(typeSeq) {
            case 0: // LASER
                damageLow = 1;
                damageHigh = 4;
                heavyAllowed = true;
                heavyDamageLow = 1;
                heavyDamageHigh = 7;
                cost = 8;
                size = 10;
                power = 25;
                beamColor = Color.yellow;
                beamStroke = 1;
                heavyStroke = 2;
                break;
            case 1: // GATLING LASER
                pellets = true;
                damageLow = 1;
                damageHigh = 4;
                attacksPerRound = 4;
                cost = 20;
                size = 20;
                power = 70;
                beamColor = Color.yellow;
                beamStroke = 1;
                heavyStroke = 2;
                break;
            case 2: // NEUTRON PELLET GUN
                pellets = true;
                damageLow = 2;
                damageHigh = 5;
                enemyShieldMod = .5f;
                cost = 7.5f;
                size = 15;
                power = 25;
                beamColor = Color.gray;
                beamStroke = 1;
                heavyStroke = 2;
                dashStroke = 1;
                break;
            case 3: // ION CANNON
                damageLow = 3;
                damageHigh = 8;
                heavyAllowed = true;
                heavyDamageLow = 3;
                heavyDamageHigh = 15;
                cost = 10;
                size = 15;
                power = 35;
                beamColor = Color.blue;
                beamStroke = 1;
                heavyStroke = 2;
                break;
            case 4: // MASS DRIVER
                pellets = true;
                weaponWidth = 2;
                damageLow = 5;
                damageHigh = 8;
                enemyShieldMod = .5f;
                cost = 18;
                size = 55;
                power = 50;
                beamColor = Color.gray;
                beamStroke = 1;
                heavyStroke = 1;
                dashStroke = 1;
                break;
            case 5: // NEUTRON BLASTER
                damageLow = 3;
                damageHigh = 12;
                heavyAllowed = true;
                heavyDamageLow = 3;
                heavyDamageHigh = 24;
                cost = 15;
                size = 20;
                power = 60;
                beamColor = Color.blue;
                beamStroke = 2;
                heavyStroke = 3;
                break;
            case 6: // GRAVITON BEAM
                damageLow = 1;
                damageHigh = 15;
                streaming = true;
                cost = 12;
                size = 30;
                power = 60;
                beamColor = Color.pink;
                beamStroke = 2;
                heavyStroke = 3;
                break;
            case 7: // HARD BEAM
                weaponWidth = 2;
                weaponSpread = 2;
                damageLow = 8;
                damageHigh = 12;
                enemyShieldMod = .5f;
                cost = 25;
                size = 50;
                power = 100;
                beamColor = Color.red;
                beamStroke = 2;
                heavyStroke = 2;
                dashStroke = 2;
                break;
            case 8: // FUSION BEAM
                damageLow = 4;
                damageHigh = 16;
                heavyAllowed = true;
                heavyDamageLow = 4;
                heavyDamageHigh = 30;
                cost = 13;
                size = 20;
                power = 75;
                beamColor = Color.yellow;
                beamStroke = 2;
                heavyStroke = 3;
                break;
            case 9: // MEGABOLT CANNON
                damageLow = 2;
                damageHigh = 20;
                computer = 3;
                cost = 16;
                size = 30;
                power = 65;
                beamColor = Color.yellow;
                beamStroke = 3;
                heavyStroke = 5;
                break;
            case 10: // PHASOR
                damageLow = 5;
                damageHigh = 20;
                heavyAllowed = true;
                heavyDamageLow = 5;
                heavyDamageHigh = 40;
                cost = 18;
                size = 20;
                power = 90;
                beamColor = Color.orange;
                beamStroke = 3;
                heavyStroke = 5;
                break;
            case 11: // AUTO-BLASTER
                damageLow = 4;
                damageHigh = 16;
                attacksPerRound = 3;
                cost = 24;
                size = 30;
                power = 90;
                beamColor = Color.yellow;
                beamStroke = 3;
                heavyStroke = 5;
                break;
            case 12: // TACHYON BEAM
                damageLow = 1;
                damageHigh = 25;
                streaming = true;
                cost = 18;
                size = 30;
                power = 80;
                beamColor = Color.pink;
                beamStroke = 3;
                heavyStroke = 5;
                break;
            case 13: // GAUSS AUTO-CANNON
                damageLow = 7;
                damageHigh = 10;
                enemyShieldMod = .5f;
                attacksPerRound = 4;
                cost = 40;
                size = 105;
                power = 105;
                beamColor = Color.yellow;
                beamStroke = 3;
                heavyStroke = 3;
                dashStroke = 2;
                break;
            case 14: // PARTICLE BEAM
                pellets = true;
                damageLow = 10;
                damageHigh = 20;
                enemyShieldMod = .5f;
                cost = 26;
                size = 90;
                power = 75;
                beamColor = Color.gray;
                beamStroke = 3;
                heavyStroke = 5;
                break;
            case 15: // PLASMA CANNON
                damageLow = 6;
                damageHigh = 30;
                cost = 24;
                size = 30;
                power = 110;
                beamColor = Color.yellow;
                beamStroke = 3;
                heavyStroke = 5;
                break;
            case 16: // DEATH RAY
                damageLow = 200;
                damageHigh = 1000;
                weaponWidth = 10;
                weaponSpread = 10;
                restricted = true;
                cost = 120;
                size = 2000;
                power = 2000;
                beamColor = Color.white;
                beamStroke = 7;
                heavyStroke = 7;
                break;
            case 17: // DISRUPTOR
                damageLow = 10;
                damageHigh = 40;
                range = 2;
                cost = 100;
                size = 70;
                power = 160;
                beamColor = Color.yellow;
                beamStroke = 3;
                heavyStroke = 5;
                break;
            case 18: // PULSE PHASOR
                damageLow = 5;
                damageHigh = 20;
                attacksPerRound = 3;
                cost = 42;
                size = 40;
                power = 120;
                beamColor = Color.orange;
                beamStroke = 3;
                heavyStroke = 5;
                break;
            case 19: // TRI-FOCUS PLASMA CANNON
                damageLow = 20;
                damageHigh = 50;
                cost = 55;
                size = 65;
                power = 180;
                beamColor = Color.yellow;
                beamStroke = 4;
                heavyStroke = 6;
                break;
            case 20: // STELLAR CONVERTOR
                damageLow = 10;
                damageHigh = 35;
                attacksPerRound = 4;
                range = 3;
                cost = 105;
                size = 200;
                power = 300;
                beamColor = Color.white;
                beamStroke = 4;
                heavyStroke = 6;
                break;
            case 21: // MAULER DEVICE
                damageLow = 20;
                damageHigh = 100;
                weaponWidth = 6;
                weaponSpread = 6;
                cost = 120;
                size = 150;
                power = 300;
                beamColor = Color.white;
                beamStroke = 5;
                heavyStroke = 5;
                break;
            case 22: // AMOEBA STREAM
                damageLow = 250;
                damageHigh = 1000;
                range = 3;
                streaming = true;
                restricted = true;
                beamColor = Color.green;
                beamStroke = 5;
                heavyStroke = 5;
                break;
            case 23: // CRYSTAL RAY
                damageLow = 100;
                damageHigh = 300;
                range = 3;
                attacksPerRound = 4;
                restricted = true;
                beamColor = Color.white;
                beamStroke = 4;
                heavyStroke = 4;
                break;
        }
    }
    @Override
    public float baseValue(Empire c) { return c.ai().scientist().baseValue(this); }
    @Override
    public float warModeFactor()        { return 2; }
    @Override
    public boolean providesShipComponent()  { return true; }
    @Override
    public float baseCost()   { return cost; }
    @Override
    public float baseSize()   { return size; }
    @Override
    public float basePower()   { return power; }
    @Override
    public boolean isObsolete(Empire c) {
        TechShipWeapon top = c.tech().topShipWeaponTech();
        if (top == null)
            return false;
        float currVal = (damageHigh() * attacksPerRound) / size;
        float tVal = (top.damageHigh()*top.attacksPerRound) / top.size;

        return tVal >= currVal;
    }
    @Override
    public void provideBenefits(Empire c) {
        super.provideBenefits(c);

        ShipWeaponBeam sh = new ShipWeaponBeam(this, false);
        c.shipLab().addWeapon(sh);
        if (!isObsolete(c))
            c.tech().topShipWeaponTech(this);
        
        if (heavyAllowed) {
            ShipWeaponBeam sh2 = new ShipWeaponBeam(this, true);
            c.shipLab().addWeapon(sh2);
        }
        if (c.isPlayer())
            galaxy().giveAdvice("MAIN_ADVISOR_SHIP_WEAPON");
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
        ShipBattleUI ui = source.mgr.ui;
        if (ui == null)
            return;

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
        drawAttack(source, target, x0, y0, x1, y1, wpnNum, dmg);
    }
    private void drawAttack(CombatStack source, CombatStack target, int x0, int y0, int x1, int y1, int wpnNum, float dmg) {
        ShipBattleUI ui = source.mgr.ui;
        if (!source.mgr.showAnimations()) 
            return;

        ShipComponent wpn = source.weapon(wpnNum);
        Graphics2D g = (Graphics2D) ui.getGraphics();
        Stroke prev = g.getStroke();

        g.setColor(beamColor);

        if ((dashStroke > 0) && (weaponStroke == null)) {
            int w = scaled(dashStroke);
            float dash = (float) w;
            weaponStroke = new BasicStroke(w, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{dash}, 0);
        }

        if (weaponStroke != null)
            g.setStroke(weaponStroke);
        else if (wpn.heavy())
            g.setStroke(BasePanel.baseStroke(heavyStroke));
        else
            g.setStroke(BasePanel.baseStroke(beamStroke));

        playAudioClip(soundEffect());

        if (streaming) {
            int xMod = (source.y == target.y) ? 0 : 1;
            int yMod = (source.x == target.x) ? 0 : 1;
            if ((source.x < target.x) && (source.y < target.y))
                xMod = -1;
            else if ((source.x > target.x) && (source.y > target.y))
                xMod = -1;
            for (int n = -5; n < 6; n++) {
                ui.paintCellsImmediately(source.x,target.x,source.y,target.y);
                int adj = scaled(n)*3;
                g.drawLine(x0, y0, x1+(xMod*adj), y1+(yMod*adj));
            }
        }
        else {
            int xAdj = scaled(roll(-4,4)*2);
            int yAdj = scaled(roll(-4,4)*2);
            g.drawLine(x0, y0, x1+xAdj, y1+yAdj);
            sleep(50);
            ui.paintAllImmediately();
        }

        String missLabel = dmg < 0 ? text("SHIP_COMBAT_DEFLECTED") : text("SHIP_COMBAT_MISS");
        g.setStroke(prev);
        target.drawAttackResult(g,x1,y1,x0, dmg,missLabel);   
        ui.paintAllImmediately();
    }
}
