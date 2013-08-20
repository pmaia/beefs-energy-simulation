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
import simulation.beefs.replication.Replicator;

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
	
	private Properties config;
	@Override
	public Context initialize(Properties config) {
		this.config = config;
		
		Context context = new Context(eventSourceMultiplexer());
		context.add(BeefsEnergySimulationConstants.MACHINES, machines());
		context.add(BeefsEnergySimulationConstants.DATA_SERVERS, dataServers());
		context.add(BeefsEnergySimulationConstants.METADATA_SERVER, metadataServer());
		context.add(BeefsEnergySimulationConstants.CLIENTS, clients());
		context.add(BeefsEnergySimulationConstants.ENERGY_CONSUMPTION_MODEL, energyConsumptionModel());

		return context;
	}
	
	private EventSourceMultiplexer _eventSourceMultiplexer = null;
	private EventSourceMultiplexer eventSourceMultiplexer() {
		if(_eventSourceMultiplexer == null) {
			Time emulationStartTime = 
					new Time(Long.parseLong(config.getProperty(BeefsEnergySimulationConstants.EMULATION_START_TIME)), Unit.SECONDS);

			EventSource []  parsers = new EventSource[machines().size() + clients().size()];

			try {
				int parserCount = 0;
				InputStream traceStream;
				for(Machine machine : machines()) {
					traceStream = 
							new FileInputStream(new File(tracesDir(), "idleness-" + machine.getName()));
					parsers[parserCount++] = new UserActivityTraceEventSource(machine, traceStream, emulationStartTime);
				}
				for(FileSystemClient client : clients()) {
					traceStream = 
							new FileInputStream(new File(tracesDir(), "fs-" + client.getHost().getName()));
					parsers[parserCount++] = new FileSystemTraceEventSource(client, traceStream);
				}

			} catch (FileNotFoundException e) {
				throw new IllegalStateException(e);
			}

			_eventSourceMultiplexer = new EventSourceMultiplexer(parsers); 
		}
		return _eventSourceMultiplexer;
	}

	private EnergyConsumptionModel _energyConsumptionModel = null;
	private Object energyConsumptionModel() {
		if(_energyConsumptionModel == null) {
			String energyConsumptionModelClassName = config.getProperty(BeefsEnergySimulationConstants.ENERGY_CONSUMPTION_MODEL); 
			_energyConsumptionModel = (EnergyConsumptionModel) instantiate(energyConsumptionModelClassName);
		}
		return _energyConsumptionModel;
	}

	private MetadataServer _metadataServer = null; 
	private MetadataServer metadataServer() {
		if(_metadataServer == null) {
			String placementPolicyName = config.getProperty(BeefsEnergySimulationConstants.PLACEMENT_POLICE);
			DataPlacement placementPolicy = DataPlacement.newDataPlacement(placementPolicyName, dataServers());
			Integer replicationLevel = Integer.valueOf(config.getProperty(BeefsEnergySimulationConstants.REPLICATION_LEVEL));
			Time timeToCoherence = 
					new Time(Long.valueOf(config.getProperty(BeefsEnergySimulationConstants.TIME_TO_COHERENCE)), Unit.SECONDS);
			Time timeToDelete = 
					new Time(Long.valueOf(config.getProperty(BeefsEnergySimulationConstants.TIME_TO_DELETE_REPLICAS)), Unit.SECONDS);

			String replicatorName = config.getProperty(BeefsEnergySimulationConstants.REPLICATOR); 
			Replicator replicator = Replicator.newReplicator(replicatorName, dataServers());

			_metadataServer = 
					new MetadataServer(dataServers(), placementPolicy, replicator, replicationLevel, timeToCoherence, timeToDelete);
		}
		return _metadataServer;
	}

	private Object instantiate(String className) {
		try {
			return Class.forName(className).newInstance();
		} catch (Throwable t) {
			throw new RuntimeException("Could not instantiate " + className, t);
		}
	}

	private File _tracesDir = null;
	private File tracesDir() {
		if(_tracesDir == null) {
			String tracesDirPath = config.getProperty(BeefsEnergySimulationConstants.TRACES_DIR);
			_tracesDir = new File(tracesDirPath);
			if(!_tracesDir.exists() || !_tracesDir.isDirectory())
				throw new IllegalArgumentException(tracesDirPath + " doesn't exist or is not a directory");
		}
		return _tracesDir;
	}
	
	private Set<Machine> _machines = null;
	private Set<Machine> machines() {
		if(_machines == null) {
			Time toSleepTimeout = 
					new Time(Long.valueOf(config.getProperty(BeefsEnergySimulationConstants.TO_SLEEP_TIMEOUT)), Unit.SECONDS);
			Time transitionDuration = 
					new Time(Long.valueOf(config.getProperty(BeefsEnergySimulationConstants.TRANSITION_DURATION)), Unit.MILLISECONDS);
			_machines = new HashSet<Machine>();
			List<String> fsTracesFiles = Arrays.asList(tracesDir().list(fsTracesFilter));
			List<String> idlenessTracesFiles = Arrays.asList(tracesDir().list(idlenessTracesFilter));

			for(String fsTraceFile : fsTracesFiles) {
				String machineName = fsTraceFile.split("-")[1];
				if(idlenessTracesFiles.contains("idleness-" + machineName)) {
					_machines.add(new Machine(machineName, toSleepTimeout, transitionDuration));
				}
			}
		}
		return _machines;
	}
	
	private Set<FileSystemClient> _clients = null;
	private Set<FileSystemClient> clients() {
		if(_clients == null) {
			Boolean wakeOnLan = Boolean.valueOf(config.getProperty(BeefsEnergySimulationConstants.WAKE_ON_LAN));
			_clients = new HashSet<FileSystemClient>();
			for(Machine machine : machines()) {
				_clients.add(new FileSystemClient(machine, metadataServer(), wakeOnLan));
			}
		}
		return _clients;
	}
	
	private Set<DataServer> _dataServers;
	private Set<DataServer> dataServers(){
		if(_dataServers == null) {
			_dataServers = new HashSet<DataServer>();

			for (Machine machine : machines()) {
				_dataServers.add(new DataServer(machine));
			}
		}
		return _dataServers;
	}

}
