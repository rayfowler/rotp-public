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
package rotp.model.ai.xilmi;

import java.awt.*;
import java.util.*;
import java.util.List;
import rotp.model.ai.interfaces.ShipCaptain;
import rotp.model.combat.*;
import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
import rotp.model.galaxy.StarSystem;
import rotp.model.ships.ShipComponent;
import rotp.model.ships.ShipDesign;
import rotp.model.tech.Tech;
import rotp.util.Base;

public class AIShipCaptain implements Base, ShipCaptain {
    private final Empire empire;
    private transient List<CombatStack> allies = new ArrayList<>();
    private transient List<CombatStack> enemies = new ArrayList<>();
    private CombatStack currentTarget = null;

    public List<CombatStack> allies() {
        if (allies == null)
            allies = new ArrayList<>();
        return allies;
    }
    public List<CombatStack> enemies() {
        if (enemies == null)
            enemies = new ArrayList<>();
        return enemies;
    }
    public AIShipCaptain (Empire c) {
        empire = c;
    }
    private ShipCombatManager combat()    { return galaxy().shipCombat(); }
    @Override
    public void performTurn(CombatStack stack) {
        ShipCombatManager mgr = galaxy().shipCombat();
        // missiles move during their target's turn
        // check if stack is still alive!
        if (stack.destroyed()) {
            mgr.turnDone(stack);
            return;
        }

        if (stack.isMissile()) {
            mgr.turnDone(stack);
            return;
        }

        if (stack.inStasis) {
            mgr.turnDone(stack);
            return;
        }

        if (empire.isPlayerControlled() && !combat().autoComplete) {
            mgr.turnDone(stack);
            return;
        }
        
        CombatStack prevTarget = null;
        
        boolean turnActive = true;
        while (turnActive) {
            float prevMove = stack.move;
            prevTarget = currentTarget;
            //ail: for moving we pick the target that is overall the most suitable, so that bombers move towards planet
            FlightPath bestPathToTarget;
            //ail: defend-stuff is problematic as stacks can be drawn out
            /*if((currentTarget == null || stack.movePointsTo(currentTarget) - stack.move > stack.maxFiringRange(currentTarget)) && stack.hasWard())
                bestPathToTarget = defendWardPath(stack, stack.ward());
            else*/
            bestPathToTarget = chooseTarget(stack, false, false);
            CombatStack tgtBeforeClose = currentTarget;
            //System.out.print("\n"+galaxy().currentTurn()+" "+empire.name()+" "+stack.fullName()+" performTurn");
            if (stack.isColony() && stack.canAttack(currentTarget)) 
            {
                //System.out.print("\n"+galaxy().currentTurn()+" "+empire.name()+" "+stack.fullName()+" supposed to fire at: "+currentTarget.fullName());
                stack.target = currentTarget;
                mgr.performAttackTarget(stack);
                mgr.turnDone(stack);
            }
            //ail: if our target to move to is not the same as the target we can currently shoot at, we shoot before moving
            // check for retreating
            if(tgtBeforeClose != null && stack.movePointsTo(tgtBeforeClose) > stack.move + stack.optimalFiringRange(tgtBeforeClose))
            {  
                chooseTarget(stack, true, false);
                if (stack.canAttack(currentTarget)) 
                    performSmartAttackTarget(stack, currentTarget);
                if(stack.isShip())
                {
                    if(shouldDodgeMissile((CombatStackShip)stack))
                    {
                        if (stack.mgr.autoResolve) {
                            Point destPt = findSafestPoint(stack);
                            if (destPt != null)
                                mgr.performMoveStackToPoint(stack, destPt.x, destPt.y);
                        }
                        else
                        {
                            FlightPath bestPathToSaveSpot = findSafestPath(stack);
                            if(bestPathToSaveSpot != null)
                                mgr.performMoveStackAlongPath(stack, bestPathToSaveSpot);
                            //System.out.print("\n"+stack.fullName()+" Kiting performed: "+(bestPathToSaveSpot != null));
                        }
                        //after we dodge we need to pick a new target including path to it as otherwise we think the path starts where we were before
                        bestPathToTarget = chooseTarget(stack, false, false);
                        //we also set our remaing move-points to 0 so we still attack stuff nearby but won't move back towards the missiles we just fled from
                        stack.move = 0;
                    }
                }
                currentTarget = tgtBeforeClose;
            }
            boolean shouldPerformKiting = false;
            if(stack.isShip())
            {
                for (int i=0;i<stack.numWeapons(); i++) {
                    if(stack.weapon(i).isMissileWeapon())
                        shouldPerformKiting = true;
                }
            }
            if(stack.repulsorRange() > 0)
                shouldPerformKiting = true;
            
            //When we are defending and can't get into attack-range of the enemy, we let them come to us
            /*if(currentTarget != null)
                System.out.println(stack.fullName()+" target: "+currentTarget.fullName()+" distaftermove: "+(stack.movePointsTo(currentTarget) - stack.move)+" DistToBeAt: "+DistanceToBeAt(stack, currentTarget));*/
            // if we need to move towards target, do it now
            if(currentTarget != null && stack.movePointsTo(currentTarget) >= stack.move + stack.optimalFiringRange(currentTarget))
            {  
                if (wantToRetreat(stack) && stack.canRetreat()) {
                    CombatStackShip shipStack = (CombatStackShip) stack;
                    StarSystem dest = retreatSystem(shipStack.mgr.system());
                    if (dest != null) {
                        mgr.retreatStack(shipStack, dest);
                        //System.out.print("\n"+stack.fullName()+" target: "+currentTarget.fullName()+" retreat because it wants to.");
                        return;
                    }
                }
            }
            
            if (currentTarget != null) {
                if (stack.mgr.autoResolve) {
                    Point destPt = findClosestPoint(stack, currentTarget);
                    if (destPt != null)
                        mgr.performMoveStackToPoint(stack, destPt.x, destPt.y);
                }
                else if ((bestPathToTarget != null) && (bestPathToTarget.size() > 0)) {
                    mgr.performMoveStackAlongPath(stack, bestPathToTarget);
                }
            }
            
            // if can attack target this turn, fire when ready
            //ail: first look for ships as targets as we can fire our beams/missiles at them and then still drop bombs afterwards
            if(currentTarget != null && currentTarget.isColony())
            {
                chooseTarget(stack, false, true);
                if (stack.canAttack(currentTarget)) 
                    performSmartAttackTarget(stack, currentTarget);
                //now chhose our previous target again
                chooseTarget(stack, false, false);
            }
            if (stack.canAttack(currentTarget)) 
                performSmartAttackTarget(stack, currentTarget);
            else
            {
                //ail: if we couldn't attack our move-to-target, we try and see if anything else can be attacked from where we are
                chooseTarget(stack, true, false);
                if (stack.canAttack(currentTarget)) 
                    performSmartAttackTarget(stack, currentTarget);
            }
            
            if(currentTarget != null)
            {
                if(stack.movePointsTo(currentTarget) + stack.maxMove > currentTarget.optimalFiringRange(stack) + currentTarget.maxMove)
                    shouldPerformKiting = true;
            }
         
            boolean enemyColonyPresent = false;
            if (stack.mgr.results().colonyStack != null && stack.mgr.results().colonyStack.colony.empire() != empire)
                enemyColonyPresent = true;
            
            //ail: only move away if I have fired at our best target and am a missile-user or have repulsors
            boolean atLeastOneWeaponCanStillFire = false;
            boolean allWeaponsCanStillFire = true;
            if(stack.isShip())
            {
                CombatStackShip shipStack = (CombatStackShip)stack;
                for (int i=0;i<stack.numWeapons(); i++) {
                    if(stack.weapon(i).groundAttacksOnly() && !enemyColonyPresent)
                        continue;
                    if(stack.weapon(i).isSpecial())
                        continue;
                    if(stack.shotsRemaining(i) < shipStack.weaponAttacks[i] || stack.weapon(i).isLimitedShotWeapon() && shipStack.roundsRemaining[i] < 1)
                    {
                        allWeaponsCanStillFire = false;
                    }
                    else
                    {
                        atLeastOneWeaponCanStillFire = true;
                    }
                }
            }
            
            //System.out.print("\n"+stack.fullName()+" shouldPerformKiting: "+shouldPerformKiting+" atLeastOneWeaponCanStillFire: "+atLeastOneWeaponCanStillFire);
            
            if (wantToRetreat(stack) && stack.canRetreat()) {
                CombatStackShip shipStack = (CombatStackShip) stack;
                StarSystem dest = retreatSystem(shipStack.mgr.system());
                if (dest != null) {
                    mgr.retreatStack(shipStack, dest);
                    //System.out.println(stack.fullName()+" retreat because it wants to after moving.");
                    return;
                }
            }
            
            if(shouldPerformKiting && !atLeastOneWeaponCanStillFire)
            {
                if (stack.mgr.autoResolve) {
                    Point destPt = findSafestPoint(stack);
                    if (destPt != null)
                        mgr.performMoveStackToPoint(stack, destPt.x, destPt.y);
                }
                else
                {
                    FlightPath bestPathToSaveSpot = findSafestPath(stack);
                    if(bestPathToSaveSpot != null)
                        mgr.performMoveStackAlongPath(stack, bestPathToSaveSpot);
                    //System.out.print("\n"+stack.fullName()+" Kiting performed: "+(bestPathToSaveSpot != null));
                }
                //turnActive = false;
            }
            // SANITY CHECK:
            // make sure we fall out if we haven't moved 
            // and we are still picking the same target
            if ((prevMove == stack.move) && (prevTarget == currentTarget)) {
                turnActive = false;
            }
            //ail: no more handling retreat from here, only kiting
            if(stack.maxMove == stack.move && allWeaponsCanStillFire && stack.isShip())
            {
                if(currentTarget == null)
                {
                    if (stack.mgr.autoResolve) {
                        Point destPt = findSafestPoint(stack);
                        if (destPt != null)
                            mgr.performMoveStackToPoint(stack, destPt.x, destPt.y);
                    }
                    else
                    {
                        FlightPath bestPathToSaveSpot = findSafestPath(stack);
                        if(bestPathToSaveSpot != null)
                            mgr.performMoveStackAlongPath(stack, bestPathToSaveSpot);
                        //System.out.print("\n"+stack.fullName()+" No target-kite performed: "+(bestPathToSaveSpot != null));
                    }
                }
            }
        }
        mgr.turnDone(stack);
    }
   
    private boolean performSmartAttackTarget(CombatStack stack, CombatStack target)
    {
        boolean performedAttack = false;
        if(target == null)
            return false;
        //1st run: fire only specials which are not repulsor or stasis-field
        for (int i=0;i<stack.numWeapons(); i++) {
            if(!stack.weapon(i).isSpecial()
                    || !((CombatStackShip)stack).shipComponentCanAttack(target, i)
                    || stack.weapon(i).tech().isType(Tech.REPULSOR)
                    || stack.weapon(i).tech().isType(Tech.STASIS_FIELD))
            {
                continue;
            }
            else
            {
                stack.fireWeapon(target, i, true);
                performedAttack = true;
            }
        }
        //2nd run: fire non-special-weapons
        for (int i=0;i<stack.numWeapons(); i++) {
            if(stack.weapon(i).isSpecial()
                    || !((CombatStackShip)stack).shipComponentCanAttack(target, i)
                    || (stack.weapon(i).isMissileWeapon() && stack.movePointsTo(target) > stack.optimalFiringRange(target)))
            {
                continue;
            }
            else
            {
                stack.fireWeapon(target, i, true);
                performedAttack = true;
            }
        }
        //3rd run: fire whatever is left, except missiles if we are too far
        for (int i=0;i<stack.numWeapons(); i++) {
            if(stack.weapon(i).isMissileWeapon() && stack.movePointsTo(target) > stack.optimalFiringRange(target))
                continue;
            if(((CombatStackShip)stack).shipComponentCanAttack(target, i))
            {
                stack.fireWeapon(target, i, true);
                performedAttack = true;
            }
        }
        return performedAttack;
    }
    private  FlightPath chooseTarget(CombatStack stack, boolean onlyInAttackRange, boolean onlyShips) {
        if (!stack.canChangeTarget())
            return null;

        List<CombatStack> potentialTargets = new ArrayList<>();
        List<CombatStack> activeStacks = new ArrayList<>(combat().activeStacks());
        
        boolean allTargetsCloaked = true;
        for (CombatStack st: activeStacks) {
            if (stack.hostileTo(st, st.mgr.system()) && !st.inStasis)
            {
                potentialTargets.add(st);
                if(!st.cloaked)
                {
                    if(!st.isColony())
                        allTargetsCloaked = false;
                    else if(st.num > 0)
                        allTargetsCloaked = false;
                }
            }
        }
        FlightPath bestPath = null;
        CombatStack bestTarget = null;
        float maxDesirability = -1;
        for (CombatStack target : potentialTargets) {
            if(onlyInAttackRange && !stack.canAttack(target))
            {
                continue;
            }
            if(onlyShips && target.isColony())
            {
                continue;
            }
            if(target.inStasis)
                continue;
            if(target.cloaked && (!allTargetsCloaked || stack.hasWard()) && stack.isShip())
                continue;
            // pct of target that this stack thinks it can kill
            float killPct = max(stack.estimatedKillPct(target), expectedPopLossPct(stack, target)); 
            //reduce attractiveness of target depending on how much damage it already has incoming from missiles
            // threat level target poses to this stack (or its ward if applicable)
            CombatStack ward = stack.hasWard() ? stack.ward() : stack;
            // want to adjust threat upward as target gets closer to ward
            int distAfterMove = target.canTeleport() ? 1 : (int) max(1,target.movePointsTo(ward)-target.maxMove());
            //ail: We run best-target twice: Once to see where to move toward by ignoring distance, so we just assume we can reach it, and once after moving so we can see what we can actually shoot
            if(!onlyInAttackRange)
                distAfterMove = 1;
            float rangeAdj = 10.0f/distAfterMove;
            if(stack.isShip())
            {
                if(target.isShip())
                {
                    boolean canStillFireShipWeapon = false;
                    for (int i=0;i<stack.numWeapons(); i++) {
                        if(!stack.weapon(i).groundAttacksOnly() && stack.shotsRemaining(i) > 0)
                        {
                            canStillFireShipWeapon = true;
                        }
                    }
                    if(!canStillFireShipWeapon)
                        killPct = 0;
                }
            }
            //System.out.print("\n"+stack.fullName()+" onlyships: "+onlyShips+" onlyInAttackRange: "+onlyInAttackRange+" looking at "+target.fullName()+" killPct: "+killPct+" rangeAdj: "+rangeAdj+" cnt: "+target.num+" target.designCost(): "+target.designCost());
            if (killPct > 0) {
                killPct = min(1,killPct);
                float adjustedKillPct = killPct - incomingMissileKillPct(target);
                float desirability = 0;
                float valueMod = 0;
                if(target.isShip())
                    valueMod = target.designCost();
                else if(target.num > 0)
                    valueMod = target.designCost();
                else if(target.isColony())
                {
                    CombatStackColony csCol = (CombatStackColony) target;
                    valueMod = csCol.colony.population() * csCol.colony.empire().tech().populationCost() + csCol.colony.industry().factories() * csCol.colony.empire().tech().baseFactoryCost();
                }
                if(adjustedKillPct > 0)
                    desirability = adjustedKillPct * max(1, target.num) * valueMod * rangeAdj;
                else
                    desirability = killPct * max(1, target.num) * valueMod * rangeAdj / 100;
                if(stack.isColony())
                    desirability *= 1 + target.estimatedKillPct(stack) * stack.designCost();
                if(!target.canPotentiallyAttack(stack))
                {
                    if(!target.isColony() || onlyShips)
                        desirability /= 100;
                }
                //System.out.print("\n"+stack.fullName()+" looking at "+target.fullName()+" desirability: "+desirability+" oir: "+onlyInAttackRange+" os: "+onlyShips+" can attack: "+stack.canAttack(target));
                if (desirability > maxDesirability) {  // this might be a better target, adjust desirability for pathing
                    if (stack.mgr.autoResolve) {
                        bestTarget = target;
                        maxDesirability = desirability;
                    }
                    else {
                        FlightPath path = findBestPathToAttack(stack, target);
                        if (path != null) {  // can we even path to this target?
                            int turnsToReachTarget = stack.canTeleport ? 1 : (int) Math.ceil(path.size() / stack.maxMove());
                            if (turnsToReachTarget > 0 && onlyInAttackRange)
                                desirability = desirability / turnsToReachTarget; // lower-value targets that can be attacked right away may be more desirable
                            if (desirability > maxDesirability) {
                                bestPath = path;
                                bestTarget = target;
                                maxDesirability = desirability;
                            }
                        }
                    }
                }
            }
        }
        currentTarget = bestTarget;
        return bestPath;
    }
    public Point findClosestPoint(CombatStack st, CombatStack tgt) {
        if (!st.canMove())
            return null;

        //ail: We will always want to go as close as possible because this increases hit-chance
        int targetDist = DistanceToBeAt(st, tgt);
        
        float maxDist = st.movePointsTo(tgt.x,tgt.y);
        if (maxDist <= targetDist)
            return null;

        int r = (int) st.move;
        if (st.canTeleport)
            r = 100;

        Point pt = new Point(st.x, st.y);

        int minMove = 0;
        for (int x1=st.x-r; x1<=st.x+r; x1++) {
            for (int y1=st.y-r; y1<=st.y+r; y1++) {
                if (combat().canMoveTo(st, x1, y1)) {
                    float dist = st.movePointsTo(tgt.x,tgt.y,x1,y1);
                    int move = st.movePointsTo(x1,y1);
                    if ((maxDist > targetDist) && (dist < maxDist)) {
                        maxDist = dist;
                        minMove = move;
                        pt.x = x1;
                        pt.y = y1;
                    }
                    else if ((dist <= targetDist)
                        && ((dist > maxDist)
                            || ((dist == maxDist) && (move < minMove)))) {
                        maxDist = dist;
                        minMove = move;
                        pt.x = x1;
                        pt.y = y1;
                    }
                }
            }
        }
        return pt;
    }
    public Point findSafestPoint(CombatStack st) {
        int bestX = st.x;
        int bestY = st.y;
        float safestScore = 0;
        for(int x = 0; x <= st.mgr.maxX; ++x)
        {
            for(int y = 0; y <= st.mgr.maxY; ++y)
            {
                float currentScore = 0;
                if(!st.mgr.validSquare(x,y))
                    continue;
                if(!st.canTeleport && !st.canMoveTo(x, y))
                    continue;
                boolean blocked = false;
                for(CombatStack other : st.mgr.activeStacks())
                {
                    if(other.x == x && other.y == y && other != st)
                    {
                        blocked = true;
                        continue;
                    }
                    if(other.canPotentiallyAttack(st))
                    {
                        currentScore += other.distanceTo(x, y);
                    }
                }
                if(blocked)
                    continue;
                if(currentScore > safestScore)
                {
                    safestScore = currentScore;
                    bestX = x;
                    bestY = y;
                }
            }
        }
        //System.out.print("\nSafest space for "+st.fullName()+" x: "+bestX+" y: "+bestY+" score: "+safestScore);
        Point pt = new Point(st.x, st.y);
        pt.x = bestX;
        pt.y = bestY;
        return pt;
    }
    public FlightPath findSafestPath(CombatStack st) {
        FlightPath bestPath = null;
        Point pt = findSafestPoint(st);
        //System.out.println("Safest space for "+st.fullName()+" x: "+pt.x+" y: "+pt.y);
        List<FlightPath> validPaths = new ArrayList<>();
        allValidPaths(st.x,st.y,pt.x,pt.y,9,st, validPaths, bestPath);
        if(validPaths.isEmpty())
            return bestPath;
        Collections.sort(validPaths,FlightPath.SORT);
        //System.out.println("Paths found: "+validPaths.size());
        return validPaths.get(0);
    }
    public FlightPath defendWardPath(CombatStack st, CombatStack tgt)
    {
        List<FlightPath> validPaths = new ArrayList<>();
        FlightPath bestPath = null;
        int bestX = st.x;
        int bestY = st.y;
        float bestScore = Float.MAX_VALUE;
        for(int x = tgt.x-1; x <= tgt.x+1; ++x)
        {
            for(int y = tgt.y-1; y <= tgt.y+1; ++y)
            {
                float currentScore = Float.MAX_VALUE;
                if(!st.mgr.validSquare(x,y))
                    continue;
                boolean blocked = false;
                for(CombatStack other : st.mgr.activeStacks())
                {
                    if(other.x == x && other.y == y && other != st)
                    {
                        blocked = true;
                        continue;
                    }
                    if(other.hostileTo(st, StarSystem.TARGET_SYSTEM))
                    {
                        currentScore = other.distanceTo(x, y);
                    }
                }
                if(blocked)
                    continue;
                if(currentScore < bestScore)
                {
                    bestScore = currentScore;
                    bestX = x;
                    bestY = y;
                }
            }
        }
        //System.out.println("Best Square for "+st.fullName()+" x: "+bestX+" y: "+bestY+" wardScore: "+bestScore);
        allValidPaths(st.x,st.y,bestX,bestY,14,st, validPaths, bestPath);
        if(validPaths.isEmpty())
            return bestPath;
        Collections.sort(validPaths,FlightPath.SORT);
        return validPaths.get(0);
    }
    public FlightPath findBestPathToAttack(CombatStack st, CombatStack tgt) {
        if (!st.isArmed())
            return null;
        //we start at r = 1 and increase up to our optimal firing-range
        FlightPath bestPath = null;
        int distanceToBeAt = DistanceToBeAt(st, tgt);
        while(bestPath == null && distanceToBeAt <= max(st.maxFiringRange(tgt),distanceToBeAt))
        {
            bestPath = findBestPathToAttack(st, tgt, distanceToBeAt);
            distanceToBeAt++;
        }
        return bestPath;
    }
    public int DistanceToBeAt(CombatStack st, CombatStack tgt)
    {
        int distanceToBeAt = st.optimalFiringRange(tgt);
        if(st.repulsorRange() > 0 && st.optimalFiringRange(tgt) > 1 && tgt.optimalFiringRange(st) < st.optimalFiringRange(tgt) && !tgt.ignoreRepulsors())
            distanceToBeAt = max(distanceToBeAt, 2);
        if(tgt.repulsorRange() > 0 && !st.ignoreRepulsors())
            distanceToBeAt = max(distanceToBeAt, 2);
        if(st.repulsorRange() > 0 && st.optimalFiringRange(tgt) == 1 && (st.move < st.movePointsTo(tgt) || (tgt.initiative() > st.initiative() && tgt.maxFiringRange(st) < 2)) && !tgt.ignoreRepulsors())
            distanceToBeAt = max(distanceToBeAt, 2);
        boolean shallGoForFirstStrike = true;
        if(galaxy().shipCombat().results().damageSustained(st.empire) > 0
                || galaxy().shipCombat().results().damageSustained(tgt.empire) > 0)
            shallGoForFirstStrike = false;
        boolean enemyCanAttackAnythingFromMe = false;
        for(CombatStack enemy : galaxy().shipCombat().activeStacks())
        {
            if(enemy.empire != empire)
                continue;
            for(CombatStack mine : galaxy().shipCombat().activeStacks())
            {
                if(mine.empire != empire)
                    continue;
                if(enemy.isArmed())
                {
                    if(enemy.maxFiringRange(mine) + enemy.maxMove <= enemy.distanceTo(mine.x(), mine.y()))
                    {
                        enemyCanAttackAnythingFromMe = true;
                        break;
                    }
                }
            }
        }
        if(!enemyCanAttackAnythingFromMe)
            shallGoForFirstStrike = true;
        if(st.maxMove <= tgt.maxMove || st.canTeleport)
            shallGoForFirstStrike = false;
        if(st.move < st.movePointsTo(tgt) - st.optimalFiringRange(tgt) && shallGoForFirstStrike)
        {
            int rangeToAssume = (int) (tgt.optimalFiringRange(st) + tgt.maxMove + 1);
            if(rangeToAssume <= st.movePointsTo(tgt))
            {
                distanceToBeAt = max(distanceToBeAt, rangeToAssume);
            }
        }
        return distanceToBeAt;
    }
    public static FlightPath findBestPathToAttack(CombatStack st, CombatStack tgt, int range) {
        if (st.movePointsTo(tgt) <= range) {
            return new FlightPath();
        }        
        int r = range;
        if (tgt.isColony() && st.hasBombs())
            r = 1;

        List<FlightPath> validPaths = new ArrayList<>();
        FlightPath bestPath = null;
        
        if (st.x > tgt.x) {
            if (st.y > tgt.y) {
                for (int x1=tgt.x+r; x1>=tgt.x-r; x1--) {
                    for (int y1=tgt.y+r; y1>=tgt.y-r; y1--) {
                        if (st.mgr.validSquare(x1,y1))
                            bestPath = allValidPaths(st.x,st.y,x1,y1,14,st, validPaths, bestPath); // get all valid paths to this point
                    }
                }
            } 
            else {
                for (int x1=tgt.x+r; x1>=tgt.x-r; x1--) {
                    for (int y1=tgt.y-r; y1<=tgt.y+r; y1++) {
                        if (st.mgr.validSquare(x1,y1))
                            bestPath = allValidPaths(st.x,st.y,x1,y1,14,st, validPaths, bestPath); // get all valid paths to this point
                    }
                }
            }
        } 
        else {
            if (st.y > tgt.y) {
                for (int x1=tgt.x-r; x1<=tgt.x+r; x1++) {
                    for (int y1=tgt.y+r; y1>=tgt.y-r; y1--) {
                        if (st.mgr.validSquare(x1,y1))
                            bestPath = allValidPaths(st.x,st.y,x1,y1,14,st, validPaths, bestPath); // get all valid paths to this point
                    }
                }
            } 
            else {
                for (int x1=tgt.x-r; x1<=tgt.x+r; x1++) {
                    for (int y1=tgt.y-r; y1<=tgt.y+r; y1++) {
                        if (st.mgr.validSquare(x1,y1))
                            bestPath = allValidPaths(st.x,st.y,x1,y1,14,st, validPaths, bestPath); // get all valid paths to this point
                    }
                }
            }
        }
            
         // there is no path to get in optimal firing range of target!
        if (validPaths.isEmpty()) {
            // ail: no longer being content when we are within max-firing-range, we'll run a loop with slowly increasing range instead
            return null;
        }  

        Collections.sort(validPaths,FlightPath.SORT);
        //System.out.println("Paths found: "+validPaths.size());
        return validPaths.get(0);
    }
    @Override
    public boolean wantToRetreat(CombatStack currStack) {
        CombatStackColony col = combat().results().colonyStack;
        EmpireView colView = (col == null) ? null : currStack.empire.viewForEmpire(col.empire);
        boolean inPact = (colView != null) && colView.embassy().pact();
            
        if (currStack == col)
            return false;
        
        // PLAYER STACKS
        // 
        // when auto-resolving, retreat player stacks ONLY when not
        // retreating would violate a pact, or when the stack is unarmed
        // armed stacks will otherwise fight to the death, per player expectations
        if (!currStack.usingAI())
        {
            boolean atLeastOneStackStillArmed = false;
            for(CombatStack cst : currStack.mgr.allStacks())
            {
                if(cst.empire == empire)
                {
                    if(cst.isArmed())
                        atLeastOneStackStillArmed = true;
                }
            }
            return inPact || !atLeastOneStackStillArmed;
        }
     
        // AI STACKS
        //System.out.print("\n"+currStack.fullName()+" canRetreat: "+currStack.canRetreat());
        if (!currStack.canRetreat()) 
            return false;
        
        if (!currStack.canMove()) 
            return false;

        // don't retreat if we still have missiles in flight
        float killPct = 0;
        float maxHit = 0;
        List<CombatStack> activeStacks = new ArrayList<>(currStack.mgr.activeStacks());
        for (CombatStack st: activeStacks) {
            for (CombatStackMissile miss: st.missiles()) {
                if (miss.owner == currStack) 
                    return false;
                if (miss.target == currStack && st.isShip())
                {
                    if(miss.maxMove > currStack.maxMove * sqrt(2) || miss.distanceTo(currStack.x(), currStack.y()) + currStack.maxMove <= miss.missile.range())
                    {
                        float hitPct;
                        hitPct = (5 + miss.attackLevel - miss.target.missileDefense()) / 10;
                        hitPct = max(.05f, hitPct);
                        hitPct = min(hitPct, 1.0f);
                        killPct += ((miss.maxDamage()-miss.target.shieldLevel())*miss.num*hitPct)/(miss.target.maxHits*miss.target.num);
                        maxHit += (miss.maxDamage() - currStack.shieldLevel()) * miss.num*hitPct;
                        //System.out.print("\n"+currStack.fullName()+" will be hit by missiles for approx "+killPct+" dmg: "+maxHit+" hp: "+currStack.hits);
                        if((killPct > 0.2f && maxHit >= currStack.hits) || (currStack.num == 1 && maxHit >= currStack.hits))
                            return true;
                    }
                }
            }
        }
        
        // if stack is pacted with colony and doesn't want war, then retreat
        // ail: Whether I want a war or not depends on whether the other faction is an enemy, not on relation!
        if ((colView != null) && !empire.enemies().contains(colView.empire()))  
            return true;
        
        // threatened to be completely disabled by warp-dissipater
        if(currStack.maxMove() <= 1 && currStack.design().combatSpeed() > currStack.maxMove())
            return true;

        // don't retreat if all enemies can only target planets
        boolean canBeTargeted = false;
        boolean canTarget = false;
        for (CombatStack st: activeStacks) {
            if (st.canPotentiallyAttack(currStack))
                canBeTargeted = true;
            if(currStack.canPotentiallyAttack(st))
                canTarget = true;
        }
        if (!canBeTargeted)
            return false;
        if(!canTarget && combat().currentStack() != null)
            return true;
        
        if (facingOverwhelmingForce(currStack)) {
            log(currStack.toString()+" retreating from overwhelming force");
            return true;
        }

        return false;
    }
    @Override
    public boolean facingOverwhelmingForce(CombatStack stack) {
        // build list of allies & enemies
        allies().clear(); enemies().clear();
        for (CombatStack st : combat().activeStacks()) {
            if (st.isMonster()) 
                enemies.add(st);
            else {
                if (stack.empire.alliedWith(id(st.empire)))
                {
                    allies().add(st);
                }
                else if (stack.empire.aggressiveWith(st.empire, combat().system()))
                    enemies().add(st);
            }
        }
        // calculate ally kills & deaths
        float allyKillTime = 0;
        float enemyKillTime = 0;
        
        float dpsOnColony = 0;
        boolean enemyHasRepulsor = false;
        boolean weCounterRepulsor = false;
        
        List<CombatStack> friends = new ArrayList<>();
        for (CombatStack ally: allies()) {
            if (ally.isArmed())
                friends.add(ally);
        }
        if (!friends.contains(stack))
            friends.add(stack);
        List<CombatStack> foes = new ArrayList<>();
        for (CombatStack enemy: enemies()) {
            if (enemy.isArmed())
                foes.add(enemy);
            if(enemy.isColony())
            {
                for(CombatStack friend : friends)
                {
                    float currentDamage = expectedPopLossPct(friend, enemy);
                    dpsOnColony += currentDamage;
                    if(currentDamage > 0 && (friend.canCloak || friend.canTeleport()))
                        weCounterRepulsor = true;
                }
            }
        }

        int foesBlockPlanet = 0;
        CombatStackColony col = combat().results().colonyStack;
        
        for (CombatStack st1 : foes) {
            if(st1.repulsorRange() > 0)
                enemyHasRepulsor = true;
            if(col != null && col.empire == st1.empire)
                if(col.movePointsTo(st1) == 1)
                    foesBlockPlanet++;
            if(st1.inStasis)
                continue;
            boolean previousCloakingState = st1.cloaked;
            st1.cloaked = false; //decloack in our mind for estimates
            float pctOfMaxHP = 0;
            pctOfMaxHP = ((st1.num-1) * st1.maxHits + st1.hits) / (st1.num * st1.maxHits);
            float damagePerTurn = 0;
            for (CombatStack st2: friends) {
                if(st2.inStasis)
                    continue;
                float killPct = min(1.0f,st2.estimatedKillPct(st1));
                if(st2.maxFiringRange(st1) <= st1.repulsorRange() && !st2.canCloak && !st2.canTeleport())
                {
                    killPct = 0;
                }
                damagePerTurn += killPct;
            }
            float healPerTurn = 0;
            if(st1.isShip())
            {
                CombatStackShip ship = (CombatStackShip)st1;
                healPerTurn = ship.designShipRepairPct() / st1.num;
            }
            damagePerTurn -= healPerTurn;
            //System.out.print("\n"+stack.mgr.system().name()+" "+st1.fullName()+" takes "+damagePerTurn+" damage per turn with heal. heal per turn: "+healPerTurn);
            if(damagePerTurn > 0)
                allyKillTime += pctOfMaxHP / min(damagePerTurn, 1.0f);
            else
            {
                allyKillTime = Float.MAX_VALUE;
                break;
            }
            st1.cloaked = previousCloakingState;
        }
        
        CombatStack invulnerableFriend = null;
        
        for (CombatStack st1 : friends) {
            if(st1.inStasis)
                continue;
            boolean previousCloakingState = st1.cloaked;
            st1.cloaked = false;
            float pctOfMaxHP = ((st1.num-1) * st1.maxHits + st1.hits) / (st1.num * st1.maxHits);
            float damagePerTurn = 0;
            for (CombatStack st2: foes) {
                if(st2.inStasis)
                    continue;
                float killPct = min(1.0f,st2.estimatedKillPct(st1));
                if(st2.maxFiringRange(st1) <= st1.repulsorRange() && !st2.canCloak && !st2.canTeleport())
                {
                    killPct = 0;
                }
                damagePerTurn += killPct;
            }
            float healPerTurn = 0;
            if(st1.isShip())
            {
                CombatStackShip ship = (CombatStackShip)st1;
                healPerTurn = ship.designShipRepairPct() / st1.num;
            }
            damagePerTurn -= healPerTurn;
            //System.out.print("\n"+stack.mgr.system().name()+" "+st1.fullName()+" takes "+damagePerTurn+" damage per turn with heal. heal per turn: "+healPerTurn);
            if(damagePerTurn > 0)
                enemyKillTime += pctOfMaxHP / min(damagePerTurn, 1.0f);
            else
            {
                if(st1.isColony())
                    invulnerableFriend = st1;
                enemyKillTime = Float.MAX_VALUE;
                break;
            }
            st1.cloaked = previousCloakingState;
        }
        //If we have an invulnerable friend, we should retreat and let him do the work. Due to the rule-change we will even stay where we are.
        if(invulnerableFriend != null && invulnerableFriend != stack)
            return true;
        
        /*if(dpsOnColony > 0)
            System.out.print("\n"+stack.mgr.system().name()+" "+stack.fullName()+" allyKillTime: "+allyKillTime+" enemyKillTime: "+enemyKillTime+" dpsOnColony: "+dpsOnColony+" col dies in: "+1 / dpsOnColony);*/
        if(dpsOnColony * enemyKillTime > 1 && (!enemyHasRepulsor || weCounterRepulsor) && foesBlockPlanet < 5)
            return false;
        
        //System.out.print("\n"+stack.mgr.system().name()+" "+stack.fullName()+" allyKillTime: "+allyKillTime+" enemyKillTime: "+enemyKillTime);
        if (enemyKillTime == allyKillTime)
            return false;
        else {
            return allyKillTime > enemyKillTime;
        }
    }
    @Override
    public StarSystem retreatSystem(StarSystem sys) {
        float speed = empire.tech().topSpeed();
        //ail: first try to use the staging-point for the system we are currently retreating from so we don't retreat to system with enemies
        int sysId = empire.optimalStagingPoint(sys, 1);
        //ail: only if that fails take overall closest system
        if(sysId == StarSystem.NULL_ID)
            sysId = empire.alliedColonyNearestToSystem(sys, speed);
        return galaxy().system(sysId);
    }
    @Override
    public FlightPath pathTo(CombatStack st, int x1, int y1) {
        List<FlightPath> validPaths = allValidPathsTo(st,x1,y1);
        if (validPaths.isEmpty())
            return null;

        Collections.sort(validPaths,FlightPath.SORT);
        return validPaths.get(0);
    }
    public List<FlightPath> allValidPathsTo(CombatStack st, int x1, int y1) {
        List<FlightPath> validPaths = new ArrayList<>();
        allValidPaths(st.x, st.y, x1, y1, (int)st.maxMove, st, validPaths, null);
        return validPaths;
    }
    public static FlightPath allValidPaths(int x0, int y0, int x1, int y1, int moves, CombatStack stack, List<FlightPath> validPaths, FlightPath bestPath) {
        FlightPath updatedBestPath = bestPath;
        ShipCombatManager mgr = stack.mgr;
        int gridW = ShipCombatManager.maxX+3;

        // all squares containing ships, asteroids, etc or non-traversable
        // can also check for enemy repulsor beam effects
        boolean[] valid = mgr.validMoveMap(stack);

        int startX = x0 + 1;
        int startY = y0 + 1;
        int endX = x1 + 1;
        int endY = y1 + 1;

        // based on general direction to travel, find most straightforward path priority
        int[] pathDeltas = bestPathDeltas(startX, startY, endX, endY);

        int start = (startY*gridW)+startX;
        int end = (endY*gridW)+endX;

        List<Integer> path = new ArrayList<>();

        loadValidPaths(start, end, valid, moves, validPaths, path, pathDeltas, gridW, updatedBestPath);
        return updatedBestPath;
    }
    private static int[] bestPathDeltas(int c0, int c1) {
        int w = FlightPath.mapW;
        return bestPathDeltas(c0%w, c0/w, c1%w, c1/w);
    }
    private static int[] bestPathDeltas(int x0, int y0, int x1, int y1) {
        if (x1 < x0) {
            if (y1 < y0)
                return FlightPath.nwPathPriority;
            else if (y1 > y0)
                return FlightPath.swPathPriority;
            else
                return FlightPath.wPathPriority;
        }
        else if (x1 > x0) {
            if (y1 < y0)
                return FlightPath.nePathPriority;
            else if (y1 > y0)
                return FlightPath.sePathPriority;
            else
                return FlightPath.ePathPriority;
        }
        else {
            if (y1 < y0)
                return FlightPath.nPathPriority;
            else
                return FlightPath.sPathPriority;
        }
    }
    private static FlightPath loadValidPaths(int curr, int end, boolean[] valid, int moves, List<FlightPath> paths, List<Integer> currPath, int[] deltas, int gridW, FlightPath bestPath) {
        FlightPath updatedBestPath = bestPath;
        if (curr == end) {
            if (currPath.size() <= pathSize(bestPath)) {
                FlightPath newPath = new FlightPath(currPath, gridW);
                paths.add(newPath);
                updatedBestPath = newPath;
            }
            return updatedBestPath;
        }
        int[] basePaths = FlightPath.basePathPriority;

        int remainingMoves = moves - 1;
        for (int dir=0;dir<deltas.length;dir++) {
            int next = curr+deltas[dir];

            if (valid[next]) {
                // are we at the end? if so create FP and fall out
                if (next == end) {
                    currPath.add(next);
                    if (currPath.size() <= pathSize(bestPath)) {
                        FlightPath newPath = new FlightPath(currPath, gridW);
                        paths.add(newPath);
                        updatedBestPath = newPath;
                    }
                }
                else if (remainingMoves > 0) {
                    int minMovesReq = moveDistance(next,end,gridW);
                    int minPossibleMoves = minMovesReq + currPath.size() + 1;
                    int bestPathSize = pathSize(updatedBestPath);
                    if ((minPossibleMoves < bestPathSize) && (minMovesReq <= remainingMoves)) {
                        int baseDir = 0;
                        for (int i=0; i<basePaths.length;i++) {
                            if (basePaths[i] == deltas[dir]) {
                                baseDir = i; 
                                break;
                            }
                        }
                        List<Integer> nextPath = new ArrayList<>(currPath);
                        nextPath.add(next);
                        boolean[] nextValid = Arrays.copyOf(valid, valid.length);
                        nextValid[curr] = false;
                        nextValid[curr + basePaths[(baseDir+1)%8]] = false;
                        nextValid[curr + basePaths[(baseDir+7)%8]] = false;
                        if (baseDir %2 == 0) {
                            nextValid[curr + basePaths[(baseDir+6)%8]] = false;
                            nextValid[curr + basePaths[(baseDir+2)%8]] = false;
                        }
                        int [] pathDeltas = bestPathDeltas(next, end);
                        updatedBestPath = loadValidPaths(next, end, nextValid, remainingMoves, paths, nextPath, pathDeltas, gridW, updatedBestPath);
                    }
                }
            }
        }
        return updatedBestPath;
    }
    private static int pathSize(FlightPath fp) {
        return fp == null ? 999 : fp.size();
    }
    private static int moveDistance(int pt0, int pt1, int w) {
        int x0 = pt0 % w;
        int y0 = pt0 / w;
        int x1 = pt1 % w;
        int y1 = pt1 / w;
        return Math.max(Math.abs(x0-x1), Math.abs(y0-y1));
    }
    public float expectedBombardDamage(CombatStackShip ship, CombatStackColony colony) {
        int num = ship.num;
        float damage = 0.0f;

        ShipDesign d = ship.design();
        for (int j=0;j<ShipDesign.maxWeapons();j++)
        {
            float dmg = num * d.wpnCount(j) * d.weapon(j).estimatedBombardDamage(d, colony);
            if(d.weapon(j).bombardAttacks() > 0)
                dmg /= d.weapon(j).bombardAttacks();
            damage += dmg;
        }
        for (int j=0;j<ShipDesign.maxSpecials();j++)
            damage += d.special(j).estimatedBombardDamage(d, colony);
        return damage;
    }
    public float expectedBioweaponDamage(CombatStackShip ship, CombatStackColony colony) {
        int num = ship.num;
        float popLoss = 0.0f;

        ShipDesign d = ship.design();
        for (int j=0;j<ShipDesign.maxWeapons();j++)
            popLoss += (num * d.wpnCount(j) * d.weapon(j).estimatedBioweaponDamage(ship, colony));
        return popLoss;
    }
    public float expectedPopulationLoss(CombatStackShip ship, CombatStackColony colony) {
        float popLost = 0;
        float bombDamage = expectedBombardDamage(ship, colony);
        if (colony.num == 0)
            popLost = bombDamage / 200;
        else
            popLost = bombDamage / 400;
        
        return popLost;
    }
    public float expectedPopLossPct(CombatStack source, CombatStack target) {
        if (!source.isShip())
            return 0;
        if (!target.isColony())
            return 0;
        
        CombatStackShip ship = (CombatStackShip) source;
        CombatStackColony colony = (CombatStackColony) target;
        
        if (colony.destroyed())
            return 0;
        
        float popLoss = expectedPopulationLoss(ship, colony);
        return popLoss/colony.colony.population();
    }
    public boolean shouldDodgeMissile(CombatStackShip currStack)
    {
        boolean retVal = false;
        List<CombatStack> activeStacks = new ArrayList<>(currStack.mgr.activeStacks());
        for (CombatStack st: activeStacks) {
            for (CombatStackMissile miss: st.missiles()) {
                if (miss.target == currStack && st.isShip())
                {
                    if(miss.maxMove <= currStack.maxMove || miss.distanceTo(currStack.x(), currStack.y()) + currStack.maxMove > miss.missile.range())
                    {
                        float hitPct;
                        hitPct = (5 + miss.attackLevel - miss.target.missileDefense()) / 10;
                        hitPct = max(.05f, hitPct);
                        hitPct = min(hitPct, 1.0f);
                        float killPct = ((miss.maxDamage()-miss.target.shieldLevel())*miss.num*hitPct)/(miss.target.maxHits*miss.target.num);
                        float maxHit = (miss.maxDamage() - currStack.shieldLevel()) * miss.num;
                        //System.out.print("\n"+currStack.fullName()+" will be hit by missiles for approx "+killPct);
                        if(killPct > 0.05f && maxHit >= currStack.hits)
                            retVal = true;
                    }
                }
            }
        }
        return retVal;
    }
    public float incomingMissileKillPct(CombatStack currStack)
    {
        float retVal = 0;
        List<CombatStack> activeStacks = new ArrayList<>(currStack.mgr.activeStacks());
        for (CombatStack st: activeStacks)
            for (CombatStackMissile miss: st.missiles())
                if (miss.target == currStack)
                {
                    float hitPct;
                    hitPct = (5 + miss.attackLevel - miss.target.missileDefense()) / 10;
                    hitPct = max(.05f, hitPct);
                    hitPct = min(hitPct, 1.0f);
                    float killPct = ((miss.maxDamage()-miss.target.shieldLevel())*miss.num*hitPct)/(miss.target.maxHits*miss.target.num);
                    //System.out.print("\n"+currStack.fullName()+" will be hit by missiles for approx "+killPct);
                    retVal += killPct;
                }
        return retVal;
    }
    @Override
    public boolean useSmartRangeForBeams()
    {
        return true;
    }
}