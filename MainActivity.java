package com.bob.largeqr;

/*
        This app uses high precision arithmetic in software to implement a QR factorization of a complex matrix.
        The high precision arithmetic software library is from:
            http://www.apfloat.org/apfloat_java/
        The computation of the Householder vector follows the definition given in:
            http://arith.cs.ucla.edu/publications/House-Asil06.pdf
        While this does give the correct answer, compared to a simple Java version, I don't consider it particularly
        useful because of the time consumed in the software implementation of arithmetic operations. For example,
        running on a 2019 variety mobile phone the execution times for a 192x120 matrix are:
            extended precision:         60 seconds
            Java double precision:      100 milliseconds
            Arm64 assembly:             3.5 milliseconds
        If the matrices were very large, where 64 bit floating point was inadequate, a parallel implementation
        would be necessary.
        These results are in contrast to a backsolve operation where complex matrices as small as 64x64 start to
        benefit and are required at size 128x128.

        The Complex class is from:
 *
 *   WRITTEN BY: Dr Michael Thomas Flanagan
 *
 *   DATE:    February 2002
 *   UPDATED: 1 August 2006, 29 April 2007, 15,21,22 June 2007, 22 November 2007
 *            20 May 2008, 26 August 2008, 9 November 2009, 6 june 2010
 *
 *   DOCUMENTATION:
 *   See Michael T Flanagan's Java library on-line web pages:
 *   http://www.ee.ucl.ac.uk/~mflanaga/java/Complex.html
 *   http://www.ee.ucl.ac.uk/~mflanaga/java/
 *
 *   Copyright (c) 2002 - 2009    Michael Thomas Flanagan
 *
 */

import static org.apfloat.ApfloatMath.sqrt;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.apfloat.Apcomplex;
import org.apfloat.Apfloat;
import org.apfloat.ApfloatRuntimeException;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "bob";

    private TextView tv;
    private ProgressBar spinner;

    public static final int APFLOAT_PRECISION = 100;                // arbitrary, choose your own value

    public static final int rows = 192;
    public static final int cols = 120;
    CreateComplexMatrix src;                                        // simple A matrix as Complex[][]

    private boolean ready = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (ready) {
            initialize();
            process();
        } else {
            Log.d(TAG, "sleeping");
        }
    }

    private void initialize() {
        tv = findViewById(R.id.tv);
        tv.setText(getResources().getString(R.string.instructions));
        tv.append("Matrix size is " + rows + "x" + cols + " complex elements.\n");
        spinner = findViewById(R.id.spinner);
        spinner.setVisibility(View.INVISIBLE);
    }

    private void process() {

//      create a large complex matrix for testing
        src = new CreateComplexMatrix(rows, cols);

        doSomeTaskAsync(rows, cols);

    }

    Apcomplex[] householderVector(Apcomplex[] x) {
        Apfloat one = new Apfloat(1.0d, APFLOAT_PRECISION);

        Apfloat xnorm = norm(x);
        Apfloat x0mag = sqrt(x[0].multiply(x[0].conj()).real());            // magnitude of first element of vector

        Apcomplex[] w = new Apcomplex[x.length];
        try {
            x[0] = x[0].multiply(one.add(xnorm.divide(x0mag)));
        } catch (ArithmeticException | ApfloatRuntimeException e) {
            Log.d(TAG, "div exception " + x.length);
        }

        System.arraycopy(x, 0, w, 0, w.length);

        Apfloat wnorm = one.divide(norm(w));

        for (int i = 0; i < w.length; i++) {                        // w vector is normalized, this is the Householder vector
            w[i] = w[i].multiply(wnorm);
        }

        return w;
    }

    Apfloat norm(Apcomplex[] x) {
        Apfloat xnorm = new Apfloat(0.0d, APFLOAT_PRECISION);
        for (int i=0; i<x.length; i++) {
            xnorm = xnorm.add(x[i].multiply(x[i].conj()).real());
        }
        xnorm = sqrt(xnorm);
        return xnorm;
    }


    private void postProcess(Apcomplex[][] QR) {
        Log.d(TAG, "results of QR:");
        int m = QR.length;
        int n = QR[0].length;                                       // this is m x n
        long start, end;

        Complex[][] z = src.getMatrix();
        start = System.currentTimeMillis();
        QRfactorizationComplex qr = new QRfactorizationComplex(z);
        Complex[][] ans = qr.getR();                                // this will be n x n
        end = System.currentTimeMillis();
        tv.append("Java time is " + (end-start) + " milliseconds.\n");

        double norm = 0., ar, ai;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                ar = ans[i][j].getReal() - QR[i][j].real().doubleValue();
                ai = ans[i][j].getImag() - QR[i][j].imag().doubleValue();
                norm += ar*ar + ai*ai;
            }
        }
        tv.append("norm of difference from Java version is " + norm + "\n");

    }

    private void doSomeTaskAsync(int m, int n) {
        Activity currActivity = MainActivity.this;
        spinner.setVisibility(View.VISIBLE);
        new BackgroundTask(currActivity) {

            String allDone;
            final Complex[][] qr = src.getMatrix();
            final Apcomplex[][] QR = new Apcomplex[m][n];
            long executionTime;
            @Override
            public void doInBackground() {

                long start, end;
//              this converts the Complex[][] source matrix to extended precision form
                for (int i=0; i<m; i++) {
                    for (int j=0; j<n; j++) {
                        QR[i][j] = new Apcomplex(new Apfloat(qr[i][j].getReal(), APFLOAT_PRECISION), new Apfloat(qr[i][j].getImag(), APFLOAT_PRECISION));
                    }
                }

                start = System.currentTimeMillis();

                Log.d(TAG, "working on it");
                int p = n;
                if (n == m) p = n-1;
                Apfloat beta = new Apfloat(2.0d, APFLOAT_PRECISION);

                for (int k=0; k<p; k++) {
                    Apcomplex[] column = new Apcomplex[m-k];
                    for (int i=0; i<m-k; i++) {
                        column[i] = QR[i + k] [k];
                    }
                    Apcomplex[] w = householderVector(column);
                    for (int j = k; j < n; j++) {
                        Apcomplex s = new Apcomplex(new Apfloat(0.0d, APFLOAT_PRECISION));
                        for (int i = k; i < m; i++) {
                            s = s.add(QR[i][j].multiply(w[i - k].conj()));
                        }
                        Apcomplex alpha = s.multiply(beta);
                        for (int i = k; i < m; i++) {
                            QR[i][j] = QR[i][j].subtract(alpha.multiply(w[i - k]));
                        }
                    }
                }

                end = System.currentTimeMillis();
                executionTime = end - start;
                Log.d(TAG, "finished in " + executionTime + " milliseconds");

                allDone = "Thank you for your attention.\n";

            }

            @Override
            public void onPostExecute() {
                spinner.setVisibility(View.INVISIBLE);
                tv.append(executionTime + " milliseconds for " + m + "x" + n + " complex matrix using extended precision.\n");
                tv.append(allDone);
                postProcess(QR);
            }
        }.execute();
    }

    public void logPrint(Complex[][] m) {
        logPrint(m, "");
    }
    public void logPrint(Complex[][] mat, String header) {
        int m = mat.length;
        int n = mat[0].length;
        if (!header.equals("")) {
            Log.d(TAG, header);
        }
        if (m == 1) {
            Log.d(TAG, "row = 0" + "   " + mat[0][0].getReal() + " +i*" + mat[0][0].getImag());
        } else if (m == 2) {
            for (int i=0; i<m; i++) {
                switch (n) {
                    case 1:
                        Log.d(TAG, "row = " + i + "   " + mat[i][0].getReal() + " +i*" + mat[i][0].getImag());
                        break;
                    case 2:
                        Log.d(TAG, "row = " + i + "   " + mat[i][0].getReal() + " +i*" + mat[i][0].getImag() + "   " + mat[i][1].getReal() + " +i*" + mat[i][1].getImag());
                        break;
                }
            }
        } else if (m == 3) {
            for (int i=0; i<m; i++) {
                switch (n) {
                    case 1:
                        Log.d(TAG, "row = " + i + "   " + mat[i][0].getReal() + " +i*" + mat[i][0].getImag());
                        break;
                    case 2:
                        Log.d(TAG, "row = " + i + "   " + mat[i][0].getReal() + " +i*" + mat[i][0].getImag() + "   " + mat[i][1].getReal() + " +i*" + mat[i][1].getImag());
                        break;
                    case 3:
                        Log.d(TAG, "row = " + i + "   " + mat[i][0].getReal() + " +i*" + mat[i][0].getImag() + "   " + mat[i][1].getReal() + " +i*" + mat[i][1].getImag() + "   " + mat[i][2].getReal() + " +i*" + mat[i][2].getImag());
                        break;
                }
            }
        } else {
            for (int i=0; i<4; i++) {
                switch (n) {
                    case 1:
                        Log.d(TAG, "row = " + i + "   " + mat[i][0].getReal() + " +i*" + mat[i][0].getImag());
                        break;
                    case 2:
                        Log.d(TAG, "row = " + i + "   " + mat[i][0].getReal() + " +i*" + mat[i][0].getImag() + "   " + mat[i][1].getReal() + " +i*" + mat[i][1].getImag());
                        break;
                    case 3:
                        Log.d(TAG, "row = " + i + "   " + mat[i][0].getReal() + " +i*" + mat[i][0].getImag() + "   " + mat[i][1].getReal() + " +i*" + mat[i][1].getImag() + "   " + mat[i][2].getReal() + " +i*" + mat[i][2].getImag());
                        break;
                    default:
                        Log.d(TAG, "row = " + i + "   " + mat[i][0].getReal() + " +i*" + mat[i][0].getImag() + "   " + mat[i][1].getReal() + " +i*" + mat[i][1].getImag() + "   " + mat[i][2].getReal() + " +i*" + mat[i][2].getImag() + "   " + mat[i][3].getReal() + " +i*" + mat[i][3].getImag());
                        break;
                }
            }
        }
    }

}