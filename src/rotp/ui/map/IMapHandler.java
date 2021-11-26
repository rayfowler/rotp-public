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
package rotp.ui.map;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.border.Border;
import rotp.model.Sprite;
import rotp.model.empires.Empire;
import rotp.model.empires.SystemView;
import rotp.model.galaxy.IMappedObject;
import rotp.model.galaxy.Location;
import rotp.model.galaxy.StarSystem;
import rotp.ui.main.GalaxyMapPanel;
import rotp.util.ImageManager;

public interface IMapHandler {
    public void repaint();
    public GalaxyMapPanel map();

    public Sprite hoveringSprite();
    public Sprite clickedSprite();
    public void clickedSprite(Sprite s);

    public Border mapBorder();
    public float startingScalePct();
    public default void checkMapInitialized() { };

    public Location mapFocus();

    public Color shadeC();
    public Color backC();
    public Color lightC();
    default public void mapFocus(IMappedObject obj) { mapFocus().setXY(obj.x(), obj.y()); }
    default public void drawYear(Graphics2D g) { }
    default public void drawTitle(Graphics2D g) { }
    
    default public boolean animating()    { return true; }

    default public boolean forwardMouseEvents() { return false; }
    default public void mouseWheelMoved(MouseWheelEvent e)  { }
    default public boolean dragSelect(int x0, int y0, int x1, int y, boolean shift) { return false; }

    default public void hoveringOverSprite(Sprite o)              { };
    default public void clickingOnSprite(Sprite o, int cnt, boolean right, boolean click)       { };
    default public void clickingNull(int cnt, boolean right) {  };
    default public boolean masksMouseOver(int x, int y)       { return false; }

    default public boolean isClicked(Sprite s)             { return clickedSprite() == s; }
    default public boolean isHovering(Sprite s)            { return hoveringSprite() == s; }
    default public boolean isHighlighting(Sprite s)        { return false; }
    default public boolean isLowlighting(Sprite s)         { return false; }
    default public boolean showTreasuryResearchBar()       { return false; }
    default public boolean showAllCurrentResearch()        { return false; }
    default public boolean showSpyReportIcon()             { return false; }
    default public boolean allowsDragSelect()              { return false; }
    default public boolean hoverOverFleets()               { return true; }
    default public boolean hoverOverSystems()              { return true; }
    default public boolean hoverOverFlightPaths()          { return true; }

    default public String systemLabel(StarSystem s)        { return Empire.thePlayer().sv.name(s.id); }
    default public String systemLabel2(StarSystem s)       { return ""; }
    default public Color systemLabelColor(StarSystem s)    { return Empire.thePlayer().sv.empireColor(s.id); }
    default public List<Sprite> nextTurnSprites()          { return new ArrayList<>(); }
    default public List<Sprite> controlSprites()           { return new ArrayList<>(); }
    default public void reselectCurrentSystem() { };

    default public int defaultFleetDisplay()             { return GalaxyMapPanel.SHOW_IMPORTANT_FLIGHTPATHS; }
    default public int defaultShipRangesDisplay()        { return GalaxyMapPanel.SHOW_STARS_AND_RANGES; }
    default public boolean showShipRanges()              { return true; }
    default public boolean defaultGridCircularDisplay()  { return false; }
    default public IMappedObject gridOrigin()            { return null; }
    default public void drawAlerts(Graphics2D g)         { }

    default Empire empireBoundaries()                    { return Empire.thePlayer(); }
    default public float systemClickRadius()             { return 1.0f; }
    default public boolean showYear()                    { return true; }
    default Color flagColor(StarSystem s)                { return Empire.thePlayer().sv.flagColor(s.id); }
    default Image flagImage(StarSystem s)                { return Empire.thePlayer().sv.flagImage(s.id); }
    default Image mapFlagImage(StarSystem s)             { return Empire.thePlayer().sv.mapFlagImage(s.id); }
    default Image flagHover(StarSystem s)                { return ImageManager.current().image("Flag_Hover"); }
    default Image flagHaze(StarSystem s)                 { return ImageManager.current().image("Flag_Haze"); }
    default boolean drawStar(StarSystem s)               { return true; }
    default boolean showOwnerReach(StarSystem s)         { return false; }
    default boolean showOwnership(StarSystem s)          { return true; }
    default float ownerReach(StarSystem s)               { return 0; }
    default boolean drawShield(StarSystem s)             { return true; } 
    default boolean shouldDrawEmpireName(Empire e, float scale)  { 
        return (e.isPlayer() || Empire.thePlayer().hasContacted(e.id)) && (scale > GalaxyMapPanel.MAX_FLEET_TRANSPORT_SCALE);
    }
    default void drawEmpireName(Empire e, GalaxyMapPanel ui, Graphics2D g)  { e.draw(ui,g); }
    default boolean drawStargate(StarSystem s)           { return true; }
    default boolean drawFlag(StarSystem s)               { return true; }
    default boolean showSystemName(StarSystem s)         { return map().showSystemNames(); } 
    default boolean showSystemData(StarSystem s)         { return map().showSystemData(); } 
    default boolean shouldDrawSprite(Sprite s)           { return true; }
    default boolean canChangeMapScales()                 { return true; }
    default boolean displayNextTurnNotice()              { return false; }
    default boolean suspendAnimationsDuringNextTurn()    { return true; }
    default void paintOverMap(GalaxyMapPanel ui, Graphics2D g) { }
    default boolean showAlerts()                         { return true; }
    default boolean drawShips()                          { return true; }
    default boolean drawBackgroundStars()                { return true; }
    default Color alertColor(SystemView sv)              { return null; }
    default Empire knownEmpire(int sysId, Empire emp)    { return emp.sv.empire(sysId); }

}
