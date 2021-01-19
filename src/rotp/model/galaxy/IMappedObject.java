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
package rotp.model.galaxy;

import java.awt.geom.Line2D;
import java.util.Comparator;

public interface IMappedObject {
    public float x();
    public float y();

    default public float distanceTo (float x1, float y1) {
        return (float) Math.sqrt( ((x1-x())*(x1-x())) + ((y1-y())*(y1-y())) );
    }
    default public float distanceTo (IMappedObject obj) {
        return distanceTo(obj.x(), obj.y());
    }
    public static Comparator<IMappedObject> MAP_ORDER = new Comparator<IMappedObject>() {
        @Override
        public int compare(IMappedObject o1, IMappedObject o2) {
            float x1 = (o1.y() * Galaxy.current().width()) + o1.x();
            float x2 = (o2.y() * Galaxy.current().width()) + o2.x();
            return  (x1 < x2) ? -1 : 1;
        }
    };
    default public boolean passesThroughNebula(IMappedObject fr, IMappedObject to) {
        Line2D.Float path = new Line2D.Float(fr.x(), fr.y(), to.x(), to.y());
        for (Nebula neb: Galaxy.current().nebulas()) {
            if (neb.intersects(path))
                return true;
        }
        return false;
    }
    default public boolean passesThroughNebula(float x0, float y0, float x1, float y1) {
        Line2D.Float path = new Line2D.Float(x0, y0, x1, y1);
        for (Nebula neb: Galaxy.current().nebulas()) {
            if (neb.intersects(path))
                return true;
        }
        return false;
    }
    default public float travelTime(IMappedObject fr, IMappedObject to, float speed) {
        float dist = fr.distanceTo(to);
        int numSegments = (int) Math.ceil(dist);
        float dX = (to.x()-fr.x())/dist;
        float dY = (to.y()-fr.y())/dist;
        float x0 = fr.x();
        float y0 = fr.y();
        float x2 = to.x();
        float y2 = to.y();
        float travelTime = 0f;
        for (int i=1;i<=numSegments;i++) {
            float x1 = i == numSegments ? x2 : x0+dX;
            float y1 = i == numSegments ? y2 : y0+dY;
            if (passesThroughNebula(x0,y0,x1,y1))
                travelTime += 1.0f;
            else 
                travelTime += (1.0f/speed);
            x0 = x1;
            y0 = y1;
        }
        // deduct calculated travel time by a small amount to account for 
        // potential rounding errors upward when summing the times of 
        // individual segments. Why? Because reporting a slightly too high
        // travel time can mistakenly result in a full additional turn of travel 
        // since all travel turns are Math.ceil(). (i.e. 4.0001 estimated turns
        // to travel takes 5 runs in game). .01f is a small time but far greater
        // than the potential rounding error
        return travelTime-0.01f;
    }
    default public float travelTimeTo(IMappedObject to, float speed) {
        float dist = distanceTo(to);
        int numSegments = (int) Math.ceil(dist);
        float dX = (to.x()-x())/dist;
        float dY = (to.y()-y())/dist;
        float x0 = x();
        float y0 = y();
        float x2 = to.x();
        float y2 = to.y();
        float travelTime = 0f;
        for (int i=1;i<=numSegments;i++) {
            float x1 = i == numSegments ? x2 : x0+dX;
            float y1 = i == numSegments ? y2 : y0+dY;
            if (passesThroughNebula(x0,y0,x1,y1))
                travelTime += 1.0f;
            else 
                travelTime += (1.0f/speed);
            x0 = x1;
            y0 = y1;
        }
        return travelTime;
    }
}
