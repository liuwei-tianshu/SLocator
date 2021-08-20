package SLocator.datastructure;


public class MethodPosition extends Position{
	private String methodName;

	public MethodPosition(String methodName, int start, int end) {
		super(start, end);
		this.methodName = methodName;

	}

	public String getMethodName() {
		return methodName.trim();
	}

	@Override
	public boolean equals(Object B) {
		MethodPosition other = (MethodPosition) B;
//		System.out.println(this.getStartPos() == other.getStartPos());
//		System.out.println(this.getEndPos() == other.getEndPos());

		if (this.getStartPos() == other.getStartPos()
				&& this.getEndPos() == other.getEndPos()
				&& this.getMethodName().equals(other.getMethodName())) {
			return true;
		}
		return false;
	}

	public String toString(){
		return this.getMethodName() + " " + this.getStartPos() + " " + this.getEndPos();
	}
	
	public int hashCode(){
		return this.toString().hashCode();
	}
}
