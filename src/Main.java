import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;


public class Main {
    public static void main(String[] args) {
        BufferedReader br = null;
        ArrayList<String> documentClasses = new ArrayList<>();
        ArrayList<HashMap<String,Double>> unigram = new ArrayList<>();
        ArrayList<HashMap<ArrayList<String>,Double>> bigram = new ArrayList<>();
        ArrayList<Integer> totalNumberOfWords = new ArrayList<>();
        ArrayList<Double> probabilityOfTrainClass = new ArrayList<>();
        int totalNumberOfTrains = 0;
        try {
            br = new BufferedReader(new InputStreamReader(
                    new FileInputStream("HAM-Train.txt"), "UTF-8"));
            String s;
            while ((s=br.readLine())!=null){
                String title[] = s.split("@@@@@@@@@@");
                String w[] = title[1].split(" ");
                if (!documentClasses.contains(title[0])) {
                    totalNumberOfWords.add(w.length + 1);
                    probabilityOfTrainClass.add(1.0);
                    totalNumberOfTrains++;
                    documentClasses.add(title[0]);
                    HashMap<String,Double> classWords = new HashMap<>();
                    HashMap<ArrayList<String>,Double> bigramTmp = new HashMap<>();
                    String lastWord = null;
                    classWords.put(null, 1.0);
                    for (int i = 0; i < w.length ; i++) {
                        ArrayList<String> tmp = new ArrayList<String>();
                        tmp.add(w[i]);
                        tmp.add(lastWord);
                        if(!classWords.containsKey(w[i])) {
                            classWords.put(w[i], 1.0);
                            bigramTmp.put(tmp,1.0);
                        }
                        else {
                            double newValue = classWords.get(w[i]) + 1;
                            classWords.replace(w[i], newValue);
                            if(!bigramTmp.containsKey(tmp))
                                bigramTmp.put(tmp, 1.0);
                            else{
                                double newBigramValue = bigramTmp.get(tmp) + 1;
                                bigramTmp.replace(tmp, newBigramValue);
                            }
                        }
                        lastWord = w[i];
                    }
                    unigram.add(classWords);
                    bigram.add(bigramTmp);
                }
                else{
                    int index = documentClasses.indexOf(title[0]);
                    double classCount = probabilityOfTrainClass.get(index) + 1;
                    probabilityOfTrainClass.remove(index);
                    probabilityOfTrainClass.add(index,classCount);
                    totalNumberOfTrains++;
                    int wordCount = totalNumberOfWords.get(index) + w.length + 1;
                    totalNumberOfWords.remove(index);
                    totalNumberOfWords.add(index,wordCount);
                    String lastWord = null;
                    double nullCount = unigram.get(index).get(null) + 1;
                    unigram.get(index).replace(null, nullCount);
                    for (int i = 0; i < w.length ; i++) {
                        ArrayList<String> tmp = new ArrayList<String>();
                        tmp.add(w[i]);
                        tmp.add(lastWord);
                        if(!unigram.get(index).containsKey(w[i])) {
                            unigram.get(index).put(w[i], 1.0);
                            bigram.get(index).put(tmp, 1.0);
                        }
                        else {
                            double newValue = unigram.get(index).get(w[i]) + 1;
                            unigram.get(index).replace(w[i], newValue);
                            if(!bigram.get(index).containsKey(tmp))
                                bigram.get(index).put(tmp, 1.0);
                            else{
                                double newBigramValue = bigram.get(index).get(tmp) + 1;
                                bigram.get(index).replace(tmp, newBigramValue);
                            }
                        }
                        lastWord = w[i];
                    }
                }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < bigram.size() ; i++) {
            for (HashMap.Entry<ArrayList<String>, Double> entry : bigram.get(i).entrySet()) {
                double p = entry.getValue() / unigram.get(i).get(entry.getKey().get(1));
                entry.setValue(p);
            }
        }
        for (int i = 0; i < unigram.size() ; i++) {
            for (HashMap.Entry<String, Double> entry : unigram.get(i).entrySet()) {
                double p = entry.getValue() / totalNumberOfWords.get(i);
                entry.setValue(p);
            }
        }
        for (int i = 0; i < probabilityOfTrainClass.size() ; i++) {
            double tmp = probabilityOfTrainClass.get(i) / totalNumberOfTrains;
            probabilityOfTrainClass.set(i,tmp);
        }

        double landa1 = 0.9, landa2 = 0.8;

        try {
            br = new BufferedReader(new InputStreamReader(
                    new FileInputStream("HAM-Test.txt"), "UTF-8"));
            String s;
            int fMeasure[][] = new int[documentClasses.size()][documentClasses.size()];
            while ((s=br.readLine())!=null){
                String title[] = s.split("@@@@@@@@@@");
                String w[] = title[1].split(" ");
                double p[] = new double[documentClasses.size()];
                for (int i = 0; i < documentClasses.size() ; i++) {
                    p[i] = Math.log(probabilityOfTrainClass.get(i));
                    String lastWord = null;
                    for (int j = 0; j < w.length ; j++) {
                        ArrayList<String> twoLastWords = new ArrayList<>();
                        twoLastWords.add(w[j]);
                        twoLastWords.add(lastWord);
                        double pbi = bigram.get(i).containsKey(twoLastWords) ? bigram.get(i).get(twoLastWords) : 0 ;
                        double puni = unigram.get(i).containsKey(w[j]) ? unigram.get(i).get(w[j]) : 0 ;
                        p[i] += Math.log((landa2 * pbi) + (1-landa2) * ((landa1 * puni) + (1-landa1)*(1.0/totalNumberOfWords.get(i))));
                        lastWord = w[j];
                    }
                    //System.out.println("P("+documentClasses.get(i)+") = " + p[i]);
                }
                int predictedClass = 0;
                for (int i = 1; i < documentClasses.size() ; i++) {
                    if(p[i] > p[predictedClass])
                        predictedClass = i;
                }
                int realClass = 0;
                for (int i = 1; i < documentClasses.size() ; i++) {
                    if(documentClasses.get(i).equals(title[0]))
                        realClass = i;
                }
                fMeasure[realClass][predictedClass]++;
                //System.out.println("Predicted : " + documentClasses.get(predictedClass) + " / real : " + title[0]);
            }
            double precision[] = new double[documentClasses.size()];
            double recall[] = new double[documentClasses.size()];
            System.out.println("Table :");
            for (int i = 0; i < documentClasses.size() ; i++) {
                System.out.print("      {" + documentClasses.get(i) + "} : ");
                for (int j = 0; j < documentClasses.size() ; j++) {
                    System.out.print(fMeasure[i][j] + " ");
                }
                System.out.println();
            }
            System.out.println();
            for (int j = 0; j < documentClasses.size() ; j++) {
                for (int i = 0; i < documentClasses.size() ; i++) {
                    precision[j] += fMeasure[i][j];
                    recall[j] += fMeasure[j][i];
                }
                precision[j] = fMeasure[j][j] / precision[j];
                recall[j] = fMeasure[j][j] / recall[j];
                System.out.println("Class{" + documentClasses.get(j) + "} : Precision = " + precision[j] + " , Recall = " + recall[j]
                        + " , F-measure = " + (2 * precision[j] * recall[j]) / (precision[j] + recall[j]));
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
