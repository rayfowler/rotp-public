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

import rotp.model.empires.Empire;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.StarSystem;
import rotp.model.ships.ShipDesign;
import rotp.model.ships.ShipDesignLab;
import rotp.model.ships.ShipSpecialColony;

public interface ShipDesigner {
    public Empire empire();
    default ShipDesignLab lab()   { return empire().shipLab(); }
    
    void nextTurn();
    
    int optimalShipDestroyerSize();
    int optimalShipFighterSize();
    
    ShipDesign newScoutDesign();
    ShipDesign newFighterDesign(int size);
    ShipDesign newBomberDesign(int size);
    ShipDesign newDestroyerDesign(int size);
    ShipDesign bestDesignToColonize(ShipFleet fl, StarSystem sys);
    
    ShipSpecialColony bestColonySpecial();
}