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
package rotp.model.events;

import java.io.Serializable;
import rotp.model.empires.Empire;
import rotp.ui.UserPreferences;
import rotp.util.Base;

public class SystemCapturedEvent implements Base, Serializable, StarSystemEvent {
    private static final long serialVersionUID = 1L;
    int empId;
    int turn;
    public SystemCapturedEvent(int id) {
        turn = galaxy().numberTurns();
        empId = id;
    }
    @Override
    public String year() {
        return UserPreferences.displayYear() ? str(galaxy().beginningYear() + turn) : str(turn);
    }
    @Override
    public String description() {
        Empire emp = galaxy().empire(empId);
        return text("SYSEVENT_CAPTURED",emp.raceName());
    } 
}
