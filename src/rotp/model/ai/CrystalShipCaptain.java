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

import java.awt.Point;
import rotp.model.ai.interfaces.ShipCaptain;
import rotp.model.combat.CombatStack;
import rotp.model.combat.FlightPath;
import rotp.model.combat.ShipCombatManager;
import rotp.model.galaxy.StarSystem;
import rotp.util.Base;

public class CrystalShipCaptain implements Base, ShipCaptain {
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
        
        Point pt = bestAttackSpot(stack);
        if ((pt.x != stack.x) || (pt.y != stack.y))
            stack.teleportTo(pt.x, pt.y, 0.5f);
        
        stack.fireWeapon(null);
        stack.mgr.turnDone(stack);
    }
    @Override
    public FlightPath pathTo(CombatStack st, int x, int y) { return null; }
    private Point bestAttackSpot(CombatStack st) {
        ShipCombatManager mgr = st.mgr;
        
        int maxX = ShipCombatManager.maxX;
        int maxY = ShipCombatManager.maxY;
        int validGrid[][] = new int[maxX+1][maxY+1];
        
        // add up stack hp values in all of their adjacent squares
        for (CombatStack stack: mgr.activeStacks()) {
            if (stack == st)
                continue;
            int x0 = max(0,stack.x-1);
            int y0 = max(0,stack.y-1);
            int x1 = min(maxX, stack.x+1);
            int y1 = min(maxY, stack.y+1);
            float maxHp = stack.num * stack.maxHits;
            for (int x=x0;x<=x1;x++) {
                for (int y=y0;y<=y1;y++)
                    validGrid[x][y] += maxHp;
            }
        }
        
        // now set to -1 all squares that have a stack
        // we can't teleport to those
        for (CombatStack stack: mgr.activeStacks()) 
            validGrid[stack.x][stack.y] = -1;
        
        // now set to -1 all squares that have asteroids
        for (int x=0;x<=maxX;x++) {
            for (int y=0;y<=maxY;y++) {
                if (!mgr.validSquare(x,y))
                    validGrid[x][y] = -1;
            }
        }
        
        // now find the square with the highest value
        // that's where we want to teleport to
        Point pt = new Point(0,0);
        int maxHp = 0;
        for (int x=0;x<=maxX;x++) {
            for (int y=0;y<=maxY;y++) {
                if (validGrid[x][y] > maxHp) {
                    maxHp = validGrid[x][y];
                    pt.x = x;
                    pt.y = y;
                }
            }
        }        
        return pt;
    }
}
