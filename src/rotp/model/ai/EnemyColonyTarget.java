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

public class EnemyColonyTarget {
    public int shieldLevel = 0;
    public boolean hasInterdictors = false;
    public float damageTaken = 0;
    public EnemyColonyTarget(TechTree tree) {
        if (tree.topDeflectorShieldTech() != null)
            shieldLevel += tree.topDeflectorShieldTech().damage;
        if (tree.topPlanetaryShieldTech() != null)
            shieldLevel += tree.topPlanetaryShieldTech().damage;
        hasInterdictors = tree.knowsTechOfType(Tech.SUBSPACE_INTERDICTOR);
    }
}
