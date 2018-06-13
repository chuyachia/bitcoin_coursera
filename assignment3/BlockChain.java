// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

import java.util.ArrayList;
import java.util.LinkedList;

public class BlockChain {
    public static final int CUT_OFF_AGE = 10;

	private Block latestBlock = null;
	private UTXOPool latestUTXOPool= null;
	private TransactionPool txPool = null;
	private int chainHeight = 0;
	
	private UTXOPool createUTXOPool(Block block){
		UTXOPool utxopool = new UTXOPool();
		ArrayList<Transaction> txInBlock = null;
		if (block.getTransactions().size()>0){
			txInBlock = block.getTransactions();
		} else {
			txInBlock = new ArrayList<Transaction>();
		}	
		txInBlock.add(block.getCoinbase());			
		for (Transaction tx : txInBlock){
			for (int i = 0;i<tx.numOutputs();i++)
			{
				UTXO utxo = new UTXO(tx.getHash(),i);
				utxopool.addUTXO(utxo, tx.getOutput(i));
			}			
		}
		return utxopool;
	}
	
	
	private class Node {
		private byte[] hash;
		private UTXOPool utxo;
		public ArrayList<Node> next = new ArrayList<Node>();
		public int height;
		public Node(byte[] hash,UTXOPool utxo,int height ){
			this.hash = hash;
			this.utxo = utxo;
			this.height = height;
		}
		public byte[] getHash(){
			return hash;
		}
		public UTXOPool getUTXOPool(){
			return new UTXOPool(utxo);
		}
	}
	
	
	private Node root = null;
	/**
     * create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
        // IMPLEMENT THIS
		latestBlock = genesisBlock;
		latestUTXOPool = createUTXOPool(genesisBlock);
		root = new Node(genesisBlock.getHash(),latestUTXOPool,0);
		txPool = new TransactionPool();
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        // IMPLEMENT THIS
		return latestBlock;
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        // IMPLEMENT THIS
		return new UTXOPool(latestUTXOPool);
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        // IMPLEMENT THIS
		return new TransactionPool(txPool);
    }

    /**
     * Add {@code block} to the block chain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}.
     * 
     * <p>
     * For example, you can try creating a new block over the genesis block (block height 2) if the
     * block chain height is {@code <=
     * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot create a new block
     * at height 2.
     * 
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        // IMPLEMENT THIS
		byte[] prevHash = block.getPrevBlockHash();
		byte[] thisHash = block.getHash();
		
		LinkedList<Node> queue = new LinkedList<Node>();
		queue.add(root);

		while(queue.size()>0){			
			 Node nodeInTree = queue.remove();			 
			 if (nodeInTree.hash == prevHash){
				int blockHeight = nodeInTree.height+1;
				if (chainHeight- blockHeight>=CUT_OFF_AGE)
					return false;
				UTXOPool prevUTXOPool = nodeInTree.getUTXOPool();
				TxHandler txHandler = new TxHandler(prevUTXOPool);
				ArrayList<Transaction> allTx= block.getTransactions();
				Transaction[] allTxArr = new Transaction[allTx.size()];
				allTxArr = allTx.toArray(allTxArr);				
				Transaction[] allValidTx = txHandler.handleTxs(allTxArr);
				if (allValidTx.length < allTxArr.length)
					return false;				
				UTXOPool thisUTXOPool = txHandler.getUTXOPool();
				UTXO utxo = new UTXO(block.getCoinbase().getHash(),0);
				thisUTXOPool.addUTXO(utxo, block.getCoinbase().getOutput(0));
				Node targetNode = new Node(thisHash,thisUTXOPool,blockHeight);
				nodeInTree.next.add(targetNode);
				System.out.println(prevHash==latestBlock.getHash());
				if (prevHash==latestBlock.getHash()){
					latestBlock = block;
					latestUTXOPool = thisUTXOPool;
					chainHeight++;
				}
				return true;
			 }
			 for (Node n : nodeInTree.next){
				 queue.add(n);
			 }
			 
		}
				
		return false;
    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        // IMPLEMENT THIS
		txPool.addTransaction(tx);
    }
}