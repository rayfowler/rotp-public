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
package rotp.util;

import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.PixelGrabber;
import java.awt.image.Raster;
import rotp.model.planet.PlanetHeightMap;
import rotp.ui.UserPreferences;

public class FastImage {
    private static final BufferedImage PROTOTYPE = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    private int w;
    private int h;
    private int[] pixels;
    public BufferedImage image() { 
        if (pixels == null)
            return null;
        return new BufferedImage(
                PROTOTYPE.getColorModel(),
                Raster.createWritableRaster(
                        PROTOTYPE.getSampleModel().createCompatibleSampleModel(w, h),
                        new DataBufferInt(pixels, pixels.length),
                        new Point()),
                false,
                null);
    }
    public void image(Image img)  { 
        if (img == null)
            pixels = null;
        else {
            w = img.getWidth(null);
            h = img.getHeight(null);
            pixels = new int[w*h];
            try {
                new PixelGrabber(img, 0, 0, w, h, pixels, 0, w).grabPixels();
            }
            catch (InterruptedException localInterruptedException) { }
        }
    }
    public static FastImage from(Image img) {
        return img == null ? null : new FastImage(img);
    }
    public static FastImage fromPixels(int[] px, int w, int h) {
        return new FastImage(px, w, h);
    }
    public static FastImage sized(int w, int h) {
        return new FastImage(w,h);
    }
    public boolean equals(FastImage img) {
        if (img == this)
            return true;
        if (img == null)
            return false;
        if (getHeight() != img.getHeight())
            return false;
        if (getWidth() != img.getWidth())
            return false;
        for (int x=0;x<getWidth();x++) {
            for (int y=0;y<getHeight();y++) {
                if (getRGB(x, y) != img.getRGB(x,y))
                        return false;
            }
        }
        return true;
    }
    public FastImage copy() {
        int[] newPx = new int[pixels.length];
        for (int i=0;i<pixels.length;i++)
            newPx[i]=pixels[i];
        return FastImage.fromPixels(newPx, w, h);
    }
    public static FastImage fromExplosionImage(BufferedImage img) {
        FastImage fi = new FastImage(img);
        int w = fi.getWidth();
        int h = fi.getHeight();
        for (int x=0;x<w;x++) {
            for (int y=0;y<h;y++) {
                int px = fi.getRGB(x, y);
                int r = px >> 16 & 0xff;
                int g = px >> 8 & 0xff;
                int b = px & 0xff;
                // move blue channel to alpha and set blue to 0
                int newPx = (b << 24)+(r << 16)+(g << 8)+0;
                fi.setRGB(x, y, newPx);
            }
        }
        return fi;
    }
    public static FastImage fromHeightMap(PlanetHeightMap map) {
        int w = map.width();
        int h = map.height();
        int[] px = new int[w*h];
        // convert each byte value (-128 to 127)
        // to integers representing grayscale (0-255 for r,g,b)
        for (int x=0;x<w;x++) {
            for (int y=0;y<h;y++) {
                int index = indexPosn(x,y,w); // find 1D index 
                int v = map.col(x, y);
                int alpha = 255;
                if (v == Byte.MIN_VALUE)
                    alpha = 0;
                int pxV = v-Byte.MIN_VALUE;    // convert to 0-255
                int newPixel = (alpha << 24)+(pxV << 16)+(pxV << 8)+pxV;
                px[index] = newPixel;
            }
        }
        //System.out.println("trans pixels: "+transPx);
        return new FastImage(px, w, h);
    }
    public int getWidth()                    { return w; }
    public int getHeight()                   { return h; }
    public int getRGB(int x, int y)          { return ((y*w)+x < 0) ? 0 : pixels[(y*w)+x]; }
    public int getAlpha(int x, int y)        { return getRGB(x,y) >> 24 & 0xff;	}

    public void setRGB(int x, int y, int px) { 
        if ((x < w) && (y < h))
            pixels[(y*w)+x] = px; 
    }
    // floats w & h, interpolating missing pixels
    public FastImage smoothScale(int newW, int newH) {
        if ((w != newW) || (h != newH))
            image(image().getScaledInstance(newW, newH, Image.SCALE_SMOOTH));
        return this;
    }
    public void squishRow(int y, int x0, int x1) {
        int w = getWidth();
        int w0 = x1-x0+1;
        // nothing to squish
        if (w == w0)
            return;

        int nullPx = (0 << 24)+(0 << 16)+(0 << 8)+0;
        // "squish" entire row
        if (x0 < 0) {
            for (int x=0;x<w;x++)
                setRGB(x, y, nullPx);
            return;
        }

        int removeCount = w - w0;
        float n = (float) w / removeCount;  
        // ex: if w=1000 and removeCnt = 30, then skip every n=33.33rd pixel

        // "squish" by radiating out from center, removing every nth pixel
        // radiate from center to left
        float cnt = n/2;
        float targetCnt = n;
        int newX = w/2;
        for (int x=w/2; x>=0; x--) {
            if ((cnt >= targetCnt) && (x > 0)) {
                x--;
                targetCnt += n;
            }
            setRGB(newX,y,getRGB(x,y));
            newX--;
            cnt++;
        }
        // radiate from center to right
        cnt = n/2;
        targetCnt = n;
        newX = w/2;
        for (int x=w/2; x<w; x++) {
            if ((cnt >= targetCnt) && (x<(w-1))) {
                x++;
                targetCnt += n;
            }
            setRGB(newX,y,getRGB(x,y));
            newX++;
            cnt++;
        }

        // null out px left of x0 and right of x1
        for (int x=0;x<x0;x++)
            setRGB(x,y,nullPx);
        for (int x=x1+1;x<w;x++)
            setRGB(x,y,nullPx);
    }
    public void clip(Rectangle r) {
        if (r == null)
            return;

        int px = 0;
        int[] newPixels = new int[r.width * r.height];
        for (int y=0;y<r.height;y++) {
            for (int x=0;x<r.width;x++) 
                newPixels[px++] = getRGB(r.x+x, r.y+y);
        }

        pixels = newPixels;
        w = r.width;
        h = r.height;
    }
    public void drawImage(Image img, int x0, int y0) {
        drawImage(img, x0, y0, null);
    }
    public void drawImage(Image img, int x0, int y0, Rectangle bounds) {
        FastImage fImg = FastImage.from(img);
        fImg.clip(bounds);
        int x = x0;
        int y = y0;
        for (int x1=0;x1<fImg.w;x1++) {
            for (int y1=0;y1<fImg.h;y1++) 
                setRGB(x+x1,y+y1,fImg.getRGB(x1,y1));
        }
    }


    private static int indexPosn(int x, int y, int w0) {
        return (y * w0) + x;
    }
    private FastImage(Image img) {
        logImage(img.getWidth(null),img.getHeight(null));
        image(img);
    }
    private FastImage(int w0, int h0) {
        logImage(w0,h0);
        w = w0;
        h = h0;
        pixels = new int[w*h];
    }
    private FastImage(int[] px, int w0, int h0) {
        logImage(w0,h0);
        w = w0;
        h = h0;
        pixels = px;
    }
    private static void logImage(int w, int h) {
        if (!UserPreferences.showMemory())
            return;
    }
}
