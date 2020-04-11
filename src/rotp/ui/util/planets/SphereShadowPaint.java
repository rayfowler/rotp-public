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
package rotp.ui.util.planets;

import java.awt.Color;
import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;

public class SphereShadowPaint implements Paint {
	protected Point2D mPoint, mPoint0;
	protected Point2D mRadius, mRadius0;
	protected Color mShadowColor;

	/*
	public SphereShadowPaint(float x, float y, float x0, float y0, Color c, Point2D r, Point2D r0) {
		set(x, y, x0, y0, c, r, r0);
	}

	public SphereShadowPaint(float x, float y, Point2D r, Color c, int dir, float pct) {
		set(x, y, r, c, dir, pct);
	}
	*/
	public void set(float x, float y, Point2D r, Color c, int dir, float pct) {
		if (r.distance(0, 0) <= 0)
			throw new IllegalArgumentException("Radius must be greater than 0.");
		mPoint = new Point2D.Float(x, y);
		mRadius = r;
		mShadowColor = c;

		float rdist = (float) r.distance(0, 0);
		float dist = rdist * 4 * pct;
		float rad = (float)Math.toRadians(dir);  // convert from degrees
		float p2X = x+((float)Math.cos(rad) * dist);
		float p2Y = y-((float)Math.sin(rad) * dist);
		mPoint0 = new Point2D.Float(p2X,p2Y);
		mRadius0 = new Point2D.Float(rdist+dist/2,0);
	}

	public void set(float x, float y, float x0, float y0, Color c, Point2D r, Point2D r0) {
		if (r.distance(0, 0) <= 0)
			throw new IllegalArgumentException("Radius must be greater than 0.");
		mPoint = new Point2D.Float(x, y);
		mPoint0 = new Point2D.Float(x0, y0);
		mShadowColor = c;
		mRadius = r;
		mRadius0 = r0;
	}
	@Override
	public PaintContext createContext(ColorModel cm,
									  Rectangle deviceBounds, Rectangle2D userBounds,
									  AffineTransform xform, RenderingHints hints) {
		Point2D xPoint = xform.transform(mPoint, null);
		Point2D xPoint0 = xform.transform(mPoint0, null);
		Point2D xRadius = xform.deltaTransform(mRadius, null);
		Point2D xRadius0 = xform.deltaTransform(mRadius0, null);
		SphereShadowContext.current().set(xPoint, xPoint0, mShadowColor, xRadius, xRadius0);
		return SphereShadowContext.current();
	}

	@Override
	public int getTransparency() {
		int a1 = mShadowColor.getAlpha();
		int a2 = 0;
//      return TRANSLUCENT;
		return (((a1 & a2) == 0xff) ? OPAQUE : TRANSLUCENT);
	}
}

