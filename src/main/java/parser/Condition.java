package parser;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor
public class Condition
{
	protected String type;
	protected String oid;
	protected String cond;
}
