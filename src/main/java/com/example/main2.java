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

public class main2 {

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
        Map<List<Character>, Boolean> testSet = DatasetLoader.load("data/test.1.gz"); //  Abbadingo testset

        //When experiment for Random dataset,
        
        // Map<List<Character>, Boolean> dataset = RandomDatasetGenerator.generate(targetDFA, alphabet, 10000,7, 15); //Random dataset
        // Map<List<Character>, Boolean> testSet = RandomDatasetGenerator.generate(targetDFA, alphabet, 3000, 7, 15); // Random testset


        // This is just a code to check how gz files are parsed 
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

        try (FileWriter writer = new FileWriter("testset1_output.txt")) {
            for (Map.Entry<List<Character>, Boolean> entry : testSet.entrySet()) {
                List<Character> input = entry.getKey();
                Boolean output = entry.getValue();

                String inputStr = input.stream()
                        .map(String::valueOf)
                        .reduce((a, b) -> a + " " + b)
                        .orElse("");

                writer.write(inputStr + " -> " + (output ? "1" : "0") + "\n");
            }
            System.out.println("Dataset saved: testset1_output.txt");
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

        // Print Result
        DFA<?, Character> hypothesis = experiment.getFinalHypothesis();

        System.out.println("States: " + hypothesis.size());
        System.out.println("Sigma: " + alphabet.size());



        //In our experiment, roundCount is closely related to the number of equivalence queries (EQs), as each round typically follows an EQ that reveals a counterexample.

        double acc = Evaluator.evaluateAccuracy(
                hypothesis,
                testSet,
                "TTT", 
                (int) experiment.getRounds().getCount(),
                mqCount,
                runtimeMillis,                    
             // save file
                "results.csv"
        );

        System.out.println("Accuracy: " + acc + "%");
        System.out.println("MQ:" + mqOracle.getStatisticalData().getSummary()); 
        System.out.println("EQ : " + experiment.getRounds().getCount());
        
    }

   
}
