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

import rotp.model.ai.AmoebaShipCaptain;
import rotp.model.events.RandomEventSpaceAmoeba;
import rotp.model.galaxy.StarSystem;

public class CombatStackSpaceAmoeba extends CombatStack {
    private static final int DAMAGE_FOR_SPLIT = 500;
    public CombatStackSpaceAmoeba() {
        num = 1;
        maxHits = hits = 3500;
        maxMove = move = 2;
        beamDefense = 1;
        missileDefense = 1;     
        reversed = random() < .5;
        captain = new AmoebaShipCaptain();
        image = image("SPACE_AMOEBA");
        scale = 1.5f;
    }
    @Override
    public String name()                { return text("SPACE_AMOEBA"); }
    @Override
    public boolean isMonster()          { return true; }
    @Override
    public boolean isArmed()            { return true; }
    @Override
    public void beginTurn() {
        super.beginTurn();
        // ok, we are splitting
        if ((maxHits - hits) >= DAMAGE_FOR_SPLIT) {
            float newMaxHits = (maxHits - DAMAGE_FOR_SPLIT) / 2;
            hits = maxHits = newMaxHits;
            ((AmoebaShipCaptain)captain).splitAmoeba(this);
        }
    }
    @Override
    public boolean hostileTo(CombatStack st, StarSystem sys)      { return !(st instanceof CombatStackSpaceAmoeba); }
    @Override
    public boolean canEat(CombatStack st)  { return (st instanceof CombatStackShip) || (st instanceof CombatStackColony); }
    @Override
    public boolean ignoreRepulsors()    { return true; }
    @Override
    public boolean canAttack(CombatStack target)  { 
        if (target.destroyed()) 
            return false;
        if (target.isColony() && !target.isArmed())
            return false;
        return (x == target.x) && (y == target.y);
    }
    @Override
    public boolean selectBestWeapon(CombatStack target)       { return canAttack(target); }
    @Override
    public void fireWeapon(CombatStack target)  { 
        if ((x == target.x) && (y == target.y)) 
            eatShips(target);
    }
    public void eatShips(CombatStack st) {
        if (st == null)
            return;
        if (!st.isShip() && !st.isColony())
            return;
        
        // only eats ships
        if (st.isShip()) {
            st.drawFadeOut(.025f);
            st.mgr.destroyStack(st); 
        }
        else if (st.isColony()) {
            CombatStackColony cStack = (CombatStackColony) st;
            st.mgr.destroyStack(st);
            RandomEventSpaceAmoeba.monster.degradePlanet(st.mgr.system());
            cStack.colonyDestroyed = true;
        }

        st.num = 0;

        // stop and enjoy the meal
        move = 0;
        if (st.mgr.showAnimations())
            st.mgr.ui.paintAllImmediately();
    }
    @Override
    public void endTurn() {
        super.endTurn();
    }
    @Override
    public boolean moveTo(int x1, int y1) {
        CombatStack potentialFood = mgr.stackAt(x1, y1);
        boolean stillAlive = super.moveTo(x1, y1);
        
        // if we made it successfully to the new dest
        // and there happens to be a ship here, eat it
        if (stillAlive) 
            eatShips(potentialFood);
        return stillAlive;
    }
    @Override 
    protected float takeDamage(float damage, float shieldAdj) {
        if (inStasis)
            return 0;
        attacked = true;

        // max damage that will trigger a split
        float maxDamage = hits+DAMAGE_FOR_SPLIT-maxHits;
        float actualDamage = min(maxDamage, damage);
        hits -= actualDamage;
        
        // if we are on smallest form and are reduced < 0, we are dead
        if (hits <= 0) {
            num = 0;
            mgr.destroyStack(this);
            return damage;
        }
        
        return actualDamage; 
    }
}