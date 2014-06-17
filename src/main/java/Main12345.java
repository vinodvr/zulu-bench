import org.apache.commons.io.FileUtils;
import org.xerial.snappy.Snappy;

import java.io.File;

/**
 * To change this template use File | Settings | File Templates.
 */
public class Main12345 {

    public static void main(String[] args) throws Exception {

        String str = FileUtils.readFileToString(new File("/home/vinodvr/tmp/sample/small.json"));

        for (int i = 0; i < 100; i++) {
            System.out.println("************");
            long st = System.currentTimeMillis();
            byte[] compress = Snappy.compress(str);
            System.out.println("Time taken for compress: (ms)" + (System.currentTimeMillis() - st) + ", compressedSize: " + compress.length);

            double x = ((double)str.length()) / compress.length;
            System.out.println(x);

            st = System.currentTimeMillis();
            byte[] uncompress = Snappy.uncompress(compress);
            System.out.println("Time taken for uncompress: (ms)" + (System.currentTimeMillis() - st));
        }
    }
}
