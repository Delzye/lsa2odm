package app;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;
import org.xml.sax.SAXException;
import java.io.File;
import java.io.IOException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.Source;

import writer.ODMWriter;
import parser.LssParser;
import parser.LsrParser;

public class App {
 
    private static final Logger logger = Logger.getLogger(App.class);
 
    public static void main(String[] args)
    {
        //Configure logger
        BasicConfigurator.configure();
		File lss_file = new File("src/main/xml/trivial_text_example.lss");
		File lsr_file = new File("src/main/xml/trivial_text_responses.lsr");
		File xsd_file = new File("src/main/xml/lss.xsd");
		try{
			validateFile(lss_file, xsd_file);	
		} catch (IOException | SAXException e) {
			logger.error(e.getMessage());
		}
		// Parse .lss document
		LssParser lss_parser = new LssParser(lss_file);
		lss_parser.parseDocument();

		// Parse .lsr document
		LsrParser lsr_parser = new LsrParser(lsr_file, lss_parser.getSurvey().getGroups());
		lsr_parser.createDocument();
		lsr_parser.parseAnswers();

		// Write ODM file
		ODMWriter odm = new ODMWriter(lss_parser.getSurvey(), lsr_parser.getResponses());
		odm.createODMFile();
		odm.writeFile();
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
	        System.out.println(xmlFile.getName() + " is not valid because ");
	        System.out.println(ex.getMessage());
	    }
	}
}
