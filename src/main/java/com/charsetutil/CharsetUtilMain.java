package com.charsetutil;

import java.io.File;
import java.io.IOException;

import org.mozilla.universalchardet.UniversalDetector;

public class CharsetUtilMain {

	public static void main(String[] args) throws IOException {
		File file = new File("C:\\Users\\huangcheng19079\\Desktop\\RELEASE");

		for (File deteFile : file.listFiles()) {
			System.out.println("detecte:" + deteFile.getName());
			byte[] buf = new byte[4096];
			java.io.InputStream fis = java.nio.file.Files.newInputStream(java.nio.file.Paths.get(deteFile.getAbsolutePath()));

			// (1)
			UniversalDetector detector = new UniversalDetector(null);

			// (2)
			int nread;
			while ((nread = fis.read(buf)) > 0 && !detector.isDone()) {
				detector.handleData(buf, 0, nread);
			}
			// (3)
			detector.dataEnd();

			// (4)
			String encoding = detector.getDetectedCharset();
			if (encoding != null) {
				System.out.println("Detected encoding = " + encoding);
			} else {
				System.out.println("No encoding detected.");
			}

			// (5)
			detector.reset();
		}
	}

}
