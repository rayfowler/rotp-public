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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import rotp.ui.UserPreferences;
import rotp.ui.game.GameUI;
import rotp.util.Base;

public enum SoundManager implements Base {
    INSTANCE;
    public static SoundManager current() { return INSTANCE; }
    private boolean soundsDisabled = false;
    private static final String soundListDir = "data/sounds/";
    private static final String listFileName = "audio.txt";

    private final HashMap<String, Sound> sounds = new HashMap<>();
    private String currentAmbienceKey = "";
    private SoundClip currentAmbience;

    static {
        loadSounds();
    }

    private SoundManager() {  }
    
    public static void loadSounds() {
        INSTANCE.init();
    }

    private void init() {
        sounds.clear();        
        long st = System.currentTimeMillis();
        try {
            loadSoundFiles(soundListDir);
        }
        catch(Exception | NoClassDefFoundError e) {
            log("SoundManager.init error: "+e.getMessage());
            disableSounds();
        }
        log("SoundManager loaded: ", str(System.currentTimeMillis()-st), "ms");
    }
    public boolean disabled()     { return soundsDisabled; }
    public boolean playSounds()   { return !soundsDisabled && UserPreferences.playSounds(); }
    public boolean playMusic()    { return !soundsDisabled && UserPreferences.playMusic(); }
    public void disableSounds()   { soundsDisabled = true; }

    public void toggleSounds()    { UserPreferences.toggleSounds(); }
    public void toggleMusic()     {
        UserPreferences.toggleMusic();
        try {
            if (playMusic())
                if (currentAmbience == null)
                    playAmbience(GameUI.AMBIENCE_KEY);
                else
                    unpauseAmbience();
            else
                pauseAmbience();
        }
        catch (Exception e) {
            log("SoundManager.music error1: "+e.getMessage());
            disableSounds();
        }
    }
    private void pauseAmbience() {
        if (currentAmbience != null)
            currentAmbience.pausePlaying();
    }
    private void unpauseAmbience() {
        if (currentAmbience != null)
            currentAmbience.resumePlaying();
    }
    @Override
    public void stopAmbience() {
        if (!playMusic())
            return;
        if (currentAmbience != null)
            currentAmbience.pausePlaying();
        currentAmbienceKey = "";
        currentAmbience = null;
    }
    @Override
    public void playAmbience(String key) {
        if (!playMusic())
            return;
        if ((key == null) || key.isEmpty()) {
            stopAmbience();
            return;
        }
        if (key.equals(currentAmbienceKey))
            return;

        log("playing ambience: ", key);
        if (currentAmbience != null)
            currentAmbience.pausePlaying();

        currentAmbienceKey = key;
        currentAmbience = playContinuously(key);
    }
    @Override
    public SoundClip playAudioClip(String key) {
        if (!playSounds())
            return null;
        Sound s = sounds.get(key);
        try {
            if (s != null)
                return s.play(s.gain);
            else
                log("no sound found for key:"+key);
        }
        catch (Exception e) {
            log("SoundManager.audio error1: "+e.getMessage());
                disableSounds();
        }
        return null;
    }
    public SoundClip alwaysPlay(String key) {
        Sound s = sounds.get(key);
        if (s != null)
            return s.play(s.gain);
        return null;
    }
    private SoundClip playContinuously(String key) {
        Sound s = sounds.get(key);
        return (s == null) ? null : s.playContinuously(s.gain);
    }
    public List<String> loadSoundFiles(String dir) {
        log("Loading Sounds: ", dir);
        List<String> soundKeysAdded = new ArrayList<>();
        BufferedReader in = reader(dir+listFileName);
        if (in == null)
            return soundKeysAdded;

        try {
            String input;
            while ((input = in.readLine()) != null) {
                String goodKey = loadSoundDataFile(input.trim());
                if (goodKey != null)
                    soundKeysAdded.add(goodKey);
            }
            in.close();
        }
        catch (IOException e) {
            err("SoundManager.loadSoundFiles -- IOException: " + e);
        }
        return soundKeysAdded;
    }
    private String loadSoundDataFile(String line) {
        if (isComment(line))
            return null;

        List<String> vals = substrings(line, ',');
        if (vals.size() < 4) {
            err("Not enough fields for sound line: ", line);
            return null;
        }
        String key = vals.get(0);
        String filename = vals.get(1);
        float gain = parseInt(vals.get(2))/100f;
        String playStyle = vals.get(3);
        sounds.put(key, new Sound(filename, gain, playStyle));
        return key;
    }
    private class Sound {
        private final String filename;
        private float gain = 0;
        public String style;
        public Sound(String fn, float g, String s) {
            filename = fn;
            gain = g;
            style = s;
        }
        public SoundClip play(float gain) {
            if (filename.endsWith("wav"))
                return WavClip.play(filename, gain);
            else
                return null;
        }
        public SoundClip playContinuously(float gain) {
            if (filename.endsWith("wav"))
                return WavClip.playContinuously(filename, gain, style);
            else
                return null;
        }
    }
}
