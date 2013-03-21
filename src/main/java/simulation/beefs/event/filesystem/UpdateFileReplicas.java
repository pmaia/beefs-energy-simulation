package simulation.beefs.event.filesystem;

import manelsim.Event;
import manelsim.Time;
import simulation.beefs.model.MetadataServer;
import simulation.beefs.model.ReplicatedFile;

public class UpdateFileReplicas extends Event {
	
	private final ReplicatedFile file;
	private final MetadataServer metadataServer;

	public UpdateFileReplicas(Time scheduledTime, ReplicatedFile file, MetadataServer metadataServer) {
		super(scheduledTime);
		
		this.file = file;
		this.metadataServer = metadataServer;
	}

	@Override
	public void process() {
		metadataServer.updateReplicas(file);
	}
	
	@Override
	public String toString() {
		return String.format("UpdateFileReplica\t%s\t%s", getScheduledTime(), file.getFullPath());
	}
	
}
