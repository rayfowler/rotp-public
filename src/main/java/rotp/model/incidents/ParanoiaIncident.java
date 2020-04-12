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

public class ParanoiaIncident extends DiplomaticIncident {
    private static final long serialVersionUID = 1L;
    final int empMe;
    final int empYou;
    public static ParanoiaIncident create(EmpireView ev) {
        return new ParanoiaIncident(ev);
    }
    private ParanoiaIncident(EmpireView ev) {
        empMe = ev.owner().id;
        empYou = ev.empire().id;
        int num = ev.empire().numColonizedSystems();
        int all = galaxy().numColonizedSystems();  // all colonies in galaxy
        int aliens = galaxy().numEmpires() - 1;
        dateOccurred = galaxy().currentYear();
        duration = 1;

        float multiplier = 1.0f;
        if (ev.owner().leader().isXenophobic())
            multiplier *= 1.5;
        if (ev.owner().leader().isDiplomat())
            multiplier /= 1.5;
        
        // if not allied, increase denominator
        if (!ev.owner().alliedWith(empYou))
            multiplier /= 2;
        else if (ev.owner().pactWith(empYou))
            multiplier /= 1.5;
        else if (ev.owner().atWarWith(empYou))
            multiplier *= 2;

        float n = -50.0f*num/all/(aliens*aliens);

        severity = multiplier*n;
        
        // no listing if too close to zero
        if (severity > -0.2)
            severity = 0;
    }
    @Override
    public String title()            { return text("INC_PARANOIA_TITLE"); }
    @Override
    public boolean triggeredByAction()   { return false; }
    @Override
    public String description()      { 
        if (severity <= -50)
            return decode(text("INC_PARANOIA_DESC5"));
        if (severity <= -17)
            return decode(text("INC_PARANOIA_DESC4"));
        else if (severity <= -6)
            return decode(text("INC_PARANOIA_DESC3"));
        else if (severity <= -2)
            return decode(text("INC_PARANOIA_DESC2"));
        else 
            return decode(text("INC_PARANOIA_DESC1"));
    }
    @Override
    public String key() {
        return concat("Paranoia:", str(dateOccurred));
    }
    @Override
    public String decode(String s) {
        String s1 = super.decode(s);
        s1 = galaxy().empire(empMe).replaceTokens(s1, "my");
        s1 = galaxy().empire(empYou).replaceTokens(s1, "your");
        return s1;
    }
}
