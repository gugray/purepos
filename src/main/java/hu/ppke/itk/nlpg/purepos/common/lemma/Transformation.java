package hu.ppke.itk.nlpg.purepos.common.lemma;

import hu.ppke.itk.nlpg.purepos.model.IVocabulary;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;

public class Transformation {

    public static long TAG_SHIFT = 10000000L;
    public static long CASING_SHIFT = 1000000L;
    public static long REMOVE_START_SHIFT = 10000L;
    public static long REMOVE_END_SHIFT = 100L;
    public static HashMap<Integer,String> CASING_DICTIONARY;

    static {
        CASING_DICTIONARY = new HashMap<Integer, String>();
        CASING_DICTIONARY.put(-1,"_"); // lowercase
        CASING_DICTIONARY.put(0,"-"); // uppercase
        CASING_DICTIONARY.put(1,"^"); // no change
    }
	/*
	 * @param tag
	 * @param casing
	 * @param removeStart
	 * @param removeEnd
	 * @return  a nine digit long long code, generated by shifting the parameters with the static variables of this class,
	 * 			where:
	 * 			[?] tag
	 * 			[1] casing, can be -1,0,1 (the sign is the sign of the whole number)
	 * 			[2] removeStart
	 * 			[2] removeEnd
	 * 			[2] addEnd
	 */
    public static long createCode(int tag, int casing,int removeStart, int removeEnd, int addEnd){
        if (casing == -1){
            return	(tag * TAG_SHIFT + Math.abs(casing) * CASING_SHIFT  + removeStart * REMOVE_START_SHIFT +
                    removeEnd * REMOVE_END_SHIFT  + addEnd) * casing;
        } else {
            return tag * TAG_SHIFT + casing * CASING_SHIFT + removeStart * REMOVE_START_SHIFT + removeEnd * REMOVE_END_SHIFT + addEnd;
        }
    }

    public static int getTag(Long code){
        return (int) (Math.abs(code) / TAG_SHIFT);
    }

    public static int getCasing(Long code){
        return (int) ((code % TAG_SHIFT) / CASING_SHIFT);
    }

    public static int getRemoveStart(Long code) {
        return (int) ((Math.abs(code) % CASING_SHIFT) / REMOVE_START_SHIFT);
    }

    public static int minimalCutLength(Long code) { // getRemoveEnd
        return (int) ((Math.abs(code) % REMOVE_START_SHIFT) / REMOVE_END_SHIFT);
    }

    public static int getAddEnd(Long code){
        return (int) (Math.abs(code) % REMOVE_END_SHIFT);
    }

    public static String getRemovedFromStart(String lemmaStuff,int addtoEnd){
        int lemmaStuffLength = lemmaStuff.length();
        if (addtoEnd == lemmaStuffLength){
            return "";
        } else {
            return lemmaStuff.substring(0,lemmaStuffLength-addtoEnd);
        }
    }

    public static String getRemovedFromEnd(String lemmaStuff, int addtoEnd){
        int lemmaStuffLength = lemmaStuff.length();
        if (addtoEnd == lemmaStuffLength){
            return lemmaStuff;
        } else {
            return lemmaStuff.substring(lemmaStuffLength-addtoEnd,lemmaStuffLength);
        }
    }

    /*
     * @param word
     * @param lemma
     * @return 	0 : the initial casing is the same
     * 			1 : the word's first letter was lowercase, but the lemma's is uppercase
     * 		   -1 : the word's first letter	was uppercase, but the lemma's is lowercase
     */
    public static int checkCasing(String word, String lemma) {
        if (word.length() > 0 && lemma.length() > 0) {
            String ws = word.substring(0, 1), ls = lemma.substring(0, 1);
            boolean isUppered = ws.toUpperCase().equals(ls);
            boolean isLowered = ws.toLowerCase().equals(ls);
            if(ws.equals(ls) || (!isUppered && !isLowered)){
                return 0;
            } else if (isUppered){
                return 1;
            } else {
                return -1;
            }
        } else {
            return 0;
        }
    }

    public static String decodeTag(int ID, IVocabulary<String, Integer> tagVocabulary){
        return tagVocabulary.getWord(ID) + " (" + ID + ")";
    }

    public static String decodeTag(String ID, IVocabulary<String, Integer> tagVocabulary){
        return tagVocabulary.getWord(Integer.valueOf(ID)) + " (" + ID + ")";
    }

    public static String transformation_toString(Pair<String,Long> representation){
        String lemmaStuff = representation.getLeft();
        long code = representation.getRight();
        // transformation decoding
        String casing = Transformation.CASING_DICTIONARY.get(Transformation.getCasing(code));
        int removeStart = Transformation.getRemoveStart(code);
        int removeEnd = Transformation.minimalCutLength(code);
        int addE = Transformation.getAddEnd(code);
        String addStart = Transformation.getRemovedFromStart(lemmaStuff,addE);
        String addEnd = Transformation.getRemovedFromEnd(lemmaStuff,addE);
        String spacer = " ";
        return casing + spacer + removeStart + spacer + "\"" + addStart + "\"" + spacer + removeEnd + spacer + "\"" + addEnd
                + "\"";
    }

    public static Pair<String,Long> parse(String pair){
        String raw = pair.substring(1,pair.length()-1);
        int splitindex = raw.lastIndexOf(",");
        return Pair.of(raw.substring(0,splitindex),Long.parseLong(raw.substring(splitindex+1)));
    }
}
