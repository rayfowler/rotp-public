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
package rotp.model.ai.interfaces;

import rotp.model.combat.CombatStack;
import rotp.model.galaxy.StarSystem;
import rotp.model.combat.FlightPath;

public interface ShipCaptain {
    void performTurn(CombatStack stack);
    StarSystem retreatSystem(StarSystem fr);
    boolean wantToRetreat(CombatStack stack);
    FlightPath pathTo(CombatStack st, int x, int y);
    
    //differentiation between behavior in Xilmi- and Base-AI
    default boolean useSmartRangeForBeams() { return false; }
}
