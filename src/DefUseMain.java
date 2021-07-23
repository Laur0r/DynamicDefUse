public class DefUseMain {

	public double ifTest(double x, int z){
		if(x == 5){
			x = x+z;
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
		int z = 1;
		double x = obj.ifTest(y, z);
		//System.out.println("Transactions completed");
	}

}
