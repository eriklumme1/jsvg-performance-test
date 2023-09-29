import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.geometry.size.FloatSize;
import com.github.weisj.jsvg.parser.SVGLoader;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class JsvgRenderTest {

    private static final int NUM_ITERATIONS = 10;

    @ParameterizedTest
    @ValueSource(strings = {"android.svg", "gallardo.svg", "islands.svg", "micelle.svg", "pencil.svg",
            "propane.svg", "venus.svg", "world.svg"})
    void testPerformance(String filename) throws IOException {
        URL url = getClass().getResource("/" + filename);

        long startTime = System.nanoTime();

        SVGLoader svgLoader = new SVGLoader();
        SVGDocument svgDocument = svgLoader.load(url);
        FloatSize size = svgDocument.size();

        long endTime = System.nanoTime();

        printTime("Create document for '" + filename + "'", startTime, endTime, 1);

        BufferedImage image = new BufferedImage((int) size.width, (int) size.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = image.createGraphics();

        startTime = System.nanoTime();
        for (int i = 0; i < NUM_ITERATIONS; i++) {
            svgDocument.render(null, graphics2D);
        }
        endTime = System.nanoTime();

        printTime("Rendered " + NUM_ITERATIONS + " iterations of '" + filename + "'", startTime, endTime, NUM_ITERATIONS);

        saveOutputImage(url, image);

        confirmChecksums(filename);
    }

    private void printTime(String caption, long startTime, long endTime, int numIterations) {
        double timeMs = (endTime - startTime) / 1000000.0 / numIterations;
        System.out.printf("%s in %.2f ms per iteration%n", caption, timeMs);
    }

    private void saveOutputImage(URL url, BufferedImage outputImage) throws IOException {
        String outputPath = url.getPath().replace(".svg", "-result.png");
        File resultFile = new File(outputPath);
        boolean writeResult = ImageIO.write(outputImage, "png", resultFile);

        if (writeResult) {
            System.out.println("Result of last render written to '" + outputPath + "'");
        } else {
            System.out.println("Unable to save output image, ImageIO.write returned 'false'");
        }
    }

    private void confirmChecksums(String filename) {
        String resultFile = filename.replace(".svg", "-result.png");
        String referenceFile = filename.replace(".svg", "-reference.png");

        try {
            URL resultURL = getClass().getResource(resultFile);
            URL referenceURL = getClass().getResource(referenceFile);

            if (referenceURL == null) {
                System.err.println("No reference image found for '" + filename + "', can't compare checksums");
                return;
            }

            byte[] resultData = Files.readAllBytes(Paths.get(resultURL.toURI()));
            byte[] resultHash = MessageDigest.getInstance("MD5").digest(resultData);

            byte[] referenceData = Files.readAllBytes(Paths.get(referenceURL.toURI()));
            byte[] referenceHash = MessageDigest.getInstance("MD5").digest(referenceData);

            if (Arrays.equals(resultHash, referenceHash)) {
                System.out.println("Confirm hash of output file matches reference file for '" + filename + "'");
            } else {
                System.err.println("Hash of output file does not match reference file for '" + filename + "'");
            }
        } catch (URISyntaxException | NullPointerException | IOException | NoSuchAlgorithmException e) {
            System.out.println("Unable to confirm checksums for file '" + filename + "': " + e.getMessage());
            e.printStackTrace();
        }
    }
}
