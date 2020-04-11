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

import java.awt.Color;
import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;

public class RoundGradientPaint  implements Paint {
    protected Point2D mPoint;
    protected Point2D mRadius;
    protected Color mPointColor, mBackgroundColor;
    private static RoundGradientContext rgc;
    protected int flareLevel;

    public RoundGradientPaint() {  }
    public RoundGradientPaint(float x, float y, Color c, Point2D r, Color c1, int f) {
        set(x, y, c, r, c1, f);
    }
    public void set(float x, float y, Color c, Point2D r, Color c1, int f) {
        if (r.distance(0, 0) <= 0)
            throw new IllegalArgumentException("Radius must be greater than zero.");
        mPoint = new Point2D.Float(x, y);
        mPointColor = c;
        mRadius = r;
        mBackgroundColor = c1;
        flareLevel = f;
    }
    @Override
    public PaintContext createContext(ColorModel cm, Rectangle deviceBounds, Rectangle2D userBounds,  AffineTransform xform, RenderingHints hints) {
        Point2D transformedPoint = xform.transform(mPoint, null);
        Point2D transformedRadius = xform.deltaTransform(mRadius, null);
        if (rgc == null)
            rgc = new RoundGradientContext(transformedPoint, mPointColor, transformedRadius, mBackgroundColor, flareLevel);
        else
            rgc.set(transformedPoint, mPointColor, transformedRadius, mBackgroundColor, flareLevel);

        return rgc;
    }
    @Override
    public int getTransparency() {
        int a1 = mPointColor.getAlpha();
        int a2 = mBackgroundColor.getAlpha();
        return (((a1 & a2) == 0xff) ? OPAQUE : TRANSLUCENT);
    }
}

