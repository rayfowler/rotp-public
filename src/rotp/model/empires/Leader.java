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
import rotp.ui.diplomacy.DialogueManager;
import rotp.util.Base;

public class Leader implements Base, Serializable {
    private static final long serialVersionUID = 1L;
    public enum Personality  {
        ERRATIC("LEADER_ERRATIC"),
        PACIFIST("LEADER_PACIFIST"),
        HONORABLE("LEADER_HONORABLE"),
        RUTHLESS("LEADER_RUTHLESS"),
        AGGRESSIVE("LEADER_AGGRESSIVE"),
        XENOPHOBIC("LEADER_XENOPHOBIC");
        private final String label;
        Personality(String s) { label = s; }
        @Override
        public String toString() { return label; }
    }
    public enum Objective {
        MILITARIST("LEADER_MILITARIST"),
        ECOLOGIST("LEADER_ECOLOGIST"),
        DIPLOMAT("LEADER_DIPLOMAT"),
        INDUSTRIALIST("LEADER_INDUSTRIALIST"),
        EXPANSIONIST("LEADER_EXPANSIONIST"),
        TECHNOLOGIST("LEADER_TECHNOLOGIST");
        private final String label;
        Objective(String s) { label = s; }
        @Override
        public String toString() { return label; }
    }
    private String name;
    public Personality personality = Personality.ERRATIC;
    public Objective objective = Objective.MILITARIST;
    private Empire empire;

    public String name()      { return name; }
    public Leader(Empire c) {
        this(c, c.race().randomLeaderName());
    }
    public Leader(Empire c, String s) {
        empire = c;
        name = s;
        if (!c.isPlayerControlled() && options().randomizeAIPersonality()) {
            personality = random(Personality.values());
            objective = random(Objective.values());
        }
        else {
            personality = Personality.values()[empire.race().randomLeaderAttitude()];
            objective = Objective.values()[empire.race().randomLeaderObjective()];
        }
    }
    public String objective()   { return text(objective.label); }
    public String personality() { return text(personality.label); }

    public boolean isErratic()     { return (personality == Personality.ERRATIC); }
    public boolean isPacifist()    { return (personality == Personality.PACIFIST); }
    public boolean isHonorable()   { return (personality == Personality.HONORABLE); }
    public boolean isAggressive()  { return (personality == Personality.AGGRESSIVE); }
    public boolean isRuthless()    { return (personality == Personality.RUTHLESS); }
    public boolean isXenophobic()  { return (personality == Personality.XENOPHOBIC); }

    public boolean isDiplomat()     { return (objective == Objective.DIPLOMAT); }
    public boolean isMilitarist()   { return (objective == Objective.MILITARIST); }
    public boolean isEcologist()    { return (objective == Objective.ECOLOGIST); }
    public boolean isIndustrialist(){ return (objective == Objective.INDUSTRIALIST); }
    public boolean isExpansionist() { return (objective == Objective.EXPANSIONIST); }
    public boolean isTechnologist() { return (objective == Objective.TECHNOLOGIST); }

    public String dialogueContactType() {
        Personality p = personality;
        
        // when using the random personality option, use the most common attitude for  
        // contact dialogue since there is not text for every race/attitude combination
        if (options().randomizeAIPersonality())
            p = Personality.values()[empire.race().mostCommonLeaderAttitude()];

        switch(p) {
            case PACIFIST:   return DialogueManager.CONTACT_PACIFIST;
            case HONORABLE:  return DialogueManager.CONTACT_HONORABLE;
            case RUTHLESS:   return DialogueManager.CONTACT_RUTHLESS;
            case AGGRESSIVE: return DialogueManager.CONTACT_AGGRESSIVE;
            case XENOPHOBIC: return DialogueManager.CONTACT_XENOPHOBIC;
            case ERRATIC:    return DialogueManager.CONTACT_ERRATIC;
            default:         return DialogueManager.CONTACT_ERRATIC;
        }
    }
    public float exploitWeakerEmpiresRatio() {
        float ratio = 1.0f;
        if (isAggressive())
            ratio /= 2;
        if (isMilitarist())
            ratio /= 1.5;
        if (isHonorable())
            ratio *= 2;
        if (isPacifist())
            ratio *= 2;
        if (isXenophobic())
            ratio *= 1.5;
        if (isExpansionist())
            ratio /= 2;
        return ratio;
    }
    public float retreatRatio(Empire c) {
        float baseRatio = 1.5f;

        if (empire.alliedWith(id(c))) {
            if (isHonorable())
                baseRatio *= 4;
            else if (!isRuthless())
                baseRatio *= 2;
        }
        if (isAggressive())
            baseRatio *= 2;
        else if (isPacifist())
            baseRatio /= 2;

        return baseRatio;
    }
    public float contemptDeclareWarMod(Empire e) {
        float power = empire.viewForEmpire(e).empirePower();
        // e's power is > than this empire's power, so decrease chance for declaration
        if (power >= 1)
            return (power-1)*-20;
        else
            return ((1/power)-1)*20;
    }
    public float contemptAcceptPeaceMod(Empire e) {
        float power = sqrt(empire.viewForEmpire(e).empirePower());
        if (power >= 1)
            return (power-1)*30;
        else
            return ((1/power)-1)*-20;
    }
    public int genocideDurationMod() {
        switch(personality) {
            case PACIFIST:   return 999999;
            case HONORABLE:  return 50;
            case XENOPHOBIC: return 100;
            case RUTHLESS:   return 10;
            case AGGRESSIVE: return 0;
            case ERRATIC:    return 25;
            default:         return 25;
        }
    }
    public float bioweaponMod() {
        float objMod = 1.0f;
        switch(objective) {
            case ECOLOGIST:  objMod = 2.0f;
        }
        switch(personality) {
            case PACIFIST:   return objMod*1;
            case HONORABLE:  return objMod*1.5f;
            case XENOPHOBIC: return objMod*0.5f;
            case RUTHLESS:   return 0;
            case AGGRESSIVE: return objMod*0.5f;
            case ERRATIC:    return objMod*0.5f;
            default:         return objMod*1;
        }
    }
    public int oathBreakerDuration() {
        int objMod = 1;
        switch(objective) {
            case DIPLOMAT:  objMod = 2;
        }
        switch(personality) {
            case PACIFIST:   return objMod*50;
            case HONORABLE:  return 999999;
            case XENOPHOBIC: return objMod*50;
            case RUTHLESS:   return 0;
            case AGGRESSIVE: return objMod*50;
            case ERRATIC:    return objMod*25;
            default:         return objMod*1;
        }
    }
    public float diplomacyAnnoyanceMod(EmpireView v) {
        // # of requests past the initial
        int addl = max(0, v.embassy().requestCount()-1);
        switch(personality) {
            case XENOPHOBIC: return -20*addl;
            case ERRATIC:    return -10*addl;
            case PACIFIST:   return -10*addl;
            case HONORABLE:  return -10*addl;
            case RUTHLESS:   return -10*addl;
            case AGGRESSIVE: return -10*addl;
            default:         return -10*addl;
        }
    }
    public float declareWarMod() {
        int a, b;
        switch(personality) {
            case PACIFIST:      a = -20; break;
            case HONORABLE:     a = 0; break;
            case XENOPHOBIC:    a = -10; break;
            case RUTHLESS:      a = 10; break;
            case AGGRESSIVE:    a = 20; break;
            case ERRATIC:       a = 10*roll(-2,2); break;
            default:            a = 0; break;
        }
        switch(objective) {
            case DIPLOMAT:      b = -20; break;
            case MILITARIST:    b = 20; break;
            case ECOLOGIST:     b = 0; break;
            case INDUSTRIALIST: b = 0; break;
            case EXPANSIONIST:  b = 10; break;
            case TECHNOLOGIST:  b = -10; break;
            default:            b = 0; break;
        }        
        return a+b;
    }
    public float acceptPeaceTreatyMod() {
        int a, b;
        switch(personality) {
            case PACIFIST:      a = 10; break;
            case HONORABLE:     a = 0; break;
            case XENOPHOBIC:    a = 5; break;
            case RUTHLESS:      a = -5; break;
            case AGGRESSIVE:    a = -10; break;
            case ERRATIC:       a = 0; break;
            default:            a = 0; break;
        }
        switch(objective) {
            case DIPLOMAT:      b = 10; break;
            case MILITARIST:    b = -10; break;
            case ECOLOGIST:     b = 0; break;
            case INDUSTRIALIST: b = 0; break;
            case EXPANSIONIST:  b = -5; break;
            case TECHNOLOGIST:  b = 5; break;
            default:            b = 0; break;
        }        
        return a+b;
    }
    public float acceptPactMod(Empire other) {
        int a, b, c;
        switch(personality) {
            case PACIFIST:      a = 20; break;
            case HONORABLE:     a = 0; break;
            case XENOPHOBIC:    a = 10; break;
            case RUTHLESS:      a = -10; break;
            case AGGRESSIVE:    a = -20; break;
            case ERRATIC:       a = 0; break;
            default:            a = 0; break;
        }
        switch(objective) {
            case DIPLOMAT:      b = 10; break;
            case MILITARIST:    b = -10; break;
            case ECOLOGIST:     b = 0; break;
            case INDUSTRIALIST: b = 5; break;
            case EXPANSIONIST:  b = -5; break;
            case TECHNOLOGIST:  b = 0; break;
            default:            b = 0; break;
        }     
        c = 10*affinityMod(personality, other.leader().personality);
        return a+b+c;
    }
    public float acceptAllianceMod(Empire other) {
        int a, b, c;
        switch(personality) {
            case PACIFIST:      a = 0; break;
            case HONORABLE:     a = 0; break;
            case XENOPHOBIC:    a = -20; break;
            case RUTHLESS:      a = -10; break;
            case AGGRESSIVE:    a = 0; break;
            case ERRATIC:       a = 0; break;
            default:            a = 0; break;
        }
        switch(objective) {
            case DIPLOMAT:      b = 10; break;
            case MILITARIST:    b = 10; break;
            case ECOLOGIST:     b = 0; break;
            case INDUSTRIALIST: b = 10; break;
            case EXPANSIONIST:  b = 10; break;
            case TECHNOLOGIST:  b = 0; break;
            default:            b = 0; break;
        }        
        c = 10*affinityMod(personality, other.leader().personality);
        return a+b+c;
    }
    public float acceptTradeMod() {
        int a, b;
        switch(personality) {
            case PACIFIST:      a = 0; break;
            case HONORABLE:     a = 0; break;
            case XENOPHOBIC:    a = -20; break;
            case RUTHLESS:      a = 0; break;
            case AGGRESSIVE:    a = 0; break;
            case ERRATIC:       a = 0; break;
            default:            a = 0; break;
        }
        switch(objective) {
            case DIPLOMAT:      b = 0; break;
            case MILITARIST:    b = 0; break;
            case ECOLOGIST:     b = 0; break;
            case INDUSTRIALIST: b = 10; break;
            case EXPANSIONIST:  b = 0; break;
            case TECHNOLOGIST:  b = 0; break;
            default:            b = 0; break;
        }        
        return a+b;
    }
    public float hateWarThreshold() {
        switch(personality) {
            case HONORABLE:  return -80;
            case PACIFIST:   return -90;
            case AGGRESSIVE: return -70;
            case RUTHLESS:   return -80;
            case XENOPHOBIC: return -80;
            case ERRATIC:    return -80;
            default:         return -80;
        }
    }
    public float acceptJointWarMod() {
        int a, b;
        switch(personality) {
            case PACIFIST:      a = -20; break;
            case HONORABLE:     a = 0; break;
            case XENOPHOBIC:    a = -10; break;
            case RUTHLESS:      a = 10; break;
            case AGGRESSIVE:    a = 20; break;
            case ERRATIC:       a = 0; break;
            default:            a = 0; break;
        }
        switch(objective) {
            case DIPLOMAT:      b = -20; break;
            case MILITARIST:    b = 20; break;
            case ECOLOGIST:     b = 0; break;
            case INDUSTRIALIST: b = 0; break;
            case EXPANSIONIST:  b = 10; break;
            case TECHNOLOGIST:  b = -10; break;
            default:            b = 0; break;
        }        
        return a+b;
    }
    public float preserveTreatyMod() {
        int a, b;
        switch(personality) {
            case PACIFIST:      a = 0; break;
            case HONORABLE:     a = 40; break;
            case XENOPHOBIC:    a = 0; break;
            case RUTHLESS:      a = 0; break;
            case AGGRESSIVE:    a = 0; break;
            case ERRATIC:       a = 0; break;
            default:            a = 0; break;
        }
        switch(objective) {
            case DIPLOMAT:      b = 20; break;
            case MILITARIST:    b = 0; break;
            case ECOLOGIST:     b = 0; break;
            case INDUSTRIALIST: b = 0; break;
            case EXPANSIONIST:  b = 0; break;
            case TECHNOLOGIST:  b = 0; break;
            default:            b = 0; break;
        }        
        return a+b;
    }
    public int affinityMod(Personality p1, Personality p2) {
        int love = 2;
        int like = 1;
        int neutral = 0;
        int dislike = -1;
                
        switch(p1) {
            case PACIFIST:
                switch(p2) {
                    case PACIFIST:      return love;
                    case HONORABLE:     return like;
                    case XENOPHOBIC:    return like;
                    case RUTHLESS:      return dislike;
                    case AGGRESSIVE:    return dislike;
                    default:            return neutral;
                }
            case HONORABLE:
                switch(p2) {
                    case PACIFIST:      return like;
                    case HONORABLE:     return love;
                    case XENOPHOBIC:    return dislike;
                    case RUTHLESS:      return dislike;
                    case AGGRESSIVE:    return like;
                    default:            return neutral;
                }
            case XENOPHOBIC:
                switch(p2) {
                    case PACIFIST:      return like;
                    case HONORABLE:     return dislike;
                    case XENOPHOBIC:    return love;
                    case RUTHLESS:      return like;
                    case AGGRESSIVE:    return dislike;
                    default:            return neutral;
                }
            case RUTHLESS:
                switch(p2) {
                    case PACIFIST:      return dislike;
                    case HONORABLE:     return dislike;
                    case XENOPHOBIC:    return like;
                    case RUTHLESS:      return love;
                    case AGGRESSIVE:    return like;
                    default:            return neutral;
                }
            case AGGRESSIVE:
                switch(p2) {
                    case PACIFIST:      return dislike;
                    case HONORABLE:     return like;
                    case XENOPHOBIC:    return dislike;
                    case RUTHLESS:      return like;
                    case AGGRESSIVE:    return love;
                    default:            return neutral;
                }
            default:
                return neutral;
        }        
    }
}
