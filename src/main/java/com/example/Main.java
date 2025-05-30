package com.example;


import java.io.FileWriter;
import java.io.IOException;
import de.learnlib.algorithm.lstar.dfa.ClassicLStarDFA;
import de.learnlib.algorithm.ttt.dfa.TTTLearnerDFA;
import de.learnlib.filter.statistic.oracle.DFACounterOracle;
import de.learnlib.util.Experiment.DFAExperiment;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.impl.Alphabets;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.automaton.fsa.impl.CompactDFA;
import net.automatalib.util.automaton.builder.AutomatonBuilders;
import java.util.*;

public class Main {

    private static CompactDFA<Character> constructSUL() {
        Alphabet<Character> sigma = Alphabets.characters('0', '1');

        return AutomatonBuilders.newDFA(sigma)
                .withInitial("q0")
                .from("q0").on('0').to("q0").on('1').to("q1")
                .from("q1").on('0').to("q2").on('1').to("q0")
                .from("q2").on('0').to("q1").on('1').to("q2")
                .withAccepting("q0")
                .create();
    }

    public static void main(String[] args) throws Exception {

        // Target DFA 
        CompactDFA<Character> targetDFA = constructSUL();
        Alphabet<Character> alphabet = targetDFA.getInputAlphabet();
       
         
        
        // DatasetLoad

        //When experiment for Abbadingo dataset,

        Map<List<Character>, Boolean> dataset = DatasetLoader.load("data/train.1.gz"); // Abbadingo Dataset
        List<List<Character>> testSet = TestsetLoader.load("data/test.1.gz"); //  Abbadingo testset

        //When experiment for Random dataset,
        
        // Map<List<Character>, Boolean> dataset = RandomDatasetGenerator.generate(targetDFA, alphabet, 1000,7, 15); //Random dataset
        // List<List<Character>> testSet = RandomDatasetGenerator.generateInputs(alphabet, 300, 7, 15); // Random testset


        //This is just to check how gz files are parsed (Dataset)
        try (FileWriter writer = new FileWriter("dataset1_output.txt")) {
            for (Map.Entry<List<Character>, Boolean> entry : dataset.entrySet()) {
                List<Character> input = entry.getKey();
                Boolean output = entry.getValue();

                String inputStr = input.stream()
                        .map(String::valueOf)
                        .reduce((a, b) -> a + " " + b)
                        .orElse("");

                writer.write(inputStr + " -> " + (output ? "1" : "0") + "\n");
            }
            System.out.println("Dataset saved: datase1t_output.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }

         //This is just to check how gz files are parsed (Testset)

        try (FileWriter writer = new FileWriter("testset1_output.txt")) {
            for (List<Character> input : testSet) {
                String inputStr = input.stream()
                        .map(String::valueOf)
                        .reduce((a, b) -> a + " " + b)
                        .orElse("");
        
                writer.write(inputStr + " -> \n");
            }
            System.out.println("Testset saved: testset1_output.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }


        // Teacher
        ExampleBasedTeacher teacher = new ExampleBasedTeacher(dataset);
        

        // Membership Oracle
        DFACounterOracle<Character> mqOracle = new DFACounterOracle<>(teacher);

        // Lstar and TTT 
        ClassicLStarDFA<Character> learner = Algorithms.createLStar(alphabet, mqOracle);
        //TTTLearnerDFA<Character> learner = Algorithms.createTTT(alphabet, mqOracle);
       

        // Experiement
        DFAExperiment<Character> experiment = new DFAExperiment<>(learner, teacher, alphabet);
        experiment.setProfile(true);
        long startTime = System.nanoTime();
        experiment.run();
        String mqCount = mqOracle.getStatisticalData().getSummary();

        // calculate Runtime in ms
        long endTime = System.nanoTime();
        long runtimeMillis = (endTime - startTime) / 1_000_000;
        System.out.println("Runtime: " + runtimeMillis + " ms");

        
        // Our DFA
        DFA<?, Character> hypothesis = experiment.getFinalHypothesis();

        // Print Result

        System.out.println("States: " + hypothesis.size());
        System.out.println("Sigma: " + alphabet.size());


        Evaluator.evaluateAndLog(
                        hypothesis,
                        testSet,             // Testset form :  List<List<Character>>
                        "Lstar",
                        (int) experiment.getRounds().getCount(),
                        mqCount,
                        runtimeMillis,
                        "results.csv"
        );

        
        System.out.println("MQ:" + mqOracle.getStatisticalData().getSummary()); 
        System.out.println("EQ : " + experiment.getRounds().getCount());
        
    }

   
}