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

		log.info("Checking Filepaths");
		File f = new File(p1_lsa_path);
		String ext = f.getName().substring(f.getName().lastIndexOf(".") + 1);

		if(!f.isFile() || !ext.equals("lsa")) {
			invalid_params();
		}

		log.info("Filename is valid");
		log.debug("Path: " + f.getParent());
		log.debug("Filename: " + f.getName());

		String lsa_path = f.getParent();
		String output_path = p2_output_path.equals("") ? lsa_path : p2_output_path;

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
		System.exit(1);
	}
}
