package SLocator.datastructure;

import lombok.Data;

/**
 * a field in the entity class / a column in the table
 */
@Data
public class JDTEntityField {
	private String fieldName = "";
	private String columnName = "";
	private boolean primaryKey = false;
	private String className = "";
	
	public JDTEntityField(String fieldName, String columnName, boolean primaryKey, String className) {
		this.fieldName = fieldName;
		this.columnName = columnName;
		this.primaryKey = primaryKey;
		this.className = className;
	}
}
