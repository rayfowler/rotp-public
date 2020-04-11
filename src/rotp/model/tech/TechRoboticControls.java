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

import rotp.model.colony.Colony;
import rotp.model.empires.Empire;

public final class TechRoboticControls extends Tech {
    public static final int BASE_ROBOT_CONTROLS = 2;
    public int mark;

    public TechRoboticControls (String typeId, int lv, int seq, boolean b, TechCategory c) {
        id(typeId, seq);
        typeSeq = seq;
        level = lv;
        cat = c;
        free = b;
        init();
    }
    @Override
    public Colony.Orders followup()                   { return Colony.Orders.FACTORIES; }
    @Override
    public void init() {
        super.init();
        techType = Tech.ROBOTIC_CONTROLS;

        switch(typeSeq) {
            case 0: mark = 3;  break;
            case 1: mark = 4;  break;
            case 2: mark = 5;  break;
            case 3: mark = 6;  break;
            case 4: mark = 7;  break;
        }
    }
    @Override
    public boolean isRoboticControlsTech()  { return true; }
    @Override
    public boolean isObsolete(Empire c) {
        return mark < c.tech().baseRobotControls();
    }
    @Override
    public float baseValue(Empire c) { return c.ai().scientist().baseValue(this); }
    @Override
    public void provideBenefits(Empire c) {
        super.provideBenefits(c);
        if (!isObsolete(c))
            c.tech().topRoboticControlsTech(this);
    }
}
