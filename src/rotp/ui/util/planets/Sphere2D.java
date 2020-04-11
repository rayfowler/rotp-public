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

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import rotp.model.planet.Planet;
import rotp.util.Base;
import rotp.util.FastImage;

public class Sphere2D implements Base {
	private final int radius;
	private final FastImage mapImage;
	public static final int FAST_PLANET_R = 50;
	public static final int SMALL_PLANET_R = 100;
	public static Map<Integer, FastImage> cachedFastOvalImg = new HashMap<>();
	public static Map<Integer, FastImage> cachedFastGlobeImg = new HashMap<>();

	public Sphere2D(FastImage terrainImg, FastImage cloudImg, Planet p) {
		FastImage compositeImg = PlanetImager.createCompositeImage(terrainImg, p);
		radius = terrainImg.getHeight()/2;
		mapImage = compositeImg;
	}

	public int radius()                 { return radius; }
	public FastImage mapImage()         { return mapImage; }

	private FastImage largeVisibleSphere(float pct, int r) {
		return visibleSphere(mapImage(), radius, pct);
	}
	private FastImage visibleSphere(FastImage baseImg, int r, float pct) {
		float p0 = pct - (int) pct;

		int h = 2*r;
		int w = (int) (r*Math.PI);
		if (!cachedFastOvalImg.containsKey(w))
			cachedFastOvalImg.put(w,FastImage.sized(w, h));
		FastImage img = cachedFastOvalImg.get(w);

		int x1Mid = (int) (0.5*r*Math.PI);
		int x0Mid = (int) (r*Math.PI);

		int imgMaxW = img.getWidth()-1;
		int baseImgMaxW = baseImg.getWidth()-1;

		for (int y=0;y<h;y++) {
			int xInt = xIntercept(y,p0,r);
			int x0Min = xIntercept(y,0,r);
			int x0Max = xIntercept(y,1,r);
			int x1Min = xIntercept(y,0.25f,r);
			int x1Max = xIntercept(y,0.75f,r)+1;
//            System.out.println("  y="+y+"   xInt:"+xInt+"   x0Min/Max:"+x0Min+"/"+x0Max+" x1MinMax:"+x1Min+"/"+x1Max);
			for (int x=x1Min;x<x1Max;x++) {
				int x1 = x+xInt;   // ostensibly the pixel we need from the full image
//                System.out.print("x:"+x+"            x0:"+x1);
				if      (x1<x0Min) {
//                    System.out.println("x1<x0min");
					x1=x0Max+x1-x0Min;
				} // check if in the fullImage oval
				else if (x1>=x0Max) {
//                    System.out.println("x1>x0max");
					x1=x1+x0Min-x0Max;
				}

//                System.out.println("         final:"+x1+"   with XbBase:"+(x1+xBase));

				int xb = x+x1Mid;
				if (xb < 0) xb = 0;
				else if (xb > imgMaxW) xb = imgMaxW;

				int x1b = x1+x0Mid;
				if (x1b < 0) x1b = 0;
				else if (x1b > baseImgMaxW) x1b = baseImgMaxW;

				img.setRGB(xb, y, baseImg.getRGB(x1b, y));
			}
		}
		return img;
	}
	public FastImage image(float pct) {
		return image(pct, radius);
	}
	private FastImage image(float pct, int r) {
		float p0 = pct - (int) pct;

		FastImage img0 = largeVisibleSphere(p0, r);
		int x0Mid = img0.getWidth()/2;

		int h = 2*r;
		int w = h;

		if (!cachedFastGlobeImg.containsKey(w))
			cachedFastGlobeImg.put(w,FastImage.sized(w, h));
		FastImage img =  cachedFastGlobeImg.get(w);
		//long t0 = System.currentTimeMillis();

		float pi = (float) Math.PI;
		float x1MinPct = 0.5f-(1/(2*pi));
		float x1MaxPct = 0.5f+(1/(2*pi));
		int x1Mid = r;

		for (int y=0;y<h;y++) {
			int x0Max = xIntercept(y,0.75f, r);
			int x1Min = xIntercept(y,x1MinPct, r);
			int x1Max = xIntercept(y,x1MaxPct, r);
			int prevx0 = -1;
			for (int x1=x1Min;x1<x1Max;x1++) {
				float x1Pct = (float)x1/x1Max;
				float x0Radians = asin(x1Pct);
				float x0Pct = (pi-(2*x0Radians))/pi;
				int x0 = x0Max-(int)(x0Pct*x0Max);
				int numpx = (prevx0 < 0) ? 1 : x0-prevx0;
				img.setRGB(x1+x1Mid, y, mergedRGB(img0,x0+x0Mid+1-numpx, y, numpx));
				prevx0 = x0;
			}
		}
		//long t1 = System.currentTimeMillis();
		//log("t1:"+(t1-t0));

		return img;
	}

	private int xIntercept(int y1, float pct, int r) {
		// returns an x DELTA from the center line

		if (pct == 0.5)
			return 0;

		float p0 = (2*pct)-1;

		// returns the x-intercept for a given y value, when the center is set to pct

		//   horizontal radius of the arc
		float a = p0*r*(float)Math.PI;

		// vertical radius of arc
		float tmp = (float)(y1-r)/r;
		int x1 = (int)(a*Math.sqrt(1-(tmp*tmp)));
		return x1;
	}
	protected void smoothEdges(FastImage img, float pct, float x0Start, float x1Start, float size) {
		int h = img.getHeight();
		int w = img.getWidth();

		int r = radius;
		for (int y=0;y<h;y++) {
			int x0Mid = (int) (x0Start+(int)(size*r*Math.PI));
			int x0Min = Math.max(0, x0Mid+xIntercept(y,0.5f-(size/2), r)-2);
			int x0Max = Math.min(w-1,x0Mid+xIntercept(y,0.5f+(size/2), r)+2);
			int x1Mid = (int) (x1Start+(int)(size*r*Math.PI));
			int x1Min = Math.max(0, x1Mid+xIntercept(y,0.5f-(size/2), r)-2);
			int x1Max = Math.min(w-1,x1Mid+xIntercept(y,0.5f+(size/2), r)+2);

			int pts = (int) (pct*(x0Max-x0Min)/2);   //100% smooth goes to midpoint
//            System.out.println("x0:"+x0Min+"/"+x0Mid+"/"+x0Max+"   x1:"+x1Min+"/"+x1Mid+"/"+x1Max);
//            System.out.println("img w:"+fullImage.getWidth()+"  y="+y+"   pts:"+pts+"   x0Min/Max:"+x0Min+"/"+x0Max);
//            pts=0;

			// blend x0 first
			for (int n=0;n<pts;n++) {
				// at n=0, 50% shared each... at n=pts... 100/0%.. at others pts+n/(2*pts)
				float blendPct = (float)(pts+n)/(pts+pts);
				if (((x0Min+n) < w) && ((x1Max-n) < w)) {
					int p0 = img.getRGB(x0Min+n,y);
					int p1 = img.getRGB(x1Max-n,y);
					img.setRGB(x0Min+n,y,blendedRGB(p0,p1,blendPct));
					img.setRGB(x1Max-n,y,blendedRGB(p1,p0,blendPct));
				}
			}
			// if size < 1, blend x1
			if (size < 1)
				for (int n=0;n<pts;n++) {
					// at n=0, 50% shared each... at n=pts... 100/0%.. at others pts+n/(2*pts)
					float blendPct = (float)(pts+n)/(pts+pts);
					int p0 = img.getRGB(x1Min+n,y);
					int p1 = img.getRGB(x0Max-n,y);
					img.setRGB(x1Min+n,y,blendedRGB(p0,p1,blendPct));
					img.setRGB(x0Max-n,y,blendedRGB(p1,p0,blendPct));
				}
		}
	}
	protected void smoothEdges(BufferedImage img, float pct, int r) {
		int h = img.getHeight();

		for (int y=0;y<h;y++) {
			int x0Mid = (int) (r*Math.PI);
			int x0Min = x0Mid+xIntercept(y,0,r);
			int x0Max = x0Mid+xIntercept(y,1,r)-1;
			int pts = (int) (pct*(x0Max-x0Min)/2);   //100% smooth goes to midpoint
//            System.out.println("img w:"+fullImage.getWidth()+"  y="+y+"   pts:"+pts+"   x0Min/Max:"+x0Min+"/"+x0Max);
//            pts=0;
			for (int n=0;n<pts;n++) {
				// at n=0, 50% shared each... at n=pts... 100/0%.. at others pts+n/(2*pts)
				float blendPct = (float)(pts+n)/(pts+pts);
				int p0 = img.getRGB(x0Min+n,y);
				int p1 = img.getRGB(x0Max-n,y);
				img.setRGB(x0Min+n,y,blendedRGB(p0,p1,blendPct));
				img.setRGB(x0Max-n,y,blendedRGB(p1,p0,blendPct));
			}
		}
	}
	protected int blendedRGB(int p0, int p1, float pct0) {
		// pct of p0, (1-pct) of p1
		float pct1 = 1 - pct0;

		int a0 = p0 >> 24 & 0xff;
		int r0 = p0 >> 16 & 0xff;
		int g0 = p0 >> 8 & 0xff;
		int b0 = p0 >> 0 & 0xff;
		int a1 = p1 >> 24 & 0xff;
		int r1 = p1 >> 16 & 0xff;
		int g1 = p1 >> 8 & 0xff;
		int b1 = p1 >> 0 & 0xff;

		int ax = (int) ((pct0*a0)+(pct1*a1));
		int rx = (int) ((pct0*r0)+(pct1*r1));
		int gx = (int) ((pct0*g0)+(pct1*g1));
		int bx = (int) ((pct0*b0)+(pct1*b1));

		return (ax << 24)+(rx << 16)+(gx << 8)+bx;
	}

	protected int mergedRGB(FastImage img, int x, int y, int numX) {
		//if (numX == 0)
		//	return 0;
		if (numX < 2)
			return img.getRGB(x,y);

		int a1 = 0;
		int r1 = 0;
		int g1 = 0;
		int b1 = 0;

		for (int i=0;i<numX;i++) {
			int p0 = img.getRGB(x+i,y);
			a1 += (p0 >> 24 & 0xff);
			r1 += (p0 >> 16 & 0xff);
			g1 += (p0 >> 8 & 0xff);
			b1 += (p0 >> 0 & 0xff);
		}
		return (a1/numX << 24)+(r1/numX << 16)+(g1/numX << 8)+b1/numX;
	}
	private FastImage squishCloudsToOval(FastImage clouds, FastImage base) {
		// must be same size as terrain base image
		if ((clouds.getWidth() != base.getWidth())
				|| (clouds.getHeight() != base.getHeight()))
			return clouds;

		for (int y=0;y<base.getHeight();y++) {
			int x0 = -1;
			int x1 = -1;
			for (int x=0;x<base.getWidth();x++) {
				if (base.getAlpha(x, y) == 255) {
					x0 = x;
					break;
				}
			}
			for (int x=base.getWidth()-1;x>=0;x--) {
				if (base.getAlpha(x, y) == 255) {
					x1 = x;
					break;
				}
			}
			clouds.squishRow(y, x0, x1);
		}

		return clouds;
	}
}
