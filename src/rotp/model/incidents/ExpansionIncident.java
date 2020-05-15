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

import rotp.model.empires.EmpireView;
import rotp.ui.diplomacy.DialogueManager;

public class ExpansionIncident extends DiplomaticIncident {
    private static final long serialVersionUID = 1L;
    int numSystems;
    float maxSystems;
    final int empYou;
    public static ExpansionIncident create(EmpireView ev, int num, float max) {
        return new ExpansionIncident(ev, num, max);
    }
    @Override
    public boolean triggeredByAction()   { return false; }
    private ExpansionIncident(EmpireView ev, int num, float max) {
        numSystems = num;
        maxSystems = max;
        empYou = ev.empire().id;
        dateOccurred = galaxy().currentYear();
        duration = 1;

        float multiplier = 1.0f;
        // penalty doubled for xenophobes
        if (ev.owner().leader().isXenophobic())
            multiplier *= 2;
        // allies are more tolerant of growth, NAPS less so
        if (!ev.owner().alliedWith(empYou))
            multiplier /= 3;
        else if (!ev.owner().pactWith(empYou))
            multiplier /= 1.5;
        
        // if you are bigger than average but the viewer is 
        // even larger, the penalty is lessened by the square
        // of the proportion... i.e. if you are 1/2 the size
        // the penalty is 1/4th
        int ownerNum = ev.owner().numColonizedSystems();
        if (ownerNum > numSystems) {
            float ratio = (float) numSystems / ownerNum;
            multiplier = multiplier * ratio * ratio;
        }
        
        float n = -10*((num*num/max/max) - 1);

        severity = multiplier*n;
    }
    @Override
    public String title()            { return text("INC_EXPANSION_TITLE"); }
    @Override
    public String description()      { return decode(text("INC_EXPANSION_DESC")); }
    @Override
    public String warningMessageId() { return  galaxy().empire(empYou).newSystems().isEmpty() ? "" :DialogueManager.WARNING_EXPANSION; }
    @Override
    public String key() {
        return concat("EmpireGrowth:", str(dateOccurred));
    }
    @Override
    public String decode(String s) {
        String s1 = super.decode(s);
        s1 = s1.replace("[num]", str(numSystems));
        s1 = galaxy().empire(empYou).replaceTokens(s1, "your");
        return s1;
    }
}
