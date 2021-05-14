package app;

import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import org.apache.log4j.BasicConfigurator;
import org.xml.sax.SAXException;

import lombok.extern.log4j.Log4j;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.Source;

import writer.ODMWriter;
import parser.LssParser;
import parser.LsrParser;

@Log4j
public class App {
 
    public static void main(String[] args)
    {
		BasicConfigurator.configure();
		// Pattern lss_pattern = Pattern.compile("^/?[.*/]*.*\\.lss$", Pattern.CASE_INSENSITIVE);
		// Matcher lss_matcher = lss_pattern.matcher(args[0]);
		// if(lss_matcher.find()) {
		// 	System.out.println("Match found");
		// } else {
		// 	System.out.println("Match not found");
		// }
		long beforeUsedMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

		String filename = "trivial_text";
		File lss_file = new File("src/main/xml/" + filename + "_example.lss");
		File lsr_file = new File("src/main/xml/" + filename + "_responses_long.lsr");
		File xsd_file = new File("src/main/xml/lss.xsd");
		try{
			validateFile(lss_file, xsd_file);	
		} catch (IOException | SAXException e) {
			log.error(e.getMessage());
		}
		// Parse .lss document
		LssParser lss_parser = new LssParser(lss_file);
		lss_parser.parseDocument();

		ODMWriter odm = new ODMWriter(lss_parser.getSurvey());
		odm.createODMFile();

		// Parse .lsr document
		LsrParser lsr_parser = new LsrParser(lsr_file, lss_parser.getSurvey().getGroups(), lss_parser.getDate_time_qids(), odm);
		lsr_parser.createDocument();
		lsr_parser.parseAnswers();

		// Write ODM file
		odm.writeFile();

		// Performance Output:
		long afterUsedMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		log.info("Utilized memory: " + ((afterUsedMem-beforeUsedMem)/(1024*1024)) + "mb");
	}

	private static void validateFile(File xmlFile, File xsdFile) throws SAXException, IOException
	{
	    SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1");
	
	    File schemaLocation = xsdFile;
	    Schema schema = factory.newSchema(schemaLocation);
	
	    Validator validator = schema.newValidator();
	    Source source = new StreamSource(xmlFile);
	
	    try
	    {
	        validator.validate(source);
	        System.out.println(xmlFile.getName() + " is valid.");
	    }
	    catch (SAXException ex)
	    {
	        log.info(xmlFile.getName() + " is not valid because ");
	        log.info(ex.getMessage());
	    }
	}
}
