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
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.StarSystem;
import rotp.model.ships.ShipDesignLab;
import rotp.ui.notifications.GNNNotification;
import rotp.util.Base;
import java.io.Serializable;
import java.util.List;

public class RandomEventComet implements Base, Serializable, RandomEvent {
    private static final long serialVersionUID = 1L;
    private int empId;
    private int sysId;
    private float cometHP = 0;
    private int turnsNeeded = 0;
    private int turnCount = 0;
    @Override
    public String statusMessage()               { return text("SYSTEMS_STATUS_COMET",str(turnsNeeded-turnCount)); }
    @Override
    public String systemKey()                   { return "MAIN_PLANET_EVENT_COMET"; }
    @Override
    public boolean goodEvent()    		{ return false; }
    @Override
    public boolean repeatable()    		{ return true; }
    @Override
    public String notificationText()    {
        String s1 = text("EVENT_COMET");
        s1 = s1.replace("[system]", player().sv.name(sysId));
        s1 = s1.replace("[years]", str((int)Math.ceil(turnsNeeded-turnCount)));
        s1 = galaxy().empire(empId).replaceTokens(s1, "target");
        return s1;
    }
    @Override
    public void trigger(Empire emp) {
        List<StarSystem> allSystems = emp.allColonizedSystems();
        StarSystem targetSystem = random(allSystems);
        targetSystem.eventKey(systemKey());

        empId = emp.id;
        sysId = targetSystem.id;

        turnsNeeded = roll(10,15);
        cometHP = 40*turnsNeeded;
        galaxy().events().addActiveEvent(this);
        if ((empId != Empire.NULL_ID) && !player().sv.name(sysId).isEmpty())
            GNNNotification.notifyRandomEvent(notificationText(), "GNN_Event_Comet");
    }
    @Override
    public void nextTurn() {
        battleComet();

        StarSystem sys = galaxy().system(sysId);
        empId = id(sys.empire()); // sometimes system empire changes
        
        if (cometHP <= 0) {
            cometDestroyed();
            return;
        }

        turnCount++;
        if (turnCount == turnsNeeded) {
            destroyColony();
            return;
        }

        if ((empId != Empire.NULL_ID) && (turnCount % 5 == 0) && (player().id == empId))
            GNNNotification.notifyRandomEvent(continuingText(), "GNN_Event_Comet");
    }
    private void battleComet() {
        StarSystem targetSystem = galaxy().system(sysId);
        List<ShipFleet> fleets = targetSystem.orbitingFleets();
        for (ShipFleet fl: fleets) {
            if (fl.isArmed() && fl.empire().alliedWith(empId))
                inflictDmgFromFleet(fl);
        }
    }
    private void inflictDmgFromFleet(ShipFleet fl) {
        float dmgInflicted = 0.0f;
        Empire empire = galaxy().empire(empId);
        for (int i = 0; i< ShipDesignLab.MAX_DESIGNS; i++) {
            if (fl.num(i) > 0) {
                int size = empire.shipLab().design(i).size();
                float weight = pow(5, size);
                dmgInflicted += (fl.num(i)*weight);
            }
        }
        cometHP = max(0, cometHP -dmgInflicted);
    }
    private String continuingText() {
        String s1 = text("EVENT_COMET_2");
        s1 = s1.replace("[system]", player().sv.name(sysId));
        s1 = s1.replace("[years]", str((int)Math.ceil(turnsNeeded-turnCount)));
        s1 = galaxy().empire(empId).replaceTokens(s1, "target");
        return s1;
    }
    private String goodEndText() {
        String s1 = text("EVENT_COMET_3");
        s1 = s1.replace("[system]", player().sv.name(sysId));
        s1 = galaxy().empire(empId).replaceTokens(s1, "target");
        return s1;
    }
    private String badEndText() {
        String s1 = text("EVENT_COMET_4");
        s1 = s1.replace("[system]", player().sv.name(sysId));
        s1 = galaxy().empire(empId).replaceTokens(s1, "target");
        return s1;
    }
    private void cometDestroyed() {
        galaxy().events().removeActiveEvent(this);
        StarSystem sys = galaxy().system(sysId);
        sys.clearEvent();
        
        session().removePendingNotification("GNN_Event_Comet");
        if ((empId != Empire.NULL_ID) && !player().sv.name(sysId).isEmpty())
            GNNNotification.notifyRandomEvent(goodEndText(), "GNN_Event_Comet");
    }
    private void destroyColony() {
        galaxy().events().removeActiveEvent(this);
        StarSystem sys = galaxy().system(sysId);       
        sys.addEvent(new SystemRandomEvent("SYSEVENT_COMET"));
        sys.clearEvent();
        sys.planet().sufferImpactEvent(); // destroys colony, downgrades planet type to Barren
        if ((empId != Empire.NULL_ID) && !player().sv.name(sysId).isEmpty())
            GNNNotification.notifyRandomEvent(badEndText(), "GNN_Event_Comet");
    }
}
