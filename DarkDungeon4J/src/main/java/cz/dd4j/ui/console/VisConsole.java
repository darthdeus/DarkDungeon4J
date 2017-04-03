package cz.dd4j.ui.console;

import java.io.PrintStream;

import cz.dd4j.simulation.data.agents.actions.Action;
import cz.dd4j.simulation.data.dungeon.Element;
import cz.dd4j.simulation.data.dungeon.elements.entities.Hero;
import cz.dd4j.simulation.data.dungeon.elements.entities.Monster;
import cz.dd4j.simulation.data.dungeon.elements.features.Feature;
import cz.dd4j.simulation.data.state.SimState;
import cz.dd4j.simulation.events.ISimEvents;
import cz.dd4j.simulation.result.SimResult;

public class VisConsole implements ISimEvents {

	public final String WHO_SIMULATOR = "Simulator";
	
	protected final PrintStream out;
	
	protected long frameNumber;
	
	protected int frameLength = 3;
	protected int whoLength = 9;
	protected int whatLength = 15;
	
	public VisConsole() {
		out = System.out;
	}
	
	public VisConsole(PrintStream out) {
		this.out = out;
	}
	
	private void log(String who, String what, String description) {
		if (frameNumber > Math.pow(10, frameLength)) frameLength = (int)(Math.ceil(Math.log10(frameNumber)));
		if (who.length() > whoLength) whoLength = who.length();
		if (what.length() > whatLength) whatLength = what.length();
		out.printf("[%" + frameLength + "d] {%" + whoLength + "s} (%" + whatLength + "s) %s", frameNumber, who, what, description);
		out.println();
		//out.println("[" + frameNumber + "] {" + who + "} (" + what + ") " + description);
	}
	
	@Override
	public void simulationBegin(SimState state) {
		frameNumber = 0;
		log(WHO_SIMULATOR, "SimBegin", "Simulation begins.");
		log(WHO_SIMULATOR, "SimBegin", "ID:   " + state.id);
		log(WHO_SIMULATOR, "SimBegin", "DESC: " + state.description);
	}

	@Override
	public void simulationFrameBegin(long frameNumber, long simMillis) {
		this.frameNumber = frameNumber;
		log(WHO_SIMULATOR, "SimFrameBegin", "Simulation frame " + frameNumber + " begun, sim time " + simMillis + "ms.");
	}
	
	public String getName(Element who) {
		String result = who.getDescription();
		if (result != null) return result;
		if (who.name == null) return who.getClass().getSimpleName() + "-" + who.id;
		return who.name + "-" + who.id;
	}
	
	protected void actionPerforming(String state, Element who, Action what) {
		String description = "";
		if (what != null) {
			description += what;
		} else {
			description = "NO-ACTION";
		}
		log(getName(who), state, description);
	}
	
	@Override
	public void actionSelected(Element who, Action what) {
		if (what == null) return;
		actionPerforming("ACTION-SELECTED", who, what);
	}
	
	@Override
	public void actionStarted(Element who, Action what) {
		actionPerforming("ACTION-STARTED", who, what);
	}

	@Override
	public void actionEnded(Element who, Action what) {
		actionPerforming("ACTION-ENDED", who, what);
	}
	
	@Override
	public void actionInvalid(Element who, Action what) {
		actionPerforming("ACTION-INVALID", who, what);
	}

	@Override
	public void elementCreated(Element element) {
		log(getName(element), "CREATED", "");
	}
	
	@Override
	public void elementDead(Element element) {
		log(getName(element), "DESTROYED", "");
	}

	@Override
	public void simulationFrameEnd(long frameNumber) {
		log(WHO_SIMULATOR, "SimFrameEnd", "Simulation frame " + frameNumber + " ended.");
	}

	@Override
	public void simulationEnd(SimResult result) {
		log(WHO_SIMULATOR, "SimEnd", "Simulation ended in frame " + result.frameNumber + ", time " + result.simTimeMillis + "ms.");
		log(WHO_SIMULATOR, "SimEndResult", getResultDescription(result));
	}

	private String getResultDescription(SimResult result) {
		switch (result.resultType) {
		case HERO_EXCEPTION: return result.resultType + "[Hero code exception.]";
		case HERO_WIN: return result.resultType + "[" + getName(result.winner.body) + "]";
		case HEROES_LOSE: return result.resultType + "[All heroes are dead.]";
		case SIMULATION_EXCEPTION: return result.resultType + "[Simulation code exception.]";
		default:
			return result.resultType + "[UNKNOWN RESULT]";
		}
	}

	

	

}
