package com.tokenizer.tokenizer.dictionary;

import com.tokenizer.tokenizer.dictionary.words.WordsElement;
import lombok.Data;

import java.util.List;

/**
 * Created by Huihua Niu
 * on 2019/7/17 11:59
 */
@Data
public class TrieNode {

    private int pos, length;

    private WordsElement phrase;

    private TrieNode parent;

    private List<TrieNode> children;
}
