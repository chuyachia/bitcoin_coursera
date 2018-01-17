import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

public class MaliciousNode implements Node {
	double p_graph;
	double p_malicious;
	double p_txDistribution;
	int numRounds;
	int count=0;//
	boolean[] followees;
	Set<Transaction> pendingTransactions;

	
    public MaliciousNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
		this.p_graph =p_graph;
		this.p_malicious=p_malicious;
		this.p_txDistribution=p_txDistribution;
		this.numRounds=numRounds;
    }

    public void setFollowees(boolean[] followees) {
        this.followees = followees;
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        this.pendingTransactions = pendingTransactions;

    }

    public Set<Transaction> sendToFollowers() {
		Set<Transaction> fakeTx = new HashSet<Transaction>();
		for(Transaction tx: pendingTransactions) {
			//System.out.println(tx.hashCode());
			fakeTx.add(tx);
			pendingTransactions.remove(tx);
			break;
		}	
		return fakeTx;	
    }

    public void receiveFromFollowees(Set<Candidate> candidates) {
        return;
    }

}
