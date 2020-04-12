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

import rotp.model.tech.Tech;
import rotp.model.tech.TechTree;

public class EnemyShipTarget {
    int shieldLevel = 0;
    boolean hasRepulsors = false;
    boolean hasInterdictors = false;
    float damageTaken = 0;
    public EnemyShipTarget(TechTree tree) {
        if (tree.topDeflectorShieldTech() != null)
            shieldLevel = tree.topDeflectorShieldTech().damage;
        hasRepulsors = tree.knowsTechOfType(Tech.REPULSOR);
        hasInterdictors = tree.knowsTechOfType(Tech.SUBSPACE_INTERDICTOR);
    }
}
