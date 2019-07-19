/**
 * 
 * APDPlat - Application Product Development Platform
 * Copyright (c) 2013, huihua.niu, yang-shangchuan@qq.com
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.tokenizer.tokenizer.dictionary.words;

import com.tokenizer.tokenizer.dictionary.Dictionary;
import com.tokenizer.tokenizer.dictionary.impl.DictionaryTrie;
import com.tokenizer.tokenizer.recognition.PersonName;
import com.tokenizer.tokenizer.util.AutoDetector;
import com.tokenizer.tokenizer.util.ResourceLoader;
import com.tokenizer.tokenizer.util.WordConfTools;
import com.tokenizer.tokenizer.util.WordsResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 词典工厂
 通过系统属性及配置文件指定词典实现类（dic.class）和词典文件（dic.path）
 指定方式一，编程指定（高优先级）：
      WordConfTools.set("dic.class", "org.apdplat.word.dictionary.impl.DictionaryTrie");
      WordConfTools.set("dic.path", "classpath:dic.txt");
 指定方式二，Java虚拟机启动参数（中优先级）：
      java -Ddic.class=org.apdplat.word.dictionary.impl.DictionaryTrie -Ddic.path=classpath:dic.txt
 指定方式三，配置文件指定（低优先级）：
      在类路径下的word.conf中指定配置信息
      dic.class=org.apdplat.word.dictionary.impl.DictionaryTrie
      dic.path=classpath:dic.txt
 如未指定，则默认使用词典实现类（org.apdplat.word.dictionary.impl.DictionaryTrie）和词典文件（类路径下的dic.txt）
 * @author huihua.niu
 */
public final class DictionaryWordsFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(DictionaryWordsFactory.class);
    private static final int INTERCEPT_LENGTH = WordConfTools.getInt("intercept.length", 16);
    private DictionaryWordsFactory(){}
    public static final WordsDictionary getDictionary(){
        return DictionaryHolder.DIC;
    }
    public static void reload(){
        DictionaryHolder.reload();
    }
    private static final class DictionaryHolder{
        private static final WordsDictionary DIC = constructDictionary();
        private static WordsDictionary constructDictionary(){
            try{
                //选择词典实现，可以通过参数选择不同的实现
                String dicClass = WordConfTools.get("dic.class", "com.tokenizer.tokenizer.dictionary.impl.TrieV4");
                LOGGER.info("dic.class="+dicClass);
                return (WordsDictionary)Class.forName(dicClass.trim()).newInstance();
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException ex) {
                System.err.println("词典装载失败:"+ex.getMessage());
                throw new RuntimeException(ex);
            }
        }
        static{
            reload();
        }
        public static void reload(){
            AutoDetector.loadAndWatch(new ResourceLoader() {

                @Override
                public void clear() {
                    DIC.clear();
                }

                @Override
                public void load(List<String> lines) {
                    LOGGER.info("初始化词典");
                    long start = System.currentTimeMillis();
                    int count = 0;
                    for (String surname : PersonName.getSurnames()) {
                        if (surname.length() == 2) {
                            count++;
                            lines.add(surname);
                        }
                    }
                    LOGGER.info("将 " + count + " 个复姓加入词典");
                    List<WordsElement> words = getAllWords(lines);
                    lines.clear();
                    //dic dump
                    String dicDumpPath = WordConfTools.get("dic.dump.path");
                    if (dicDumpPath != null && dicDumpPath.length() > 0) {
                        try {
                            Files.write(Paths.get(dicDumpPath), Collections.singleton(words.toString()));
                        }catch (Exception e){
                            LOGGER.error("dic dump error!", e);
                        }
                    }
                    //构造词典
                    DIC.addAll(words);
                    //输出统计信息
                    showStatistics(words);
                    if (DIC instanceof DictionaryTrie) {
                        DictionaryTrie dictionaryTrie = (DictionaryTrie) DIC;
                        dictionaryTrie.showConflict();
                    }
                    System.gc();
                    LOGGER.info("词典初始化完毕，耗时：" + (System.currentTimeMillis() - start) + " 毫秒");
                }

                private void showStatistics(List<WordsElement> words) {
                    Map<Integer, AtomicInteger> map = new HashMap<Integer, AtomicInteger>();
                    words.forEach(word -> {
                        map.putIfAbsent(word.getValue().length(), new AtomicInteger());
                        map.get(word.getValue().length()).incrementAndGet();
                    });
                    //统计词数
                    int wordCount = 0;
                    //统计平均词长
                    int totalLength = 0;
                    for (int len : map.keySet()) {
                        totalLength += len * map.get(len).get();
                        wordCount += map.get(len).get();
                    }
                    LOGGER.info("词数目：" + wordCount + "，词典最大词长：" + DIC.getMaxLength());
                    for (int len : map.keySet()) {
                        if (len < 10) {
                            LOGGER.info("词长  " + len + " 的词数为：" + map.get(len));
                        } else {
                            LOGGER.info("词长 " + len + " 的词数为：" + map.get(len));
                        }
                    }
                    LOGGER.info("词典平均词长：" + (float) totalLength / wordCount);
                }

                @Override
                public void add(String line) {
                    //加入词典
                    getWords(line).stream().filter(w -> w.getValue().length() <= INTERCEPT_LENGTH).forEach(DIC::add);
                }

                @Override
                public void remove(String line) {
                    //移除词
                    getWords(line).forEach(DIC::remove);
                }

                private List<WordsElement> getAllWords(List<String> lines) {
                    List<WordsElement> wordsElements = new ArrayList<>();
                  //  List<WordsElement> wordsElements = lines.stream().flatMap(line -> wordsElements.contains(getWords(line))).collect(Collectors.toList());
                    return lines.stream().map(line -> getWords(line).get(0)).filter(w -> w!=null&&w.getValue().length() <= INTERCEPT_LENGTH).distinct().sorted().collect(Collectors.toList());
                }

                private List<WordsElement> getWords(String line) {
                    List<WordsElement> words = new ArrayList<>();
                    //一行以空格分隔可以放多个词
                    for (String word : line.split("\\s+")) {

                        WordsElement wordsElement = new WordsElement();
                        //处理词性词典
                        if (word.length() > 2 && word.contains(":")) {
                            String[] attr = word.split(":");
                            if (attr != null && attr.length > 0) {
                                if (attr.length==1)  wordsElement.setValue(attr[0]);
                                //忽略单字
                                if (attr[0].length() >0) wordsElement.setValue(attr[0]);
                                if (attr[0].length() >1) wordsElement.setTypeSet(new HashSet<>(Arrays.asList(attr[1].split("/"))));
                                if (attr.length>2)wordsElement.setFrequence(Integer.parseInt(attr[2]));

                            }
                        }else if (!StringUtils.isEmpty(word))wordsElement.setValue(word);
                        words.add(wordsElement);
                    }
                    return words;
                }
            }, WordConfTools.get("dic.path", "classpath:dic.txt")
                    + "," + WordConfTools.get("punctuation.path", "classpath:punctuation.txt")
                    + "," + WordConfTools.get("part.of.speech.dic.path", "classpath:part_of_speech_dic.txt")
                    + "," + WordConfTools.get("word.synonym.path", "classpath:word_synonym.txt")
                    + "," + WordConfTools.get("word.entity.path", "classpath:word_entity.txt")
                    + "," + WordConfTools.get("word.attribute.path", "classpath:word_attribute.txt")
                    + "," + WordConfTools.get("word.ask.path", "classpath:word_ask.txt")
                    + "," + WordConfTools.get("word.decoration.path", "classpath:word_ask.txt")
                    + "," + WordConfTools.get("word.antonym.path", "classpath:word_antonym.txt"));

        }
    }

    private static void test(String dicClass) throws Exception{
        WordConfTools.set("dic.class", dicClass);
        System.gc();
        Thread.sleep(60000);
        WordsDictionary dictionary = DictionaryWordsFactory.getDictionary();
        System.gc();
        Thread.sleep(60000);
        AtomicInteger h = new AtomicInteger();
        AtomicInteger e = new AtomicInteger();
        Stream<String> words = Files.lines(Paths.get("src/test/resources/dic.txt"));
        System.gc();
        Thread.sleep(60000);
        long start = System.currentTimeMillis();
        for(int i=0; i<100; i++){
            words.forEach(word -> {
                for (int j = 0; j < word.length(); j++) {
                    String sw = word.substring(0, j + 1);
                    for (int k = 0; k < sw.length(); k++) {
                        if (dictionary.contains(sw, k, sw.length() - k)) {
                            h.incrementAndGet();
                        } else {
                            e.incrementAndGet();
                        }
                    }
                }
            });
        }
        long cost = System.currentTimeMillis() - start;
        LOGGER.info(dicClass + " 未查询到的次数："+ e.get() + "， 查询到的次数：" +  h.get() + " 耗时：" + cost + " 毫秒");
        System.gc();
        Thread.sleep(60000);
        LOGGER.info("test finish");
        System.exit(0);
    }
    public static void main(String[] args) throws Exception{
        //速度和内存测试
        //打开 jvisualvm 后分别运行测试，不同测试位于不同进程中，避免相互影响
        //通过 jvisualvm 可以查看内存使用情况
        //test("org.apdplat.word.dictionary.impl.DictionaryTrie");
        test("com.tokenizer.tokenizer.dictionary.impl.DoubleArrayDictionaryTrie");
    }
}