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
package rotp.model.combat;

import rotp.model.ai.CrystalShipCaptain;
import rotp.model.events.RandomEventSpaceCrystal;
import rotp.model.galaxy.StarSystem;
import rotp.model.ships.ShipWeaponMissileType;

public class CombatStackSpaceCrystal extends CombatStack {
    private static final int MAX_WEAPON_DAMAGE = 1000;
    private boolean weaponUsed = false;
    public CombatStackSpaceCrystal() {
        num = 1;
        maxHits = hits = 7000;
        canTeleport = true;
        beamDefense = 1;
        missileDefense = 1;
        maxShield = shield = 5.0f;
        captain = new CrystalShipCaptain();
        image = image("SPACE_CRYSTAL");
    }    
    public float missileInterceptPct(ShipWeaponMissileType wpn)   {
        return max(0, 0.75f - (0.01f * wpn.tech().level));
    }
    @Override
    public String name()                { return text("SPACE_CRYSTAL"); }
    @Override
    public boolean isMonster()          { return true; }
    @Override
    public boolean isArmed()            { return true; }
    @Override
    public boolean immuneToStasis()     { return true; }
    @Override
    public void reloadWeapons()         { weaponUsed = false; };
    @Override
    public boolean hostileTo(CombatStack st, StarSystem sys)  { return true; }
    @Override
    public boolean selectBestWeapon(CombatStack target)       { return !weaponUsed; }
    @Override
    public void fireWeapon(CombatStack target)  { 
        weaponUsed = true;
        float dam = roll(1,MAX_WEAPON_DAMAGE);

        drawAttack();

        String attackText = text("SHIP_COMBAT_MISS");
        for (int x0=x-1; x0<=x+1; x0++) {
            for (int y0=y-1; y0<=y+1; y0++) {
                CombatStack st = mgr.stackAt(x0,y0);
                if ((st != null) && (st != this)) {
                    if (st.isShip() || st.isMonster() || st.isColony()) {
                        st.drawDamageTaken(dam, attackText);
                        st.takePulsarDamage(dam, 1);
                        st.attackLevel = max(0, st.attackLevel-1);
                        st.repairPct = max(0, st.repairPct-0.05f);
                    }
                    if (st.isColony() && st.destroyed()) {
                        CombatStackColony cStack = (CombatStackColony) st;
                        st.mgr.destroyStack(st);
                        RandomEventSpaceCrystal.monster.degradePlanet(st.mgr.system());                   
                        cStack.colonyDestroyed = true;
                    }
                }
            }
        }
    }
    @Override
    public float initiativeRank() {
        return 100;
    }
    public void drawAttack() { 
        if (!mgr.showAnimations())
            return;
        
        brighten = 1.0f;
        for (int i=0;i<2;i++) {
            scale += 1.5;
            transparency -= 0.45;
            brighten -= .005;
            long t0 = System.currentTimeMillis();
            mgr.ui.paintAllImmediately();
            long t1 = System.currentTimeMillis() - t0;
            if (t1 < 50)
                sleep(50-t1);
        }
        for (int i=0;i<12;i++) {
            scale -= 0.25;
            transparency += 0.075;
            brighten -= 0.075f;
            long t0 = System.currentTimeMillis();
            mgr.ui.paintAllImmediately();
            long t1 = System.currentTimeMillis() - t0;
            if (t1 < 50)
                sleep(50-t1);
        }
        brighten = 0;
        mgr.ui.paintAllImmediately();
    }
}
        