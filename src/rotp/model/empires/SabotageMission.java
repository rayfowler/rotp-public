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
package rotp.model.empires;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import rotp.model.colony.Colony;
import rotp.model.galaxy.StarSystem;
import rotp.model.incidents.SabotageBasesIncident;
import rotp.model.incidents.SabotageFactoriesIncident;
import rotp.model.incidents.SabotageRebellionIncident;
import rotp.util.Base;

public class SabotageMission implements Base, Serializable {
    private static final long serialVersionUID = 1L;
    public static final int FACTORIES = 1;
    public static final int BASES = 2;
    public static final int REBELLION = 3;
    public static final int NO_ACTION = 4;

    private final SpyNetwork spies;
    private final Spy spy;
    private final List<Empire> empiresToFrame = new ArrayList<>();
    private StarSystem system;
    private int factoriesDestroyed = 0;
    private int missileBasesDestroyed = 0;
    private int rebelsIncited = 0;
    private int missionType;

    public SabotageMission(SpyNetwork sn, Spy sp) {
        spies = sn;
        spy = sp;

        List<Empire> commonContacts = new ArrayList<>();
        for (Empire emp1 : sn.owner().contactedEmpires()) {
            EmpireView  eview2 = sn.empire().viewForEmpire(emp1);
            if ((eview2 != null) && eview2.embassy().contact())
                commonContacts.add(emp1);
        }
        if (commonContacts.size() > 1) {
            Empire frame1 = random(commonContacts);
            commonContacts.remove(frame1);
            empiresToFrame.add(frame1);
            empiresToFrame.add(random(commonContacts));
        }
    }
    public SpyNetwork spies()                  { return spies; }
    public Spy spy()                           { return spy; }
    public Empire target()                     { return spies.empire(); }
    public StarSystem starSystem()             { return system; }
    public int factoriesDestroyed()            { return factoriesDestroyed; }
    public int missileBasesDestroyed()         { return missileBasesDestroyed; }
    public int rebelsIncited()                 { return rebelsIncited; }
    public boolean isDestroyFactories()        { return missionType == FACTORIES; }
    public boolean isDestroyBases()            { return missionType == BASES; }
    public boolean isInciteRebellion()         { return missionType == REBELLION; }
    public boolean isNoAction()                { return missionType == NO_ACTION; }

    public void destroyFactories(StarSystem sys) {
        system = sys;
        missionType = FACTORIES;
        float weaponLevel = spies.owner().tech().weapon().techLevel();
        int chances = (int) Math.ceil(weaponLevel/10);

        factoriesDestroyed = 0;
        for (int i=0;i<chances;i++) {
            if (random() < .5)
                factoriesDestroyed += roll(1,5);
        }

        float factories = sys.colony().industry().factories();
        factoriesDestroyed = bounds(1, factoriesDestroyed, (int) factories);
        if (factoriesDestroyed > 0) {
            spies.report().recordSabotage(missionType, system.id, factoriesDestroyed);
            DiplomaticTreaty treaty = spies.owner().treaty(spies.empire());
            if (treaty != null)
                treaty.loseFactories(spies.empire(), factoriesDestroyed);
            sys.colony().industry().factories(factories-factoriesDestroyed);
            SabotageFactoriesIncident.addIncident(this);
            spies.checkForTreatyBreak();
            spies.owner().sv.refreshSpyScan(sys.id);
        }
    }
    public void destroyMissileBases(StarSystem sys) {
        system = sys;
        missionType = BASES;
        float weaponLevel = spies.owner().tech().weapon().techLevel();
        int chances = (int) Math.ceil(weaponLevel/10);

        missileBasesDestroyed = 0;
        for (int i=0;i<chances;i++) {
            if (random() < .5)
                missileBasesDestroyed++;
        }

        missileBasesDestroyed = bounds(1, missileBasesDestroyed, (int) sys.colony().defense().bases());

        if (missileBasesDestroyed > 0) {
            spies.report().recordSabotage(missionType, system.id, missileBasesDestroyed);
            sys.colony().defense().destroyBases(missileBasesDestroyed);
            SabotageBasesIncident.addIncident(this);
            spies.checkForTreatyBreak();
            spies.owner().sv.refreshSpyScan(sys.id);
        }
    }
    public void inciteRebellion(StarSystem sys) {
        log(spies.empire().name()+ " spies incite rebellion on "+sys.empire().raceName()+" system: "+player().sv.name(sys.id));
        system = sys;
        missionType = REBELLION;
        Colony col = sys.colony();

        // no rebels on homeworlds
        if (col.isCapital())
            return;

        float pct = roll(2,10)/100.0f;
        rebelsIncited = col.inciteRebels(pct, "GNN_PLAYER_REBELLION");

        if (rebelsIncited > 0) {
            spies.report().recordSabotage(missionType, system.id, rebelsIncited);
            SabotageRebellionIncident.addIncident(this);
            spies.checkForTreatyBreak();
        }
    }
    public void cancelMission() {
        missionType = NO_ACTION;
    }
}
