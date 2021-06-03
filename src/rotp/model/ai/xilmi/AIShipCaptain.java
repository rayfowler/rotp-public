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
    private CombatStack closeTarget = null;
    private CombatStack distantTarget = null;

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
            chooseTarget(stack, true, true);
            chooseTarget(stack, false, false);
            //ail: if our target to move to is not the same as the target we can currently shoot at, we shoot before moving
            
            if (stack.isColony() && stack.canAttack(currentTarget)) 
            {
                stack.target = currentTarget;
                mgr.performAttackTarget(stack);
                mgr.turnDone(stack);
            }
            
            if(closeTarget != null && closeTarget != distantTarget)
            {
                if (stack.canAttack(closeTarget)) 
                    performSmartAttackTarget(stack, closeTarget);
            }
            // check for retreating
            if (wantToRetreat(stack) && stack.canRetreat()) {
                //attack before retreating, if we can
                if(currentTarget != null && stack.canAttack(currentTarget))
                    performSmartAttackTarget(stack, closeTarget);
                CombatStackShip shipStack = (CombatStackShip) stack;
                StarSystem dest = retreatSystem(shipStack.mgr.system());
                if (dest != null) {
                    mgr.retreatStack(shipStack, dest);
                    return;
                }
            }
            //When we are defending and can't get into attack-range of the enemy, we let them come to us
            /*if(currentTarget != null)
                System.out.println(stack.fullName()+" target: "+currentTarget.fullName()+" distaftermove: "+(stack.movePointsTo(currentTarget) - stack.move)+" maxFR: "+stack.maxFiringRange(currentTarget));*/
            FlightPath bestPathToTarget = null;
            //ail: defend-stuff is problematic as stacks can be drawn out
            /*if((currentTarget == null || stack.movePointsTo(currentTarget) - stack.move > stack.maxFiringRange(currentTarget)) && stack.hasWard())
                bestPathToTarget = defendWardPath(stack, stack.ward());
            else*/
            bestPathToTarget = chooseTarget(stack, false, false);
            // if we need to move towards target, do it now
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
                    //mgr.performAttackTarget(stack);
                //now chhose our previous target again
                chooseTarget(stack, false, false);
            }
            if (stack.canAttack(currentTarget)) 
                performSmartAttackTarget(stack, currentTarget);
                //mgr.performAttackTarget(stack);
            else
            {
                //ail: if we couldn't attack our move-to-target, we try and see if anything else can be attacked from where we are
                chooseTarget(stack, true, false);
                if (stack.canAttack(currentTarget)) 
                    performSmartAttackTarget(stack, currentTarget);
                    //mgr.performAttackTarget(stack);
            }
         
            //ail: only move away if I have fired at our best target and am a missile-user or have repulsors
            boolean atLeastOneWeaponCanStillFire = false;
            boolean allWeaponsCanStillFire = true;
            boolean shouldPerformKiting = false;
            if(stack.isShip())
            {
                CombatStackShip shipStack = (CombatStackShip)stack;
                for (int i=0;i<stack.numWeapons(); i++) {
                    if(currentTarget != null && !currentTarget.isColony() && stack.weapon(i).groundAttacksOnly())
                        continue;
                    if(stack.weapon(i).isSpecial())
                        continue;
                    if(stack.weapon(i).isMissileWeapon())
                        shouldPerformKiting = true;
                    if(stack.shotsRemaining(i) < shipStack.weaponAttacks[i])
                    {
                        allWeaponsCanStillFire = false;
                    }
                    else
                    {
                        atLeastOneWeaponCanStillFire = true;
                    }
                }
            }
            if(stack.repulsorRange() > 0)
                shouldPerformKiting = true;
            
            if(shouldPerformKiting && !atLeastOneWeaponCanStillFire && (distantTarget == currentTarget || distantTarget == null))
            {
                FlightPath bestPathToSaveSpot = findSafestSpace(stack);
                if(bestPathToSaveSpot != null)
                    mgr.performMoveStackAlongPath(stack, bestPathToSaveSpot);
                turnActive = false;
            }
            // SANITY CHECK:
            // make sure we fall out if we haven't moved 
            // and we are still picking the same target
            if ((prevMove == stack.move) && (prevTarget == currentTarget)) {
                turnActive = false;
            }
            //ail: we have not moved and not fired... so we probably can't get to our target, then retreat
            if(stack.maxMove == stack.move && allWeaponsCanStillFire && stack.isShip())
            {
                if(currentTarget == null)
                {
                    FlightPath bestPathToSaveSpot = findSafestSpace(stack);
                    if(bestPathToSaveSpot != null)
                        mgr.performMoveStackAlongPath(stack, bestPathToSaveSpot);
                    turnActive = false;
                }
                else
                {
                    CombatStackShip shipStack = (CombatStackShip) stack;
                    StarSystem dest = retreatSystem(shipStack.mgr.system());
                    if (dest != null) {
                        mgr.retreatStack(shipStack, dest);
                        return;
                    }
                }
            }
        }
        mgr.turnDone(stack);
    }
   
    private void performSmartAttackTarget(CombatStack stack, CombatStack target)
    {
        if(target == null)
            return;
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
                stack.fireWeapon(target, i);
            }
        }
        //2nd run: fire non-special-weapons
        for (int i=0;i<stack.numWeapons(); i++) {
            if(stack.selectedWeapon().isSpecial()
                    || !((CombatStackShip)stack).shipComponentCanAttack(target, i)
                    || (stack.weapon(i).isMissileWeapon() && stack.movePointsTo(target) > 2))
            {
                continue;
            }
            else
            {
                stack.fireWeapon(target, i);
            }
        }
        //3rd run: fire whatever is left, except missiles if we are too far
        for (int i=0;i<stack.numWeapons(); i++) {
            if(stack.weapon(i).isMissileWeapon() && stack.movePointsTo(target) > 2)
                continue;
            if(((CombatStackShip)stack).shipComponentCanAttack(target, i))
            {
                stack.fireWeapon(target, i);
            }
        }
    }
    private  FlightPath chooseTarget(CombatStack stack, boolean onlyInAttackRange, boolean onlyShips) {
        if (!stack.canChangeTarget())
            return null;

        List<CombatStack> potentialTargets = new ArrayList<>();
        List<CombatStack> activeStacks = new ArrayList<>(combat().activeStacks());

        for (CombatStack st: activeStacks) {
            if (stack.hostileTo(st, st.mgr.system()) && !st.inStasis)
                potentialTargets.add(st);
        }
        FlightPath bestPath = null;
        CombatStack bestTarget = null;
        float maxDesirability = -1;
        float threatLevel = 0;
        boolean currentCanBomb = false;
        for (CombatStack target : potentialTargets) {
            if(onlyInAttackRange && !stack.canAttack(target))
            {
                continue;
            }
            if(onlyShips && target.isColony())
            {
                continue;
            }
            // pct of target that this stack thinks it can kill
            float killPct = max(stack.estimatedKillPct(target), expectedPopLossPct(stack, target)); 
            // threat level target poses to this stack (or its ward if applicable)
            CombatStack ward = stack.hasWard() ? stack.ward() : stack;
            // want to adjust threat upward as target gets closer to ward
            int distAfterMove = target.canTeleport() ? 1 : (int) max(1,target.movePointsTo(ward)-target.maxMove());
            //ail: We run best-target twice: Once to see where to move toward by ignoring distance, so we just assume we can reach it, and once after moving so we can see what we can actually shoot
            if(!onlyInAttackRange)
                distAfterMove = 1;
            float rangeAdj = 10.0f/distAfterMove;
            //System.out.print("\n"+stack.fullName()+" onlyships: "+onlyShips+" onlyInAttackRange: "+onlyInAttackRange+" looking at "+target.fullName()+" killPct: "+killPct+" rangeAdj: "+rangeAdj+" cnt: "+target.num+" target.designCost(): "+target.designCost());
            if (killPct > 0) {
                killPct = min(1,killPct);
                float desirability = killPct * max(1, target.num) * target.designCost() * rangeAdj;
                //System.out.print("\n"+stack.fullName()+" looking at "+target.fullName()+" desirability: "+desirability);
                if (desirability > maxDesirability) {  // this might be a better target, adjust desirability for pathing
                    if (stack.mgr.autoResolve) {
                        bestTarget = target;
                        maxDesirability = desirability;
                    }
                    else if (stack.isColony()) {
                        bestTarget = target;
                        maxDesirability = desirability;
                    }
                    else {
                        FlightPath path = findBestPathToAttack(stack, target);
                        if (path != null) {  // can we even path to this target?
                            int turnsToReachTarget = stack.canTeleport ? 1 : (int) Math.ceil(path.size() / stack.maxMove());
                            if (turnsToReachTarget > 0)
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
        if(onlyInAttackRange)
            closeTarget = bestTarget;
        else
            distantTarget = bestTarget;
        return bestPath;
    }
    public Point findClosestPoint(CombatStack st, CombatStack tgt) {
        if (!st.canMove())
            return null;

        //ail: We will always want to go as close as possible because this increases hit-chance
        int targetDist = 1;
        if ((targetDist <= tgt.repulsorRange())
        && !st.ignoreRepulsors())
            targetDist = tgt.repulsorRange() + 1;
        
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
    public FlightPath findSafestSpace(CombatStack st) {
        FlightPath bestPath = null;
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
                    if(other.hostileTo(st, StarSystem.TARGET_SYSTEM))
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
        //System.out.println("Safest space for "+st.fullName()+" x: "+bestX+" y: "+bestY+" safetyScore: "+safestScore);
        List<FlightPath> validPaths = new ArrayList<>();
        allValidPaths(st.x,st.y,bestX,bestY,(int)st.move,st, validPaths, bestPath);
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
        int r = 1;
        FlightPath bestPath = null;
        while(bestPath == null && r <= st.optimalFiringRange(tgt))
        {
            bestPath = findBestPathToAttack(st, tgt, r);
            r++;
        }
        return bestPath;
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
            return inPact || !currStack.isArmed();
     
        // AI STACKS
        //System.out.print("\n"+currStack.fullName()+" canRetreat: "+currStack.canRetreat());
        if (!currStack.canRetreat()) 
            return false;
        
        if (!currStack.canMove()) 
            return false;

        // don't retreat if we still have missiles in flight
        List<CombatStack> activeStacks = new ArrayList<>(currStack.mgr.activeStacks());
        for (CombatStack st: activeStacks) {
            for (CombatStackMissile miss: st.missiles()) {
                if (miss.owner == currStack) 
                    return false;
            }
        }
        
        // if stack is pacted with colony and doesn't want war, then retreat
        // ail: Whether I want a war or not depends on whether the other faction is an enemy, not on relation!
        if ((colView != null) && !empire.enemies().contains(col.empire))  
            return true;

        // don't retreat if all enemies can only target planets
        boolean canBeTargeted = false;
        for (CombatStack st: activeStacks) {
            if (st.canPotentiallyAttack(currStack))
                canBeTargeted = true;
        }
        if (!canBeTargeted)
            return false;
        
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
        float allyKills = 0;
        float enemyKills = 0;
        
        float allyValue = 0;
        float enemyValue = 0;
        
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
        }

//        log("friends:"+friends.size()+"   foes:"+foes.size());
        for (CombatStack st1 : friends) {
            float maxKillValue = -1;
            float pctOfMaxHP = ((st1.num-1) * st1.maxHits + st1.hits) / (st1.num * st1.maxHits);
            allyValue += st1.num * pctOfMaxHP * st1.designCost();
            for (CombatStack st2: foes) {
                float killPct = min(1.0f,st1.estimatedKillPct(st2)); // modnar: killPct should have max of 1.00 instead of 100?
                //ail: If the enemy has brought a colonizer, we split our kill because otherwise each of our stacks thinks they can kill all the colonizers despite it's already dead
                if(st2.isShip() && st2.design().isColonyShip())
                    killPct /= friends.size();
                //ail: 0 damage possible when they have repulsor and we can't outrange
                if(st1.optimalFiringRange(st2) <= st2.repulsorRange() && !st1.canCloak && !st1.canTeleport())
                {
                    //System.out.print("\n"+stack.fullName()+" seeing uncountered repulsor.");
                    killPct = 0;
                }
                float killValue = killPct*st2.num*st2.designCost();
                //System.out.print("\n"+stack.fullName()+" "+st1.fullName()+" thinks it can kill "+killPct+" val: "+killValue+" of "+st2.fullName()+" it has: "+pctOfMaxHP);
//                log(st1.name()+"="+killPct+"    "+st2.name());
                if (killValue > maxKillValue)
                    maxKillValue = killValue;
            }
            allyKills += maxKillValue;
        }
       for (CombatStack st1 : foes) {
            float maxKillValue = -1;
            float pctOfMaxHP = ((st1.num-1) * st1.maxHits + st1.hits) / (st1.num * st1.maxHits);
            enemyValue += st1.num * pctOfMaxHP * st1.designCost();
            for (CombatStack st2: friends) {
                //ail: When we have brought colonizers to a battle and are not the colonizer ourselves, we ignore their lack of combat-power for our own retreat-decision. They can still retreat when they are too scared!
                if(stack != st2 && st2.isShip() && st2.design().isColonyShip())
                    continue;
                float killPct = min(1.0f,st1.estimatedKillPct(st2)); // modnar: killPct should have max of 1.00 instead of 100?
                if(st1.optimalFiringRange(st2) <= st2.repulsorRange() && !st1.canCloak && !st1.canTeleport())
                {
                    //System.out.print("\n"+stack.fullName()+" seeing uncountered repulsor.");
                    killPct = 0;
                }
                float killValue = killPct*st2.num*st2.designCost();
//                log(st1.name()+"="+killPct+"    "+st2.name());
                //System.out.print("\n"+stack.fullName()+" "+st1.fullName()+" thinks it can kill "+killPct+" val: "+killValue+" of "+st2.fullName()+" it has: "+pctOfMaxHP);
                if (killValue > maxKillValue)
                    maxKillValue = killValue;
            }
            enemyKills += maxKillValue;
        }
        if (enemyKills == 0)
            return false;
        else if (allyKills == 0)
            return true;
        else {
            //System.out.print("\n"+stack.fullName()+" enemy-superiority: "+(enemyKills * enemyValue) / (allyKills * allyValue)+" kills (Enemy vs. mine): "+enemyKills / allyKills+" Cost: (enemy vs. mine): "+enemyValue/allyValue);
            return (enemyKills * enemyValue) / (allyKills * allyValue) > 1.0f;
        }
    }
    @Override
    public StarSystem retreatSystem(StarSystem sys) {
        float speed = empire.tech().topSpeed();
        int sysId = empire.alliedColonyNearestToSystem(sys, speed);
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
            damage += (num * d.wpnCount(j) * d.weapon(j).estimatedBombardDamage(d, colony));
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
        
        float bioDamage = expectedBioweaponDamage(ship, colony);

        return popLost+bioDamage;
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
}