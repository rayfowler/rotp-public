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
package rotp.util.sound;

import java.io.BufferedInputStream;
import rotp.util.Base;
import javax.sound.sampled.*;
import static javax.sound.sampled.FloatControl.Type.MASTER_GAIN;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import rotp.Rotp;

public class WavClip  implements SoundClip, Base {
    static HashMap<String, WavClip> loadedClips = new HashMap<>();
    Clip clip;
    boolean loaded = false;
    String filename;
    float gain;
    int position = 0;
    boolean continuous = false;
    String style = "";
    
    public static WavClip play(String fn, float clipGain, float masterVolume) {
        if (!loadedClips.containsKey(fn)) 
            loadedClips.put(fn, new WavClip(fn, clipGain));
        
        WavClip wc = loadedClips.get(fn);
        wc.setVolume(masterVolume);
        wc.play();
        return wc;
    }
    public static WavClip playContinuously(String fn, float clipGain, String s, float masterVolume) {
         if (!loadedClips.containsKey(fn)) 
            loadedClips.put(fn, new WavClip(fn, clipGain));
        
        WavClip wc = loadedClips.get(fn);
        wc.setVolume(masterVolume);
        wc.style = s;
        wc.playContinuously();
        return wc;
    }          
    public static void setVolume(String fn, float vol) {
         if (!loadedClips.containsKey(fn))
             return;
        
        WavClip wc = loadedClips.get(fn);
        wc.setVolume(vol);
    }          
    public WavClip(String fn, float vol) {
        filename = fn;
        gain = vol;
        loaded = false;
        
        AudioInputStream ais = null;
        try {
            if (!loaded) {
                BufferedInputStream is = new BufferedInputStream(wavFileStream(fn));
                ais = AudioSystem.getAudioInputStream(is);
                DataLine.Info info = new DataLine.Info(Clip.class, ais.getFormat());
                clip = (Clip)AudioSystem.getLine(info);
                clip.open(ais);
                if (vol < 1 && clip.isControlSupported(MASTER_GAIN)) {
                    log("setting gain for sound: "+filename+"  to "+(int)(gain*100));
                    FloatControl gain = (FloatControl) clip.getControl(MASTER_GAIN);
                    gain.setValue(20f * (float) Math.log10(vol));
                }
                loaded = true;
            }
        }
        catch (IOException | LineUnavailableException | UnsupportedAudioFileException e) {
            System.err.println(e.toString());
            System.err.println(e.getStackTrace());
        }
        finally {
            if (ais != null)
                try { ais.close(); } catch (IOException e) {}
        }
    }
    public void setVolume(float masterVolume) {
        if (!clip.isControlSupported(MASTER_GAIN))
            return;
        
        float volume = min(1.0f, masterVolume*gain);
        log("setting volume*gain for sound: "+filename+"  to "+(int)(volume*100));
        FloatControl gain = (FloatControl) clip.getControl(MASTER_GAIN);
        gain.setValue(20f * (float) Math.log10(volume));
    }
    public void play() {
        clip.setFramePosition(position);
        clip.start();
    }
    public void playContinuously() {
        continuous = true;
        if (style.equals("L"))
            clip.setFramePosition(0);
        else {
            try { clip.setFramePosition(position); }
            catch(IllegalArgumentException e) {
                // thrown if invalid frame position
                clip.setFramePosition(0);
            }
        }
        clip.start();
        clip.loop(Clip.LOOP_CONTINUOUSLY);
    }
    @Override
    public void pausePlaying() {
        position = clip.getFramePosition();
        clip.stop();
    }
    @Override
    public void resumePlaying() {
        clip.setFramePosition(position);
        clip.start();
        if (continuous)
            clip.loop(Clip.LOOP_CONTINUOUSLY);
    }
    @Override
    public void endPlaying() {
        position = clip.getFramePosition();
        clip.stop();
    }
    public static InputStream wavFileStream(String n) {
        String fullString = "../rotp/" +n;

        try { return new FileInputStream(new File(Rotp.jarPath(), n)); } 
        catch (FileNotFoundException e) {
                try { return new FileInputStream(fullString); } 
                catch (FileNotFoundException ex) {
                    return Rotp.class.getResourceAsStream(n);
                }
        }
    }
}
