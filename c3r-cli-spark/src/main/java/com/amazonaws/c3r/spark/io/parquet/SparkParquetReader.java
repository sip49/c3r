// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazonaws.c3r.spark.io.parquet;

import com.amazonaws.c3r.config.ColumnHeader;
import com.amazonaws.c3r.exception.C3rRuntimeException;
import com.amazonaws.c3r.internal.Limits;
import lombok.NonNull;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utility class for Spark to read Parquet files from disk.
 */
public abstract class SparkParquetReader {
    /**
     * Maximum number of columns allowed.
     */
    static final int MAX_COLUMN_COUNT = 10000;

    /**
     * Reads the input file for processing, normalizing headers.
     *
     * @param sparkSession The spark session to read with
     * @param source       Location of input data
     * @return The source data to be processed
     */
    public static Dataset<Row> readInput(@NonNull final SparkSession sparkSession,
                                         @NonNull final String source) {
        return readInput(sparkSession, source, false);
    }

    /**
     * Reads the input file for processing, optionally normalizing headers.
     *
     * @param sparkSession            The spark session to read with
     * @param source                  Location of input data
     * @param skipHeaderNormalization Whether to skip the normalization of read in headers
     * @return The source data to be processed
     */
    public static Dataset<Row> readInput(@NonNull final SparkSession sparkSession,
                                         @NonNull final String source,
                                         final boolean skipHeaderNormalization) {
        final Map<String, String> options = new HashMap<>();
        Dataset<Row> dataset = sparkSession.read().options(options).parquet(source);
        if (!skipHeaderNormalization) {
            final Map<String, String> renameMap = Arrays.stream(dataset.columns())
                    .collect(Collectors.toMap(Function.identity(), c -> new ColumnHeader(c).toString()));
            dataset = dataset.withColumnsRenamed(renameMap);
        }
        validate(dataset);
        return dataset;
    }

    /**
     * Validate that the dataset is within the required limits.
     *
     * @param dataset The dataset to validate
     * @throws C3rRuntimeException If the dataset exceeds any limits.
     */
    static void validate(final Dataset<Row> dataset) {
        if (dataset.columns().length > MAX_COLUMN_COUNT) {
            throw new C3rRuntimeException("Couldn't parse input file. Please verify that column count does not exceed "
                    + MAX_COLUMN_COUNT + ".");
        }
        if (dataset.count() > Limits.ROW_COUNT_MAX) {
            throw new C3rRuntimeException("A table cannot contain more than " + Limits.ROW_COUNT_MAX + " rows.");
        }
    }
}
