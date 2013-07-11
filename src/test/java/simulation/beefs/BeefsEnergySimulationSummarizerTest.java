package simulation.beefs;

import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import manelsim.Context;
import manelsim.EventScheduler;
import manelsim.EventSource;
import manelsim.EventSourceMultiplexer;
import manelsim.Time;
import manelsim.Time.Unit;

import org.junit.Test;

import simulation.beefs.energy.Conservative;
import simulation.beefs.model.DataServer;
import simulation.beefs.model.Machine;

public class BeefsEnergySimulationSummarizerTest {
	@Test
	public void summarizeTest() {
		BeefsEnergySimulationSummarizer summarizer = new BeefsEnergySimulationSummarizer();
		
		EventScheduler.setup(Time.GENESIS, Time.THE_FINAL_JUDGMENT, new EventSourceMultiplexer(new EventSource[0]));
		
		Machine machine = new Machine("jurupoca", new Time(900, Unit.SECONDS), new Time(2500, Unit.MILLISECONDS));
		machine.setIdle(Time.GENESIS, new Time(87300000, Unit.SECONDS));
		
		DataServer dataServer = new DataServer(machine);
		
		Set<DataServer> dataServers = new HashSet<DataServer>();
		dataServers.add(dataServer);
		
		EventScheduler.start();
		
		Context context = new Context(null);
		context.add(BeefsEnergySimulationConstants.ENERGY_CONSUMPTION_MODEL, new Conservative());
		context.add(BeefsEnergySimulationConstants.DATA_SERVERS, dataServers);

		String summary = summarizer.summarize(context);

		assertTrue(summary.contains("jurupoca"));
		assertTrue(summary.contains("72.776353 kWh"));
		assertTrue(summary.contains("1 transitions"));
	}
}
