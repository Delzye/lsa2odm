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

	// https://www.baeldung.com/java-compress-and-uncompress
	public static void unzipFile(String path, String folder_name)
	{
		String fileZip = path;
        File destDir = new File(folder_name);
		destDir.mkdirs();
        byte[] buffer = new byte[1024];
		try {
			ZipInputStream zis = new ZipInputStream(new FileInputStream(fileZip));
			ZipEntry zipEntry = zis.getNextEntry();
			while (zipEntry != null) {
				File newFile = newFile(destDir, zipEntry);
				// write file content
				FileOutputStream fos = new FileOutputStream(newFile);
				int len;
				while ((len = zis.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}
				fos.close();
				zipEntry = zis.getNextEntry();
			}
			zis.closeEntry();
			zis.close();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	
	public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
    File destFile = new File(destinationDir, zipEntry.getName());

    String destDirPath = destinationDir.getCanonicalPath();
    String destFilePath = destFile.getCanonicalPath();
	log.info(destFilePath);
	if (destFilePath.contains(".lss")) {
		lss_file = destFile;
	} else if (destFilePath.contains(".lsr")) {
		lsr_file = destFile;
	}

    if (!destFilePath.startsWith(destDirPath + File.separator)) {
        throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
    }

    return destFile;
	}
}
