package SLocator.datastructure;

import lombok.Getter;

/**
 * 
 */
public enum Program {
    PETCLINIC("Petclinic", "E:/Projects/analyze-workspace/workspace-old-petclinic"), 
    CLOUD_STORE("BroadleafCommerce", "E:/Projects/analyze-workspace/CloudStore"),
    WALL_RIDE("WallRide", "E:/Projects/analyze-workspace/WallRide"),
    PUBLIC_CMS("PublicCMS", "E:/Projects/analyze-workspace/PublicCMS-workspace-springboot");
	
    @Getter private String name;  
    @Getter private String path;
    
    Program(String name, String path) {  
        this.name = name;  
        this.path = path;  
    }
    
}
