import java.util.HashSet;
import javafx.util.Pair;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ArrayList;
import java.security.PublicKey;

public class TxHandler {


	private UTXOPool currentUTXOPool;
	private HashMap<UTXO,ArrayList<Pair<Transaction, Integer>>> UTXONeeded = new HashMap<UTXO,ArrayList<Pair<Transaction, Integer>>>();
	private HashMap<Transaction, ArrayList> transactionLeft = new HashMap<Transaction, ArrayList>();
	private HashMap<Transaction, Double> inputAccumlated = new HashMap<Transaction, Double>();
	
	 /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
	 
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
		currentUTXOPool = new UTXOPool(utxoPool);
    }
	
	/**
	 * return the sum of all outputs in a given transaction
	 */
	 
	private double computeOutputSum(Transaction tx){
		ArrayList<Transaction.Output> txOutputs = tx.getOutputs();
		int outputLen = tx.numOutputs();
		double outputSum = 0;
		for (int i = 0;i <outputLen;i++) {
			double txOutputVal = txOutputs.get(i).value;
			if (txOutputVal < 0){
				return -1.0;
			} else {
				outputSum+= txOutputVal;
			}
		}
		return outputSum;
	}
	
	/**
	 * remove from the UTXO pool all the UTXOs used as inputs in a given transaction
	 */ 
	
	private void removeFromPool(Transaction tx){
		ArrayList<Transaction.Input> txInputs = tx.getInputs();
		int txInputLen = tx.numInputs();
		for (int i = 0;i < txInputLen;i++) {
			UTXO utxo = new UTXO(txInputs.get(i).prevTxHash,txInputs.get(i).outputIndex);
			currentUTXOPool.removeUTXO(utxo);
		}
	}
	
	/**
	 * add to the UTXO pool as UTXOs all the outputs produced by a given transaction
	 */ 
	 
	private void addToPool(Transaction tx){
		ArrayList<Transaction.Output> txOutputs = tx.getOutputs();
		int txOutputLen = tx.numOutputs();
		for (int i = 0;i < txOutputLen;i++) {
			UTXO utxo = new UTXO(tx.getHash(), i);//byte[] txHash, int index
			currentUTXOPool.addUTXO(utxo,txOutputs.get(i));
		}
	}
	
    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
	 
    public boolean isValidTx(Transaction tx) {
		HashSet<UTXO> claimedUtxo = new HashSet<UTXO>();
		ArrayList<Integer> notFoundInputIndex = new ArrayList<Integer>();
		ArrayList<UTXO> notFoundUTXO = new ArrayList<UTXO>();
		
		ArrayList<Transaction.Input> txInputs = tx.getInputs();
		int inputLen = tx.numInputs();
		
		double inputSum = 0;
		
		for (int i = 0;i<inputLen;i++){
			Transaction.Input txInput = txInputs.get(i);
			UTXO newClaim = new UTXO(txInput.prevTxHash, txInput.outputIndex);
			if (!currentUTXOPool.contains(newClaim)){
				//System.out.println("Fund source not contained in the pool");
				notFoundUTXO.add(newClaim);
				notFoundInputIndex.add(new Integer(i));
			} else {
				Transaction.Output claimedOutput =  currentUTXOPool.getTxOutput(newClaim);
				if (!Crypto.verifySignature(claimedOutput.address,tx.getRawDataToSign(i),txInput.signature)) {
					//System.out.println("Invalid signature");
					return false;
				} else {
					inputSum+=claimedOutput.value;
				}
			}
			
			if (claimedUtxo.contains(newClaim)) {
				//System.out.println("UTXO already claimed previously");
				return false;
			} else {
				claimedUtxo.add(newClaim);
			}
		}
		
		if (notFoundInputIndex.size()>0) {			
			transactionLeft.put(tx, notFoundInputIndex);
			inputAccumlated.put(tx, new Double(inputSum));
			for (int j = 0;j < notFoundUTXO.size(); j++) {
				Pair<Transaction, Integer> txIndexPair = new Pair<Transaction, Integer> (tx,notFoundInputIndex.get(j));
				UTXO newClaim = notFoundUTXO.get(j);
				if (UTXONeeded.containsKey(newClaim)){
					ArrayList<Pair<Transaction, Integer>> currentPairList = UTXONeeded.get(newClaim);
					currentPairList.add(txIndexPair);
					UTXONeeded.put(newClaim,currentPairList);
				} else {
					ArrayList<Pair<Transaction,Integer>> pairList = new ArrayList<Pair<Transaction, Integer>>();
					pairList.add(txIndexPair);
					UTXONeeded.put(newClaim,pairList);
				}
			}
			return false;
		} else {
			double outputSum = computeOutputSum(tx);
			if (outputSum==-1.0){
				//System.out.println("Negative output value");
				return false;
			}
			if (inputSum<outputSum){
				//System.out.println("Fund source sum smaller than transaction output");
				return false;
			} else {
				return true;
			}
		}
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
		ArrayList<Transaction> validTxs = new ArrayList<Transaction>();
		for (int i = 0;i < possibleTxs.length;i++){
			Transaction candidate = possibleTxs[i];
			if (isValidTx(candidate)) {
				validTxs.add(candidate);
				removeFromPool(candidate);
				addToPool(candidate);
			}
		}
		ArrayList<UTXO> UTXOLeft;
		ArrayList<UTXO> newUTXOLeft;
		do {
			UTXOLeft = currentUTXOPool.getAllUTXO();
			for (int j = 0; j < UTXOLeft.size(); j++){
				UTXO utxo = UTXOLeft.get(j);
				if (UTXONeeded.containsKey(utxo)){
					ArrayList<Pair<Transaction, Integer>> claimedBy = UTXONeeded.get(utxo);
					Transaction.Output output = currentUTXOPool.getTxOutput(utxo);
					for (int k=0;k<claimedBy.size();k++){
						Pair<Transaction, Integer> pair = claimedBy.get(k);
						Transaction tx = pair.getKey();
						Integer index = pair.getValue();
						if (Crypto.verifySignature(output.address,tx.getRawDataToSign(index),tx.getInput(index).signature)) {
							if (transactionLeft.containsKey(tx)&&inputAccumlated.containsKey(tx)) {
								ArrayList indexList = transactionLeft.get(tx);
								indexList.remove(index);
								transactionLeft.put(tx, indexList);
								inputAccumlated.put(tx, inputAccumlated.get(tx) +output.value);
								if (transactionLeft.get(tx).size()==0){
									double outputSum = computeOutputSum(tx);
									double inputSum = inputAccumlated.get(tx).doubleValue();
									if (outputSum!= -1.0 && inputSum>= outputSum){
										validTxs.add(tx);
										removeFromPool(tx);
										addToPool(tx);
										break;
									}						
								}
							}
						} else {
							transactionLeft.remove(tx);
							inputAccumlated.remove(tx);
						}
					}
				}
			}
			newUTXOLeft = currentUTXOPool.getAllUTXO();
		} while (!(newUTXOLeft.containsAll(UTXOLeft) && UTXOLeft.containsAll(newUTXOLeft)));
		
		Transaction[] validTxsReturn = validTxs.toArray(new Transaction[validTxs.size()]);
		return validTxsReturn;
    }

}
