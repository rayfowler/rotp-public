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

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Toolkit;

public enum CursorManager implements Base {
    INSTANCE;
    public static CursorManager current()    { return INSTANCE; }

    private static Cursor[] cursor = new Cursor[19];
    private static final int CURSOR_HAND = 0;
    private static final int CURSOR_SHIP = 1;
    private static final int CURSOR_CROSSHAIRS = 2;
    private static final int CURSOR_PTR_SCRAP = 3;
    private static final int CURSOR_PTR_SHIP = 4;
    private static final int CURSOR_PTR_TO = 5;
    private static final int CURSOR_PTR_WHO = 6;
    private static final int CURSOR_SHIP_INVALID = 7;
    private static final int CURSOR_SHIP_TARGET = 8;
    private static final int CURSOR_SHIP_TARGET_DONE = 9;
    private static final int CURSOR_QUESTION = 10;
    private static final int CURSOR_SHIP_N = 11;
    private static final int CURSOR_SHIP_S = 12;
    private static final int CURSOR_SHIP_E = 13;
    private static final int CURSOR_SHIP_W = 14;
    private static final int CURSOR_SHIP_NE = 15;
    private static final int CURSOR_SHIP_NW = 16;
    private static final int CURSOR_SHIP_SE = 17;
    private static final int CURSOR_SHIP_SW = 18;

    static {
        current().loadCursors();
    }
    private void loadCursors() {
        Point topLeft = new Point(0,0);
        Point center = new Point(15,15);
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        cursor[CURSOR_HAND] = toolkit.createCustomCursor(icon("images/cursors/hand.png").getImage(), topLeft, "moo_hand");
        cursor[CURSOR_SHIP] = toolkit.createCustomCursor(icon("images/cursors/ship.png").getImage(), topLeft, "moo_ship");
        cursor[CURSOR_CROSSHAIRS] = toolkit.createCustomCursor(icon("images/cursors/crosshairs.png").getImage(), center, "moo_crosshairs");
        cursor[CURSOR_PTR_SCRAP] = toolkit.createCustomCursor(icon("images/cursors/ptr_scrap.png").getImage(), topLeft, "moo_ptr_scrap");
        cursor[CURSOR_PTR_SHIP] = toolkit.createCustomCursor(icon("images/cursors/ptr_ship.png").getImage(), topLeft, "moo_ptr_ship");
        cursor[CURSOR_PTR_TO] = toolkit.createCustomCursor(icon("images/cursors/ptr_to.png").getImage(), topLeft, "moo_ptr_to");
        cursor[CURSOR_PTR_WHO] = toolkit.createCustomCursor(icon("images/cursors/ptr_who.png").getImage(), topLeft, "moo_ptr_who");
        cursor[CURSOR_SHIP_INVALID] = toolkit.createCustomCursor(icon("images/cursors/ship_invalid.png").getImage(), topLeft, "moo_ship_invalid");
        cursor[CURSOR_SHIP_TARGET] = toolkit.createCustomCursor(icon("images/cursors/ship_target.png").getImage(), center, "moo_ship_target");
        cursor[CURSOR_SHIP_TARGET_DONE] = toolkit.createCustomCursor(icon("images/cursors/ship_target_done.png").getImage(), center, "moo_ship_target_done");
        cursor[CURSOR_QUESTION] = toolkit.createCustomCursor(icon("images/cursors/ship_prompt.png").getImage(), center, "moo_ship_prompt");
        cursor[CURSOR_SHIP_N] = toolkit.createCustomCursor(icon("images/cursors/ship_N.png").getImage(), topLeft, "moo_ship_n");
        cursor[CURSOR_SHIP_S] = toolkit.createCustomCursor(icon("images/cursors/ship_S.png").getImage(), topLeft, "moo_ship_s");
        cursor[CURSOR_SHIP_E] = toolkit.createCustomCursor(icon("images/cursors/ship_E.png").getImage(), topLeft, "moo_ship_e");
        cursor[CURSOR_SHIP_W] = toolkit.createCustomCursor(icon("images/cursors/ship_W.png").getImage(), topLeft, "moo_ship_w");
        cursor[CURSOR_SHIP_NE] = toolkit.createCustomCursor(icon("images/cursors/ship_NE.png").getImage(), topLeft, "moo_ship_ne");
        cursor[CURSOR_SHIP_NW] = toolkit.createCustomCursor(icon("images/cursors/ship_NW.png").getImage(), topLeft, "moo_ship_nw");
        cursor[CURSOR_SHIP_SE] = toolkit.createCustomCursor(icon("images/cursors/ship_SE.png").getImage(), topLeft, "moo_ship_se");
        cursor[CURSOR_SHIP_SW] = toolkit.createCustomCursor(icon("images/cursors/ship_SW.png").getImage(), topLeft, "moo_ship_sw");
    }

    public Cursor defaultCursor()         { return cursor[CURSOR_HAND];  }
    public Cursor crosshairs()            { return cursor[CURSOR_CROSSHAIRS]; }
    public Cursor scrap()                 { return cursor[CURSOR_PTR_SCRAP]; }
    public Cursor to()                    { return cursor[CURSOR_PTR_TO]; }
    public Cursor prompt()                { return cursor[CURSOR_QUESTION]; }
}
