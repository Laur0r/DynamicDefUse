public class DefUseMain {

	public static void main(String[] args) {
		//int length = args.length;
		//DefUseMain obj = new DefUseMain();
		//DefUseMain obj2 = obj;
		Increment inc = new Increment();
		Test t = new Test();
		//Increment inc2 = new Increment();
		//inc.x = 5;
		//inc2.x = 6;
		//System.out.println(inc.x);
		inc.add(t.h);
		//System.out.println("Transactions completed");
	}

}

class Test{
 	public String h = "hello";
}
