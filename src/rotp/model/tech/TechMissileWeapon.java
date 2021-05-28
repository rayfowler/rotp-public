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
package rotp.model.tech;

import java.awt.Graphics2D;
import java.awt.Image;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import rotp.model.colony.Colony;
import rotp.model.colony.MissileBaseMissile;
import rotp.model.combat.CombatStack;
import rotp.model.empires.Empire;
import rotp.model.game.GameSession;
import rotp.model.ships.ShipWeaponMissile;
import rotp.ui.RotPUI;
import rotp.ui.combat.ShipBattleUI;

public final class TechMissileWeapon extends Tech {
    public static List<String> missileTypes = new ArrayList<>();
    public static List<List<ImageIcon>> missileIcons = new ArrayList<>();

    public MissileBaseMissile baseMissile;
    public String imageType = "MISSILE";
    public String imageKey = "";
    private int damage = 0;
    public float speed = 1;
    public float speed2 = 1;
    public int computer = 0;
    public int attacks = 1;

    public boolean shipOnly = false;

    public int shots = 1;
    public int range = 0;
    public int shots2 = 0;
    public int range2 = 0;
    public int damageLoss = 0;

    public int scatterAttacks()  { return attacks; }
    public int damage()   { return (int) (session().damageBonus() * damage); }

    @Override
    public String imageKey()   { return imageKey; }

    public TechMissileWeapon(String typeId, int lv, int seq, boolean b, TechCategory c) {
        id(typeId, seq);
        typeSeq = seq;
        level = lv;
        cat = c;
        free = b;
        init();
    }
    @Override
    public boolean canBeMiniaturized()      { return true; }
    @Override
    public Colony.Orders followup()	      { return Colony.Orders.BASES; }
    @Override
    public float baseReallocateAmount()          { return .10f; }
    public ImageIcon icon(int count) {
        List<ImageIcon> icons = iconsForType(imageType);
        int i = (int) Math.sqrt(count);
        if (i < 1)
            return icons.get(0);
        else if (i > icons.size())
            return icons.get(icons.size()-1);
        else
            return icons.get(i-1);
    }
    public Image image(int count) {
            return icon(count).getImage();
    }
    @Override
    public void init() {
        super.init();
        techType = Tech.MISSILE_WEAPON;

        switch(typeSeq) {
            case 0: // NUCLEAR MISSILE
                damage = 4;
                speed = 3; speed2 = 2;
                cost = 11;
                size = 50;
                power = 20;
                shots = 2;  shots2 = 5;
                range = 6;  range2 = 4;
                baseMissile = new MissileBaseMissile(this, 27);
                imageKey = "MISSILE_NUCLEAR";
                break;
            case 1: // HYPER-V ROCKETS
                damage = 6;
                speed = 3.5f; speed2 = 2.5f;
                size = 70;
                power = 20;
                cost = 12;
                shots = 2;  shots2 = 5;
                range = 7;  range2 = 5;
                baseMissile = new MissileBaseMissile(this, 38);
                imageKey = "MISSILE_HYPER_V";
                break;
            case 2: // HYPER-X ROCKETS
                damage = 8;
                speed = 3.5f; speed2 = 2.5f;
                size = 100;
                power = 20;
                cost = 14;
                computer = 1;
                shots = 2;  shots2 = 5;
                range = 7;  range2 = 5;
                baseMissile = new MissileBaseMissile(this, 65);
                imageKey = "MISSILE_HYPER_X";
                break;
            case 3: // SCATTER PACK V ROCKETS
                damage = 6;
                speed = 3.5f; speed2 = 2.5f;
                size = 115;
                power = 50;
                cost = 28;
                attacks = 5;
                shots = 2;  shots2 = 5;
                range = 7;  range2 = 5;
                baseMissile = new MissileBaseMissile(this, 97);
                imageKey = "MISSILE_SCATTER_PACK_V";
                break;
            case 4: // MERCULITE MISSILES
                damage = 10;
                speed = 4; speed2 = 3;
                size = 105;
                power = 20;
                cost = 15;
                computer = 2;
                shots = 2;  shots2 = 5;
                range = 8;  range2 = 6;
                baseMissile = new MissileBaseMissile(this, 70);
                imageKey = "MISSILE_MERCULITE";
                break;
            case 5: // STINGER MISSILES
                damage = 15;
                speed = 4.5f; speed2 = 3.5f;
                size = 155;
                power = 30;
                cost = 25;
                computer = 3;
                shots = 2;  shots2 = 5;
                range = 9;  range2 = 7;
                baseMissile = new MissileBaseMissile(this, 84);
                imageKey = "MISSILE_STINGER";
                break;
            case 6: // SCATTER PACK VII MISSILES
                damage = 10;
                speed = 4; speed2 = 3;
                cost = 50;
                size = 230;
                power = 50;
                computer = 2;
                attacks = 7;
                shots = 2;  shots2 = 5;
                range = 8;  range2 = 6;
                baseMissile = new MissileBaseMissile(this, 151);
                imageKey = "MISSILE_SCATTER_PACK_VII";
                break;
            case 7: // PULSON MISSILES
                damage = 20;
                speed = 5; speed2 = 4;
                cost = 25;
                size = 160;
                power = 40;
                computer = 4;
                shots = 2;  shots2 = 5;
                range = 10;  range2 = 8;
                baseMissile = new MissileBaseMissile(this, 108);
                imageKey = "MISSILE_PULSON";
                break;
            case 8: // HERCULAR MISSILES
                damage = 25;
                speed = 5.5f; speed2 = 4.5f;
                size = 220;
                power = 40;
                cost = 30;
                computer = 5;
                shots = 2;  shots2 = 5;
                range = 10;  range2 = 9;
                baseMissile = new MissileBaseMissile(this, 141);
                imageKey = "MISSILE_HERCULAR";
                break;
            case 9: // ZEON MISSILES
                damage = 30;
                speed = 6; speed2 = 5;
                size = 250;
                power = 50;
                cost = 36;
                computer = 6;
                shots = 2;  shots2 = 5;
                range = 10;  range2 = 10; // modnar: correct missile range
                baseMissile = new MissileBaseMissile(this, 162);
                imageKey = "MISSILE_ZEON";
                break;
            case 10: // SCATTER PACK X MISSILES
                damage = 15;
                speed = 4.5f; speed2 = 3.5f;
                size = 250;
                power = 50;
                cost = 36;
                computer = 3;
                attacks = 10;
                shots = 2;  shots2 = 5;
                range = 9;  range2 = 7; // modnar: correct missile range
                baseMissile = new MissileBaseMissile(this, 162);
                imageKey = "MISSILE_SCATTER_PACK_X";
                break;
        }
    }
    @Override
    public float baseValue(Empire c) { return c.ai().scientist().baseValue(this); }
    @Override
    public float warModeFactor()           { return 2; }
    @Override
    public boolean isMissileWeaponTech()    { return true; }
    @Override
    public boolean isMissileBaseWeapon()    { return (baseMissile != null); }
    public float range()			       { return 0; }
    @Override
    public boolean providesShipComponent()  { return true; }
    @Override
    public boolean isObsolete(Empire c) {
        if (isMissileBaseWeapon()) {
            if (attacks > 1) 
                return (c.tech().topBaseScatterPackTech() != null) && (typeSeq < c.tech().topBaseScatterPackTech().typeSeq);
            else
                return (c.tech().topBaseMissileTech() != null) && (typeSeq < c.tech().topBaseMissileTech().typeSeq);
        }
        return false;
    }
    @Override
    public void provideBenefits(Empire c) {
        super.provideBenefits(c);
        if (isMissileBaseWeapon()) {
            if (!isObsolete(c)) {
                if (attacks > 1) 
                    c.tech().topBaseScatterPackTech(this);
                else
                    c.tech().topBaseMissileTech(this);
                c.tech().updateMissileBase();
            }
        }
        c.shipLab().addWeapon(new ShipWeaponMissile(this, false, shots, range, speed));
        c.shipLab().addWeapon(new ShipWeaponMissile(this, true, shots2, range2, speed2));
    }
    public static List<ImageIcon> iconsForType(String typeName) {
        int i = missileTypes.indexOf(typeName);
        if ( (i >=0) && (i < missileIcons.size()))
            return missileIcons.get(i);
        else
            return new ArrayList<>();
    }
    public static void startup() {
        loadMissileImages();
    }
    public static void loadMissileImages() {
        BufferedReader in = RotPUI.instance().reader("images/missiles/missiles.txt");
        try {
            String input;
            while ((input = in.readLine()) != null)
                addMissileImageType(input);
            in.close();
        }
        catch (IOException e) {
            System.err.println("MissileWeaponTech.loadMissileImages -- IOException: " + e);
        }
    }
    public static void addMissileImageType(String input) {
        if (input.trim().isEmpty())
            return;

        char char0 = input.charAt(0);

        // check for comment strings
        if ((char0 == '/') || (char0 == '\\') || (char0 == '*'))
            return;

        int from = 0;

        // get image type name,
        int mark = input.indexOf(',', from);
        String typeName = input.substring(from, mark).trim();

        if (typeName.isEmpty())
            return;

        boolean moreFileNames = true;
        List<ImageIcon> images = new ArrayList<>();

        while (moreFileNames) {
            moreFileNames = false;
            from = mark + 1;
            mark = input.indexOf(',', from);
            if (mark > from) {
                String fileName = input.substring(from, mark).trim();
                if (!fileName.isEmpty()) {
                    moreFileNames = true;
                    ImageIcon icon = GameSession.instance().icon("images/missiles/"+fileName);
                    if (icon == null)
                        System.err.println("MissileWeaponTech.addMissileImageType -- null icon from file: " + fileName);
                    else
                        images.add(icon);
                }
            }
        }

        if (!images.isEmpty()) {
            missileTypes.add(typeName);
            missileIcons.add(images);
        }
    }
    @Override
    public void drawSuccessfulAttack(CombatStack nullStack, CombatStack target, int wpnNum, float dmg) {
        ShipBattleUI ui = target.mgr.ui;
        if (ui == null)
            return;

        int stW = ui.stackW();
        int stH = ui.stackH();
        int st1X = ui.stackX(target);
        int st1Y = ui.stackY(target);

        int x1 = st1X+stW/2;
        int y1 = st1Y+stH/2;

        Graphics2D g = (Graphics2D) ui.getGraphics();
        target.drawAttackResult(g,x1,y1,x1, dmg,text("SHIP_COMBAT_MISS"));   
        ui.paintAllImmediately();
    }
}
