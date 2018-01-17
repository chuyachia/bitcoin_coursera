//TODO Record tx count with regards to sender and calculate threshold according to number of followees
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;

/* CompliantNode refers to a node that follows the rules (not malicious)*/
public class CompliantNode implements Node {
	double p_graph;
	double p_malicious;
	double p_txDistribution;
	int numRounds;
	boolean[] followees;
	int count = 0;
	Set<Transaction> pendingTransactions;
	Set<Transaction> consensusTransactions = new HashSet<Transaction>();
	Set<Integer> roundCountedTx = new HashSet<Integer>();
	HashMap<Integer,Integer> txCount = new HashMap<Integer,Integer>();
	int threshold = 2;
	int numFollowee = 0; 
	
    public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
        // IMPLEMENT THIS
		this.p_graph =p_graph;
		this.p_malicious=p_malicious;
		this.p_txDistribution=p_txDistribution;
		this.numRounds=numRounds;
    }

    public void setFollowees(boolean[] followees) {
        // IMPLEMENT THIS
		this.followees = followees;
		for (boolean f : this.followees){
			if(f){
				numFollowee++;
			}
		}
		//threshold = 1.5*numFollowee;
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        // IMPLEMENT THIS
		this.pendingTransactions = pendingTransactions;			
    }

    public Set<Transaction> sendToFollowers() {
        // IMPLEMENT THIS
		if (count < numRounds){
			count++;
			return pendingTransactions;
		} else {
			count++;
			return consensusTransactions;
		}
    }

    public void receiveFromFollowees(Set<Candidate> candidates) {
        // IMPLEMENT THIS
		if (count == numRounds){
			for (Candidate c :candidates){
				Integer txID = new Integer(c.tx.hashCode());
				if (txCount.containsKey(txID) && txCount.get(txID)>threshold){
					consensusTransactions.add(c.tx);
				}
			}
		}  else {
			for (Candidate c : candidates){
				pendingTransactions.add(c.tx);
				Integer txID = new Integer(c.tx.hashCode());
				if (!roundCountedTx.contains(txID)){
					if (!txCount.containsKey(txID)){
						txCount.put(txID,new Integer(1));
					} else {
						txCount.put(txID,txCount.get(txID)+1);
					}
					roundCountedTx.add(txID);
				}
			}
			roundCountedTx.clear();
		}

    }


}
