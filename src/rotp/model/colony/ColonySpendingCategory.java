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
package rotp.model.colony;

import java.io.Serializable;
import rotp.model.empires.Empire;
import rotp.model.planet.Planet;
import rotp.model.tech.TechTree;
import rotp.util.Base;

public abstract class ColonySpendingCategory implements Base, Serializable {
    private static final long serialVersionUID = 1L;
    public static String noneText = "MAIN_COLONY_SPENDING_NONE";
    public static String reserveText = "MAIN_COLONY_SPENDING_RESERVE";
    public static String convertAlienFactoriesText = "MAIN_COLONY_SPENDING_CONVERT";
    public static String refitFactoriesText = "MAIN_COLONY_SPENDING_REFIT";
    public static String maximumFactoriesText = "MAIN_COLONY_SPENDING_MAX_FACT";
    public static String wasteText = "MAIN_COLONY_SPENDING_WASTE";
    public static String atmosphereText = "MAIN_COLONY_SPENDING_ATMOSPHERE";
    public static String enrichSoilText = "MAIN_COLONY_SPENDING_ENRICH_SOIL";
    public static String terraformText = "MAIN_COLONY_SPENDING_TERRAFORM";
    public static String cleanupText = "MAIN_COLONY_SPENDING_CLEANUP";
    public static String growthText = "MAIN_COLONY_SPENDING_GROWTH";
    public static String shieldText = "MAIN_COLONY_SPENDING_SHIELD";
    public static String upgradeBasesText = "MAIN_COLONY_SPENDING_UPG_BASES";
    public static String researchPointsText = "MAIN_COLONY_SPENDING_RP";
    public static String yearsLongText = "MAIN_COLONY_COMPLETION_CENTURY";
    public static String yearsText = "MAIN_COLONY_COMPLETION_YEARS";
    public static String yearText = "MAIN_COLONY_COMPLETION_YEAR";
    public static String perYearText = "MAIN_COLONY_COMPLETION_PER_YEAR";

    public static final int MAX_TICKS = 50;

    private Colony colony;

    public abstract String upcomingResult();
    public abstract int categoryType();
    public abstract boolean isCompleted();
    public abstract void nextTurn(float prod, float rsv);
    public abstract void assessTurn();

    public ColonySpendingCategory () {  }
    @Override
    public String toString()            { return str(allocation()); }
    public float totalBC()              { return pct() * colony().totalIncome(); }
    public float totalBCForEmpire()     { return totalBC(); }
    public int allocation()             { return colony.allocation(categoryType()); }
    public float pct()                  { return (float)allocation()/ MAX_TICKS; }
    public boolean warning()            { return false; }
    public void init(Colony c) {
        colony = c;
    }
    public Colony colony()            { return colony; }
    public Planet planet()            { return colony().planet(); }
    public Empire empire()            { return colony().empire(); }
    public TechTree tech()            { return empire().tech(); }
    public String name()              { return ""; }
    public float orderedValue()      { return colony.locked(categoryType()) ? pct() : 0; }
    public void removeSpendingOrders() { }
    public boolean canLowerMaintenance() { return false; }
    public void lowerMaintenance()       { }
    public int orderedAllocation()      { return (int) Math.ceil(orderedValue() * MAX_TICKS);  }  
    public int adjustValue(int amt) {
        // attempt to adjust current value by amt
        // return the actual amount adjusted
        int oldValue = allocation();
        colony.allocation(categoryType(), bounds(0,oldValue+amt,MAX_TICKS));
        return allocation() - oldValue;
    }
}
