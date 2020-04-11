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

public class KillMonsterIncident extends DiplomaticIncident {
    private static final long serialVersionUID = 1L;
    final int empMe;
    final int empYou;
    String monsterKey;
    public static KillMonsterIncident create(int e1, int e2, String key) {
        KillMonsterIncident inc = new KillMonsterIncident(e1, e2, key);
        return inc;
    }
    private KillMonsterIncident(int e1, int e2, String key) {
        empMe = e1;
        empYou = e2;
        monsterKey = key;
        severity = 50;

        dateOccurred = galaxy().currentYear();
        duration = 30;
    }
    @Override
    public String title()         { return text("INC_KILLED_MONSTER_TITLE", text(monsterKey)); }
    @Override
    public String key()           { return "Killed Monster: "+monsterKey; }
    @Override
    public String description()   { return decode(text("INC_KILLED_MONSTER_DESC")); }
    @Override
    public String decode(String s) {
        String s1 = super.decode(s);
        s1 = galaxy().empire(empMe).replaceTokens(s1, "my");
        s1 = galaxy().empire(empYou).replaceTokens(s1, "your");
        s1 = s1.replace("[monster]", text(monsterKey));
        return s1;
    }
}
