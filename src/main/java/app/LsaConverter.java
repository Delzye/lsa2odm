package app;

import org.xml.sax.SAXException;

import lombok.extern.log4j.Log4j;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import parser.lss.LssParser;
import parser.lsr.LsrParser;
import parser.lsr.Response;
import utils.ZipUtils;
import utils.LssValidator;
import writer.ODMWriter;

@Log4j
public class LsaConverter
{
	public static void convert(String p1_lsa_path, String p2_output_path)
	{		
		File lss_file;
		File lsr_file;
		File lst_file;

		// Check if the first parameter is actually a lsa path
		String fs = File.separator;
		char fsc = File.separatorChar;
		Pattern lsa_pattern = Pattern.compile("^(\\.?"+fs+"?(?:.*"+fs+")*)(.*?)\\.lsa$", Pattern.CASE_INSENSITIVE);
		Matcher lsa_matcher = lsa_pattern.matcher(p1_lsa_path);
		log.info("Checking Filepaths");

		if(!lsa_matcher.find()) {
			invalid_params();
		}

		log.info("LSA-Filename is valid");
		log.debug("Path: " + lsa_matcher.group(1));
		log.debug("Filename: " + lsa_matcher.group(2));

		String lsa_path = lsa_matcher.group(1);
		String output_path = p2_output_path.equals("") ? lsa_path : p2_output_path;
		// Path must end with a '/' (unix) or '\' (windows)
		output_path += output_path.charAt(output_path.length()-1) == fsc ? "" : fs;
		log.info("Unzipping archive");
		ZipUtils.unzipFile(p1_lsa_path, output_path);

		lss_file = ZipUtils.getLss_file();
		lsr_file = ZipUtils.getLsr_file();
		lst_file = ZipUtils.getLst_file();

		if (lss_file == null || lsr_file == null) {
			log.error("Could not find lss and lsr file!");
			System.exit(1);
		}
		lss_file.deleteOnExit();
		lsr_file.deleteOnExit();
		if(lst_file != null){ 
			lst_file.deleteOnExit();
		}

//=================================================================validation============================================================================
		try{
			LssValidator.validateFile(lss_file);	
		} catch (IOException | SAXException e) {
			log.error(e.getMessage());
		}
//==================================================================parsing==============================================================================
		// Parse .lss document
		LssParser lss_parser = new LssParser(lss_file);
		lss_parser.parseDocument();


		// Parse .lsr document
		LsrParser lsr_parser = new LsrParser(lsr_file, lss_parser.getSurvey().getGroups(), lss_parser.getDate_time_qids());
		lsr_parser.createDocument();
		ArrayList<Response> responses = lsr_parser.parseAnswers();

		// Write ODM file
		ODMWriter odm = new ODMWriter(lss_parser.getSurvey());
		odm.createODMFile();
		odm.addAnswers(responses);
		odm.writeFile(output_path);
	}

	public static void invalid_params()
	{
		log.info("Usage: java -jar <lsa2odm-jar-name> <.lsa-File> <Output-Path (optional)>");
	}
}
