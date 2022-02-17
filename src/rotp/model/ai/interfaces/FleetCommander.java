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

import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.StarSystem;

public interface FleetCommander {
    boolean inExpansionMode();
    void nextTurn();
    float transportPriority(StarSystem sys);
    float maxShipMaintainance();
    
    //Xilmi
    default float bcValue(ShipFleet fl, boolean countScouts, boolean countFighters, boolean countBombers, boolean countColonizers) { return 0; }
    default float bridgeHeadConfidence(StarSystem sys) { return 1; }
}
