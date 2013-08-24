package simulation.beefs.model;

import manelsim.EventScheduler;
import manelsim.Time;
import manelsim.Time.Unit;
import simulation.beefs.event.filesystem.Read;
import simulation.beefs.event.filesystem.Write;

public class FileSystemClient {
	
	private final Time ONE_SECOND = new Time(1, Unit.SECONDS);

	private final MetadataServer metadataServer;
	
	private final Machine host;
	
	private final boolean wakeOnLan;
	
	private long readsWhileClientSleeping = 0;
	
	private long readsWhileDataServerSleeping = 0;
	
	private long writesWhileClientSleeping = 0;
	
	private long writesWhileDataServerSleeping = 0;
	
	/**
	 * 
	 * @param host the {@link Machine} in which this client is running
	 * @param metadataServer the {@link MetadaServer} common to all clients on this system
	 * @param wakeOnLan indicates if this client must or must not wakes up target servers when they are sleeping 
	 */
	public FileSystemClient(Machine host, MetadataServer metadataServer, boolean wakeOnLan) {
		this.metadataServer = metadataServer;
		this.host = host;
		this.wakeOnLan = wakeOnLan;
	}
	
	public ReplicatedFile createOrOpen(String fullpath) {
		return metadataServer.createOrOpen(this, fullpath);
	}
	
	public void read(String filePath, long bytesTransfered, Time begin, Time duration) {
		if(!host.isReachable()) {
			readsWhileClientSleeping++;
		} else {
			ReplicatedFile file = createOrOpen(filePath);
		
			DataServer primary = file.primary();
			if(!primary.host().isReachable() && wakeOnLan){
				primary.host().wakeOnLan(begin);
				
				Time delta = primary.host().transitionDuration().plus(ONE_SECOND);
				EventScheduler.schedule(
						new Read(this, begin.plus(delta), duration, filePath, bytesTransfered, false));
			} else {
				readsWhileDataServerSleeping++;
			}
		}
	}
	
	public void write(String filePath, long fileSize, long bytesTransfered, Time begin, Time duration) {
		if(!host.isReachable()) {
			writesWhileClientSleeping++;
		} else {
			ReplicatedFile replicatedFile = createOrOpen(filePath);
			
			DataServer primary = replicatedFile.primary();
			if(primary.host().isReachable()) {
				replicatedFile.write(bytesTransfered, fileSize);
			} else if(wakeOnLan) {
				primary.host().wakeOnLan(begin);
				
				Time delta = primary.host().transitionDuration().plus(ONE_SECOND);
				EventScheduler.schedule(
						new Write(this, begin.plus(delta), duration, filePath, bytesTransfered, fileSize, false));
			} else {
				writesWhileDataServerSleeping++;
			}
		}
	}
	
	public MetadataServer metadataServer() {
		return metadataServer;
	}

	public Machine host() {
		return host;
	}

	public void close(String filePath) {
		metadataServer.close(filePath);		
	}

	public void delete(String filePath) {
		metadataServer.delete(filePath);		
	}

	public long readsWhileClientSleeping() {
		return readsWhileClientSleeping;
	}

	public long readsWhileDataServerSleeping() {
		return readsWhileDataServerSleeping;
	}

	public long writesWhileDataServerSleeping() {
		return writesWhileDataServerSleeping;
	}

	public long writesWhileClientSleeping() {
		return writesWhileClientSleeping;
	}

}
