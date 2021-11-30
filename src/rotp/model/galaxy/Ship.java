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
package rotp.model.galaxy;

import java.util.Comparator;
import rotp.model.empires.Empire;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.sprites.FlightPathSprite;
import rotp.util.Base;

public interface Ship extends IMappedObject {
    static final int NOT_LAUNCHED = -1;
    default float hullPoints()          { return 0; }
    default boolean isRallied()         { return false; }
    default boolean passesThroughNebula(IMappedObject to) { return passesThroughNebula(this, to); }
    default boolean validDestination(int sysId) { return canSendTo(sysId);  }
    default boolean nullDest()          { return destSysId() == StarSystem.NULL_ID; }
    default boolean retreating()        { return false; }
    default boolean isTransport()       { return false; }

    public boolean canSendTo(int sysId);
    public float arrivalTime();
    public boolean visibleTo(int empId);
    public int empId();
    public int destSysId();
    public boolean inTransit();
    public boolean deployed();
    public FlightPathSprite pathSprite();
    public int maxMapScale();
    public void setDisplayed(GalaxyMapPanel map);
    public boolean displayed();

    public boolean isPotentiallyArmed(Empire e);
    public static Comparator<Ship> ARRIVAL_TIME = (Ship sh1, Ship sh2) -> Base.compare(sh1.arrivalTime(),sh2.arrivalTime());
    public static Comparator<Ship> EMPIRE_ID = (Ship sh1, Ship sh2) -> Base.compare(sh1.empId(), sh2.empId());
}
