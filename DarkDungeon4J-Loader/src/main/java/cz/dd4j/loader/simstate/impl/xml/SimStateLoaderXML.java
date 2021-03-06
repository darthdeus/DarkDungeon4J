package cz.dd4j.loader.simstate.impl.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cz.dd4j.agents.IAgent;
import cz.dd4j.agents.IFeatureAgent;
import cz.dd4j.agents.IHeroAgent;
import cz.dd4j.agents.IMonsterAgent;
import cz.dd4j.loader.LoaderXML;
import cz.dd4j.loader.agents.AgentsLoader;
import cz.dd4j.loader.dungeon.DungeonLoader;
import cz.dd4j.loader.simstate.ISimStateLoaderImpl;
import cz.dd4j.simulation.data.agents.AgentMindBody;
import cz.dd4j.simulation.data.agents.Agents;
import cz.dd4j.simulation.data.dungeon.Dungeon;
import cz.dd4j.simulation.data.dungeon.elements.entities.Feature;
import cz.dd4j.simulation.data.dungeon.elements.entities.Hero;
import cz.dd4j.simulation.data.dungeon.elements.entities.Monster;
import cz.dd4j.simulation.data.dungeon.elements.items.Item;
import cz.dd4j.simulation.data.dungeon.elements.places.Room;
import cz.dd4j.simulation.data.state.SimState;
import cz.dd4j.utils.Id;

public class SimStateLoaderXML extends LoaderXML<SimStateXML> implements ISimStateLoaderImpl {
	
	public SimStateLoaderXML() {
		super(SimStateXML.class);
	}

	@Override
	public SimState loadSimState(File xmlFile) {
		SimStateXML simStateXML = load(xmlFile);
		
		if (simStateXML.dungeons.size() == 0) {
			throw new RuntimeException("SimState does not contain any dungeon definition! File: " + xmlFile.getAbsolutePath());
		}
		
		List<File> dungeonXMLFiles = new ArrayList<File>(simStateXML.dungeons.size());
		List<File> agentsXMLFiles = new ArrayList<File>(simStateXML.agents.size());
		
		for (FileXML dungeon : simStateXML.dungeons) {
			dungeonXMLFiles.add(new File(xmlFile.getParent(), dungeon.path));			
		}
		
		for (FileXML agents : simStateXML.agents) {
			agentsXMLFiles.add(new File(xmlFile.getParent(), agents.path));			
		}
		
		return loadSimState(dungeonXMLFiles, agentsXMLFiles);
	}
	
	public SimState loadSimState(List<File> dungeonXMLFiles, List<File> agentsXMLFiles) {
		
		Dungeon dungeon = new Dungeon();
		Agents<IMonsterAgent> monsters = new Agents<IMonsterAgent>();
		Agents<IFeatureAgent> features = new Agents<IFeatureAgent>();
				
		// LOAD DUNGEON FILES, ADDITIVELY BLEND LATER ONES OVER EARLIER ONES...
		DungeonLoader dungeonLoader = new DungeonLoader();		
		for (File dungeonXMLFile : dungeonXMLFiles) {
			Dungeon append = dungeonLoader.loadDungeon(dungeonXMLFile);
			blend(dungeon, append);
		}
		
		// LOAD AGENT FILES, ADDITIVELY BLEND LATER ONES OVER EARLIER ONES...
		AgentsLoader agentsLoader = new AgentsLoader();
		for (File agentsXMLFile : agentsXMLFiles) {
			Agents append = agentsLoader.loadAgents(agentsXMLFile);
			blend(monsters, features, append);
		}
		
		// SET UP THE SimState
		SimState state = new SimState();
		
		state.dungeon = dungeon;
		
		// TINKER THE SimState
		
		// search for heroes and monsters and features...
		// ...create their MindBodies...
		for (Room room : state.dungeon.rooms.values()) {
			if (room.hero != null) {
				if (state.heroes.containsKey(room.hero.id)) throw new RuntimeException("There are more than one Hero[id=" + room.hero.id + "] within the state!");
				AgentMindBody<Hero, IHeroAgent> hero = new AgentMindBody<Hero, IHeroAgent>();
				hero.body = room.hero;
				hero.body.atRoom = room;
				state.heroes.put(hero.body.id, hero);					
			}
			if (room.monster != null) {
				if (state.monsters.containsKey(room.monster.id)) throw new RuntimeException("There are more than one Monster[id=" + room.monster.id + "] within the state!");
				AgentMindBody<Monster, IMonsterAgent> monster = new AgentMindBody<Monster, IMonsterAgent>();
				monster.body = room.monster;
				monster.body.atRoom = room;
				monster.mind = monsters.agents.get(monster.body.id);
				if (monster.mind == null) {
					throw new RuntimeException("Monster agent not specified for the Monster[id=" + monster.body.id +"].");
				}
				state.monsters.put(monster.body.id, monster);					
			}
			if (room.feature != null) {
				if (state.features.containsKey(room.feature.id)) throw new RuntimeException("There are more than one Feature[id=" + room.feature.id + "] within the state!");
				AgentMindBody<Feature, IFeatureAgent> feature = new AgentMindBody<Feature, IFeatureAgent>();
				feature.body = room.feature;
				feature.body.atRoom = room;
				feature.mind = features.agents.get(feature.body.id);
				if (feature.mind == null) {
					throw new RuntimeException("Feature agent not specified for the Feature[id=" + feature.body.id +"].");
				}
				state.features.put(feature.body.id, feature);	
			}
		}
		
		// WE'RE DONE!
		
		return state;
	}
	
	public static void blend(Dungeon target, Dungeon append) {
		for (Map.Entry<Id, Room> entry : append.rooms.entrySet()) {
			Room targetRoom = target.rooms.get(entry.getKey());
			if (targetRoom == null) {
				target.rooms.put(entry.getKey(), entry.getValue());
			} else {
				blend(targetRoom, entry.getValue());
			}
		}
	}
	
	private static void blend(Room targetRoom, Room append) {
		if (append.label != null) {
			targetRoom.label = append.label;
		}
		if (targetRoom.monster == null) {
			targetRoom.monster = append.monster;
		} else
		if (append.monster != null) {
			blend(targetRoom.monster, append.monster);
		}
		if (targetRoom.feature == null) {
			targetRoom.feature = append.feature;
		} else
		if (append.feature != null) {
			blend(targetRoom.feature, append.feature);
		}
		if (targetRoom.hero == null) {
			targetRoom.hero = append.hero;
		} else
		if (append.hero != null) {
			blend(targetRoom.hero, append.hero);
		}
		if (targetRoom.item == null) {
			targetRoom.item = append.item;
		} else
		if (append.item != null) {
			blend(targetRoom.item, append.item);
		}
	}

	private static void blend(Item target, Item append) {
		if (append.type != null) {
			throw new RuntimeException("Cannot overwrite existing item! " + target + " <- " + append);
		}
	}

	private static void blend(Hero target, Hero append) {
		if (target.hand == null) {
			target.hand = append.hand;
		} else 
		if (append.hand != null) {
			blend(target.hand, append.hand);
		}
		for (Map.Entry<Id, Item> entry : append.inventory.getData().entrySet()) {
			Item targetItem = target.inventory.get(entry.getKey());
			if (targetItem == null) {
				target.inventory.add(entry.getValue());
			} else
			if (entry.getValue() != null) {
				blend(targetItem, entry.getValue());
			}
		}		 
	}

	private static void blend(Feature target, Feature append) {
	}

	private static void blend(Monster target, Monster append) {		
	}

	public static void blend(Agents<IMonsterAgent> monsters, Agents<IFeatureAgent> features, Agents append) {
		for (Object entryObj : append.agents.entrySet()) {
			Map.Entry<Id, IAgent> entry = (Map.Entry<Id, IAgent>)entryObj;
			if (entry.getValue() instanceof IMonsterAgent) {
				monsters.agents.put(entry.getKey(), (IMonsterAgent)entry.getValue());
			}
			if (entry.getValue() instanceof IFeatureAgent) {
				features.agents.put(entry.getKey(), (IFeatureAgent)entry.getValue());
			}
		}
	}

}
