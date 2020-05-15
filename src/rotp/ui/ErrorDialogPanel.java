package rotp.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import rotp.ui.main.SystemPanel;

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
public class ErrorDialogPanel extends BasePanel implements MouseListener {
    private static final long serialVersionUID = 1L;
    private static final Color backgroundHaze = new Color(0,0,0,160);
    String error = "";
    public ErrorDialogPanel(String msg) {
        error = msg;
        initModel();
    }
    private void initModel() {
        setOpaque(false);
        addMouseListener(this);
    }
    @Override
    public void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        
        Graphics2D g = (Graphics2D) g0;

        int w = getWidth();
        int h = getHeight();

        // draw background "haze"
        g.setColor(backgroundHaze);
        g.fillRect(0, 0, w, h);
        // get length of title and determine box width
        g.setFont(narrowFont(20));
        String title = text(error);
        int titleSW = g.getFontMetrics().stringWidth(title);

        int boxWidth = titleSW+s70;
        int boxHeight = scaled(40);

        int x0 = (w - boxWidth)/2;
        int y0 = h/3;
        
        // draw box
        g.setColor(Color.white);
        g.fillRect(x0, y0, boxWidth, boxHeight);
        g.setColor(Color.black);
        g.fillRect(x0+s3, y0+s3, boxWidth-s6, boxHeight-s6);

        g.setColor(SystemPanel.whiteText);
        g.drawString(title, x0+s35, y0+boxHeight-s13);
    }
    private void exit() {
        softClick();
        disableGlassPane();
    }
    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_ESCAPE) {
            exit();
            return;
        }
    }
    @Override
    public void mouseClicked(MouseEvent e) {}
    @Override
    public void mousePressed(MouseEvent e) { }
    @Override
    public void mouseReleased(MouseEvent e) {
        exit();
    }
    @Override
    public void mouseEntered(MouseEvent e) { }
    @Override
    public void mouseExited(MouseEvent e) { }
}
