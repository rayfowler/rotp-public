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
package rotp.model.incidents;

import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
import rotp.ui.diplomacy.DialogueManager;

public class CouncilVoteIncident extends DiplomaticIncident {
    private static final long serialVersionUID = 1L;
    private static final int FOR = 1;
    private static final int ABSTAIN = 0;
    private static final int AGAINST = -1;
    final int empVotee; // who was voted for... could be null on abstention
    final int empVoter; // the voter getting the praise/warning
    final int empCandidate; // the candidate giving the praise/warning
    final int empRival; // the candidate's rival

    public static void create(EmpireView ev, Empire votee, Empire rival) {
        // don't care if he voted for himself
        if ((ev == null) || (ev.empire() == votee))
            return;
        ev.owner().diplomatAI().noticeIncident(new CouncilVoteIncident(ev, votee, rival), ev.empire());
    }
    private CouncilVoteIncident(EmpireView ev, Empire votee, Empire rival) {
        empVotee = votee == null ? Empire.NULL_ID : votee.id;
        empVoter = ev.empire().id;
        empCandidate = ev.owner().id;
        empRival = rival.id;
        severity = calculateSeverity();
        dateOccurred = galaxy().currentYear();
        duration = 10;
    }
    @Override
    public String title()               { return text("INC_COUNCIL_VOTE_TITLE"); }
    @Override
    public String description() {
        if (empCandidate == empVotee)
            return decode(text("INC_COUNCIL_VOTE_FOR_DESC"));
        else if (empVotee == Empire.NULL_ID)
            return decode(text("INC_COUNCIL_ABSTAIN_DESC"));
        else
            return decode(text("INC_COUNCIL_VOTE_AGAINST_DESC"));
    }
    private float calculateSeverity() {
        if (empVotee == empVoter)
            return 0;

        int type = AGAINST;
        if (empVotee == Empire.NULL_ID)
            type = ABSTAIN;
        else if (empVotee == empCandidate)
            type = FOR;

        Empire candidate = galaxy().empire(empCandidate);
        if (candidate.alliedWith(empVoter)) {
            switch(type) {
                case FOR: return 5;
                case ABSTAIN: return -5;
                case AGAINST: return -15;
            }
        }
        if (candidate.atWarWith(empVoter)) {
            switch(type) {
                case FOR: return 25;
                case ABSTAIN: return 0;
                case AGAINST: return -5;
            }
        }
        else {
            switch(type) {
                case FOR: return 25;
                case ABSTAIN: return 0;
                case AGAINST: return -15;
            }
        }
        return 0;
    }
    @Override
    public String praiseMessageId()   { return severity > 0 ? DialogueManager.PRAISE_COUNCIL_VOTE : ""; }
    @Override
    public String warningMessageId() {  return severity < 0 ? DialogueManager.WARNING_COUNCIL_VOTE : ""; }
    @Override
    public String key() {
        return concat(str(dateOccurred), ":CouncilVote");
    }
    @Override
    public String decode(String s) {
        String s1 = super.decode(s);
        s1 = galaxy().empire(empVoter).replaceTokens(s1, "voter");
        s1 = galaxy().empire(empCandidate).replaceTokens(s1, "candidate");
        s1 = galaxy().empire(empRival).replaceTokens(s1, "rival");
        s1 = galaxy().empire(empRival).replaceTokens(s1, "other");  // sometimes used instead of rival
        return s1;
    }
}
