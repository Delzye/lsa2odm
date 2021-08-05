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
import java.io.IOException;
import java.io.InputStream;

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
	/**
	 * <p>Check if the parameter is a valid XML-File when checked against the lss.xsd file. Results are logged to console</p>
	 * @param lss_file The file to check
	 * */
	public static void validateFile(File lss_file) throws SAXException, IOException
	{
	    SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1");
	
		InputStream is = LssValidator.class.getResourceAsStream("/lss.xsd");
		Source schemaLocation = new StreamSource(is);
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
