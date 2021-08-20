package SLocator.datastructure;

public class Position {
	private int start;
	private int end;
	
	public Position(int start, int end){
		this.start = start;
		this.end = end;
	}
	
	public int getStartPos(){
		return start;
	}
	
	public int getEndPos(){
		return end;
	}
	
	/**
	 * See if this is within p
	 * @param p
	 * @return
	 */
	public boolean isWithin(Position p){
		if (p.getStartPos() < this.start && p.getEndPos() > this.end)
			return true;
		return false;
	}
	
	@Override
	public boolean equals(Object B){
		if(this.start == ((Position)B).start && this.end == ((Position)B).end){
			return true;
		}
		return false;
	}
	
	@Override
	public int hashCode(){
		return start+end;
	}
	
	@Override
	public String toString(){
		return ""+start + end;
	}
}
