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

import rotp.model.ai.ShipPlan;
import rotp.model.colony.Colony;
import rotp.model.galaxy.StarSystem;

public interface Governor {
    void setInitialAllocations(Colony c);
    void setColonyAllocations(Colony c);
    void lowerExpenses(Colony c);
    float maxShipBCPermitted(Colony c);
    boolean readyToBuild(Colony c, ShipPlan sh, int designCost);
    int suggestedEmpireTaxLevel();
    float targetPopPct(int sysId);
    
    // specific to Xilmi AI
    default float productionScore(StarSystem sys) { return 0; }
}
