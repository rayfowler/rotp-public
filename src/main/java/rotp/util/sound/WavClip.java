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
    float volume;
    int position = 0;
    boolean continuous = false;
    String style = "";
    
    public static WavClip play(String fn, float vol) {
        if (!loadedClips.containsKey(fn)) 
            loadedClips.put(fn, new WavClip(fn, vol));
        
        WavClip wc = loadedClips.get(fn);
        wc.play();
        return wc;
    }
    public static WavClip playContinuously(String fn, float vol, String s) {
         if (!loadedClips.containsKey(fn)) 
            loadedClips.put(fn, new WavClip(fn, vol));
        
        WavClip wc = loadedClips.get(fn);
        wc.style = s;
        wc.playContinuously();
        return wc;
    }          
    public  WavClip(String fn, float vol) {
        filename = fn;
        volume = vol;
        loaded = false;
        
        BufferedInputStream is = null;
        try {
            if (!loaded) {
                clip = AudioSystem.getClip();
                is = new BufferedInputStream(wavFileStream(fn));
                clip.open(AudioSystem.getAudioInputStream(is));
                if (vol < 1) {
                    log("setting volume for sound: "+fn+"  to "+(int)(vol*100));
                    FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
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
            if (is != null)
                try { is.close(); } catch (IOException e) {} 
        }
    }
    public void play() {
        clip.setFramePosition(position);
        clip.start();
    }
    public void playContinuously() {
        continuous = true;
        if (style.equals("L"))
            clip.setFramePosition(0);
        else
            clip.setFramePosition(position);
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
