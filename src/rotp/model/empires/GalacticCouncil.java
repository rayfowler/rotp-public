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
import rotp.model.galaxy.Galaxy;
import rotp.model.galaxy.StarSystem;
import rotp.model.incidents.CouncilVoteIncident;
import rotp.model.incidents.FinalWarIncident;
import rotp.model.tech.Tech;
import rotp.ui.diplomacy.DialogueManager;
import rotp.ui.notifications.CouncilVoteNotification;
import rotp.ui.notifications.DiplomaticNotification;
import rotp.ui.notifications.GNNNotification;
import rotp.util.Base;

public class GalacticCouncil implements Base, Serializable {
    private static final long serialVersionUID = 1L;
    private static final int CHECK = 0;
    private static final int SCHEDULE = 1;
    private static final int CONVENE = 2;

    private static final int INACTIVE = 0;
    private static final int ACTIVE = 1;
    private static final int DISBANDED = 2;

    private static final int noticeDuration = 5;
    private static final int interval = 20;
    
    private static final float PCT_REQUIRED = 0.667f;

    private int nextAction = CHECK;
    private int currentStatus = INACTIVE;
    private int actionCountdown = 1;
    private Empire leader;
    private final List<Empire> rebels = new ArrayList<>();
    private final List<Empire> allies = new ArrayList<>();

    //convention variables - reset when convention starts
    private transient List<Empire> empires;
    private transient int voteIndex = 0;
    private transient int[] votes;
    private transient int totalVotes, votes1, votes2, lastVotes;
    private transient Empire candidate1, candidate2, lastVoter, lastVoted;

    public Empire leader()             { return leader; }
    public void leader(Empire e)       { leader = e; }
    public List<Empire>  allies()      { return allies; }
    public boolean finalWar()          { return !rebels.isEmpty(); }
    public void addAlly(Empire e)      { allies.add(e); }
    public void addRebel(Empire e)     { rebels.add(e); }
    public boolean isAllied(Empire e)  { return allies.contains(e); }

    public List<Empire> empires() {
        if (empires == null) 
            initEmpires();
        return empires;
    }
    public void init() {
        if (galaxy().numActiveEmpires() > 2)
            nextAction = CHECK;
    }
    public void checkIfDisband() {
        int num = galaxy().numActiveEmpires();
        if (active() &&  (num < 3)) {
            if (num == 2)
                GNNNotification.notifyCouncil(text("GNN_END_COUNCIL"));
            end();
        }
    }

    public void nextTurn() {
        if (galaxy().numActiveEmpires() < 3)
            return;
        if (disbanded())
            return;

        actionCountdown--;
        if (actionCountdown > 0)
            return;

        switch (nextAction) {
            case CHECK:    checkFormation(); break;
            case SCHEDULE: schedule(); break;
            case CONVENE:  convene(); break;
        }
    }
    public boolean inactive()         { return currentStatus == INACTIVE; }
    public boolean active()           { return currentStatus == ACTIVE; }
    public boolean disbanded()        { return currentStatus == DISBANDED; }
    private void checkFormation() {
        Galaxy gal = galaxy();
        int limit = (int) Math.ceil(gal.numStarSystems()*PCT_REQUIRED);
        float colonized = 0;
        for (int i=0; i<gal.numStarSystems(); i++) {
            StarSystem sys = gal.system(i);
            if (sys.isColonized())
                colonized++;
        }
        if (colonized >= limit) {
            currentStatus = ACTIVE;
            schedule();
        }
    }
    private void schedule() {
        galaxy().giveAdvice("MAIN_ADVISOR_COUNCIL");
        GNNNotification.notifyCouncil(text("GNN_FORM_COUNCIL"));
        nextAction = CONVENE;
        actionCountdown = noticeDuration;
    }
    private void convene() {
        openConvention();
        CouncilVoteNotification.create();
    }
    public boolean votingInProgress()  { return voteIndex < empires.size(); }
    public boolean hasVoted(Empire e)  { return empires.indexOf(e) < voteIndex; }
    public int votes(Empire e)         { return votes[empires.indexOf(e)]; }
    public Empire nextVoter()  { return empires.get(voteIndex); }
    public Empire candidate1() { return candidate1; }
    public Empire candidate2() { return candidate2; }
    public Empire lastVoter()  { return lastVoter; }
    public Empire lastVoted()  { return lastVoted; }
    public int totalVotes()    { return totalVotes; }
    public int votesToElect()  { return (int) Math.ceil(totalVotes * 2 / 3.0); }
    public int votes1()        { return votes1; }
    public int votes2()        { return votes2; }
    public int lastVotes()     { return lastVotes; }
    public int nextVotes()     { return votes[voteIndex]; }
    public boolean hasLeader() { return leader != null; }
    public void castNextVote() {
        // will not cast vote for player
        if (!nextVoter().isPlayer())
            castNextVote(nextVoter().diplomatAI().councilVoteFor(candidate1(), candidate2()));
    }
    public void castPlayerVote(Empire chosen) {
        if (nextVoter().isPlayer())
            castNextVote(chosen);
    }
    public boolean nextVoteWouldElect(Empire emp) {
        if ((emp != candidate1())
        && (emp != candidate2()))
            return false;
        int votesAlreadyCast = emp == candidate1() ? votes1() : votes2();
        return (nextVotes() + votesAlreadyCast) >= votesToElect();
    }
    private void castNextVote(Empire chosen) {
        lastVoter = empires.get(voteIndex);
        lastVotes = votes[voteIndex];

        if (chosen == candidate1) {
            lastVoter.lastCouncilVoteEmpId(id(candidate1));
            votes1 += lastVotes;
            lastVoted = candidate1;
        }
        else if (chosen == candidate2) {
            lastVoter.lastCouncilVoteEmpId(id(candidate2));
            votes2 += lastVotes;
            lastVoted = candidate2;
        }
        else {
            lastVoter.lastCouncilVoteEmpId(Empire.ABSTAIN_ID);
            lastVoted = null;
        }

        voteIndex++;
        if (!votingInProgress())
            closeConvention();
    }
    private void end() {
        currentStatus = DISBANDED;
        if (leader == null)
            return;
        
        boolean playerWasAllied = player().alliedWith(leader.id);

        boolean electedLeaderIsCrazy = rebels.contains(leader);
        if (electedLeaderIsCrazy) {
            Empire crazyEmpire = leader;
            if (crazyEmpire == candidate1)
                leader = candidate2;
            else
                leader = candidate1;
            allies.addAll(rebels);
            allies.remove(crazyEmpire);
            rebels.clear();
            rebels.add(crazyEmpire);
        }
        
        // if player won the vote and no rebels, game over
        if (leader.isPlayer() && rebels.isEmpty()) {
            session().status().winDiplomatic();
            return;
        }
        // if player accepted ruling, also game over
        else if (!leader.isPlayer() && allies.contains(player())) {
            if (playerWasAllied)
                session().status().winCouncilAlliance();
            else
                session().status().loseDiplomatic();
            return;
        }

        // final war: player is rebelling aginst leader, or player
        // is leader and at least one AI is rebelling
        if (leader.isPlayer())
            galaxy().giveAdvice("MAIN_ADVISOR_COUNCIL_RESISTED", leader.raceName());                   
        else 
            galaxy().giveAdvice("MAIN_ADVISOR_RESIST_COUNCIL");

        // all members of alliance declare final war on player and all rebels
        // everyone gets the incident first. Once Final War is declared, no
        // more incidents are checked
        for (Empire rebel: rebels) {
            for (Empire ally: allies) {
                FinalWarIncident.create(ally, leader, rebel);
            }
        }
        
        // all members of alliance declare final war on player and all rebels
        for (Empire rebel: rebels) {
            for (Empire ally: allies) {
                ally.viewForEmpire(rebel).embassy().declareFinalWar();
            }
        }
        
        // all mmembers of alliance establish unity with each other
        // this ensures no spying costs and all learned techs traded freely
        for (Empire ally1: allies) {
            for (Empire ally2: allies) {
                if (ally1 != ally2) {
                    EmpireView v = ally1.viewForEmpire(ally2);
                    v.embassy().establishUnity();
                }
            }
        }
        // all members of alliance share techs with leader
        for (Empire ally: allies) {
            for (Tech tech : ally.tech().techsUnknownTo(leader))
                leader.tech().acquireTechThroughTrade(tech.id, ally.id);
        }
        // leader then shares all techs with allies
        for (Empire ally: allies) {
            for (Tech tech : leader.tech().techsUnknownTo(ally))
                ally.tech().acquireTechThroughTrade(tech.id, leader.id);
        }
        if (leader.isPlayer()) {
            for (Empire rebel: rebels) {
                EmpireView v = rebel.viewForEmpire(leader);
                DiplomaticNotification.create(v, DialogueManager.WARNING_REBELLING_AGAINST); 
            }
        }
        else {
            for (Empire rebel: rebels) {
                if (!rebel.isPlayer()) {
                    EmpireView v = rebel.viewForEmpire(leader);
                    DiplomaticNotification.create(v, DialogueManager.PRAISE_REBELLING_WITH); 
                }
            }
        }
    }
    private void openConvention() {
        initConventionVars();

        // calculate vote total for each empire
        for (int i = 0; i <empires.size(); i++) {
            Empire voter = empires.get(i);
            votes[i] = (int) Math.ceil(voter.totalPlanetaryPopulation() / 100);
            totalVotes += votes[i];
        }

        log("Convening council. # empires: " + empires.size());
    }
    private void initEmpires() {
        empires = galaxy().activeEmpires();
        Collections.sort(empires, Empire.TOTAL_POPULATION);
    }
    private void initConventionVars() {
        initEmpires();
        votes = new int[empires().size()];
        votes1 = 0;
        votes2 = 0;
        candidate1 = empires.get(0);
        candidate2 = empires.get(1);
        voteIndex = 0;
        totalVotes = 0;
    }
    private void closeConvention() {
        // determine leader
        int minVotes = this.votesToElect();
        leader = null;

        if (votes1 >= minVotes)
            leader = candidate1;
        else if (votes2 >= minVotes)
            leader = candidate2;

        List<Empire> allVoters = new ArrayList<>(empires);
        // if leader is elected, ask all empires to accept ruling
        if (leader != null) {
            rebels.addAll(allVoters);
            for (Empire c : allVoters)
                c.diplomatAI().acceptCouncilRuling(this);
            return;
        }
        else 
            ensureFullContact();


        // create incidents between voters and the candidates
        for (Empire voter: empires) {
            Empire lastVote = galaxy().empire(voter.lastCouncilVoteEmpId());
            CouncilVoteIncident.create(candidate1.viewForEmpire(voter), lastVote, candidate2);
            CouncilVoteIncident.create(candidate2.viewForEmpire(voter), lastVote, candidate1);
        }

        // schedule next council
        nextAction = SCHEDULE;
        actionCountdown = interval;
    }
    public void defyRuling(Empire e) {
        empires.remove(e);
        if (empires.isEmpty())
            end();
    }
    public void acceptRuling(Empire e) {
        empires.remove(e);
        rebels.remove(e);
        allies.add(e);
        if (empires.isEmpty())
            end();
    }

    public void removeEmpire(Empire deadEmpire) {
        allies.remove(deadEmpire);
        rebels.remove(deadEmpire);
        
        if (deadEmpire.isPlayer()) {
            if (leader().isPlayer())
                // player was leader of alliance and still lost!
                session().status().loseNewRepublic();
            else
                // player was a rebel against alliance and lost
                session().status().loseRebellion();
        }
        else {
            if (rebels.isEmpty() && leader().isPlayer())
                // rebellion has been defeated
                session().status().winNewRepublic();
            else if (allies.isEmpty() && !leader().isPlayer()) {
                // New Republic has been defeated
                if (rebels.size() == 1) 
                    session().status().winRebellion();
                else
                    session().status().winRebellionAlliance();
            }               
        }
    }
    private void ensureFullContact() {
        List<Empire> emps = new ArrayList<>(galaxy().activeEmpires());
        for (Empire emp1: galaxy().activeEmpires()) {
            emps.remove(emp1);
            for (Empire emp2: emps)
                emp1.makeContact(emp2);
        }
    }
}