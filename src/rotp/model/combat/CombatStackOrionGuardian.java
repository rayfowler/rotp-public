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

import java.util.ArrayList;
import java.util.List;
import rotp.model.ai.OrionGuardianCaptain;
import rotp.model.galaxy.StarSystem;
import rotp.model.ships.ShipComponent;
import rotp.model.ships.ShipSpecial;
import rotp.model.ships.ShipSpecialBeamFocus;
import rotp.model.ships.ShipSpecialRepair;
import rotp.model.ships.ShipWeaponBeam;
import rotp.model.ships.ShipWeaponMissile;
import rotp.model.ships.ShipWeaponMissileType;
import rotp.model.ships.ShipWeaponTorpedo;
import rotp.model.tech.TechAutomatedRepair;
import rotp.model.tech.TechBeamFocus;
import rotp.model.tech.TechMissileWeapon;
import rotp.model.tech.TechShipWeapon;
import rotp.model.tech.TechTorpedoWeapon;

public class CombatStackOrionGuardian extends CombatStack {
    public final List<ShipComponent> weapons = new ArrayList<>();
    public int[] weaponCount = new int[4];
    public final ShipSpecial[] specials = new ShipSpecial[3];
    public int[] roundsRemaining = new int[4]; // how many rounds you can fire (i.e. missiles)
    public int[] shotsRemaining = new int[4]; // how many uses (shots) left onthe current turn
    public int[] baseTurnsToFire = new int[4];    // how many turns to wait before you can fire again
    public int[] wpnTurnsToFire = new int[4];    // how many turns to wait before you can fire again
    public ShipComponent selectedWeapon;
    public CombatStackOrionGuardian() {
        num = 1;
        maxHits = hits = 10000;
        maxMove = move = 2;
        maneuverability = 2;
        attackLevel = 10;
        beamDefense = 9;
        missileDefense = 9;
        maxShield = shield = 9.0f;
        repairPct = 0.30f;
        beamRangeBonus = 3;
        captain = new OrionGuardianCaptain();
        image = image("ORION_GUARDIAN");
        specials[0] = new ShipSpecialBeamFocus((TechBeamFocus)tech("BeamFocus:0"));
        specials[1] = new ShipSpecialRepair((TechAutomatedRepair)tech("AutomatedRepair:1"));
        weapons.add(new ShipWeaponMissile((TechMissileWeapon) tech("MissileWeapon:10"), false, 5, 7, 3.5f)); // scatter pack X missiles, modnar: fix missile range
        weapons.add(new ShipWeaponBeam((TechShipWeapon) tech("ShipWeapon:20"), false)); // stellar converters
        weapons.add(new ShipWeaponTorpedo((TechTorpedoWeapon) tech("TorpedoWeapon:3"))); // plasma torpedos
        weapons.add(new ShipWeaponBeam((TechShipWeapon) tech("ShipWeapon:16"), false)); // death ray
        weaponCount[0] = 85;
        weaponCount[1] = 45;
        weaponCount[2] = 18;
        weaponCount[3] = 1;
        roundsRemaining[0] = 5;
        roundsRemaining[1] = 1;
        roundsRemaining[2] = 1;
        roundsRemaining[3] = 1;
        shotsRemaining[0] = 1;
        shotsRemaining[1] = 1;
        shotsRemaining[2] = 1;
        shotsRemaining[3] = 1;
        baseTurnsToFire[0] = 1;
        baseTurnsToFire[1] = 1;
        baseTurnsToFire[2] = 2; // torps every other round
        baseTurnsToFire[3] = 1;
        wpnTurnsToFire[0] = 1;
        baseTurnsToFire[1] = 1;
        wpnTurnsToFire[2] = 1;
        wpnTurnsToFire[3] = 1;
        if (weapons.size() > 0)
            selectedWeapon = weapons.get(0);
    }    
    public float missileInterceptPct(ShipWeaponMissileType wpn)   {
        return max(0, 0.75f - (0.01f * wpn.tech().level));
    }
    @Override
    public String name()                { return text("PLANET_ORION_GUARDIAN"); }
    @Override
    public boolean isMonster()          { return true; }
    @Override
    public boolean isArmed()            { return true; }
    @Override
    public boolean immuneToStasis()     { return true; }
    @Override
    public int numWeapons()               { return weapons.size(); }
    @Override
    public ShipComponent weapon(int i)    { return weapons.get(i); }
    @Override
    public void reloadWeapons()         {
        for (int i=0;i<shotsRemaining.length;i++) 
            shotsRemaining[i] = 1;
        for (ShipComponent c: weapons)
            c.reload(); 
    };
    @Override
    public int shotsRemaining(int i) { return shotsRemaining[i]; }
    @Override
    public boolean hostileTo(CombatStack st, StarSystem sys)  { return true; }
    @Override
    public boolean selectBestWeapon(CombatStack target) {
        if (target.destroyed())
            return false;
        if (currentWeaponCanAttack(target))
            return true;

        rotateToUsableWeapon(target);
        return currentWeaponCanAttack(target);
    }
    @Override
    public void endTurn() {
        super.endTurn();
        for (int i=0;i<shotsRemaining.length;i++) 
            wpnTurnsToFire[i] = shotsRemaining[i] == 0 ? baseTurnsToFire[i] : wpnTurnsToFire[i]-1;          
    }
    @Override
    public void rotateToUsableWeapon(CombatStack target) {
        if (selectedWeapon == null)
            return;
        int i = weapons.indexOf(selectedWeapon);
        int j = i;
        boolean looking = true;

        while (looking) {
            j++;
            if (j == weapons.size())
                j = 0;
            selectedWeapon = weapons.get(j);
            if ((j == i) || currentWeaponCanAttack(target))
                looking = false;
        }
    }
    @Override
    public boolean canAttack(CombatStack st) {
        if (st == null)
            return false;
        if (st.inStasis)
            return false;
        for (int i=0;i<weapons.size();i++) {
            if (shipComponentCanAttack(st, i))
                return true;
        }
        return false;
    }
    @Override
    public boolean currentWeaponCanAttack(CombatStack target) {
        if (selectedWeapon() == null)
            return false;

        int wpn = weapons.indexOf(selectedWeapon());

        return shipComponentCanAttack(target, wpn);
    }
    private boolean shipComponentCanAttack(CombatStack target, int index) {
        if (target == null)
            return false;

        if (roundsRemaining[index]< 1)
            return false;

        if (shotsRemaining[index] < 1)
            return false;
        
        if (wpnTurnsToFire[index] > 1)
            return false;

        if (target.inStasis || target.isMissile())
            return false;

        ShipComponent shipWeapon = weapons.get(index);

        if (!shipWeapon.isWeapon())
            return false;

        if (shipWeapon.isLimitedShotWeapon() && (roundsRemaining[index] < 1))
            return false;

        if (shipWeapon.groundAttacksOnly() && !target.isColony())
            return false;

        int minMove = movePointsTo(target);
        if (weaponRange(shipWeapon) < minMove)
            return false;

        return true;
    }
    @Override
    public int weaponNum(ShipComponent comp) {
        return weapons.indexOf(comp);
    }
    @Override
    public int weaponIndex() {
        return weapons.indexOf(selectedWeapon);
    }
    @Override
    public void fireWeapon(CombatStack targetStack) {
        fireWeapon(targetStack, weaponIndex());
    }
    @Override
    public void fireWeapon(CombatStack targetStack, int index)  { 
         if (targetStack == null)
            return;

        target = targetStack;
        target.damageSustained = 0;
        // only fire if we have shots remaining... this is a missile concern
        if ((roundsRemaining[index] > 0)) {
            selectedWeapon = weapons.get(index);
            // some weapons (beams) can fire multiple per round
            int shots = (int) selectedWeapon.attacksPerRound();
            int count = num*shots*weaponCount[index];
            if (selectedWeapon.isMissileWeapon()) {
                CombatStackMissile missile = new CombatStackMissile(this, (ShipWeaponMissileType) selectedWeapon, count);
                log(fullName(), " launching ", missile.fullName(), " at ", targetStack.fullName());
                mgr.addStackToCombat(missile);
            }
            else {
                log(fullName(), " firing ", str(count), " ", selectedWeapon.name(), " at ", targetStack.fullName());
                selectedWeapon.fireUpon(this, target, count);
            }
            if (target == null) 
                log("TARGET IS NULL AFTER BEING FIRED UPON!");
            shotsRemaining[index] = max(0, shotsRemaining[index]-1);
            if (selectedWeapon.isLimitedShotWeapon())
                roundsRemaining[index] = max(0, roundsRemaining[index]-1);
            if (target.damageSustained > 0)
                log("weapon damage: ", str(target.damageSustained));
        }
        rotateToUsableWeapon(targetStack);
        target.damageSustained = 0;
    }
    @Override
    public float initiativeRank() {
        return 5;
    }
    @Override
    public ShipComponent selectedWeapon() { return selectedWeapon; }
    public void drawAttack() { 
        if (mgr.ui == null)
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
        