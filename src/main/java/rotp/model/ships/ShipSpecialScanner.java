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

import rotp.model.tech.TechScanner;

public final class ShipSpecialScanner extends ShipSpecial {
    private static final long serialVersionUID = 1L;
    public ShipSpecialScanner(TechScanner t) {
        tech(t);
        sequence(t.level + .05f);
    }
    @Override
    public TechScanner tech() { return (TechScanner) super.tech(); }
    @Override
    public boolean allowsScanning()    { return true; }
    @Override
    public int attackBonus()           { return tech().shipAttackBonus; }
    @Override
    public int initiativeBonus()       { return tech().shipInitiativeBonus; }
}
