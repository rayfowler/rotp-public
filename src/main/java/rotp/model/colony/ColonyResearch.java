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

public class ColonyResearch extends ColonySpendingCategory {
    private static final long serialVersionUID = 1L;
    private float projectBC = 0;
    private ColonyResearchProject project;

    public ColonyResearchProject project()         { return project; }
    public boolean hasProject()                    { return project != null; }
    public void project(ColonyResearchProject p)   { project = p; }
    public void endProject()                       { project = null; }

    @Override
    public int categoryType()           { return Colony.RESEARCH; }
    @Override
    public boolean isCompleted()        { return false; }
    @Override
    public float totalBC()              { return totalSpending() * researchBonus(); }
    @Override
    public float totalBCForEmpire()     { return max(0, totalBC() - projectRemainingBC()); }
    public float totalSpending()        { return super.totalBC(); }
    public float projectRemainingBC()   { return project == null ? 0 : project.remainingResearchBC(); }
    @Override
    public void nextTurn(float totalProd, float totalReserve) {
        projectBC = 0;
        // there may be a special project at this colony that consumes research
        if (project != null) {
            projectBC = min(totalBC(), project.remainingResearchBC());
            project.addResearchBC(projectBC);
        }
    }
    private float researchBonus() { return  planet().researchAdj() * empire().race().researchBonusPct() * session().researchBonus(); }
    @Override
    public boolean warning()      {        
        return (project != null) && (totalBC() < project.remainingResearchBC()); }
    @Override
    public String upcomingResult() {
        if (colony().allocation(categoryType()) == 0)
            return text(noneText);

        float bc = totalBC();
        if (project != null) {
            bc -=  project.remainingResearchBC();
            if (bc <= 0)
                return text(project.projectKey());
        }
        return text(researchPointsText, (int)bc);
    }
    @Override
    public void assessTurn() { }
    public void commitTurn() { }
}
