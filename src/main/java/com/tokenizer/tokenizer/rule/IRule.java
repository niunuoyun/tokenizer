package com.tokenizer.tokenizer.rule;

import com.tokenizer.tokenizer.dictionary.words.WordsElement;

import java.util.List;

/**
 * Created by Huihua Niu
 * on 2019/7/17 11:45
 */
public interface IRule {
    List<WordsElement> matchRule();
}
