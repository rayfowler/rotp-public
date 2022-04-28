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
package rotp.model.ai;

import java.util.ArrayList;
import java.util.List;
import rotp.model.ai.base.AIShipCaptain;
import rotp.model.ai.interfaces.ShipCaptain;
import rotp.model.combat.CombatStack;
import rotp.model.combat.CombatStackColony;
import rotp.model.combat.CombatStackSpaceAmoeba;
import rotp.model.combat.FlightPath;
import rotp.model.combat.ShipCombatManager;
import rotp.model.events.RandomEventSpaceAmoeba;
import rotp.model.galaxy.StarSystem;
import rotp.util.Base;

public class AmoebaShipCaptain implements Base, ShipCaptain {
    @Override
    public StarSystem retreatSystem(StarSystem fr) { return null; }
    @Override
    public boolean wantToRetreat(CombatStack stack) { return false; }
    public boolean facingOverwhelmingForce(CombatStack stack) { return false; }
    @Override
    public void performTurn(CombatStack stack)  {
        ShipCombatManager mgr = galaxy().shipCombat();
        if (stack.destroyed()) {
            mgr.turnDone(stack);
            return;
        }

        if (stack.inStasis) {
            mgr.turnDone(stack);
            return;
        }

        CombatStack prevTarget = null;
        while (stack.move > 0) {
            FlightPath bestPathToTarget = chooseTarget(stack);
            // if we need to move towards target, do it now
            if ((bestPathToTarget == null) || (bestPathToTarget.size() == 0)) 
                break;
        
            float prevMove = stack.move;
            mgr.performMoveStackAlongPath(stack, bestPathToTarget);

            // if can attack target this turn, fire when ready
            if (stack.canAttack(stack.target)) 
                mgr.performAttackTarget(stack);
            
            // SANITY CHECK:
            // make sure we fall out if we haven't moved 
            // or if we are still picking the same target
            if (prevMove == stack.move)
                stack.move = 0;
            if (prevTarget == stack.target)
                stack.move = 0;
        }
        stack.mgr.turnDone(stack);
    }
    @Override
    public FlightPath pathTo(CombatStack st, int x, int y) { return null; }

    public void splitAmoeba(CombatStackSpaceAmoeba st) {
        float newScale = st.scale == 1.5f ? 1.0f : st.scale*2/3;

        CombatStackSpaceAmoeba newStack = new CombatStackSpaceAmoeba();
        newStack.maxHits = st.maxHits;
        newStack.hits = st.maxHits;
        newStack.x = st.x;
        newStack.y = st.y;        
        
        st.scale = newScale;
        newStack.scale = newScale;
        
        // add to the event so this new stack can carry over to potential
        // combats with other fleets later in this turn
        RandomEventSpaceAmoeba.monster.addCombatStack(newStack);   
        st.mgr.addStackToCombat(newStack);
        CombatStack eatenStack = st.mgr.moveStackNearest(newStack, st.x, st.y);
        newStack.eatShips(eatenStack);
    }
    private  FlightPath chooseTarget(CombatStack stack) {
        ShipCombatManager mgr = galaxy().shipCombat();
        CombatStackColony colony = stack.mgr.results().colonyStack;

        List<CombatStack> activeStacks = new ArrayList<>(mgr.activeStacks());
        List<CombatStack> potentialTargets = new ArrayList<>();
        for (CombatStack st: activeStacks) {
            if (st.isShip() || st.isColony())
                potentialTargets.add(st);
        }
        if ((colony != null) && !colony.isArmed())
            potentialTargets.remove(colony);

        FlightPath bestPath = null;
        CombatStack bestTarget = null;
        int bestTurns = 9999;

        // can we eat any stacks? (range 0 weapon)
        for (CombatStack target : potentialTargets) {
            FlightPath path = AIShipCaptain.findBestPathToAttack(stack, target, 0);
            if (path != null) {  // can we even path to this target?
                if (bestPath == null) {
                    bestPath = path;
                    bestTarget = target;
                    bestTurns = (int) Math.ceil(path.size() / stack.maxMove());
                }
                else {
                    int turns = (int) Math.ceil(path.size() / stack.maxMove());
                    if (turns < bestTurns) {
                        bestPath = path;
                        bestTarget = target;
                        bestTurns = turns;
                    }
                    else if ((turns == bestTurns) && (target.totalHits() > bestTarget.totalHits())) {
                        bestPath = path;
                        bestTarget = target;
                        bestTurns = turns;
                    }
                }
            }
        }
        stack.target = bestTarget;
        return bestPath;
    }
}
