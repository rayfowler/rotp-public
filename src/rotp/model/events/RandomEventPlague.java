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

import rotp.model.colony.Colony;
import rotp.model.colony.ColonyResearchProject;
import rotp.model.empires.Empire;
import rotp.model.galaxy.StarSystem;
import rotp.ui.notifications.GNNNotification;
import rotp.util.Base;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RandomEventPlague implements Base, Serializable, RandomEvent, ColonyResearchProject {
    private static final long serialVersionUID = 1L;
    private int empId;
    private int sysId;
    private float researchNeeded = 0;
    private int turnCount = 0;
    private float researchRemaining = 0;
    @Override
    public String statusMessage()                   { return text("SYSTEMS_STATUS_PLAGUE", str((int)Math.ceil(researchRemaining))); }
    @Override
    public String systemKey()                   { return "MAIN_PLANET_EVENT_PLAGUE"; }
    @Override
    public boolean goodEvent()    		{ return false; }
    @Override
    public boolean repeatable()    		{ return true; }
    @Override
    public String notificationText()    {
        String s1 = text("EVENT_PLAGUE");
        s1 = s1.replace("[system]", galaxy().empire(empId).sv.name(sysId));
        s1 = galaxy().empire(empId).replaceTokens(s1, "target");
        return s1;
    }
    @Override
    public String projectKey()          { return text("MAIN_COLONY_SPENDING_PLAGUE"); }
    @Override
    public float remainingResearchBC()   { return researchRemaining; }
    @Override
    public void addResearchBC(float amt) {
        researchRemaining -= amt;
        if (researchRemaining <= 0)
            endPlague();
    }
    @Override
    public void trigger(Empire emp) {
        // find a random colony that has at least 30 population that does not already have a research project and is not in rebellion
        // allowing these combinations would complicate a lot of code in other places, so not allowed by fiat
        List<StarSystem> systems = new ArrayList<>();
        for (StarSystem sys : emp.allColonizedSystems()) {
            Colony col = sys.colony();
            if ((col.population() >= 30) && !col.research().hasProject() && !col.inRebellion())
                systems.add(sys);
        }
        if (systems.isEmpty())
            return;

        turnCount = 0;

        StarSystem targetSystem = random(systems);
        empId = emp.id;
        sysId = targetSystem.id;
        
        targetSystem.eventKey(systemKey());
        researchNeeded = roll(3,10) * targetSystem.colony().totalProductionIncome();
        researchRemaining = researchNeeded;
        if (player().knowsOf(empId)
        && !player().sv.name(sysId).isEmpty())
            GNNNotification.notifyRandomEvent(notificationText(), "GNN_Event_Plague");

        affectColony();
        galaxy().events().addActiveEvent(this);
        targetSystem.addEvent(new SystemRandomEvent("SYSEVENT_PLAGUE"));
    }
    @Override
    public void nextTurn() {
        affectColony();
        turnCount++;

        if ((turnCount % 5 == 0) && (player().id == empId))
            GNNNotification.notifyRandomEvent(continuingText(), "GNN_Event_Plague");
    }
    private void affectColony() {
        // reduce population by 5-10%
        StarSystem sys = galaxy().system(sysId);
        Colony targetColony = sys.colony();
        if (targetColony == null) {
            sys.abandoned(false);
            endPlague();
            return;
        }
            
        targetColony.becomeQuarantined();
        targetColony.research().project(this);
        
        Empire sysEmp = sys.empire();

        // if colony changed hands instead
        // transfer project to  new empire
        if (sysEmp.id != empId) {
            empId = sysEmp.id;
            researchRemaining = researchNeeded;
            if (sysEmp.isPlayerControlled())
                turnCount = -1;  // resets the notification counter so player is immediately notified
        }
        
        float popLossPct = roll(5,10)/100.0f;
        float newPop = targetColony.population() * (1-popLossPct);
        targetColony.setPopulation(newPop);
    }
    private String continuingText() {
        String s1 = text("EVENT_PLAGUE_2");
        s1 = s1.replace("[amt]", str((int)Math.ceil(researchRemaining)));
        s1 = s1.replace("[system]", galaxy().empire(empId).sv.name(sysId));
        s1 = galaxy().empire(empId).replaceTokens(s1, "target");        return s1;
    }
    private String endText() {
        String s1 = text("EVENT_PLAGUE_3");
        s1 = s1.replace("[system]", galaxy().empire(empId).sv.name(sysId));
        s1 = galaxy().empire(empId).replaceTokens(s1, "target");
        return s1;
    }
    private void endPlague() {
        galaxy().events().removeActiveEvent(this);
        galaxy().system(sysId).clearEvent();
        StarSystem sys = galaxy().system(sysId);
        sys.addEvent(new SystemRandomEvent("SYSEVENT_PLAGUE_ENDED"));
        Colony col = sys.colony();
        // possible colony is destroyed before plague ends
        
        session().removePendingNotification("GNN_Event_Plague");
        if (col != null) {
            col.research().endProject();
            col.clearQuarantine();
            if (player().knowsOf(empId)
            && !player().sv.name(sysId).isEmpty())
                GNNNotification.notifyRandomEvent(endText(), "GNN_Event_Plague");
        }
    }
}
