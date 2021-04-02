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

import java.util.Set;

import rotp.model.empires.Empire;
import rotp.ui.diplomacy.DialogueManager;

public class OathBreakerIncident extends DiplomaticIncident {
    private static final long serialVersionUID = 1L;
    private static final int ALLIANCE_SEV = -30;
    private static final int PACT_SEV = -20;
    private final int oathBreakType;
    final int empBreaker;
    final int empVictim;
    public static void alertBrokenAlliance(Empire breaker, Empire victim) {
        alertBrokenAlliance(breaker,victim,null);
    }
    public static void alertBrokenAlliance(Empire breaker, Empire victim, Empire requestor) {
        Set<Empire> allContacts = Empire.allContacts(breaker, victim);
        allContacts.add(victim);
        for (Empire contact: allContacts) {
            if (contact != requestor) {
                OathBreakerIncident inc = new OathBreakerIncident(breaker, victim, contact, 1,ALLIANCE_SEV);
                contact.viewForEmpire(breaker).embassy().addIncident(inc);
            }
        }
    }
    public static void alertBrokenPact(Empire breaker, Empire victim) {
        alertBrokenPact(breaker,victim,null);
    }
    public static void alertBrokenPact(Empire breaker, Empire victim, Empire requestor) {
        Set<Empire> allContacts = Empire.allContacts(breaker, victim);
        allContacts.add(victim);
        for (Empire contact: allContacts) {
            if (contact != requestor) {
                OathBreakerIncident inc = new OathBreakerIncident(breaker, victim, contact, 2,PACT_SEV);
                contact.viewForEmpire(breaker).embassy().addIncident(inc);
            }
        }
    }
    private OathBreakerIncident(Empire brk, Empire vic, Empire obs,int type, int sev) {
        empBreaker = brk.id;
        empVictim = vic.id;
        oathBreakType = type;
        dateOccurred = galaxy().currentYear();
        
        duration = obs.diplomatAI().leaderOathBreakerDuration();
        
        // zero duration means zero severity. That's ruthless!
        if (duration == 0) {
            duration = 1; // avoid /0 errors
            severity = 0;
            return;
        }
        
        // longer duration for the victim of the oathbreaking
        if (vic == obs)
            duration *= 2;

        severity = max(-30,sev);
    }
    @Override
    public String title()        { return text("INC_OATHBREAKER_TITLE"); }
    @Override
    public String description()  {
        switch (oathBreakType) {
            case 1: return decode(text("INC_BROKE_ALLIANCE_DESC"));
            case 2: return decode(text("INC_BROKE_PACT_DESC"));
        }
        return "";
    }
    @Override
    public String warningMessageId() { return DialogueManager.WARNING_OATHBREAKER; }
    @Override
    public String key()              { return "Oath Break"; }
    @Override
    public String decode(String s) {
        String s1 = s.replace("[year]", str(dateOccurred));
        // this is a 3rd-party penalty... where "my" empire is upset that "your" empire broke a treaty with another empire (the victim)
        // this means that "my_empire" tag in the text needs to be replaced with the victim empire name
        s1 = galaxy().empire(empVictim).replaceTokens(s1, "my");
        s1 = galaxy().empire(empBreaker).replaceTokens(s1, "your");
        return s1;
    }
}
