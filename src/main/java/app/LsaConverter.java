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

import org.xml.sax.SAXException;

import lombok.extern.log4j.Log4j;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import parser.lss.LssParser;
import parser.lsr.LsrParser;
import parser.lsr.Response;
import utils.ZipUtils;
import utils.LssValidator;
import writer.ODMWriter;

@Log4j
public class LsaConverter
{
	/**
	 * <p>Convert a lsa archive to ODM and save the file to p2_output_path</p>
	 * @param p1_lsa_path Path to the lsa archive
	 * @param p2_output_path Where to put the result
	 *
	 * */
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
