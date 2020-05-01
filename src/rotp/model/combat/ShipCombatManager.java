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
import java.util.Collections;
import java.util.List;
import rotp.model.empires.DiplomaticEmbassy;
import rotp.model.empires.Empire;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.SpaceMonster;
import rotp.model.galaxy.StarSystem;
import rotp.model.ships.ShipDesign;
import rotp.model.ships.ShipDesignLab;
import rotp.ui.RotPUI;
import rotp.ui.combat.ShipBattleUI;
import rotp.util.Base;

public class ShipCombatManager implements Base {
    private static final int MAX_TURNS = 100;
    private static Thread autoRunThread;
    // combat vars
    public ShipBattleUI ui;
    private StarSystem system;
    private List<Empire> empiresInConflict = new ArrayList<>();

    private final List<CombatStack> allStacks = new ArrayList<>();
    private boolean interdiction = false;
    public boolean autoComplete = false;
    public boolean autoResolve = false;
    public boolean performingStackTurn = false;
    public boolean showAnimations = true;
    public boolean playerInBattle = false;
    private CombatStack currentStack;
    private ShipCombatResults results;
    private boolean finished = false;
    public static final int maxX = 9;
    public static final int maxY = 7;
    private int turnCounter = 0;
    private final int[] startingPosn = { 30,40,20,50,10,60,0 };
    private final double[][] riskMap = new double[maxX+1][maxY+1];
    public boolean[][] asteroidMap = new boolean[maxX+1][maxY+1];

    public boolean interdiction()              { return interdiction; }
    public ShipCombatResults results()         { return results; }
    public StarSystem system()                 { return system; }
    public CombatStack currentStack()          { return currentStack; }
    public List<CombatStack> activeStacks()    { return results.activeStacks(); }
    public void ui(ShipBattleUI panel)         { ui = panel; }
    public boolean showAnimations()            { return showAnimations && (ui != null) && playerInBattle; }
    public List<CombatStack> allStacks()       { return allStacks; }

    public boolean involves(Empire emp) {
        return (results.attacker() == emp) || (results.defender() == emp);
    }
    public boolean redrawMap = false;
    public void battle(StarSystem sys) {
        playerInBattle = false;
        if (sys.hasMonster()) {
            battle(sys, sys.monster());
            galaxy().ships.disembarkFleets(system.id);
            return;                   
        }
        empiresInConflict = sys.empiresInConflict();
        log("Ship Combat starting in ", player().sv.name(sys.id), " between empires: ", empiresInConflict.toString());

        // build list of possible conflcts, where two orbiting fleets are in conflict at this system
        List<EmpireMatchup> matchups = new ArrayList<>();
        for (Empire e1: empiresInConflict) {
            for (Empire e2: empiresInConflict) {
                if (e1.aggressiveWith(e2, sys)) {
                    boolean alreadyAdded = false;
                    for (EmpireMatchup m: matchups) {
                        if (m.matches(e1,e2))
                            alreadyAdded = true;
                        
                    }
                    if (!alreadyAdded)
                        matchups.add(new EmpireMatchup(e1,e2));
                }
            }
        }
        
        // randomize the order that we do the potential combats
        Collections.shuffle(matchups);
        
        // decide for each matchup if we should start combat
        // if a fleet retreats or is destroyed in one combat, it will 
        //  be "null" in subsequent potential combats
        while (!matchups.isEmpty()) {
            EmpireMatchup match = matchups.get(0);
            matchups.remove(match);
            Empire emp1 = match.emp1;
            Empire emp2 = match.emp2;
            ShipFleet fleet1 = sys.orbitingFleetForEmpire(emp1);
            ShipFleet fleet2 = sys.orbitingFleetForEmpire(emp2);
            boolean fleet1Armed = (fleet1 != null) && fleet1.isArmed(sys);
            boolean fleet2Armed = (fleet2 != null) && fleet2.isArmed(sys);
            Empire homeEmpire = sys.empire();
            boolean startCombat = false;
            // if both fleets are armed, we always have combat
            if (fleet1Armed && fleet2Armed) 
                startCombat = true; 
            // if we have a colony that belongs to either of the fleets, combat 
            // rules are different
            else if (sys.isColonized()  && ((sys.empire() == emp1) || (sys.empire() == emp2))) {
                // if colony is armed, we have combat
                if (sys.colony().defense().isArmed())
                    startCombat = true;
                // else if colony is unarmed we might have combat if one of the fleets is armed
                // and there is a home fleet. If no home fleet, then this goes to the bombardment phase
                else if (fleet1Armed || fleet2Armed) {
                    if ((fleet1 != null) && (fleet1.empire() == homeEmpire)) 
                        startCombat = true;
                    else if ((fleet2 != null) && (fleet2.empire() == homeEmpire)) 
                        startCombat = true;
               }
            }
            // else there is no colony in combat.. start battle if at least one fleet is armed
            else if (fleet1Armed || fleet2Armed)
                startCombat = true;
            // if any of those choices matched, begin combat
            if (startCombat) {
                battle(sys, emp1, emp2);
                Empire victor = results.victor();
                if (emp1 != victor)
                    retreatEmpire(emp1);
                if (emp2 != victor)
                    retreatEmpire(emp2);
                galaxy().ships.disembarkFleets(system.id);
            }
        }
    }
    public void battle(StarSystem sys, SpaceMonster monster) {
        monster.initCombat();
        empiresInConflict = sys.empiresInConflict();
        List<Empire> empires = new ArrayList<>(empiresInConflict);
        Collections.shuffle(empires);
        for (Empire emp: empires) {
            battle(sys, emp, monster);
            if (!monster.alive())
                break;
        }    
    }
    public boolean validSquare(int x, int y) {
        if ((x < 0) || (y< 0) || (x>maxX) || (y>maxY))
            return false;
        return !asteroidMap[x][y];
    }
    private void battle(StarSystem sys, Empire emp1, Empire emp2) {
        finished = false;
        playerInBattle = emp1.isPlayer() || emp2.isPlayer();
        
        ShipFleet fl1 = sys.orbitingFleetForEmpire(emp1);
        ShipFleet fl2 = sys.orbitingFleetForEmpire(emp2);
        
        if (fl1 != null)
            emp2.encounterFleet(fl1);
        if (fl2 != null)
            emp1.encounterFleet(fl2);
        
        beginInSystem(sys, emp1, emp2);
        log("Resolving ship battle between empire1:", emp1.name(), "  empire2:", emp2.name());
        ui = null;

        setupBattle(emp1, emp2);

        if (combatIsFinished())
            return;

        checkDeclareWar(emp1, emp2);
        checkDeclareWar(emp2, emp1);

        if (playerInBattle())
            RotPUI.instance().promptForShipCombat(this);
        else {
            resolveAllCombat();
            endOfCombat(true);
        }
    }
    private void battle(StarSystem sys, Empire emp, SpaceMonster monster) {
        finished = false;
        playerInBattle = emp.isPlayer();
        system = sys;
        results = new ShipCombatResults(this, system, emp, monster);
        if (system.empire() == emp)
            emp.lastAttacker(monster);
        monster.lastAttacker(emp);
        log("Resolving ship battle between empire1:", emp.name(), "  monster:", monster.name());
        setupBattle(emp, monster);
        if (combatIsFinished())
            return;

        if (emp.isPlayer())
            RotPUI.instance().promptForShipCombat(this);
        else {
            resolveAllCombat();
            endOfCombat(true);
        }
    }
    private void beginInSystem(StarSystem s, Empire emp1, Empire emp2) {
        system = s;
        results = new ShipCombatResults(this, system, emp1, emp2);
        
        // set last attacker for colony empire in case genocide occurs
        if (system.empire() == emp1)
            emp1.lastAttacker(emp2);
        else if (system.empire() == emp2)
            emp2.lastAttacker(emp1);
    }
    private boolean playerInBattle() {
        for (CombatStack st : results.activeStacks()) {
            if (st.isPlayer())
                return true;
        }
        return false;
    }
    public void toggleAutoComplete() {
        autoComplete = !autoComplete;
        log("Toggling Auto Complete: "+autoComplete);
        if (autoComplete) {
            autoRunThread = new Thread(autoRunProcess());
            autoRunThread.start();
        }
        else
            continueToNextPlayerStack();
    }
    public void resolveAllCombat() {
        autoComplete = true;
        autoResolve = true;
        performingStackTurn = true;
        while (shouldContinue())
            performNextStackTurn();
    } 
    public boolean shouldContinue() {
        return autoComplete && !combatIsFinished();
    }
    public void continueToNextPlayerStack() {
        log("Continuing To Next Player Stack");
        if (combatIsFinished()) 
            return;
        
        performingStackTurn = true;
        currentStack.performTurn();
        boolean playerTurn = false;
        while (!playerTurn) {
            if (!currentStack.usingAI() && !currentStack.isTurnComplete())
                playerTurn = true;
            else
                performNextStackTurn();
            if (combatIsFinished()) 
                return;
        }

        performingStackTurn = false;
    }
    private Runnable autoRunProcess() {
        return () -> {
            performingStackTurn = true;
            while (shouldContinue())
                performNextStackTurn();
            performingStackTurn = false;
        };
    }
    public boolean canScan(Empire civ, CombatStack st) {
        if (st.empire == civ)
            return true;
        if (st.cloaked)
            return false;
        if (system.empire() == civ)
            return true;

        for (CombatStack stack : results.activeStacks()) {
            if ((stack.empire == civ) && stack.canScan())
                return true;
        }
        return false;
    }
    public void setupBombardment(StarSystem sys, ShipFleet fleet) {
        ui = null;
        system = sys;
        checkDeclareWar(fleet.empire(), system.empire());

        beginInSystem(system, fleet.empire(), null);

        if (results.colonyStack != null)
            addStackToCombat(results.colonyStack);

        addInitialStacks(fleet.empire());
        scanShips();
        addEmpiresToCombat();
        results().defender = system.empire();
    }
    private void checkDeclareWar(Empire emp1, Empire emp2) {
        // decide if we should declare war
        if (emp1.isPlayer())
            return;  // player can declare his own wars
        
        // dont automaticall trigger war over uncolonized systems
        if (!system.isColonized())
            return;
        
        // don't automatially trigger when emp2 stumbled into our system
        if (system.empire() == emp1)
            return;
        DiplomaticEmbassy emb1 = emp1.viewForEmpire(emp2).embassy();
        if (!emb1.anyWar()) {
            if (emb1.onWarFooting())
                emb1.declareWar();
        }
    }
    private void setupBattle(Empire emp1, Empire emp2) {
        raiseHostilityLevels();

        turnCounter = 0;
        interdiction = false;
        performingStackTurn = false;
        autoComplete = false;
        autoResolve = false;
        showAnimations = true;
        redrawMap = true;
        initCombatStacks(emp1, emp2);

        if (combatIsFinished())
            return;

        placeAsteroids();
        placeCombatStacks();
        allStacks.clear();
        allStacks.addAll(results.activeStacks());

        Collections.sort(results.activeStacks(), CombatStack.INITIATIVE);
        currentStack = results.activeStacks().get(0);
        currentStack.beginTurn();
    }
    private void setupBattle(Empire emp, SpaceMonster monster) {
        turnCounter = 0;
        interdiction = false;
        performingStackTurn = false;
        autoComplete = false;
        autoResolve = false;
        showAnimations = true;
        redrawMap = true;
        initCombatStacks(emp, monster);

        if (combatIsFinished())
            return;

        placeAsteroids();
        placeCombatStacks();
        allStacks.clear();
        allStacks.addAll(results.activeStacks());

        Collections.sort(results.activeStacks(), CombatStack.INITIATIVE);
        currentStack = results.activeStacks().get(0);
        currentStack.beginTurn();
    }
    public void endOfCombat(boolean logIncidents) {
        // send retreating ships on their way
        results.killRebels();

        results.refreshSystemScans();

        if (logIncidents)
            results.logIncidents();
    }
    public void retreatStack(CombatStackShip stack, StarSystem s) {
        log("Retreating: ", stack.fullName());
        performingStackTurn = true;
        stack.drawRetreat();
        results.addShipsRetreated(stack.design, stack.num);
        removeFromCombat(stack);
        stack.retreatToSystem(s);
        //turnDone(stack);
        performingStackTurn = false;
    }
    public void destroyStack(CombatStack stack) {
        log("Destroyed: ", stack.fullName());
        if (stack instanceof CombatStackShip)
            results.addShipStackDestroyed(((CombatStackShip)stack).design, stack.num);
        else if (stack instanceof CombatStackColony)
            results.addBasesDestroyed(stack.num);

        stack.becomeDestroyed();
        
        if (!stack.isColony())
           removeFromCombat(stack);
        if (stack == currentStack)
            turnDone(stack);
    }
    private void raiseHostilityLevels() {
        List<ShipFleet> fleets = system.orbitingFleets();
        for (ShipFleet fl : fleets)
            fl.empire().sv.raiseHostility(system.id);
    }
    private void addInitialStacks(Empire emp) {
        CombatStack colonyWard = null;
        if ((results.colonyStack != null) && (results.colonyStack.colony.empire() == emp))
            colonyWard = results.colonyStack;
        
        ShipFleet fl = system.orbitingFleetForEmpire(emp);
        if (fl == null)
            return;

        fl.system(system);
        fl.retreating(false);
        for (int i=0;i<ShipDesignLab.MAX_DESIGNS;i++) {
            if (fl.num(i) > 0) {
                ShipDesign d = fl.empire().shipLab().design(i);
                if (d != null) {
                    CombatStackShip stack = new CombatStackShip(fl, i, this);
                    if (stack.isArmed())
                        stack.ward(colonyWard);
                    addStackToCombat(stack);
                }
            }
        }
    }
    public void initCombatStacks(Empire emp1, Empire emp2) {
        if (results.colonyStack != null)
            addStackToCombat(results.colonyStack);

        addInitialStacks(emp1);
        addInitialStacks(emp2);
        scanShips();

        // unless system has monster, remove any stacks that want to retreat
        if (system.hasMonster())
            return;

        boolean retreating = true;
        List<CombatStack> retreatingFleets = new ArrayList<>();

        while (retreating) {
            retreatingFleets.clear();
            for (CombatStack st : results.activeStacks()) {
                if (st.usingAI() && st.wantToRetreat()) {
                    if (st.retreat())
                        retreatingFleets.add(st);
                }
            }
            results.activeStacks().removeAll(retreatingFleets);
            retreating = !retreatingFleets.isEmpty();
        }
        List<Empire> passives = new ArrayList<>();
        passives.add(emp1);
        passives.add(emp2);
        // ships & armed colonies mean there is still a
        // "combatable" stack for the empire
        for (CombatStack st: results.activeStacks()) {
            if (st.isArmed() || !st.isColony())
                passives.remove(st.empire);
        }
        for (Empire passiveEmp: passives) {
            log("retreating empires from init: ",passives.toString());
            empiresInConflict.remove(passiveEmp);
        }
    }
    public void initCombatStacks(Empire emp, SpaceMonster monster) {
        if (results.colonyStack != null)
            addStackToCombat(results.colonyStack);

        addInitialStacks(emp);
        for (CombatStack st: monster.combatStacks()) {
            if (!st.destroyed())
                addStackToCombat(st);
        }

        // unless system has monster, remove any stacks that want to retreat
        if (system.hasMonster())
            return;

        boolean retreating = true;
        List<CombatStack> retreatingFleets = new ArrayList<>();

        while (retreating) {
            retreatingFleets.clear();
            for (CombatStack st : results.activeStacks()) {
                if (st.usingAI() && st.wantToRetreat()) {
                    if (st.retreat())
                        retreatingFleets.add(st);
                }
            }
            results.activeStacks().removeAll(retreatingFleets);
            retreating = !retreatingFleets.isEmpty();
        }
        List<Empire> passives = new ArrayList<>();
        passives.add(emp);
        // ships & armed colonies mean there is still a
        // "combatable" stack for the empire
        for (CombatStack st: results.activeStacks()) {
            if (st.isArmed() || !st.isColony())
                passives.remove(st.empire);
        }
        for (Empire passiveEmp: passives) {
            log("retreating empires from init: ",passives.toString());
            empiresInConflict.remove(passiveEmp);
        }
    }
    public void retreatEmpire(Empire e) {
        List<CombatStack> retreatingStacks = new ArrayList<>();

        List<CombatStack> activeStacks = new ArrayList<>(results.activeStacks());
        for (CombatStack st : activeStacks) {
            if ((st.empire == e) && st.isShip()) {
                CombatStackShip ship = (CombatStackShip) st;
                if (ship.retreat()) {
                    retreatingStacks.add(ship);
                    ship.drawRetreat();
                }
            }
        }
        results.activeStacks().removeAll(retreatingStacks);
    }
    public void addEmpiresToCombat() {
        boolean playerInCombat = false;
        List<Empire> empiresInCombat = new ArrayList<>();
        for (CombatStack st : results.activeStacks()) {
            if (st.isPlayer())
                playerInCombat = true;
            if (!empiresInCombat.contains(st.empire))
                empiresInCombat.add(st.empire);
        }

        // build civs array, placing player ships first
        results.clearEmpires();
        if (playerInCombat) {
            results.addEmpire(player());
            empiresInCombat.remove(player());
        }

        for (Empire c : empiresInCombat)
            results.addEmpire(c);
    }
    private void placeAsteroids() {
        boolean isAsteroidSystem = system().planet().type().isAsteroids();
        CombatStackColony colony = results().colonyStack;
        int beltW = isAsteroidSystem ? 8: 4;
        int startX = 0;
        if (isAsteroidSystem)
            startX = 1;
        else if (colony == null)
            startX = 3;
        else if (colony.isPlayer())
            startX = 4;
        else
            startX = 2;
        int endX = startX+beltW;
        for (int x=startX; x<endX;x++) {
            int num = 0;
            for (int y=0;y<=maxY;y++) {
                asteroidMap[x][y] = false;
                if ((num < 4) && (random() < 0.375)) {
                    num++;
                    asteroidMap[x][y] = true;
                }
            }
        }
    }
    private void trimAsteroids() {
        for (int x=0; x<=maxX; x++) {
            for (int y=0;y<=maxY; y++) {
                if (asteroidMap[x][y] && (random() < .03)) {
                    asteroidMap[x][y] = false;
                    redrawMap = true;
                    break;
                }
            }
        }
    }
    private void placeCombatStacks() {
        addEmpiresToCombat();

        int[] posnAdj = { 0,9 };
        int empIndex = 0;
        // for each empire, place ship stacks
        for (Empire c : results.empires()) {
            int stackIndex = 0;
            for (CombatStack st : results.activeStacks()) {
                if (st.empire == c) {
                    int stackPosn = startingPosn[stackIndex] + posnAdj[empIndex];
                    st.x = stackPosn % 10;
                    st.y = stackPosn / 10;
                    if (st.x > 5)
                        st.reverse();
                    stackIndex++;
                    log("Ship Stack: "+st);
                }
            }
            empIndex++;
        }

        // set interdiction flag
        interdiction = false;

        if (results.colonyStack != null) {
            if (results.colonyStack.colony.hasInterdiction()) {
                for (CombatStack st : results.activeStacks()) {
                    if (st.hasTeleporting() && st.aggressiveWith(results.colonyStack))
                        interdiction = true;
                }
            }
        }
    }
    public CombatStack moveStackNearest(CombatStack newStack, int x, int y) {
        float minDist = Float.MAX_VALUE;
        List<Integer> nearX = new ArrayList<>();
        List<Integer> nearY = new ArrayList<>();
        for (int x1=0;x1<=maxX;x1++) {
            for (int y1=0;y1<=maxY;y1++) {
                if (asteroidMap[x1][y1])
                    continue;
                float dist = distance(x, y,x1, y1);
                if ((dist == 0) || (dist > minDist))
                    continue;
                CombatStack prevStack = stackAt(x1,y1);
                if ((prevStack != null) && !newStack.canEat(prevStack))
                    continue;
                if (dist < minDist) {
                    nearX.clear();
                    nearY.clear();
                }
                minDist = dist;
                nearX.add(x1);
                nearY.add(y1);
            }
        } 
        int index = roll(0, nearX.size()-1);
        int tgtX = nearX.get(index);
        int tgtY = nearY.get(index);
        CombatStack tgtStack = stackAt(tgtX, tgtY);
        this.moveStack(newStack, tgtX, tgtY);
        //newStack.x = tgtX;
        //newStack.y = tgtY;
        return tgtStack;
    }
    public void scanShips() {
        // scan only if have scanners and NOT same civ as planet (already scanned)
        for (CombatStack st : results.activeStacks()) {
            if (st.canScan()) {
                for (CombatStack st2 : results.activeStacks()) {
                    if (st2.isShip()) {
                        CombatStackShip sh2 = (CombatStackShip) st2;
                        st.empire.scanDesign(sh2.design, st2.empire);
                    }
                }
            }
        }
    }
    private boolean remainingStacksInConflict() {
        // check remaining stacks for conflict, excluding unarmed colony stacks
        // which may have been added as potential bombing targets
        List<CombatStack> combatableStacks = new ArrayList<>();
        List<CombatStack> activeStacks = new ArrayList<>(results.activeStacks());
        for (CombatStack st: activeStacks) {
            if (st.isColony()) {
                if (st.isArmed())
                    combatableStacks.add(st);
            }
            else
                combatableStacks.add(st);
        }
        for (CombatStack stack1 : combatableStacks) {
            for (CombatStack stack2 : combatableStacks) {
                if (stack1.isArmed() || stack2.isArmed()) {
                    if ((stack1 != stack2) && stack1.hostileTo(stack2, system))
                        return true;
                }
            }
        }
        return false;
    }
    public void addStackToCombat(CombatStack st) {
        st.mgr = this;

        if (st.isMissile()) {
            CombatStackMissile miss =(CombatStackMissile) st;
            miss.target.addMissile(miss);
            return;
        }
        results.activeStacks().add(st);
    }

    public CombatStack stackAt(int x, int y) {
        for (CombatStack st : results.activeStacks()) {
            if (st.atGrid(x,y))
                return st;
        }
        return null;
    }
    public boolean canMoveTo(CombatStack st, int x, int y) {
        if ((x < 0) || (x > maxX) || (y < 0) || (y > maxY))
            return false;

        if (!st.canCollide() && (stackAt(x, y) != null))
            return false;

        if (st.canTeleport() && !interdiction)
            return true;

        return st.canMoveTo(x, y);
    }
    public boolean canTacticallyMoveTo(CombatStack st, int x, int y) {
        if ((x < 0) || (x > maxX) || (y < 0) || (y > maxY))
            return false;

        if (asteroidMap[x][y])
            return false;

        if (!st.canCollide() && (stackAt(x, y) != null))
            return false;

        return st.canMoveTo(x, y);
    }
    public boolean canTeleportTo(CombatStack st, int x, int y) {
        if ((x < 0) || (x > maxX) || (y < 0) || (y > maxY))
            return false;

        if (asteroidMap[x][y])
            return false;

        if (!st.canCollide() && (stackAt(x, y) != null))
            return false;

        if (st.canTeleport() && !interdiction)
            return true;

        return false;
    }
    public boolean finished()    { return finished; }
    public boolean combatIsFinished() {
        if (finished)
            return true;
        // stop after max turns to avoid infinite looping
        if (turnCounter > MAX_TURNS) {
            retreatEmpire(results.attacker());
            log("combat finished-- max turns exceeded. Retreating: "+results.attacker());
            finished = true;
            if (showAnimations())
                ui.showResult();
            return true;
        }

        // no one is in conflict, the battle is over
        if (!remainingStacksInConflict()) {
            log("combat finished-- remaining stacks unarmed or not in conflict");
            finished = true;
            if (showAnimations())
                ui.showResult();
            endOfCombat(true);
            return true;
        }
        return false;
    }
    public void turnDone(CombatStack st) {
        log(st.fullName(), " - Done");
        st.endTurn();

        List<CombatStack> stacks = new ArrayList<>(results.activeStacks());
        if (stacks.isEmpty()) {
            endOfCombat(true);
            return;
        }
        
        int i = stacks.indexOf(st);
        if (i+1 == stacks.size()) {
            Collections.sort(stacks, CombatStack.INITIATIVE);
            currentStack = stacks.get(0);
            turnCounter++;
            trimAsteroids();
        }
        else
            currentStack = stacks.get(i+1);

        currentStack.beginTurn();
    }
    public void performNextStackTurn() {
        generateRiskMap(currentStack);
        currentStack.performTurn();
    }
    public void removeFromCombat(CombatStack st) {
        if (currentStack == st)
            turnDone(st);
        activeStacks().remove(st);
        if (!st.isColony())
            allStacks.remove(st);
        if (st.isMissile()) {
            CombatStackMissile miss = (CombatStackMissile) st;
            miss.target.missiles().remove(miss);
        }
    }
    public void performMoveStackToPoint(CombatStack st, int x, int y) {
        if (!st.canMove())
            return;
        else if ((x == st.x) && (y == st.y))
            return;
        else if (st.canMoveTo(x, y))
            moveStack(st, x, y);
        else
            teleportStack(st, x, y);
    }
    public void performMoveStackAlongPath(CombatStack st, FlightPath path) {
        // if proposed path is too long for this stack's remaining
        // move but the stack can teleport, then do that,
        if ((path.size() > st.move) && st.canTeleport) {
            teleportStack(st, path.destX(), path.destY());
            return;
        }

        // proposed path may be too long for this stack. If stack
        // can't teleport, cut down length of path
        if (path.size() > st.move) 
            path.limitMoves((int)st.move);

        // movet the stack along it's path until done or destroyed (by missiles)
        for (int i=0;i<path.size();i++) {
            if (!st.destroyed()) 
                moveStack(st, path.mapX(i), path.mapY(i));
        }
    }
    public boolean moveStack(CombatStack st, int x1, int y1) {
        log(currentStack.fullName(), " moving to: ", str(x1), ",", str(y1));
        return st.moveTo(x1,y1);
    }
    public void teleportStack(CombatStack st, int x1, int y1) {
        log(currentStack.fullName() + " teleporting to: " + x1 + "," + y1);
        st.teleportTo(x1,y1, 0.1f);
    }
    public void performAttackTarget(CombatStack st) {
        while (st.selectBestWeapon(st.target)) 
            st.fireWeapon(st.target);
    }
    public void attackTarget(CombatStack st) {
        //log("mgr attackTarget done:" + st.civ.race.name + st.name());
    }
    private void generateRiskMap(CombatStack st) {
        for (int x=0;x<=maxX;x++) {
            for (int y=0;y<maxY;y++)
                    riskMap[x][y] = riskAt(st, x, y);
        }
    }
    private double riskAt(CombatStack stack, int x, int y) {
        return 0.0d;
    }
    public boolean[] validMoveMap(CombatStack stack) {
        int gridW = maxX+3;
        int gridH = maxY+3;
        boolean[] valid = new boolean[gridW*gridH];

        // outside borders are non-traversable
        for (int x=0;x<gridW;x++) {
            for (int y=0;y<gridH;y++)
                valid[y*gridW+x] = (x>0) && (x<gridW-1) && (y>0) && (y<gridH-1);
        }

        // asteroids are not traversable
        for (int x=0;x<=maxX;x++) {
            for (int y=0;y<=maxY;y++) {
                if (asteroidMap[x][y])
                    valid[(y+1)*gridW+(x+1)] = false;
            }
        }

        // combat stacks are not traversable
        // enemy stacks may have a repulsor range that is also not traversable
        List<CombatStack> stacks = new ArrayList<>(results.activeStacks());
        for (CombatStack s: stacks) {            
            int r = stack.cloaked || (s.empire == stack.empire) || s.inStasis || stack.ignoreRepulsors() ? 0 : s.repulsorRange();
            if ((r == 0) && stack.canEat(s)) 
                continue;
            else if (r == 0) 
                valid[(s.y+1)*gridW+(s.x+1)] = false;
            else {
                for (int x=0-r;x<=r;x++) {
                    for (int y=0-r;y<=r;y++) {
                        int x0 = s.x+x+1;
                        int y0 = s.y+y+1;
                        valid[y0*gridW+x0] = false;
                    }
                }
            }
        }
        return valid;
    }
    class EmpireMatchup {
        Empire emp1;
        Empire emp2;
        public EmpireMatchup(Empire e1, Empire e2) {
            emp1 = e1;
            emp2 = e2;
        }
        public boolean matches (Empire e1, Empire e2) {
            if ((emp1 == e1) && (emp2 == e2))
                return true;
            if ((emp1 == e2) && (emp2 == e1))
                return true;
            return false;
        }
        public boolean includes(Empire e1) {
            return (emp1 == e1) || (emp2 == e1);
        }
    }
}
