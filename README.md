# TimeHash

[![Build Status](https://travis-ci.org/h-j-k/timehash.svg?branch=master)](https://travis-ci.org/h-j-k/timehash) 
[![codecov](https://codecov.io/gh/h-j-k/timehash/branch/master/graph/badge.svg)](https://codecov.io/gh/h-j-k/timehash)
[![Quality Gate](https://sonarqube.com/api/badges/gate?key=com.ikueb:timehash)](https://sonarqube.com/dashboard/?id=com.ikueb:timehash)
[![Technical Debt Ratio](https://sonarqube.com/api/badges/measure?key=com.ikueb:timehash&metric=sqale_debt_ratio)](https://sonarqube.com/dashboard/?id=com.ikueb:timehash)
[![Comments](https://sonarqube.com/api/badges/measure?key=com.ikueb:timehash&metric=comment_lines_density)](https://sonarqube.com/dashboard/?id=com.ikueb:timehash)

A simple utility class for hashing [`LocalDateTime`][1] values.

[Homepage](https://h-j-k.github.io/timehash)

[GitHub project page](https://github.com/h-j-k/timehash)

[Javadocs](https://h-j-k.github.io/timehash/javadoc)

# Motivation

`TimeHashUtils` provides multiple outputs for hashing a `LocalDateTime` based on the required precision.

 <table summary="Value description">
    <thead>
        <tr>
            <td>Value</td>
            <td>Period</td>
            <td>Frequency</td>
            <td>Precision compared to previous value</td>
            <td>Number of characters including <code>YMdHms</code></td>
            <td>Example</td>
            <td><code>LocalDateTime</code> value</td>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td><code>TRIM</code></td>
            <td>1 s</td>
            <td>1 Hz</td>
            <td>&nbsp;</td>
            <td>6</td>
            <td><code>7569sQ</code></td>
            <td><code>2017-01-02T03:45:06</code></td>
        </tr>
        <tr>
            <td><code>MILLIGROUP</code></td>
            <td>25 ms</td>
            <td>40 Hz</td>
            <td>40x</td>
            <td>7</td>
            <td><code>7569sQg</code></td>
            <td><code>2017-01-02T03:45:06.775</code></td>
        </tr>
        <tr>
            <td><code>MILLIS</code></td>
            <td>1 ms</td>
            <td>1 KHz</td>
            <td>25x</td>
            <td>8</td>
            <td><code>7569sQNT</code></td>
            <td><code>2017-01-02T03:45:06.789</code></td>
        </tr>
        <tr>
            <td><code>MICROGROUP</code></td>
            <td>10 μs</td>
            <td>100 KHz</td>
            <td>100x</td>
            <td>9</td>
            <td><code>7569sQkHn</code></td>
            <td><code>2017-01-02T03:45:06.789010</code></td>
        </tr>
        <tr>
            <td><code>NANOGROUP</code></td>
            <td>200 ns</td>
            <td>5 MHz</td>
            <td>50x</td>
            <td>10</td>
            <td><code>7569sQlhJn</code></td>
            <td><code>2017-01-02T03:45:06.789012200</code></td>
        </tr>
        <tr>
            <td><code>QUADNANO</code></td>
            <td>4 ns</td>
            <td>250 MHz</td>
            <td>50x</td>
            <td>11</td>
            <td><code>7569sQnCdML</code></td>
            <td><code>2017-01-02T03:45:06.789012344</code></td>
        </tr>
        <tr>
            <td><code>NANOS</code></td>
            <td>1 ns</td>
            <td>1 GHz</td>
            <td>4x</td>
            <td>12</td>
            <td><code>7569sQ78fTKF</code></td>
            <td><code>2017-01-02T03:45:06.789012345</code></td>
        </tr>
    </tbody>
 </table>

# Bugs/feedback

Please make use of the GitHub features to report any bugs, issues, or even pull requests. :)

Enjoy!

[1]: https://docs.oracle.com/javase/8/docs/api/java/time/LocalDateTime.html