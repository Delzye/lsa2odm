package parser;

import java.io.File;
import java.util.ArrayList;
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
	@Getter ArrayList<Response> responses;

	public LsrParser(File lsr_file, ArrayList<QuestionGroup> qg_list)
	{
		this.lsr_file = lsr_file;
		this.qg_list = qg_list;
		responses = new ArrayList<Response>();
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
		
		// Iterate over all row-Elements (one per survey-participant)
		for (Element row : row_list) {

			Response r = new Response();
			r.id = Integer.parseInt(row.element("id").getText());
			
			for (QuestionGroup qg : qg_list) {
				r.answers.put(qg.getGID(), new ArrayList<Answer>());
			}
			@SuppressWarnings("unchecked")
			List<Element> children = row.elements();
			// Iterate over all elements in row (most of which are answers, but also id, language etc.)
			for (Element e : children) {

				// Pattern for Answer-Names: _{Survey_id}X{gid}X{qid}{ext}
				// Where ext can be either the title of a subquestion or other/comment
				Pattern ans_p = Pattern.compile("^_\\d+X(\\d+)X(.+?)$");
				log.info(e.getName());
				Matcher e_match = ans_p.matcher(e.getName());

				// If this is an answer and not another element
				if (e_match.find()) {
					log.info("Found matching Element, parsing first group: " + e_match.group(1) + " Second group: " + e_match.group(2));
					String ans = e.getText();

					// only add an answer if it isn't empty
					if (ans != "") { 
						Answer a = new Answer(Integer.parseInt(e_match.group(1)), e_match.group(2), ans);
						log.info(a.toString());
						r.addToAnswers(a);
					}
				}
			}
			responses.add(r);
		}
	}
}
