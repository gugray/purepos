package hu.ppke.itk.nlpg.purepos.common;

import hu.ppke.itk.nlpg.corpusreader.CorpusReader;
import hu.ppke.itk.nlpg.corpusreader.ParsingException;
import hu.ppke.itk.nlpg.docmodel.IDocument;
import hu.ppke.itk.nlpg.purepos.model.internal.RawModel;

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;

import org.junit.Test;

public class SerializatorTest {
	@Test
	public void readWriteTest() throws ParsingException, IOException,
			ClassNotFoundException {
		CorpusReader r = new CorpusReader();
		IDocument d = r
				.read("Michael#Michael#[FN][NOM] Karaman#??Karaman#[FN][NOM]"
						+ " ,#,#[PUNCT] az#az#[DET] Ann#Ann#[FN][NOM] 1#1#[SZN][NOM]");
		RawModel model = new RawModel(3, 3, 10, 10);
		model.train(d);
		String pathname = "./_test.model";
		File f = new File(pathname);
		Serializator.writeModel(model, f);
		RawModel readModel = Serializator.readModel(f);

		// TODO: write equality test case, now it is enough that it doesn't fail
		String modelTagVocab = model.getTagVocabulary().toString();
		String readTagVocab = readModel.getTagVocabulary().toString();
		// System.out.println(modelTagVocab);
		// System.out.println(readTagVocab);
		Assert.assertEquals(modelTagVocab.length(), readTagVocab.length());
		Assert.assertEquals(model.getEmissionOrder(),
				readModel.getEmissionOrder());
		Assert.assertEquals(model.getRareFreqency(),
				readModel.getRareFreqency());
		Assert.assertEquals(model.getSuffixLength(),
				readModel.getSuffixLength());
		Assert.assertEquals(model.getTaggingOrder(),
				readModel.getTaggingOrder());
		Assert.assertEquals(model.getBOSIndex(), readModel.getBOSIndex());
		Assert.assertEquals(model.getEOSIndex(), readModel.getEOSIndex());
		Assert.assertEquals(model.getEOSTag(), readModel.getEOSTag());
		Assert.assertEquals(model.getEOSToken(), readModel.getEOSToken());
		Assert.assertEquals(model.getBOSTag(), readModel.getBOSTag());
		Assert.assertEquals(model.getBOSToken(), readModel.getBOSToken());
		Assert.assertEquals(model.getLastStat(), readModel.getLastStat());
		Assert.assertEquals(model.getSpecTokensLexicon().size(), readModel
				.getSpecTokensLexicon().size());
		Assert.assertEquals(model.getStandardTokensLexicon().size(), readModel
				.getStandardTokensLexicon().size());

		Serializator.deleteModel(f);
	}
}
