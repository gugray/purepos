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
package hu.ppke.itk.nlpg.purepos.model.internal;

import hu.ppke.itk.nlpg.purepos.model.IMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TagMapper implements IMapper<Integer> {
	LinkedHashMap<Pattern, String> mapping;
	private Vocabulary<String, Integer> vocabulary;

	public TagMapper(Vocabulary<String, Integer> vocabulary) {
		this.vocabulary = vocabulary;
		mapping = new LinkedHashMap<Pattern, String>();
		init();
	}

	private void init() {
		add("^(.*)(\\|lat)(.*)$", "$1$3");
	}

	private void add(String regexp, String replacement) {
		Pattern p = Pattern.compile(regexp);
		mapping.put(p, replacement);
	}

	@Override
	public Integer map(Integer tag) {
		String tagStr = vocabulary.getWord(tag);
		for (Pattern p : mapping.keySet()) {
			Matcher matcher = p.matcher(tagStr);
			if (matcher.matches()) {
				String replacement = mapping.get(p);
				String repTagStr = matcher.replaceAll(replacement);
				Integer retTag = vocabulary.getIndex(repTagStr);
				if (retTag != null)
					return retTag;
			}
		}
		return tag;

	}

	@Override
	public List<Integer> map(List<Integer> elements) {
		ArrayList<Integer> ret = new ArrayList<Integer>();
		for (Integer e : elements) {
			ret.add(map(e));
		}
		return ret;
	}

}