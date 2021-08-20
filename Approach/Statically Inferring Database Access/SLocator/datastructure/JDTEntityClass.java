package SLocator.datastructure;

import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;

import SLocator.core.hibernate.HibernateRelationship;
import lombok.Data;

/**
 * an entity class / a table in the database
 */
@Data
public class JDTEntityClass extends JDTJavaClass {
	private String dbTableName = "";
	private List<JDTEntityField> entityFieldList;
	
	/**
	 * [ManyToOne|org.springframework.samples.petclinic.model.Visit|org.springframework.samples.petclinic.model.Pet|true|None]
	 * [ManyToMany|org.springframework.samples.petclinic.model.Vet|java.util.Set<Specialty>|true|None]
	 * 
	 * [ManyToOne|org.springframework.samples.petclinic.model.Pet|org.springframework.samples.petclinic.model.PetType|true|None, 
	 * ManyToOne|org.springframework.samples.petclinic.model.Pet|org.springframework.samples.petclinic.model.Owner|true|None, 
	 * OneToMany|org.springframework.samples.petclinic.model.Pet|java.util.Set<Visit>|true|All]
	 * 
	 * [OneToMany|org.springframework.samples.petclinic.model.Owner|java.util.Set<Pet>|false|All]
	 * 
	 */
	private	List<String> relationshipResult;
	private List<HibernateRelationship> hibernateRelationshipResult;
	
	public boolean hasRespondingRelationship(HibernateRelationship.RelationShipType type, JDTEntityClass entity) {
		if(hibernateRelationshipResult == null || hibernateRelationshipResult.isEmpty()) {
			return false;
		}
		
		for(HibernateRelationship re:hibernateRelationshipResult) {
			if(re.getRelationShipType() == type) {
				if(re.getTargetClassName() == null) {
					System.err.println("hasRespondingRelationship re.getTargetClassName() == null entity:" + entity);
					return false;
				}
				if(re.getTargetClassName().equals(entity.getClassName())) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	public JDTEntityClass(String packageName, String className) {
		super(packageName, className);
	}

	public JDTEntityClass(String packageName, String className, ICompilationUnit unit) {
		super(packageName, className, unit, JDTClassType.JDT_CLASS);
	}

	public JDTEntityClass(String packageName, String className, String dbTableName) {
		super(packageName, className);
		this.dbTableName = dbTableName;
	}
	
	public JDTEntityClass(String packageName, String className, String dbTableName, List<JDTEntityField> entityFieldList) {
		super(packageName, className);
		this.dbTableName = dbTableName;
		this.entityFieldList = entityFieldList;
	}

	public String toString() {
		if (this.getPackageName().equals(""))
			return this.getDbTableName();
		return this.getPackageName() + "." + this.getClassName();
		// return this.getDBTableName();
	}
	
	/**
	 *	select
	        test0_.id as id1_1_0_,
	        test0_.NAME as NAME2_1_0_ 
	    from
	        TEST test0_ 
	    where
	        test0_.id=?
	 * @return
	 */
	public String getSelectSql() {
		String sql = "select ";
		String field = "";
		for (JDTEntityField jdtEntityField : entityFieldList) {
			field += " " + jdtEntityField.getColumnName();
		}
		field = field.trim().replaceAll(" ", ", ");
		sql = sql + field + " from " + dbTableName + " where " + gertPrimaryKeyName() + "=?";
		return sql;
	}
	
	/**
	 * get all columns/fields
	 * 
	 * @return
	 */
	public String getField() {
		String field = "";
		for (JDTEntityField jdtEntityField : entityFieldList) {
			field += " " + jdtEntityField.getColumnName();
		}
		field = field.trim().replaceAll(" ", ", ");
		return field;
	}
	
	/**
	 * 	left outer join owners owner1_ on pet0_.owner_id=owner1_.id 
	    left outer join types pettype2_ on pet0_.type_id=pettype2_.id 
	    left outer join visits visits3_ on pet0_.id=visits3_.pet_id 
	 * @return
	 */
	public String getLeftJoin() {
		return "left outer join " + dbTableName;
	}
	
	/**
	 *  insert 
        into
            TEST
            (id, NAME) 
        values
            (null, ?)
	 * @return
	 */
	public String getPersistSql() {
		String sql = "insert into " + dbTableName;
		String field = "";
		for (JDTEntityField jdtEntityField : entityFieldList) {
			field += " " + jdtEntityField.getColumnName();
		}
		field = field.trim().replaceAll(" ", ", ");
		
		String value = "";
		for (JDTEntityField jdtEntityField : entityFieldList) {
			value += " ?";
		}
		value = value.trim().replaceAll(" ", ", ");
		
		sql = sql + " (" + field + ") " +"values" + " (" + value + ")";
		return sql;
	}
	
	/**
	 *  delete 
        from
            TEST 
        where
            id=?
	 * @return
	 */
	public String getRemoveSql() {
		String sql = "delete from " + dbTableName + " where " + gertPrimaryKeyName() + "=?";
		return sql;
	}
	
	public String gertPrimaryKeyName() {
		for (JDTEntityField jdtEntityField : entityFieldList) {
			if (jdtEntityField.isPrimaryKey()) {
				return jdtEntityField.getColumnName();
			}
		}
		return "id";
	}
	
	/**
	 * update owners set first_name=?, last_name=?, address=?, city=?, telephone=? where id=?
	 * @return
	 */
	public String getUpdateSql() {
		String sql = "update " + dbTableName + " set ";
		String field = "";
		for (JDTEntityField jdtEntityField : entityFieldList) {
			field += " " + jdtEntityField.getColumnName() + "=?";
		}
		field = field.trim().replaceAll(" ", ", ");
		return sql + field + " where " + gertPrimaryKeyName() + "=?";
	}
}
