package raft;

import gash.router.server.PrintUtil;
import gash.router.server.edges.EdgeInfo;
import pipe.common.Common.Header;
import pipe.election.Election.RequestVote;
import pipe.work.Work.WorkMessage;
import routing.Pipe.CommandMessage;
import redis.clients.jedis.Jedis; 

public class CandidateState implements RaftState{
	private RaftManager Manager;
	private double voteCount=0;
	private int candidateId;
	private int votedFor=-1;
	private double clusterSize=0;
	
	public void process(){
		System.out.println("reached candidate State");
		try {			
			//if (Manager.getElectionTimeout() <= 0 && (System.currentTimeMillis() - Manager.getLastKnownBeat() > Manager.getHbBase())) {
				System.out.println("Node : " + Manager.getNodeId() + " timed out");
				//be followers method impl should come here
				requestVote();
				Manager.randomizeElectionTimeout();
				Thread.sleep(200);
				long dt = Manager.getElectionTimeout() - (System.currentTimeMillis() - Manager.getTimerStart());
				Manager.setElectionTimeout(dt);		
				return;
			//}else{	         
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}	
	//CREATE REQUEST VOTE MESSAGE
 	public WorkMessage buildRequestVote() {
	
 		Header.Builder hb = Header.newBuilder();
		hb.setNodeId(Manager.getNodeId());
		hb.setDestination(-1);	
		
		RequestVote.Builder rvb=RequestVote.newBuilder();
		rvb.setCurrentTerm(Manager.getTerm());
		rvb.setCandidateID(Manager.getNodeId());	
		
		
		WorkMessage.Builder wb = WorkMessage.newBuilder();
		wb.setHeader(hb);
		wb.setSecret(10);
		wb.setReqvote(rvb);
			
		
		return wb.build();
	}
	public synchronized void requestVote(){
		
		System.out.println("reached requestVote method of candidate");
		Manager.setTerm(Manager.getTerm()+1);
		clusterSize=0;
		for(EdgeInfo ei:Manager.getEdgeMonitor().getOutBoundEdges().map.values())
		{
			if(ei.isActive()&&ei.getChannel()!=null)
			{
				clusterSize++;
			}
		}
		if(clusterSize==0){
			Manager.randomizeElectionTimeout();
			System.out.println("Leader Elected and the Node Id is "+ Manager.getNodeId()+"total active nodes is"+clusterSize);
			Manager.setLeaderId(Manager.getNodeId());
			Manager.setLeaderPort(Manager.getCommandPort());
			Manager.setLeaderHost(Manager.getSelfHost());
			Manager.setCurrentState(Manager.Leader);
			System.out.println(Manager.getLeaderId());
			Jedis jedis = new Jedis("localhost"); 
		    System.out.println("Connection to server sucessfully"); 
		    //check whether server is running or not 		    
		    System.out.println("Server is running: "+jedis.ping());
		    String ip=Manager.getLeaderHost();		    
		    int port=Manager.getLeaderPort();
		    System.out.println("ip "+ip+" port "+port);
		    jedis.set("5",ip+":"+port);
		    
		}
		else
		clusterSize++;
		System.out.println("active count is"+clusterSize);
		voteCount=0;
		voteCount++;
		System.out.println("voted for self");
		for(EdgeInfo ei:Manager.getEdgeMonitor().getOutBoundEdges().map.values())
		{			
			if(ei.isActive()&&ei.getChannel()!=null)
			{			
				System.out.println("voteRequest sent to"+ei.getRef());							    
				Manager.getEdgeMonitor().sendMessage(buildRequestVote());		
			}
		}
		return;		
	}
	
	
	//if somebody requests vote of candidate
	@Override
	public synchronized void onRequestVoteReceived(WorkMessage msg) {
		// TODO Auto-generated method stub
		/*System.out.println("Candidates Vote requested by "+msg.getHeader().getNodeId());
		if (msg.getReqvote().getCurrentTerm() > Manager.getTerm()) {
			votedFor = -1;
			Manager.randomizeElectionTimeout();			
			Manager.setCurrentState(Manager.Follower);
			Manager.getCurrentState().onRequestVoteReceived(msg);
			
		} */
	}
	
	//received vote
	@Override
	public synchronized void receivedVoteReply(WorkMessage msg)
	{		
		System.out.println("received vote from: "+msg.getVote().getVoterID()+" to me");
		voteCount++;
		
		System.out.println("required votes to win :"+clusterSize/2);
		if(voteCount>=(clusterSize/2))
		{
			Manager.randomizeElectionTimeout();
			System.out.println("Leader Elected and the Node Id is "+ Manager.getNodeId()+"total active nodes is"+clusterSize);
			Manager.setLeaderId(Manager.getNodeId());
			votedFor=-1;
			clusterSize=0;
			Jedis jedis = new Jedis("localhost"); 
		    System.out.println("Connection to server sucessfully"); 
		    //check whether server is running or not 		    
		    System.out.println("Server is running: "+jedis.ping());
		    String ip=Manager.getLeaderHost();
		    int port=Manager.getLeaderPort();
		    jedis.set("5",ip+":"+port);
		    String out=jedis.get("5");
		    System.out.println("out is "+out);
			Manager.setCurrentState(Manager.Leader);				
		}
	}
	
		@Override
		public synchronized void setManager(RaftManager Mgr){
			this.Manager = Mgr;
		}

		@Override
		public synchronized RaftManager getManager() {
			return Manager;
		}
		
		@Override
		public synchronized void receivedHeartBeat(WorkMessage msg)
		{
			Manager.randomizeElectionTimeout();		
			System.out.println("received hearbeat from the Leader: "+msg.getLeader().getLeaderId());
			PrintUtil.printWork(msg);		
			Manager.setCurrentState(Manager.Follower);
			Manager.setLastKnownBeat(System.currentTimeMillis());
		}
		public void receivedLogToWrite(CommandMessage msg)
		{
			return;
		}
	 
		public void chunkReceived(WorkMessage msg)
		  {
			  return;
		  }
		
		public void responseToChuckSent(WorkMessage msg)
		  {
			return;  
		  }
		@Override
		public void receivedCommitChunkMessage(WorkMessage msg) {
			// TODO Auto-generated method stub
			
		}
		@Override
		public void readChunksFromFollowers(String fileName, int numOfchunks) {
			// TODO Auto-generated method stub
			
		}
		@Override
		public void fetchChunk(WorkMessage msg) {
			// TODO Auto-generated method stub
			
		}
		@Override
		public void sendChunkToClient(WorkMessage msg) {
			// TODO Auto-generated method stub
			
		}
		@Override
		public void replicateDatatoNewNode(int newNodeId) {
			// TODO Auto-generated method stub
			
		}
		@Override
		public void logReplicationMessage(WorkMessage msg) {
			// TODO Auto-generated method stub
			
		}
		
	
	
}

