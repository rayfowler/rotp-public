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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.LinearGradientPaint;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import rotp.model.game.GameSession;
import rotp.ui.BasePanel;
import rotp.ui.BaseTextField;
import rotp.ui.NoticeMessage;
import rotp.ui.RotPUI;
import rotp.ui.UserPreferences;
import rotp.ui.main.SystemPanel;

public final class SaveGameUI extends BasePanel implements MouseListener, MouseWheelListener {
    private static final long serialVersionUID = 1L;
    private static final int  MAX_FILES = 20;
    private static final int SORT_FN_UP = 1;
    private static final int SORT_FN_DN = 2;
    private static final int SORT_DT_UP = 3;
    private static final int SORT_DT_DN = 4;
    private static final int SORT_SZ_UP = 5;
    private static final int SORT_SZ_DN = 6;
    private final static String CANCEL_ACTION = "cancel-input";
    private final static String SAVE_ACTION   = "save-input";
    private static final SimpleDateFormat fileDateFmt = new SimpleDateFormat("MMM dd, HH:mm");
    static SaveGameUI current;

    private final BaseTextField newFileField = new BaseTextField("");

    FileListingPanel listingPanel;
    List<String> saveFiles = new ArrayList<>();
    List<Long> saveSizes = new ArrayList<>();
    List<String> saveDates = new ArrayList<>();
    int selectIndex = -1;
    Rectangle hoverBox;
    Rectangle selectBox;
    int start = 0;
    int end = 0;
    boolean saving = false;

    int sortOrder = SORT_DT_UP;
    int buttonW, button1X, button2X;
    private final Rectangle cancelBox = new Rectangle();
    private final Rectangle saveBox = new Rectangle();
    private final Rectangle fileNameBox = new Rectangle();
    private final Rectangle fileSizeBox = new Rectangle();
    private final Rectangle fileDateBox = new Rectangle();
    private LinearGradientPaint[] saveBackC;
    private LinearGradientPaint[] cancelBackC;

    public SaveGameUI() {
        current = this;
        initModel();
    }
    public void init() {
        saveFiles.clear();
        saveDates.clear();
        hoverBox = null;
        selectBox = null;
        selectIndex = -1;
        start = 0;
        end = 0;
        newFileField.setFont(narrowFont(20));
        newFileField.setText(GameUI.gameName);                
        saving = false;

        sortListing();
    }
    private void sortListing() {
        saveFiles.clear();
        saveSizes.clear();
        saveDates.clear();
        String ext = GameSession.SAVEFILE_EXTENSION;

        saveFiles.add("");
        saveSizes.add(0L);
        saveDates.add("");

        File curDir = new File(UserPreferences.saveDirectoryPath());
        File[] filesList = curDir.listFiles();
        
        switch(sortOrder) {
            case SORT_FN_UP : Arrays.sort(filesList, FILE_NAME); break;
            case SORT_FN_DN : Arrays.sort(filesList, Collections.reverseOrder(FILE_NAME)); break;
            case SORT_DT_UP : Arrays.sort(filesList, FILE_DATE); break;
            case SORT_DT_DN : Arrays.sort(filesList, Collections.reverseOrder(FILE_DATE)); break;
            case SORT_SZ_UP : Arrays.sort(filesList, FILE_SIZE); break;
            case SORT_SZ_DN : Arrays.sort(filesList, Collections.reverseOrder(FILE_SIZE)); break;
        }
        for (File f : filesList){
            if (f.isFile()) {
                String name = f.getName();
                if (name.endsWith(ext)
                && !name.equalsIgnoreCase(GameSession.RECENT_SAVEFILE)) {
                    saveFiles.add(name.substring(0, name.length()-ext.length()));
                    saveDates.add(fileDateFmt.format(f.lastModified()));
                    saveSizes.add(f.length());
                }
            }
        }
    }
    private void initGradients() {
        int w = getWidth();
        buttonW = s100+s100;
        button1X = (w/2)-s10-buttonW;
        button2X = (w/2)+s10;
        Point2D start1 = new Point2D.Float(button1X, 0);
        Point2D end1 = new Point2D.Float(button1X+buttonW, 0);
        Point2D start2 = new Point2D.Float(button2X, 0);
        Point2D end2 = new Point2D.Float(button2X+buttonW, 0);
        float[] dist = {0.0f, 0.5f, 1.0f};

        Color brownEdgeC = new Color(100,70,50);
        Color brownMidC = new Color(161,110,76);
        Color[] brownColors = {brownEdgeC, brownMidC, brownEdgeC };

        Color grayEdgeC = new Color(59,66,65);
        Color grayMidC = new Color(107,118,117);
        Color[] grayColors = {grayEdgeC, grayMidC, grayEdgeC };

        saveBackC = new LinearGradientPaint[2];
        cancelBackC = new LinearGradientPaint[2];

        saveBackC[0] = new LinearGradientPaint(start1, end1, dist, brownColors);
        cancelBackC[0] = new LinearGradientPaint(start2, end2, dist, brownColors);
        saveBackC[1] = new LinearGradientPaint(start1, end1, dist, grayColors);
        cancelBackC[1] = new LinearGradientPaint(start2, end2, dist, grayColors);
    }
    private boolean validFileName(int index) {
        if (index == 0)
            return !newFileField.getText().trim().isEmpty();
        else
            return true;
    }
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (saveBackC == null)
            initGradients();
        Image back = GameUI.defaultBackground;
        int imgW = back.getWidth(null);
        int imgH = back.getHeight(null);
        g.drawImage(back, 0, 0, getWidth(), getHeight(), 0, 0, imgW, imgH, this);
    }
    private void initModel() {
        newFileField.setLimit(255);
        listingPanel = new FileListingPanel();
        setLayout(new BorderLayout());
        add(listingPanel, BorderLayout.CENTER);
        addMouseWheelListener(this);
    }
    private void scrollDown() {
        int prevStart = start;
        int prevSelect = selectIndex;
        start = max(0,min(start+1, saveFiles.size()-MAX_FILES));
        if ((start == prevStart) && (selectIndex >= 0))
            selectIndex = min(selectIndex+1, saveFiles.size()-1, MAX_FILES-1);
        if ((prevStart != start) || (prevSelect != selectIndex))
            repaint();
    }
    private void scrollUp() {
        int prevStart = start;
        int prevSelect = selectIndex;
        start = max(start-1, 0);
        if ((start == prevStart) && (selectIndex >= 0))
            selectIndex = max(selectIndex-1, 1);
        if ((prevStart != start) || (prevSelect != selectIndex))
            repaint();
    }
    private void sortByFileName() {
        switch (sortOrder) {
            case SORT_FN_UP : sortOrder = SORT_FN_DN; break;
            default         : sortOrder = SORT_FN_UP; break;
        }
        sortListing();
        repaint();
    }
    private void sortByDate() {
        switch (sortOrder) {
            case SORT_DT_UP : sortOrder = SORT_DT_DN; break;
            default         : sortOrder = SORT_DT_UP; break;
        }
        sortListing();
        repaint();
    }
    private void sortBySize() {
        switch (sortOrder) {
            case SORT_SZ_UP : sortOrder = SORT_SZ_DN; break;
            default         : sortOrder = SORT_SZ_UP; break;
        }
        sortListing();
        repaint();
    }
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        int count = e.getUnitsToScroll();
        if (count < 0)
            scrollUp();
        else
            scrollDown();
    }
    @Override
    public void mouseClicked(MouseEvent e) {}
    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited(MouseEvent e) {}
    @Override
    public void mousePressed(MouseEvent e) {}
    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() > 3)
            return;
        RotPUI.instance().selectMainPanel(false);
    }
    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        switch(k) {
            case KeyEvent.VK_DOWN:  scrollDown();  return;
            case KeyEvent.VK_UP:    scrollUp();    return;
            case KeyEvent.VK_ENTER:
            case KeyEvent.VK_S:
                saveGame();
                return;
            case KeyEvent.VK_ESCAPE:
            case KeyEvent.VK_C:     cancelSave();      return;
        }
    }
    private void saveGame() {
        String fName = fullSelectedFileName();
        if (!fName.isEmpty())
            saveGame(fName);        
    }
    private boolean canSelect()    { return selectIndex >= 0; }
    private String fullSelectedFileName() {
        String fileName = newFileField.getText().trim();
        if (!fileName.isEmpty())
            return  concat(fileName, GameSession.SAVEFILE_EXTENSION);
        return "";
    }
    public void saveGame(String s) {
        if (s.isEmpty())
            return;
        if (!validFileName(selectIndex)) {
            misClick();
            return;
        }
        saving = true;
        newFileField.setVisible(false);
        GameUI.gameName = newFileField.getText().trim();
        repaint();
        buttonClick();
        final Runnable save = () -> {
            try {
                GameSession.instance().saveSession(s, false);
                RotPUI.instance().selectGamePanel();
            }
            catch(Exception e) {
                showError(concat("Save unsuccessful: ", s));
                saving = false;
                return;
            }
        };
        SwingUtilities.invokeLater(save);
    }
    public void cancelSave() {
        buttonClick();
        RotPUI.instance().selectGamePanel();
    }
    class FileListingPanel extends BasePanel implements MouseListener, MouseMotionListener {
        private static final long serialVersionUID = 1L;
        private final Rectangle[] gameBox = new Rectangle[MAX_FILES];
        private final Rectangle listBox = new Rectangle();
        private boolean dragging = false;
        private int lastMouseY;
        private int yOffset = 0;
        private int lineH = s50;
        public FileListingPanel() {
            init();
        }
        private void init() {
            setOpaque(false);
            for (int i=0;i<gameBox.length;i++)
                gameBox[i] = new Rectangle();
            addMouseListener(this);
            addMouseMotionListener(this);

            newFileField.setBackground(GameUI.loadHoverBackground());
            newFileField.setBorder(this.newEmptyBorder(0,0,0,0));
            newFileField.setFont(narrowFont(20));
            newFileField.setForeground(Color.black);
            newFileField.setCaretColor(Color.black);
            newFileField.putClientProperty("caretWidth", s4);
            newFileField.addMouseListener(this);
            newFileField.setVisible(false);
            InputMap im = newFileField.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            ActionMap am = newFileField.getActionMap();
            im.put(KeyStroke.getKeyStroke("ESCAPE"), CANCEL_ACTION);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0), SAVE_ACTION);
            am.put(CANCEL_ACTION, new CancelAction());
            am.put(SAVE_ACTION, new SaveAction());

            setLayout(null);
            add(newFileField);            
        }
        @Override
        public void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;

            for (int i=0;i<gameBox.length;i++)
                gameBox[i].setBounds(0,0,0,0);

            int w = getWidth();
            lineH = s22;

            String title = text("SAVE_GAME_TITLE");
            g.setFont(font(60));
            int sw = g.getFontMetrics().stringWidth(title);
            drawShadowedString(g, title, 1, 3, (w-sw)/2, scaled(140), GameUI.titleShade(), GameUI.titleColor());

            end = min(saveFiles.size(), start+saveFiles.size());

            int w0 = scaled(650);
            int x0 = (w-w0)/2;
            int h0 = s5+(MAX_FILES*lineH);
            int y0 = scaled(180);

            // draw back mask
            int wTop = s10;
            int wSide = s40;
            int wBottom = s80;
            g.setColor(GameUI.loadListMask());
            g.fillRect(x0-wSide, y0-wTop, w0+wSide+wSide, h0+lineH+wTop+wBottom);

            g.setColor(GameUI.raceCenterColor());
            g.fillRect(x0, y0, w0, h0+lineH);
            
            g.setColor(GameUI.sortLabelBackColor());
            g.fillRect(x0, y0, w0, lineH);
            drawFilenameButton(g, x0+s30, y0+lineH);
            drawSizeButton(g, x0+w0-scaled(150), y0+lineH);
            drawDateButton(g, x0+w0-s30, y0+lineH);
            // draw list of games to load
            int lineY = y0+s5+lineH;
            listBox.setBounds(x0, y0, w0, h0);
            if ((start+MAX_FILES) >= 1) {
                for (int i=start+1;i<start+MAX_FILES;i++) {
                    int boxIndex = i-start;
                    if (boxIndex == selectIndex) {
                        g.setPaint(GameUI.loadHoverBackground());
                        g.fillRect(x0+s20, lineY-s4, w0-s40, lineH);
                    }
                    else if (i % 2 == 1) {
                        g.setPaint(GameUI.loadHiBackground());
                        g.fillRect(x0+s20, lineY-s4, w0-s40, lineH);
                    }
                    if (i<end) {
                        drawSaveGame(g, boxIndex, saveFiles.get(i), saveSizes.get(i), saveDates.get(i), x0, lineY, w0, lineH);
                        gameBox[boxIndex].setBounds(x0,lineY,w0,lineH);
                    }
                    lineY += lineH;
                }
            }

            // always draw the first entry (the blank entry for a new save file)
            drawSaveGame(g, 0, saveFiles.get(0), saveSizes.get(0), saveDates.get(0), x0, y0, w0, lineH);
            gameBox[0].setBounds(x0,lineY+s5,w0,lineH);
            if (!newFileField.isVisible()) {
                newFileField.setMargin(new Insets(s2,s30,0,0));
                newFileField.setBounds(x0,lineY+s5,w0,lineH);
                newFileField.setVisible(true);
                newFileField.requestFocus();
            }
            lineY += lineH;

            // draw load button
            int buttonY = lineY+s20;
            int buttonH = s40;
            saveBox.setBounds(button1X,buttonY,buttonW,buttonH);
            g.setColor(SystemPanel.buttonShadowC);
            g.fillRoundRect(button1X+s1,buttonY+s3,buttonW,buttonH,s8,s8);
            g.fillRoundRect(button1X+s2,buttonY+s4,buttonW,buttonH,s8,s8);
            g.setPaint(saveBackC[GameUI.opt()]);
            g.fillRoundRect(button1X,buttonY,buttonW,buttonH,s5,s5);

            String text1 = text("SAVE_GAME_OK");
            g.setFont(narrowFont(30));
            int sw1 = g.getFontMetrics().stringWidth(text1);
            int x1 = button1X + ((buttonW-sw1)/2);

            Color textC = (saveBox == hoverBox) ? GameUI.textHoverColor() : GameUI.textColor();
            drawShadowedString(g, text1, 0, 2, x1, buttonY+buttonH-s10, GameUI.textShade(), textC);

            if (saveBox == hoverBox) {
                Stroke prev2 = g.getStroke();
                g.setStroke(stroke1);
                g.drawRoundRect(button1X,buttonY,buttonW,buttonH,s5,s5);
                g.setStroke(prev2);
            }

            // draw cancel button
            cancelBox.setBounds(button2X,buttonY,buttonW,buttonH);
            g.setColor(SystemPanel.buttonShadowC);
            g.fillRoundRect(button2X+s1,buttonY+s3,buttonW,buttonH,s8,s8);
            g.fillRoundRect(button2X+s2,buttonY+s4,buttonW,buttonH,s8,s8);
            g.setPaint(cancelBackC[GameUI.opt()]);
            g.fillRoundRect(button2X,buttonY,buttonW,buttonH,s5,s5);

            String text2 = text("SAVE_GAME_CANCEL");
            g.setFont(narrowFont(30));
            int sw2 = g.getFontMetrics().stringWidth(text2);
            int x2 = button2X + ((buttonW-sw2)/2);

            textC = (cancelBox == hoverBox) ? GameUI.textHoverColor() : GameUI.textColor();
            drawShadowedString(g, text2, 0, 2, x2, buttonY+buttonH-s10, GameUI.textShade(), textC);

            if (cancelBox == hoverBox) {
                Stroke prev2 = g.getStroke();
                g.setStroke(stroke1);
                g.drawRoundRect(button2X,buttonY,buttonW,buttonH,s5,s5);
                g.setStroke(prev2);
            }

            if (saving) {
                NoticeMessage.setStatus(text("SAVE_GAME_SAVING"));
                drawNotice(g,30);
            }
        }
        private void drawFilenameButton(Graphics g, int x, int y) {
            Color textC = fileNameBox == hoverBox ? GameUI.textHoverColor() : GameUI.textColor();
            g.setFont(narrowFont(20));
            String title = text("SAVE_GAME_FILENAME");
            int sw = g.getFontMetrics().stringWidth(title);
            fileNameBox.setBounds(x, y-lineH,sw,lineH);
            g.setColor(textC);
            drawString(g,title, x, y-(lineH/5));
        }
        private void drawSizeButton(Graphics g, int x, int y) {
            Color textC = fileSizeBox == hoverBox ? GameUI.textHoverColor() : GameUI.textColor();
            g.setFont(narrowFont(20));
            String title = text("SAVE_GAME_SIZE");
            int sw = g.getFontMetrics().stringWidth(title);
            fileSizeBox.setBounds(x-sw, y-lineH, sw, lineH);
            g.setColor(textC);
            drawString(g,title, x-sw, y-(lineH/5));
        }
        private void drawDateButton(Graphics g, int x, int y) {
            Color textC = fileDateBox == hoverBox ? GameUI.textHoverColor() : GameUI.textColor();
            g.setFont(narrowFont(20));
            String title = text("SAVE_GAME_DATE");
            int sw = g.getFontMetrics().stringWidth(title);
            fileDateBox.setBounds(x-sw, y-lineH, sw, lineH);
            g.setColor(textC);
            drawString(g,title, x-sw, y-(lineH/5));
        }
        private void scrollY(int deltaY) {
            yOffset += deltaY;
            if (yOffset > lineH) {
                yOffset -= lineH;
                scrollUp();
            }
            else if (yOffset < -lineH) {
                yOffset += lineH;
                scrollDown();
            }
        }
        private void drawSaveGame(Graphics2D g, int index, String filename, long sz, String dt, int x, int y, int w, int h) {
            Color c0 = (index != selectIndex) && (hoverBox == gameBox[index]) ? GameUI.loadHoverBackground() : Color.black;
            g.setColor(c0);
            g.setFont(narrowFont(20));
            int sw0 = g.getFontMetrics().stringWidth(filename);
            int maxW = w-scaled(250);
            g.setClip(x+s25, y+h-s30, maxW, s30);
            drawString(g,filename, x+s30, y+h-s8);
            g.setClip(null);
            if (sw0 > maxW)
                drawString(g,text("LOAD_GAME_TOO_LONG"), x+s25+maxW, y+h-s8);

            if (sz > 0) {
                String szStr = shortFmt(sz);
                int sw1 = g.getFontMetrics().stringWidth(szStr);
                drawString(g,szStr, x+w-scaled(150)-sw1, y+h-s8);
            }

            int sw2 = g.getFontMetrics().stringWidth(dt);
            drawString(g,dt, x+w-s30-sw2, y+h-s8);
        }
        @Override
        public void mouseDragged(MouseEvent e) {
            mouseMoved(e);
        }
        @Override
        public void mouseMoved(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();
            int deltaY = y - lastMouseY;
            lastMouseY = y;

            if (dragging && listBox.contains(x,y))
                scrollY(deltaY);

            Rectangle oldHover = hoverBox;
            hoverBox = null;

            if (saveBox.contains(x,y))
                hoverBox = saveBox;
            else if (cancelBox.contains(x,y))
                hoverBox = cancelBox;
            else if (fileNameBox.contains(x,y))
                hoverBox = fileNameBox;
            else if (fileDateBox.contains(x,y))
                hoverBox = fileDateBox;
            else if (fileSizeBox.contains(x,y))
                hoverBox = fileSizeBox;
            else {
                for (int i=0;i<gameBox.length;i++) {
                    if (gameBox[i].contains(x,y))
                        hoverBox = gameBox[i];
                }
            }

            if (hoverBox != oldHover)
                repaint();
        }
        @Override
        public void mouseClicked(MouseEvent arg0) {  }
        @Override
        public void mouseEntered(MouseEvent arg0) { }
        @Override
        public void mouseExited(MouseEvent arg0) {
            if (hoverBox != null) {
                hoverBox = null;
                repaint();
            }
        }
        @Override
        public void mousePressed(MouseEvent arg0) {
            dragging = true;
        }
        @Override
        public void mouseReleased(MouseEvent e) {
            dragging = false;
            if (e.getButton() > 3)
                return;
            int count = e.getClickCount();
            if (e.getSource() == newFileField) {
                newFileField.requestFocus();
                hoverBox = gameBox[0];
                if (count == 2)
                    newFileField.selectAll();
                return;
            }

            if (hoverBox == saveBox) {
                saveGame(fullSelectedFileName());
                return;
            }
            if (hoverBox == fileNameBox) {
                sortByFileName();
                return;
            }
            if (hoverBox == fileSizeBox) {
                sortBySize();
                return;
            }
            if (hoverBox == fileDateBox) {
                sortByDate();
                return;
            }
            if (hoverBox == cancelBox) {
                cancelSave();
                return;
            }
            if (hoverBox != selectBox) {
                softClick();
                selectBox = hoverBox;
                for (int i=0;i<gameBox.length;i++) {
                    if (gameBox[i] == hoverBox)
                        selectIndex = i;
                }
                if (selectIndex > 0) {
                    String saveName = saveFiles.get(start+selectIndex).trim();
                    newFileField.setText(saveName);
                }
                current.repaint();
            }
        }
    }
    class CancelAction extends AbstractAction {
        private static final long serialVersionUID = 1L;
        @Override
        public void actionPerformed(ActionEvent ev) {
            cancel();
        }
    }
    class SaveAction extends AbstractAction {
        private static final long serialVersionUID = 1L;
        @Override
        public void actionPerformed(ActionEvent ev) {
            saveGame();
        }
    }
    public static Comparator<File> FILE_NAME = (File f1, File f2) -> f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase());
    public static Comparator<File> FILE_DATE = (File f1, File f2) -> Long.valueOf(f2.lastModified()).compareTo(f1.lastModified());
    public static Comparator<File> FILE_SIZE = (File f1, File f2) -> Long.valueOf(f2.length()).compareTo(f1.length());
}
