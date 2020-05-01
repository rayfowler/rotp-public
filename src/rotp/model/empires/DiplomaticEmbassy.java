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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import rotp.model.galaxy.StarSystem;
import rotp.model.incidents.BreakAllianceIncident;
import rotp.model.incidents.BreakPactIncident;
import rotp.model.incidents.BreakTradeIncident;
import rotp.model.incidents.DeclareWarIncident;
import rotp.model.incidents.DemandTributeIncident;
import rotp.model.incidents.DiplomaticIncident;
import rotp.model.incidents.EncroachmentIncident;
import rotp.model.incidents.ErraticWarIncident;
import rotp.model.incidents.ExchangeTechnologyIncident;
import rotp.model.incidents.FirstContactIncident;
import rotp.model.incidents.OathBreakerIncident;
import rotp.model.incidents.ParanoiaIncident;
import rotp.model.incidents.SignAllianceIncident;
import rotp.model.incidents.SignBreakAllianceIncident;
import rotp.model.incidents.SignDeclareWarIncident;
import rotp.model.incidents.SignPactIncident;
import rotp.model.incidents.SignPeaceIncident;
import rotp.model.incidents.SignTradeIncident;
import rotp.model.tech.Tech;
import rotp.ui.diplomacy.DialogueManager;
import rotp.ui.notifications.DiplomaticNotification;
import rotp.ui.notifications.GNNAllianceBrokenNotice;
import rotp.ui.notifications.GNNAllianceFormedNotice;
import rotp.ui.notifications.GNNAllyAtWarNotification;
import rotp.util.Base;

public class DiplomaticEmbassy implements Base, Serializable {
    private static final long serialVersionUID = 1L;
    public static final float MAX_ADJ_POWER = 10;

    public static final int TECH_DELAY = 1;
    public static final int TRADE_DELAY = 10;
    public static final int PEACE_DELAY = 10;
    public static final int PACT_DELAY = 20;
    public static final int ALLIANCE_DELAY = 30;
    public static final int JOINT_WAR_DELAY = 20;
    public static final int UNALLY_DELAY = 30;
    public static final int MAX_REQUESTS_TURN = 4;

    private final EmpireView view;
    private final Map<String, DiplomaticIncident> incidents = new HashMap<>();
    private HashMap<String, List<String>> offeredTechs = new HashMap<>();
    private transient List<DiplomaticIncident> newIncidents = new ArrayList<>();

    private boolean contact = false;
    private int contactYear = 0;
    private float treatyDate = -1;
    private boolean warFooting = false;
    // using 'casus belli' as variable name since using that word means I made a smart AI
    private String casusBelli;
    private DiplomaticIncident casusBelliInc;
    private DiplomaticTreaty treaty;

    private int[] timers = new int[20];
    private float relations = 0;
    private int peaceDuration = 0;
    private int tradeTimer = 0;
    private int lastRequestedTradeLevel = 0;
    private int tradeRefusalCount = 0;
    private int techTimer = 0;
    private int peaceTimer = 0;
    private int pactTimer = 0;
    private int allianceTimer = 0;
    private int jointWarTimer = 0;
    private int diplomatGoneTimer = 0;
    private int warningLevel = 0;
    private boolean tradePraised = false;
    private int currentMaxRequests = MAX_REQUESTS_TURN;
    private int requestCount = 0;
    private int minimumPraiseLevel = 0;
    private int minimumWarnLevel = 0;

    public Empire empire()                               { return view.empire(); }
    public Empire owner()                                { return view.owner(); }
    public float treatyDate()                            { return treatyDate; }
    public DiplomaticTreaty treaty()                     { return treaty; }
    public Collection<DiplomaticIncident> allIncidents() { return incidents.values(); }
    public int requestCount()                            { return requestCount; }
    public float relations()                             { return relations; }
    public boolean contact()                             { return contact; }
    public boolean onWarFooting()                        { return warFooting; }
    public void beginWarPreparations(String cb, DiplomaticIncident inc) {
        // dont replace an existing casus belli unless the new one is worse
        if (casusBelliInc != null) {
            if (casusBelliInc.moreSevere(inc))
                return;
        }
        warFooting = true;
        casusBelli = cb;
        casusBelliInc = inc;
    }
    public void endWarPreparations() {
        warFooting = false;
        casusBelli = null;
        casusBelliInc = null;
    }
    private void evaluateWarPreparations() {
        if (casusBelliInc == null)
            return;
        if (casusBelliInc.currentSeverity() == 0) 
            endWarPreparations();
    }
    public void contact(boolean b)                       { contact = b; }
    public List<DiplomaticIncident> newIncidents() {
        if (newIncidents == null)
            newIncidents = new ArrayList<>();
        return newIncidents;
    }
    private HashMap<String, List<String>> offeredTechs() {
        if (offeredTechs == null)
            offeredTechs = new HashMap<>();
        return offeredTechs;
    }
    public DiplomaticEmbassy(EmpireView v) {
        view = v;
        treaty = new TreatyNone(view.owner(), view.empire());
    }
    public float currentSpyIncidentSeverity() {
        float sev = 0;
        for (DiplomaticIncident inc: allIncidents()) {
            if (inc.isSpying())
                sev += inc.currentSeverity();
        }
        return max(-100,sev);
    }
    public void nextTurn(float prod) {
        evaluateWarPreparations();
        treaty.nextTurn(empire());
    }
    public boolean finalWar()               { return treaty.isFinalWar(); 	}
    public boolean war()                    { return treaty.isWar(); 	}
    public boolean noTreaty()               { return treaty.isNoTreaty(); }
    public boolean pact()                   { return treaty.isPact(); }
    public boolean alliance()               { return treaty.isAlliance(); }
    public boolean unity()                  { return treaty.isUnity(); }
    public boolean readyForTrade(int level) {
        // trade cooldown timer must be back to zero -AND-
        // new trade level must exceed last requested level by 25% * each consecutive refusal
        return (tradeTimer <= 0)
        && (level > (lastRequestedTradeLevel*(1+(tradeRefusalCount/4.0))));
    }
    public void resetTradeTimer(int level)  {
        if (empire().isPlayer())
            tradeTimer = 1;
        else {
            tradeTimer = TRADE_DELAY;
            lastRequestedTradeLevel = level;
        }
    }
    public void tradeRefused()              { tradeRefusalCount++; }
    public void tradeAccepted()             { tradeRefusalCount = 0; }
    public boolean alreadyOfferedTrade()    { return tradeTimer == TRADE_DELAY; }
    public boolean readyForTech()           { return techTimer <= 0; }
    public void resetTechTimer()            { techTimer = TECH_DELAY; }
    public boolean alreadyOfferedTech()     { return techTimer == TECH_DELAY; }
    public boolean readyForPeace()          { return peaceTimer <= 0; }
    public void resetPeaceTimer()           { resetPeaceTimer(1); }
    public void resetPeaceTimer(int mult)   { peaceTimer = mult*PEACE_DELAY; }
    public boolean alreadyOfferedPeace()    { return peaceTimer == PEACE_DELAY; }
    public boolean readyForPact()           { return pactTimer <= 0; }
    public void resetPactTimer()            { pactTimer = PACT_DELAY; }
    public boolean alreadyOfferedPact()     { return pactTimer == PACT_DELAY; }
    public boolean readyForAlliance()       { return allianceTimer <= 0; }
    public void resetAllianceTimer()        { allianceTimer = ALLIANCE_DELAY; }
    public boolean alreadyOfferedAlliance() { return allianceTimer == ALLIANCE_DELAY; }
    public boolean readyForJointWar()       { return jointWarTimer <= 0; }
    public void resetJointWarTimer()        { jointWarTimer = empire().isPlayer() ? JOINT_WAR_DELAY : 1; }
    public boolean alreadyOfferedJointWar() { return jointWarTimer == JOINT_WAR_DELAY; }
    public int minimumPraiseLevel()         { return max(10, minimumPraiseLevel); }
    public int minimumWarnLevel()           { return max(10, minimumWarnLevel); }
    public void praiseSent()                { minimumPraiseLevel = minimumPraiseLevel()+10;  }
    public void warningSent()               { minimumWarnLevel = minimumWarnLevel()+5;  }
    public void giveExpansionWarning()      { warningLevel = 1; }
    public boolean gaveExpansionWarning()   { return warningLevel > 0; }
    public void noteRequest() {
        if (requestCount == currentMaxRequests)
            currentMaxRequests--;
        requestCount++;
    }
    public boolean tooManyRequests()        { return requestCount > currentMaxRequests; }
    public float otherRelations()          { return otherEmbassy().relations(); }
    public int contactAge()                 { return (galaxy().currentYear() - contactYear); }
    public DiplomaticEmbassy otherEmbassy() { return view.otherView().embassy(); }
    public boolean tradePraised()           { return tradePraised; }
    public void tradePraised(boolean b)     { tradePraised = b; }
    public boolean encroaching(StarSystem sys) {
        for (DiplomaticIncident inc: allIncidents()) {
            if (inc instanceof EncroachmentIncident) {
                EncroachmentIncident enInc = (EncroachmentIncident) inc;
                if (enInc.sysId() == sys.id)
                    return true;
            }
        }
        return false;
    }
    public void logTechExchangeRequest(Tech wantedTech, List<Tech> counterTechs) {
        if (!offeredTechs().containsKey(wantedTech.id()))
            offeredTechs().put(wantedTech.id(), new ArrayList<>());

        List<String> list = offeredTechs().get(wantedTech.id());
        for (Tech t: counterTechs) {
            if (!list.contains(t.id()))
                list.add(t.id());
        }
    }
    public List<Tech> alreadyOfferedTechs(Tech wantedTech) {
        if (!offeredTechs().containsKey(wantedTech.id()))
            return null;

        List<Tech> techs = new ArrayList<>();
        for (String s: offeredTechs().get(wantedTech.id()))
            techs.add(tech(s));

        return techs;
    }
    private void withdrawAmbassador(int turns) {
        diplomatGoneTimer = turns;
    }
    public void withdrawAmbassador() {
        int baseTurns = 2;
        if (empire().leader().isDiplomat())
            baseTurns /= 2;
        else if (empire().leader().isXenophobic())
            baseTurns *= 2;

        if (finalWar())
            baseTurns = 9999;
        else if (war())
            baseTurns *= 2;
        withdrawAmbassador(baseTurns+1);
    }
    public void assessTurn() {
        log(view+" Embassy: assess turn");
        checkForIncidents();

        recalculateRelationsLevel();

        // player refusals are remembered for the 
        // entire duration to avoid the AI spamming the player
        // AI  refusals are completely reset after each turn
        // to allow players to continue asking once each turn
        // if they want
        if (view.owner().isPlayer()) {
            tradeTimer--;
            techTimer--;
            peaceTimer--;
            pactTimer--;
            allianceTimer--;
        }
        else {
            tradeTimer = 0;
            techTimer = 0;
            peaceTimer = 0;
            pactTimer = 0;
            allianceTimer = 0;

        }
        diplomatGoneTimer--;
        requestCount = 0;
        currentMaxRequests = Math.min(currentMaxRequests+1, MAX_REQUESTS_TURN);
        minimumPraiseLevel = min(20,minimumPraiseLevel);
        minimumWarnLevel = min(20, minimumWarnLevel);
        minimumPraiseLevel = minimumPraiseLevel() - 1;
        minimumWarnLevel = minimumWarnLevel() - 1;
    }
    public void recallAmbassador()     { diplomatGoneTimer = Integer.MAX_VALUE; }
    public void openEmbassy()          { diplomatGoneTimer = 0; }
    public boolean diplomatGone()      { return diplomatGoneTimer > 0;  }
    public boolean wantWar()           { return otherEmbassy().relations() < -50; }
    public boolean isAlly()            { return (alliance() || unity()); }
    public boolean alliedWithEnemy() {
        List<Empire> myEnemies = owner().enemies();
        List<Empire> hisAllies = empire().allies();
        for (Empire cv1 : myEnemies) {
            for (Empire cv2 : hisAllies) {
                if (cv1 == cv2)
                    return true;
            }
        }
        return false;
    }
    public boolean canAttackWithoutPenalty() { return anyWar() || noTreaty(); }
    public boolean canAttackWithoutPenalty(StarSystem s) {
        if (anyWar() || noTreaty())
            return true;
        if (pact())
            return s.hasColonyForEmpire(owner());
        return false;
    }
    public boolean peaceTreatyInEffect()   { return peaceDuration > 0; }
    private void setTreaty(DiplomaticTreaty tr) {
        treaty = tr;
        otherEmbassy().treaty = tr;
        view.setSuggestedAllocations();
        view.otherView().setSuggestedAllocations();
    }
    public boolean isFriend()    { return pact() || alliance() || unity(); }
    public boolean anyWar()      { return war() || finalWar(); }
    public boolean atPeace()     { return !anyWar() && peaceTreatyInEffect(); }

    public DiplomaticIncident exchangeTechnology(Tech offeredTech, Tech requestedTech) {
        // civ() is the requestor, and will be learning the requested tech
        // owner() is the requestee, who will be learning the counter-offered tech
        owner().tech().acquireTechThroughTrade(offeredTech.id, empire().id);
        empire().tech().acquireTechThroughTrade(requestedTech.id, owner().id);
        DiplomaticIncident inc = ExchangeTechnologyIncident.create(owner(), empire(), offeredTech, requestedTech);
        addIncident(inc);
        otherEmbassy().addIncident(ExchangeTechnologyIncident.create(empire(), owner(), requestedTech, offeredTech));
        return inc;
    }
    public DiplomaticIncident establishTradeTreaty(int level) {
        view.embassy().tradePraised(false);
        treatyDate = galaxy().currentTime();
        otherEmbassy().treatyDate = galaxy().currentTime();
        view.trade().startRoute(level);
        DiplomaticIncident inc = SignTradeIncident.create(owner(), empire(), level);
        addIncident(inc);
        otherEmbassy().addIncident(SignTradeIncident.create(empire(), owner(), level));
        return inc;
    }
    public void declareFinalWar() {
        beginFinalWar();
        otherEmbassy().beginFinalWar();
    }
    public void beginFinalWar() {
        treaty = new TreatyFinalWar(view.owner(), view.empire());
        view.trade().stopRoute();
        if (empire().isPlayer())
            galaxy().giveAdvice("MAIN_ADVISOR_RALLY_POINTS");
    }
    public DiplomaticIncident demandTribute() {
        DiplomaticIncident inc = DemandTributeIncident.create(owner(), empire(), true);
        addIncident(inc);
        otherEmbassy().addIncident(DemandTributeIncident.create(empire(), owner(), false));
        return inc;
    }
    public DiplomaticIncident declareJointWar() {
        // when we are declaring a war as a result of a joint war request, ignore
        // any existing casus belli. This ensures that a DeclareWarIncident is returned 
        // instead of some existing casus belli incident. This ensures that [other...]
        // tags are replaced properly in the war announcement to the player
        casusBelli = null;
        casusBelliInc = null;
        return declareWar();
    }
    public DiplomaticIncident declareWar() {
        endTreaty();
        int oathBreakType = 0;
        if (alliance())
            oathBreakType = 1;
        else if (pact())
            oathBreakType = 2;

        view.trade().stopRoute();

        // if we're not at war yet, inform player that war is upon him
        if (view.empire().isPlayer() && !anyWar()) {
           if (casusBelli == null)
                DiplomaticNotification.createAndNotify(view, DialogueManager.DECLARE_HATE_WAR);
            else
                DiplomaticNotification.createAndNotify(view, casusBelli);
        }

        setTreaty(new TreatyWar(view.owner(), view.empire()));
        resetPeaceTimer(3);
        withdrawAmbassador();
        otherEmbassy().withdrawAmbassador();

        // add war-causing incident to embassy
        DiplomaticIncident inc = casusBelliInc;
        if (inc == null) {
            if (casusBelli == null)
                inc = DeclareWarIncident.create(owner(), empire());
            else {
                switch(casusBelli) {
                    case DialogueManager.DECLARE_ERRATIC_WAR :
                        inc = ErraticWarIncident.create(owner(), empire()); break;
                    case DialogueManager.DECLARE_HATE_WAR:
                    default:
                        inc = DeclareWarIncident.create(owner(), empire());
                        oathBreakType = 0;
                        break;
                }
            }
        }
        otherEmbassy().addIncident(inc);

        // if oath broken, then create that incident as well
        switch(oathBreakType) {
            case 1:
                GNNAllianceBrokenNotice.create(owner(), empire());
                OathBreakerIncident.alertBrokenAlliance(owner(),empire()); break;
            case 2: OathBreakerIncident.alertBrokenPact(owner(),empire()); break;
        }
        
        // if the player is one of our allies, let him know
        for (Empire ally : owner().allies()) {
            if (ally.isPlayer())
                GNNAllyAtWarNotification.create(owner(), empire());
        }
        // if the player is one of our enemy's allies, let him know
        for (Empire ally : empire().allies()) {
            if (ally.isPlayer())
                GNNAllyAtWarNotification.create(empire(), owner());
        }
        
        if (empire().isPlayer())
            galaxy().giveAdvice("MAIN_ADVISOR_RALLY_POINTS", owner().raceName());
        else if  (owner().isPlayer())
            galaxy().giveAdvice("MAIN_ADVISOR_RALLY_POINTS", empire().raceName());

        return inc;
    }
    public DiplomaticIncident breakTrade() {
        view.trade().stopRoute();
        DiplomaticIncident inc = BreakTradeIncident.create(owner(), empire());
        otherEmbassy().addIncident(inc);
        return inc;
    }
    public DiplomaticIncident signPeace() {
        beginTreaty();
        int duration = roll(8,15);
        endWarPreparations();
        beginPeace(duration);
        otherEmbassy().beginPeace(duration);
        DiplomaticIncident inc = SignPeaceIncident.create(owner(), empire(), duration);
        addIncident(inc);
        otherEmbassy().addIncident(SignPeaceIncident.create(empire(), owner(), duration));
        return inc;
    }
    public DiplomaticIncident signPact() {
        beginTreaty();
        endWarPreparations();
        setTreaty(new TreatyPact(view.owner(), view.empire()));
        DiplomaticIncident inc = SignPactIncident.create(owner(), empire());
        addIncident(inc);
        otherEmbassy().addIncident(SignPactIncident.create(empire(), owner()));
        return inc;
    }
    public void reopenEmbassy() {
        diplomatGoneTimer = 0;
    }
    public void closeEmbassy() {
        withdrawAmbassador(Integer.MAX_VALUE);
    }
    public DiplomaticIncident breakPact() {
        endTreaty();
        setTreaty(new TreatyNone(view.owner(), view.empire()));
        DiplomaticIncident inc = BreakPactIncident.create(owner(), empire());
        otherEmbassy().addIncident(inc);
        OathBreakerIncident.alertBrokenPact(owner(),empire());
        return inc;
    }
    public DiplomaticIncident signAlliance() {
        beginTreaty();
        endWarPreparations();
        setTreaty(new TreatyAlliance(view.owner(), view.empire()));
        owner().setRecalcDistances();
        empire().setRecalcDistances();
        DiplomaticIncident inc = SignAllianceIncident.create(owner(), empire());
        addIncident(inc);
        otherEmbassy().addIncident(SignAllianceIncident.create(empire(), owner()));
        GNNAllianceFormedNotice.create(owner(), empire());
        return inc;
    }
    public DiplomaticIncident breakAlliance() {
        endTreaty();
        setTreaty(new TreatyNone(view.owner(), view.empire()));
        DiplomaticIncident inc = BreakAllianceIncident.create(owner(), empire());
        otherEmbassy().addIncident(inc);
        GNNAllianceBrokenNotice.create(owner(), empire());
        OathBreakerIncident.alertBrokenAlliance(owner(),empire());
        return inc;
    }
    public DiplomaticIncident signJointWar(Empire target) {
        EmpireView view2 = empire().viewForEmpire(target);
        view2.embassy().declareWar();
        DiplomaticIncident inc = SignDeclareWarIncident.create(owner(), empire(), target);
        addIncident(inc);
        otherEmbassy().addIncident(SignDeclareWarIncident.create(empire(), owner(), target));
        return inc;
    }
    public DiplomaticIncident signUnally(Empire target) {
        EmpireView view2 = empire().viewForEmpire(target);
        view2.embassy().breakAlliance();
        DiplomaticIncident inc = SignBreakAllianceIncident.create(owner(), empire(), target);
        addIncident(inc);
        otherEmbassy().addIncident(SignBreakAllianceIncident.create(empire(), owner(), target));
        return inc;
    }
    public void establishUnity() {
        beginTreaty();
        setContact();
        otherEmbassy().setContact();
        endWarPreparations();
        setTreaty(new TreatyUnity(view.owner(), view.empire()));
        owner().setRecalcDistances();
        empire().setRecalcDistances();
        owner().joinGalacticAlliance();
        empire().joinGalacticAlliance();
        
        // stop spying
        view.spies().activeSpies().clear();
        view.spies().maxSpies(0);
        view.spies().allocation(0);
    }
    public void setContact() {
        if (!contact()) {
            contactYear = galaxy().currentYear();
            contact(true);
            DiplomaticIncident inc = FirstContactIncident.create(owner(), empire());
            addIncident(inc);
            if (empire().isPlayer())
                galaxy().giveAdvice("MAIN_ADVISOR_DIPLOMACY", owner().raceName());
            else if (owner().isPlayer())
                galaxy().giveAdvice("MAIN_ADVISOR_DIPLOMACY", empire().raceName());
        }
    }
    public void makeFirstContact() {
        log("First Contact: ", owner().name(), " & ", empire().name());
        setContact();
        if (empire().isPlayer())
            DiplomaticNotification.create(view, owner().leader().dialogueContactType());
    }
    public void removeContact() {
        contact = false;
        resetTreaty();
        view.spies().beginHide();
        view.trade().stopRoute();
        if (otherEmbassy().contact)
            otherEmbassy().removeContact();
    }
    public void resetTreaty()   { setTreaty(new TreatyNone(view.owner(), view.empire())); }
    public void addIncident(DiplomaticIncident inc) {
        // add new incidents to current list
        // hash by incident key to filter out overlapping events
        String k = inc.key();
        log("addIncident key:"+k);
        DiplomaticIncident matchingEvent = incidents.get(k);
        log(view.toString(), ": Adding incident- ", inc.key(), ":", str(inc.currentSeverity()), ":", inc.toString());
        if (inc.moreSevere(matchingEvent))
            incidents.put(k,inc);
        recalculateRelationsLevel();
    }
    private void recalculateRelationsLevel() {
        float rel = owner().race().baseRelations(empire().race());
        rel += treatyRelationsAdj();
        for (DiplomaticIncident ev: incidents.values()) 
            rel += ev.currentSeverity();       
        relations = bounds(-100,rel,100);
    }
    private void checkForIncidents() {
        newIncidents().clear();
        clearForgottenIncidents();

        List<DiplomaticIncident> newEventsAll = new ArrayList<>();
        addIncident(ParanoiaIncident.create(view));
        owner().diplomatAI().noticeNoRelationIncident(view, newEventsAll);
        owner().diplomatAI().noticeAtWarWithAllyIncidents(view, newEventsAll);
        owner().diplomatAI().noticeAlliedWithEnemyIncidents(view, newEventsAll);
        owner().diplomatAI().noticeTrespassingIncidents(view, newEventsAll);
        owner().diplomatAI().noticeExpansionIncidents(view, newEventsAll);
        owner().diplomatAI().noticeBuildupIncidents(view, newEventsAll);

        for (DiplomaticIncident ev: newEventsAll)
            addIncident(ev);

        // make special list of incidents added in this turn
        for (DiplomaticIncident ev: incidents.values()) {
            if ((galaxy().currentYear() - ev.dateOccurred()) < 1)
                newIncidents().add(ev);
        }
    }
    private void clearForgottenIncidents() {
        List<String> keys = new ArrayList<>(incidents.keySet());
        for (String key: keys) {
            DiplomaticIncident inc = incidents.get(key);
            if (inc.isForgotten()) {
                log("Forgetting: ", incidents.get(key).toString());
                incidents.remove(key);
            }
        }
    }
    private int treatyRelationsAdj() {
        if (this.finalWar())
            return -200;
        else if (war())
            return -10;
        else if (pact())
            return 5;
        else if (alliance())
            return 10;
        else if (unity())
            return 200;
        else
            return 0;
    }
    private void beginTreaty() {
        treatyDate = galaxy().currentTime();
        otherEmbassy().treatyDate = galaxy().currentTime();
    }
    private void endTreaty() {
        treatyDate = -1;
        otherEmbassy().treatyDate = -1;
        owner().setRecalcDistances();
        empire().setRecalcDistances();
    }
    private void beginPeace(int duration) {
        beginTreaty();
        peaceDuration = duration;
        treaty = new TreatyNone(view.empire(), view.owner());
        view.setSuggestedAllocations();
    }
}