package simulation.beefs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import manelsim.Context;
import manelsim.EventSource;
import manelsim.EventSourceMultiplexer;
import manelsim.Initializer;
import manelsim.Time;
import manelsim.Time.Unit;
import simulation.beefs.energy.EnergyConsumptionModel;
import simulation.beefs.event.filesystem.FileSystemTraceEventSource;
import simulation.beefs.event.machine.UserActivityTraceEventSource;
import simulation.beefs.model.DataServer;
import simulation.beefs.model.FileSystemClient;
import simulation.beefs.model.Machine;
import simulation.beefs.model.MetadataServer;
import simulation.beefs.placement.DataPlacement;

/**
 * 
 * @author Patrick Maia
 *
 */
public class BeefsEnergySimulationInitializer implements Initializer {
	
	private static final FilenameFilter fsTracesFilter = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			return name.startsWith("fs-");
		}
	};

	private static final FilenameFilter idlenessTracesFilter = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			return name.startsWith("idleness-");
		}
	};
	
	@Override
	public Context initialize(Properties config) {
		
		String tracesDirPath = config.getProperty(BeefsEnergySimulationConstants.TRACES_DIR);
		File tracesDir = new File(tracesDirPath);
		if(!tracesDir.exists() || !tracesDir.isDirectory())
			throw new IllegalArgumentException(tracesDirPath + " doesn't exist or is not a directory");

		// create machines
		Time toSleepTimeout = 
				new Time(Long.valueOf(config.getProperty(BeefsEnergySimulationConstants.TO_SLEEP_TIMEOUT)), Unit.SECONDS);
		Time transitionDuration = 
				new Time(Long.valueOf(config.getProperty(BeefsEnergySimulationConstants.TRANSITION_DURATION)), Unit.MILLISECONDS);
		Set<Machine> machines = 
				createMachines(tracesDir, toSleepTimeout, transitionDuration);

		// create data servers
		Set<DataServer> dataServers = createDataServers(machines);

		//create metadata server
		String placementPolicyName = config.getProperty(BeefsEnergySimulationConstants.PLACEMENT_POLICE);
		DataPlacement placementPolicy = DataPlacement.newDataPlacement(placementPolicyName, dataServers);
		Integer replicationLevel = Integer.valueOf(config.getProperty(BeefsEnergySimulationConstants.REPLICATION_LEVEL));
		Time timeToCoherence = 
				new Time(Long.valueOf(config.getProperty(BeefsEnergySimulationConstants.TIME_TO_COHERENCE)), Unit.SECONDS);
		Time timeToDelete = 
				new Time(Long.valueOf(config.getProperty(BeefsEnergySimulationConstants.TIME_TO_DELETE_REPLICAS)), Unit.SECONDS);
		
		Boolean wakeOnLan = Boolean.valueOf(config.getProperty(BeefsEnergySimulationConstants.WAKE_ON_LAN));
		MetadataServer metadataServer = 
				new MetadataServer(dataServers, placementPolicy, replicationLevel, timeToCoherence, timeToDelete, wakeOnLan); 

		// create clients
		Set<FileSystemClient> clients = createClients(machines, metadataServer, wakeOnLan);
		
		// instantiate energy consumption model
		String energyConsumptionModelClassName = 
				config.getProperty(BeefsEnergySimulationConstants.ENERGY_CONSUMPTION_MODEL); 
		
		EnergyConsumptionModel energyConsumptionModel;
		try {
			energyConsumptionModel = (EnergyConsumptionModel)Class.forName(energyConsumptionModelClassName).newInstance();
		} catch (Throwable t) {
			throw new RuntimeException("Could not instantiate EnergyConsumptionModel", t);
		}
		
		// setup context
		Time emulationStartTime = 
			new Time(Long.parseLong(config.getProperty(BeefsEnergySimulationConstants.EMULATION_START_TIME)), Unit.SECONDS);
		EventSourceMultiplexer eventSourceMultiplexer = 
				createEventSourceMultiplexer(clients, machines, emulationStartTime, tracesDir);
		
		Context context = new Context(eventSourceMultiplexer);
		context.add(BeefsEnergySimulationConstants.MACHINES, machines);
		context.add(BeefsEnergySimulationConstants.DATA_SERVERS, dataServers);
		context.add(BeefsEnergySimulationConstants.METADATA_SERVER, metadataServer);
		context.add(BeefsEnergySimulationConstants.CLIENTS, clients);
		context.add(BeefsEnergySimulationConstants.ENERGY_CONSUMPTION_MODEL, energyConsumptionModel);

		return context;
	}

	private EventSourceMultiplexer createEventSourceMultiplexer(Set<FileSystemClient> clients, 
			Set<Machine> machines, Time emulationStartTime, File tracesDir) {
	
		EventSource []  parsers = new EventSource[machines.size() + clients.size()];
	
		try {
			int parserCount = 0;
			InputStream traceStream;
			for(Machine machine : machines) {
				traceStream = 
					new FileInputStream(new File(tracesDir, "idleness-" + machine.getName()));
				parsers[parserCount++] = new UserActivityTraceEventSource(machine, traceStream, emulationStartTime);
			}
			for(FileSystemClient client : clients) {
				traceStream = 
					new FileInputStream(new File(tracesDir, "fs-" + client.getHost().getName()));
				parsers[parserCount++] = new FileSystemTraceEventSource(client, traceStream);
			}
			
		} catch (FileNotFoundException e) {
			throw new IllegalStateException(e);
		}
		
		return new EventSourceMultiplexer(parsers);
	}
	
	private Set<Machine> createMachines(File tracesDir, Time toSleepTimeout, Time transitionDuration) {
		Set<Machine> machines = new HashSet<Machine>();
		List<String> fsTracesFiles = Arrays.asList(tracesDir.list(fsTracesFilter));
		List<String> idlenessTracesFiles = Arrays.asList(tracesDir.list(idlenessTracesFilter));
	
		for(String fsTraceFile : fsTracesFiles) {
			String machineName = fsTraceFile.split("-")[1];
			if(idlenessTracesFiles.contains("idleness-" + machineName)) {
				machines.add(new Machine(machineName, toSleepTimeout, transitionDuration));
			}
		}
	
		return machines;
	}
	
	private Set<FileSystemClient> createClients(Set<Machine> machines, 
			MetadataServer metadataServer, boolean wakeOnLan) {
	
		Set<FileSystemClient> newClients = new HashSet<FileSystemClient>();
	
		for(Machine machine : machines) {
			newClients.add(new FileSystemClient(machine, metadataServer, wakeOnLan));
		}
	
		return newClients;
	}
	
	private Set<DataServer> createDataServers(Set<Machine> machines) {
		Set<DataServer> dataServers = new HashSet<DataServer>();
	
		for (Machine machine : machines) {
			dataServers.add(new DataServer(machine));
		}
	
		return dataServers;
	}

}
