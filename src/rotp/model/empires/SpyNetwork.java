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
import java.util.Collections;
import java.util.List;
import rotp.model.galaxy.StarSystem;
import rotp.model.incidents.EspionageTechIncident;
import rotp.model.incidents.SpyConfessionIncident;
import rotp.model.ships.ShipDesign;
import rotp.model.tech.Tech;
import rotp.model.tech.TechTree;
import rotp.ui.RotPUI;
import rotp.ui.notifications.SabotageNotification;
import rotp.util.Base;

public final class SpyNetwork implements Base, Serializable {
    private static final long serialVersionUID = 1L;
    private static final String noneText = "RACES_INTEL_SPENDING_NONE";
    private static final String yearsText = "RACES_INTEL_COMPLETION_YEARS";
    private static final String perYearText = "RACES_INTEL_COMPLETION_PER_YEAR";
    private static final int MAX_SPENDING_TICKS = 20;
    private static final float MAX_SPENDING_PCT = 0.10f;
    private static final int MIN_SPIES_FOR_FLEET_VIEW = 1;

    public enum Sabotage {
        FACTORIES, MISSILES, REBELS;
    }
    public enum Mission {
        HIDE, SABOTAGE, ESPIONAGE;
    }

    private final EmpireView view;
    private final List<Spy> activeSpies = new ArrayList<>();
    private final TechTree tech = new TechTree();
    private final List<ShipView> shipViews = new ArrayList<>();
    private int maxSpies = 1;

    // from 0-20 ticks, with each tick representing 0.5% of total empire production
    // full spending is 10% of empire production
    private int allocation = 0;
    private float allocationBC = 0;
    private Mission mission = Mission.HIDE;
    private int lastSpyDate = -1;

    private transient List<Tech> possibleTechs = new ArrayList<>();
    private final FleetView fleetView  = new FleetView();
    private transient int spiesLost = 0;
    private transient List<StarSystem> baseTargets;
    private transient List<StarSystem> factoryTargets;
    private transient List<StarSystem> rebellionTargets;

    public EmpireView view()         { return view; }
    public FleetView fleetView()     { return fleetView; }
    public List<StarSystem> baseTargets() {
        if (baseTargets == null)
            baseTargets = new ArrayList<>();
        return baseTargets;
    }
    public List<StarSystem> factoryTargets() {
        if (factoryTargets == null)
            factoryTargets = new ArrayList<>();
        return factoryTargets;
    }
    public List<StarSystem> rebellionTargets() {
        if (rebellionTargets == null)
            rebellionTargets = new ArrayList<>();
        return rebellionTargets;
    }
    public SpyNetwork(EmpireView v) {
        view = v;
        tech.init(empire(), true);
    }

    public Empire owner()            { return view.owner(); }
    public Empire empire()           { return view.empire(); }
    public int lastSpyDate()         { return lastSpyDate; }
    public TechTree tech()           { return tech; }
    public List<ShipView> ships()    { return shipViews; }
    public void beginHide()          {  mission = Mission.HIDE; }
    public void beginSabotage()      {  mission = Mission.SABOTAGE; }
    public void beginEspionage()     {  mission = Mission.ESPIONAGE; }

    public boolean isHide()          { return mission == Mission.HIDE; }
    public boolean isSabotage()      { return mission == Mission.SABOTAGE; }
    public boolean isEspionage()     { return mission == Mission.ESPIONAGE; }
    
    public int maxSpies()            { return maxSpies; }
    public void maxSpies(int n)      { maxSpies = max(0,n); }
    public void increaseMaxSpies()   { maxSpies++; }
    public void decreaseMaxSpies()   { maxSpies = max(0, maxSpies-1); }
    
    public String numSpiesLabel() {
        int n = activeSpies.size();
        if (n == 0)
            return text("RACES_INTEL_NO_SPIES");
        else if (n == 1)
            return text("RACES_INTEL_1_SPY");
        else
            return text("RACES_INTEL_N_SPIES", str(n));
    }
    public String missionName() {
        switch(mission) {
            case HIDE:       return text("SPY_HIDE");
            case ESPIONAGE:  return text("SPY_ESPIONAGE");
            case SABOTAGE:   return text("SPY_SABOTAGE");
        }
        return "?";
    }
    public void nextMission() {
        switch(mission) {
            case HIDE:       mission = Mission.ESPIONAGE; break;
            case ESPIONAGE:  mission = Mission.SABOTAGE; break;
            case SABOTAGE:   mission = Mission.HIDE; break;
        }        
    }
    public void prevMission() {
        switch(mission) {
            case HIDE:       mission = Mission.SABOTAGE; break;
            case ESPIONAGE:  mission = Mission.HIDE; break;
            case SABOTAGE:   mission = Mission.ESPIONAGE; break;
        }        
    }
    public boolean hasSpies()        { return !activeSpies.isEmpty(); }
    public List<Spy> activeSpies()   { return activeSpies; }
    public int numActiveSpies()      { return activeSpies().size(); }
    public int spiesLost()           { return spiesLost; }

    public List<Tech> possibleTechs() {
        if (possibleTechs == null)
            possibleTechs = new ArrayList<>();
        return possibleTechs;
    }
    public int allocation()           { return allocation; }
    public void allocation(int i)     { allocation = bounds(0,i,MAX_SPENDING_TICKS); }
    public float allocationPct()     { return (float) allocation/MAX_SPENDING_TICKS; }
    public float allocationCostPct() { 
        if (numActiveSpies() >= maxSpies)
            return 0;
        if (!view.inEconomicRange())
            return 0;

        return allocationPct() * MAX_SPENDING_PCT; 
    }
    public void decreaseSpending()    { allocation(allocation-1); }
    public void increaseSpending()    { allocation(allocation+1); }
    public void allocationPct(float d) {
        // d assumed to be between 0 & 1, representing pct of slider clicked
        float incr = 1.0f/(MAX_SPENDING_TICKS+1);
        float sum = 0;
        for (int i=0;i<MAX_SPENDING_TICKS+1;i++) {
            sum += incr;
            if (d <= sum) {
                allocation(i);
                return;
            }
        }
        allocation(MAX_SPENDING_TICKS);
    }
    public int reportAge() {
        return lastSpyDate < 0 ? -1 : galaxy().currentYear() - lastSpyDate;
    }
    public String newSpiesExpected() {
        if (allocationCostPct() == 0)
            return text(noneText);
        if (view.embassy().unity())
            return text(noneText);
        if (!view.inEconomicRange())
            return text(noneText);

        float civProd = this.owner().totalPlanetaryProduction();
        float expectedBC = civProd * allocationCostPct();
        float cost = costForNextSpy();
        float netCost = cost - allocationBC;

        if (expectedBC < netCost) {
            int turns = (int) Math.ceil(netCost/expectedBC);
            return text(yearsText, turns);
        }

        int newSpies = 1;
        expectedBC -= netCost;
        cost *= 2;
        while (expectedBC > cost) {
            expectedBC -= cost;
            newSpies++;
            cost *= 2;
        }

        if (newSpies == 1)
            return text(yearsText, 1);
        else
            return text(perYearText, newSpies);
    }
    public void nextTurn(float prod) {
        Collections.sort(shipViews, ShipView.VIEW_DATE);

        if (!view().inEconomicRange())
            return;
        
        // auto-update everything at no cost if unity 
        if (view.embassy().unity()) {
            lastSpyDate = galaxy().currentYear();
            view().refreshSystemSpyViews();
            updateTechList();
            allocation(0);
            activeSpies.clear();
            maxSpies(0);
            return;
        }
        
        log(view+" Spies: nextTurn");
        if (!activeSpies().isEmpty())
            view().refreshSystemSpyViews();
        
        baseTargets = sabotageBaseTargets();
        factoryTargets = sabotageFactoryTargets();
        rebellionTargets = sabotageRebellionTargets();

        spiesLost = 0;
        if (empire().extinct()) {
            activeSpies.clear();
            return;
        }

        allocateSpyBC(prod * allocationCostPct());
        if (activeSpies.isEmpty())
            return;

        if (activeSpies.size() >= MIN_SPIES_FOR_FLEET_VIEW)
            updateFleetView(empire());

        lastSpyDate = galaxy().currentYear();
        updateTechList();

        boolean spyConfessed = sendSpiesToInfiltrate();

        if (spyConfessed || activeSpies.isEmpty() || isHide())
            return;

        sendSpiesToAttemptMission();

        if (isEspionage())
            startEspionageMission();
        else if (isSabotage())
            startSabotageMission();
    }
    public boolean canSabotage() {
        // cannot sabotage until we've scouted at least one system
        // this prevents activity after contact based on fleet contact
        return view.owner().numSystemsForCiv(view.empire()) > 0;
    }
    public void updateTechList() {
        tech.spyOnTechs(empire().tech());
        float maxTech = owner().tech().maxTechLevel();
        possibleTechs = empire().tech().worseTechsUnknownToCiv(owner().tech(), maxTech);
    }
    private boolean sendSpiesToInfiltrate() {
        boolean confession = false;
        float adj = overallSecurityAdj();
        List<Spy> spiesAttempting = new ArrayList<>(activeSpies());
        for (Spy spy: spiesAttempting) {
            spy.attemptInfiltration(adj);
            if (spy.eliminated()) {
                spiesLost++;
                activeSpies().remove(spy);
            }
            confession = confession || spy.confesses();
        }
        // spy confesses to enemy, who gets a Diplomatic Incident
        if (confession)
            view.otherView().embassy().addIncident(new SpyConfessionIncident(view.otherView(), this));

        return confession;
    }
    private void sendSpiesToAttemptMission() {
        float adj = spyInfiltrationAdj();
        for (Spy spy: activeSpies)
            spy.attemptMission(adj);
    }
    private void startEspionageMission() {
        // no unknown techs to potentially steal
        if (possibleTechs().isEmpty())
            return;

        // from active spies with successful, find the one with the
        // highest "steal number"... he will acquire the best tech
        float maxTechLevel = owner().tech().maxTechLevel();
        float bestStealNumber = 0;
        Spy bestSpy = null;
        for (Spy spy: activeSpies()) {
            if (spy.missionSucceeds()) {
                float spyStealNumber = random() * maxTechLevel;
                if (spyStealNumber > bestStealNumber) {
                    bestStealNumber = spyStealNumber;
                    bestSpy = spy;
                }
            }
        }

        // if no spies had successful missions,  quit
        if (bestSpy == null)
            return;

        // build list of techs at or below the steal number
        List<Tech> espionageChoices = new ArrayList<>();
        for (Tech pTech: possibleTechs()) {
            if (pTech.level() <= bestStealNumber)
                espionageChoices.add(pTech);
        }

        // if no techs can be stolen this turn
        if (espionageChoices.isEmpty())
            return;

        EspionageMission eMission = chooseTechToSteal(bestSpy, espionageChoices);

        // if spy caught or is going to frame an empire, create incident
        if (bestSpy.caught() || bestSpy.canFrame()) {
            Empire victim = view.empire();
            Empire thief = eMission.thief();
            EmpireView victimView = victim.viewForEmpire(thief);
            if (victimView != null)
                victimView.embassy().addIncident(new EspionageTechIncident(victimView, eMission));
        }
    }
    private EspionageMission chooseTechToSteal(Spy spy, List<Tech> techs) {
        if (techs.isEmpty())
            return null;

        StarSystem randomSystem = random(empire().allColonizedSystems());
        EspionageMission eMission = new EspionageMission(this, spy, techs, randomSystem);

        // ai will choose now.. player choice is deferred until UI is displayed
        if (owner().isAIControlled()) {
            eMission.stealTech(owner().ai().scientist().mostDesirableTech(techs));
            if (eMission.canFrame())
                eMission.frameEmpire(owner().spyMasterAI().suggestToFrame(eMission.empiresToFrame()));
        }
        else {
            // this brings up category selection panel
            // which triggers mission.stealTech() and displayed tech stolen panel
            // which then checks mission.canFrame and triggers mission.frameEmpire()
            RotPUI.instance().selectEspionageMissionPanel(eMission, empire().id);
        }

        return eMission;
    }
    public void setSuggestedAllocations() {
        owner().spyMasterAI().setSpyingAllocation(view);
        owner().spyMasterAI().setSpyingMission(view);
    }
    public List<Tech> unknownTechs() {
        return tech.techsUnknownTo(owner());
    }
    private void allocateSpyBC(float bc) {
        allocationBC += bc;
        float cost = costForNextSpy();

        while (allocationBC >= cost) {
            addNewSpy();
            allocationBC -= cost;
            cost = costForNextSpy();
        }
    }
    private void addNewSpy() { activeSpies.add(new Spy(this));  }

    private float costForNextSpy() {
        float spyCost = owner().baseSpyCost();
        for (int i=1;i<activeSpies.size();i++)
            spyCost *= 2;
        return spyCost;
    }
    private float overallSecurityAdj() {
        // security pct based on their spending
        float adj = empire().totalInternalSecurityPct();

        // adjust for their race
        adj += empire().race().internalSecurityAdj();

        // if spy is at a computer tech disadvantage, give bonus to them
        float techDiff = spyTechAdvantage();
        if (techDiff < 0)
            adj -= techDiff;

        // if no techs to steal on an espionage mission, then treat as 'hiding' this turn
        // eliminates some micro-managing by player
        if ((isEspionage() && possibleTechs().isEmpty())
        || isHide())
            adj -= .3;
        return adj;
    }
    private float spyInfiltrationAdj() {
        // start with our racial bonus
        float adj = owner().race().spyInfiltrationAdj();
        // if spy is at a computer tech advantage, give bonus to spy
        float techDiff = spyTechAdvantage();
        if (techDiff > 0)
            adj += techDiff;
        return adj;
    }
    private float spyTechAdvantage() {
        return (owner().tech().computer().techLevel() - empire().tech().computer().techLevel()) / 100;
    }
    private void startSabotageMission() {
        // from active spies with successful, find the one with the
        // highest "steal number"... he will acquire the best tech
        float maxTechLevel = owner().tech().maxTechLevel();
        float bestStealNumber = 0;
        Spy bestSpy = null;
        for (Spy spy: activeSpies()) {
            if (spy.missionSucceeds()) {
                float spyStealNumber = random() * maxTechLevel;
                if (spyStealNumber > bestStealNumber) {
                    bestStealNumber = spyStealNumber;
                    bestSpy = spy;
                }
            }
        }

        if (bestSpy == null)
            return;

        Sabotage sabotageChoice = owner().spyMasterAI().bestSabotageChoice(view);
        if (sabotageChoice == null)
            return;

        SabotageMission eMission = new SabotageMission(this, bestSpy);

        StarSystem chosenSystem = owner().spyMasterAI().bestSystemForSabotage(view, sabotageChoice);

        if (chosenSystem == null)
            return;

        if (owner().isPlayer()) {
            SabotageNotification.addMission(eMission, chosenSystem.id);
            return;
        }

        switch(sabotageChoice) {
            case FACTORIES:
                eMission.destroyFactories(chosenSystem); break;
            case MISSILES:
                eMission.destroyMissileBases(chosenSystem); break;
         case REBELS:
                eMission.inciteRebellion(chosenSystem); break;
        }
    }
    public ShipView shipViewFor(ShipDesign d) {
        for (ShipView sv : shipViews) {
            if (sv.matches(d))
                return sv;
        }
        return null;
    }
    public void detectShip(ShipDesign d) {
        ShipView sv = shipViewFor(d);
        if (sv == null)
            sv = addShipView(d);

        sv.detect();
    }
    public void encounterShip(ShipDesign d) {
        ShipView sv = shipViewFor(d);
        if (sv == null)
            sv = addShipView(d);

        sv.encounter();
    }
    public void scanShip(ShipDesign d) {
        ShipView sv = shipViewFor(d);
        if (sv == null)
            sv = addShipView(d);

        sv.scan();
    }
    private List<StarSystem> sabotageBaseTargets() {
        List<StarSystem> targets = new ArrayList<>();
        Empire owner = view().owner();
        for (StarSystem sys: owner.systemsForCiv(view().empire())) {
            if (owner.sv.canSabotageBases(sys.id))
                targets.add(sys);
        }
        return targets;
    }
    private List<StarSystem> sabotageFactoryTargets() {
        List<StarSystem> targets = new ArrayList<>();
        Empire owner = view().owner();
        for (StarSystem sys: view().owner().systemsForCiv(view().empire())) {
            if (owner.sv.canSabotageFactories(sys.id))
                targets.add(sys);
        }
        return targets;
    }
    private List<StarSystem> sabotageRebellionTargets() {
        List<StarSystem> targets = new ArrayList<>();
        Empire owner = view().owner();
        for (StarSystem sys: view().owner().systemsForCiv(view().empire())) {
            if (owner.sv.canInciteRebellion(sys.id))
                targets.add(sys);
        }
        return targets;
    }
    private ShipView addShipView(ShipDesign d) {
        ShipView sv = new ShipView(owner(), d);
        shipViews.add(sv);
        return sv;
    }
    private void updateFleetView(Empire e) {
        fleetView.date(galaxy().currentYear());
        fleetView.small(e.shipCount(ShipDesign.SMALL));
        fleetView.medium(e.shipCount(ShipDesign.MEDIUM));
        fleetView.large(e.shipCount(ShipDesign.LARGE));
        fleetView.huge(e.shipCount(ShipDesign.HUGE));
    }
    public class FleetView implements Serializable {
        private static final long serialVersionUID = 1L;
        private int small, medium, large, huge, date;
        public int date()         { return date; }
        public void date(int i)   { date = i; }
        public int small()        { return small; }
        public void small(int i)  { small = i; }
        public int medium()       { return medium; }
        public void medium(int i) { medium = i; }
        public int large()        { return large; }
        public void large(int i)  { large = i; }
        public int huge()         { return huge; }
        public void huge(int i)   { huge = i; }
        public boolean noReport() { return date == 0; }
        public int reportAge()    { return galaxy().currentYear() - date; }
    }
}