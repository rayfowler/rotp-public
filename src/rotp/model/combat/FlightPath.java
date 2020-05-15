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
package rotp.model.combat;

import rotp.util.Base;
import java.util.Comparator;
import java.util.List;

public class FlightPath implements Base {
    public static final int mapW = 12;
    public static int[] basePathPriority = { -mapW, -mapW+1, 1, mapW+1, mapW, mapW-1, -1, -mapW-1 };
    public static int[] nwPathPriority =   { -mapW-1, -mapW, -1, -mapW+1, mapW-1, 1, mapW, mapW+1 };
    public static int[] nPathPriority =    { -mapW, -mapW-1, -mapW+1, -1, 1, mapW-1, mapW+1, mapW };
    public static int[] nePathPriority =   { -mapW+1, -mapW, 1, -mapW-1, mapW+1, -1, mapW, mapW-1 };
    public static int[] ePathPriority =    { 1, -mapW+1, mapW+1, -mapW, mapW, -mapW-1, mapW-1, -1 };
    public static int[] sePathPriority =   { mapW+1, 1, mapW, -mapW+1, mapW-1, -mapW, -1, -mapW-1 };
    public static int[] sPathPriority =    { mapW, mapW+1, mapW-1, 1, -1, -mapW+1, -mapW-1, -mapW };
    public static int[] swPathPriority =   { mapW-1, mapW, -1, mapW+1, -mapW-1, 1, -mapW, -mapW+1 };
    public static int[] wPathPriority =    { -1, -mapW-1, mapW-1, -mapW, mapW, -mapW+1, mapW+1, 1 };

    List<Integer> points;
    final int gridW;
    private int straightness = -1;
    private float sortValue = -1;

    public List<Integer> points() { return points; }
    public void add(int pt)       { points.add(pt); }
    public int size()             { return points.size(); }
    public int mapX(int i)        { return (points.get(i) % gridW) -1; 	}
    public int mapY(int i)        { return (points.get(i) / gridW) -1; 	}
    public void limitMoves(int n) {
        if ((n >= 0) && points.size() > n) 
            points = points.subList(0,n);
    }
    public float sortValue()      {
        if (sortValue < 0)
            calculateSortValue();
        return sortValue;
    }
    private void calculateSortValue() {
        if (straightness < 0)
            calculateStraightness();
        sortValue = size()+(straightness/100.0f);
    }
    public FlightPath(List<Integer> pts, int w) {
        points = pts;
        gridW = w;
    }
    public int destX() {
        return mapX(size()-1);
    }
    public int destY() {
        return mapY(size()-1);
    }
    private void calculateStraightness() {
        int prevX = -1;
        int prevY = -1;
        for (int i=0;i<size();i++) {
            int x0 = mapX(i);
            int y0 = mapY(i);
            if (prevX >= 0) {
                straightness += abs(x0 - prevX);
                straightness += abs(y0 - prevY);
            }
            prevX = x0;
            prevY = y0;
        }
    }
    public static Comparator<FlightPath> SORT = new Comparator<FlightPath>() {
        @Override
        public int compare(FlightPath col1, FlightPath col2) {
            return Base.compare(col1.sortValue(),col2.sortValue());
        }
    };
}
