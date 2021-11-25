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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import rotp.model.empires.Empire;
import rotp.model.galaxy.SpaceMonster;
import rotp.model.galaxy.StarSystem;
import rotp.model.incidents.AttackedAllyIncident;
import rotp.model.incidents.AttackedEnemyIncident;
import rotp.model.incidents.BioweaponIncident;
import rotp.model.incidents.ColonyAttackedIncident;
import rotp.model.incidents.ColonyDestroyedIncident;
import rotp.model.incidents.SkirmishIncident;
import rotp.model.ships.ShipDesign;
import rotp.util.Base;

public final class ShipCombatResults implements Base {
    StarSystem system;
    public CombatStackColony colonyStack;
    Empire defender, attacker;
    private final SpaceMonster monster;
    List<Empire> empires = new ArrayList<>();
    List<CombatStack> activeStacks = new ArrayList<>();
    Map<ShipDesign, Integer> shipsDestroyed = new HashMap<>();
    Map<ShipDesign, Integer> shipsDamaged   = new HashMap<>();
    Map<ShipDesign, Integer> shipsRetreated = new HashMap<>();
    int basesDestroyed = 0;
    List<Empire> usedBioweapons = new ArrayList<>();

    public StarSystem system()            { return system; }
    public Empire defender()              { return defender; }
    public Empire attacker()              { return attacker; }
    public SpaceMonster monster()         { return monster; }
    public List<Empire> empires()         { return empires; }
    public int basesDestroyed()           { return basesDestroyed; }
    public int popDestroyed()             { return colonyStack == null ? 0 : (int) Math.ceil(colonyStack.populationLost()); }
    public int factoriesDestroyed()       { return colonyStack == null ? 0 : (int) Math.ceil(colonyStack.factoriesLost()); }
    public void killRebels()              { if (colonyStack != null) colonyStack.killRebels(); }

    public List<CombatStack> activeStacks()  { return activeStacks; }
    public Map<ShipDesign, Integer> shipsDestroyed()  { return shipsDestroyed; }
    public Map<ShipDesign, Integer> shipsDamaged()    { return shipsDamaged; }
    public Map<ShipDesign, Integer> shipsRetreated()  { return shipsRetreated; }

    public float shipHullPointsDestroyed(Empire e) {
        float bc = 0;
        for (ShipDesign d: shipsDestroyed.keySet()) {
            if (d.empire() == e) {
                int num = shipsDestroyed.get(d);
                if (num > 0)
                    bc += (num * d.hullPoints());
            }
        }
        return bc;
    }
    public boolean isMonsterAttack()  { return monster != null; }
    public boolean isMonsterVictory() { return isMonsterAttack() && victor() == null; }
    public String aiRaceName()  {
        if (monster != null)
            return monster.name();
        else if (empires.get(0).isPlayer())
            return empires.get(1).raceName();
        else
            return empires.get(0).raceName();
    }
    public Empire aiEmpire() {
        if (monster != null)
            return null;
        else if (empires.get(0).isPlayer())
            return empires.get(1);
        else
            return empires.get(0);
    }
    public String victorName() {
        Empire victor = victor();
        if (victor != null)
            return victor.raceName();
        else if (isMonsterAttack())
            return monster.name();
        else 
            return "";
    }
    public void addBioweaponUse(Empire e) {
        if (!usedBioweapons.contains(e))
            usedBioweapons.add(e);
    }
    public float damageSustained(Empire e) {
        // calculates total damage sustained and results pct of total prod affected
        float bc = 0;
        for (ShipDesign d: shipsDestroyed.keySet()) {
            if (d.empire() == e)
                bc += (shipsDestroyed.get(d) * d.cost());
        }
        if (defender == e) {
            bc += (basesDestroyed * e.tech().newMissileBaseCost());
            bc += (factoriesDestroyed() * e.tech().baseFactoryCost());
            bc += (popDestroyed() * (e.tech().populationCost()+e.workerProductivity()));
        }
        float prod = e == null ? 0 : e.totalPlanetaryProduction();
        return prod <= 0 ? 1.0f : min(1.0f, bc / prod);
    }
    public ShipCombatResults(ShipCombatManager mgr, StarSystem s, Empire emp1, Empire emp2) {
        system = s;
        monster = null;
        
        // set up default attacker/defender assignment. 
        attacker = emp1;
        defender = emp2;
        
        boolean neutralSystem = !system.isColonized();

        // if system is colonized and one of the fleet is allied to it,
        // set it up as a defender
        if (system.isColonized()) {
            Empire sysEmpire = system.empire();
            if ((emp1 == sysEmpire) || emp1.alliedWith(sysEmpire.id)) {
                defender = emp1;
                attacker = emp2;
                if (defender == sysEmpire)
                    colonyStack = new CombatStackColony(system().colony(), mgr);
            }
            // this is when a fleet for emp1 is bombarding a colony with no defending fleet
            else if (emp2 == null) {
                attacker = emp1;
                defender = null;
                colonyStack = new CombatStackColony(system().colony(), mgr);
            }
            else if ((emp2 == sysEmpire) || emp2.alliedWith(sysEmpire.id)) {
                defender = emp2;
                attacker = emp1;
                if (defender == sysEmpire)
                    colonyStack = new CombatStackColony(system().colony(), mgr);
            }
            else
                neutralSystem = true;
        }
        
        // when there are two fleets in combat over a neutral colony (or in an
        // uncolonized system), then randomize which fleet is the "attacker" and
        // which is the "defender". This prevents using the combat turn limit as 
        // a way to maintain control in a system without engaging in combat since
        // the expiring turn limit requires the attacker fleet to retreat
        if (neutralSystem) {
            if (random() < 0.5) {
                attacker = emp1; 
                defender = emp2;
            }
            else {
                attacker = emp2;
                defender = emp1;
            }
        }

        empires.add(emp1);
        if (emp2 != null)
            empires.add(emp2);
    }
    public ShipCombatResults(ShipCombatManager mgr, StarSystem s, Empire emp, SpaceMonster m) {
        system = s;
        monster = m;

        // set up default attacker/defender assignment
        defender = emp;

        // if system is colonized and one of the fleet is allied to it,
        // set it up as a defender
        if (system.isColonized()) {
            Empire sysEmpire = system.empire();
            if ((emp == sysEmpire) || emp.alliedWith(sysEmpire.id)) {
                defender = emp;
                if (defender == sysEmpire)
                    colonyStack = new CombatStackColony(system.colony(), mgr);
            }
        }

        empires.add(emp);
    }
    public Empire victor() {
        // if there is a ship stack, that empire is the victor
        for (CombatStack st: activeStacks) {
            if (!st.isColony())
                return st.empire;
        }
        return activeStacks.isEmpty() ? null : activeStacks.get(0).empire;
    }
    public void addEmpire(Empire e)  { empires.add(e);  }
    public void clearEmpires()       { empires.clear(); }
    public void addBasesDestroyed(int num) {
        basesDestroyed += num;
    }
    public void addShipDestroyed(ShipDesign d, int count) {
        // called when individual ships in a stack are destroyed
        if (shipsDestroyed.containsKey(d))
            shipsDestroyed.put(d, count+shipsDestroyed.get(d));
        else
            shipsDestroyed.put(d, count);
    }
    public void addShipStackDestroyed(ShipDesign d, int count) {
        shipsDestroyed.put(d, count);
    }
    public void addShipsDamaged(ShipDesign d, int count) {
        if (shipsDamaged.containsKey(d))
            shipsDamaged.put(d, count+shipsDamaged.get(d));
        else
            shipsDamaged.put(d, count);
    }
    public void addShipsRetreated(ShipDesign d, int count) {
        if (shipsRetreated.containsKey(d))
            shipsRetreated.put(d, count+shipsRetreated.get(d));
        else
            shipsRetreated.put(d, count);
    }
    public void logIncidents() {
        for (Empire e: usedBioweapons)
            BioweaponIncident.create(defender(), e, system());

        if (monster != null) {
            if (!monster.alive()) 
                monster.plunder();
            return;
        }
        // if a neutral system, then a skirmish for all
        if (defender == null)
            SkirmishIncident.create(this);
        // if genocide, incidents already created at extinction time
        else if (defender.extinct())
                return;
        else if (colonyStack != null) {
            if (colonyStack.colonyDestroyed)
                ColonyDestroyedIncident.create(this);
            else if (colonyStack.attacked)
                ColonyAttackedIncident.create(this);
        }
        else
            SkirmishIncident.create(this);

        // attack enemy/ally incidents only trigger in a
        // defended system
        if (defender != null) {
            AttackedEnemyIncident.alert(attacker, defender, this);
            AttackedAllyIncident.alert(attacker, defender, this);
        }
    }
}
