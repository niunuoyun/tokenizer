package com.tokenizer.tokenizer.dictionary.words;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Huihua Niu
 * on 2019/7/17 11:40
 */
@Data
public class WordsElement implements Comparable<WordsElement> {

    public WordsElement() {
    }

    public WordsElement(String value) {
        this.value = value;
    }

    /** 词性 */
    private Set<String> typeSet = new HashSet<>();

    /** 词语内容 */
    private String value;

    /** 词频 */
    private double frequence;

    /** 在句子中的成分 **/
    private String sentenceElement;

    @Override
    public String toString() {
        return value+":"+typeSet.toString().replaceAll(",","/")+":"+frequence;
    }

    @Override
    public int compareTo(WordsElement o) {
        return this.getValue().compareTo(o.getValue());
    }
}
