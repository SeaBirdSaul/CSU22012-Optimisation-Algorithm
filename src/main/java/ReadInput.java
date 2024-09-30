package main.java;

import java.io.*;
import java.util.*;

import static java.util.Arrays.*;



public class ReadInput {
    public Map<String, Object> data;

    public ReadInput() {
        data = new HashMap<String, Object>();
    }

    public double fitness(int[][] solution) {
        // checks to see if cache is overflowing
        int numOfCache = (int) data.get("number_of_caches");
        int numOfVideos = (int) data.get("number_of_videos");
        int cacheSize = (int) data.get("cache_size");
        int[] videoSize = (int[]) data.get("video_size_desc");
        // check solution incase any caches are overflowing
        for (int i = 0; i < numOfCache; i++) {
            int temp = 0;
            for (int j = 0; j < numOfVideos; j++) {
                if (solution[i][j] == 1) {
                    temp = temp + videoSize[j];
                    if (temp > cacheSize) return -1.0;
                }
            }
        }

        Map vidReq = (Map) data.get("video_ed_request"); //format: "fileID,EPID: numOfReq"
        //math: ans = number of requests x (LatencyFromDC - latencyFromCache)
        double totalReq = 0;
        double totalTimeSaved = 0;
        for (Object entry : vidReq.entrySet()) {
            String[] split = entry.toString().split("=");
            String[] IDs = split[0].split(",");
//            IDs[0]; //File ID
//            IDs[1]; //EP ID
//            split[1]; //requ
            List EPToDCLate = (List) data.get("ep_to_dc_latency"); // letency for each ep
            int DCLate = (int) EPToDCLate.get(Integer.parseInt(IDs[1]));
            List<List> EPToCacheLateList = (List<List>) data.get("ep_to_cache_latency");// 2d array ep down left cache across top
            List<Integer> EPToCacheLate = EPToCacheLateList.get(Integer.parseInt(IDs[1])); //array of cache delays to ep
            int cacheOfFile = -1;
            for (int i = 0; i < numOfCache; i++) {
                if (solution[i][Integer.parseInt(IDs[0])] == 1) {
                    cacheOfFile = i;
                    break;
                }
            }
            if (cacheOfFile >= 0) {
                int lateDiff = DCLate - EPToCacheLate.get(cacheOfFile);
                int ans = lateDiff * Integer.parseInt(split[1]);
                if (ans > 0) {
                    totalTimeSaved = totalTimeSaved + ans;
                }
            }
            totalReq = totalReq + Integer.parseInt(split[1]);
//            System.out.println(Integer.parseInt(split[1]));
//            System.out.println(lateDiff);
//            System.out.println((ans));
        }
//        System.out.println(totalTimeSaved);
//        System.out.println(totalReq);
//        System.out.println((totalTimeSaved/totalReq)*1000);
        return (totalTimeSaved / totalReq) * 1000;
    }


    // tested and works on example.in
    public int[][] solution() {
        int numOfCache = (int) data.get("number_of_caches");
        int numOfVideos = (int) data.get("number_of_videos");
        int[][] solution = new int[numOfCache][numOfVideos];
        int cacheSize = (int) data.get("cache_size");
        int[] videoSize = (int[]) data.get("video_size_desc");
        for (int i = 0; i < numOfCache; i++) {
            int tempSize = 0;
            for (int j = 0; j < numOfVideos; j++) {
                if (i > 0) {
                    for (int k = 0; k < numOfCache; ) {
                        if (solution[k][j] == 1) {
                            solution[i][j] = 0;
                            j++;
                            k = 0;
                        } else k++;
                    }
                    if (tempSize + videoSize[j] <= cacheSize) {
                        solution[i][j] = 1;
                        tempSize += videoSize[j];
                    } else solution[i][j] = 0;
                    break;
                }
                if (tempSize + videoSize[j] <= cacheSize) {
                    solution[i][j] = 1;
                    tempSize += videoSize[j];
                } else solution[i][j] = 0;
            }
        }

        return solution;
    }


    public int[][] hillClimbing() {
        double currentBest = 0;
        int[][] solution = new int[(int) data.get("number_of_caches")][(int) data.get("number_of_videos")];
        //int[][] bestMove = new int[1][1];
        boolean improve = true;
        while (improve) {
            int bestRow = 0;
            int bestCol = 0;
            improve = false;
            for (int i = 0; i < (int) data.get("number_of_caches"); i++) {
                for (int j = 0; j < (int) data.get("number_of_videos"); j++) {
                    solution[i][j] = 1;
                    double solFit = fitness(solution);
                    if (solFit > currentBest) {
                        currentBest = solFit;
                        bestRow = i;
                        bestCol = j;
                        improve = true;
                    }else solution[i][j] = 0;
                }
            }
            //System.out.println(currentBest);
            if (!improve) return solution;
            else solution[bestRow][bestCol] = 1;
        }
        return solution;
    }




    public void genetic() {
        int popSize = 10;
        List<List<Object>> population = new ArrayList<>();
        genRandPop(popSize, population);
        sort(population);
        for(int l = 0; l < popSize; l++){
            mutation((int[][]) population.get(l).get(1));
            if(getRandom(1,10) == 10) crossOver(popSize, population, 0, 0);
        }
        theCulling(popSize, population);
        double bestValue = (double) population.get(0).get(0);
        for(int i = (population.size()/popSize)+5; i <= population.size()/2; i++){
            crossOver((population.size()/popSize)+5,population, 1, i);
            mutation((int[][]) population.get(i).get(1));
            mutation((int[][]) population.get(population.size()-i).get(1));
        }
        if(bestValue <1) bestValue = fitness(hillClimbing());
        double currentBest = -4;
        while(bestValue != currentBest){
            if(currentBest>bestValue) bestValue = currentBest;
            sort(population);
            for(int l = 0; l < popSize; l++){
                if(population.size() == 0) genRandPop(popSize, population);
                mutation((int[][]) population.get(l).get(1));
                if(getRandom(1,10) == 10) crossOver(popSize, population, 0, 0);
            }
            theCulling(popSize, population);
            currentBest = (double) population.get(0).get(0);
            if(currentBest<1) {
                int[][] temp = hillClimbing();
                population.get(0).set(0,(int)fitness(temp));
                population.get(0).set(1, temp);
            }
            for(int i = (population.size()/popSize)+5; i <= population.size()/2; i++){
                crossOver((population.size()/popSize)+5, population, 1, i);
                mutation((int[][]) population.get(i).get(1));
                mutation((int[][]) population.get(population.size()-i).get(1));
            }
            System.out.println(currentBest);
            if(currentBest < 0){
                population.clear();
                genetic();
            }
        }
        if(bestValue == -1.0){
            population.clear();
            genetic();
        }
        //System.out.println(bestValue);
    }


    private void genRandPop(int popSize, List<List<Object>> population){
        for (int k = 0; k < popSize; k++) {
            int[][] solution = new int[(int) data.get("number_of_caches")][(int) data.get("number_of_videos")];
            for (int i = 0; i < (int)data.get("number_of_caches"); i++) {
                for (int j = 0; j < (int)data.get("number_of_videos"); j++) {
                    int tempVal = getRandom(1, 6);
                    if (tempVal == 6) {
                        solution[i][j] = 1;
                    }
                }
            }
            population.add(new ArrayList<>());
            population.get(k).add(fitness(solution));
            population.get(k).add(solution);
        }
    }
    private void sort(List population){
        Comparator<ArrayList<Object>> comparator = (a,b) -> Double.compare((int)(double)b.get(0), (int)(double) a.get(0));
        population.sort(comparator);
    }
    public void theCulling(int popSize, List<List<Object>> toCull) {
        sort(toCull);
        int[][] emptySol = new int[(int)data.get("number_of_caches")][(int)data.get("number_of_videos")];
        for(int i = popSize/10; i < toCull.size(); i++){
            toCull.get(i).set(1, emptySol);
            toCull.get(i).set(0,-1.0);
        }
    }

    public void crossOver(int popSize, List<List<Object>> population, int check, int checkLink){
        //randomly pics 2 solutions
        //splits them in half and creates 2 new solutions
        //if new solutions returns -1, delete else keep

        int splitValue1 = getRandom(0, popSize-2); //random number of solutions from start
        int splitValue2 = popSize - splitValue1; //random number of solutions from end
        int[][] solutionStart = (int[][]) population.get(splitValue1).get(1);
        int[][] solutionEnd = (int[][]) population.get(splitValue2).get(1);
        int[][] child1 = new int[(int)data.get("number_of_caches")][(int)data.get("number_of_videos")];
        int[][] child2 = child1;
        for(int i = 0; i < (int)data.get("number_of_caches"); i++){
            for(int j = 0; j <(int)data.get("number_of_videos"); j++){
                if(i >= (int)data.get("number_of_caches")/2) {
                    child1[i][j] = solutionEnd[i][j];
                    child2[i][j] = solutionStart[i][j];
                }else{
                    child1[i][j] = solutionStart[i][j];
                    child2[i][j] = solutionEnd[i][j];
                }
            }
        }
        if(check == 0) {
            if (fitness(child1) != -1) {
                population.add(new ArrayList<>());
                population.get(population.size() - 1).add(fitness(child1));
                population.get(population.size() - 1).add(child1);
            }
            if (fitness(child2) != -1) {
                population.add(new ArrayList<>());
                population.get(population.size() - 1).add(fitness(child2));
                population.get(population.size() - 1).add(child2);
            }
        } else if(check == 1){
            population.get(checkLink).set(0, fitness((child1)));
            population.get(checkLink).set(1, child1);
            population.get(population.size()-checkLink).set(0, fitness((child2)));
            population.get(population.size()-checkLink).set(1, child2);
        }
        sort(population);
    }

    private void mutation(int[][] solution){
        int outOf = (int)data.get("number_of_caches") * (int)data.get("number_of_videos");
        for(int i = 0; i < (int)data.get("number_of_caches"); i++){
            for(int j = 0; j <(int)data.get("number_of_videos"); j++) {
                if(getRandom(1,outOf) == outOf){
                    if(solution[i][j] == 1) solution[i][j] = 0;
                    else solution[i][j] = 1;
                }
            }
        }
    }
    private int getRandom(int min, int max) {
        return new Random().nextInt(max - min + 1) + 1;
    }



/*
* line 1 contains 5 numbers:
* num of vids - v
* num of eps - e
* num of req - r
* num of cache - c
* cap of cache - x
*
* line 2 contains v numbers
* sizes of videos
*
* next section contains e lines and describes each ep
* first line: contains latency to dc and how many caches is connected (ep 0)
* next c lines are latency of ep (0) to respective cache
*
* last section contains R request descriptions on seperate lines. Formatted as follows
* Rv, video id
* Re, ep that sent request
* Rn, number of requests
* */


    public void readGoogle(String filename) throws IOException {
             
        BufferedReader fin = new BufferedReader(new FileReader(filename)); // makes a reader called "fin"

        String system_desc = fin.readLine(); // reads the first line of "fin" stored in "system_desc"
        String[] system_desc_arr = system_desc.split(" "); // splits the line and stores split data in array called "system_desc_arr"
        // moves each item in the array into a labelled int
        int number_of_videos = Integer.parseInt(system_desc_arr[0]);
        int number_of_endpoints = Integer.parseInt(system_desc_arr[1]);
        int number_of_requests = Integer.parseInt(system_desc_arr[2]);
        int number_of_caches = Integer.parseInt(system_desc_arr[3]);
        int cache_size = Integer.parseInt(system_desc_arr[4]);
    
        Map<String, String> video_ed_request = new HashMap<String, String>(); // makes a new hashmap called "video_ed_request"
        String video_size_desc_str = fin.readLine(); // reads the second line of "fin"
        String[] video_size_desc_arr = video_size_desc_str.split(" "); // splits the line and stores split data in array called "video_size_desc_arr"
        int[] video_size_desc = new int[video_size_desc_arr.length]; // makes an int arry that is the same size as the string array
        for (int i = 0; i < video_size_desc_arr.length; i++) {
            video_size_desc[i] = Integer.parseInt(video_size_desc_arr[i]); // stores the values of the string array into the int array
        }

        // makes 3 new array lists
        List<List<Integer>> ed_cache_list = new ArrayList<List<Integer>>();
        List<Integer> ep_to_dc_latency = new ArrayList<Integer>();
        List<List<Integer>> ep_to_cache_latency = new ArrayList<List<Integer>>();
        for (int i = 0; i < number_of_endpoints; i++) {
            ep_to_dc_latency.add(0); // adds 0 to end of "ep_to_dc_latency"
            ep_to_cache_latency.add(new ArrayList<Integer>()); // adds a new arrayList to ep_to_cache_latency
    
            String[] endpoint_desc_arr = fin.readLine().split(" "); // splits the 3rd line of "fin" and stores it in "endpoint_desc_arr"
            int dc_latency = Integer.parseInt(endpoint_desc_arr[0]); // stores the first element array above in "ds_latency"
            int number_of_cache_i = Integer.parseInt(endpoint_desc_arr[1]); // stores the second element in "number_of_cache_i"
            ep_to_dc_latency.set(i, dc_latency); // replaces element at pos i with "dc_latency"
    
            for (int j = 0; j < number_of_caches; j++) {
                ep_to_cache_latency.get(i).add(ep_to_dc_latency.get(i) + 1); // adds element in "ep_to_dc_latency"+1 at i to i pos at "ep_to_cache_latency"
            }
    
            List<Integer> cache_list = new ArrayList<Integer>();
            for (int j = 0; j < number_of_cache_i; j++) {
                String[] cache_desc_arr = fin.readLine().split(" "); // stores the next split line of "fin" in an array
                int cache_id = Integer.parseInt(cache_desc_arr[0]); // saves the first element of "cache_desc_arr" to an int
                int latency = Integer.parseInt(cache_desc_arr[1]); // saves the second element of "cache_desc_arr" to an int
                cache_list.add(cache_id); // adds the "cache_id" to the "cache_list"
                ep_to_cache_latency.get(i).set(cache_id, latency); // replaces element at pos cache_id with "latency"
            }
            ed_cache_list.add(cache_list); // adds "cache_list" to "ed_cache_list"
        }
    
        for (int i = 0; i < number_of_requests; i++) {
            String[] request_desc_arr = fin.readLine().split(" "); // gets next line of "fin"
            String video_id = request_desc_arr[0];
            String ed_id = request_desc_arr[1];
            String requests = request_desc_arr[2];
            video_ed_request.put(video_id + "," + ed_id, requests); //addes next lines of "fin" to "video_ed_request"
        }
    
        data.put("number_of_videos", number_of_videos);
        data.put("number_of_endpoints", number_of_endpoints);
        data.put("number_of_requests", number_of_requests);
        data.put("number_of_caches", number_of_caches);
        data.put("cache_size", cache_size);
        data.put("video_size_desc", video_size_desc);
        data.put("ep_to_dc_latency", ep_to_dc_latency);
        data.put("ep_to_cache_latency", ep_to_cache_latency);
        data.put("ed_cache_list", ed_cache_list);
        data.put("video_ed_request", video_ed_request);
    
        fin.close();
     
     }

     public String toString() {
        String result = "";

        //for each endpoint: 
        for(int i = 0; i < (Integer) data.get("number_of_endpoints"); i++) {
            result += "enpoint number " + i + "\n";
            //latendcy to DC
            int latency_dc = ((List<Integer>) data.get("ep_to_dc_latency")).get(i);
            result += "latency to dc " + latency_dc + "\n";
            //for each cache
            for(int j = 0; j < ((List<List<Integer>>) data.get("ep_to_cache_latency")).get(i).size(); j++) {
                int latency_c = ((List<List<Integer>>) data.get("ep_to_cache_latency")).get(i).get(j); 
                result += "latency to cache number " + j + " = " + latency_c + "\n";
            }
        }

        return result;
    }

    public static void main(String[] args) throws IOException {  
        ReadInput ri = new ReadInput();
        ri.readGoogle("input/me_at_the_zoo.in");
        System.out.println(ri.data.get("video_ed_request"));
        System.out.println(ri.toString());
        //System.out.println(ri.fitness(ri.solution()));
        //ri.hillClimbing();
        ri.genetic();
        //int[][] solution = ri.solution();
    }
}
