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

	private long readsWhileClientSleeping = 0;

	private long writesWhileClientSleeping = 0;

	/**
	 * 
	 * @param host the {@link Machine} in which this client is running
	 * @param metadataServer the {@link MetadaServer} common to all clients on this system
	 */
	public FileSystemClient(Machine host, MetadataServer metadataServer) {
		this.metadataServer = metadataServer;
		this.host = host;
	}

	public ReplicatedFile createOrOpen(String fullpath, long size) {
		return metadataServer.createOrOpen(this, fullpath, size);
	}

	public void read(String filePath, long bytesTransfered, Time begin, Time duration) {
		if(!host.isReachable()) {
			readsWhileClientSleeping++;
		} else {
			ReplicatedFile file = createOrOpen(filePath, 0); //passing 0 as the file size to make sure the ReplicatedFile will be created 

			DataServer primary = file.primary();
			if(!primary.host().isReachable()){

				if(file.replicasAreConsistent()) {
					for(FileReplica replica : file.replicas()) {
						if(replica.dataServer().host().isReachable()) {
							return;
						}
					}
				}

				primary.host().wakeOnLan(begin);

				Time delta = primary.host().transitionDuration().plus(ONE_SECOND);
				EventScheduler.schedule(
						new Read(this, begin.plus(delta), duration, filePath, bytesTransfered, false));
			}
		}
	}

	public void write(String filePath, long fileSize, long bytesTransfered, Time begin, Time duration) {
		if(!host.isReachable()) {
			writesWhileClientSleeping++;
		} else {
			ReplicatedFile replicatedFile = createOrOpen(filePath, bytesTransfered);

			if(replicatedFile == null) {
				String msg = String.format("*could not create file - %s", EventScheduler.now());
				System.out.println(msg);
				return;
			}

			DataServer primary = replicatedFile.primary();
			if(primary.host().isReachable()) {
				replicatedFile.write(bytesTransfered, fileSize);
			} else {
				if(replicatedFile.replicasAreConsistent()){
					for(FileReplica replica : replicatedFile.replicas()) {
						if(replica.dataServer().host().isReachable()) {
							replicatedFile.promoteReplica(replica);
							replicatedFile.write(bytesTransfered, fileSize);
							return;
						}
					} 
				} 
				primary.host().wakeOnLan(begin);

				System.out.println("delayed write");
				Time delta = primary.host().transitionDuration().plus(ONE_SECOND);
				EventScheduler.schedule(
						new Write(this, begin.plus(delta), duration, filePath, bytesTransfered, fileSize, false));
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

	public long writesWhileClientSleeping() {
		return writesWhileClientSleeping;
	}

}
