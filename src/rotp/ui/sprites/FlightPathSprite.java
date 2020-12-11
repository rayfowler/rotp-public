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
package rotp.ui.sprites;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.List;
import rotp.model.Sprite;
import rotp.model.empires.Empire;
import rotp.model.galaxy.Ship;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.StarSystem;
import rotp.model.game.GameSession;
import rotp.ui.BasePanel;
import rotp.ui.main.GalaxyMapPanel;

public class FlightPathSprite extends MapSprite {
    static Stroke[][] lines;
    static Stroke[] rallyStroke;
    static final Color rallyColor = new Color(96,0,128);
    Ship ship;
    StarSystem fr;
    StarSystem to;
    boolean isColonyRelocation = false;
    Polygon selectionArea;
    static int animationSpeed = 5;
    int ptX[] = new int[8];
    int ptY[] = new int[8];
    boolean displayed = false;

    public static void workingPath(FlightPathSprite s) {
        List<FlightPathSprite> paths = workingPaths();
        paths.clear();

        if (s == null)
            return;

        if (s.ship().destSysId() == s.destination().id)
            return;

        paths.add(s);
    }
    public static List<FlightPathSprite> workingPaths() {
        List<FlightPathSprite> paths = (List<FlightPathSprite>) GameSession.instance().var("WORKING_PATHS");
        if (paths == null) {
            paths = new ArrayList<>();
            GameSession.instance().var("WORKING_PATHS", paths);
        }
        return paths;
    }
    public static void clearWorkingPaths() {
        workingPaths().clear();
    }
    public FlightPathSprite()  {}
    public FlightPathSprite(Ship s, StarSystem sv) {
        ship = s;
        to = sv;
    }
    public FlightPathSprite(StarSystem sv1, StarSystem sv2) {
        fr = sv1;
        to = sv2;
        isColonyRelocation = true;
    }
    public StarSystem destination()         { return to; }
    public void destination(StarSystem sv)  { to = sv; }
    public boolean isPlayer() { return ship.empId() == Empire.PLAYER_ID; }
    public boolean aggressiveToPlayer() {
        Empire pl = player();
        Empire shipEmpire = galaxy().empire(ship.empId());
        int targetEmpId = pl.sv.empId(to.id);
        return shipEmpire.isAI()
            && pl.alliedWith(targetEmpId)
            && shipEmpire.aggressiveWith(targetEmpId)
            && ship.isPotentiallyArmed(pl);
    }

    private Color lineColor(GalaxyMapPanel map, StarSystem sys) {
        // ship is null for relocation lines
        if (isColonyRelocation)
            return rallyColor;

        Color c0;

        // ships of of allies' enemies will show in red
        if (ship.empId() == Empire.PLAYER_ID) {
            if  (!ship.validDestination(id(sys)))
                c0 = Color.yellow;
            else if ((workingPaths().contains(this))
                && ship.passesThroughNebula(sys))
                c0 = Color.magenta;
            else
                c0 = Color.green;
        }
        else {
            if (aggressiveToPlayer())
                c0 = Color.red;
            else
                c0 = Color.yellow;
        }
        return c0;
    }
    public Sprite from()                       { return (Sprite) ship; }
    public Ship ship()                         { return ship; }

    public void to(StarSystem sv)              { to = sv; }
    @Override
    public void click(GalaxyMapPanel map, int count, boolean rightClick, boolean click) {
        // clicking on a flight path is really clicking on its fleetsprite
        if (ship() != null)
            map.parent().clickingOnSprite(from(), count, rightClick, click);
    }
    private void setSelectionArea(int x1, int y1, int x2, int y2) {
        Point pt1 = new Point();
        Point pt2 = new Point();
        // pt1 is always the leftmost end of the flight path
        if (x1 < x2) {
            pt1.x = x1;
            pt1.y = y1;
            pt2.x = x2;
            pt2.y = y2;
        }
        else {
            pt1.x = x2;
            pt1.y = y2;
            pt2.x = x1;
            pt2.y = y1;
        }

        int r = BasePanel.s5;
        if (pt2.y > pt1.y) { // sloping down to the right
            ptX[0] = pt1.x+r; ptY[0]=pt1.y+0;
            ptX[1] = pt1.x+0; ptY[1]=pt1.y-r;
            ptX[2] = pt1.x-r; ptY[2]=pt1.y+0;
            ptX[3] = pt1.x+0; ptY[3]=pt1.y+r;
            ptX[4] = pt2.x-r; ptY[4]=pt2.y+0;
            ptX[5] = pt2.x+0; ptY[5]=pt2.y+r;
            ptX[6] = pt2.x+r; ptY[6]=pt2.y+0;
            ptX[7] = pt2.x+0; ptY[7]=pt2.y-r;
        }
        else {  // sloping up to the right
            ptX[0] = pt1.x+0; ptY[0]=pt1.y-r;
            ptX[1] = pt1.x-r; ptY[1]=pt1.y+0;
            ptX[2] = pt1.x+0; ptY[2]=pt1.y+r;
            ptX[3] = pt1.x+r; ptY[3]=pt1.y+0;
            ptX[4] = pt2.x+0; ptY[4]=pt2.y+r;
            ptX[5] = pt2.x+r; ptY[5]=pt2.y+0;
            ptX[6] = pt2.x+0; ptY[6]=pt2.y-r;
            ptX[7] = pt2.x-r; ptY[7]=pt2.y+0;
        }

        if (selectionArea == null)
            selectionArea = new Polygon(ptX, ptY, 8);
        else {
            selectionArea.reset();
            for (int i=0;i<ptX.length;i++)
                selectionArea.addPoint(ptX[i], ptY[i]);
        }
    }
    @Override
    public boolean isSelectableAt(GalaxyMapPanel map, int mapX, int mapY) {
        return displayed && (selectionArea != null) && selectionArea.contains(mapX, mapY);
    }
    @Override
    public void draw(GalaxyMapPanel map, Graphics2D g2) {
        draw(map, g2, to);
    }
    public void draw(GalaxyMapPanel map, Graphics2D g2, StarSystem dest) {
        displayed = false;
        if (dest == null)
            return;
        else if (ship != null)
            drawShipPath(map,g2,dest);
        else if (fr != null)
            drawPlanetPath(map,g2,dest);
    }
    public void drawPlanetPath(GalaxyMapPanel map, Graphics2D g2, StarSystem dest) {
        int x1 = fr.centerMapX(map);
        int y1 = fr.centerMapY(map);
        int x2 = dest.centerMapX(map);
        int y2 = dest.centerMapY(map);

        boolean isHovering = hovering || map.parent().isClicked(this);
        draw(g2, map.animationCount(), map.scaleX(), isHovering, x1, y1, x2, y2, lineColor(map, dest));
    };
    private void drawShipPath(GalaxyMapPanel map, Graphics2D g2, StarSystem dest) {
        Sprite spr = (Sprite) ship;
        int x1 = spr.centerMapX(map);
        int y1 = spr.centerMapY(map);
        int x2 = dest.centerMapX(map);
        int y2 = dest.centerMapY(map);
        boolean isHovering = hovering || map.parent().isClicked(this) ;

        draw(g2, map.animationCount(), map.scaleX(), isHovering, x1, y1, x2, y2, lineColor(map, dest));
    };
    public void draw(Graphics2D g2, int animationCount, float scale, boolean hovering, int x1, int y1, int x2, int y2, Color c0) {
        if (lines == null)
            initStrokes();

        // unless we are a working path, we should never display if our
        // associated ship is not also displayed
        if (!workingPaths().contains(this)) {
           // never draw a flight path if the associated ship is not drawn
            if ((ship != null) && (ship instanceof ShipFleet) && !ship.displayed())
                return;
        }

        displayed = true;
        setSelectionArea(x1,y1,x2,y2);

        Stroke prevStroke = g2.getStroke();

        int animationIndex = (animationCount/animationSpeed) %6;
        if (isColonyRelocation) 
            g2.setStroke(rallyStroke[animationIndex]);
        else if (workingPaths().contains(this)) 
            g2.setStroke(workingLine(scale,animationIndex));
        else if (hovering) 
            g2.setStroke(hoveringLine(scale, animationIndex));
        else 
            g2.setStroke(pathLine(scale, animationIndex));

        g2.setColor(c0);
        g2.drawLine(x2, y2, x1, y1);

        int s9 = BasePanel.s9;
        int s18 = BasePanel.s18;
        g2.drawOval(x2-s9, y2-s9, s18, s18);

        g2.setStroke(prevStroke);
    }
    private Stroke workingLine(float scale, int index) {
        return lines[0][index];
    }
    private Stroke pathLine(float scale, int index) {
        if (scale > 20)
            return lines[0][index];
        else
            return lines[1][index];
    }
    private Stroke hoveringLine(float scale, int index) {
        if (scale > 50)
            return lines[0][index];
        else if (scale > 40)
            return lines[1][index];
        else if (scale > 20)
            return lines[2][index];
        else
            return lines[3][index];
    }
    private void initStrokes() {
        lines = new BasicStroke[6][6];
        rallyStroke = new BasicStroke[9];
        float f15 = BasePanel.s15;
        float f14 = BasePanel.s14;
        float f4 = BasePanel.s4;
        float f10 = BasePanel.s10;
        float f3 = BasePanel.s3;
		float f12 = BasePanel.s12; // modnar: line dash change
        float f6 = BasePanel.s6; // modnar: line dash change

        for (int i=0;i<9;i++) {
            rallyStroke[i] = new BasicStroke(BasePanel.s3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,    // Join style
                            f10, new float[] {f12, f6}, i * BasePanel.s3); // modnar: line phase change, mod%6 animation
        }

        for (int i=0;i<6;i++) {
            for (int j=0;j<6;j++) {
                int w = scaled(i+2); // modnar: line width change
                float dashPhase = j * BasePanel.s3;
                lines[i][j] = new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,    // Join style
                                f10, new float[] {f12, f6}, dashPhase);
            }
        }
    }
}
