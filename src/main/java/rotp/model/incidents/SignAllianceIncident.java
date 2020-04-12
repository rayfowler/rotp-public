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

public class SignAllianceIncident extends DiplomaticIncident {
    private static final long serialVersionUID = 1L;
    final int empMe;
    final int empYou;
    public static SignAllianceIncident create(Empire e1, Empire e2) {
        SignAllianceIncident inc = new SignAllianceIncident(e1, e2);
        return inc;
    }
    private SignAllianceIncident(Empire e1, Empire e2) {
        empMe = e1.id;
        empYou = e2.id;
        severity = 5;

        dateOccurred = galaxy().currentYear();
        duration = 10;
    }
    @Override
    public String title()         { return text("INC_SIGNED_ALLIANCE_TITLE"); }
    @Override
    public String key()           { return "Sign Alliance"; }
    @Override
    public String description()   { return decode(text("INC_SIGNED_ALLIANCE_DESC")); }
    @Override
    public String decode(String s) {
        String s1 = super.decode(s);
        s1 = galaxy().empire(empMe).replaceTokens(s1, "my");
        s1 = galaxy().empire(empYou).replaceTokens(s1, "your");
        return s1;
    }
}
