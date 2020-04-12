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
package rotp.model.game;

import java.util.ArrayList;
import java.util.List;

public enum RulesetManager {
    INSTANCE;
    public static RulesetManager current() { return INSTANCE; }
    private final List<IGameOptions> rulesets = new ArrayList<>();	
    private RulesetManager() {
        rulesets.add(new MOO1GameOptions());
    }
    public List<String> availableRulesets() {
        List<String> rules = new ArrayList<>();
        for (IGameOptions opts: rulesets) 
            rules.add(opts.name());
        return rules;
    }
    public IGameOptions ruleset(int i) {
        return rulesets.get(i);
    }
    public IGameOptions named(String s) {
        for (IGameOptions opts: rulesets) {
            if (opts.name().equals(s))
                return opts;
        }
        return rulesets.get(0);
    }
    public IGameOptions defaultRuleset() { return rulesets.get(0); }
}
