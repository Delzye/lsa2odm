/* MIT License

Copyright (c) 2021 Anton Mende (Delzye)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE. */
package utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import lombok.Getter;
import lombok.extern.log4j.Log4j;

@Log4j
public class ZipUtils
{
	@Getter protected static File lss_file;
	@Getter protected static File lsr_file;
	@Getter protected static File lst_file;

	// https://www.baeldung.com/java-compress-and-uncompress
	public static void unzipFile(String path, String folder_name)
	{
		String zip_path = path;
        File dest_directory = new File(folder_name);
		dest_directory.mkdirs();
        byte[] buf = new byte[2048];

		try {
			ZipInputStream zis = new ZipInputStream(new FileInputStream(zip_path));
			ZipEntry zip_entry = zis.getNextEntry();

			while (zip_entry != null) {
				File file = newFile(dest_directory, zip_entry);
				// write file content
				FileOutputStream fos = new FileOutputStream(file);
				int len;
				while ((len = zis.read(buf)) > 0) {
					fos.write(buf, 0, len);
				}
				fos.close();
				zip_entry = zis.getNextEntry();
			}

			zis.closeEntry();
			zis.close();
		} catch (Exception e) {
			log.error(e.getMessage());
			System.exit(1);
		}
	}
	
	public static File newFile(File output_directory, ZipEntry zip_entry) throws IOException {
    File file = new File(output_directory, zip_entry.getName());

    String dest_dir_path = output_directory.getCanonicalPath();
    String dest_file_path = file.getCanonicalPath();

	log.debug(dest_file_path);
	if (dest_file_path.contains(".lss")) {
		lss_file = file;
	} else if (dest_file_path.contains(".lsr")) {
		lsr_file = file;
	} else if (dest_file_path.contains(".lst")) {
		lst_file = file;
	}

    if (!dest_file_path.startsWith(dest_dir_path + File.separator)) {
        throw new IOException("Entry is not in target directory: " + zip_entry.getName());
    }

    return file;
	}
}
