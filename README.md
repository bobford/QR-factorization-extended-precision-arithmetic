# QR-factorization-extended-precision-arithmetic

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
