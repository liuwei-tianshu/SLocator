package SLocator.core.hibernate;

import lombok.Data;
import lombok.Getter;

/**
 * [ManyToOne|org.springframework.samples.petclinic.model.Visit|org.springframework.samples.petclinic.model.Pet|true|None]
 * [ManyToMany|org.springframework.samples.petclinic.model.Vet|java.util.Set<Specialty>|true|None]
 * 
 * [ManyToOne|org.springframework.samples.petclinic.model.Pet|org.springframework.samples.petclinic.model.PetType|true|None, 
 * ManyToOne|org.springframework.samples.petclinic.model.Pet|org.springframework.samples.petclinic.model.Owner|true|None, 
 * OneToMany|org.springframework.samples.petclinic.model.Pet|java.util.Set<Visit>|true|All]
 * 
 * [OneToMany|org.springframework.samples.petclinic.model.Owner|java.util.Set<Pet>|false|All]
 */
@Data
public class HibernateRelationship {
	RelationShipType relationShipType = null;			// ManyToOne,OneToMany		
	
	String currentClassName = "";		// without ".java"
	String currentFullClassName = "";	// packageName+"."+className
	
	String targetClassName = "";
	String targetFullClassName = "";	
	
	boolean eager = false;	
	String cascadeType = "";
	
	public enum RelationShipType {
		MANY_TO_ONE("ManyToOne"),
		ONE_TO_MANY("OneToMany"),
		ONE_TO_ONE("OneToOne"),
		MANY_TO_MANY("ManyToMany");
		
		@Getter private String type;
		
		RelationShipType(String type) {
			this.type = type;
		}
	}
	
	public HibernateRelationship(RelationShipType relationshipType,String currentFullClassName,String targetClassName) {
		this.relationShipType = relationshipType;
		
		this.currentFullClassName = currentFullClassName;
		this.currentClassName = getClassNameFromFullName(currentFullClassName);
		
		if(targetClassName.contains("<") && targetClassName.contains(">")) {
			this.targetClassName = JDTEntityHibernateUtil.getClassName(targetClassName);
		} else{
			this.targetFullClassName = targetClassName;
			this.targetClassName = getClassNameFromFullName(targetClassName);
		}
	}
	
	private static String getClassNameFromFullName(String fullClassName) {
		String[] str = fullClassName.split("\\.");
		if(str !=null && str.length >= 1) {
			return str[str.length-1];
		}
		System.err.println("error in getClassNameFromFullName, fullClassName=" + fullClassName);
		return "";
	}
}
