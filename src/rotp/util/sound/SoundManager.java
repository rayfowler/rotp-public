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
    private static final String soundsFileName = "audio_sounds.txt";
    private static final String musicFileName = "audio_music.txt";
    public static String errorString = "";

    private final HashMap<String, Sound> sounds = new HashMap<>();
    private final HashMap<String, Sound> music = new HashMap<>();
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
            loadMusicFiles(soundListDir);
        }
        catch(Exception | NoClassDefFoundError e) {
            log("SoundManager.init error: "+e.getMessage());
            disableOnError("on init: "+e.getMessage());
        }
        log("SoundManager loaded: ", str(System.currentTimeMillis()-st), "ms");
    }
    public static int soundLevel()       { return UserPreferences.soundVolume(); }
    public static int musicLevel()       { return UserPreferences.musicVolume(); }
    public boolean disabled()     { return soundsDisabled; }
    public boolean playSounds()   { return !soundsDisabled && UserPreferences.playSounds(); }
    public boolean playMusic()    { return !soundsDisabled && UserPreferences.playMusic(); }
    public void disableSounds()   { soundsDisabled = true; }
    private void disableOnError(String s) {
        disableSounds();
        errorString = s;
    }

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
            disableOnError("on toggle:"+e.getMessage());
        }
    }
    public void resetSoundVolumes() {
        float vol = soundLevel()/10.0f;
        for (Sound s: sounds.values())
            s.setVolume(vol);
    }
    public void resetMusicVolumes() {
        float vol = musicLevel()/10.0f;
        for (Sound s: music.values())
            s.setVolume(vol);
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
        try {
            currentAmbience = playContinuously(key);
        }
        catch (IllegalArgumentException e) {
            err("error: "+e.toString());
            disableOnError("on ambience:"+e.getMessage());
        }
    }
    @Override
    public SoundClip playAudioClip(String key) {
        log("play audio clip: "+key);
        if (!playSounds())
            return null;
        Sound s = sounds.get(key);
        try {
            if (s != null)
                return s.play(s.gain);
            else
                err("no sound found for key:"+key);
        }
        catch (Exception e) {
            err("SoundManager.audio error1: "+e.getMessage());
            disableOnError("on play:"+e.getMessage());
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
        Sound s = music.get(key);
        return (s == null) ? null : s.playContinuously(s.gain);
    }
    public List<String> loadSoundFiles(String dir) {
        log("Loading Sounds: ", dir);
        List<String> soundKeysAdded = new ArrayList<>();
        BufferedReader in = reader(dir+soundsFileName);
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
    public List<String> loadMusicFiles(String dir) {
        log("Loading Music: ", dir);
        List<String> soundKeysAdded = new ArrayList<>();
        BufferedReader in = reader(dir+musicFileName);
        if (in == null)
            return soundKeysAdded;

        try {
            String input;
            while ((input = in.readLine()) != null) {
                String goodKey = loadMusicDataFile(input.trim());
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
        sounds.put(key, new Sound(filename, gain, playStyle, false));
        return key;
    }
    private String loadMusicDataFile(String line) {
        if (isComment(line))
            return null;

        List<String> vals = substrings(line, ',');
        if (vals.size() < 4) {
            err("Not enough fields for music line: ", line);
            return null;
        }
        String key = vals.get(0);
        String filename = vals.get(1);
        float gain = parseInt(vals.get(2))/100f;
        String playStyle = vals.get(3);
        music.put(key, new Sound(filename, gain, playStyle,true));
        return key;
    }
    private class Sound {
        private final String filename;
        private float gain = 0;
        public String style;
        boolean music = false;
        public Sound(String fn, float g, String s, boolean b) {
            filename = fn;
            gain = g;
            style = s;
            music = b;
        }
        public float masterVolume() {
            if (music)
                return SoundManager.musicLevel() / 10.0f;
            else
                return SoundManager.soundLevel()/ 10.0f;
        }
        public void setVolume(float vol) {
            if (filename.endsWith("wav"))
                WavClip.setVolume(filename, vol);
        }
        public SoundClip play(float gain) {
            if (filename.endsWith("wav"))
                return WavClip.play(filename, gain, masterVolume());
            else
                return null;
        }
        public SoundClip playContinuously(float gain) {
            if (filename.endsWith("wav"))
                return WavClip.playContinuously(filename, gain, style, masterVolume());
            else
                return null;
        }
    }
}
