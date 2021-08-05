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
package app;

import lombok.extern.log4j.Log4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Properties;

@Log4j
public class App {
	protected static File lss_file;
	protected static File lsr_file;

	/**
	 *<p>Main Method: Call a converter for the file in the first command line parameter and save the output to the optional second path</p>
	 *
	 * */
    public static void main(String[] args)
    {
		long beforeUsedMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

		// Properties File
		propsConfig();

		// There have to be 1 or 2 parameters, LSA-File and output path
		if (args.length == 0 || args.length > 2) {
			invalid_params();
		}
		String p2 = args.length == 2 ? args[1] : "";
		LsaConverter.convert(args[0], p2);

		// Performance Output:
		long afterUsedMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		log.info("Utilized memory: " + ((afterUsedMem-beforeUsedMem)/(1024*1024)) + "mb");
	}

	/**
	 *<p> Add a config file for properties, if one does not exist yet </p>
	 *
	 * */
	private static void propsConfig()
	{
		String fs = File.separator;
		File file = new File("src" + fs + "main" + fs + "resources" + fs + "config.properties");
		if (!file.exists()) {
			try (OutputStream output = new FileOutputStream("src" + fs + "main" + fs + "resources" + fs + "config.properties")) {

            Properties prop = new Properties();

            // set the properties value
            prop.setProperty("dummy.survey_oid", "PlaceholderOID");
            prop.setProperty("dummy.study_event_oid", "Event.1");
            prop.setProperty("dummy.study_event_name", "StudyEvent.1");
			prop.setProperty("dummy.study_name", "StudyPlaceholderName");
			prop.setProperty("dummy.protocol_name", "StudyProtocolPlaceholder");
            prop.setProperty("ext.cond", ".cond");
			prop.setProperty("odm.meta_data_prefix", "MetaData");
            prop.setProperty("imi.syntax_name", "imi");

            prop.store(output, null);

			} catch (IOException io) {
				io.printStackTrace();
			}
		}
	}

	/**
	 *<p>Inform the user that the program expects different parameters and exit the application</p>
	 *
	 * */
	public static void invalid_params()
	{
		log.info("Usage: java -jar <lsa2odm-jar-name> <.lsa-File> <Output-Path (optional)>");
		System.exit(1);
	}
}
