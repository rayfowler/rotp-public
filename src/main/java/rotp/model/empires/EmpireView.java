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

import java.awt.Image;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import rotp.model.galaxy.StarSystem;
import rotp.model.incidents.DiplomaticIncident;
import rotp.ui.diplomacy.DialogueManager;
import rotp.ui.diplomacy.DiplomaticReply;
import rotp.ui.diplomacy.DiplomaticCounterReply;
import rotp.util.Base;

public final class EmpireView implements Base, Serializable {
    private static final long serialVersionUID = 1L;
    private final Empire empire;
    private final Empire owner;
    private final DiplomaticEmbassy embassy;
    private final SpyNetwork spies;
    private final TradeRoute trade;
    private transient EmpireView otherView;

    public Empire empire()                { return empire; }
    public Empire owner()                 { return owner; }
    public int empId()                    { return empire.id; }
    public int ownerId()                  { return owner.id; }
    public DiplomaticEmbassy embassy()    { return embassy; }
    public SpyNetwork spies()             { return spies; }
    public TradeRoute trade()             { return trade; }
    public EmpireView otherView() {
        if (otherView == null)
            otherView = empire.viewForEmpire(owner);
        return otherView;
    }

    public EmpireView(Empire o, Empire c) {
        empire = c;
        owner = o;
        spies = new SpyNetwork(this);
        trade = new TradeRoute(this);
        embassy = new DiplomaticEmbassy(this);
        setSuggestedAllocations();
    }
    @Override
    public String toString()   { return concat(owner.raceName(), " View of: ", empire.raceName()); }

    public Integer listOrder() {
        int rangeMod = this.inEconomicRange() ? 0 : 100;
        return rangeMod + embassy().treaty().listOrder();
    }
    public boolean diplomats() {
        return !embassy.diplomatGone() && !otherView().embassy.diplomatGone() && !embassy.finalWar();
    }
    public Image flag() {
        if (embassy().anyWar())
            return empire().race().flagWar();
        else if (embassy().isFriend())
            return empire().race().flagPact();
        else
            return empire().race().flagNorm();
    }
    public Image dialogueBox() {
        if (embassy().anyWar())
            return owner().race().dialogWar();
        else if (embassy().isFriend())
            return owner().race().dialogPact();
        else
                    return owner().race().dialogNorm();
    }
    public float scaleOfContempt() {
        // returns 0 if equal power
        // returns 1, 2, 3 if we are 2x,3x,4x more powerful
        // reutrns -1,-2,-3 if we are 1/2x, 1/3x, 1/4x as powerful
        float ourPower = empireWeakness();
        if (ourPower == 1)
            return 0;
        else if (ourPower > 1)
            return ourPower-1;
        else
            return -(1/ourPower)+1;
    }
    public float empireWeakness() { return 1 / empirePower(); }
    public float empirePower() {
        // This returns the estimated strength of the view.civ vs. the view.owner
        // > 1 means empire is STRONGER than view.owner
        // < 1 means empire is WEAKER than view.owner
        float num = owner.powerLevel(empire)+1;
        float den = owner.powerLevel(owner);
        return num / den;
    }
    public void refresh() {
        if (!embassy.contact()) {
            float range = owner.shipRange();
            for (int id=0; id<owner.sv.count();id++) {
                if ((owner.sv.empire(id) == empire) && owner.sv.withinRange(id, range))
                    setContact();
            }
        }
        if (!embassy.contact())
            return;
    }
    public boolean inEconomicRange() { return owner().inEconomicRange(empId()); }
    public void breakAllTreaties() {
        trade.stopRoute();
        embassy.resetTreaty();
    }
    public List<Integer> nominalTradeLevels() {
        float prod = min(empire.totalPlanetaryProduction(), owner.totalPlanetaryProduction());
        float currTrade = trade.level();

        List<Integer> values = new ArrayList<>();
        int maxOptions = 4;
        float maxPct = 0.25f;
        float lvlPct = maxPct/maxOptions;
        for (int i=1;i<=maxOptions;i++) {
            float pct = lvlPct*i;
            Integer level = 25* ((int) (pct * prod/25));
            if ((level > currTrade) && !values.contains(level))
                values.add(level);
        }
        return values;
    }
    public void setSuggestedAllocations() {
        if (owner.isAIControlled())
            spies.setSuggestedAllocations();
    }
    public void setContact() {
        if (owner.extinct() || empire.extinct())
            return;
        // when civ is within propulsion range
        if (!owner.hasContacted(empire))
            embassy.makeFirstContact();
        else
            embassy.setContact();

        trade.setContact();

        if (!otherView().embassy.contact())
            otherView().setContact();

        setSuggestedAllocations();
    }
    public void refreshSystemSpyViews() {
        // get all #empire views AND all views we think belong to #empire
        // keeps us from sabotaging #empire colonies that have been destroyed
        Set<StarSystem> allSystems = new HashSet<>(empire.allColonizedSystems());
        allSystems.addAll(owner.systemsForCiv(empire));
        for (StarSystem sys : allSystems)
            owner.sv.refreshSpyScan(sys.id);
    }
    public void nextTurn(float prod) {
        log(this+": nextTurn");
        if (empire.extinct())
            return;

        embassy.nextTurn(prod);
        spies.nextTurn(prod);
    }
    public void makeDiplomaticOffers() {
        log(this+": assessTurn");
        embassy.assessTurn();
        trade.assessTurn();
        if (owner.isAI())
            owner.diplomatAI().makeDiplomaticOffers(this);
    }
    public String decode(String s) {
        String s1 = owner.replaceTokens(s, "my");
        s1 = empire.replaceTokens(s1, "your");
        return s1;
    }
    public String decode(String s, Empire other) {
        String s1 = owner.replaceTokens(s, "my");
        s1 = empire.replaceTokens(s1, "your");
        s1 = other.replaceTokens(s1, "other");
        return s1;
    }
    private String decodedMessage(String key) {
        return decode(DialogueManager.current().randomMessage(key, empire));
    }
    private String decodedMessage(String key, Empire otherEmp) {
        return decode(DialogueManager.current().randomMessage(key, empire), otherEmp);
    }
    private String decodedMessage(String key, DiplomaticIncident inc) {
        return decode(inc.decode(DialogueManager.current().randomMessage(key, empire)));
    }
    public DiplomaticReply refuse(String reason) {
        return DiplomaticReply.answer(false, decodedMessage(reason));
    }
    public DiplomaticReply refuse(String reason, Empire otherEmp) {
        return DiplomaticReply.answer(false, decodedMessage(reason, otherEmp));
    }
    public DiplomaticReply accept(String reason) {
        return DiplomaticReply.answer(true, decodedMessage(reason));
    }
    public DiplomaticReply accept(String reason, DiplomaticIncident inc) {
        return DiplomaticReply.answer(true, decodedMessage(reason, inc));
    }
    public DiplomaticReply counter(String reason, Empire target, List<String> techs, float bribeAmt) {
        return DiplomaticCounterReply.answer(true, decodedMessage(reason, target), id(target), techs, bribeAmt);
    }
    public static Comparator<EmpireView> PLAYER_LIST_ORDER = (EmpireView o1, EmpireView o2) -> o1.listOrder().compareTo(o2.listOrder());
    public static Comparator<EmpireView> BY_RACENAME       = (EmpireView o1, EmpireView o2) -> o1.empire().raceName().compareTo(o2.empire().raceName());
}