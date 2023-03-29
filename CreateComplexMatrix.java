package com.bob.largeqr;

import android.util.Log;

import java.util.Random;

public class CreateComplexMatrix {

    public static final String TAG = MainActivity.TAG;

    public double SCALE = 32768.0D;

    private final Complex[][] Amatrix;

    public CreateComplexMatrix(int rows, int cols) {
        double[][] Areal = {{+1, +2, +3, -3}, {+2, +3, +2, -6}, {+1, +2, +3, +1}, {+3, +4, +4, +2}};
        double[][] Aimag = {{+2, -3, +4, +1}, {-3, +1, -2, -7}, {-1, -4, +2, +2}, {-1, +3, -2, +4}};

        Amatrix = new Complex[rows][cols];
        int min = -32767;
        int max = +32767;
        Random r = new Random();

        for (int i = 0; i< rows; i++) {
            for (int j = 0; j < cols; j++) {
                double ar = (min + r.nextInt(max - min)) / SCALE;
                double ai = (min + r.nextInt(max - min)) / SCALE;
                Amatrix[i][j] = new Complex(ar, ai);
                if (rows < 5 && cols < 5) {
                    Amatrix[i][j] = new Complex(Areal[i][j], Aimag[i][j]);
                }
            }
        }
    }

    public Complex[][] getMatrix() {
        return Amatrix;
    }
}
