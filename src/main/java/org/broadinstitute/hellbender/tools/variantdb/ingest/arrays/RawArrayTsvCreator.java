package org.broadinstitute.hellbender.tools.variantdb.ingest.arrays;
import htsjdk.variant.variantcontext.VariantContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.broadinstitute.hellbender.engine.FeatureContext;
import org.broadinstitute.hellbender.engine.ReadsContext;
import org.broadinstitute.hellbender.engine.ReferenceContext;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.tools.variantdb.SchemaUtils;
import org.broadinstitute.hellbender.tools.variantdb.ingest.IngestConstants;
import org.broadinstitute.hellbender.utils.tsv.SimpleXSVWriter;


import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class RawArrayTsvCreator {
    static final Logger logger = LogManager.getLogger(RawArrayTsvCreator.class);

    private SimpleXSVWriter rawArrayWriter = null;
    private final String sampleId;

    enum GT_encoding {
        HOM_REF("null"),
        HET("X"),
        HOM_VAR("V"),
        HET_NON_REF("T"),
        MISSING("U");

        String value;
        GT_encoding(String v) {
            value = v;
        }
        String getValue() {
            return value;
        }
    }

    public RawArrayTsvCreator(String sampleName, String sampleId, Path sampleDirectoryPath) {
        this.sampleId = sampleId;
        // If the raw directory inside it doesn't exist yet -- create it
        final String rawDirectoryName = "raw";
        final Path rawDirectoryPath = sampleDirectoryPath.resolve(rawDirectoryName);
        final File rawDirectory = new File(rawDirectoryPath.toString());
        if (!rawDirectory.exists()) {
            rawDirectory.mkdir();
        }
        try {
            // Create a raw file to go into the raw dir for _this_ sample
            final String rawOutputName = sampleName + rawDirectoryName + IngestConstants.FILETYPE;
            final Path rawOutputPath = rawDirectoryPath.resolve(rawOutputName);
            // write header to it
            List<String> rawHeader = RawArrayTsvCreator.getHeaders();
            rawArrayWriter = new SimpleXSVWriter(rawOutputPath, IngestConstants.SEPARATOR);
            rawArrayWriter.setHeaderLine(rawHeader);
        } catch (final IOException e) {
            throw new UserException("Could not create raw outputs", e);
        }
    }

    public List<String> createRow(final VariantContext variant, final String sampleId) {
        List<String> row = new ArrayList<>();
        row.add(sampleId);
        for ( final RawArrayFieldEnum fieldEnum : RawArrayFieldEnum.values() ) {
            if (!fieldEnum.equals(RawArrayFieldEnum.sample_id)) {
                row.add(fieldEnum.getColumnValue(variant));
            }
        }
        return row;
    }

    public static List<String> getHeaders() {
        return Arrays.stream(RawArrayFieldEnum.values()).map(String::valueOf).collect(Collectors.toList());
    }

    public List<List<String>> createRows(long start, long end, VariantContext variant, String sampleId) {
        // this doesn't apply to arrays
        throw new UserException.UnimplementedFeature("Not implemented.");
    }

    public void apply(final VariantContext variant, final ReadsContext readsContext, final ReferenceContext referenceContext, final FeatureContext featureContext) {
        if (!variant.getFilters().contains("ZEROED_OUT_ASSAY")) {
            final List<String> TSVLinesToCreate = createRow(variant, sampleId);

            // write the row to the XSV
            SimpleXSVWriter.LineBuilder rawLine = rawArrayWriter.getNewLineBuilder();
            rawLine.setRow(TSVLinesToCreate);
            rawLine.write();
        }
    }

    public void closeTool() {
        if (rawArrayWriter != null) {
            try {
                rawArrayWriter.close();
            } catch (final Exception e) {
                throw new IllegalArgumentException("Couldn't close RAW array writer", e);
            }
        }
    }
}