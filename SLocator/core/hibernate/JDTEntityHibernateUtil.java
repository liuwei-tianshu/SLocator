package SLocator.core.hibernate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import SLocator.datastructure.JDTEntityClass;
import SLocator.datastructure.JDTEntityField;
import SLocator.util.SqlUtil;


public class JDTEntityHibernateUtil {
	/**
	 * @param sql
	 * @param jdtEntityClassSet
	 * @return
	 */
	public static List<String> getHibernateFullSqls(String sql, Set<JDTEntityClass> jdtEntityClassSet){
		List<String> fullSqls = new ArrayList<String>();
		fullSqls.add(sql);
		
		// TODO take the first table as main table
		List<String> classNames = SqlUtil.getTableClassList(sql);
		if (classNames == null) {
			System.err.println("error in SqlUtil.getTableClassList(sql), sql = " + sql);
			return null;
		}
		
		// get entity from table names
		JDTEntityClass mainEntity = getJDTEntityClassByClassName(classNames.get(0), jdtEntityClassSet);
		for(int index = 1; index < classNames.size(); index++) {
			JDTEntityClass extraEntity = getJDTEntityClassByClassName(classNames.get(index), jdtEntityClassSet);
			if (extraEntity != null) {
				List<String> extraSelectSqls = getExtraSelectSqls(extraEntity, mainEntity, jdtEntityClassSet);
				if (extraSelectSqls != null) {
					fullSqls.addAll(extraSelectSqls);	
				}				
			}
		}
		
		return fullSqls;
	}
	
	/**
	 * 
	 * before update, hibernate needs to select from database
		Hibernate: select pet0_.id as id1_1_1_, pet0_.name as name2_1_1_, pet0_.birth_date as birth_da3_1_1_, pet0_.owner_id as owner_id4_1_1_, pet0_.type_id as type_id5_1_1_, visits1_.pet_id as pet_id4_1_3_, visits1_.id as id1_6_3_, visits1_.id as id1_6_0_, visits1_.visit_date as visit_da2_6_0_, visits1_.description as descript3_6_0_, visits1_.pet_id as pet_id4_6_0_ from pets pet0_ left outer join visits visits1_ on pet0_.id=visits1_.pet_id where pet0_.id=?
		Hibernate: select owner0_.id as id1_0_1_, owner0_.first_name as first_na2_0_1_, owner0_.last_name as last_nam3_0_1_, owner0_.address as address4_0_1_, owner0_.city as city5_0_1_, owner0_.telephone as telephon6_0_1_, pets1_.owner_id as owner_id4_0_3_, pets1_.id as id1_1_3_, pets1_.id as id1_1_0_, pets1_.name as name2_1_0_, pets1_.birth_date as birth_da3_1_0_, pets1_.owner_id as owner_id4_1_0_, pets1_.type_id as type_id5_1_0_ from owners owner0_ left outer join pets pets1_ on owner0_.id=pets1_.owner_id where owner0_.id=?
		Hibernate: select pettype0_.id as id1_3_0_, pettype0_.name as name2_3_0_ from types pettype0_ where pettype0_.id=?
		Hibernate: select pettype0_.id as id1_3_0_, pettype0_.name as name2_3_0_ from types pettype0_ where pettype0_.id=?
		Hibernate: select visits0_.pet_id as pet_id4_1_0_, visits0_.id as id1_6_0_, visits0_.id as id1_6_1_, visits0_.visit_date as visit_da2_6_1_, visits0_.description as descript3_6_1_, visits0_.pet_id as pet_id4_6_1_ from visits visits0_ where visits0_.pet_id=?
		Hibernate: select visits0_.pet_id as pet_id4_1_0_, visits0_.id as id1_6_0_, visits0_.id as id1_6_1_, visits0_.visit_date as visit_da2_6_1_, visits0_.description as descript3_6_1_, visits0_.pet_id as pet_id4_6_1_ from visits visits0_ where visits0_.pet_id=?
		Hibernate: update pets set name=?, birth_date=?, owner_id=?, type_id=? where id=?
	 * @param mainEntity
	 * @param jdtEntityClassSet
	 * @return
	 */
	public static List<String> getSelectSqlsBeforeUpdate(JDTEntityClass mainEntity, Set<JDTEntityClass> jdtEntityClassSet){
		List<String> fullSqls = new ArrayList<String>();
		List<JDTEntityClass> extraEntity = new ArrayList<JDTEntityClass>();
		// for extraEntity, it needs to avoid from/to entity
		List<JDTEntityClass[]> extraEntityList = new ArrayList<JDTEntityClass[]>();
		
		
		List<HibernateRelationship> hibernateRelationshipResult = mainEntity.getHibernateRelationshipResult();
		if (hibernateRelationshipResult == null || hibernateRelationshipResult.isEmpty()) {
			return fullSqls;
		}
		
		// OneToMany
		for(HibernateRelationship hibernateRelationship : hibernateRelationshipResult) {
			if (hibernateRelationship.getRelationShipType() == HibernateRelationship.RelationShipType.ONE_TO_MANY) {
				String targetClassName = hibernateRelationship.getTargetClassName();
				JDTEntityClass targetEntityClass = getTargetEntityByClassName(targetClassName, jdtEntityClassSet);
				if(targetEntityClass == null) {
					System.err.println("JDTEntityClass targetEntityClass = getTargetEntityByClassName(targetClassName, jdtEntityClassSet) targetEntityClass == null"
							+ "hibernateRelationshipResult:" + hibernateRelationshipResult);
					continue;
				}
				if (targetEntityClass.hasRespondingRelationship(HibernateRelationship.RelationShipType.MANY_TO_ONE, mainEntity)) {
					fullSqls.add(twoJointSqls(mainEntity, targetEntityClass));
					extraEntity.add(targetEntityClass);
					JDTEntityClass[] temp = new JDTEntityClass[2];
					temp[0] = targetEntityClass; temp[1] = mainEntity;
					extraEntityList.add(temp);
				}
			}
		}
		
		// ManyToOne
		for(HibernateRelationship hibernateRelationship : hibernateRelationshipResult) {
			if (hibernateRelationship.getRelationShipType() == HibernateRelationship.RelationShipType.MANY_TO_ONE) {
				String targetClassName = hibernateRelationship.getTargetClassName();
				JDTEntityClass from = getTargetEntityByClassName(targetClassName, jdtEntityClassSet);
				if (from == null) {
					System.err.println("from == null, targetClassName=" + targetClassName);
				} else if (from.hasRespondingRelationship(HibernateRelationship.RelationShipType.ONE_TO_MANY, mainEntity)) {
					fullSqls.add(twoJointSqls(from, mainEntity));
					JDTEntityClass[] temp = new JDTEntityClass[2];
					temp[0] = mainEntity; temp[1] = from;
					extraEntityList.add(temp);
				}
			}
		}
		
		for (JDTEntityClass[] entityClass:extraEntityList) {
			List<String> extraSelectSqls = getExtraSelectSqls2(entityClass, mainEntity, jdtEntityClassSet);
			if (extraSelectSqls != null && !extraSelectSqls.isEmpty()) {	
				for(String extraSql:extraSelectSqls) {
					if(!fullSqls.contains(extraSql)) {
						fullSqls.add(extraSql);
					}
				}
			}	
		}
		
		return fullSqls;
	}
	
	/**
	 * select pet0_.id as id1_1_1_, pet0_.name as name2_1_1_, pet0_.birth_date as birth_da3_1_1_, pet0_.owner_id as owner_id4_1_1_, pet0_.type_id as type_id5_1_1_, visits1_.pet_id as pet_id4_1_3_, visits1_.id as id1_6_3_, visits1_.id as id1_6_0_, visits1_.visit_date as visit_da2_6_0_, visits1_.description as descript3_6_0_, visits1_.pet_id as pet_id4_6_0_ from pets pet0_ left outer join visits visits1_ on pet0_.id=visits1_.pet_id where pet0_.id=?
	 * @param from
	 * @param to
	 * @return
	 */
	private static String twoJointSqls(JDTEntityClass from, JDTEntityClass to) {
		String field = from.getField();
		String join = (to.getLeftJoin() + " on " + from.getDbTableName() + "=" + to.getDbTableName() + " ");
		String sql = "select " + field + " from " + from.getDbTableName()  + " " + join  + " where " + from.gertPrimaryKeyName() + "=?";
		return sql;
	}
	
	/**
	 * 
		@Override
	    public Pet findById(int id) {
	        return this.em.find(Pet.class, id);
	    }
	    Hibernate: 
	    select pet0_.id as id1_1_0_, pet0_.name as name2_1_0_, pet0_.birth_date as birth_da3_1_0_, pet0_.owner_id as owner_id4_1_0_, pet0_.type_id as type_id5_1_0_, owner1_.id as id1_0_1_, owner1_.first_name as first_na2_0_1_, owner1_.last_name as last_nam3_0_1_, owner1_.address as address4_0_1_, owner1_.city as city5_0_1_, owner1_.telephone as telephon6_0_1_, pettype2_.id as id1_3_2_, pettype2_.name as name2_3_2_, visits3_.pet_id as pet_id4_1_3_, visits3_.id as id1_6_3_, visits3_.id as id1_6_4_, visits3_.visit_date as visit_da2_6_4_, visits3_.description as descript3_6_4_, visits3_.pet_id as pet_id4_6_4_ 
	    from pets pet0_ 
	    left outer join owners owner1_ on pet0_.owner_id=owner1_.id 
	    left outer join types pettype2_ on pet0_.type_id=pettype2_.id 
	    left outer join visits visits3_ on pet0_.id=visits3_.pet_id 
	    where pet0_.id=?
	    
	 * @param sql
	 * @param jdtEntityClassSet
	 * @return
	 */
	public static String getHibernateJoinSql(JDTEntityClass mainEntity, Set<JDTEntityClass> jdtEntityClassSet){
		String join = "";
		String field = mainEntity.getField();
		List<String> relationshipResult = mainEntity.getRelationshipResult();
		if (relationshipResult == null || relationshipResult.isEmpty()) {
			return mainEntity.getSelectSql();
		}
		
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
		for(String relationshipStr : relationshipResult) {
			String[] strs = relationshipStr.split("\\|");
			String relationship = strs[0];
			String currentClassName = strs[1];
			String targetClassName = strs[2];
			boolean eager = strs[3].equals("true") ? true : false;
			String cascadeType = strs[4];
			
			// need to ignore main table in mainEntity 
			JDTEntityClass targetEntityClass = null;
			if(!targetClassName.equals(mainEntity.getFullNameWithOutJava())) {
				if(targetClassName.contains("<") && targetClassName.contains(">")) {
					String realTargetClassName = getClassName(targetClassName);
					targetEntityClass = getTargetEntityByClassName(realTargetClassName, jdtEntityClassSet);
				} else{
					targetEntityClass = getTargetEntityByFullClassName(targetClassName, jdtEntityClassSet);
				}
				
				if(targetEntityClass == null) {
					System.err.println("targetEntityClass == null");
				} else {
					field += (", " + targetEntityClass.getField());
					join += (targetEntityClass.getLeftJoin() + " on " + mainEntity.getDbTableName() + "=" + targetEntityClass.getDbTableName() + " ");
				}
			}
		}
		
		String sql = "select " + field + " from " + mainEntity.getDbTableName() + " " + join  + " where " + mainEntity.gertPrimaryKeyName() + "=?";
		return sql;
	}
	
	/**
	 * hibernate needs to select from database before update
	 * this.em.merge(owner);
	 * Hibernate: select owner0_.id as id1_0_1_, owner0_.first_name as first_na2_0_1_, owner0_.last_name as last_nam3_0_1_, owner0_.address as address4_0_1_, owner0_.city as city5_0_1_, owner0_.telephone as telephon6_0_1_, pets1_.owner_id as owner_id4_0_3_, pets1_.id as id1_1_3_, pets1_.id as id1_1_0_, pets1_.name as name2_1_0_, pets1_.birth_date as birth_da3_1_0_, pets1_.owner_id as owner_id4_1_0_, pets1_.type_id as type_id5_1_0_ from owners owner0_ left outer join pets pets1_ on owner0_.id=pets1_.owner_id where owner0_.id=?
	   Hibernate: select pettype0_.id as id1_3_0_, pettype0_.name as name2_3_0_ from types pettype0_ where pettype0_.id=?
	   Hibernate: select visits0_.pet_id as pet_id4_1_0_, visits0_.id as id1_6_0_, visits0_.id as id1_6_1_, visits0_.visit_date as visit_da2_6_1_, visits0_.description as descript3_6_1_, visits0_.pet_id as pet_id4_6_1_ from visits visits0_ where visits0_.pet_id=?
	   Hibernate: update owners set first_name=?, last_name=?, address=?, city=?, telephone=? where id=?
	 */
	public static List<String> getHibernateMergeFullSqls(JDTEntityClass mainEntity, Set<JDTEntityClass> jdtEntityClassSet){
		List<String> fullSqls = new ArrayList<String>();
		
		List<String> sqlsBeforeUpdate = getSelectSqlsBeforeUpdate(mainEntity,jdtEntityClassSet);
		if (sqlsBeforeUpdate != null && !sqlsBeforeUpdate.isEmpty()) {
			fullSqls.addAll(sqlsBeforeUpdate);
		}
		fullSqls.add(mainEntity.getUpdateSql());
		return fullSqls;
	} 
	
	/**
	 * getAssociationEntity for mainEntity
	 * @param mainEntity
	 * @return
	 */
	public static List<JDTEntityClass> getAssociationEntity(JDTEntityClass mainEntity, Set<JDTEntityClass> jdtEntityClassSet) {
		List<JDTEntityClass> result = new ArrayList<JDTEntityClass>();
		List<String> relationshipResult = mainEntity.getRelationshipResult();
		if (relationshipResult == null || relationshipResult.isEmpty()) {
			return null;
		}
		
		for(String relationshipStr : relationshipResult) {
			String[] strs = relationshipStr.split("\\|");
			String relationship = strs[0];
			String currentClassName = strs[1];
			String targetClassName = strs[2];
			boolean eager = strs[3].equals("true") ? true : false;
			String cascadeType = strs[4];
			
			// need to ignore main table in mainEntity 
			JDTEntityClass targetEntityClass = null;
			if(!targetClassName.equals(mainEntity.getFullNameWithOutJava())) {
				if(targetClassName.contains("<") && targetClassName.contains(">")) {
					String realTargetClassName = getClassName(targetClassName);
					targetEntityClass = getTargetEntityByClassName(realTargetClassName, jdtEntityClassSet);
				} else{
					targetEntityClass = getTargetEntityByFullClassName(targetClassName, jdtEntityClassSet);
				}
				
				if(targetEntityClass == null) {
					System.err.println("targetEntityClass == null");
				} else {
					result.add(targetEntityClass);
				}
			}
		}
		return result;
	}
	
	/**
	 * example1:
	 * 		sql:"SELECT owner FROM Owner owner left join fetch owner.pets WHERE owner.id =:id"
	 * 		className: owner.pets
	 * 		return: EntityClass Pet
	 * @param className
	 * @return
	 */
	public static JDTEntityClass getJDTEntityClassByClassName(String className, Set<JDTEntityClass> jdtEntityClassSet) {
		if (className.contains(".")) {
			String[] strs = className.split("\\.");
			String firstName = strs[0];
			String secondName = strs[1];
			String entityClassName = null;
			
			/**
			 * TODO it's just a check here
			 */
			for(JDTEntityClass entity : jdtEntityClassSet) {
				if(entity.getClassName().toLowerCase().equals(firstName.toLowerCase())) {
					for(JDTEntityField field : entity.getEntityFieldList()) {
						if(field.getFieldName().equals(secondName)) {
							entityClassName = field.getClassName();
						}
					}
				}
			}
			
			if(entityClassName == null) {
				System.err.println("getJDTEntityClassByClassName error,entityClassName == null, className=" + className);
				return null;
			}
			
			className = entityClassName;
		}
		
		for(JDTEntityClass entity : jdtEntityClassSet) {
			if (entity.getClassName().equals(className)) {
				return entity;
			}
		}
		System.err.println("getJDTEntityClassByClassName error,className=" + className);
		return null;
	}
	
	/**
	 * get all extra sqls for JDTEntityClass
	 * need to ignore the main table in mainEntity
	 * 
	 * example:
	 * 		"SELECT owner FROM Owner owner left join fetch owner.pets WHERE owner.id =:id"
	 *      entityClass: Pet
	 *      mainEntity:Owner
	 *      
	 * example:
	 * 		
	 * @param entityClass [0] is the entityClass, [1] is the entityClass need to be avoided
	 * @param mainEntity
	 * @return
	 */
	public static List<String> getExtraSelectSqls(JDTEntityClass entityClass, JDTEntityClass mainEntity, Set<JDTEntityClass> jdtEntityClassSet){
		List<String> extraSelectSqls = new ArrayList<String>();
		List<String> relationshipResult = entityClass.getRelationshipResult();
		String avoidClassName = entityClass.getClassName();
		if (relationshipResult == null || relationshipResult.isEmpty()) {
			return null;
		}
		
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
		for(String relationshipStr : relationshipResult) {
			String[] strs = relationshipStr.split("\\|");
			String relationship = strs[0];
			String currentClassName = strs[1];
			String targetClassName = strs[2];
			boolean eager = strs[3].equals("true") ? true : false;
			String cascadeType = strs[4];
			
			// need to ignore main table in mainEntity 
			JDTEntityClass targetEntityClass = null;
			if(!targetClassName.equals(mainEntity.getFullNameWithOutJava()) && !targetClassName.equals(avoidClassName)) {
				if(targetClassName.contains("<") && targetClassName.contains(">")) {
					String realTargetClassName = getClassName(targetClassName);
					targetEntityClass = getTargetEntityByClassName(realTargetClassName, jdtEntityClassSet);
				} else{
					targetEntityClass = getTargetEntityByFullClassName(targetClassName, jdtEntityClassSet);
				}
				
				if(targetEntityClass == null) {
					System.err.println("targetEntityClass == null");
				} else {
					extraSelectSqls.add(targetEntityClass.getSelectSql());
				}
			}
		}
		return extraSelectSqls;
	}
	
	/**
	 * get all extra sqls for JDTEntityClass
	 * need to ignore the main table in mainEntity
	 * 
	 * example:
	 * 		"SELECT owner FROM Owner owner left join fetch owner.pets WHERE owner.id =:id"
	 *      entityClass: Pet
	 *      mainEntity:Owner
	 *      
	 * example:
	 * 		
	 * @param entityClass [0] is the entityClass, [1] is the entityClass need to be avoided
	 * @param mainEntity
	 * @return
	 */
	public static List<String> getExtraSelectSqls2(JDTEntityClass[] entityClass, JDTEntityClass mainEntity, Set<JDTEntityClass> jdtEntityClassSet){
		List<String> extraSelectSqls = new ArrayList<String>();
		List<String> relationshipResult = entityClass[0].getRelationshipResult();
		String avoidClassName = entityClass[1].getClassName();
		if (relationshipResult == null || relationshipResult.isEmpty()) {
			return null;
		}
		
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
		for(String relationshipStr : relationshipResult) {
			String[] strs = relationshipStr.split("\\|");
			String relationship = strs[0];
			String currentClassName = strs[1];
			String targetClassName = strs[2];
			String targetSimpleClassName = getClassNameFromFullName(targetClassName);
			boolean eager = strs[3].equals("true") ? true : false;
			String cascadeType = strs[4];
			
			// need to ignore main table in mainEntity 
			JDTEntityClass targetEntityClass = null;
			if(!targetClassName.equals(mainEntity.getFullNameWithOutJava()) && !targetSimpleClassName.equals(avoidClassName)) {
				if(targetClassName.contains("<") && targetClassName.contains(">")) {
					String realTargetClassName = getClassName(targetClassName);
					targetEntityClass = getTargetEntityByClassName(realTargetClassName, jdtEntityClassSet);
				} else{
					targetEntityClass = getTargetEntityByFullClassName(targetClassName, jdtEntityClassSet);
				}
				
				if(targetEntityClass == null) {
					System.err.println("targetEntityClass == null");
				} else {
					extraSelectSqls.add(targetEntityClass.getSelectSql());
				}
			}
		}
		return extraSelectSqls;
	}
	
	private static String getClassNameFromFullName(String fullClassName) {
		String[] str = fullClassName.split("\\.");
		if(str !=null && str.length >= 1) {
			return str[str.length-1];
		}
		System.err.println("error in getClassNameFromFullName, fullClassName=" + fullClassName);
		return "";
	}
	
	private static JDTEntityClass getTargetEntityByFullClassName(String className, Set<JDTEntityClass> jdtEntityClassSet) {
		for(JDTEntityClass entity : jdtEntityClassSet) {
			if (entity.getFullNameWithOutJava().equals(className)) {
				return entity;
			}
		}
		System.err.println("getTargetEntityByFullClassName error,className=" + className);
		return null;
	}
	
	private static JDTEntityClass getTargetEntityByClassName(String className, Set<JDTEntityClass> jdtEntityClassSet) {
		for(JDTEntityClass entity : jdtEntityClassSet) {
			if (entity.getClassName().equals(className)) {
				return entity;
			}
		}
		System.err.println("getTargetEntityByClassName error,className=" + className);
		return null;
	}
	
	/**
	 * example: 
	 * 		java.util.Set<Pet>
	 * 		java.util.List<Pet>
	 * 		java.util.Map<String,StaticAssetDescription>
	 * @return
	 */
	public static String getClassName(String str) {
		String regEx = "";
		if (str.startsWith("java.util.Set") || str.startsWith("java.util.List") || str.startsWith("java.util.SortedSet")) {
			regEx = "(?<=\\<)(\\w+)(?=\\>)";
			Pattern pattern = Pattern.compile(regEx);
			Matcher matcher = pattern.matcher(str);
			String className = "";
			if (matcher.find()) {
				className = matcher.group();
				return className;
			} else {
				System.err.println("JDTEntityHibernateUtil.getClassName getClassName(String str) error str=" + str);
				return null;
			}
		} else if (str.startsWith("java.util.Map")) {
			regEx = "(?<=\\<)(\\w+,\\s*\\w+)(?=\\>)";
			Pattern pattern = Pattern.compile(regEx);
			Matcher matcher = pattern.matcher(str);
			String className = "";
			if (matcher.find()) {
				className = matcher.group();
				String[] calssNames = className.split(",");
				if (calssNames == null || calssNames.length !=2) {
					System.err.println("JDTEntityHibernateUtil.getClassName getClassName(String str) in java.util.Map error str=" + str);
					return null;
				}
				return calssNames[1].trim();
			} else {
				System.err.println("JDTEntityHibernateUtil.getClassName getClassName(String str) error str=" + str);
				return null;
			}
		} else {
			System.err.println("JDTEntityHibernateUtil.getClassName getClassName(String str) no regEx error str=" + str);
			return null;
		}
	}
	
	public static void main(String[] args){
		System.out.println(getClassName("java.util.Set<Pet>"));
		System.out.println(getClassName("java.util.Set<Visit>"));
		System.out.println(getClassName("java.util.List<Pet>"));
		System.out.println(getClassName("java.util.Map<String,StaticAssetDescription>"));
		System.out.println(getClassName("java.util.Map<String, StaticAssetDescription>"));
		System.out.println(getClassName("java.util.Map<String,  StaticAssetDescription>"));
	}
}
