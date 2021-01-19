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
import rotp.ui.diplomacy.DialogueManager;

public class ErraticWarIncident extends DiplomaticIncident {
    private static final long serialVersionUID = 1L;
    final int empAggressor;
    final int empVictim;
    public static ErraticWarIncident create(Empire e1, Empire e2) {
        ErraticWarIncident inc = new ErraticWarIncident(e1, e2);
        return inc;
    }
    private ErraticWarIncident(Empire e1, Empire e2) {
        severity = -50;
        empAggressor = e1.id;
        empVictim = e2.id;
        dateOccurred = galaxy().currentYear();
        duration = 10;
    }
    @Override
    public String title()            { return text("INC_ERRATIC_WAR_TITLE"); }
    @Override
    public String description()      { return decode(text("INC_ERRATIC_WAR_DESC")); }
    @Override
    public String key()              { return "Erratic War"; }
    @Override
    public String declareWarId()     { return DialogueManager.DECLARE_ERRATIC_WAR; }
    @Override
    public String decode(String s) {
        String s1 = super.decode(s);
        s1 = galaxy().empire(empAggressor).replaceTokens(s1, "aggressor");
        s1 = galaxy().empire(empVictim).replaceTokens(s1, "victim");
        return s1;
    }
}
