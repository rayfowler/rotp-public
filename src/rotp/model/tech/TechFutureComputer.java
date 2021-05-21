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
package rotp.model.tech;

import rotp.model.empires.Empire;

public final class TechFutureComputer extends Tech {
    int number;
    public TechFutureComputer (String typeId, int lv, int seq, boolean b, TechCategory c) {
        id(typeId, seq);
        level = lv;
        cat = c;
        free = b;
        techType = Tech.FUTURE_COMPUTER;
        typeSeq = seq;
        init();
    }
    @Override
    public boolean isFutureTech()        { return true; }
    @Override
    public int futureTechLevel()         { return typeSeq+1; } // number is zero-based
    // all future techs can always be research
    @Override
    public float discoveryPct()          { return 1.0f; }
    @Override
    public float baseValue(Empire c) { return c.ai().scientist().baseValue(this); }
}
