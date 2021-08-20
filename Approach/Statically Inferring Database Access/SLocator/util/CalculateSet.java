package SLocator.util;

/**
 */
public class CalculateSet {
	
		public <T> T genericMethod(Class<T> tClass)throws InstantiationException ,
		  IllegalAccessException{
		        T instance = tClass.newInstance();
		        return instance;
		}

}
