package com.bob.largeqr;

/*
        This QR factorization of a complex matrix, at the class level, is modelled after an AI version.
        ChatGPT was asked for this but produced a QR factorization for a real matrix instead,
        illustrating the important concept that AI doesn't know what it doesn't know.

        The actual Java code is a conversion of the assembly code from an implementation on an Arm processor
        originally written by Bob Ford.
		
        The computation of the Householder vector follows the definition given in:
            http://arith.cs.ucla.edu/publications/House-Asil06.pdf

		This implementation seems particularly inefficient as it runs about 30 times slower than the
		assembly language version on an Armv8 processor. This may be due to the use of the Complex class.
		Specifically, on a 192x120 matrix, the assembly version takes 3.5 milliseconds while this Java version
		takes about 100 milliseconds, both on an older Android phone. As a comparison, similar code for a
		real matrix only differs by a factor of 2.5-3.

		Calling sequence:
		    Complex[][] z;                      // input matrix, m x n
		    QRfactorizationComplex qr = new QRfactorizationComplex(z);
		    Complex[][] r = qr.getR();          // output matrix will be n x n

 */

public class QRfactorizationComplex {

    private final int m, n, p;
    private final Complex[][] QR;

    public QRfactorizationComplex(Complex[][] A) {
        m = A.length;
        n = A[0].length;
        if (n == m) p = n-1;
        else p = n;
        QR = new Complex[m][n];
        for (int i = 0; i < m; i++) {
            System.arraycopy(A[i], 0, QR[i], 0, n);
        }
        factorize();
    }
    private void factorize() {
        for (int k=0; k<p; k++) {
            Complex[] x = new Complex[m - k];                           // x vector is a sub column of A
            for (int i = k; i < m; i++) {
                x[i - k] = QR[i][k];
            }
            double xnorm = norm(x);                                     // norm of x[]

            double x0mag = Math.sqrt(x[0].getReal()*x[0].getReal() + x[0].getImag()*x[0].getImag());

            Complex[] w = new Complex[m - k];

            x[0] = x[0].times(1. + xnorm / x0mag);
            System.arraycopy(x, 0, w, 0, x.length);

            xnorm = 1./norm(w);

            for (int i = 0; i < w.length; i++) {                        // w vector is normalized, this is the Householder vector
                w[i] = w[i].times(xnorm);
            }

            double beta = 2.0;
//          we need w conjugate, i.e., Hermitian
            Complex[] wconjugate = new Complex[w.length];
            for (int i=0; i<w.length; i++) {
                wconjugate[i] = new Complex(w[i].getReal(), -w[i].getImag());
            }
			
            for (int j = k; j < n; j++) {
                Complex s = new Complex();
                for (int i = k; i < m; i++) {
                    s.plusEquals(QR[i][j].times(wconjugate[i - k]));
                }
                Complex alpha = s.times(beta);
                for (int i = k; i < m; i++) {
                    QR[i][j].minusEquals(alpha.times(w[i - k]));
                }
            }
        }
    }

    public Complex[][] getR() {
        Complex[][] R = new Complex[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i > j) {
                    R[i][j] = new Complex();
                } else {
                    R[i][j] = QR[i][j];
                }
            }
        }
        return R;
    }

    private double norm(Complex[] x) {
        double result = 0;
        for (Complex complex : x) {
            double re = complex.getReal();
            double im = complex.getImag();
            result += re * re + im * im;
        }
        return Math.sqrt(result);
    }

}
