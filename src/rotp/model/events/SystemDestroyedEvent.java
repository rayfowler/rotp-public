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
import rotp.model.galaxy.NamedObject;
import rotp.util.Base;

public class SystemDestroyedEvent implements Base, Serializable, StarSystemEvent {
    private static final long serialVersionUID = 1L;
    NamedObject attacker;
    int turn;
    public SystemDestroyedEvent(NamedObject att) {
        turn = galaxy().numberTurns();
        attacker = att;
    }
    @Override
    public int turn()                    { return turn; }
    @Override
    public boolean changesOwnership()    { return true; }
    @Override
    public int owner()                   { return Empire.NULL_ID; }
    @Override
    public String description() {
        if (attacker instanceof Empire) {
            Empire emp = (Empire) attacker;
            String s = text("SYSEVENT_DESTROYED");
            s = emp.replaceTokens(s, "alien");
            return s;
        }
        else if (attacker instanceof NamedObject) 
            return text("SYSEVENT_DESTROYED_MONSTER", attacker.name());
        else
            return text("SYSEVENT_DESTROYED_UNKNOWN");
    } 
}
