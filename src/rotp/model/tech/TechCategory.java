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
package rotp.model.tech;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import rotp.model.empires.Race;
import rotp.util.Base;

public final class TechCategory implements Base, Serializable {
    private static final long serialVersionUID = 1L;
    public static int ATTEMPTS = 0;
    public static float RESEARCH_INTEREST = 0.25f;
    public static final int MAX_ALLOCATION_TICKS = 60;
    private static final int MAX_QUINTILES = 20;
    private static String[] researchKeys = { "TECH_RESEARCH_COMPUTER", "TECH_RESEARCH_CONSTRUCTION",
                    "TECH_RESEARCH_FORCE_FIELD", "TECH_RESEARCH_PLANETOLOGY", "TECH_RESEARCH_PROPULSION", "TECH_RESEARCH_WEAPON" };
    private static String[] categoryKeys = { "TECH_COMPUTERS", "TECH_CONSTRUCTION", "TECH_FORCE_FIELDS",
                    "TECH_PLANETOLOGY", "TECH_PROPULSION", "TECH_WEAPONS" };
    public static String id(int i) {
        switch (i) {
            case 0 : return "TECH_COMPUTERS";
            case 1 : return "TECH_CONSTRUCTION";
            case 2 : return "TECH_FORCE_FIELDS";
            case 3 : return "TECH_PLANETOLOGY";
            case 4 : return "TECH_PROPULSION";
            case 5 : return "TECH_WEAPONS";
        }
        return "";
    }

    private int index;
    private int allocation = 0;
    private String currentTech;
    private boolean locked = false;
    private final List<String> knownTechs = new ArrayList<>();
    // possible techs are what remains of the normal research tree
    private final List<String> possibleTechs = new ArrayList<>();
    // bonus techs are special techs that can be researched regardless of their
// location on the research tree. Typically granted by artifact planets.
    private final List<String> bonusTechs = new ArrayList<>();
    private TechTree tree;
    private float discoveryPct = 1;
    private float totalBC = 0;
    private int futureTechLevel = 0;

    public int index()                     { return index; }
    public void index(int i)               { index = i; }
    public List<String> knownTechs()       { return knownTechs; }
    public String currentTech()            { return currentTech; }
    public String currentTechName()        { return tech(currentTech).name(); }
    public void currentTech(Tech t)        { currentTech = t.id(); }
    public boolean locked()                { return locked; }
    public void toggleLock()               { locked = !locked; }
    public String id()                     { return id(index); }
    public float totalBC()                 { return totalBC; }
    public float techLevel()               { return currentTechLevel(); }
    public String researchKey()            { return researchKeys[index]; }
    public String key()                    { return categoryKeys[index]; }
    public boolean isWeaponTechCategory()  { return (this == tree.weapon()); }
    private float racialMod()             { return tree == null? 1.0f : tree.empire().race().techMod[index]; }
    public float discoveryPct()           { return discoveryPct; }

    public TechCategory() { }

    public TechCategory (int i, TechTree tr, float p) {
        index = i;
        tree = tr;
        discoveryPct = p;
        init();
    }

    public int allocation()            { return allocation; }
    public void allocation(int i)      { allocation = bounds(0,i,MAX_ALLOCATION_TICKS); }
    public float allocationPct()      { return (float) allocation/MAX_ALLOCATION_TICKS; }
    public void adjustAllocation(int i) { allocation(allocation+i); }
    public void increaseAllocation()   { allocation(allocation+1); }
    public void decreaseAllocation()   { allocation(allocation-1); }
    public void allocationPct(float d) {
        // d assumed to be between 0 & 1, representing pct of slider clicked
        float incr = 1.0f/(MAX_ALLOCATION_TICKS+1);
        float sum = 0;
        for (int i=0;i<MAX_ALLOCATION_TICKS+1;i++) {
            sum += incr;
            if (d <= sum) {
                allocation(i);
                return;
            }
        }
        allocation(MAX_ALLOCATION_TICKS);
    }
    public float baseResearchCost()  { return options().researchCostBase()*session().researchMapSizeAdjustment();}
    private void init() {
        if (!tree.spy())
            buildResearchList();
    }
    public void addPossibleTech(String id) {
        if (!possibleTechs.contains(id))
            possibleTechs.add(id);
    }
    private void addKnownTech(String id) {
        if (!knownTechs().contains(id)) {
            knownTechs().add(id);
            Collections.sort(knownTechs, Tech.LEVEL);
        }
    }
    private void addBonusTech(String id)      {  bonusTechs.add(id); }
    public void allowResearch(String id) {
        if (knownTechs().contains(id))
            return;
        if (possibleTechs.contains(id))
            return;
        addPossibleTech(id);
        addBonusTech(id);
    }
    public void spyKnownTechs(TechCategory cat) { spyKnownTechs(cat, 99); }
    private void spyKnownTechs(TechCategory cat, int maxLevel) {
        for (String id: cat.knownTechs()) {
            Tech t = tech(id);
            if (t.level() <= maxLevel)
                addKnownTech(id);
        }
        //can't see new techs
        knownTechs().removeAll(tree.newTechs());
    }
    public String randomKnownTech() {
        return random(knownTechs());
    }
    public Tech randomUnknownTech(int minLevel, int levelDiff) {
        // find level of highest known tech
        int highestLevel = 0;
        for (String id: knownTechs()) {
            Tech t = tech(id);
            highestLevel = max(highestLevel, t.level());
        }

        // add the level diff, don't select tech levels > 50 (future techs)
        int maxLevel = min(50, highestLevel + levelDiff);

        // from ALL techs unknown and <= level, pick one at random
        List<Tech> techList = new ArrayList<>();
        for (String id: allTechs()) {
            Tech t = tech(id);
            if (!knownTechs().contains(id) && !t.restricted && (t.level() >= minLevel) && (t.level() <= maxLevel))
                techList.add(t);
        }
        return random(techList);
    }

    public List<String> possibleTechs()  { return possibleTechs; }
    public List<String> allTechs()       { return TechLibrary.baseCategory[index].possibleTechs(); }

    private void buildResearchList() {
        TechCategory baseCat = TechLibrary.baseCategory[index];

        Race race = tree.empire().race();
        possibleTechs.clear();

        Object[] techsByQuintile =new Object[MAX_QUINTILES];
        for (int i=0;i<MAX_QUINTILES;i++) 
            techsByQuintile[i] = new ArrayList<String>();      

        for (int i=0;i<baseCat.possibleTechs.size();i++) {
            String id = baseCat.possibleTechs.get(i);
            Tech t = tech(id);
            if (!t.restricted && t.canBeResearched(race) && !t.free ) {
                List<String> techs = (List<String>) techsByQuintile[t.quintile()-1];
                techs.add(id);
            }
        }

        for (int i=0;i<MAX_QUINTILES;i++) {
            boolean found = false;
            List<String> techs = (List<String>) techsByQuintile[i];
            for (String id: techs) {
                if (random() <= discoveryPct()) {
                    addPossibleTech(id);
                    found = true;
                }
            }
            if (!found)
                addPossibleTech(random(techs));
        }
    }
    public void learnFreeTechs() {
        TechCategory baseCat = TechLibrary.baseCategory[index];
        for (int i=0; i<baseCat.possibleTechs.size(); i++) {
            String id = baseCat.possibleTechs.get(i);
            Tech t = tech(id);
            if (t.free && !tree.spy())
                learnTech(id);
        }
    }
    public boolean studyingFutureTech() {
        return (futureTechLevel > 0);
    }
    public String techDescription1(boolean key) {
        switch(index) {
                case 0: return key ? text("TECH_FACTORY_CONTROLS") : text("TECH_FACTORY_CONTROLS_BONUS", tree.empire().maxRobotControls());
                case 1: 
                    if (key)
                        return text("TECH_WASTE_REDUCTION");
                   else if (tree.factoryWasteMod() == 1)
                        return text("TECH_WASTE_REDUCTION_NONE");
                 else
                        return text("TECH_WASTE_REDUCTION_AMT", (100-(int)(tree.factoryWasteMod()*100)));
                case 2: 
                    if (key)
                        return text("TECH_PLANETARY_SHIELDS");
                    else if  (tree.maxPlanetaryShieldLevel() == 0)
                        return text("TECH_PLANETARY_SHIELDS_NONE");
                    else
                        return text("TECH_PLANETARY_SHIELDS_CLASS", (int)tree.maxPlanetaryShieldLevel());
                case 3: 
                    if (key)
                        return text("TECH_TERRAFORM");
                    else if  (tree.terraformAdj() == 0)
                        return text("TECH_TERRAFORM_NO_BONUS");
                    else
                        return text("TECH_TERRAFORM_BONUS", (int)tree.terraformAdj());
                case 4: return key ? text("TECH_SHIP_RANGE") : text("TECH_SHIP_RANGE_AMT", (int)tree.shipRange());
                case 5: 
                    if (key)
                        return text("TECH_GROUND_COMBAT");
                    else if  (tree.weaponGroundBonus() == 0)
                        return text("TECH_GROUND_COMBAT_NO_BONUS");
                    else
                        return text("TECH_GROUND_COMBAT_BONUS", (int)tree.weaponGroundBonus());
        }
        return "";
    }
    public String techDescription2(boolean key) {
        switch(index) {
            case 0: 
                if (key)
                    return text("TECH_PLANET_SCANNER");
                else if (tree.empire().planetScanningRange() == 0)
                    return text("TECH_PLANET_SCANNER_NONE");
                else
                    return text("TECH_PLANET_SCANNER_RANGE", (int)tree.empire().planetScanningRange());
            case 1: 
                int bonus = (int)(tree.armorGroundBonus() +tree.battleSuitGroundBonus());
                if (key)
                    return text("TECH_GROUND_COMBAT");
                else if  (bonus == 0)
                    return text("TECH_GROUND_COMBAT_NO_BONUS");
                else
                    return text("TECH_GROUND_COMBAT_BONUS", bonus);
            case 2: 
                if (key)
                    return text("TECH_GROUND_COMBAT");
                else if  (tree.shieldGroundBonus() == 0)
                    return text("TECH_GROUND_COMBAT_NO_BONUS");
                else
                    return text("TECH_GROUND_COMBAT_BONUS", (int)tree.shieldGroundBonus());
            case 3: return key ? text("TECH_WASTE_CLEANUP") : text("TECH_WASTE_CLEANUP_BONUS", (int)tree.wasteElimination());
            case 4: return key ? text("TECH_SHIP_SPEED") : text("TECH_SHIP_SPEED_AMT", (int)tree.topSpeed());
            case 5: return "";
        }
        return "";
    }
    private float currentTechLevel() {
        int max = 0;
        int free = 0;
        List<String> ids = new ArrayList<>(knownTechs());
        for (String id: ids) {
            Tech t = tech(id);
            if (t.free)
                free++;
            if (t.level > max)
                max = t.level;
        }
        return max(1, ((.80f * max) + ids.size() - free));
    }
    public void knowAll() {
        for (String id: possibleTechs()) {
            Tech t = tech(id);
            if (!t.restricted)
                addKnownTech(id);
        }
    }
    public void knowAll(int maxLevel, float pct) {
        TechCategory baseCat = TechLibrary.baseCategory[index];
        for (String id: baseCat.allTechs()) {
            Tech t = tech(id);
            if (!t.restricted && (t.level <= maxLevel) && (random() < pct))
                addKnownTech(t.id());
        }
    }
    public void learnAll() {
        TechCategory baseCat = TechLibrary.baseCategory[index];
        List<String> possCopy = new ArrayList<>();
        for (String id: baseCat.possibleTechs())
            possCopy.add(id);
        for (String id: possCopy)
            learnTech(id);
    }
    private Tech techNamed(String name) {
        for (String id: possibleTechs()) {
            Tech t = tech(id);
            if (t.name().equals(name))
                return t;
        }
        return null;
    }
    public void beginResearchOnTechNamed(String name) {
        currentTech = techNamed(name).id();
    }
    public int maxKnownQuintile() {
        int max = 0;
        for (String id: knownTechs()) {
            Tech t = tech(id);
            if (!t.free)
                max = max(max,t.quintile());
        }
        return max;
    }
    public int maxResearchableQuintile() {
        int maxQ = maxKnownQuintile();
        // until future techs (maxQ = 10), can alway learn techs in the next quintile
        if (maxQ < 10)
            return maxQ+1;
        
        // we have techs known in the last non-future quintile. At this point, we
        // can only research techs up to the current maxQ if there are any unresearched
        // techs in current maxQ or lower
        for (String id: possibleTechs) {
            Tech t = tech(id);
            if (t.quintile() <= maxQ)
                return maxQ;
        }
        
        // all unresearched techs are above current maxQ. Therefore we can learn
        // future techs in the next quintile
        return maxQ+1;
    }
    public List<String> techIdsAvailableForResearch() {
        return techIdsAvailableForResearch(true);
    }
    public List<String> techIdsAvailableForResearch(boolean lookAhead) {
        int q = maxResearchableQuintile();
        List<String> r = new ArrayList<>();
        String first = null;

        for (String id: possibleTechs()) {
            Tech t = tech(id);
            if (bonusTechs.contains(id)
            || ((t.quintile() <= q) && possibleTechs.contains(id))) {
                r.add(id);
                if (first == null)
                    first = id;
            }
        }
        if ((r.isEmpty()) && (first != null))
            r.add(first);

        return r;
    }
    public List<Tech> techsAvailableForResearch() {
        return techsAvailableForResearch(true);
    }
    public List<Tech> techsAvailableForResearch(boolean lookAhead) {
        int q = maxKnownQuintile();
        if (lookAhead)
            q++;
        List<Tech> r = new ArrayList<>();
        Tech first = null;

        for (String id: possibleTechs()) {
            Tech t = tech(id);
            if (bonusTechs.contains(id)
            || ((t.quintile() <= q) && possibleTechs.contains(id))) {
                r.add(t);
                if (first == null)
                    first = t;
            }
        }
        if (r.isEmpty() && (first != null))
            r.add(first);

        return r;
    }
    public float costForTech(Tech t) {
        return baseResearchCost() * t.level * t.level * racialMod();
    }
    private float discoveryChance() {
        if (currentTech == null)
            return 0;

        float cost = costForTech(tech(currentTech));
        float nextTurnBC = totalBC + currentResearch();
        if (nextTurnBC <= cost)
            return 0;

        return (nextTurnBC - cost) / (cost * 2);
    }
    public float upcomingDiscoveryChance(float totalRP) {
        if (currentTech == null)
            return 0;

        Tech current = tech(currentTech);
        float nextTurnBC = totalBC + currentResearch(totalRP);
        float t = nextTurnBC / costForTech(current);
        return t <= 1 ? t : min(2,((t + 1) /2));
    }
    public float upcomingDiscoveryChance() {
        if (currentTech == null)
            return 0;

        return upcomingDiscoveryChance(tree.empire().totalPlanetaryResearch());
    }
    public float currentResearch() {
        return currentResearch(tree.empire().totalPlanetaryResearch());
    }
    public float currentResearch(float totalRP) {
        float categoryBC = totalRP * allocationPct();
        float interestBC = RESEARCH_INTEREST * min(totalRP/6, categoryBC);
        return categoryBC+interestBC;
    }
    public void allocateResearchBC() {
        totalBC = totalBC + currentResearch();

        // currentTech == null should only happen at game start
        // and when all category techs have been research
        // knowntechs contains currentTech is an error condition
        // that needs to be recovered from
        if (((currentTech == null) && (totalBC > 0)) 
        || knownTechs.contains(currentTech)){
            List<String> techs = techIdsAvailableForResearch();
            if (techs.isEmpty())
                return; // we are out of techs... skip this category
            else
                setTechToResearch();
        }

        if (random() < discoveryChance()) 
            tree.empire().learnTech(currentTech);
    }
    public void setTechToResearch() {
        tree.empire().ai().scientist().setTechToResearch(this);
    }
    public void resetResearchBC()   { totalBC = 0; }
    public boolean learnTech(String id) {
        Tech t = tech(id);
        // infrequently we get here for a tech we already know... perhaps
        // if a tech is discovered/stolen on the same turn it is researched
        // in this case, we'll return false so that players don't get an
        // extra notification of it.
        //log(tree.empire().name(), " learns tech: ", t.name());
        boolean newTech = !knownTechs().contains(id);

        addKnownTech(id);
        if (tree.spy())
            return newTech;

        bonusTechs.remove(id);
        possibleTechs.remove(id);
        t.provideBenefits(tree.empire());

        if (id.equals(currentTech())) {
            resetResearchBC();
            List<String> techs = techIdsAvailableForResearch();
            if (!techs.isEmpty())
                setTechToResearch();
        }
        return newTech;
    }
}
