/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rotp.model.events;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import rotp.model.empires.Empire;
import rotp.util.Base;


/**
 *  The purpose of this class is to allow the creation of custom events for mods
 *  will not create saves that are incompatible with the base game. Basically, 
 *  each generic event is created with a unique string key and then all of the 
 *  behavior methods will then toggle based on that key. This one class would 
 *  basically contain ALL of the behavior for all custom events. Spawn off private
 *  methods as needed to organize code, but creating new classes will "break" saves
 *  in the base game.
 * 
 *  When a save with custom events is loaded in the base game, they all have the 
 *  same behavior... do nothing when trigger. If already active in the save, then
 *  do nothing on Next Turn. 
 * 
 * @author RayGame
 */
public class RandomEventGeneric   implements Base, Serializable, RandomEvent {
    private static final long serialVersionUID = 1L;
    private String eventKey;
    private Map<String,Object> eventData = new HashMap<>();
    public RandomEventGeneric(String key) {
        eventKey = key;
        initData(key);
    }
    @Override
    public boolean monsterEvent() { 
        // monsterEvent flag for each event. 
        switch(eventKey) {
            default:
                return false; 
        }
    }
    @Override
    public int minimumTurn() { 
        // minimum turn for each event. 
        switch(eventKey) {
            default:
                return RandomEvents.START_TURN; 
        }
    }
    @Override
    public String statusMessage() { 
        // statusMessage behavior for each event. Probably should spin off
        // a separate private method for each event
        switch(eventKey) {
            default:
                return ""; 
        }
    }
    @Override
    public String systemKey() { 
        // system key for each event. 
        switch(eventKey) {
            default:
                return ""; 
        }
    }
    @Override
    public boolean goodEvent() { 
        // good event flag for each event. 
        switch(eventKey) {
            default:
            return true; 
        }
    }
    @Override
    public boolean repeatable() { 
        // repeatable flag for each event. 
        switch(eventKey) {
            default:
            return false; 
        }
    }
    @Override
    public String notificationText() {
        // notificationText behavior for each event. Probably should spin off
        // a separate private method for each event
        switch(eventKey) {
            default:
            return ""; 
        }
    }
    @Override
    public void trigger(Empire emp) {
        // trigger behavior for each event. Probably should spin off
        // a separate private method for each event
        switch(eventKey) {
            default:
                
        }
    }
    private void initData(String key) {
        // this is where you define the data map for each particuar generic
        // event. The default is to give each generic event a map keyed for 
        // a objectname and an object
        
        // you don't have to init here. You can do it in the trigger method
        // if it makes more sense.
        
        switch(key) {
            //example:
            //case "PirateRampageEvent":
            //  eventData.put("SystemList", new ArrayList<StarSystem>());
            //  eventData.put("PirateHP", new Integer(500));
            //  break;
            default:
               
        }
        
    }
}
