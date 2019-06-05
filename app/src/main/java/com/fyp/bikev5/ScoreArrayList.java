package com.fyp.bikev5;

import java.util.List;


public class ScoreArrayList {
    private List<Double> score;

    public ScoreArrayList(List<Double> score) {
        this.score = score;
    }

    public Double getAverage() {
        double sum = 0;
        for (Double i : score)
            sum = sum + i;

        int count
                = score.size();
        return sum / count;
    }

}
