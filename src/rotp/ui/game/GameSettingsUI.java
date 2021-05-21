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
package rotp.ui.game;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.util.List;
import javax.swing.JFileChooser;
import rotp.ui.BasePanel;
import static rotp.ui.BasePanel.s100;
import static rotp.ui.BasePanel.s20;
import static rotp.ui.BasePanel.s90;
import rotp.ui.BaseText;
import rotp.ui.UserPreferences;
import rotp.ui.main.SystemPanel;
import rotp.util.sound.SoundManager;

public class GameSettingsUI extends BasePanel implements MouseListener, MouseMotionListener, MouseWheelListener {
    private static final long serialVersionUID = 1L;
    private static final Color backgroundHaze = new Color(0,0,0,160);
    
    public static final Color lightBrown = new Color(178,124,87);
    public static final Color brown = new Color(141,101,76);
    public static final Color darkBrown = new Color(112,85,68);
    public static final Color darkerBrown = new Color(75,55,39);
    
    Rectangle hoverBox;
    Rectangle okBox = new Rectangle();
    Rectangle defaultBox = new Rectangle();
    BasePanel parent;
    BaseText displayModeText;
    BaseText texturesText;
    BaseText mouseText;
    BaseText autoColonizeText;
    BaseText soundsText;
    BaseText memoryText;
    BaseText musicText;
    BaseText graphicsText;
    BaseText autoBombardText;
    BaseText backupTurnsText;
    BaseText saveDirText;
    
    public GameSettingsUI() {
        init0();
    }
    private void init0() {
        setOpaque(false);
        Color textC = SystemPanel.whiteText;
        displayModeText =  new BaseText(this, false, 20, 20,-78,  textC, textC, hoverC, depressedC, textC, 0, 0, 0);
        graphicsText =     new BaseText(this, false, 20, 20,-78,  textC, textC, hoverC, depressedC, textC, 0, 0, 0);
        texturesText =     new BaseText(this, false, 20, 20,-78,  textC, textC, hoverC, depressedC, textC, 0, 0, 0);
        mouseText =        new BaseText(this, false, 20, 20,-78,  textC, textC, hoverC, depressedC, textC, 0, 0, 0);
        soundsText =       new BaseText(this, false, 20, 20,-78,  textC, textC, hoverC, depressedC, textC, 0, 0, 0);
        memoryText =       new BaseText(this, false, 20, 20,-78,  textC, textC, hoverC, depressedC, textC, 0, 0, 0);
        autoColonizeText = new BaseText(this, false, 20, 20,-78,  textC, textC, hoverC, depressedC, textC, 0, 0, 0);
        musicText =        new BaseText(this, false, 20, 20,-78,  textC, textC, hoverC, depressedC, textC, 0, 0, 0);
        autoBombardText =  new BaseText(this, false, 20, 20,-78,  textC, textC, hoverC, depressedC, textC, 0, 0, 0);
        backupTurnsText =  new BaseText(this, false, 20, 20,-78,  textC, textC, hoverC, depressedC, textC, 0, 0, 0);
        saveDirText =      new BaseText(this, false, 20, 20,-78,  textC, textC, hoverC, depressedC, textC, 0, 0, 0);
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
    }
    public void init() {
        texturesText.displayText(texturesStr());
        mouseText.displayText(mouseStr());
        soundsText.displayText(soundsStr());
        memoryText.displayText(memoryStr());
        displayModeText.displayText(displayModeStr());
        autoColonizeText.displayText(autoColonizeStr());
        musicText.displayText(musicStr());
        graphicsText.displayText(graphicsStr());
        autoBombardText.displayText(autoBombardStr());
        backupTurnsText.displayText(backupTurnsStr());
        saveDirText.displayText(saveDirStr());
    }
    public void open(BasePanel p) {
        parent = p;
        init();
        enableGlassPane(this);
    }
    public void close() {
        disableGlassPane();
    }
    public void setToDefault() {
        UserPreferences.setToDefault();
        init();
        repaint();
    }
    @Override
    public void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        
        int w = getWidth();
        int h = getHeight();
        Graphics2D g = (Graphics2D) g0;
        
        
        // draw background "haze"
        g.setColor(backgroundHaze);
        g.fillRect(0, 0, w, h);
        
        int numColumns = 3;
        int columnPad = s20;
        int lineH = s17;
        Font descFont = narrowFont(15);
        int leftM = s100;
        int rightM = s100;
        int topM = s90;
        int w1 = w-leftM-rightM;
        int h1 = h-topM-s90;
        g.setPaint(GameUI.settingsSetupBackground(w));
        g.fillRect(leftM, topM, w1, h1);
        String title = text("GAME_SETTINGS_TITLE");
        g.setFont(narrowFont(30));
        int sw = g.getFontMetrics().stringWidth(title);
        int x1 = leftM+((w1-sw)/numColumns);
        int y1 = topM+s40;
        drawBorderedString(g, title, 1, x1, y1, Color.black, Color.white);
        
        g.setFont(narrowFont(18));
        String expl = text("GAME_SETTINGS_DESCRIPTION");
        g.setColor(SystemPanel.blackText);
        drawString(g,expl, leftM+s10, y1+s30);
        
        Stroke prev = g.getStroke();
        g.setStroke(stroke3);

        // left column
        int y2 = scaled(200);
        int x2 = leftM+s10;
        int w2 = (w1/numColumns)-columnPad;
        int h2 = s90;
        g.setColor(SystemPanel.blackText);
        g.drawRect(x2, y2, w2, h2);
        g.setPaint(GameUI.settingsSetupBackground(w));
        g.fillRect(x2+s10, y2-s10, displayModeText.stringWidth(g)+s10,s30);
        displayModeText.setScaledXY(x2+s20, y2+s7);
        displayModeText.draw(g);
        String desc = text("GAME_SETTINGS_DISPLAY_MODE_DESC");
        g.setColor(SystemPanel.blackText);
        g.setFont(descFont);
        List<String> lines = this.wrappedLines(g,desc, w2-s30);
        int y3 = y2+s10;
        for (String line: lines) {
            y3 += lineH;
            drawString(g,line, x2+s20, y3);
        }
        
        y2 += (h2+s20);
        g.setColor(SystemPanel.blackText);
        g.drawRect(x2, y2, w2, h2);
        g.setPaint(GameUI.settingsSetupBackground(w));
        g.fillRect(x2+s10, y2-s10, graphicsText.stringWidth(g)+s10,s30);
        graphicsText.setScaledXY(x2+s20, y2+s7);
        graphicsText.draw(g);
        desc = text("GAME_SETTINGS_GRAPHICS_DESC");
        g.setColor(SystemPanel.blackText);
        g.setFont(descFont);
        lines = this.wrappedLines(g,desc, w2-s30);
        y3 = y2+s10;
        for (String line: lines) {
            y3 += lineH;
            drawString(g,line, x2+s20, y3);
        }       
       
        y2 += (h2+s20);
        g.setColor(SystemPanel.blackText);
        g.drawRect(x2, y2, w2, h2);
        g.setPaint(GameUI.settingsSetupBackground(w));
        g.fillRect(x2+s10, y2-s10, texturesText.stringWidth(g)+s10,s30);
        texturesText.setScaledXY(x2+s20, y2+s7);
        texturesText.draw(g);
        desc = text("GAME_SETTINGS_TEXTURES_DESC");
        g.setColor(SystemPanel.blackText);
        g.setFont(descFont);
        lines = this.wrappedLines(g,desc, w2-s30);
        y3 = y2+s10;
        for (String line: lines) {
            y3 += lineH;
            drawString(g,line, x2+s20, y3);
        }
        
        y2 += (h2+s20);
        g.setColor(SystemPanel.blackText);
        g.drawRect(x2, y2, w2, h2);
        g.setPaint(GameUI.settingsSetupBackground(w));
        g.fillRect(x2+s10, y2-s10, mouseText.stringWidth(g)+s10,s30);
        mouseText.setScaledXY(x2+s20, y2+s7);
        mouseText.draw(g);
        desc = text("GAME_SETTINGS_SENSITIVITY_DESC");
        g.setColor(SystemPanel.blackText);
        g.setFont(descFont);
        lines = this.wrappedLines(g,desc, w2-s30);
        y3 = y2+s10;
        for (String line: lines) {
            y3 += lineH;
            drawString(g,line, x2+s20, y3);
        }
        
        // middle column
        y2 = scaled(200);
        x2 = x2+w2+s20;
        h2 = s90;
        g.setColor(SystemPanel.blackText);
        g.drawRect(x2, y2, w2, h2);
        g.setPaint(GameUI.settingsSetupBackground(w));
        g.fillRect(x2+s10, y2-s10, soundsText.stringWidth(g)+s10,s30);
        soundsText.setScaledXY(x2+s20, y2+s7);
        soundsText.draw(g);
        desc = SoundManager.current().disabled() ? text("GAME_SETTINGS_SOUNDS_ERR_DESC", SoundManager.errorString) : text("GAME_SETTINGS_SOUNDS_DESC");
        g.setColor(SystemPanel.blackText);
        g.setFont(descFont);
        lines = this.wrappedLines(g,desc, w2-s30);
        y3 = y2+s10;
        for (String line: lines) {
            y3 += lineH;
            drawString(g,line, x2+s20, y3);
        }
        
        y2 += (h2+s20);
        g.setColor(SystemPanel.blackText);
        g.drawRect(x2, y2, w2, h2);
        g.setPaint(GameUI.settingsSetupBackground(w));
        g.fillRect(x2+s10, y2-s10, musicText.stringWidth(g)+s10,s30);
        musicText.setScaledXY(x2+s20, y2+s7);
        musicText.draw(g);
        desc = text("GAME_SETTINGS_MUSIC_DESC");
        g.setColor(SystemPanel.blackText);
        g.setFont(descFont);
        lines = this.wrappedLines(g,desc, w2-s30);
        y3 = y2+s10;
        for (String line: lines) {
            y3 += lineH;
            drawString(g,line, x2+s20, y3);
        }
        
        y2 += (h2+s20);
        g.setColor(SystemPanel.blackText);
        g.drawRect(x2, y2, w2, h2);
        g.setPaint(GameUI.settingsSetupBackground(w));
        g.fillRect(x2+s10, y2-s10, memoryText.stringWidth(g)+s10,s30);
        memoryText.setScaledXY(x2+s20, y2+s7);
        memoryText.draw(g);
        desc = text("GAME_SETTINGS_MEMORY_DESC");
        g.setColor(SystemPanel.blackText);
        g.setFont(descFont);
        lines = this.wrappedLines(g,desc, w2-s30);
        y3 = y2+s10;
        for (String line: lines) {
            y3 += lineH;
            drawString(g,line, x2+s20, y3);
        }
          
        // right side
        y2 = scaled(200);
        h2 = s90;
        x2 = x2+w2+s20;
        g.setColor(SystemPanel.blackText);
        g.drawRect(x2, y2, w2, h2);
        g.setPaint(GameUI.settingsSetupBackground(w));
        g.fillRect(x2+s10, y2-s10, autoColonizeText.stringWidth(g)+s10,s30);
        autoColonizeText.setScaledXY(x2+s20, y2+s7);
        autoColonizeText.draw(g);
        desc = text("GAME_SETTINGS_AUTOCOLONIZE_DESC");
        g.setColor(SystemPanel.blackText);
        g.setFont(descFont);
        lines = this.wrappedLines(g,desc, w2-s30);
        y3 = y2+s10;
        for (String line: lines) {
            y3 += lineH;
            drawString(g,line, x2+s20, y3);
        }

        
        y2 += (h2+s20);
        g.setColor(SystemPanel.blackText);
        g.drawRect(x2, y2, w2, h2);
        g.setPaint(GameUI.settingsSetupBackground(w));
        g.fillRect(x2+s10, y2-s10, autoBombardText.stringWidth(g)+s30,s30);
        autoBombardText.setScaledXY(x2+s20, y2+s7);
        autoBombardText.draw(g);
        desc = text("GAME_SETTINGS_AUTOBOMBARD_DESC");
        g.setColor(SystemPanel.blackText);
        g.setFont(descFont);
        lines = this.wrappedLines(g,desc, w2-s30);
        y3 = y2+s10;
        for (String line: lines) {
            y3 += lineH;
            drawString(g,line, x2+s20, y3);
        }
        
        y2 += (h2+s20);
        g.setColor(SystemPanel.blackText);
        g.drawRect(x2, y2, w2, h2);
        g.setPaint(GameUI.settingsSetupBackground(w));
        g.fillRect(x2+s10, y2-s10, backupTurnsText.stringWidth(g)+s30,s30);
        backupTurnsText.setScaledXY(x2+s20, y2+s7);
        backupTurnsText.draw(g);
        desc = text("GAME_SETTINGS_BACKUP_DESC");
        g.setColor(SystemPanel.blackText);
        g.setFont(descFont);
        lines = this.wrappedLines(g,desc, w2-s30);
        y3 = y2+s10;
        for (String line: lines) {
            y3 += lineH;
            drawString(g,line, x2+s20, y3);
        }
        
        y2 += (h2+s20);
        g.setColor(SystemPanel.blackText);
        g.drawRect(x2, y2, w2, h2);
        g.setPaint(GameUI.settingsSetupBackground(w));
        g.fillRect(x2+s10, y2-s10, saveDirText.stringWidth(g)+s30,s30);
        saveDirText.setScaledXY(x2+s20, y2+s7);
        saveDirText.draw(g);
        String saveDir = UserPreferences.saveDir();
        desc = saveDir.isEmpty() ? text("GAME_SETTINGS_SAVEDIR_DESC1") : text("GAME_SETTINGS_SAVEDIR_DESC2", saveDir);
        g.setColor(SystemPanel.blackText);
        g.setFont(descFont);
        lines = this.wrappedLines(g,desc, w2-s30);
        y3 = y2+s10;
        for (String line: lines) {
            y3 += lineH;
            drawString(g,line, x2+s20, y3);
        }
        
        g.setStroke(prev);

        // draw settings button
        int cnr = s5;
        int smallButtonH = s30;
        int smallButtonW = scaled(180);
        okBox.setBounds(w-scaled(289), scaled(640), smallButtonW, smallButtonH);
        g.setColor(GameUI.buttonBackgroundColor());
        g.fillRoundRect(okBox.x, okBox.y, smallButtonW, smallButtonH, cnr, cnr);
        g.setFont(narrowFont(20));
        String text6 = text("GAME_SETTINGS_EXIT");
        int sw6 = g.getFontMetrics().stringWidth(text6);
        int x6 = okBox.x+((okBox.width-sw6)/2);
        int y6 = okBox.y+okBox.height-s8;
        Color c6 = hoverBox == okBox ? Color.yellow : GameUI.borderBrightColor();
        drawShadowedString(g, text6, 2, x6, y6, GameUI.borderDarkColor(), c6);
        prev = g.getStroke();
        g.setStroke(stroke1);
        g.drawRoundRect(okBox.x, okBox.y, okBox.width, okBox.height, cnr, cnr);
        g.setStroke(prev);

        String text7 = text("GAME_SETTINGS_DEFAULT");
        int sw7 = g.getFontMetrics().stringWidth(text7);
        smallButtonW = sw7+s30;
        defaultBox.setBounds(okBox.x-smallButtonW-s30, scaled(640), smallButtonW, smallButtonH);
        g.setColor(GameUI.buttonBackgroundColor());
        g.fillRoundRect(defaultBox.x, defaultBox.y, smallButtonW, smallButtonH, cnr, cnr);
        g.setFont(narrowFont(20));
        int x7 = defaultBox.x+((defaultBox.width-sw7)/2);
        int y7 = defaultBox.y+defaultBox.height-s8;
        Color c7 = hoverBox == defaultBox ? Color.yellow : GameUI.borderBrightColor();
        drawShadowedString(g, text7, 2, x7, y7, GameUI.borderDarkColor(), c7);
        prev = g.getStroke();
        g.setStroke(stroke1);
        g.drawRoundRect(defaultBox.x, defaultBox.y, defaultBox.width, defaultBox.height, cnr, cnr);
        g.setStroke(prev);
    }
    private String texturesStr() {
        String opt = text(UserPreferences.texturesMode());
        return text("GAME_SETTINGS_TEXTURES", opt)+"   ";
    }
    private String mouseStr() {
        String opt = text(UserPreferences.sensitivityMode());
        return text("GAME_SETTINGS_SENSITIVITY", opt)+"   ";
    }
    private String autoColonizeStr() {
        String val = UserPreferences.autoColonize()?  text("GAME_SETTINGS_AUTOCOLONIZE_YES"): text("GAME_SETTINGS_AUTOCOLONIZE_NO");
        return text("GAME_SETTINGS_AUTOCOLONIZE", val+"   ");
    }
    private String soundsStr() {
        String val;
        if (SoundManager.current().disabled())
            val = text("GAME_SETTINGS_SOUNDS_DISABLED");
        else if (SoundManager.current().playSounds())
            val = text("GAME_SETTINGS_SOUNDS_ON", str(SoundManager.soundLevel()));
        else
            val = text("GAME_SETTINGS_SOUNDS_OFF");
        
        return text("GAME_SETTINGS_SOUNDS", val+"   ");
    }
    private String musicStr() {
        String val;
        if (SoundManager.current().playMusic())
            val = text("GAME_SETTINGS_MUSIC_ON", str(SoundManager.musicLevel()));
        else
            val = text("GAME_SETTINGS_MUSIC_OFF");
        
        return text("GAME_SETTINGS_MUSIC", val+"   ");
    }
    private String memoryStr() {
        String val;
        if (UserPreferences.showMemory())
            val = text("GAME_SETTINGS_MEMORY_YES");
        else
            val = text("GAME_SETTINGS_MEMORY_NO");
        
        return text("GAME_SETTINGS_MEMORY", val+"   ");
    }
    private String displayModeStr() {
        String opt = text(UserPreferences.displayMode());
        return text("GAME_SETTINGS_DISPLAY_MODE", opt)+"   ";
    }
    private String graphicsStr() {
        String opt = text(UserPreferences.graphicsMode());
        return text("GAME_SETTINGS_GRAPHICS", opt)+"   ";
    }
    private String autoBombardStr() {
        String opt = text(UserPreferences.autoBombardMode());
        return text("GAME_SETTINGS_AUTOBOMBARD", opt)+"   ";
    }
    private String backupTurnsStr() {
        int turns = UserPreferences.backupTurns();
        String val;
        if (turns == 0)
            val = text("GAME_SETTINGS_BACKUP_OFF");
        else 
            val = text("GAME_SETTINGS_BACKUP_ON", str(turns));
        
        return text("GAME_SETTINGS_BACKUP", val+"   ");
    }
    private String saveDirStr() {
        String saveDir = UserPreferences.saveDirStr();
        
        return text("GAME_SETTINGS_SAVEDIR", text(saveDir));
    }
    private void toggleDisplayMode() {
        softClick();
        UserPreferences.toggleDisplayMode();
        displayModeText.repaint(displayModeStr());
    //    repaint();
    }
    private void toggleTextures() {
        softClick();
        UserPreferences.toggleTexturesMode();
        texturesText.repaint(texturesStr());
    }
    private void toggleSensitivity() {
        softClick();
        UserPreferences.toggleSensitivityMode();
        mouseText.repaint(mouseStr());
    }
    private void toggleMemory() {
        softClick();
        UserPreferences.toggleMemory();
        memoryText.repaint(memoryStr());
        repaint();
    }
    private void scrollSounds(boolean up) {
        if (up)
            UserPreferences.increaseSoundLevel();
        else
            UserPreferences.decreaseSoundLevel();
        soundsText.repaint(soundsStr());
    }
    private void toggleSounds() {
        softClick();
        SoundManager.current().toggleSounds();
        soundsText.repaint(soundsStr());
    }
    private void scrollMusic(boolean up) {
        if (up)
            UserPreferences.increaseMusicLevel();
        else
            UserPreferences.decreaseMusicLevel();
        musicText.repaint(musicStr());
    }
    private void toggleMusic() {
        softClick();
        SoundManager.current().toggleMusic();
        musicText.repaint(musicStr());
    }
    private void scrollBackupTurns(boolean up) {
        int turns = UserPreferences.backupTurns();
        if (up)
            UserPreferences.backupTurns(turns+1);
        else
            UserPreferences.backupTurns(turns-1);
        backupTurnsText.repaint(backupTurnsStr());
    }
    private void toggleBackupTurns() {
        UserPreferences.toggleBackupTurns();
        backupTurnsText.repaint(backupTurnsStr());
    }
    private void toggleSaveDir() {
        final JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        File saveDir = new File(UserPreferences.saveDirectoryPath());
        fc.setCurrentDirectory(saveDir);
        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String path = fc.getSelectedFile().getAbsolutePath();
            UserPreferences.saveDir(path);
            saveDirText.repaint(saveDirStr());
            repaint();
        }
    }
    private void toggleAutoColonize() {
        softClick();
        UserPreferences.toggleAutoColonize();
        autoColonizeText.repaint(autoColonizeStr());
    }
    private void toggleAutoBombard() {
        softClick();
        UserPreferences.toggleAutoBombard();
        autoBombardText.repaint(autoBombardStr());
    }
    private void toggleGraphics(MouseEvent e) {
        softClick();
        UserPreferences.toggleGraphicsMode();
        graphicsText.repaint(graphicsStr());
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch(e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
                close();
                break;
            case KeyEvent.VK_SPACE:
            case KeyEvent.VK_ENTER:
                parent.advanceHelp();
                break;
        }
    }
    @Override
    public void mouseDragged(MouseEvent e) {  }
    @Override
    public void mouseMoved(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        Rectangle prevHover = hoverBox;
        hoverBox = null;
        if (texturesText.contains(x,y))
            hoverBox = texturesText.bounds();
        else if (mouseText.contains(x,y))
            hoverBox = mouseText.bounds();
        else if (autoColonizeText.contains(x,y))
            hoverBox = autoColonizeText.bounds();
        else if (autoBombardText.contains(x,y))
            hoverBox = autoBombardText.bounds();
        else if (displayModeText.contains(x,y))
            hoverBox = displayModeText.bounds();
        else if (soundsText.contains(x,y))
            hoverBox = soundsText.bounds();
        else if (memoryText.contains(x,y))
            hoverBox = memoryText.bounds();
        else if (musicText.contains(x,y))
            hoverBox = musicText.bounds();
        else if (graphicsText.contains(x,y))
            hoverBox = graphicsText.bounds();
        else if (backupTurnsText.contains(x,y))
            hoverBox = backupTurnsText.bounds();
        else if (saveDirText.contains(x,y))
            hoverBox = saveDirText.bounds();
        else if (okBox.contains(x,y))
            hoverBox = okBox;
        else if (defaultBox.contains(x,y))
            hoverBox = defaultBox;
		
        if (hoverBox != prevHover) {
            if (prevHover == texturesText.bounds())
                texturesText.mouseExit();
            else if (prevHover == mouseText.bounds())
                mouseText.mouseExit();
            else if (prevHover == displayModeText.bounds())
                displayModeText.mouseExit();
            else if (prevHover == autoColonizeText.bounds())
                autoColonizeText.mouseExit();
            else if (prevHover == autoBombardText.bounds())
                autoBombardText.mouseExit();
            else if (prevHover == soundsText.bounds())
                soundsText.mouseExit();
            else if (prevHover == memoryText.bounds())
                memoryText.mouseExit();
            else if (prevHover == musicText.bounds())
                musicText.mouseExit();
            else if (prevHover == graphicsText.bounds())
                graphicsText.mouseExit();
            else if (prevHover == backupTurnsText.bounds())
                backupTurnsText.mouseExit();
            else if (prevHover == saveDirText.bounds())
                saveDirText.mouseExit();
            if (hoverBox == texturesText.bounds())
                texturesText.mouseEnter();
            if (hoverBox == mouseText.bounds())
                mouseText.mouseEnter();
            else if (hoverBox == displayModeText.bounds())
                displayModeText.mouseEnter();
            else if (hoverBox == autoColonizeText.bounds())
                autoColonizeText.mouseEnter();
            else if (hoverBox == autoBombardText.bounds())
                autoBombardText.mouseEnter();
            else if (hoverBox == soundsText.bounds())
                soundsText.mouseEnter();
            else if (hoverBox == memoryText.bounds())
                memoryText.mouseEnter();
            else if (hoverBox == musicText.bounds())
                musicText.mouseEnter();
            else if (hoverBox == graphicsText.bounds())
                graphicsText.mouseEnter();
            else if (hoverBox == backupTurnsText.bounds())
                backupTurnsText.mouseEnter();
            else if (hoverBox == saveDirText.bounds())
                saveDirText.mouseEnter();
            if (prevHover != null)
                repaint(prevHover);
            if (hoverBox != null)
                repaint(hoverBox);
        }
    }
    @Override
    public void mouseClicked(MouseEvent e) { }
    @Override
    public void mousePressed(MouseEvent e) { }
    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() > 3)
            return;
        if (hoverBox == null)
            return;
        int x = e.getX();
        int y = e.getY();
        if (hoverBox == texturesText.bounds())
            toggleTextures();
        else if (hoverBox == mouseText.bounds())
            toggleSensitivity();
        else if (hoverBox == displayModeText.bounds())
            toggleDisplayMode();
        else if (hoverBox == autoColonizeText.bounds())
            toggleAutoColonize();
        else if (hoverBox == autoBombardText.bounds())
            toggleAutoBombard();
        else if (hoverBox == soundsText.bounds())
            toggleSounds();
        else if (hoverBox == memoryText.bounds())
            toggleMemory();
        else if (hoverBox == musicText.bounds())
            toggleMusic();
        else if (hoverBox == graphicsText.bounds())
            toggleGraphics(e);
        else if (hoverBox == backupTurnsText.bounds())
            toggleBackupTurns();
        else if (hoverBox == saveDirText.bounds())
            toggleSaveDir();
        else if (hoverBox == okBox)
            close();
        else if (hoverBox == defaultBox)
            setToDefault();
    }
    @Override
    public void mouseEntered(MouseEvent e) { }
    @Override
    public void mouseExited(MouseEvent e) {
        if (hoverBox != null) {
            hoverBox = null;
            repaint();
        }
    }
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        boolean up = e.getWheelRotation() < 0;
        if (hoverBox == soundsText.bounds())
            scrollSounds(up);
        else if (hoverBox == musicText.bounds())
            scrollMusic(up);
        else if (hoverBox == backupTurnsText.bounds())
            scrollBackupTurns(up);
    }
}
