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
package rotp.model.ships;

import rotp.model.tech.TechCloaking;

public final class ShipSpecialCloaking extends ShipSpecial {
    private static final long serialVersionUID = 1L;
    public ShipSpecialCloaking (TechCloaking t) {
        tech(t);
        sequence(t.level + .05f);
    }
    @Override
    public boolean allowsCloaking() { return true;  }
    @Override
    public String name()            { return tech().name(); }
    @Override
    public String desc()            { return tech().brief(); }
}
