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

public class EvictedSpiesIncident extends DiplomaticIncident {
    private static final long serialVersionUID = 1L;
    final int empBreaker;
    final int empMe;
    public static EvictedSpiesIncident create(EmpireView ev) {
        EvictedSpiesIncident inc = new EvictedSpiesIncident(ev.empire(), ev.owner());
        return inc;
    }
    private EvictedSpiesIncident(Empire e1, Empire e2) {
        empBreaker = e1.id;
        empMe = e2.id;
        severity = -10;
        dateOccurred = galaxy().currentYear();
        duration = 10;
    }
    @Override
    public String title()            { return text("INC_EVICTED_SPIES_TITLE"); }
    @Override
    public String description()      { return  decode(text("INC_EVICTED_SPIES_DESC")); }
    @Override
    public String key()              { return "Evicted Spies"; }
    @Override
    public float currentSeverity()   { return severity; } // does not degrade over time
    @Override
    public String decode(String s) {
        String s1 = super.decode(s);
        s1 = galaxy().empire(empBreaker).replaceTokens(s1, "your");
        s1 = galaxy().empire(empMe).replaceTokens(s1, "my");
        return s1;
    }
}
