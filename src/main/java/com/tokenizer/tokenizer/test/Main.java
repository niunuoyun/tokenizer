package com.tokenizer.tokenizer.test;


import com.tokenizer.tokenizer.WordSegmenter;
import com.tokenizer.tokenizer.segmentation.SegmentationAlgorithm;
import com.tokenizer.tokenizer.segmentation.Word;

import java.util.List;

/**
 * Created by Huihua Niu
 * on 2019/7/17 12:24
 */
public class Main {
    public static void main(String[] args) {
        List<Word> words = WordSegmenter.seg("思必驰有哪些产品？", SegmentationAlgorithm.BidirectionalMaximumMatching);
        System.out.println(words);
    }
}
