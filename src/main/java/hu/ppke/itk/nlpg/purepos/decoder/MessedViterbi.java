package hu.ppke.itk.nlpg.purepos.decoder;

import hu.ppke.itk.nlpg.purepos.model.Model;
import hu.ppke.itp.nlpg.purepos.morphology.IMorphologicalAnalyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

public class MessedViterbi extends FastDecoder {

	public MessedViterbi(Model<String, Integer> model,
			IMorphologicalAnalyzer morphologicalAnalyzer, double logTheta,
			int maxGuessedTags) {
		super(model, morphologicalAnalyzer, logTheta, maxGuessedTags);

	}

	protected Logger logger = Logger.getLogger(getClass());
	Map<Integer, State> trellis = new HashMap<Integer, State>();

	@Override
	public List<Integer> decode(final List<String> observations) {
		trellis.clear();
		List<String> obs = new ArrayList<String>(observations);

		obs.add(Model.getEOSToken()); // adds 1 EOS marker as in HunPos
		int n = model.getTaggingOrder();

		ArrayList<Integer> startTags = new ArrayList<Integer>();

		for (int j = 0; j < n; ++j) { // TODO:n?*
			startTags.add(model.getBOSIndex());
		}

		State s = new State(startTags, 0.0); // TODO: hunpos worlflow is a bit
												// different here*

		trellis.put(model.getBOSIndex(), s);

		forward(obs);

		try { // find maximal element
			State maximalState = Collections.max(trellis.values(),
					new Comparator<State>() {
						@Override
						public int compare(State first, State second) {
							return Double.compare(first.getWeight(),
									second.getWeight());
						}
					});
			List<Integer> ret = maximalState.getPath();
			return ret.subList(model.getTaggingOrder(), ret.size() - 1);
		} catch (java.util.NoSuchElementException e) {
			// TODO: is it really needed?
			logger.trace(observations);
			logger.trace(trellis);
			throw new RuntimeException(e);
		}

	}

	protected void forward(List<String> observations) {
		Map<Integer, Map<Integer, Pair<Double, Double>>> nextWeights = new HashMap<Integer, Map<Integer, Pair<Double, Double>>>();

		boolean isFirst = true;
		for (String obs : observations) {

			logger.trace("current observation: " + obs);

			nextWeights = computeNextWeights(isFirst, obs);
			// logger.trace(tab + "nextweights: " + nextWeights);
			logger.trace("\tcurrent states:");
			for (State s : trellis.values())
				logger.trace("\t\t" + s.getPath() + " - " + s.getWeight());

			trellis = updateTrellis(nextWeights);

			isFirst = false;

			trellis = doBeamPruning(trellis);

		}

	}

	public Map<Integer, Map<Integer, Pair<Double, Double>>> computeNextWeights(
			boolean isFirst, String obs) {
		Map<Integer, Map<Integer, Pair<Double, Double>>> nextWeights = new HashMap<Integer, Map<Integer, Pair<Double, Double>>>();

		for (Integer fromTag : trellis.keySet()) {
			State s = trellis.get(fromTag);
			Map<Integer, Pair<Double, Double>> nextProb = getNextProb(
					s.getPath(), obs, isFirst);
			nextWeights.put(fromTag, nextProb);

		}
		return nextWeights;
	}

	public Map<Integer, State> updateTrellis(
			Map<Integer, Map<Integer, Pair<Double, Double>>> nextWeights) {
		Map<Integer, State> trellisTmp = new HashMap<Integer, State>();
		// Set<Integer> tags = nextWeights
		for (Integer nextTag : tags) {
			Integer fromTag = findMaxFor(nextTag, nextWeights);
			// transition prob
			Pair<Double, Double> plusWeightpair = nextWeights.get(fromTag).get(
					nextTag);

			if (plusWeightpair != null) {
				// emission prob
				Double plusWeight = plusWeightpair.getLeft();
				if (nextNodesNum(nextWeights) > 1) {
					plusWeight += plusWeightpair.getRight();
				}
				// logger.trace(tab
				// + "next state: .."
				// + fromTag
				// + ","
				// + nextTag
				// + " :"
				// + ((plusWeightpair.getLeft())
				// + +plusWeightpair.getRight() + trellis.get(
				// fromTag).getWeight()));
				trellisTmp.put(nextTag,
						trellis.get(fromTag).createNext(nextTag, plusWeight));
			}

		}
		return trellisTmp;
	}

	protected int nextNodesNum(
			Map<Integer, Map<Integer, Pair<Double, Double>>> nextWeights) {
		int num = 0;
		for (Map<Integer, Pair<Double, Double>> t : nextWeights.values()) {
			num += t.keySet().size();
		}
		return num;
	}

	protected Map<Integer, State> doBeamPruning(Map<Integer, State> trellis) {
		Map<Integer, State> ret = new HashMap<Integer, State>();
		Map.Entry<Integer, State> maxElement = Collections.max(
				trellis.entrySet(),
				new Comparator<Map.Entry<Integer, State>>() {

					@Override
					public int compare(Entry<Integer, State> o1,
							Entry<Integer, State> o2) {
						return Double.compare(o1.getValue().getWeight(), o2
								.getValue().getWeight());
					}
				});
		Double maxWeight = maxElement.getValue().getWeight();
		for (Integer key : trellis.keySet()) {
			Double w = trellis.get(key).getWeight();
			if (!(w < maxWeight - logTheta)) {
				ret.put(key, trellis.get(key));
			}
		}

		return ret;
	}

	protected int findMaxFor(final Integer nextTag,
			final Map<Integer, Map<Integer, Pair<Double, Double>>> nextWeights) {

		// find maximum according to tag transition probabilities
		Entry<Integer, Map<Integer, Pair<Double, Double>>> max = Collections
				.max(nextWeights.entrySet(),
						new Comparator<Map.Entry<Integer, Map<Integer, Pair<Double, Double>>>>() {

							@Override
							public int compare(
									Entry<Integer, Map<Integer, Pair<Double, Double>>> o1,
									Entry<Integer, Map<Integer, Pair<Double, Double>>> o2) {
								if (o1.getValue() == null
										|| o1.getValue().get(nextTag) == null)
									return 1;
								if (o2.getValue() == null
										|| o2.getValue().get(nextTag) == null)
									return -1;
								return Double.compare(o1.getValue()
										.get(nextTag).getLeft()
										+ trellis.get(o1.getKey()).getWeight(),
										o2.getValue().get(nextTag).getLeft()
												+ trellis.get(o2.getKey())
														.getWeight());
							}
						});
		return max.getKey();
		//
		// double max = Double.MIN_VALUE;
		// int maxTag = -1;
		// for (Entry<Integer, Map<Integer, Double>> entry : nextWeights
		// .entrySet()) {
		// double val = entry.getValue().get(nextTag);
		// if (val > max) {
		// max = val;
		// maxTag = entry.getKey();
		// }
		// }
		// return maxTag;
	}

}
