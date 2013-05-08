/*******************************************************************************
 * Copyright (c) 2012 György Orosz, Attila Novák.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/
 * 
 * This file is part of PurePos.
 * 
 * PurePos is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * PurePos is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 * 
 * Contributors:
 *     György Orosz - initial API and implementation
 ******************************************************************************/
package hu.ppke.itk.nlpg.purepos;

import hu.ppke.itk.nlpg.docmodel.IToken;
import hu.ppke.itk.nlpg.docmodel.internal.Sentence;
import hu.ppke.itk.nlpg.docmodel.internal.Token;
import hu.ppke.itk.nlpg.purepos.common.Util;
import hu.ppke.itk.nlpg.purepos.common.lemma.ILemmaTransformation;
import hu.ppke.itk.nlpg.purepos.common.lemma.LemmaComparator;
import hu.ppke.itk.nlpg.purepos.common.lemma.LemmaUtil;
import hu.ppke.itk.nlpg.purepos.decoder.StemFilter;
import hu.ppke.itk.nlpg.purepos.model.internal.CompiledModel;
import hu.ppke.itk.nlpg.purepos.morphology.IMorphologicalAnalyzer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

/**
 * Tagger that performs full morphological disambiguation.
 * 
 * @author György Orosz
 * 
 */
public class MorphTagger extends POSTagger implements ITagger {

	LemmaComparator lemmaComparator;
	StemFilter stemFilter;

	public MorphTagger(CompiledModel<String, Integer> model,
			IMorphologicalAnalyzer analyzer, double logTheta, double sufTheta,
			int maxGuessedTags, boolean useBeamSearch) {
		super(model, analyzer, logTheta, sufTheta, maxGuessedTags,
				useBeamSearch);

		lemmaComparator = new LemmaComparator(model.getCompiledData(),
				model.getData());
		stemFilter = Util.createStemFilter();
	}

	@Override
	protected List<IToken> merge(List<String> sentence, List<Integer> tags) {
		List<IToken> res = super.merge(sentence, tags);
		// ISentence taggedSentence = super.tagSentence(sentence);
		List<IToken> tmp = new ArrayList<IToken>();
		int pos = 0;
		for (IToken t : res) {
			IToken bestStemmedToken = findBestLemma(t, pos);
			bestStemmedToken = new Token(bestStemmedToken.getToken(),
					bestStemmedToken.getStem().replace(" ", "_"),
					bestStemmedToken.getTag());
			tmp.add(bestStemmedToken);
			pos++;
		}
		return new Sentence(tmp);
	}

	private IToken findBestLemma(IToken t, int position) {
		Collection<IToken> stems;
		if (Util.analysisQueue.hasAnal(position)) {
			stems = Util.analysisQueue.getAnalysises(position);
		} else {
			stems = analyzer.analyze(t);
		}

		Map<ILemmaTransformation<String, Integer>, Double> tagLogProbabilities = model
				.getLemmaGuesser().getTagLogProbabilities(t.getToken());
		Map<IToken, Pair<ILemmaTransformation<String, Integer>, Double>> lemmaSuffixProbs = LemmaUtil
				.batchConvert(tagLogProbabilities, t.getToken(),
						model.getTagVocabulary());

		if (Util.isEmpty(stems)) {
			// the guesser is used

			stems = lemmaSuffixProbs.keySet();
		}
		// matching tags
		Collection<IToken> possibleStems = new HashSet<IToken>();
		for (IToken ct : stems) {
			if (t.getTag().equals(ct.getTag())) {
				possibleStems.add(new Token(ct.getToken(), ct.getStem(), ct
						.getTag()));
				possibleStems.add(new Token(ct.getToken(), Util.toLower(ct
						.getStem()), ct.getTag()));
			}
		}

		if (Util.isEmpty(possibleStems)) {
			// error handling
			return new Token(t.getToken(), t.getToken(), t.getTag());
		}

		// most frequrent stem
		IToken best;
		if (possibleStems.size() == 1) {
			best = possibleStems.iterator().next();
		} else {

			if (stemFilter != null) {
				possibleStems = stemFilter.filterStem(possibleStems);
			}
			List<Pair<IToken, ILemmaTransformation<String, Integer>>> comp = new LinkedList<Pair<IToken, ILemmaTransformation<String, Integer>>>();
			for (IToken possTok : possibleStems) {
				Pair<ILemmaTransformation<String, Integer>, Double> pair = lemmaSuffixProbs
						.get(possTok);
				ILemmaTransformation<String, Integer> traf;
				if (pair != null) {
					traf = pair.getKey();

				} else {
					traf = LemmaUtil.defaultLemmaRepresentation(possTok,
							model.getData());
				}
				comp.add(Pair.of(possTok, traf));

			}
			try {
				best = Collections.max(comp, lemmaComparator).getKey();
			} catch (Exception e) {
				System.err.println(t);
				return null;
			}
		}
		return best;
	}
}
