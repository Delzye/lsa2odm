package parser.lsr;

import writer.ODMWriter;
import parser.lss.QuestionGroup;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Getter;
import lombok.extern.log4j.Log4j;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

@Log4j
public class LsrParser
{
	File lsr_file;
	Document doc;
	Element rows;
	ArrayList<QuestionGroup> qg_list;
	@Getter LinkedList<Response> responses;
	List<String> date_time_qids;
	ODMWriter writer;

	public LsrParser(File lsr_file, ArrayList<QuestionGroup> qg_list, ArrayList<String> dt_qids, ODMWriter writer)
	{
		this.lsr_file = lsr_file;
		this.qg_list = qg_list;
		responses = new LinkedList<Response>();
		this.date_time_qids = dt_qids;
		this.writer = writer;
	}

	public void createDocument()
	{
		try {
		SAXReader saxReader = new SAXReader();
		doc = saxReader.read(lsr_file); 
		} catch (Exception e) {
			log.error("Could not read lsr_file: " + e.getMessage());
		}
	}

	public void parseAnswers()
	{
		@SuppressWarnings("unchecked")
		List<Element> row_list = doc.selectNodes("//document/responses/rows/row");

		log.debug(date_time_qids);
		
		int x = 0;
		// Iterate over all row-Elements (one per survey-participant)
		for (Element row : row_list) {

			Response r = new Response();
			r.id = Integer.parseInt(row.element("id").getText());
			
			// Sort Answers by the question group the question belongs to
			for (QuestionGroup qg : qg_list) {
				r.answers.put(qg.getGid(), new ArrayList<Answer>());
			}
			@SuppressWarnings("unchecked")
			List<Element> children = row.elements();

			// Iterate over all elements in row (most of which are answers, but also id, language etc.)
			for (Element e : children) {

				// Pattern for Answer-Names: _{Survey_id}X{gid}X{qid}{subquestion_title}?{ext}?
				// Where ext can be one of "other|comment"
				Pattern ans_p = Pattern.compile("^_\\d+X(\\d+)X(.+?)$");
				log.debug(e.getName());
				Matcher e_match = ans_p.matcher(e.getName());

				// If this is an answer and not another element
				if (e_match.find()) {
					log.debug("Found matching Element, parsing first group: " + e_match.group(1) + " Second group: " + e_match.group(2));
					String gid = e_match.group(1);
					String qid = e_match.group(2);

					log.info("Working on question: " + qid);

					String ans = e.getText();

					// only add an answer if it isn't empty
					if (ans != "") { 
						if (date_time_qids.contains(qid)) {
							ans = ans.replace(" ", "T");
						}
						Answer a = new Answer(Integer.parseInt(gid), qid, ans);
						r.addToAnswers(a);
					}
				}
			}
			responses.add(r);
			x++;
			if (x == 1000) {
				x = 0;
				writer.addAnswers(responses);
				responses.clear();
			}
		}
		writer.addAnswers(responses);
	}
}
