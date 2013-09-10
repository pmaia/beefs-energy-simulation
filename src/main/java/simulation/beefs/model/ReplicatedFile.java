package simulation.beefs.model;

import java.util.HashSet;
import java.util.Set;

import manelsim.EventScheduler;

public class ReplicatedFile {
	
	private final String fullpath;
	private final int expectedReplicationLevel;
	
	private long size = 0;
	private long bytesWritten = 0;
	private DataServer primary;
	private Set<FileReplica> replicas;
	
	public ReplicatedFile(String fullpath, DataServer primary, int expectedReplicationLevel, Set<FileReplica> replicas) {
		this.fullpath = fullpath;
		this.primary = primary;
		this.replicas = (replicas != null) ? replicas : new HashSet<FileReplica>();
		this.expectedReplicationLevel = expectedReplicationLevel;
	}
	
	public void delete() {
		primary.cleanSpace(bytesWritten);
		for(FileReplica replica : replicas) {
			replica.delete();
		}
		System.out.println(String.format("!%s deleted - %s", fullpath, EventScheduler.now()));
	}
	
	public void updateReplicas(Set<FileReplica> replicas) {
		this.replicas = replicas;
		logChange();
	}
	
	public void write(long bytes, long currentFileSize) {
		long actualBytesWritten = Math.min(bytes, primary.freeSpace());
		long bytesNotWritten = bytes - actualBytesWritten;
		
		if(bytesNotWritten != 0) {
			String msg = String.format("%%primary write failed in %s (%d bytes not written) - %s", primary.host().name(), 
					bytesNotWritten, EventScheduler.now());
			System.out.println(msg);
		}
		
		if(actualBytesWritten > 0) {
			primary.useDisk(actualBytesWritten); //i am considering that every write is a append. this is the worst case scenario.

			bytesWritten += actualBytesWritten;
			if(replicasAreConsistent()) {
				invalidateReplicas();
				logChange();
			}
		}
		
		size = currentFileSize + actualBytesWritten;
	}

	private void logChange() {
		System.out.println(String.format("!%s %d - %s", fullpath, replicasUpToDate(), EventScheduler.now()));
	}

	private int replicasUpToDate() {
		int count = 0;
		for(FileReplica replica : replicas) {
			if(replica.isConsistent()) {
				count++;
			}
		}
		return count;
	}

	public DataServer primary() {
		return primary;
	}
	
	public Set<FileReplica> replicas() {
		return new HashSet<FileReplica>(replicas);
	}
	
	public String fullPath() {
		return fullpath;
	}
	
	public long bytesWritten() {
		return bytesWritten;
	}
	
	public long size() {
		return size;
	}
	
	public boolean replicasAreConsistent() {
		for(FileReplica replica : replicas) {
			if(!replica.isConsistent()) {
				return false;
			}
		}
		return true;
	}
	
	public int actualReplicationLevel() {
		return replicas.size();
	}

	public int expectedReplicationLevel() {
		return expectedReplicationLevel;
	}
	
	private void invalidateReplicas() {
		for(FileReplica replica : replicas) {
			replica.invalidate();
		}
 	}

	public void promoteReplica(FileReplica replica) {
		primary.cleanSpace(bytesWritten);
		
		primary = replica.dataServer();
		bytesWritten = replica.size();
		
		replicas.remove(replica);
		logChange();
	}

}
