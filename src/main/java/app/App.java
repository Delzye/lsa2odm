package app;

// import org.apache.log4j.BasicConfigurator;

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

    public static void main(String[] args)
    {
		// BasicConfigurator.configure();
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

	public static void invalid_params()
	{
		log.info("Usage: java -jar <lsa2odm-jar-name> <.lsa-File> <Output-Path (optional)>");
	}
}
