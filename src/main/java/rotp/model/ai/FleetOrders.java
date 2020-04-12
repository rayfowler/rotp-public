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

import java.util.ArrayList;
import java.util.List;
import rotp.model.galaxy.ShipFleet;
import rotp.model.ships.ShipDesign;
import rotp.model.ships.ShipDesignLab;
import rotp.util.Base;

public class FleetOrders implements Base {
    public ShipFleet owningFleet;
    public int destSysId;
    public final List<ShipFleet> reassignedFleets = new ArrayList<>();

    private final int[] num = new int[ShipDesignLab.MAX_DESIGNS];

    public FleetOrders(ShipFleet fl) {
        owningFleet = fl;
    }
    public float x()                         { return owningFleet.x(); }
    public float y()                         { return owningFleet.y(); }
    public int numAvailable(ShipDesign d)    { return num(d.id());  }
    public ShipDesign newestOfType(int type) { return newestOfMission(type); }
    @Override
    public String toString() {
        return concat("FleetOrders for dest:", str(destSysId), " fleet:", owningFleet.toString());
    }
    public void reset() {
        if (owningFleet.inTransit())
            destSysId = owningFleet.destSysId();
        else
            destSysId = owningFleet.sysId();

        reassignedFleets.clear();
        for (int i=0;i<num.length;i++)
            num[i] = owningFleet.num(i);
    }
    public void reassignShip(ShipDesign design, int destId, int numShips) {
        int designId = design.id();
        // keep track of any other ships from this fleet order going to the same
        // dest so they can be combined into one fleet
        int[] counts = new int[ShipDesignLab.MAX_DESIGNS];
        counts[design.id()] = numShips;
        galaxy().ships.deploySubfleet(owningFleet, counts, destId);
        removeShips(designId, numShips);
    }
    //
    // TEMP FLEET BEHAVIOR
    //
    public boolean hasShips() {
        boolean res = false;
        for (int i=0;i<num.length;i++)
            res = res || num[i]>0;
        return res;
    }
    public int numStacks()                        { return num.length; }
    public int num(int designNum)                 { return num[designNum];  }
    public void addShips(int designNum, int n)    { num[designNum] += n; }
    public void removeShips(int designNum, int n) { num[designNum] -= n; }

    public ShipDesign design(int i) {
        return owningFleet.empire().shipLab().design(i);
    }

    public ShipDesign newestOfMission(int type) {
        ShipDesignLab lab = owningFleet.empire().shipLab();
        ShipDesign newest = null;
        for (int i=0;i<num.length;i++) {
            if (num[i] > 0) {
                ShipDesign des = lab.design(i);
                if ((des != null) && (des.mission() == type)) {
                    if ((newest == null) || (newest.seq() < des.seq()))
                        newest = des;
                }
            }
        }
        return newest;
    }
}
