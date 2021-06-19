package utils;

import java.io.File;
import java.io.IOException;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.SAXException;

import lombok.extern.log4j.Log4j;

@Log4j
public class LssValidator
{
	public static void validateFile(File lss_file) throws SAXException, IOException
	{
	    SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1");
	
	    File schemaLocation = new File("src/main/xml/lss.xsd");
	    Schema schema = factory.newSchema(schemaLocation);
	
	    Validator validator = schema.newValidator();
	    Source source = new StreamSource(lss_file);
	
	    try
	    {
	        validator.validate(source);
			log.info("##################################################################################");
	        log.info(lss_file.getName() + " is valid.");
			log.info("##################################################################################");
	    }
	    catch (SAXException ex)
	    {
			log.info("##################################################################################");
	        log.info(lss_file.getName() + " is not valid because ");
			log.info(ex.getMessage());
			log.info("##################################################################################");
	    }
	}
}
