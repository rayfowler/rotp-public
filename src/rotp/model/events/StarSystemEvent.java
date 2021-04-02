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

import rotp.model.empires.Empire;
import rotp.model.galaxy.Galaxy;
import rotp.ui.UserPreferences;

public interface StarSystemEvent {
    int turn();
    String description();
    default boolean changesOwnership()    { return false; }
    default int owner()                   { return Empire.NULL_ID; }
    default String year() {
        return UserPreferences.displayYear() ? ""+(Galaxy.current().beginningYear() + turn()) : ""+(turn()+1);
    }
}
