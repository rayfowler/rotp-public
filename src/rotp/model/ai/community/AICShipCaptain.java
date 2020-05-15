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
package rotp.model.ai.community;

import java.awt.*;
import java.util.*;
import java.util.List;
import rotp.model.ai.interfaces.ShipCaptain;
import rotp.model.combat.*;
import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
import rotp.model.galaxy.StarSystem;
import rotp.model.ships.ShipDesign;
import rotp.util.Base;

public class AICShipCaptain implements Base, ShipCaptain {
    private final Empire empire;
    private transient List<CombatStack> allies = new ArrayList<>();
    private transient List<CombatStack> enemies = new ArrayList<>();

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
    public AICShipCaptain (Empire c) {
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

        // check for retreating
        if (wantToRetreat(stack)) {
            CombatStackShip shipStack = (CombatStackShip) stack;
            StarSystem dest = retreatSystem(shipStack.mgr.system());
            if (dest != null) {
                mgr.retreatStack(shipStack, dest);
                return;
            }
        }
        
        CombatStack prevTarget = null;
        
        boolean turnActive = true;
        while (turnActive) {
            float prevMove = stack.move;
            prevTarget = stack.target;
            FlightPath bestPathToTarget = chooseTarget(stack);
            // if we need to move towards target, do it now
            if (stack.target != null) {
                if (stack.mgr.autoResolve) {
                    Point destPt = findClosestPoint(stack, stack.target);
                    if (destPt != null)
                        mgr.performMoveStackToPoint(stack, destPt.x, destPt.y);
                }
                else if ((bestPathToTarget != null) && (bestPathToTarget.size() > 0)) {
                    mgr.performMoveStackAlongPath(stack, bestPathToTarget);
                }
            }

            // if can attack target this turn, fire when ready
            if (stack.canAttack(stack.target)) 
                mgr.performAttackTarget(stack);
         
            // SANITY CHECK:
            // make sure we fall out if we haven't moved 
            // and we are still picking the same target
            if ((prevMove == stack.move) && (prevTarget == stack.target)) {
                turnActive = false;
            }
        }
        mgr.turnDone(stack);
    }
    private  FlightPath chooseTarget(CombatStack stack) {
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
            // pct of target that this stack thinks it can kill
            float killPct = max(stack.estimatedKillPct(target), expectedPopLossPct(stack, target)); 
            // threat level target poses to this stack (or its ward if applicable)
            CombatStack ward = stack.hasWard() ? stack.ward() : stack;
            // want to adjust threat upward as target gets closer to ward
            int distAfterMove = target.canTeleport() ? 1 : (int) max(1,target.movePointsTo(ward)-target.maxMove());
            // treat those who can move to bombing range (distAfterMove == 1) as maximum threats
            if (ward.isColony()) {
                CombatStackColony colony = (CombatStackColony) ward;
                float popLossPct  =  expectedPopLossPct(target, colony); 
                float baseLossPct = target.estimatedKillPct(colony);
                float maxLossPct = max(popLossPct,baseLossPct);
                // if this is the first potential target that can reach and damage the colony, 
                // ignore any previous selected targets
                if ((!currentCanBomb) && (distAfterMove <= 1) && (maxLossPct > 0.05f)) {
                    threatLevel = maxLossPct;                     
                    currentCanBomb = true;
                    bestTarget = null;
                    maxDesirability = -1;
                }
                // if we have a target that can actually bomb us, ignore any future
                // targets that cannot yet
                else if (currentCanBomb && (distAfterMove > 1)) {
                    threatLevel = 0;
                }
                // this and no previous targets can yet bomb our colony, so evaluate
                // based on threat and distance
                else {
                    float rangeAdj = 10.0f/distAfterMove;
                    threatLevel = rangeAdj * maxLossPct;                     
                } 
            }
            else {
                float rangeAdj = 10.0f/distAfterMove;
                threatLevel = rangeAdj * target.estimatedKillPct(ward);  
            }
            if (killPct > 0) {
                killPct = min(1,killPct);
                float desirability = max((10000* threatLevel * threatLevel * killPct), .01f);
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
        stack.target = bestTarget;
        return bestPath;
    }
    public Point findClosestPoint(CombatStack st, CombatStack tgt) {
        if (!st.canMove())
            return null;

        int targetDist = st.optimalFiringRange(tgt);
        if (tgt.isColony() && st.hasBombs())
            targetDist = 1;

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
        return null;
    }
    public static FlightPath findBestPathToAttack(CombatStack st, CombatStack tgt) {
        if (!st.isArmed())
            return null;
        int r = st.optimalFiringRange(tgt);
        return findBestPathToAttack(st, tgt, r);
    }
    public static FlightPath findBestPathToAttack(CombatStack st, CombatStack tgt, int range) {
        int r = range;
        if (tgt.isColony() && st.hasBombs())
            r = 1;

        List<FlightPath> validPaths = new ArrayList<>();
        FlightPath bestPath = null;
        for (int x1=tgt.x+r; x1>=tgt.x-r; x1--) {
            for (int y1=tgt.y-r; y1<=tgt.y+r; y1++) {
                if (st.mgr.validSquare(x1,y1))
                    bestPath = allValidPaths(st.x,st.y,x1,y1,14,st, validPaths, bestPath); // get all valid paths to this point
            }
        }
        if (validPaths.isEmpty())  // there is no path to get in firing range of target!
            return null;

        Collections.sort(validPaths,FlightPath.SORT);
        //System.out.println("Paths found: "+validPaths.size());
        return validPaths.get(0);
    }
    @Override
    public boolean wantToRetreat(CombatStack currStack) {
        // armed stacks controlled by players will never retreat
        // when the player is auto-resolving
        if (!currStack.usingAI() && currStack.isArmed())
            return false;

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
        if (combat().results().colonyStack != null) {
            EmpireView cv = currStack.empire.viewForEmpire(combat().results().colonyStack.empire);
            if ((cv != null) && cv.embassy().pact() && !cv.embassy().wantWar())  
                return true;
        }

        // if stack has ward still in combat, don't retreat
        if (currStack.hasWard() && currStack.isArmed()) {
            if (activeStacks.contains(currStack.ward())) 
                return false;
        }

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
                    allies().add(st);
                else if (stack.empire.aggressiveWith(st.empire, combat().system()))
                    enemies().add(st);
            }
        }

        // calculate ally kills & deaths
        float allyKills = 0;
        float enemyKills = 0;

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
            for (CombatStack st2: foes) {
                float killPct = min(100,st1.estimatedKillPct(st2));
                float killValue = killPct*st2.num*st2.designCost();
//                log(st1.name()+"="+killPct+"    "+st2.name());
                if (killValue > maxKillValue)
                    maxKillValue = killValue;
            }
            allyKills += maxKillValue;
        }
       for (CombatStack st1 : foes) {
            float maxKillValue = -1;
            for (CombatStack st2: friends) {
                float killPct = min(100,st1.estimatedKillPct(st2));
                float killValue = killPct*st2.num*st2.designCost();
//                log(st1.name()+"="+killPct+"    "+st2.name());
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
            float retreatRatio = stack.empire.leader().retreatRatio(combat().system().empire());
            log("retreat ratio: "+retreatRatio+"   enemyKillValue:"+enemyKills+"  allyKillValue:"+allyKills);
            return (enemyKills / allyKills) > retreatRatio;
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