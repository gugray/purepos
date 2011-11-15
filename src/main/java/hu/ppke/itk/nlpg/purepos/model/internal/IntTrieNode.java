package hu.ppke.itk.nlpg.purepos.model.internal;

/**
 * TrieNode class which holds Integer values.
 * 
 * @author György Orosz
 * 
 * @param <W>
 *            word type
 */
public class IntTrieNode<W> extends TrieNode<Integer, Integer, W> {
	IntTrieNode(Integer id) {
		super(id);
	}

	public IntTrieNode(Integer id, W word) {
		super(id, word);
	}

	@Override
	protected Integer zero() {
		return 0;
	}

	@Override
	protected Integer increment(Integer n) {
		return n + 1;
	}

	@Override
	protected TrieNode<Integer, Integer, W> createNode(Integer id) {
		return new IntTrieNode<W>(id);
	}

	public Double getAprioriProb(W word) {
		if (hasWord(word)) {
			return (double) getWord(word) / (double) getNum();
		} else {
			return 0.0;
		}
	}

}