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

public class RandomEventPiracy implements Base, Serializable, RandomEvent {
    private static final long serialVersionUID = 1L;
    private int empId;
    private int sysId;
    private float pirateHP = 0;
    private int turnCount = 0;
    @Override
    public String statusMessage()               { return text("SYSTEMS_STATUS_PIRACY"); }
    @Override
    public String systemKey()                   { return "MAIN_PLANET_EVENT_PIRACY"; }
    @Override
    public boolean goodEvent()    		{ return false; }
    @Override
    public boolean repeatable()    		{ return true; }
    @Override
    public String notificationText()    {
        String s1 = text("EVENT_PIRACY");
        s1 = s1.replace("[amt]", str((int)Math.ceil(100*piracyRate())));
        s1 = s1.replace("[system]", galaxy().empire(empId).sv.name(sysId));
        s1 = galaxy().empire(empId).replaceTokens(s1, "target");
        return s1;
    }
    @Override
    public void trigger(Empire emp) {
        if (!emp.hasTrade())
            return;

        StarSystem targetSystem = random(emp.allColonizedSystems());
        empId = emp.id;
        sysId = targetSystem.id;
  
        targetSystem.eventKey(systemKey());
        targetSystem.piracy(true);
        pirateHP = roll(300,450);
        affectEmpireTrade();
        galaxy().events().addActiveEvent(this);
        if (player().knowsOf(empId)
        && !player().sv.name(sysId).isEmpty())
            GNNNotification.notifyRandomEvent(notificationText(), "GNN_Event_Piracy");
    }
    @Override
    public void nextTurn() {
        StarSystem sys = galaxy().system(sysId);
        // if colony destroyed, pirates leave
        if (!sys.isColonized()) {
            endPiracy();
            return;
        }

        // if colony changed hands instead
        // transfer piracy rate to new empire
        Empire prevEmp = galaxy().empire(empId);
        Empire sysEmp = sys.empire();
        if (sysEmp.id != empId) {
            sysEmp.tradePiracyRate(prevEmp.tradePiracyRate());
            prevEmp.tradePiracyRate(0.0f);
            empId = sysEmp.id;
            if (sysEmp.isPlayerControlled())
                turnCount = -1;  // resets the notification counter so player is immediately notified
        }

        battlePirates();

        if (pirateHP <= 0) {
            endPiracy();
            return;
        }

        affectEmpireTrade();
        turnCount++;

        if ((turnCount % 5 == 0) && sysEmp.isPlayerControlled())
            GNNNotification.notifyRandomEvent(continuingText(), "GNN_Event_Piracy");
    }
    private float piracyRate()          { return pirateHP / 500.0f; }
    private void affectEmpireTrade()    { galaxy().empire(empId).tradePiracyRate(piracyRate()); }
    private void battlePirates() {
        StarSystem sys = galaxy().system(sysId);
        List<ShipFleet> fleets = sys.orbitingFleets();
        for (ShipFleet fl: fleets) {
            if (fl.isArmed() && fl.empire().alliedWith(empId))
                inflictDmgFromFleet(fl);
        }
    }
    private void inflictDmgFromFleet(ShipFleet fl) {
        float dmgInflicted = 0.0f;
        Empire emp = fl.empire();
        for (int i = 0; i< ShipDesignLab.MAX_DESIGNS; i++) {
            if (fl.num(i) > 0) {
                int size = emp.shipLab().design(i).size();
                float weight = pow(5, size);
                dmgInflicted += (fl.num(i)*weight);
            }
        }
        pirateHP = max(0, pirateHP-dmgInflicted);
    }
    private String continuingText() {
        String s1 = text("EVENT_PIRACY_2");
        s1 = s1.replace("[amt]", str((int)Math.ceil(100*piracyRate())));
        s1 = s1.replace("[system]", galaxy().empire(empId).sv.name(sysId));
        s1 = galaxy().empire(empId).replaceTokens(s1, "target");
        return s1;
    }
    private String endText() {
        String s1 = text("EVENT_PIRACY_3");
        s1 = s1.replace("[system]", galaxy().empire(empId).sv.name(sysId));
        s1 = galaxy().empire(empId).replaceTokens(s1, "target");
        return s1;
    }
    private void endPiracy() {
        StarSystem sys = galaxy().system(sysId);
        Empire emp = galaxy().empire(empId);
        galaxy().events().removeActiveEvent(this);
        sys.clearEvent();
        sys.piracy(false);
        emp.tradePiracyRate(0.0f);
        
        session().removePendingNotification("GNN_Event_Piracy");
        if (player().knowsOf(empId)
        && !player().sv.name(sysId).isEmpty())
            GNNNotification.notifyRandomEvent(endText(), "GNN_Event_Piracy");
    }
}
