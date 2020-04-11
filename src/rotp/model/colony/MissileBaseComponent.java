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
package rotp.model.colony;

import java.io.Serializable;
import rotp.model.empires.Empire;
import rotp.model.tech.Tech;
import rotp.util.Base;

public class MissileBaseComponent implements Base, Serializable { 
    private static final long serialVersionUID = 1L;
    private String techId;
    private int baseCost = 0;

    public Tech tech()                { return tech(techId); }
    public void tech(Tech t)          { techId = t.id(); }
    public int baseCost()             { return baseCost; }
    public void baseCost(int d)       { baseCost = d; }

    public float cost(Empire emp) {
        return techId == null ? 0 : tech().costMiniaturization(emp) * baseCost;
    }
}
