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

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import rotp.Rotp;
import rotp.ui.UserPreferences;

public enum AnimationManager implements Base {
    INSTANCE;
    public static AnimationManager current()  { return INSTANCE; }

    private static final String explosionBaseDir = "images/explosions/";
    private static final String explosionListFile = "images/explosions/listing.txt";
    private static final String animationListFile = "data/animations.txt";

    private static final HashMap<Integer,List<BufferedImage>> cachedImages = new HashMap<>();
    private static final HashMap<Integer,List<BufferedImage>> activeImages = new HashMap<>();


    private final HashMap<String, Animation> animations = new HashMap<>();
    private final HashMap<String, List<String>> explosions = new HashMap<>();

    private static int imgKey(int w, int h) {
        return (w*1000)+h;
    }
    private AnimationManager() {
        loadAnimationList(animationListFile);
        loadExplosions();
    }
    public boolean animationsDisabled()  { return lowMemory(); }
    @Override
    public boolean playAnimations()   { return UserPreferences.playAnimations() && !animationsDisabled(); }
    public void addAnimation(String key, Animation anim) {
        animations.put(key, anim);
    }
    public void reset(String animationSpecifier) {
        List<String> specs = this.substrings(animationSpecifier, ',');
        String key = specs.get(0);

        Animation anim = animations.get(key);
        if (anim != null)
            anim.reset();
    }
    public List<Image> allImages(String key) {
        List<Image> results = new ArrayList<>();
        if ((key == null) || key.isEmpty())
            return results;

        Rectangle area = null;
        Animation anim = animations.get(key);

        for (AnimationImage imgSpec: anim.images) {
            for (AnimationImageFrame frame: imgSpec.frames) 
                results.add(frame.image());
        }
        return results;
    }
    @Override
    public void allFrames(String animationSpecifier, int cnt, int imgIndex, List<Image> frames, List<Integer> refs) {
        if ((animationSpecifier == null) || animationSpecifier.isEmpty())
            return;

        List<String> specs = substrings(animationSpecifier, ',');
        String key = specs.get(0);
        Rectangle area = null;

        try {
            area = specs.size() > 1 ? parseArea(specs.get(1)) : null;
        } catch (Exception e) {
            err(e.getMessage());
        }

        // get an animation with this name
        Animation anim = animations.get(key);
        if (anim == null)
            return;
             
        // get the animation image for the specified index
        List<AnimationImage> imageSpecs = anim.images;        
        if (imgIndex > (imageSpecs.size()-1))
            return;

        AnimationImage imgSpec = imageSpecs.get(imgIndex);
        
        List<AnimationImageFrame> imgFrames = imgSpec.frames;
        
        for (AnimationImageFrame imgFrame: imgFrames) {
            frames.add(image(imgFrame.imageKey));
            refs.add(imgFrame.displayCount());
        }
        return;
    }
    @Override
    public BufferedImage currentFrame(String animationSpecifier) {
        return currentFrame(animationSpecifier, null);
    }
    @Override
    public BufferedImage currentFrame(String animationSpecifier, List<String> exclusionKeys) {
        if ((animationSpecifier == null) || animationSpecifier.isEmpty())
            return null;

        List<String> specs = substrings(animationSpecifier, ',');
        String key = specs.get(0);
        Rectangle area = null;

        try {
            area = specs.size() > 1 ? parseArea(specs.get(1)) : null;
        } catch (Exception e) {
            err(e.getMessage());
        }

        BufferedImage result;
        Animation anim = animations.get(key);
        if (anim != null)
            result = anim.currentFrame(exclusionKeys);
        else 
            result = newFullScreenImage(ImageManager.current().image(key));

        // err if no animation or image found matching the requested key
        if (result == null)
            throw new RuntimeException("Invalid animation requested. key: "+key);

        // use subimage if requested
        if (area != null)
            result = result.getSubimage(area.x, area.y, area.width, area.height);

        return result;
    }
    @Override
    public List<BufferedImage> allExplosionFrames(String name) {
        if (!explosions.containsKey(name))
            return null;

        List<String> fileNames = explosions.get(name);
        if (fileNames.isEmpty())
            return null;

        List<BufferedImage> result = new ArrayList<>();
        for (String fileName: fileNames) {
            Image baseImg = icon(fileName).getImage();
            BufferedImage targetImg = newFullScreenImage(1200,1200);
            int w = targetImg.getWidth();
            int h = targetImg.getHeight();
            Graphics g = targetImg.getGraphics();
            setFontHints(g);
            g.drawImage(baseImg,0,0,w,h,null);
            g.dispose();
            result.add(targetImg);
        }
        return result;
    }
    public void loadAnimationList(String filename) {
        log("Loading Animations: ", filename);
        BufferedReader in = reader(filename);
        if (in == null)
            return;

        try {
            String input;
            while ((input = in.readLine()) != null)
                loadAnimationDefinition(input.trim());
            in.close();
        }
        catch (IOException e) {
            err("AnimationManager.loadAnimationList -- IOException: " + e);
        }
    }
    private void loadAnimationDefinition(String line) {
        if (isComment(line))
            return;

        List<String> vals = substrings(line, '|');
        if (vals.size() < 2) {
            err("Not enough fields for image line: ", line);
            return;
        }
        addAnimation(vals.get(0), new Animation(vals.get(1)));
    }
    private void loadExplosions() {
        log("Loading Explosions...");
        BufferedReader in = reader(explosionListFile);
        if (in == null)
            return;
        try {
            String input;
            while ((input = in.readLine()) != null) {
                if (!isComment(input))
                    loadExplosionImageDirectory(input.trim());
            }
            in.close();
        }
        catch (IOException e) {
            err("AnimationManager.loadExplosions -- IOException: ", e.toString());
        }
    }
    private void loadExplosionImageDirectory(String dirName) {
        String dirPath = explosionBaseDir+dirName+"/";
        BufferedReader in = reader(dirPath+"listing.txt");
        List<String> fileNames = new ArrayList<>();
        if (in == null)
            return;
        try {
            String input;
            while ((input = in.readLine()) != null) {
                if (!isComment(input))
                    fileNames.add(dirPath+input.trim());
            }
            in.close();
        }
        catch (IOException e) {
            err("AnimationManager.loadExplosionImageDirectory -- IOException: ", e.toString());
        }
        explosions.put(dirName, fileNames);
    }

    private Rectangle parseArea(String s) throws Exception {
        if (s.isEmpty() || s.contains("ALL"))
            return null;

        List<String> vals = substrings(s,'|');

        List<String> xy = substrings(vals.get(0),'@');

        if ((xy.size() != 2))
            throw new Exception(concat("Invalid size specification-1: [", s, "] for animation"));

        int x = this.parseInt(xy.get(0));
        int y = this.parseInt(xy.get(1));

        if ((x<0) || (y<0))
                throw new Exception(concat("Invalid size specification-2: [", s, "] for animation"));
        int w = 0;
        int h = 0;

        if (vals.size() > 1) {
            List<String> wh = substrings(vals.get(1),'x');
            if (wh.size() != 2)
                throw new Exception(concat("Invalid size specification-3: [", s, "] for animation"));

            w = this.parseInt(wh.get(0));
            h = this.parseInt(wh.get(1));

            if ((w<1) || (h<1))
                throw new Exception(concat("Invalid size specification-4: [", s, "] for animation"));
        }
        return new Rectangle(x,y,w,h);
    }
    class Animation implements Base {
        String filename;
        int stepRate = 1;
        List<AnimationImage> images = new ArrayList<>();
        public Animation(String fn) {
            loadDefinition(fn);
        }
        public void reset() {
            for (AnimationImage img: images)
                img.reset();
        }
        public BufferedImage frame(int index) {
            BufferedImage resultImg = null;
            for (AnimationImage image : images)
                resultImg = image.drawFrame(resultImg, index);
            return resultImg;
        }
        public BufferedImage currentFrame() {
            BufferedImage resultImg = null;
            for (AnimationImage image: images)
                resultImg = image.drawCurrentFrame(resultImg, null);
            return resultImg;
        }
        public BufferedImage currentFrame(List<String> exclusionKeys) {
            BufferedImage resultImg = null;
            for (AnimationImage image: images)
                resultImg = image.drawCurrentFrame(resultImg, exclusionKeys);
            return resultImg;
        }
        private void loadDefinition(String fn) {
            filename = fn;
            BufferedReader in = reader(fn);
            if (in == null)
                return;

            try {
                String input;
                while ((input = in.readLine()) != null)
                    loadAnimationDefinitionLine(input.trim());
                in.close();
            }
            catch (IOException e) {
                err("AnimationManager.Animation.loadDefinition -- IOException: " + e);
            }
        }
        private void loadAnimationDefinitionLine(String line) {
            if (isComment(line))
                return;

            List<String> vals = substrings(line, ',');

            // check for setting the step rate
            // note that you could arguably have different step rates
            // for images if you interspersed them in the anim def'n
            if (vals.get(0).equalsIgnoreCase("Skip")) {
                stepRate = parseInt(vals.get(1));
                return;
            }
            if (vals.size() < 3) {
                err("Not enough fields for animation file line: ", line);
                return;
            }
            String key = vals.get(0);
            Rectangle area = null;
            try {
                area = parseArea(vals.get(1));
            } catch (Exception e) {
                err(e.getMessage(), " in animation file: ", filename);
            }
            List<String> imageSpecs = substrings(vals.get(2),'+');
            AnimationImage img = new AnimationImage(key, stepRate, area, imageSpecs);
            images.add(img);
        }
    }
    class AnimationImage implements Base {
        String key;
        Rectangle area;
        List<AnimationImageFrame> frames = new ArrayList<>();
        int framesToSkip = 0;
        int maxDuration = 0;
        int minDuration = 0;
        int frameIndex = -1;
        int frameRemainingCount = 0;
        long prevAnimationCount = 0;
        int skipsRemaining = 0;
        public AnimationImage(String s, int skip, Rectangle rect, List<String> frameSpecs) {
            key = s;
            framesToSkip = max(0, skip); // can't be < 0
            skipsRemaining = framesToSkip;
            area = rect;

            for (String frameSpec: frameSpecs) {
                //log("AnimationImage: "+key+"  frame:"+frameSpec+"   area:"+area);
                AnimationImageFrame frame = new AnimationImageFrame(frameSpec);
                frames.add(frame);
                minDuration += frame.msLo;
                maxDuration += frame.msHi;
            }
            goToNextFrame();
        }
        public void reset() {
            frameIndex = -1;
            frameRemainingCount = 0;
            prevAnimationCount = 0;
            skipsRemaining = framesToSkip;
        }
        public BufferedImage drawCurrentFrame(BufferedImage resultImg, List<String> exclusionKeys) {
            // if we are excluding this animation, result the default first frame
            skipsRemaining--;
            if ((exclusionKeys != null) && exclusionKeys.contains(key))
                return drawFrame(resultImg, 0);
            findCurrentFrame();
            return drawFrame(resultImg, frameIndex);
        }
        public BufferedImage drawFrame(BufferedImage resultImg, int index) {
            // get image of current frame
            Image frameImg = skipsRemaining > 0 ? null : currentFrame(index).image();
            // if resultImg==null, returns frame image
            if (resultImg == null)
                resultImg = current().newFullScreenImage(Rotp.IMG_W,Rotp.IMG_H);

            if (frameImg == null)
                return resultImg;

            Graphics2D g = resultImg.createGraphics();
            setFontHints(g);
            // draws correct image frame on img
            if (area == null)
                //resultImg.drawImage(frameImg, 0, 0);
                // if no area specified, draw full image at 0,0
                g.drawImage(frameImg, 0, 0, null);
            else if (area.width == 0)
                //resultImg.drawImage(frameImg, area.x, area.y);
                // if area has no w/h, draw full image at x,y
                g.drawImage(frameImg, area.x, area.y, null);
            else
                g.drawImage(frameImg, area.x, area.y, area.x+area.width, area.y+area.height, area.x, area.y, area.x+area.width, area.y+area.height, null);
            g.dispose();
            return resultImg;
        }
        private void findCurrentFrame() {
            if (skipsRemaining > 0)
                return;

            if (!playAnimations())  {
                frameIndex = 0;
                return;
            }

            if ((frames.size() == 1)
            || (maxDuration == 0)) {
                frameIndex = 0;
                //log("frame:"+key+"  ms:"+currentMs+"  frames:"+frames.size()+"  maxDur:"+maxDuration+"   area:"+area);
                return;
            }

            frameRemainingCount--;
            if (frameRemainingCount < 1)
                goToNextFrame();
        }
        private AnimationImageFrame currentFrame(int index) {
            return frames.get(index);
        }
        private void goToNextFrame() {
            frameIndex = (frameIndex+1) % frames.size();
            frameRemainingCount = currentFrame(frameIndex).displayCount();
        }
    }
    class AnimationImageFrame implements Base {
        String imageKey;
        int msLo;
        int msHi;
        public AnimationImageFrame(String spec) {
            parseSpec(spec);
        }
        public Image image() {
            return image(imageKey);
        }
        public int displayCount() {
            return roll(msLo,msHi);
        }
        private void parseSpec(String spec) {
            List<String> vals = this.substrings(spec, ':');
            imageKey = vals.get(0);

            if (!ImageManager.current().valid(imageKey)) 
                err("Animation references invalid image key: ", imageKey);

            msLo = vals.size() < 2 ? 1 : Math.max(0,parseInt(vals.get(1)));
            msHi = vals.size() < 3 ? msLo :  Math.max(0,parseInt(vals.get(2)));
            if (msLo > msHi) {
                err("Invalid animation image specification-2: [", spec, "]");
                msHi = msLo;
            }
        }
    }
    public static void reclaimImages() {
        List<Integer> keys = new ArrayList<>(activeImages.keySet());
        for (int key: keys) {
            cachedImages.get(key).addAll(activeImages.get(key));
            activeImages.get(key).clear();
        }
    }
    private BufferedImage newFullScreenImage(Image srcImg) {
        return newFullScreenImage(srcImg, srcImg.getWidth(null), srcImg.getHeight(null));
    }
    private BufferedImage newFullScreenImage(int w, int h) {
        return newFullScreenImage(null, w, h);
    }
    private BufferedImage newFullScreenImage(Image srcImg, int w, int h) {
        BufferedImage newImg = newBufferedImage(w, h);
        // copy srcIMg into it
        if (srcImg != null) {
            final Runnable copyImgThread = new Runnable() {
                @Override
                public void run() {
                    Graphics2D g = (Graphics2D) newImg.getGraphics();
                    setFontHints(g);
                    g.drawImage(srcImg, 0, 0, w, h, 0, 0, srcImg.getWidth(null), srcImg.getHeight(null), null);
                    g.dispose();
                }
            };
            invokeAndWait(copyImgThread);
        }
        return newImg;
    }
}
