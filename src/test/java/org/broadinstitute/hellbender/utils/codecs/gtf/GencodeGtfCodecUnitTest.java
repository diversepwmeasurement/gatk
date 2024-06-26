package org.broadinstitute.hellbender.utils.codecs.gtf;

import htsjdk.tribble.Tribble;
import htsjdk.tribble.annotation.Strand;
import htsjdk.tribble.index.Index;
import htsjdk.tribble.index.IndexFactory;
import htsjdk.tribble.readers.LineIterator;
import org.broadinstitute.hellbender.GATKBaseTest;
import org.broadinstitute.hellbender.engine.FeatureDataSource;
import org.broadinstitute.hellbender.utils.SimpleInterval;
import org.broadinstitute.hellbender.utils.io.IOUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;


/**
 * Test class for the GENCODE GTF Reader ({@link GencodeGtfCodec}).
 * Modeled after the {@link org.broadinstitute.hellbender.utils.codecs.table.TableCodecUnitTest}, with extras specific to this file format.
 * Created by jonn on 7/27/17.
 */
public class GencodeGtfCodecUnitTest extends GATKBaseTest {

    private static final String testResourceDir = publicTestDir + "org/broadinstitute/hellbender/utils/codecs/gtf/";
    private static final String eColiTestDir = publicTestDir + "org/broadinstitute/hellbender/tools/funcotator/ecoli_ds/gencode/ASM584v2/";
    private static final String xyzTestFile = largeFileTestDir + "gencode.v26.primary_assembly.annotation.XYZ.gtf";
    private static final String gencodeHg19TestFile = largeFileTestDir + "gencode.v19.LargeFile.gtf";
    private static final String gencodeHG38V43TestFile = largeFileTestDir + "gencode.v43.LargeFile.gtf";

    private static final String BIOTYPE_TEMPLATE_PLACEHOLDER = "<BIO_TYPE>";

    /**
     * Checks the given feature and the given start and end positions for whether the regions overlap.
     * @param feature {@link GencodeGtfFeature} to use for overlap checking
     * @param contig Contig to check for overlap
     * @param start Interval start position to check for overlap
     * @param end Interval end position to check for overlap
     * @return {@code true} if the region in the {@link GencodeGtfFeature} and the given interval overlap, {@code false} otherwise.
     */
    private boolean checkForOverlap(final GencodeGtfFeature feature,
                                    final String contig,
                                    final int start,
                                    final int end) {

        final SimpleInterval other = new SimpleInterval(contig, start, end);
        return other.overlaps(feature);
    }

    /**
     * Tests that a given query in a file returns the correct number of results
     * @param contig Contiguous region in the genome in which to search.
     * @param start Start position within {@code contig} in which to search.
     * @param end End position within {@code contig} in which to search.
     * @param numExpectedGenes The number of expected results from the query.
     * @param testFile A GENCODE GTF {@link File} to query against.
     */
    private void testIndexHelper(final String contig, final int start, final int end, final int numExpectedGenes, final File testFile) {
        // Now we do our queries:
        try (final FeatureDataSource<GencodeGtfFeature> featureDataSource = new FeatureDataSource<>(testFile) )
        {
            final Iterator<GencodeGtfFeature> it = featureDataSource.query( new SimpleInterval(contig, start, end) );

            int geneCount = 0;

            while ( it.hasNext() ) {

                final GencodeGtfFeature feature = it.next();

                // Verify the bounds:
                Assert.assertTrue( checkForOverlap(feature, contig, start, end) );

                // Keep track of how many genes we've seen:
                ++geneCount;
            }

            Assert.assertEquals( geneCount, numExpectedGenes );
        }
    }

    /**
     * Creates a valid {@link GencodeGtfGeneFeature} that corresponds to the data in the file {@code gencode.valid1.gtf}
     * @return a {@link GencodeGtfGeneFeature} representing the data in the file {@code gencode.valid1.gtf}
     */
    private GencodeGtfGeneFeature createGencodeGtfGene_gencode_valid1() {

        // Create the Features as they exist in the test file:
        GencodeGtfFeatureBaseData data;

        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 6, "chr1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.GENE,
                30366, 30503, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000284332.1", null, GencodeGTFFieldConstants.KnownGeneBiotype.MIRNA.toString(),
                null, "MIR1302-2", null, null, null, -1, null, GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(), null);
        final GencodeGtfGeneFeature gene = (GencodeGtfGeneFeature)GencodeGtfFeature.create(data);

        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 7, "chr1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.TRANSCRIPT,
                30366, 30503, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000284332.1", "ENST00000607096.1", GencodeGTFFieldConstants.KnownGeneBiotype.MIRNA.toString(),
                null, "MIR1302-2", GencodeGTFFieldConstants.KnownGeneBiotype.MIRNA.toString(), null, "MIR1302-2-201", -1, null, GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.NA.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfTranscriptFeature transcript = (GencodeGtfTranscriptFeature) GencodeGtfFeature.create(data);

        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 8, "chr1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                30366, 30503, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000284332.1", "ENST00000607096.1", GencodeGTFFieldConstants.KnownGeneBiotype.MIRNA.toString(),
                null, "MIR1302-2", GencodeGTFFieldConstants.KnownGeneBiotype.MIRNA.toString(), null, "MIR1302-2-201", 1, "ENSE00003695741.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.NA.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfExonFeature exon = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);

        // ======================
        // Here we'll set the version number of each feature.
        // Normally this is set in the decode method, so we need to do it manually.

        exon.setUcscGenomeVersion("hg38");
        transcript.setUcscGenomeVersion("hg38");
        gene.setUcscGenomeVersion("hg38");

        // Aggregate the Features as they should be:
        transcript.addExon(exon);
        gene.addTranscript(transcript);

        return gene;
    }

    /**
     * Creates a valid {@link GencodeGtfGeneFeature} that corresponds to the data in the file {@code gencode.valid_gencode_file2.gtf}
     * @return a {@link GencodeGtfGeneFeature} representing the data in the file {@code gencode.valid_gencode_file2.gtf}
     */
    private GencodeGtfGeneFeature createGencodeGtfGene_gencode_valid_gencode_file2() {

        // Let's define all our features up front:
        GencodeGtfFeatureBaseData data;

        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 6, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.GENE,
                50200979, 50217616, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.13", null, GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", null, null, null, -1, null, GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Collections.singletonList(
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3")
                        )
                )
                );
        final GencodeGtfGeneFeature gene1 = (GencodeGtfGeneFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 7, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.TRANSCRIPT,
                50200979, 50217615, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.13", "ENST00000611222.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-201", -1, null, GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000483593.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.NO_SINGLE_TRANSCRIPT_SUPPORT.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_ALTERNATIVE_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3")
                        )
                )
                );
        final GencodeGtfTranscriptFeature transcript1 = (GencodeGtfTranscriptFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 8, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                50200979, 50201590, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.13", "ENST00000611222.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-201", 1, "ENSE00001541223.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000483593.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.NO_SINGLE_TRANSCRIPT_SUPPORT.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_ALTERNATIVE_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3")
                        )
                )
                );
        final GencodeGtfExonFeature exon1 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 9, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.CDS,
                50201037, 50201590, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.ZERO, "ENSG00000073169.13", "ENST00000611222.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-201", 1, "ENSE00001541223.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000483593.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.NO_SINGLE_TRANSCRIPT_SUPPORT.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_ALTERNATIVE_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3")
                        )
                )
                );
        final GencodeGtfCDSFeature cds1 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 10, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.START_CODON,
                50201037, 50201039, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.ZERO, "ENSG00000073169.13", "ENST00000611222.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-201", 1, "ENSE00001541223.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000483593.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.NO_SINGLE_TRANSCRIPT_SUPPORT.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_ALTERNATIVE_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3")
                        )
                )
                );
        final GencodeGtfStartCodonFeature start_codon1 = (GencodeGtfStartCodonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 11, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                50206317, 50206520, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.13", "ENST00000611222.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-201", 2, "ENSE00001129529.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000483593.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.NO_SINGLE_TRANSCRIPT_SUPPORT.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_ALTERNATIVE_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3")
                        )
                )
                );
        final GencodeGtfExonFeature exon2 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 12, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.CDS,
                50206317, 50206520, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.ONE, "ENSG00000073169.13", "ENST00000611222.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-201", 2, "ENSE00001129529.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000483593.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.NO_SINGLE_TRANSCRIPT_SUPPORT.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_ALTERNATIVE_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3")
                        )
                )
                );
        final GencodeGtfCDSFeature cds2 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 13, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                50208536, 50208716, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.13", "ENST00000611222.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-201", 3, "ENSE00001129524.2", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000483593.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.NO_SINGLE_TRANSCRIPT_SUPPORT.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_ALTERNATIVE_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3")
                        )
                )
                );
        final GencodeGtfExonFeature exon3 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 14, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.CDS,
                50208536, 50208716, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.ONE, "ENSG00000073169.13", "ENST00000611222.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-201", 3, "ENSE00001129524.2", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000483593.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.NO_SINGLE_TRANSCRIPT_SUPPORT.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_ALTERNATIVE_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3")
                        )
                )
                );
        final GencodeGtfCDSFeature cds3 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 15, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                50210181, 50210311, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.13", "ENST00000611222.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-201", 4, "ENSE00003473644.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000483593.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.NO_SINGLE_TRANSCRIPT_SUPPORT.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_ALTERNATIVE_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3")
                        )
                )
                );
        final GencodeGtfExonFeature exon4 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 16, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.CDS,
                50210181, 50210311, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.ZERO, "ENSG00000073169.13", "ENST00000611222.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-201", 4, "ENSE00003473644.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000483593.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.NO_SINGLE_TRANSCRIPT_SUPPORT.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_ALTERNATIVE_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3")
                        )
                )
                );
        final GencodeGtfCDSFeature cds4 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 17, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                50210631, 50210911, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.13", "ENST00000611222.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-201", 5, "ENSE00003503715.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000483593.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.NO_SINGLE_TRANSCRIPT_SUPPORT.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_ALTERNATIVE_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3")
                        )
                )
                );
        final GencodeGtfExonFeature exon5 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 18, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.CDS,
                50210631, 50210911, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.ONE, "ENSG00000073169.13", "ENST00000611222.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-201", 5, "ENSE00003503715.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000483593.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.NO_SINGLE_TRANSCRIPT_SUPPORT.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_ALTERNATIVE_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3")
                        )
                )
                );
        final GencodeGtfCDSFeature cds5 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 19, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                50215717, 50215867, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.13", "ENST00000611222.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-201", 6, "ENSE00003573348.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000483593.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.NO_SINGLE_TRANSCRIPT_SUPPORT.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_ALTERNATIVE_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3")
                        )
                )
                );
        final GencodeGtfExonFeature exon6 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 20, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.CDS,
                50215717, 50215867, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.TWO, "ENSG00000073169.13", "ENST00000611222.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-201", 6, "ENSE00003573348.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000483593.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.NO_SINGLE_TRANSCRIPT_SUPPORT.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_ALTERNATIVE_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3")
                        )
                )
                );
        final GencodeGtfCDSFeature cds6 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 21, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                50216691, 50216876, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.13", "ENST00000611222.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-201", 7, "ENSE00003510005.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000483593.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.NO_SINGLE_TRANSCRIPT_SUPPORT.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_ALTERNATIVE_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3")
                        )
                )
                );
        final GencodeGtfExonFeature exon7 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 22, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.CDS,
                50216691, 50216876, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.ONE, "ENSG00000073169.13", "ENST00000611222.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-201", 7, "ENSE00003510005.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000483593.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.NO_SINGLE_TRANSCRIPT_SUPPORT.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_ALTERNATIVE_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3")
                        )
                )
                );
        final GencodeGtfCDSFeature cds7 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 23, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                50216972, 50217128, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.13", "ENST00000611222.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-201", 8, "ENSE00003591346.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000483593.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.NO_SINGLE_TRANSCRIPT_SUPPORT.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_ALTERNATIVE_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3")
                        )
                )
                );
        final GencodeGtfExonFeature exon8 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 24, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.CDS,
                50216972, 50217128, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.ONE, "ENSG00000073169.13", "ENST00000611222.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-201", 8, "ENSE00003591346.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000483593.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.NO_SINGLE_TRANSCRIPT_SUPPORT.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_ALTERNATIVE_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3")
                        )
                )
                );
        final GencodeGtfCDSFeature cds8 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 25, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                50217205, 50217357, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.13", "ENST00000611222.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-201", 9, "ENSE00003728455.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000483593.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.NO_SINGLE_TRANSCRIPT_SUPPORT.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_ALTERNATIVE_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3")
                        )
                )
                );
        final GencodeGtfExonFeature exon9 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 26, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.CDS,
                50217205, 50217357, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.ZERO, "ENSG00000073169.13", "ENST00000611222.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-201", 9, "ENSE00003728455.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000483593.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.NO_SINGLE_TRANSCRIPT_SUPPORT.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_ALTERNATIVE_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3")
                        )
                )
                );
        final GencodeGtfCDSFeature cds9 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 27, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                50217361, 50217615, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.13", "ENST00000611222.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-201", 10, "ENSE00003739808.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000483593.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.NO_SINGLE_TRANSCRIPT_SUPPORT.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_ALTERNATIVE_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3")
                        )
                )
                );
        final GencodeGtfExonFeature exon10 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 28, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.CDS,
                50217361, 50217366, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.ZERO, "ENSG00000073169.13", "ENST00000611222.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-201", 10, "ENSE00003739808.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000483593.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.NO_SINGLE_TRANSCRIPT_SUPPORT.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_ALTERNATIVE_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3")
                        )
                )
                );
        final GencodeGtfCDSFeature cds10 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 29, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.STOP_CODON,
                50217367, 50217369, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.ZERO, "ENSG00000073169.13", "ENST00000611222.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-201", 10, "ENSE00003739808.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000483593.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.NO_SINGLE_TRANSCRIPT_SUPPORT.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_ALTERNATIVE_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3")
                        )
                )
                );
        final GencodeGtfStopCodonFeature stop_codon1 = (GencodeGtfStopCodonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 30, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.UTR,
                50200979, 50201036, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.13", "ENST00000611222.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-201", 1, "ENSE00001541223.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000483593.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.NO_SINGLE_TRANSCRIPT_SUPPORT.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_ALTERNATIVE_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3")
                        )
                )
                );
        final GencodeGtfUTRFeature utr1 = (GencodeGtfUTRFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 31, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.UTR,
                50217367, 50217615, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.13", "ENST00000611222.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-201", 10, "ENSE00003739808.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000483593.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.NO_SINGLE_TRANSCRIPT_SUPPORT.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_ALTERNATIVE_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3")
                        )
                )
                );
        final GencodeGtfUTRFeature utr2 = (GencodeGtfUTRFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 32, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.TRANSCRIPT,
                50200979, 50217616, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.13", "ENST00000380903.6", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-001", -1, null, GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000370288.2"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfTranscriptFeature transcript2 = (GencodeGtfTranscriptFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 33, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.SELENOCYSTEINE,
                50217358, 50217360, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.13", "ENST00000380903.6", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-001", -1, null, GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000370288.2"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfSelenocysteineFeature selenocysteine1 = (GencodeGtfSelenocysteineFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 34, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.EXON,
                50200979, 50201590, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.13", "ENST00000380903.6", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-001", 1, "ENSE00001541223.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000370288.2"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfExonFeature exon11 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 35, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.CDS,
                50201037, 50201590, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.ZERO, "ENSG00000073169.13", "ENST00000380903.6", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-001", 1, "ENSE00001541223.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000370288.2"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfCDSFeature cds11 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 36, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.START_CODON,
                50201037, 50201039, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.ZERO, "ENSG00000073169.13", "ENST00000380903.6", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-001", 1, "ENSE00001541223.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000370288.2"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfStartCodonFeature start_codon2 = (GencodeGtfStartCodonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 37, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.EXON,
                50206317, 50206520, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.13", "ENST00000380903.6", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-001", 2, "ENSE00001129529.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000370288.2"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfExonFeature exon12 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 38, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.CDS,
                50206317, 50206520, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.ONE, "ENSG00000073169.13", "ENST00000380903.6", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-001", 2, "ENSE00001129529.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000370288.2"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfCDSFeature cds12 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 39, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.EXON,
                50208536, 50208716, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.13", "ENST00000380903.6", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-001", 3, "ENSE00001129524.2", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000370288.2"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfExonFeature exon13 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 40, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.CDS,
                50208536, 50208716, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.ONE, "ENSG00000073169.13", "ENST00000380903.6", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-001", 3, "ENSE00001129524.2", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000370288.2"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfCDSFeature cds13 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 41, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.EXON,
                50210181, 50210311, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.13", "ENST00000380903.6", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-001", 4, "ENSE00003473644.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000370288.2"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfExonFeature exon14 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 42, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.CDS,
                50210181, 50210311, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.ZERO, "ENSG00000073169.13", "ENST00000380903.6", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-001", 4, "ENSE00003473644.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000370288.2"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfCDSFeature cds14 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 43, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.EXON,
                50210631, 50210911, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.13", "ENST00000380903.6", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-001", 5, "ENSE00003503715.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000370288.2"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfExonFeature exon15 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 44, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.CDS,
                50210631, 50210911, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.ONE, "ENSG00000073169.13", "ENST00000380903.6", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-001", 5, "ENSE00003503715.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000370288.2"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfCDSFeature cds15 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 45, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.EXON,
                50215717, 50215867, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.13", "ENST00000380903.6", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-001", 6, "ENSE00003573348.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000370288.2"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfExonFeature exon16 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 46, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.CDS,
                50215717, 50215867, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.TWO, "ENSG00000073169.13", "ENST00000380903.6", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-001", 6, "ENSE00003573348.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000370288.2"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfCDSFeature cds16 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 47, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.EXON,
                50216691, 50216876, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.13", "ENST00000380903.6", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-001", 7, "ENSE00003510005.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000370288.2"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfExonFeature exon17 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 48, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.CDS,
                50216691, 50216876, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.ONE, "ENSG00000073169.13", "ENST00000380903.6", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-001", 7, "ENSE00003510005.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000370288.2"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfCDSFeature cds17 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 49, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.EXON,
                50216972, 50217128, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.13", "ENST00000380903.6", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-001", 8, "ENSE00003591346.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000370288.2"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfExonFeature exon18 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 50, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.CDS,
                50216972, 50217128, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.ONE, "ENSG00000073169.13", "ENST00000380903.6", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-001", 8, "ENSE00003591346.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000370288.2"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfCDSFeature cds18 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 51, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.EXON,
                50217205, 50217616, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.13", "ENST00000380903.6", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-001", 9, "ENSE00003512975.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000370288.2"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfExonFeature exon19 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 52, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.CDS,
                50217205, 50217366, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.ZERO, "ENSG00000073169.13", "ENST00000380903.6", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-001", 9, "ENSE00003512975.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000370288.2"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfCDSFeature cds19 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 53, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.STOP_CODON,
                50217367, 50217369, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.ZERO, "ENSG00000073169.13", "ENST00000380903.6", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-001", 9, "ENSE00003512975.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000370288.2"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfStopCodonFeature stop_codon2 = (GencodeGtfStopCodonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 54, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.UTR,
                50200979, 50201036, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.13", "ENST00000380903.6", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-001", 1, "ENSE00001541223.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000370288.2"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfUTRFeature utr3 = (GencodeGtfUTRFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 55, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.UTR,
                50217367, 50217616, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.13", "ENST00000380903.6", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "SELENOO-001", 9, "ENSE00003512975.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000370288.2"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL_2.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfUTRFeature utr4 = (GencodeGtfUTRFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 56, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.TRANSCRIPT,
                50206442, 50217616, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.13", "ENST00000492092.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROCESSED_TRANSCRIPT.toString(), null, "SELENOO-002", -1, null, GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000316993.1")
                        )
                )
                );
        final GencodeGtfTranscriptFeature transcript3 = (GencodeGtfTranscriptFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 57, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.EXON,
                50206442, 50206520, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.13", "ENST00000492092.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROCESSED_TRANSCRIPT.toString(), null, "SELENOO-002", 1, "ENSE00001890724.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000316993.1")
                        )
                )
                );
        final GencodeGtfExonFeature exon20 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 58, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.EXON,
                50208488, 50208716, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.13", "ENST00000492092.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROCESSED_TRANSCRIPT.toString(), null, "SELENOO-002", 2, "ENSE00001952603.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000316993.1")
                        )
                )
                );
        final GencodeGtfExonFeature exon21 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 59, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.EXON,
                50210181, 50210311, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.13", "ENST00000492092.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROCESSED_TRANSCRIPT.toString(), null, "SELENOO-002", 3, "ENSE00003583919.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000316993.1")
                        )
                )
                );
        final GencodeGtfExonFeature exon22 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 60, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.EXON,
                50210631, 50210911, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.13", "ENST00000492092.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROCESSED_TRANSCRIPT.toString(), null, "SELENOO-002", 4, "ENSE00003620115.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000316993.1")
                        )
                )
                );
        final GencodeGtfExonFeature exon23 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 61, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.EXON,
                50215717, 50215867, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.13", "ENST00000492092.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROCESSED_TRANSCRIPT.toString(), null, "SELENOO-002", 5, "ENSE00003636069.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000316993.1")
                        )
                )
                );
        final GencodeGtfExonFeature exon24 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 62, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.EXON,
                50216691, 50216876, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.13", "ENST00000492092.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROCESSED_TRANSCRIPT.toString(), null, "SELENOO-002", 6, "ENSE00003579717.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000316993.1")
                        )
                )
                );
        final GencodeGtfExonFeature exon25 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 63, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.EXON,
                50216972, 50217128, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.13", "ENST00000492092.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROCESSED_TRANSCRIPT.toString(), null, "SELENOO-002", 7, "ENSE00003650938.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000316993.1")
                        )
                )
                );
        final GencodeGtfExonFeature exon26 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 64, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.EXON,
                50217205, 50217616, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.13", "ENST00000492092.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "SELENOO", GencodeGTFFieldConstants.KnownGeneBiotype.PROCESSED_TRANSCRIPT.toString(), null, "SELENOO-002", 8, "ENSE00003475904.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000316993.1")
                        )
                )
                );
        final GencodeGtfExonFeature exon27 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);

        // ======================
        // Here we'll set the version number of each feature.
        // Normally this is set in the decode method, so we need to do it manually.

        cds1.setUcscGenomeVersion("hg38");
        cds2.setUcscGenomeVersion("hg38");
        cds3.setUcscGenomeVersion("hg38");
        cds4.setUcscGenomeVersion("hg38");
        cds5.setUcscGenomeVersion("hg38");
        cds6.setUcscGenomeVersion("hg38");
        cds7.setUcscGenomeVersion("hg38");
        cds8.setUcscGenomeVersion("hg38");
        cds9.setUcscGenomeVersion("hg38");
        cds10.setUcscGenomeVersion("hg38");
        cds11.setUcscGenomeVersion("hg38");
        cds12.setUcscGenomeVersion("hg38");
        cds13.setUcscGenomeVersion("hg38");
        cds14.setUcscGenomeVersion("hg38");
        cds15.setUcscGenomeVersion("hg38");
        cds16.setUcscGenomeVersion("hg38");
        cds17.setUcscGenomeVersion("hg38");
        cds18.setUcscGenomeVersion("hg38");
        cds19.setUcscGenomeVersion("hg38");
        exon1.setUcscGenomeVersion("hg38");
        exon2.setUcscGenomeVersion("hg38");
        exon3.setUcscGenomeVersion("hg38");
        exon4.setUcscGenomeVersion("hg38");
        exon5.setUcscGenomeVersion("hg38");
        exon6.setUcscGenomeVersion("hg38");
        exon7.setUcscGenomeVersion("hg38");
        exon8.setUcscGenomeVersion("hg38");
        exon9.setUcscGenomeVersion("hg38");
        exon10.setUcscGenomeVersion("hg38");
        exon11.setUcscGenomeVersion("hg38");
        exon12.setUcscGenomeVersion("hg38");
        exon13.setUcscGenomeVersion("hg38");
        exon14.setUcscGenomeVersion("hg38");
        exon15.setUcscGenomeVersion("hg38");
        exon16.setUcscGenomeVersion("hg38");
        exon17.setUcscGenomeVersion("hg38");
        exon18.setUcscGenomeVersion("hg38");
        exon19.setUcscGenomeVersion("hg38");
        exon20.setUcscGenomeVersion("hg38");
        exon21.setUcscGenomeVersion("hg38");
        exon22.setUcscGenomeVersion("hg38");
        exon23.setUcscGenomeVersion("hg38");
        exon24.setUcscGenomeVersion("hg38");
        exon25.setUcscGenomeVersion("hg38");
        exon26.setUcscGenomeVersion("hg38");
        exon27.setUcscGenomeVersion("hg38");
        start_codon1.setUcscGenomeVersion("hg38");
        stop_codon1.setUcscGenomeVersion("hg38");
        start_codon2.setUcscGenomeVersion("hg38");
        stop_codon2.setUcscGenomeVersion("hg38");
        transcript1.setUcscGenomeVersion("hg38");
        transcript2.setUcscGenomeVersion("hg38");
        transcript3.setUcscGenomeVersion("hg38");
        utr1.setUcscGenomeVersion("hg38");
        utr2.setUcscGenomeVersion("hg38");
        utr3.setUcscGenomeVersion("hg38");
        utr4.setUcscGenomeVersion("hg38");
        selenocysteine1.setUcscGenomeVersion("hg38");
        gene1.setUcscGenomeVersion("hg38");

        // ======================
        // Now let's collapse these objects into their correct structure:

        exon1.setCds(cds1);
        exon1.setStartCodon(start_codon1);

        exon2.setCds(cds2);
        exon3.setCds(cds3);
        exon4.setCds(cds4);
        exon5.setCds(cds5);
        exon6.setCds(cds6);
        exon7.setCds(cds7);
        exon8.setCds(cds8);
        exon9.setCds(cds9);

        exon10.setCds(cds10);
        exon10.setStopCodon(stop_codon1);

        transcript1.addExon(exon1);
        transcript1.addExon(exon2);
        transcript1.addExon(exon3);
        transcript1.addExon(exon4);
        transcript1.addExon(exon5);
        transcript1.addExon(exon6);
        transcript1.addExon(exon7);
        transcript1.addExon(exon8);
        transcript1.addExon(exon9);
        transcript1.addExon(exon10);

        transcript1.addUtr(utr1);
        transcript1.addUtr(utr2);

        gene1.addTranscript(transcript1);

        // ======================

        transcript2.addSelenocysteine(selenocysteine1);

        exon11.setCds(cds11);
        exon11.setStartCodon(start_codon2);

        exon12.setCds(cds12);
        exon13.setCds(cds13);
        exon14.setCds(cds14);
        exon15.setCds(cds15);
        exon16.setCds(cds16);
        exon17.setCds(cds17);
        exon18.setCds(cds18);

        exon19.setCds(cds19);
        exon19.setStopCodon(stop_codon2);

        transcript2.addExon(exon11);
        transcript2.addExon(exon12);
        transcript2.addExon(exon13);
        transcript2.addExon(exon14);
        transcript2.addExon(exon15);
        transcript2.addExon(exon16);
        transcript2.addExon(exon17);
        transcript2.addExon(exon18);
        transcript2.addExon(exon19);

        transcript2.addUtr(utr3);
        transcript2.addUtr(utr4);

        gene1.addTranscript(transcript2);

        // ======================

        transcript3.addExon(exon20);
        transcript3.addExon(exon21);
        transcript3.addExon(exon22);
        transcript3.addExon(exon23);
        transcript3.addExon(exon24);
        transcript3.addExon(exon25);
        transcript3.addExon(exon26);
        transcript3.addExon(exon27);

        gene1.addTranscript(transcript3);

        // ======================

        return gene1;
    }

    /**
     * Creates a valid {@link GencodeGtfGeneFeature} that corresponds to the data in the file {@code gencode.and.this.is.a.valid.one.too.table.gtf}
     * @return a {@link GencodeGtfGeneFeature} representing the data in the file {@code gencode.and.this.is.a.valid.one.too.table.gtf}
     */
    private GencodeGtfGeneFeature createGencodeGtfGene_gencode_and_this_is_a_valid_one_too_table() {

        // Let's define all our features up front:
        GencodeGtfFeatureBaseData data;

        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 6, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.GENE,
                138082, 161852, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000277196.4", null, GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", null, null, null, -1, null, GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>()
        );

        final GencodeGtfGeneFeature gene1 = (GencodeGtfGeneFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 7, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.TRANSCRIPT,
                138082, 161750, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000277196.4", "ENST00000615165.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-202", -1, null, GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000482462.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfTranscriptFeature transcript1 = (GencodeGtfTranscriptFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 8, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                161689, 161750, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000277196.4", "ENST00000615165.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-202", 1, "ENSE00003735197.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000482462.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfExonFeature exon1 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 9, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                156289, 156497, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000277196.4", "ENST00000615165.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-202", 2, "ENSE00003737280.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000482462.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfExonFeature exon2 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 10, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.CDS,
                156289, 156446, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.ZERO, "ENSG00000277196.4", "ENST00000615165.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-202", 2, "ENSE00003737280.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000482462.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfCDSFeature cds1 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 11, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.START_CODON,
                156444, 156446, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.ZERO, "ENSG00000277196.4", "ENST00000615165.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-202", 2, "ENSE00003737280.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000482462.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfStartCodonFeature start_codon1 = (GencodeGtfStartCodonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 12, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                150987, 151021, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000277196.4", "ENST00000615165.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-202", 3, "ENSE00003731891.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000482462.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfExonFeature exon3 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 13, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.CDS,
                150987, 151021, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.ONE, "ENSG00000277196.4", "ENST00000615165.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-202", 3, "ENSE00003731891.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000482462.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfCDSFeature cds2 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 14, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                150350, 150499, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000277196.4", "ENST00000615165.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-202", 4, "ENSE00003724613.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000482462.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfExonFeature exon4 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 15, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.CDS,
                150350, 150499, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.TWO, "ENSG00000277196.4", "ENST00000615165.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-202", 4, "ENSE00003724613.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000482462.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfCDSFeature cds3 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 16, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                148414, 148478, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000277196.4", "ENST00000615165.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-202", 5, "ENSE00003732418.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000482462.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfExonFeature exon5 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 17, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.CDS,
                148414, 148478, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.TWO, "ENSG00000277196.4", "ENST00000615165.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-202", 5, "ENSE00003732418.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000482462.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfCDSFeature cds4 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 18, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                148116, 148232, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000277196.4", "ENST00000615165.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-202", 6, "ENSE00003733960.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000482462.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfExonFeature exon6 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 19, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.CDS,
                148116, 148232, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.ZERO, "ENSG00000277196.4", "ENST00000615165.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-202", 6, "ENSE00003733960.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000482462.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfCDSFeature cds5 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 20, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                147624, 147703, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000277196.4", "ENST00000615165.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-202", 7, "ENSE00003727207.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000482462.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfExonFeature exon7 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 21, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.CDS,
                147624, 147703, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.ZERO, "ENSG00000277196.4", "ENST00000615165.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-202", 7, "ENSE00003727207.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000482462.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfCDSFeature cds6 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 22, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                146640, 146721, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000277196.4", "ENST00000615165.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-202", 8, "ENSE00003728972.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000482462.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfExonFeature exon8 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 23, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.CDS,
                146640, 146721, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.ONE, "ENSG00000277196.4", "ENST00000615165.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-202", 8, "ENSE00003728972.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000482462.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfCDSFeature cds7 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 24, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                145004, 145096, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000277196.4", "ENST00000615165.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-202", 9, "ENSE00003733844.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000482462.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfExonFeature exon9 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 25, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.CDS,
                145004, 145096, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.ZERO, "ENSG00000277196.4", "ENST00000615165.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-202", 9, "ENSE00003733844.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000482462.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfCDSFeature cds8 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 26, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                144749, 144895, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000277196.4", "ENST00000615165.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-202", 10, "ENSE00003752738.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000482462.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfExonFeature exon10 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 27, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.CDS,
                144749, 144895, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.ZERO, "ENSG00000277196.4", "ENST00000615165.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-202", 10, "ENSE00003752738.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000482462.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfCDSFeature cds9 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 28, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                143614, 143789, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000277196.4", "ENST00000615165.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-202", 11, "ENSE00003720006.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000482462.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfExonFeature exon11 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 29, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.CDS,
                143614, 143789, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.ZERO, "ENSG00000277196.4", "ENST00000615165.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-202", 11, "ENSE00003720006.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000482462.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfCDSFeature cds10 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 30, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                142194, 142292, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000277196.4", "ENST00000615165.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-202", 12, "ENSE00003719283.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000482462.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfExonFeature exon12 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 31, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.CDS,
                142194, 142292, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.ONE, "ENSG00000277196.4", "ENST00000615165.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-202", 12, "ENSE00003719283.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000482462.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfCDSFeature cds11 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 32, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                138743, 138831, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000277196.4", "ENST00000615165.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-202", 13, "ENSE00003751415.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000482462.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfExonFeature exon13 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 33, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.CDS,
                138743, 138831, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.ONE, "ENSG00000277196.4", "ENST00000615165.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-202", 13, "ENSE00003751415.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000482462.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfCDSFeature cds12 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 34, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                138082, 138667, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000277196.4", "ENST00000615165.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-202", 14, "ENSE00003753010.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000482462.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfExonFeature exon14 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 35, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.CDS,
                138483, 138667, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.TWO, "ENSG00000277196.4", "ENST00000615165.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-202", 14, "ENSE00003753010.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000482462.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfCDSFeature cds13 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 36, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.STOP_CODON,
                138480, 138482, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.ZERO, "ENSG00000277196.4", "ENST00000615165.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-202", 14, "ENSE00003753010.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000482462.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfStopCodonFeature stop_codon1 = (GencodeGtfStopCodonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 37, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.UTR,
                161689, 161750, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000277196.4", "ENST00000615165.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-202", 1, "ENSE00003735197.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000482462.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfUTRFeature utr1 = (GencodeGtfUTRFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 38, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.UTR,
                156447, 156497, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000277196.4", "ENST00000615165.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-202", 2, "ENSE00003737280.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000482462.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfUTRFeature utr2 = (GencodeGtfUTRFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 39, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.UTR,
                138082, 138482, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000277196.4", "ENST00000615165.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-202", 14, "ENSE00003753010.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000482462.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfUTRFeature utr3 = (GencodeGtfUTRFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 40, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.TRANSCRIPT,
                138082, 161852, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000277196.4", "ENST00000621424.4", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-201", -1, null, GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000481127.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfTranscriptFeature transcript2 = (GencodeGtfTranscriptFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 41, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                161689, 161852, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000277196.4", "ENST00000621424.4", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-201", 1, "ENSE00003746084.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000481127.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfExonFeature exon15 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 42, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                161314, 161626, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000277196.4", "ENST00000621424.4", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-201", 2, "ENSE00003719550.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000481127.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfExonFeature exon16 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 43, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.CDS,
                161314, 161586, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.ZERO, "ENSG00000277196.4", "ENST00000621424.4", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-201", 2, "ENSE00003719550.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000481127.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfCDSFeature cds14 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 44, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.START_CODON,
                161584, 161586, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.ZERO, "ENSG00000277196.4", "ENST00000621424.4", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-201", 2, "ENSE00003719550.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000481127.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfStartCodonFeature start_codon2 = (GencodeGtfStartCodonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 45, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                156289, 156497, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000277196.4", "ENST00000621424.4", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-201", 3, "ENSE00003723757.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000481127.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfExonFeature exon17 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 46, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.CDS,
                156289, 156497, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.ZERO, "ENSG00000277196.4", "ENST00000621424.4", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-201", 3, "ENSE00003723757.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000481127.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfCDSFeature cds15 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 47, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                150987, 151021, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000277196.4", "ENST00000621424.4", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-201", 4, "ENSE00003731891.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000481127.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfExonFeature exon18 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 48, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.CDS,
                150987, 151021, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.ONE, "ENSG00000277196.4", "ENST00000621424.4", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-201", 4, "ENSE00003731891.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000481127.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfCDSFeature cds16 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 49, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                150350, 150499, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000277196.4", "ENST00000621424.4", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-201", 5, "ENSE00003724613.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000481127.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfExonFeature exon19 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 50, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.CDS,
                150350, 150499, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.TWO, "ENSG00000277196.4", "ENST00000621424.4", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-201", 5, "ENSE00003724613.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000481127.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfCDSFeature cds17 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 51, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                148414, 148478, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000277196.4", "ENST00000621424.4", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-201", 6, "ENSE00003732418.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000481127.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfExonFeature exon20 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 52, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.CDS,
                148414, 148478, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.TWO, "ENSG00000277196.4", "ENST00000621424.4", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-201", 6, "ENSE00003732418.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000481127.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfCDSFeature cds18 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 53, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                148116, 148232, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000277196.4", "ENST00000621424.4", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-201", 7, "ENSE00003733960.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000481127.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfExonFeature exon21 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 54, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.CDS,
                148116, 148232, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.ZERO, "ENSG00000277196.4", "ENST00000621424.4", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-201", 7, "ENSE00003733960.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000481127.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfCDSFeature cds19 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 55, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                147624, 147703, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000277196.4", "ENST00000621424.4", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-201", 8, "ENSE00003727207.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000481127.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfExonFeature exon22 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 56, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.CDS,
                147624, 147703, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.ZERO, "ENSG00000277196.4", "ENST00000621424.4", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-201", 8, "ENSE00003727207.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000481127.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfCDSFeature cds20 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 57, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                146640, 146721, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000277196.4", "ENST00000621424.4", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-201", 9, "ENSE00003728972.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000481127.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfExonFeature exon23 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 58, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.CDS,
                146640, 146721, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.ONE, "ENSG00000277196.4", "ENST00000621424.4", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-201", 9, "ENSE00003728972.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000481127.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfCDSFeature cds21 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 59, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                145004, 145096, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000277196.4", "ENST00000621424.4", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-201", 10, "ENSE00003733844.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000481127.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfExonFeature exon24 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 60, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.CDS,
                145004, 145096, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.ZERO, "ENSG00000277196.4", "ENST00000621424.4", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-201", 10, "ENSE00003733844.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000481127.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfCDSFeature cds22 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 61, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                144749, 144895, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000277196.4", "ENST00000621424.4", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-201", 11, "ENSE00003752738.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000481127.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfExonFeature exon25 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 62, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.CDS,
                144749, 144895, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.ZERO, "ENSG00000277196.4", "ENST00000621424.4", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-201", 11, "ENSE00003752738.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000481127.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfCDSFeature cds23 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 63, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                143614, 143789, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000277196.4", "ENST00000621424.4", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-201", 12, "ENSE00003720006.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000481127.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfExonFeature exon26 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 64, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.CDS,
                143614, 143789, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.ZERO, "ENSG00000277196.4", "ENST00000621424.4", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-201", 12, "ENSE00003720006.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000481127.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfCDSFeature cds24 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 65, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                142194, 142292, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000277196.4", "ENST00000621424.4", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-201", 13, "ENSE00003719283.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000481127.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfExonFeature exon27 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 66, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.CDS,
                142194, 142292, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.ONE, "ENSG00000277196.4", "ENST00000621424.4", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-201", 13, "ENSE00003719283.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000481127.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfCDSFeature cds25 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 67, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                138743, 138831, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000277196.4", "ENST00000621424.4", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-201", 14, "ENSE00003751415.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000481127.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfExonFeature exon28 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 68, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.CDS,
                138743, 138831, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.ONE, "ENSG00000277196.4", "ENST00000621424.4", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-201", 14, "ENSE00003751415.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000481127.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfCDSFeature cds26 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 69, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                138082, 138667, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000277196.4", "ENST00000621424.4", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-201", 15, "ENSE00003753010.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000481127.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfExonFeature exon29 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 70, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.CDS,
                138483, 138667, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.TWO, "ENSG00000277196.4", "ENST00000621424.4", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-201", 15, "ENSE00003753010.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000481127.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfCDSFeature cds27 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 71, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.STOP_CODON,
                138480, 138482, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.ZERO, "ENSG00000277196.4", "ENST00000621424.4", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-201", 15, "ENSE00003753010.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000481127.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfStopCodonFeature stop_codon2 = (GencodeGtfStopCodonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 72, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.UTR,
                161689, 161852, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000277196.4", "ENST00000621424.4", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-201", 1, "ENSE00003746084.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000481127.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfUTRFeature utr4 = (GencodeGtfUTRFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 73, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.UTR,
                161587, 161626, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000277196.4", "ENST00000621424.4", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-201", 2, "ENSE00003719550.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000481127.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfUTRFeature utr5 = (GencodeGtfUTRFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 74, "KI270734.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.UTR,
                138082, 138482, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000277196.4", "ENST00000621424.4", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                null, "AC007325.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), null, "AC007325.2-201", 15, "ENSE00003753010.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("protein_id", "ENSP00000481127.1"),
                                new GencodeGtfFeature.OptionalField<String>("transcript_support_level", GencodeGTFFieldConstants.TranscriptSupportLevel.ALL_MRNA_VERIFIED.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfUTRFeature utr6 = (GencodeGtfUTRFeature) GencodeGtfFeature.create(data);

        // ======================
        // Here we'll set the version number of each feature.
        // Normally this is set in the decode method, so we need to do it manually.

        cds1.setUcscGenomeVersion("hg38");
        cds2.setUcscGenomeVersion("hg38");
        cds3.setUcscGenomeVersion("hg38");
        cds4.setUcscGenomeVersion("hg38");
        cds5.setUcscGenomeVersion("hg38");
        cds6.setUcscGenomeVersion("hg38");
        cds7.setUcscGenomeVersion("hg38");
        cds8.setUcscGenomeVersion("hg38");
        cds9.setUcscGenomeVersion("hg38");
        cds10.setUcscGenomeVersion("hg38");
        cds11.setUcscGenomeVersion("hg38");
        cds12.setUcscGenomeVersion("hg38");
        cds13.setUcscGenomeVersion("hg38");
        cds14.setUcscGenomeVersion("hg38");
        cds15.setUcscGenomeVersion("hg38");
        cds16.setUcscGenomeVersion("hg38");
        cds17.setUcscGenomeVersion("hg38");
        cds18.setUcscGenomeVersion("hg38");
        cds19.setUcscGenomeVersion("hg38");
        cds20.setUcscGenomeVersion("hg38");
        cds21.setUcscGenomeVersion("hg38");
        cds22.setUcscGenomeVersion("hg38");
        cds23.setUcscGenomeVersion("hg38");
        cds24.setUcscGenomeVersion("hg38");
        cds25.setUcscGenomeVersion("hg38");
        cds26.setUcscGenomeVersion("hg38");
        cds27.setUcscGenomeVersion("hg38");
        exon1.setUcscGenomeVersion("hg38");
        exon2.setUcscGenomeVersion("hg38");
        exon3.setUcscGenomeVersion("hg38");
        exon4.setUcscGenomeVersion("hg38");
        exon5.setUcscGenomeVersion("hg38");
        exon6.setUcscGenomeVersion("hg38");
        exon7.setUcscGenomeVersion("hg38");
        exon8.setUcscGenomeVersion("hg38");
        exon9.setUcscGenomeVersion("hg38");
        exon10.setUcscGenomeVersion("hg38");
        exon11.setUcscGenomeVersion("hg38");
        exon12.setUcscGenomeVersion("hg38");
        exon13.setUcscGenomeVersion("hg38");
        exon14.setUcscGenomeVersion("hg38");
        exon15.setUcscGenomeVersion("hg38");
        exon16.setUcscGenomeVersion("hg38");
        exon17.setUcscGenomeVersion("hg38");
        exon18.setUcscGenomeVersion("hg38");
        exon19.setUcscGenomeVersion("hg38");
        exon20.setUcscGenomeVersion("hg38");
        exon21.setUcscGenomeVersion("hg38");
        exon22.setUcscGenomeVersion("hg38");
        exon23.setUcscGenomeVersion("hg38");
        exon24.setUcscGenomeVersion("hg38");
        exon25.setUcscGenomeVersion("hg38");
        exon26.setUcscGenomeVersion("hg38");
        exon27.setUcscGenomeVersion("hg38");
        exon28.setUcscGenomeVersion("hg38");
        exon29.setUcscGenomeVersion("hg38");
        start_codon1.setUcscGenomeVersion("hg38");
        start_codon2.setUcscGenomeVersion("hg38");
        stop_codon1.setUcscGenomeVersion("hg38");
        stop_codon2.setUcscGenomeVersion("hg38");
        transcript1.setUcscGenomeVersion("hg38");
        transcript2.setUcscGenomeVersion("hg38");
        utr1.setUcscGenomeVersion("hg38");
        utr2.setUcscGenomeVersion("hg38");
        utr3.setUcscGenomeVersion("hg38");
        utr4.setUcscGenomeVersion("hg38");
        utr5.setUcscGenomeVersion("hg38");
        utr6.setUcscGenomeVersion("hg38");
        gene1.setUcscGenomeVersion("hg38");

        // ======================
        // Now let's collapse these objects into their correct structure:

        exon2.setCds(cds1);
        exon2.setStartCodon(start_codon1);

        exon3.setCds(cds2);
        exon4.setCds(cds3);
        exon5.setCds(cds4);
        exon6.setCds(cds5);
        exon7.setCds(cds6);
        exon8.setCds(cds7);
        exon9.setCds(cds8);
        exon10.setCds(cds9);
        exon11.setCds(cds10);
        exon12.setCds(cds11);
        exon13.setCds(cds12);

        exon14.setCds(cds13);
        exon14.setStopCodon(stop_codon1);

        transcript1.addExon(exon1);
        transcript1.addExon(exon2);
        transcript1.addExon(exon3);
        transcript1.addExon(exon4);
        transcript1.addExon(exon5);
        transcript1.addExon(exon6);
        transcript1.addExon(exon7);
        transcript1.addExon(exon8);
        transcript1.addExon(exon9);
        transcript1.addExon(exon10);
        transcript1.addExon(exon11);
        transcript1.addExon(exon12);
        transcript1.addExon(exon13);
        transcript1.addExon(exon14);

        transcript1.addUtr(utr1);
        transcript1.addUtr(utr2);
        transcript1.addUtr(utr3);

        gene1.addTranscript(transcript1);

        // ======================

        exon16.setCds(cds14);
        exon16.setStartCodon(start_codon2);

        exon17.setCds(cds15);
        exon18.setCds(cds16);
        exon19.setCds(cds17);
        exon20.setCds(cds18);
        exon21.setCds(cds19);
        exon22.setCds(cds20);
        exon23.setCds(cds21);
        exon24.setCds(cds22);
        exon25.setCds(cds23);
        exon26.setCds(cds24);
        exon27.setCds(cds25);
        exon28.setCds(cds26);

        exon29.setCds(cds27);
        exon29.setStopCodon(stop_codon2);

        transcript2.addExon(exon15);
        transcript2.addExon(exon16);
        transcript2.addExon(exon17);
        transcript2.addExon(exon18);
        transcript2.addExon(exon19);
        transcript2.addExon(exon20);
        transcript2.addExon(exon21);
        transcript2.addExon(exon22);
        transcript2.addExon(exon23);
        transcript2.addExon(exon24);
        transcript2.addExon(exon25);
        transcript2.addExon(exon26);
        transcript2.addExon(exon27);
        transcript2.addExon(exon28);
        transcript2.addExon(exon29);

        transcript2.addUtr(utr4);
        transcript2.addUtr(utr5);
        transcript2.addUtr(utr6);

        gene1.addTranscript(transcript2);

        // ======================

        return gene1;
    }

    private GencodeGtfGeneFeature createGencodeGtfGene_gencode_v19_valid_file1() {

        // Create the Features as they exist in the test file:
        GencodeGtfFeatureBaseData data;

        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 6, "chr1", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.GENE,
                11869, 14412, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000223972.4", "ENSG00000223972.4",
                GencodeGTFFieldConstants.KnownGeneBiotype.PSEUDOGENE.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "DDX11L1", GencodeGTFFieldConstants.KnownGeneBiotype.PSEUDOGENE.toString(),
                 GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "DDX11L1", -1, null, GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Collections.singletonList(
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000000961.2")
                        )
                )
                );
        final GencodeGtfGeneFeature gene = (GencodeGtfGeneFeature)GencodeGtfFeature.create(data);

        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 7, "chr1", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.TRANSCRIPT,
                11869, 14409, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000223972.4", "ENST00000456328.2",
                GencodeGTFFieldConstants.KnownGeneBiotype.PSEUDOGENE.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "DDX11L1", GencodeGTFFieldConstants.KnownGeneBiotype.PROCESSED_TRANSCRIPT.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "DDX11L1-002", -1, null, GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000000961.2"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000362751.1")
                        )
                )
                );
        final GencodeGtfTranscriptFeature transcript = (GencodeGtfTranscriptFeature) GencodeGtfFeature.create(data);

        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 8, "chr1", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.EXON,
                11869, 12227, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000223972.4", "ENST00000456328.2",
                GencodeGTFFieldConstants.KnownGeneBiotype.PSEUDOGENE.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "DDX11L1", GencodeGTFFieldConstants.KnownGeneBiotype.PROCESSED_TRANSCRIPT.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "DDX11L1-002", 1, "ENSE00002234944.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000000961.2"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000362751.1")
                        )
                )
                );
        final GencodeGtfExonFeature exon = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);

        // ======================
        // Here we'll set the version number of each feature.
        // Normally this is set in the decode method, so we need to do it manually.

        exon.setUcscGenomeVersion("hg19");
        transcript.setUcscGenomeVersion("hg19");
        gene.setUcscGenomeVersion("hg19");

        // Aggregate the Features as they should be:
        transcript.addExon(exon);
        gene.addTranscript(transcript);

        return gene;
    }

    private ArrayList<GencodeGtfGeneFeature> createGencodeGtfGene_gencode_v19_valid_gencode_file2() {

        // Create the Features as they exist in the test file:
        GencodeGtfFeatureBaseData data;

        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 6, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.GENE,
                50637519, 50638976, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000273253.1", "ENSG00000273253.1", GencodeGTFFieldConstants.KnownGeneBiotype.ANTISENSE.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.NOVEL.toString(), "RP3-402G11.26", GencodeGTFFieldConstants.KnownGeneBiotype.ANTISENSE.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.NOVEL.toString(), "RP3-402G11.26", -1, null, GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Collections.singletonList(
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000186123.2")
                        )
                )
                );
        final GencodeGtfGeneFeature gene1 = (GencodeGtfGeneFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 7, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.TRANSCRIPT,
                50637519, 50638976, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000273253.1", "ENST00000608025.1", GencodeGTFFieldConstants.KnownGeneBiotype.ANTISENSE.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.NOVEL.toString(), "RP3-402G11.26", GencodeGTFFieldConstants.KnownGeneBiotype.ANTISENSE.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "RP3-402G11.26-001", -1, null, GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000186123.2"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000472292.2")
                        )
                )
                );
        final GencodeGtfTranscriptFeature transcript1 = (GencodeGtfTranscriptFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 8, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.EXON,
                50638505, 50638976, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000273253.1", "ENST00000608025.1", GencodeGTFFieldConstants.KnownGeneBiotype.ANTISENSE.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.NOVEL.toString(), "RP3-402G11.26", GencodeGTFFieldConstants.KnownGeneBiotype.ANTISENSE.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "RP3-402G11.26-001", 1, "ENSE00003710600.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000186123.2"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000472292.2")
                        )
                )
                );
        final GencodeGtfExonFeature exon1 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 9, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.EXON,
                50637519, 50637757, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000273253.1", "ENST00000608025.1", GencodeGTFFieldConstants.KnownGeneBiotype.ANTISENSE.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.NOVEL.toString(), "RP3-402G11.26", GencodeGTFFieldConstants.KnownGeneBiotype.ANTISENSE.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "RP3-402G11.26-001", 2, "ENSE00003710731.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000186123.2"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000472292.2")
                        )
                )
                );
        final GencodeGtfExonFeature exon2 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 10, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.GENE,
                50639408, 50656045, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.9", "ENSG00000073169.9", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO", -1, null, GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Collections.singletonList(
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3")
                        )
                )
                );
        final GencodeGtfGeneFeature gene2 = (GencodeGtfGeneFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 11, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.TRANSCRIPT,
                50639408, 50656045, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.9", "ENST00000380903.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO-001", -1, null, GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfTranscriptFeature transcript2 = (GencodeGtfTranscriptFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 12, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.SELENOCYSTEINE,
                50655787, 50655789, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.9", "ENST00000380903.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO-001", -1, null, GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfSelenocysteineFeature selenocysteine1 = (GencodeGtfSelenocysteineFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 13, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.EXON,
                50639408, 50640019, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.9", "ENST00000380903.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO-001", 1, "ENSE00001541223.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfExonFeature exon3 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 14, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.CDS,
                50639466, 50640019, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.ZERO, "ENSG00000073169.9", "ENST00000380903.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO-001", 1, "ENSE00001541223.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfCDSFeature cds1 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 15, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.START_CODON,
                50639466, 50639468, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.ZERO, "ENSG00000073169.9", "ENST00000380903.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO-001", 1, "ENSE00001541223.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfStartCodonFeature start_codon1 = (GencodeGtfStartCodonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 16, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.EXON,
                50644746, 50644949, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.9", "ENST00000380903.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO-001", 2, "ENSE00001129529.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfExonFeature exon4 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 17, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.CDS,
                50644746, 50644949, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.ONE, "ENSG00000073169.9", "ENST00000380903.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO-001", 2, "ENSE00001129529.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfCDSFeature cds2 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 18, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.EXON,
                50646965, 50647145, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.9", "ENST00000380903.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO-001", 3, "ENSE00001129524.2", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfExonFeature exon5 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 19, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.CDS,
                50646965, 50647145, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.ONE, "ENSG00000073169.9", "ENST00000380903.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO-001", 3, "ENSE00001129524.2", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfCDSFeature cds3 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 20, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.EXON,
                50648610, 50648740, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.9", "ENST00000380903.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO-001", 4, "ENSE00003473644.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfExonFeature exon6 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 21, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.CDS,
                50648610, 50648740, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.ZERO, "ENSG00000073169.9", "ENST00000380903.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO-001", 4, "ENSE00003473644.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfCDSFeature cds4 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 22, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.EXON,
                50649060, 50649340, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.9", "ENST00000380903.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO-001", 5, "ENSE00003503715.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfExonFeature exon7 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 23, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.CDS,
                50649060, 50649340, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.ONE, "ENSG00000073169.9", "ENST00000380903.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO-001", 5, "ENSE00003503715.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfCDSFeature cds5 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 24, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.EXON,
                50654146, 50654296, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.9", "ENST00000380903.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO-001", 6, "ENSE00003573348.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfExonFeature exon8 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 25, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.CDS,
                50654146, 50654296, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.TWO, "ENSG00000073169.9", "ENST00000380903.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO-001", 6, "ENSE00003573348.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfCDSFeature cds6 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 26, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.EXON,
                50655120, 50655305, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.9", "ENST00000380903.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO-001", 7, "ENSE00003510005.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfExonFeature exon9 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 27, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.CDS,
                50655120, 50655305, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.ONE, "ENSG00000073169.9", "ENST00000380903.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO-001", 7, "ENSE00003510005.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfCDSFeature cds7 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 28, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.EXON,
                50655401, 50655557, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.9", "ENST00000380903.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO-001", 8, "ENSE00003591346.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfExonFeature exon10 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 29, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.CDS,
                50655401, 50655557, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.ONE, "ENSG00000073169.9", "ENST00000380903.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO-001", 8, "ENSE00003591346.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfCDSFeature cds8 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 30, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.EXON,
                50655634, 50656045, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.9", "ENST00000380903.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO-001", 9, "ENSE00003512975.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfExonFeature exon11 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 31, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.CDS,
                50655634, 50655795, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.ZERO, "ENSG00000073169.9", "ENST00000380903.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO-001", 9, "ENSE00003512975.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfCDSFeature cds9 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 32, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.STOP_CODON,
                50655796, 50655798, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.ZERO, "ENSG00000073169.9", "ENST00000380903.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO-001", 9, "ENSE00003512975.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfStopCodonFeature stop_codon1 = (GencodeGtfStopCodonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 33, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.UTR,
                50639408, 50639465, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.9", "ENST00000380903.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO-001", -1, null, GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfUTRFeature utr1 = (GencodeGtfUTRFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 34, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.UTR,
                50655796, 50656045, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.9", "ENST00000380903.2", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO-001", -1, null, GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.APPRIS_PRINCIPAL.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.CCDS.toString()),
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.SELENO.toString()),
                                new GencodeGtfFeature.OptionalField<String>("ccdsid", "CCDS43034.1"),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000075003.2")
                        )
                )
                );
        final GencodeGtfUTRFeature utr2 = (GencodeGtfUTRFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 35, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.TRANSCRIPT,
                50644871, 50656045, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.9", "ENST00000492092.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO", GencodeGTFFieldConstants.KnownGeneBiotype.PROCESSED_TRANSCRIPT.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO-002", -1, null, GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000316993.1")
                        )
                )
                );
        final GencodeGtfTranscriptFeature transcript3 = (GencodeGtfTranscriptFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 36, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.EXON,
                50644871, 50644949, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.9", "ENST00000492092.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO", GencodeGTFFieldConstants.KnownGeneBiotype.PROCESSED_TRANSCRIPT.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO-002", 1, "ENSE00001890724.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000316993.1")
                        )
                )
                );
        final GencodeGtfExonFeature exon12 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 37, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.EXON,
                50646917, 50647145, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.9", "ENST00000492092.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO", GencodeGTFFieldConstants.KnownGeneBiotype.PROCESSED_TRANSCRIPT.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO-002", 2, "ENSE00001952603.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000316993.1")
                        )
                )
                );
        final GencodeGtfExonFeature exon13 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 38, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.EXON,
                50648610, 50648740, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.9", "ENST00000492092.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO", GencodeGTFFieldConstants.KnownGeneBiotype.PROCESSED_TRANSCRIPT.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO-002", 3, "ENSE00003583919.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000316993.1")
                        )
                )
                );
        final GencodeGtfExonFeature exon14 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 39, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.EXON,
                50649060, 50649340, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.9", "ENST00000492092.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO", GencodeGTFFieldConstants.KnownGeneBiotype.PROCESSED_TRANSCRIPT.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO-002", 4, "ENSE00003620115.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000316993.1")
                        )
                )
                );
        final GencodeGtfExonFeature exon15 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 40, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.EXON,
                50654146, 50654296, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.9", "ENST00000492092.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO", GencodeGTFFieldConstants.KnownGeneBiotype.PROCESSED_TRANSCRIPT.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO-002", 5, "ENSE00003636069.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000316993.1")
                        )
                )
                );
        final GencodeGtfExonFeature exon16 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 41, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.EXON,
                50655120, 50655305, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.9", "ENST00000492092.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO", GencodeGTFFieldConstants.KnownGeneBiotype.PROCESSED_TRANSCRIPT.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO-002", 6, "ENSE00003579717.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000316993.1")
                        )
                )
                );
        final GencodeGtfExonFeature exon17 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 42, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.EXON,
                50655401, 50655557, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.9", "ENST00000492092.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO", GencodeGTFFieldConstants.KnownGeneBiotype.PROCESSED_TRANSCRIPT.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO-002", 7, "ENSE00003650938.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000316993.1")
                        )
                )
                );
        final GencodeGtfExonFeature exon18 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);
        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 43, "chr22", GencodeGtfFeature.ANNOTATION_SOURCE_HAVANA, GencodeGtfFeature.FeatureType.EXON,
                50655634, 50656045, Strand.POSITIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000073169.9", "ENST00000492092.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO", GencodeGTFFieldConstants.KnownGeneBiotype.PROCESSED_TRANSCRIPT.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "SELO-002", 8, "ENSE00003475904.1", GencodeGTFFieldConstants.LocusLevel.MANUALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Arrays.asList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString()),
                                new GencodeGtfFeature.OptionalField<String>("havana_gene", "OTTHUMG00000044645.3"),
                                new GencodeGtfFeature.OptionalField<String>("havana_transcript", "OTTHUMT00000316993.1")
                        )
                )
                );
        final GencodeGtfExonFeature exon19 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);

        // ======================
        // Here we'll set the version number of each feature.
        // Normally this is set in the decode method, so we need to do it manually.

        exon1.setUcscGenomeVersion("hg19");
        exon2.setUcscGenomeVersion("hg19");
        exon3.setUcscGenomeVersion("hg19");
        exon4.setUcscGenomeVersion("hg19");
        exon5.setUcscGenomeVersion("hg19");
        exon6.setUcscGenomeVersion("hg19");
        exon7.setUcscGenomeVersion("hg19");
        exon8.setUcscGenomeVersion("hg19");
        exon9.setUcscGenomeVersion("hg19");
        exon10.setUcscGenomeVersion("hg19");
        exon11.setUcscGenomeVersion("hg19");
        exon12.setUcscGenomeVersion("hg19");
        exon13.setUcscGenomeVersion("hg19");
        exon14.setUcscGenomeVersion("hg19");
        exon15.setUcscGenomeVersion("hg19");
        exon16.setUcscGenomeVersion("hg19");
        exon17.setUcscGenomeVersion("hg19");
        exon18.setUcscGenomeVersion("hg19");
        exon19.setUcscGenomeVersion("hg19");
        transcript1.setUcscGenomeVersion("hg19");
        transcript2.setUcscGenomeVersion("hg19");
        transcript3.setUcscGenomeVersion("hg19");
        gene1.setUcscGenomeVersion("hg19");
        gene2.setUcscGenomeVersion("hg19");
        selenocysteine1.setUcscGenomeVersion("hg19");
        start_codon1.setUcscGenomeVersion("hg19");
        stop_codon1.setUcscGenomeVersion("hg19");
        cds1.setUcscGenomeVersion("hg19");
        cds2.setUcscGenomeVersion("hg19");
        cds3.setUcscGenomeVersion("hg19");
        cds4.setUcscGenomeVersion("hg19");
        cds5.setUcscGenomeVersion("hg19");
        cds6.setUcscGenomeVersion("hg19");
        cds7.setUcscGenomeVersion("hg19");
        cds8.setUcscGenomeVersion("hg19");
        cds9.setUcscGenomeVersion("hg19");
        utr1.setUcscGenomeVersion("hg19");
        utr2.setUcscGenomeVersion("hg19");

        // ======================
        // Now let's collapse these objects into their correct structure:

        transcript1.addExon(exon1);
        transcript1.addExon(exon2);
        gene1.addTranscript(transcript1);

        // ======================
        // ======================

        transcript2.addSelenocysteine(selenocysteine1);

        exon3.setCds(cds1);
        exon3.setStartCodon(start_codon1);

        exon4.setCds(cds2);
        exon5.setCds(cds3);
        exon6.setCds(cds4);
        exon7.setCds(cds5);
        exon8.setCds(cds6);
        exon9.setCds(cds7);
        exon10.setCds(cds8);

        exon11.setCds(cds9);
        exon11.setStopCodon(stop_codon1);

        transcript2.addExon(exon3);
        transcript2.addExon(exon4);
        transcript2.addExon(exon5);
        transcript2.addExon(exon6);
        transcript2.addExon(exon7);
        transcript2.addExon(exon8);
        transcript2.addExon(exon9);
        transcript2.addExon(exon10);
        transcript2.addExon(exon11);

        transcript2.addUtr(utr1);
        transcript2.addUtr(utr2);

        // ======================

        transcript3.addExon(exon12);
        transcript3.addExon(exon13);
        transcript3.addExon(exon14);
        transcript3.addExon(exon15);
        transcript3.addExon(exon16);
        transcript3.addExon(exon17);
        transcript3.addExon(exon18);
        transcript3.addExon(exon19);

        // ======================

        gene2.addTranscript(transcript2);
        gene2.addTranscript(transcript3);

        return new ArrayList<>( Arrays.asList(gene1, gene2) );
    }

    private GencodeGtfGeneFeature createGencodeGtfGene_gencode_v19_and_this_is_a_valid_one_too() {

        // Create the Features as they exist in the test file:
        GencodeGtfFeatureBaseData data;

        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 6, "GL000218.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.GENE,
                38792, 97421, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000215615.1", "ENSG00000215615.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "AL354822.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "AL354822.1", -1, null, GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>() );

        final GencodeGtfGeneFeature gene1 = (GencodeGtfGeneFeature) GencodeGtfFeature.create(data);

        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 7, "GL000218.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.TRANSCRIPT,
                38792, 97421, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000215615.1", "ENST00000400681.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "AL354822.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "AL354822.1-201", -1, null, GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Collections.singletonList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfTranscriptFeature transcript1 = (GencodeGtfTranscriptFeature) GencodeGtfFeature.create(data);

        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 8, "GL000218.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                97368, 97421, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000215615.1", "ENST00000400681.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "AL354822.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "AL354822.1-201", 1, "ENSE00001544212.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Collections.singletonList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfExonFeature exon1 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);

        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 9, "GL000218.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                95174, 95232, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000215615.1", "ENST00000400681.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "AL354822.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "AL354822.1-201", 2, "ENSE00001849396.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Collections.singletonList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfExonFeature exon2 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);

        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 10, "GL000218.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.CDS,
                95174, 95230, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.ZERO, "ENSG00000215615.1", "ENST00000400681.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "AL354822.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "AL354822.1-201", 2, "ENSE00001849396.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Collections.singletonList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfCDSFeature cds1 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);

        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 11, "GL000218.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.START_CODON,
                95228, 95230, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.ZERO, "ENSG00000215615.1", "ENST00000400681.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "AL354822.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "AL354822.1-201", 2, "ENSE00001849396.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Collections.singletonList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfStartCodonFeature start_codon1 = (GencodeGtfStartCodonFeature) GencodeGtfFeature.create(data);

        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 12, "GL000218.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                40642, 40872, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000215615.1", "ENST00000400681.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "AL354822.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "AL354822.1-201", 3, "ENSE00001900862.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Collections.singletonList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfExonFeature exon3 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);

        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 13, "GL000218.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.CDS,
                40642, 40872, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.ZERO, "ENSG00000215615.1", "ENST00000400681.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "AL354822.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "AL354822.1-201", 3, "ENSE00001900862.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Collections.singletonList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfCDSFeature cds2 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);

        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 14, "GL000218.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                39874, 40028, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000215615.1", "ENST00000400681.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "AL354822.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "AL354822.1-201", 4, "ENSE00001544206.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Collections.singletonList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfExonFeature exon4 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);

        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 15, "GL000218.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.CDS,
                39874, 40028, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.ZERO, "ENSG00000215615.1", "ENST00000400681.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "AL354822.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "AL354822.1-201", 4, "ENSE00001544206.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Collections.singletonList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfCDSFeature cds3 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);

        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 16, "GL000218.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.EXON,
                38792, 39019, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000215615.1", "ENST00000400681.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "AL354822.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "AL354822.1-201", 5, "ENSE00001544203.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Collections.singletonList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfExonFeature exon5 = (GencodeGtfExonFeature) GencodeGtfFeature.create(data);

        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 17, "GL000218.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.CDS,
                38794, 39019, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.ONE, "ENSG00000215615.1", "ENST00000400681.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "AL354822.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "AL354822.1-201", 5, "ENSE00001544203.1", GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Collections.singletonList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfCDSFeature cds4 = (GencodeGtfCDSFeature) GencodeGtfFeature.create(data);

        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 18, "GL000218.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.UTR,
                97368, 97421, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000215615.1", "ENST00000400681.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "AL354822.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "AL354822.1-201", -1, null, GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Collections.singletonList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfUTRFeature utr1 = (GencodeGtfUTRFeature) GencodeGtfFeature.create(data);

        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 19, "GL000218.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.UTR,
                95231, 95232, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000215615.1", "ENST00000400681.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "AL354822.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "AL354822.1-201", -1, null, GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Collections.singletonList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfUTRFeature utr2 = (GencodeGtfUTRFeature) GencodeGtfFeature.create(data);

        data = new GencodeGtfFeatureBaseData(GencodeGtfCodec.GTF_FILE_TYPE_STRING, 20, "GL000218.1", GencodeGtfFeature.ANNOTATION_SOURCE_ENSEMBL, GencodeGtfFeature.FeatureType.UTR,
                38792, 38793, Strand.NEGATIVE, GencodeGtfFeature.GenomicPhase.DOT, "ENSG00000215615.1", "ENST00000400681.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(),
                GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "AL354822.1", GencodeGTFFieldConstants.KnownGeneBiotype.PROTEIN_CODING.toString(), GencodeGTFFieldConstants.GeneTranscriptStatus.KNOWN.toString(), "AL354822.1-201", -1, null, GencodeGTFFieldConstants.LocusLevel.AUTOMATICALLY_ANNOTATED.toString(),
                new ArrayList<>(
                        Collections.singletonList(
                                new GencodeGtfFeature.OptionalField<String>("tag", GencodeGTFFieldConstants.FeatureTag.BASIC.toString())
                        )
                )
                );
        final GencodeGtfUTRFeature utr3 = (GencodeGtfUTRFeature) GencodeGtfFeature.create(data);

        // ======================
        // Here we'll set the version number of each feature.
        // Normally this is set in the decode method, so we need to do it manually.

        exon1.setUcscGenomeVersion("hg19");
        exon2.setUcscGenomeVersion("hg19");
        exon3.setUcscGenomeVersion("hg19");
        exon4.setUcscGenomeVersion("hg19");
        exon5.setUcscGenomeVersion("hg19");
        cds1.setUcscGenomeVersion("hg19");
        cds2.setUcscGenomeVersion("hg19");
        cds3.setUcscGenomeVersion("hg19");
        cds4.setUcscGenomeVersion("hg19");
        start_codon1.setUcscGenomeVersion("hg19");
        utr1.setUcscGenomeVersion("hg19");
        utr2.setUcscGenomeVersion("hg19");
        utr3.setUcscGenomeVersion("hg19");
        transcript1.setUcscGenomeVersion("hg19");
        gene1.setUcscGenomeVersion("hg19");
        
        // ======================
        // Now let's collapse these objects into their correct structure:

        exon2.setCds(cds1);
        exon2.setStartCodon(start_codon1);

        exon3.setCds(cds2);
        exon4.setCds(cds3);
        exon5.setCds(cds4);

        transcript1.addExon(exon1);
        transcript1.addExon(exon2);
        transcript1.addExon(exon3);
        transcript1.addExon(exon4);
        transcript1.addExon(exon5);

        transcript1.addUtr(utr1);
        transcript1.addUtr(utr2);
        transcript1.addUtr(utr3);

        gene1.addTranscript(transcript1);

        return gene1;
    }

    /**
     * Helper method to create data for the {@link DataProvider} {@link #decodeTestProvider()}
     * @return An {@link Object} array of size 2 containing a file name the object-representation of a file's data.
     */
    private Object[] createTestData_gencode_valid1() {
        return new Object[] {
            "gencode.valid1.gtf",
            new ArrayList<>( Collections.singletonList( createGencodeGtfGene_gencode_valid1()) ),
            "hg38"
        };
    }

    /**
     * Helper method to create data for the {@link DataProvider} {@link #decodeTestProvider()}
     * @return An {@link Object} array of size 2 containing a file name the object-representation of a file's data.
     */
    private Object[] createTestData_gencode_valid_gencode_file2() {

        return new Object[] {
                "gencode.valid_gencode_file2.gtf",
                new ArrayList<>( Collections.singletonList( createGencodeGtfGene_gencode_valid_gencode_file2()) ),
                "hg38"
        };
    }

    private Object[] createTestData_gencode_and_this_is_a_valid_one_too_table() {
        return new Object[] {
                "gencode.and.this.is.a.valid.one.too.table.gtf",
                new ArrayList<>( Collections.singletonList( createGencodeGtfGene_gencode_and_this_is_a_valid_one_too_table()) ),
                "hg38"
        };
    }

    private Object[] createTestData_gencode_v19_valid_file1() {
        return new Object[] {
                "gencode.v19.valid1.gtf",
                new ArrayList<>( Collections.singletonList( createGencodeGtfGene_gencode_v19_valid_file1() ) ),
                "hg19"
        };
    }

    private Object[] createTestData_gencode_v19_valid_gencode_file2() {
        return new Object[] {
                "gencode.v19.valid_gencode_file2.gtf",
                createGencodeGtfGene_gencode_v19_valid_gencode_file2(),
                "hg19"
        };
    }

    private Object[] createTestData_gencode_v19_and_this_is_a_valid_one_too() {
        return new Object[] {
                "gencode.v19.and.this.is.a.valid.one.too.gtf",
                new ArrayList<>( Collections.singletonList( createGencodeGtfGene_gencode_v19_and_this_is_a_valid_one_too() ) ),
                "hg19"
        };
    }

    // ============================================================================================================
    // ============================================================================================================
    // ============================================================================================================

    @DataProvider
    private Object[][] testIndexingProvider() {

        return new Object[][] {

                // HG38
                { xyzTestFile, "chrX", 105958620, 106070470, 1, },          // gene fits in region
                { xyzTestFile, "chrX", 106033442, 106034738, 1, },          // region fits in gene
                { xyzTestFile, "chrX", 106033442, 106070470, 1, },          // region start in gene
                { xyzTestFile, "chrX", 105958620, 106034738, 1, },          // region end in gene
                { xyzTestFile, "chrX", 1, 200000000, 2366, },               // Many genes in region
                { xyzTestFile, "chrX", 10000, 200000, 0, },                 // no genes in region

                // HG19
                { gencodeHg19TestFile, "chr1", 32198, 41014, 1, },          // gene fits in region
                { gencodeHg19TestFile, "chr1", 35001, 35500, 1, },          // region fits in gene
                { gencodeHg19TestFile, "chr1", 35500, 37000, 1, },          // region start in gene
                { gencodeHg19TestFile, "chr1", 32198, 35500, 1, },          // region end in gene
                { gencodeHg19TestFile, "chr1", 1, 200000000, 1982, },       // Many genes in region
                { gencodeHg19TestFile, "chr1", 33001, 34091, 0, },          // no genes in region

                // v43 test
                { gencodeHG38V43TestFile, "chr1", 32198, 41014, 1, },          // gene fits in region
                { gencodeHG38V43TestFile, "chr1", 35001, 35500, 1, },          // region fits in gene
                { gencodeHG38V43TestFile, "chr1", 35500, 37000, 1, },          // region start in gene
                { gencodeHG38V43TestFile, "chr1", 32198, 35500, 1, },          // region end in gene
                { gencodeHG38V43TestFile, "chr1", 1, 200000000, 677, },        // Many genes in region
                { gencodeHG38V43TestFile, "chr1", 33001, 34091, 0, },          // no genes in region


        };
    }

    @DataProvider
    private Object[][] toStringTestProvider() {

        // Hand-done results:
        final GencodeGtfGeneFeature gene = createGencodeGtfGene_gencode_valid1();

        final String expected = "chr1\tENSEMBL\tgene\t30366\t30503\t.\t+\t.\tgene_id \"ENSG00000284332.1\"; gene_type \"miRNA\"; gene_name \"MIR1302-2\"; level 3;\n" +
                "chr1\tENSEMBL\ttranscript\t30366\t30503\t.\t+\t.\tgene_id \"ENSG00000284332.1\"; transcript_id \"ENST00000607096.1\"; gene_type \"miRNA\"; gene_name \"MIR1302-2\"; transcript_type \"miRNA\"; transcript_name \"MIR1302-2-201\"; level 3; transcript_support_level \"NA\"; tag \"basic\";\n" +
                "chr1\tENSEMBL\texon\t30366\t30503\t.\t+\t.\tgene_id \"ENSG00000284332.1\"; transcript_id \"ENST00000607096.1\"; gene_type \"miRNA\"; gene_name \"MIR1302-2\"; transcript_type \"miRNA\"; transcript_name \"MIR1302-2-201\"; exon_number 1; exon_id \"ENSE00003695741.1\"; level 3; transcript_support_level \"NA\"; tag \"basic\";";

        return new Object[][] {
                {gene, expected},
        };
    }

    @DataProvider
    public Object[][] canDecodeProvider() {

        return new Object[][] {
                { "a.tsv"     , testResourceDir, false },                                    // Wrong File name / extension
                { "a.table.gz", testResourceDir, false },                                    // Wrong File name / extension
                { "a.bed"     , testResourceDir, false },                                    // Wrong File name / extension
                { "a.bcf"     , testResourceDir, false },                                    // Wrong File name / extension
                { "a.hapmap"  , testResourceDir, false },                                    // Wrong File name / extension
                { "a.refseq"  , testResourceDir, false },                                    // Wrong File name / extension
                { "a.beagle"  , testResourceDir, false },                                    // Wrong File name / extension
                { "a.table"   , testResourceDir, false },                                    // Wrong File name / extension

                { "gencode.v26.annotation.gtf.tsv", testResourceDir, false},                 // Wrong File name / extension
                { "gencode.v26.annotation.tgz"    , testResourceDir, false},                 // Wrong File name / extension
                { "gencode.v26.annotation.tar.gz" , testResourceDir, false},                 // Wrong File name / extension

                { "gencode.gtf"                                , testResourceDir, false},    // File does not exist
                { "gencode.v26.primary_assembly.annotation.gtf", testResourceDir, false},    // File does not exist
                { "gencode.v26.long_noncoding_RNAs.gtf"        , testResourceDir, false},    // File does not exist

                { "gencode.invalid_short_header.gtf"           , testResourceDir, false},    // File exists, has invalid header
                { "gencode.invalid_malformed_header.gtf"       , testResourceDir, false},    // File exists, has invalid header
                { "gencode.invalid_malformed_header_desc.gtf"  , testResourceDir, false},    // File exists, has invalid header
                { "gencode.invalid_malformed_header_prov.gtf"  , testResourceDir, false},    // File exists, has invalid header
                { "gencode.invalid_malformed_header_cont.gtf"  , testResourceDir, false},    // File exists, has invalid header
                { "gencode.invalid_malformed_header_form.gtf"  , testResourceDir, false},    // File exists, has invalid header
                { "gencode.invalid_malformed_header_date.gtf"  , testResourceDir, false},    // File exists, has invalid header

                { "gencode.valid1.gtf"                           , testResourceDir, true},   // Valid file
                { "gencode.valid_gencode_file2.gtf"              , testResourceDir, true},   // Valid file
                { "gencode.and.this.is.a.valid.one.too.table.gtf", testResourceDir, true},   // Valid file

                { "gencode.v43.LargeFile.gtf"                    , largeFileTestDir, true},   // Valid file - v43 (with previously disallowed tags)

                { "Escherichia_coli_str_k_12_substr_mg1655.ASM584v2.44.gtf", eColiTestDir, false},   // Not valid GENCODE GTF
        };
    }

    @DataProvider
    public Object[][] headerProvider() {
        return new Object[][] {

                { new ArrayList<String>(), false },                             // Wrong length header
                { Arrays.asList( "",
                                 "",
                                 "",
                                 "",
                                 ""  ),
                                 false },                                // Bad content
                { Arrays.asList( "##descr",
                                 "##provider: GENCODE",
                                 "##contact: gencode-help@sanger.ac.uk",
                                 "##format: gtf",
                                 "##date: 2017-04-08" ),
                                 false },                                // Bad header - description
                { Arrays.asList( "##description: THIS IS A SAMPLE",
                                 "##provider: GARBAGEDAY",
                                 "##contact: gencode-help@sanger.ac.uk",
                                 "##format: gtf",
                                 "##date: 2017-04-08" ),
                                false },                                // Bad header - provider
                { Arrays.asList( "##description: THIS IS A SAMPLE",
                                 "##provider: GENCODE",
                                 "##contact: gencode@NORTHPOLE.pl",
                                 "##format: gtf",
                                 "##date: 2017-04-08" ),
                                 false },                                // Bad header - contact
                { Arrays.asList( "##description: THIS IS A SAMPLE",
                                 "##provider: GENCODE",
                                 "##contact: SANTACLAUSE@sanger.ac.uk",
                                 "##format: gtf",
                                 "##date: 2017-04-08" ),
                                 false },                                // Bad header - contact
                { Arrays.asList( "##description: THIS IS A SAMPLE",
                                 "##provider: GENCODE",
                                 "##contact: gencode-help@sanger.ac.uk",
                                 "##format: dumpy",
                                 "##date: 2017-04-08" ),
                                false },                                // Bad header - format
                { Arrays.asList( "##description: THIS IS A SAMPLE",
                                 "##provider: GENCODE",
                                 "##contact: gencode-help@sanger.ac.uk",
                                 "##format: gtf",
                                 "##doom: ID Software" ),
                                 false },                                // Bad header - date
                { Arrays.asList( "##description: evidence-based annotation of the human genome (GRCh37), version 19 (Ensembl 74)",
                                 "##provider: GENCODE",
                                 "##contact: gencode@sanger.ac.uk",
                                 "##format: gtf",
                                 "##date: 2014-07-25" ),
                                 true },                                // Good Header!
                { Arrays.asList( "##description: evidence-based annotation of the human genome (GRCh38), version 26 (Ensembl 88)",
                                 "##provider: GENCODE",
                                 "##contact: gencode-help@sanger.ac.uk",
                                 "##format: gtf",
                                 "##date: 2014-07-25" ),
                                 true },                                 // Good Header!

                // -------------

                { Arrays.asList( "#!genome-build ASM584v2",
                        "#!genome-version ASM584v2",
                        "#!genome-date 2014-08",
                        "#!genome-build-accession GCA_000005845.2",
                        "#!genebuild-last-updated 2014-08" ),
                        false },                                           // Good ENSEMBL GTF Header, bad GENCODE header!

                { Arrays.asList( "ASM584v2",
                        "#!genome-version ASM584v2",
                        "#!genome-date 2014-08",
                        "#!genome-build-accession GCA_000005845.2",
                        "#!genebuild-last-updated 2014-08" ),
                        false },                                           // Bad header - genome-build
                { Arrays.asList( "#!genome-build ASM584v2",
                        "ASM584v2",
                        "#!genome-date 2014-08",
                        "#!genome-build-accession GCA_000005845.2",
                        "#!genebuild-last-updated 2014-08" ),
                        false },                                           // Bad header - genome-version
                { Arrays.asList( "#!genome-build ASM584v2",
                        "#!genome-version ASM584v2",
                        "#2014-08",
                        "#!genome-build-accession GCA_000005845.2",
                        "#!genebuild-last-updated 2014-08" ),
                        false },                                           // Bad header - genome-date
                { Arrays.asList( "#!genome-build ASM584v2",
                        "#!genome-version ASM584v2",
                        "#!genome-date 2014-08",
                        "#GCA_000005845.2",
                        "#!genebuild-last-updated 2014-08" ),
                        false },                                           // Bad header - genome-build-accession
                { Arrays.asList( "#!genome-build ASM584v2",
                        "#!genome-version ASM584v2",
                        "#!genome-date 2014-08",
                        "#!genome-build-accession GCA_000005845.2",
                        "#2014-08" ),
                        false },                                           // Bad header - genebuild-last-updated

                { Arrays.asList( "##description: evidence-based annotation of the human genome (GRCh38), version 26 (Ensembl 88)",
                        "##provider: GENCODE",
                        "##contact: gencode-help@sanger.ac.uk",
                        "##date: 2014-07-25" ),
                        false },                                 // Bad Gencode GTF Header - too few lines.

        };
    }

    @DataProvider
    public Object[][] decodeTestProvider() {

        return new Object[][] {
                createTestData_gencode_valid1(),
                createTestData_gencode_valid_gencode_file2(),
                createTestData_gencode_and_this_is_a_valid_one_too_table(),
                createTestData_gencode_v19_valid_file1(),
                createTestData_gencode_v19_valid_gencode_file2(),
                createTestData_gencode_v19_and_this_is_a_valid_one_too()
        };
    }

    @DataProvider
    private Object[] createTestData_gencode_arbitrary_9th_column_fields(){

        final String templateFile = "";

        // Testing arbitary biotypes here with random values so that we ensure that any string can be
        // a valid biotype.
        // This is so that we can "future-proof" the codec to allow for any biotypes that have not yet
        // been defined in our datasources.  Since we do not know what the possible values can be, we should
        // make sure to test even some extreme cases.
        return new Object[][] {
                { "gene_type", "GAJUAEWJIWIEG", "gencode.biotype_template.gtf", testResourceDir },
                { "gene_type", "2e#H!FXh&F!N4xn$VD*e", "gencode.biotype_template.gtf", testResourceDir },
                { "gene_type", "G23&#MPqP6gveAOcakY2", "gencode.biotype_template.gtf", testResourceDir },
                { "gene_type", "eGPvPbMcHYSbMoUDd@TY", "gencode.biotype_template.gtf", testResourceDir },
                { "gene_type", "9SjyZMBAXY=Gg9eAyk62", "gencode.biotype_template.gtf", testResourceDir },
                { "gene_type", "F9rt#9p9Ggex4dXs=EYO", "gencode.biotype_template.gtf", testResourceDir },
                { "transcript_type", "GAJUAEWJIWIEG", "gencode.biotype_template.gtf", testResourceDir },
                { "transcript_type", "2e#H!FXh&F!N4xn$VD*e", "gencode.biotype_template.gtf", testResourceDir },
                { "transcript_type", "G23&#MPqP6gveAOcakY2", "gencode.biotype_template.gtf", testResourceDir },
                { "transcript_type", "eGPvPbMcHYSbMoUDd@TY", "gencode.biotype_template.gtf", testResourceDir },
                { "transcript_type", "9SjyZMBAXY=Gg9eAyk62", "gencode.biotype_template.gtf", testResourceDir },
                { "transcript_type", "F9rt#9p9Ggex4dXs=EYO", "gencode.biotype_template.gtf", testResourceDir },

                // Optional and non-optional fields we are opening up to arbitrary values
                { "tag", "2e#H!FXh&F!N4xn$VD*e", "gencode.biotype_template.gtf", testResourceDir },
                { "tag", "G23&#MPqP6gveAOcakY2", "gencode.biotype_template.gtf", testResourceDir },
                { "gene_id", "2e#H!FXh&F!N4xn$VD*e", "gencode.biotype_template.gtf", testResourceDir },
                { "transcript_id", "G23&#MPqP6gveAOcakY2", "gencode.biotype_template.gtf", testResourceDir },
                { "gene_status", "2e#H!FXh&F!N4xn$VD*e", "gencode.biotype_template.gtf", testResourceDir },
                { "transcript_type", "G23&#MPqP6gveAOcakY2", "gencode.biotype_template.gtf", testResourceDir },
                { "transcript_status", "G23&#MPqP6gveAOcakY2", "gencode.biotype_template.gtf", testResourceDir },
                { "transcript_name", "G23&#MPqP6gveAOcakY2", "gencode.biotype_template.gtf", testResourceDir },
                { "havana_gene", "G23&#MPqP6gveAOcakY2", "gencode.biotype_template.gtf", testResourceDir },
                { "remap_status", "G23&#MPqP6gveAOcakY2", "gencode.biotype_template.gtf", testResourceDir },
                { "remap_original_id", "G23&#MPqP6gveAOcakY2", "gencode.biotype_template.gtf", testResourceDir },
                { "remap_target_status", "G23&#MPqP6gveAOcakY2", "gencode.biotype_template.gtf", testResourceDir },
                { "transcript_support_level", "G23&#MPqP6gveAOcakY2", "gencode.biotype_template.gtf", testResourceDir },
                { "havana_transcript", "G23&#MPqP6gveAOcakY2", "gencode.biotype_template.gtf", testResourceDir },
                { "level", "G23&#MPqP6gveAOcakY2", "gencode.biotype_template.gtf", testResourceDir },
                { "exon_id", "G23&#MPqP6gveAOcakY2", "gencode.biotype_template.gtf", testResourceDir },
        };
    }
    // =============================================================================================================

    @Test(dataProvider = "canDecodeProvider")
    public void testCanDecode(final String fileName, final String containingFolder, final boolean expected) {
        final GencodeGtfCodec gencodeGtfCodec = new GencodeGtfCodec();
        Assert.assertEquals(gencodeGtfCodec.canDecode(containingFolder + fileName), expected, fileName);
    }

    @Test(dataProvider = "headerProvider")
    public void testValidateHeader( final List<String> header, final boolean expected ) {
        final GencodeGtfCodec gencodeGtfCodec = new GencodeGtfCodec();
        Assert.assertEquals( gencodeGtfCodec.validateHeader(header), expected );
    }

    @Test(dataProvider = "decodeTestProvider")
    public void testDecode( final String filePath, final List<GencodeGtfFeature> expected, final String expectedUcscVersion) throws IOException {
        final GencodeGtfCodec gencodeGtfCodec = new GencodeGtfCodec();

        try (final BufferedInputStream bufferedInputStream =
                     new BufferedInputStream(
                             new FileInputStream(testResourceDir + filePath)
                     )
        ) {
            // Get the line iterator:
            final LineIterator lineIterator = gencodeGtfCodec.makeSourceFromStream(bufferedInputStream);

            // Get the header (required for the read to work correctly):
            gencodeGtfCodec.readHeader(lineIterator);

            // Setup our expected data iterator:
            final Iterator<GencodeGtfFeature> expectedIterator = expected.iterator();

            // Now read our features and make sure they're what we expect:
            int numDecoded = 0;
            while ( lineIterator.hasNext() ) {
                final GencodeGtfFeature feature = gencodeGtfCodec.decode(lineIterator);

                Assert.assertTrue(expectedIterator.hasNext());

                for ( final GencodeGtfFeature subFeature : feature.getAllFeatures() ) {
                    Assert.assertEquals(subFeature.getUcscGenomeVersion(), expectedUcscVersion);
                }
                Assert.assertEquals(feature, expectedIterator.next());

                ++numDecoded;
            }

            Assert.assertEquals(numDecoded, expected.size());
        }
    }

    // helper function to replace a field in a GTF line
    private static String replaceGTFField(final String input, final String fieldName, final String newValue) {
        String newGTFLine;
        if (fieldName.equals("level")){
            String regex = fieldName + "\s*[^\"]*;";
            // Perform the replacement
            newGTFLine = input.replaceAll(regex, fieldName + " " + newValue.replace("$", "\\$") + ";");
        } else {
            // Regex pattern to match the field
            String regex = fieldName + "\s*\"[^\"]*\";";
            // Perform the replacement
            newGTFLine = input.replaceAll(regex, fieldName + " \"" + newValue.replace("$", "\\$") + "\";");
        }
        return newGTFLine;
    }

    @Test(dataProvider = "createTestData_gencode_arbitrary_9th_column_fields")
    public void testDecodeCanParseArbitraryFields(final String field,
                                                  final String newValue,
                                                  final String templateFile,
                                                  final String containingFolder) throws IOException {
        // 1 - write out a file with the given biotype:
        final String templateContents = new String(
                Files.readAllBytes(IOUtils.getPath(containingFolder + File.separator + templateFile))
        );
        final File testGtfFile = IOUtils.writeTempFile(
                replaceGTFField(templateContents, field, newValue),
                "testArbitraryBiotype",
                "gtf"
        );

        // 2 -Read in the template file and make sure it doesn't fail:
        final GencodeGtfCodec gencodeGtfCodec = new GencodeGtfCodec();
        try (final BufferedInputStream bufferedInputStream =
                     new BufferedInputStream(
                             new FileInputStream(testGtfFile)
                     )
        ) {
            // Get the line iterator:
            final LineIterator lineIterator = gencodeGtfCodec.makeSourceFromStream(bufferedInputStream);

            // Get the header (required for the read to work correctly):
            gencodeGtfCodec.readHeader(lineIterator);

            // Now read the whole file and make sure it works:
            while ( lineIterator.hasNext() ) {
                final GencodeGtfFeature feature = gencodeGtfCodec.decode(lineIterator);

                feature.getAllFeatures().forEach(f -> {
                    //todo for now we special case the required field accessors
                    switch (field) {
                        case "gene_type":
                            Assert.assertEquals(f.getGeneType(), newValue);
                            break;
                        case "transcript_type":
                            Assert.assertEquals(f.getTranscriptType(), newValue);
                            break;
                        case "gene_status":
                            Assert.assertEquals(f.getGeneStatus(), newValue);
                            break;
                        case "transcript_status":
                            Assert.assertEquals(f.getTranscriptStatus(), newValue);
                            break;
                        case "level":
                            Assert.assertEquals(f.getLocusLevel(), newValue);
                            break;
                        case "exon_id":
                            if (f.getFeatureType() == GencodeGtfFeature.FeatureType.EXON) {
                                Assert.assertEquals(f.getExonId(), newValue);
                            }
                            break;
                        default:
                            for(GencodeGtfFeature.OptionalField<?> optionalTag : f.getOptionalField(field)) {
                                Assert.assertEquals(optionalTag.getValue(), newValue);
                            }
                            break;
                    }
                });
            }
        }
    }

    @Test(dataProvider = "toStringTestProvider")
    public void testToString( final GencodeGtfFeature feature, final String expected ) {
        Assert.assertEquals(feature.serializeToString(), expected);
    }

    @Test(dataProvider = "testIndexingProvider")
    public void testIndexing( final String fileName, final String contig, final int start, final int end, final int numExpectedGenes ) {

        final File gencodeTestFile = new File(fileName);
        testIndexHelper(contig, start, end, numExpectedGenes, gencodeTestFile);
    }

    @Test(dataProvider = "testIndexingProvider")
    public void testIndexingAndIndexCreation( final String fileName,
                                              final String contig,
                                              final int start,
                                              final int end,
                                              final int numExpectedGenes ) throws IOException {

        final GencodeGtfCodec codec = new GencodeGtfCodec();

        // Create a temp dir:
        final File tmpDir = createTempDir("testIndexingAndIndexCreation_" + start + "_" + end);

        // Create a copy of our index file:
        final File originalTestFile = new File(fileName);
        final File testFile = new File(tmpDir.getAbsolutePath() + File.separator + originalTestFile.getName());

        // Copy our file to the tmp dir:
        Files.copy(originalTestFile.toPath(), testFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        // Create our Index:
        final File indexFile = Tribble.indexFile(testFile);
        final Index index = IndexFactory.createDynamicIndex(testFile, codec, IndexFactory.IndexBalanceApproach.FOR_SEEK_TIME);
        index.write(indexFile);

        // Make sure it works:
        testIndexHelper(contig, start, end, numExpectedGenes, testFile);
    }
}
