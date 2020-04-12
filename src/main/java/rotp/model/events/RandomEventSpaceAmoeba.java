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
import rotp.model.colony.Colony;
import rotp.model.empires.Empire;
import rotp.model.galaxy.SpaceAmoeba;
import rotp.model.galaxy.SpaceMonster;
import rotp.model.galaxy.StarSystem;
import rotp.model.planet.PlanetType;
import rotp.ui.notifications.GNNNotification;
import rotp.util.Base;

public class RandomEventSpaceAmoeba implements Base, Serializable, RandomEvent {
    private static final long serialVersionUID = 1L;
    public static SpaceMonster monster;
    private int empId;
    private int sysId;
    private int turnCount = 0;
    
    static {
        initMonster();
    }
    @Override
    public String statusMessage()               { return text("SYSTEMS_STATUS_SPACE_AMOEBA"); }
    @Override
    public String systemKey()                   { return "MAIN_PLANET_EVENT_AMOEBA"; }
    @Override
    public boolean goodEvent()    		{ return false; }
    @Override
    public boolean repeatable()    		{ return false; }
    @Override
    public int minimumTurn()                    { return RandomEvents.START_TURN + 50; }
    @Override
    public String notificationText()    {
        String s1 = text("EVENT_SPACE_AMOEBA");
        s1 = s1.replace("[system]", galaxy().empire(empId).sv.name(sysId));
        return s1;
    }
    @Override
    public void trigger(Empire emp) {
        log("Starting Amoeba event against: "+emp.raceName());
        StarSystem targetSystem = random(emp.allColonizedSystems());
        empId = emp.id;
        sysId = targetSystem.id;
        turnCount = 2;
        galaxy().events().addActiveEvent(this);
    }
    @Override
    public void nextTurn() {
        if (turnCount == 2) 
            approachSystem();     
        else if (turnCount == 0) 
            enterSystem();
        turnCount--;
    }
    private static void initMonster() {
        monster = new SpaceAmoeba();
    }
    private void enterSystem() {
        monster.visitSystem(sysId);
        monster.initCombat();
        StarSystem targetSystem = galaxy().system(sysId);
        targetSystem.clearEvent();
        Colony col = targetSystem.colony();
        if (!targetSystem.orbitingFleets().isEmpty())
            startCombat();
        else if ((col != null) && col.defense().isArmed())
            startCombat();
        
        if (monster.alive()) 
            destroyColony();
        else 
            amoebaDestroyed();         
    }
    private void startCombat() {
        StarSystem targetSystem = galaxy().system(sysId);
        galaxy().shipCombat().battle(targetSystem, monster);
    }
    private void approachSystem() {
        StarSystem targetSystem = galaxy().system(sysId);
        targetSystem.eventKey(systemKey());
        Empire pl = player();
        if (targetSystem.isColonized()) { 
            if (pl.knowsOf(targetSystem.empire()) || !pl.sv.name(sysId).isEmpty())
                GNNNotification.notifyRandomEvent(notificationText("EVENT_SPACE_AMOEBA", targetSystem.empire()), "GNN_Event_Amoeba");
        }
        else if (!pl.sv.name(sysId).isEmpty())
            GNNNotification.notifyRandomEvent(notificationText("EVENT_SPACE_AMOEBA_1", null), "GNN_Event_Amoeba");   
    }
    private void destroyColony() {
        StarSystem targetSystem = galaxy().system(sysId);
        Colony col = targetSystem.colony();
        if (col != null) {
            Empire pl = player();
            if (pl.knowsOf(targetSystem.empire()) || !pl.sv.name(sysId).isEmpty())
                GNNNotification.notifyRandomEvent(notificationText("EVENT_SPACE_AMOEBA_2", targetSystem.empire()), "GNN_Event_Amoeba");
            targetSystem.empire().lastAttacker(monster);
            targetSystem.planet().degradeToType(PlanetType.BARREN);
            col.destroy();
        }
        moveToNextSystem(); 
    }
    private void amoebaDestroyed() {
        galaxy().events().removeActiveEvent(this);
        
        monster.plunder();

        if (player().knowsOf(empId)|| !player().sv.name(sysId).isEmpty())
            GNNNotification.notifyRandomEvent(notificationText("EVENT_SPACE_AMOEBA_3", monster.lastAttacker()), "GNN_Event_Amoeba");
    }
    private void moveToNextSystem() {
        StarSystem targetSystem = galaxy().system(sysId);
        // next system is one of the 10 nearest systems
        // more likely to go to new system (25%) than visited system (5%)
        int[] near = targetSystem.nearbySystems();
        boolean stopLooking = false;
        
        int nextSysId = -1;
        int loops = 0;
        if (near.length > 0) {
            while (!stopLooking) {
                loops++;
                for (int i=0;i<near.length;i++) {
                    float chance = monster.vistedSystems().contains(near[i]) ? 0.05f : 0.25f;
                    if (random() < chance) {
                        nextSysId = near[i];
                        stopLooking = true;
                        break;
                    }
                }
                if (loops > 10) 
                    stopLooking = true;
            }
        }
        
        if (nextSysId < 0) {
            log("ERR: Could not find next system. Space Amoeba removed.");
            galaxy().events().removeActiveEvent(this);
            return;
        }
    
        log("Space Amoeba moving to system: "+nextSysId);
        StarSystem nextSys = galaxy().system(nextSysId);
        turnCount = (int) Math.ceil(3*nextSys.distanceTo(targetSystem));
        sysId = nextSys.id;        
    }
    private String notificationText(String key, Empire emp)    {
        String s1 = text(key);
        if (emp != null) {
            s1 = s1.replace("[system]", emp.sv.name(sysId));
            s1 = s1.replace("[race]", emp.raceName());
        }
        else 
            s1 = s1.replace("[system]", player().sv.name(sysId));
            
        return s1;
    }
}
