public class DefUseMain {

	public double ifTest(double x){
		if(x == 5){
			x = x+1;
		} else {
			x = 5;
		}
		return x;
	}

	public static void main(String[] args) {
		//int length = args.length;
		DefUseMain obj = new DefUseMain();
		DefUseMain obj2 = obj;
		double y = 5;
		double x = obj.ifTest(y);
		//System.out.println("Transactions completed");
	}

}
